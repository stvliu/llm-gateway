package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.common.ProviderCapabilities;
import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenAI 兼容接口适配器
 *
 * <p>实现 OpenAI API 兼容端点的适配器。</p>
 * <p>使用 OkHttp 进行 HTTP 通信。</p>
 */
@Slf4j
public class OpenAIAdapter implements LLMAdapter {

    public static final String PROVIDER_CODE = "openai";
    /** OpenAI Chat Completions API 路径 */
    protected static final String CHAT_COMPLETIONS_URL = "/v1/chat/completions";

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    public OpenAIAdapter(OkHttpClient httpClient, String baseUrl, String apiKey, int timeoutSeconds) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.OPENAI;
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        log.info("OpenAI chat request: model={}, stream=false", request.getModel());

        Map<String, Object> requestBody = buildRequestBody(request);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(getChatCompletionsUrl())
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json");

            // 子类可覆盖添加额外头部
            addExtraHeaders(requestBuilder);

            requestBuilder.post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

            Request httpRequest = requestBuilder.build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenAI chat request failed: " + response);
                }

                String responseBody = response.body().string();
                LLMResponse llmResponse = parseResponse(responseBody);
                log.info("OpenAI chat response: id={}, model={}", llmResponse.getId(), llmResponse.getModel());
                return llmResponse;
            }
        } catch (IOException e) {
            log.error("OpenAI chat error: model={}, error={}", request.getModel(), e.getMessage(), e);
            throw new RuntimeException("OpenAI chat request failed", e);
        }
    }

    @Override
    public void chatStream(LLMRequest request, StreamCallback callback) {
        log.info("OpenAI chat stream request: model={}, stream=true", request.getModel());

        request.setStream(true);
        Map<String, Object> requestBody = buildRequestBody(request);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(getChatCompletionsUrl())
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream");

            // 子类可覆盖添加额外头部
            addExtraHeaders(requestBuilder);

            requestBuilder.post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

            Request httpRequest = requestBuilder.build();

            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("OpenAI stream error: {}", e.getMessage(), e);
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody body = response.body()) {
                        if (body == null) {
                            callback.onError(new RuntimeException("Empty response body"));
                            return;
                        }

                        java.io.BufferedReader reader = new java.io.BufferedReader(body.charStream());
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.isEmpty() && line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if (!"[DONE]".equals(data)) {
                                    log.debug("OpenAI stream chunk: {}", data);
                                    callback.onChunk(data);
                                }
                            }
                        }
                        log.info("OpenAI stream completed");
                        callback.onComplete();
                    } catch (Exception e) {
                        log.error("OpenAI stream error: {}", e.getMessage(), e);
                        callback.onError(e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("OpenAI stream error: {}", e.getMessage(), e);
            callback.onError(e);
        }
    }

    @Override
    public LLMResponse messages(LLMRequest request) {
        throw new UnsupportedOperationException(
                "OpenAI adapter does not support Anthropic messages format. Use chat() instead.");
    }

    @Override
    public void messagesStream(LLMRequest request, StreamCallback callback) {
        throw new UnsupportedOperationException(
                "OpenAI adapter does not support Anthropic messages format. Use chatStream() instead.");
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public boolean isHealthy() {
        try {
            return isAvailable();
        } catch (Exception e) {
            log.warn("OpenAI health check failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean checkConnection() {
        Request request = new Request.Builder()
                .url(baseUrl + "/v1/models")
                .header("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            log.warn("OpenAI connection check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return new ProviderCapabilities(
                ProviderType.OPENAI,
                true,   // supportsChatCompletion
                false,  // supportsMessages (Anthropic 格式)
                true,   // supportsEmbeddings
                true,   // supportsStreaming
                true,   // supportsFunctionCalling
                Set.of("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo",
                      "text-embedding-3-small", "text-embedding-3-large")
        );
    }

    @Override
    public int getDefaultTimeout() {
        return timeoutSeconds;
    }

    private Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        body.put("messages", request.getMessages());
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.isStream()) {
            body.put("stream", true);
        }
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools());
        }
        if (request.getToolChoice() != null) {
            body.put("tool_choice", request.getToolChoice());
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private LLMResponse parseResponse(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            return LLMResponse.builder()
                    .provider(PROVIDER_CODE)
                    .id((String) response.get("id"))
                    .model((String) response.get("model"))
                    .created(response.get("created") != null ? ((Number) response.get("created")).longValue() : null)
                    .content(parseContent(response))
                    .usage(parseUsage(response))
                    .finishReason((String) ((List<Map<String, Object>>) response.get("choices")).get(0).get("finish_reason"))
                    .stream(false)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private LLMResponse.Content parseContent(Map<String, Object> response) {
        var choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        var choice = choices.get(0);
        var message = (Map<String, Object>) choice.get("message");
        if (message == null) {
            return null;
        }
        return LLMResponse.Content.builder()
                .role((String) message.get("role"))
                .text((String) message.get("content"))
                .toolCalls(parseToolCalls(message))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<LLMResponse.ToolCall> parseToolCalls(Map<String, Object> message) {
        var toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
        if (toolCalls == null || toolCalls.isEmpty()) {
            return null;
        }
        return toolCalls.stream().map(tc -> {
            var function = (Map<String, Object>) tc.get("function");
            return LLMResponse.ToolCall.builder()
                    .id((String) tc.get("id"))
                    .type((String) tc.get("type"))
                    .function(LLMResponse.FunctionCall.builder()
                            .name((String) function.get("name"))
                            .arguments((String) function.get("arguments"))
                            .build())
                    .build();
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private LLMResponse.Usage parseUsage(Map<String, Object> response) {
        var usage = (Map<String, Object>) response.get("usage");
        if (usage == null) {
            return null;
        }
        return LLMResponse.Usage.builder()
                .promptTokens(usage.get("prompt_tokens") != null ? ((Number) usage.get("prompt_tokens")).intValue() : null)
                .completionTokens(usage.get("completion_tokens") != null ? ((Number) usage.get("completion_tokens")).intValue() : null)
                .totalTokens(usage.get("total_tokens") != null ? ((Number) usage.get("total_tokens")).intValue() : null)
                .build();
    }

    /**
     * 获取 Chat Completions API 的完整 URL
     *
     * <p>子类可覆盖此方法以支持不同的 API 路径。</p>
     * <p>例如火山引擎使用 /v3/chat/completions</p>
     *
     * @return 完整的 API URL
     */
    protected String getChatCompletionsUrl() {
        return baseUrl + CHAT_COMPLETIONS_URL;
    }

    /**
     * 添加额外的请求头
     *
     * <p>子类可覆盖此方法以添加额外的请求头。</p>
     *
     * @param requestBuilder 请求构建器
     */
    protected void addExtraHeaders(Request.Builder requestBuilder) {
        // 默认无操作，子类可覆盖
    }
}
