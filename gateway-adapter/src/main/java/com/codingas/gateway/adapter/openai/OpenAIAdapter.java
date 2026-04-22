package com.codingas.gateway.adapter.openai;

import com.codingas.gateway.adapter.LLMProviderAdapter;
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
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
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
        if (request.getStream() != null) {
            body.put("stream", request.getStream());
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
