package com.codingas.gateway.adapter.openai;

import com.codingas.gateway.adapter.LLMProviderAdapter;
import com.codingas.gateway.adapter.StreamCallback;
import com.codingas.gateway.adapter.common.ProviderCapabilities;
import com.codingas.gateway.adapter.common.ProviderType;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
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

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容接口适配器
 *
 * <p>实现 OpenAI API 兼容端点的适配器。</p>
 * <p>非流式请求使用 RestClient，流式请求使用 OkHttp SSE。</p>
 */
@Slf4j
public class OpenAIAdapter implements LLMProviderAdapter {

    public static final String PROVIDER_CODE = "openai";
    private static final String CHAT_COMPLETIONS_URL = "/v1/chat/completions";

    private final RestClient restClient;
    private final OkHttpClient okHttpClient;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    public OpenAIAdapter(String baseUrl, String apiKey, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = new ObjectMapper();

        // RestClient for non-streaming
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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
        return ProviderType.OPENAI;
    }

    @Override
    public LLMResponse chat(LLMRequest request) {
        log.info("OpenAI chat request: model={}, stream=false", request.getModel());

        Map<String, Object> requestBody = buildRequestBody(request);

        try {
            String response = restClient.post()
                    .uri(CHAT_COMPLETIONS_URL)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            LLMResponse llmResponse = parseResponse(responseMap);

            log.info("OpenAI chat response: id={}, model={}, promptTokens={}, completionTokens={}",
                    llmResponse.getId(), llmResponse.getModel(),
                    llmResponse.getUsage() != null ? llmResponse.getUsage().getPromptTokens() : null,
                    llmResponse.getUsage() != null ? llmResponse.getUsage().getCompletionTokens() : null);

            return llmResponse;
        } catch (Exception e) {
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
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));

            Request httpRequest = new Request.Builder()
                    .url(baseUrl + CHAT_COMPLETIONS_URL)
                    .post(body)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .build();

            EventSources.createFactory(okHttpClient)
                    .newEventSource(httpRequest, new EventSourceListener() {
                        @Override
                        public void onEvent(EventSource eventSource, String id, String type, String data) {
                            if (data != null && !data.isEmpty() && !"[DONE]".equals(data)) {
                                log.debug("OpenAI stream chunk: {}", data);
                                callback.onChunk(data);
                            }
                        }

                        @Override
                        public void onClosed(EventSource eventSource) {
                            log.info("OpenAI stream completed");
                            callback.onComplete();
                        }

                        @Override
                        public void onFailure(EventSource eventSource, Throwable t, Response response) {
                            log.error("OpenAI stream error: {}", t != null ? t.getMessage() : "unknown", t);
                            callback.onError(t != null ? t : new IOException("Stream request failed"));
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

    @Override
    public boolean checkConnection() {
        try {
            // 使用 models 端点进行轻量级连接检查
            Request request = new Request.Builder()
                    .url(baseUrl + "/v1/models")
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
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
    private LLMResponse parseResponse(Map<String, Object> response) {
        return LLMResponse.builder()
                .providerCode(PROVIDER_CODE)
                .id((String) response.get("id"))
                .model((String) response.get("model"))
                .created(response.get("created") != null ? ((Number) response.get("created")).longValue() : null)
                .content(parseContent(response))
                .usage(parseUsage(response))
                .finishReason((String) ((List<Map<String, Object>>) response.get("choices")).get(0).get("finish_reason"))
                .stream(false)
                .build();
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
}
