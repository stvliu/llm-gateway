package com.codingas.gateway.adapter.anthropic;

import com.codingas.gateway.adapter.LLMProviderAdapter;
import com.codingas.gateway.adapter.StreamCallback;
import com.codingas.gateway.adapter.common.ProviderCapabilities;
import com.codingas.gateway.adapter.common.ProviderType;
import com.codingas.gateway.adapter.dto.LLMRequest;
import com.codingas.gateway.adapter.dto.LLMResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Anthropic Claude 适配器
 *
 * <p>实现 Anthropic 消息 API 格式的适配器。</p>
 * <p>非流式请求使用 RestClient，流式请求使用 OkHttp SSE。</p>
 */
@Slf4j
public class AnthropicAdapter implements LLMProviderAdapter {

    public static final String PROVIDER_CODE = "anthropic";
    private static final String MESSAGES_URL = "/v1/messages";

    private final RestClient restClient;
    private final OkHttpClient okHttpClient;
    private final String baseUrl;
    private final String apiKey;
    private final String version;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    public AnthropicAdapter(String baseUrl, String apiKey, String version, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.version = version != null ? version : "2023-06-01";
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = new ObjectMapper();

        // RestClient for non-streaming
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader("anthropic-version", this.version)
                .build();

        // OkHttpClient for streaming
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .writeTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
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
            String response = restClient.post()
                    .uri(MESSAGES_URL)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            LLMResponse llmResponse = parseResponse(responseMap);

            log.info("Anthropic messages response: id={}, model={}, promptTokens={}, completionTokens={}",
                    llmResponse.getId(), llmResponse.getModel(),
                    llmResponse.getUsage() != null ? llmResponse.getUsage().getPromptTokens() : null,
                    llmResponse.getUsage() != null ? llmResponse.getUsage().getCompletionTokens() : null);

            return llmResponse;
        } catch (Exception e) {
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
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));

            Request httpRequest = new Request.Builder()
                    .url(baseUrl + MESSAGES_URL)
                    .post(body)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .header("anthropic-version", version)
                    .build();

            EventSources.createFactory(okHttpClient)
                    .newEventSource(httpRequest, new EventSourceListener() {
                        @Override
                        public void onEvent(EventSource eventSource, String id, String type, String data) {
                            if (data != null && !data.isEmpty() && !"[DONE]".equals(data)) {
                                log.debug("Anthropic stream chunk: {}", data);
                                callback.onChunk(data);
                            }
                        }

                        @Override
                        public void onClosed(EventSource eventSource) {
                            log.info("Anthropic stream completed");
                            callback.onComplete();
                        }

                        @Override
                        public void onFailure(EventSource eventSource, Throwable t, Response response) {
                            log.error("Anthropic stream error: {}", t != null ? t.getMessage() : "unknown", t);
                            callback.onError(t != null ? t : new java.io.IOException("Stream request failed"));
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

    @Override
    public boolean checkConnection() {
        try {
            // 使用轻量级请求验证连接
            Map<String, Object> body = new HashMap<>();
            body.put("model", "claude-haiku-3-5-20250514"); // 使用最小的模型
            body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
            body.put("max_tokens", 1);

            String jsonBody = objectMapper.writeValueAsString(body);
            RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(baseUrl + MESSAGES_URL)
                    .post(requestBody)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .header("anthropic-version", version)
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                // Anthropic 可能返回 400 (bad request) 但表示连接正常
                return response.code() >= 200 && response.code() < 500;
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
    private LLMResponse parseResponse(Map<String, Object> response) throws com.fasterxml.jackson.core.JsonProcessingException {
        return LLMResponse.builder()
                .providerCode(PROVIDER_CODE)
                .id((String) response.get("id"))
                .model((String) response.get("model"))
                .content(parseContent(response))
                .usage(parseUsage(response))
                .finishReason((String) response.get("stop_reason"))
                .stream(false)
                .build();
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