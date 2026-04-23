package com.codingas.gateway.adapter.openai;

import com.codingas.gateway.adapter.LLMProviderAdapter;
import com.codingas.gateway.adapter.common.ProviderCapabilities;
import com.codingas.gateway.adapter.common.ProviderErrorType;
import com.codingas.gateway.adapter.common.ProviderException;
import com.codingas.gateway.adapter.common.ProviderType;
import com.codingas.gateway.adapter.dto.LLMRequest;
import com.codingas.gateway.adapter.dto.LLMResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * OpenAI 兼容接口适配器
 *
 * <p>实现 OpenAI API 兼容端点的适配器。</p>
 */
@Slf4j
public class OpenAIAdapter implements LLMProviderAdapter {

    public static final String PROVIDER_CODE = "openai";

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;

    public OpenAIAdapter(String baseUrl, String apiKey, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
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
    public Mono<LLMResponse> chat(LLMRequest request) {
        log.debug("OpenAI chat request: model={}", request.getModel());

        Map<String, Object> requestBody = buildRequestBody(request);

        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::parseResponse)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnError(e -> log.error("OpenAI chat error: {}", e.getMessage()));
    }

    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        log.debug("OpenAI chat stream request: model={}", request.getModel());

        request.setStream(true);
        Map<String, Object> requestBody = buildRequestBody(request);

        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isEmpty() && line.startsWith("data: "))
                .filter(line -> !line.equals("data: [DONE]"))
                .map(this::parseStreamResponse)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnError(e -> log.error("OpenAI stream error: {}", e.getMessage()));
    }

    @Override
    public Mono<LLMResponse> messages(LLMRequest request) {
        // OpenAI 不支持 Anthropic 格式的 messages API
        return Mono.error(new UnsupportedOperationException(
                "OpenAI adapter does not support Anthropic messages format. Use chat() instead."));
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public boolean isHealthy() {
        try {
            // 简单的健康检查：验证 API Key 是否可用
            return isAvailable();
        } catch (Exception e) {
            log.warn("OpenAI health check failed: {}", e.getMessage());
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
        Map<String, Object> body = new java.util.HashMap<>();
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
                .finishReason((String) response.get("finish_reason"))
                .stream(false)
                .build();
    }

    @SuppressWarnings("unchecked")
    private LLMResponse.Content parseContent(Map<String, Object> response) {
        var choices = (java.util.List<Map<String, Object>>) response.get("choices");
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
                .build();
    }

    @SuppressWarnings("unchecked")
    private LLMResponse.Usage parseUsage(Map<String, Object> response) {
        var usage = (java.util.Map<String, Object>) response.get("usage");
        if (usage == null) {
            return null;
        }
        return LLMResponse.Usage.builder()
                .promptTokens(usage.get("prompt_tokens") != null ? ((Number) usage.get("prompt_tokens")).intValue() : null)
                .completionTokens(usage.get("completion_tokens") != null ? ((Number) usage.get("completion_tokens")).intValue() : null)
                .totalTokens(usage.get("total_tokens") != null ? ((Number) usage.get("total_tokens")).intValue() : null)
                .build();
    }

    private LLMResponse parseStreamResponse(String line) {
        // 解析 SSE 格式: data: {"choices":[{"delta":{"content":"..."}}]}
        String json = line.substring(6); // Remove "data: "
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
            return parseStreamChunk(data);
        } catch (Exception e) {
            log.warn("Failed to parse stream response: {}", line);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private LLMResponse parseStreamChunk(Map<String, Object> data) {
        var choices = (java.util.List<Map<String, Object>>) data.get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        var choice = choices.get(0);
        var delta = (Map<String, Object>) choice.get("delta");

        String content = delta != null ? (String) delta.get("content") : null;
        String finishReason = (String) choice.get("finish_reason");

        return LLMResponse.builder()
                .providerCode(PROVIDER_CODE)
                .model((String) data.get("model"))
                .id((String) data.get("id"))
                .content(content != null ? LLMResponse.Content.builder().text(content).build() : null)
                .finishReason(finishReason)
                .stream(true)
                .build();
    }
}
