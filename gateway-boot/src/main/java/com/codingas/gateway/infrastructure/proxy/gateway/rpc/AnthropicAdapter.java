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
 * Anthropic Claude 适配器
 *
 * <p>实现 Anthropic 消息 API 格式的适配器。</p>
 * <p>使用 OkHttp 进行 HTTP 通信。</p>
 */
@Slf4j
public class AnthropicAdapter implements LLMAdapter {

    public static final String PROVIDER_CODE = "anthropic";
    private static final String MESSAGES_URL = "/v1/messages";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String version;
    private final int timeoutSeconds;

    public AnthropicAdapter(OkHttpClient httpClient, String baseUrl, String apiKey, String version, int timeoutSeconds) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.version = version != null ? version : "2023-06-01";
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.ANTHROPIC;
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        throw new UnsupportedOperationException(
                "Anthropic adapter does not support OpenAI chat format. Use messages() instead.");
    }

    @Override
    public void chatStream(LLMRequest request, StreamCallback callback) {
        throw new UnsupportedOperationException(
                "Anthropic adapter does not support OpenAI chat format. Use messagesStream() instead.");
    }

    @Override
    public LLMResponse messages(LLMRequest request) {
        log.info("Anthropic messages request: model={}, stream=false", request.getModel());

        Map<String, Object> requestBody = buildMessagesRequestBody(request);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request httpRequest = new Request.Builder()
                    .url(baseUrl + MESSAGES_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("anthropic-version", version)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Anthropic messages request failed: " + response);
                }

                String responseBody = response.body().string();
                LLMResponse llmResponse = parseResponse(responseBody);
                log.info("Anthropic messages response: id={}, model={}", llmResponse.getId(), llmResponse.getModel());
                return llmResponse;
            }
        } catch (IOException e) {
            log.error("Anthropic messages error: model={}, error={}", request.getModel(), e.getMessage(), e);
            throw new RuntimeException("Anthropic messages request failed", e);
        }
    }

    @Override
    public void messagesStream(LLMRequest request, StreamCallback callback) {
        log.info("Anthropic messages stream request: model={}, stream=true", request.getModel());

        request.setStream(true);
        Map<String, Object> requestBody = buildMessagesRequestBody(request);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request httpRequest = new Request.Builder()
                    .url(baseUrl + MESSAGES_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("anthropic-version", version)
                    .header("Accept", "text/event-stream")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("Anthropic stream error: {}", e.getMessage(), e);
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
                                    log.debug("Anthropic stream chunk: {}", data);
                                    callback.onChunk(data);
                                }
                            }
                        }
                        log.info("Anthropic stream completed");
                        callback.onComplete();
                    } catch (Exception e) {
                        log.error("Anthropic stream error: {}", e.getMessage(), e);
                        callback.onError(e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Anthropic stream error: {}", e.getMessage(), e);
            callback.onError(e);
        }
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
            log.warn("Anthropic health check failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean checkConnection() {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "claude-haiku-3-5-20250514");
        body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
        body.put("max_tokens", 1);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            Request request = new Request.Builder()
                    .url(baseUrl + MESSAGES_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("anthropic-version", version)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.warn("Anthropic connection check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return new ProviderCapabilities(
                ProviderType.ANTHROPIC,
                false,  // supportsChatCompletion (OpenAI 格式)
                true,   // supportsMessages (Anthropic 格式)
                false,  // supportsEmbeddings (Anthropic 不支持)
                true,   // supportsStreaming
                true,   // supportsFunctionCalling
                Set.of("claude-opus-4-5", "claude-sonnet-4-6", "claude-haiku-4-5",
                      "claude-opus-4", "claude-sonnet-4", "claude-haiku-3-5")
        );
    }

    @Override
    public int getDefaultTimeout() {
        return timeoutSeconds;
    }

    private Map<String, Object> buildMessagesRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        body.put("messages", request.getMessages());

        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        } else {
            body.put("max_tokens", 1024);
        }

        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }

        if (request.getSystemPrompt() != null) {
            body.put("system", request.getSystemPrompt());
        }

        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools());
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
                    .content(parseContent(response))
                    .usage(parseUsage(response))
                    .finishReason((String) response.get("stop_reason"))
                    .stream(false)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Anthropic response", e);
        }
    }

    @SuppressWarnings("unchecked")
    private LLMResponse.Content parseContent(Map<String, Object> response) throws com.fasterxml.jackson.core.JsonProcessingException {
        var content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            return null;
        }

        StringBuilder textBuilder = new StringBuilder();
        List<LLMResponse.ToolCall> toolCalls = null;

        for (Map<String, Object> block : content) {
            String blockType = (String) block.get("type");
            if ("text".equals(blockType)) {
                String text = (String) block.get("text");
                if (text != null) {
                    textBuilder.append(text);
                }
            } else if ("tool_use".equals(blockType)) {
                if (toolCalls == null) {
                    toolCalls = new java.util.ArrayList<>();
                }
                String toolName = (String) block.get("name");
                String toolInputJson = objectMapper.writeValueAsString(block.get("input"));
                String toolId = (String) block.get("id");

                toolCalls.add(LLMResponse.ToolCall.builder()
                        .id(toolId)
                        .type("function")
                        .function(LLMResponse.FunctionCall.builder()
                                .name(toolName)
                                .arguments(toolInputJson)
                                .build())
                        .build());
            }
        }

        return LLMResponse.Content.builder()
                .role("assistant")
                .text(textBuilder.toString())
                .toolCalls(toolCalls)
                .build();
    }

    @SuppressWarnings("unchecked")
    private LLMResponse.Usage parseUsage(Map<String, Object> response) {
        var usage = (Map<String, Object>) response.get("usage");
        if (usage == null) {
            return null;
        }
        return LLMResponse.Usage.builder()
                .promptTokens(usage.get("input_tokens") != null ? ((Number) usage.get("input_tokens")).intValue() : null)
                .completionTokens(usage.get("output_tokens") != null ? ((Number) usage.get("output_tokens")).intValue() : null)
                .totalTokens(null)
                .build();
    }
}
