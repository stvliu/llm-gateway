package com.codingas.gateway.application.experience;

import com.codingas.gateway.application.experience.dto.ExperienceChatEvent;
import com.codingas.gateway.application.experience.dto.ExperienceChatRequest;
import com.codingas.gateway.application.experience.dto.ExperienceModelResponse;
import com.codingas.gateway.domain.proxy.protocol.OpenAIChatRequest;
import com.codingas.gateway.domain.proxy.protocol.AnthropicMessagesRequest;
import com.codingas.gateway.domain.proxy.protocol.ProtocolRequest;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.product.entity.ProductApiKey;
import com.codingas.gateway.domain.product.gateway.ProductApiKeyGateway;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayFactory;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 模型体验服务
 *
 * <p>提供流式聊天体验功能，支持两种模式：</p>
 * <ol>
 *   <li>使用已保存配置：传入 productId，可选 apiKeyId</li>
 *   <li>临时配置：传入 protocolName(协议名称), apiKey, baseUrl(可选)</li>
 * </ol>
 *
 * <p>注意：已迁移到新架构，使用 ProductApiKey 替代 ProviderApiKey。</p>
 */
@Slf4j
@Service
public class ModelExperienceService {

    private final ProtocolGatewayFactory protocolGatewayFactory;
    private final ProviderGateway providerGateway;
    private final ProductGateway productGateway;
    private final ProductApiKeyGateway productApiKeyGateway;
    private final ModelGateway modelGateway;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelExperienceService(ProtocolGatewayFactory protocolGatewayFactory,
                                  ProviderGateway providerGateway,
                                  ProductGateway productGateway,
                                  ProductApiKeyGateway productApiKeyGateway,
                                  ModelGateway modelGateway) {
        this.protocolGatewayFactory = protocolGatewayFactory;
        this.providerGateway = providerGateway;
        this.productGateway = productGateway;
        this.productApiKeyGateway = productApiKeyGateway;
        this.modelGateway = modelGateway;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ModelExperienceService executor...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取供应商的模型列表
     *
     * @param providerId 供应商 ID
     * @return 模型列表
     */
    public List<ExperienceModelResponse> getModelsByProviderId(Long providerId) {
        return modelGateway.findByProviderId(providerId).stream()
            .filter(Model::isAvailable)
            .map(model -> new ExperienceModelResponse(
                model.getId(),
                model.getProviderModelId(),
                model.getDisplayName() != null ? model.getDisplayName() : model.getProviderModelId()
            ))
            .toList();
    }

    /**
     * 流式聊天体验
     *
     * @param request 体验请求
     * @return SSE Emitter
     */
    public SseEmitter chatStream(ExperienceChatRequest request) {
        // 验证请求
        if (!request.isValid()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                    .name("ERROR")
                    .data(new ExperienceChatEvent.ErrorData("无效的请求：使用已保存配置时需提供 productId，临时配置时需提供 protocolName 和 apiKey")));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 创建 SSE Emitter（超时 60 秒）
        SseEmitter emitter = new SseEmitter(60_000L);

        // 异步执行聊天
        executor.execute(() -> {
            try {
                doChatStream(request, emitter);
            } catch (Exception e) {
                log.error("Experience chat error: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("ERROR")
                        .data(new ExperienceChatEvent.ErrorData(e.getMessage())));
                    emitter.complete();
                } catch (IOException ex) {
                    log.warn("Failed to send error event: {}", ex.getMessage());
                    emitter.completeWithError(e);
                }
            }
        });

        // 设置超时和错误处理
        emitter.onTimeout(() -> {
            log.warn("Experience chat timeout");
            emitter.complete();
        });

        emitter.onError(e -> {
            log.error("SSE error: {}", e.getMessage());
        });

        return emitter;
    }

    /**
     * 执行流式聊天
     */
    private void doChatStream(ExperienceChatRequest request, SseEmitter emitter) throws IOException {
        ResolvedConfig config = resolveConfig(request);
        log.info("Experience chat: protocolName={}, model={}", config.protocolName, request.getModel());

        String baseUrl = config.baseUrl != null ? config.baseUrl : "";
        ProtocolGateway protocolGateway = protocolGatewayFactory.create(config.protocolName, baseUrl, config.apiKey, 60);

        ProtocolRequest protocolRequest = buildProtocolRequest(config.protocolName, request);

        final int[] completionTokens = {0};

        StreamCallback callback = new StreamCallback() {
            @Override
            public void onChunk(String chunk) {
                try {
                    String content = extractContent(chunk);
                    if (content != null && !content.isEmpty()) {
                        emitter.send(SseEmitter.event()
                            .name("CONTENT")
                            .data(new ExperienceChatEvent.ContentData(content)));
                        completionTokens[0] += estimateTokens(content);
                    }
                } catch (Exception e) {
                    log.error("Error sending chunk: {}", e.getMessage());
                }
            }

            @Override
            public void onComplete() {
                try {
                    emitter.send(SseEmitter.event()
                        .name("USAGE")
                        .data(new ExperienceChatEvent.UsageData(0, completionTokens[0])));
                    emitter.send(SseEmitter.event().name("DONE"));
                    emitter.complete();
                } catch (Exception e) {
                    log.error("Error completing stream: {}", e.getMessage());
                }
            }

            @Override
            public void onError(Throwable t) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("ERROR")
                        .data(new ExperienceChatEvent.ErrorData(t.getMessage())));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(t);
                }
            }
        };

        protocolGateway.chatStream(protocolRequest, callback);
    }

    /**
     * 根据协议类型构建协议请求 DTO
     */
    private ProtocolRequest buildProtocolRequest(String protocolName, ExperienceChatRequest request) {
        List<Map<String, String>> rawMessages = request.getMessages();

        if ("anthropic".equals(protocolName)) {
            List<AnthropicMessagesRequest.Message> messages = rawMessages.stream()
                .map(msg -> AnthropicMessagesRequest.Message.builder()
                    .role(msg.get("role"))
                    .content(msg.get("content"))
                    .build())
                .toList();

            return AnthropicMessagesRequest.builder()
                .model(request.getModel())
                .messages(messages)
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 1024)
                .temperature(request.getTemperature())
                .stream(true)
                .build();
        } else {
            List<OpenAIChatRequest.Message> messages = rawMessages.stream()
                .map(msg -> OpenAIChatRequest.Message.builder()
                    .role(msg.get("role"))
                    .content(msg.get("content"))
                    .build())
                .toList();

            return OpenAIChatRequest.builder()
                .model(request.getModel())
                .messages(messages)
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .stream(true)
                .build();
        }
    }

    /**
     * 解析配置
     *
     * <p>支持两种模式：</p>
     * <ol>
     *   <li>使用已保存配置：从数据库读取 Product 和 ProductApiKey</li>
     *   <li>临时配置：直接使用请求中的配置</li>
     * </ol>
     */
    private ResolvedConfig resolveConfig(ExperienceChatRequest request) {
        if (request.useSavedConfig()) {
            // 从数据库读取配置（新架构：使用 productId）
            var product = productGateway.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("产品不存在: " + request.getProductId()));

            ProductApiKey apiKey;
            if (request.getApiKeyId() != null) {
                apiKey = productApiKeyGateway.findById(request.getApiKeyId())
                    .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + request.getApiKeyId()));
                if (!apiKey.getProductId().equals(request.getProductId())) {
                    throw new IllegalArgumentException("API Key 不属于该产品");
                }
            } else {
                apiKey = productApiKeyGateway.findDefaultByProductId(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("产品没有默认 API Key，请指定要使用的 Key"));
            }

            if (!apiKey.isAvailable()) {
                throw new IllegalArgumentException("API Key 不可用");
            }

            // 从产品端点获取 baseUrl
            Map<String, String> endpoints = product.getEndpoints();
            String baseUrl = endpoints != null ? endpoints.get(request.getProtocolName()) : null;

            // 使用已保存配置
            return new ResolvedConfig(
                request.getProtocolName(),
                baseUrl,
                apiKey.getApiKeyPlain()
            );
        } else {
            // 使用临时配置：baseUrl 可选，由协议网关提供默认值
            return new ResolvedConfig(
                request.getProtocolName(),
                request.getBaseUrl(),
                request.getApiKey()
            );
        }
    }

    /**
     * 解析后的配置
     */
    private record ResolvedConfig(
        String protocolName,
        String baseUrl,
        String apiKey
    ) {}

    /**
     * 从 SSE 数据中提取内容
     *
     * <p>支持多种格式：</p>
     * <ul>
     *   <li>OpenAI 标准格式：delta.content</li>
     *   <li>火山引擎推理格式：delta.reasoning_content（当 content 为空时使用）</li>
     * </ul>
     */
    private String extractContent(String chunk) {
        try {
            JsonNode node = objectMapper.readTree(chunk);
            JsonNode choices = node.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).path("delta");

                if (delta.has("content") && !delta.get("content").isNull()) {
                    String content = delta.get("content").asText();
                    if (!content.isEmpty()) {
                        return content;
                    }
                }

                if (delta.has("reasoning_content") && !delta.get("reasoning_content").isNull()) {
                    String reasoningContent = delta.get("reasoning_content").asText();
                    if (!reasoningContent.isEmpty()) {
                        return reasoningContent;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse SSE chunk: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 估算 Token 数量（简单估算：字符数 / 4）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return Math.max(1, text.length() / 4);
    }

    /**
     * 估算输入 Token 数量
     */
    private int estimatePromptTokens(List<Map<String, String>> messages) {
        int total = 0;
        for (Map<String, String> msg : messages) {
            String content = msg.get("content");
            if (content != null) {
                total += estimateTokens(content);
            }
        }
        return Math.max(1, total);
    }
}