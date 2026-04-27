package com.codingas.gateway.infrastructure.adapter.openai;

import com.codingas.gateway.infrastructure.adapter.LLMProviderAdapter;
import com.codingas.gateway.infrastructure.adapter.StreamCallback;
import com.codingas.gateway.common.ProviderCapabilities;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenAI 兼容接口适配器
 *
 * <p>实现 OpenAI API 兼容端点的适配器。</p>
 * <p>使用 Spring WebFlux WebClient 进行 HTTP 通信。</p>
 */
@Slf4j
public class OpenAIAdapter implements LLMProviderAdapter {

    public static final String PROVIDER_CODE = "openai";
    private static final String CHAT_COMPLETIONS_URL = "/v1/chat/completions";

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    public OpenAIAdapter(WebClient webClient, String baseUrl, String apiKey, int timeoutSeconds) {
        this.webClient = webClient;
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
    public Mono<LLMResponse> chat(LLMRequest request) {
        log.info("OpenAI chat request: model={}, stream=false", request.getModel());

        Map<String, Object> requestBody = buildRequestBody(request);

        return webClient.post()
                .uri(baseUrl + CHAT_COMPLETIONS_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .doOnSuccess(llmResponse -> log.info("OpenAI chat response: id={}, model={}",
                        llmResponse.getId(), llmResponse.getModel()))
                .doOnError(e -> log.error("OpenAI chat error: model={}, error={}",
                        request.getModel(), e.getMessage(), e))
                .onErrorMap(e -> new RuntimeException("OpenAI chat request failed", e));
    }

    @Override
    public Mono<Void> chatStream(LLMRequest request, StreamCallback callback) {
        log.info("OpenAI chat stream request: model={}, stream=true", request.getModel());

        request.setStream(true);
        Map<String, Object> requestBody = buildRequestBody(request);

        return webClient.post()
                .uri(baseUrl + CHAT_COMPLETIONS_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .exchangeToFlux(response -> response.bodyToFlux(String.class))
                .filter(data -> !data.isEmpty() && !"[DONE]".equals(data))
                .doOnNext(data -> {
                    log.debug("OpenAI stream chunk: {}", data);
                    callback.onChunk(data);
                })
                .doOnComplete(() -> {
                    log.info("OpenAI stream completed");
                    callback.onComplete();
                })
                .doOnError(error -> {
                    log.error("OpenAI stream error: {}", error.getMessage(), error);
                    callback.onError(error);
                })
                .then()
                .onErrorMap(e -> {
                    if (e instanceof RuntimeException) {
                        return (RuntimeException) e;
                    }
                    return new RuntimeException("OpenAI stream request failed", e);
                });
    }

    @Override
    public Mono<LLMResponse> messages(LLMRequest request) {
        return Mono.error(new UnsupportedOperationException(
                "OpenAI adapter does not support Anthropic messages format. Use chat() instead."));
    }

    @Override
    public Mono<Void> messagesStream(LLMRequest request, StreamCallback callback) {
        return Mono.error(new UnsupportedOperationException(
                "OpenAI adapter does not support Anthropic messages format. Use chatStream() instead."));
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
        // 使用 models 端点进行轻量级连接检查
        return webClient.get()
                .uri(baseUrl + "/v1/models")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> true)
                .onErrorReturn(false)
                .block();
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
                    .providerCode(PROVIDER_CODE)
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
}
