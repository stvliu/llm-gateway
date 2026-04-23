package com.codingas.gateway.adapter.anthropic;

import com.codingas.gateway.adapter.LLMProviderAdapter;
import com.codingas.gateway.adapter.common.ProviderCapabilities;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Anthropic Claude 适配器
 *
 * <p>实现 Anthropic 消息 API 格式的适配器。</p>
 */
@Slf4j
public class AnthropicAdapter implements LLMProviderAdapter {

    public static final String PROVIDER_CODE = "anthropic";

    private final WebClient webClient;
    private final String baseUrl;
    private final String apiKey;
    private final String version;
    private final int timeoutSeconds;

    public AnthropicAdapter(String baseUrl, String apiKey, String version, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.version = version != null ? version : "2023-06-01";
        this.timeoutSeconds = timeoutSeconds;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("anthropic-version", this.version)
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
    public Mono<LLMResponse> chat(LLMRequest request) {
        // Anthropic 不支持 OpenAI 格式的 chat 接口
        return Mono.error(new UnsupportedOperationException(
                "Anthropic adapter does not support OpenAI chat format. Use messages() instead."));
    }

    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        // Anthropic 支持流式，但需要使用 messages API
        request.setStream(true);
        Map<String, Object> requestBody = buildMessagesRequestBody(request);

        return webClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isEmpty() && line.startsWith("data: "))
                .filter(line -> !line.equals("data: [DONE]"))
                .map(this::parseStreamResponse)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnError(e -> log.error("Anthropic stream error: {}", e.getMessage()));
    }

    @Override
    public Mono<LLMResponse> messages(LLMRequest request) {
        log.debug("Anthropic messages request: model={}", request.getModel());

        Map<String, Object> requestBody = buildMessagesRequestBody(request);

        return webClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::parseResponse)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnError(e -> log.error("Anthropic messages error: {}", e.getMessage()));
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
            body.put("max_tokens", 1024); // Anthropic 要求必填
        }

        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }

        if (request.getSystemPrompt() != null) {
            body.put("system", request.getSystemPrompt());
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
                .content(parseContent(response))
                .usage(parseUsage(response))
                .finishReason((String) response.get("stop_reason"))
                .stream(false)
                .build();
    }

    @SuppressWarnings("unchecked")
    private LLMResponse.Content parseContent(Map<String, Object> response) {
        var content = (java.util.List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            return null;
        }

        var firstBlock = content.get(0);
        String text = (String) firstBlock.get("text");

        return LLMResponse.Content.builder()
                .role("assistant")
                .text(text)
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
                .totalTokens(null) // Anthropic 不直接返回 total
                .build();
    }

    private LLMResponse parseStreamResponse(String line) {
        // 解析 SSE 格式: data: {"type":"content_block_delta","index":0,...}
        String json = line.substring(6); // Remove "data: "
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
            return parseStreamChunk(data);
        } catch (Exception e) {
            log.warn("Failed to parse Anthropic stream response: {}", line);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private LLMResponse parseStreamChunk(Map<String, Object> data) {
        String type = (String) data.get("type");

        if ("content_block_delta".equals(type)) {
            var delta = (Map<String, Object>) data.get("delta");
            String text = delta != null ? (String) delta.get("text") : null;

            return LLMResponse.builder()
                    .providerCode(PROVIDER_CODE)
                    .model((String) data.get("model"))
                    .content(text != null ? LLMResponse.Content.builder().text(text).build() : null)
                    .stream(true)
                    .build();
        }

        // 忽略其他事件类型
        return null;
    }
}