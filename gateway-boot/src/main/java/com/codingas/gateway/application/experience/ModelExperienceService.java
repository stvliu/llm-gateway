package com.codingas.gateway.application.experience;

import com.codingas.gateway.application.experience.dto.ExperienceChatEvent;
import com.codingas.gateway.application.experience.dto.ExperienceChatRequest;
import com.codingas.gateway.application.experience.dto.ExperienceModelResponse;
import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayRegistry;
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
 *   <li>使用已保存配置：传入 providerId，可选 apiKeyId</li>
 *   <li>临时配置：传入 protocolName(协议名称), apiKey, baseUrl(可选)</li>
 * </ol>
 */
@Slf4j
@Service
public class ModelExperienceService {

    private final ProtocolGatewayRegistry protocolGatewayRegistry;
    private final ProviderGateway providerGateway;
    private final ProviderApiKeyGateway providerApiKeyGateway;
    private final ModelGateway modelGateway;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModelExperienceService(ProtocolGatewayRegistry protocolGatewayRegistry,
                                  ProviderGateway providerGateway,
                                  ProviderApiKeyGateway providerApiKeyGateway,
                                  ModelGateway modelGateway) {
        this.protocolGatewayRegistry = protocolGatewayRegistry;
        this.providerGateway = providerGateway;
        this.providerApiKeyGateway = providerApiKeyGateway;
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
                    .data(new ExperienceChatEvent.ErrorData("无效的请求：使用已保存配置时需提供 providerId，临时配置时需提供 protocolName 和 apiKey")));
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
        // 解析配置
        ResolvedConfig config = resolveConfig(request);
        log.info("Experience chat: protocolName={}, model={}", config.protocolName, request.getModel());

        // 查找协议网关
        ProtocolGateway protocolGateway = protocolGatewayRegistry.getGateway(config.protocolName)
            .orElseThrow(() -> new IllegalArgumentException("不支持的协议类型: " + config.protocolName));

        // 解析 baseUrl：优先使用请求传入，否则使用协议默认
        String baseUrl = config.baseUrl != null ? config.baseUrl : protocolGateway.getDefaultBaseUrl();

        // 转换消息格式
        List<LLMRequest.Message> messages = new ArrayList<>();
        for (Map<String, String> msg : request.getMessages()) {
            messages.add(LLMRequest.Message.builder()
                .role(msg.get("role"))
                .content(msg.get("content"))
                .build());
        }

        // 构建 LLM 请求
        LLMRequest llmRequest = LLMRequest.builder()
            .model(request.getModel())
            .messages(messages)
            .maxTokens(request.getMaxTokens())
            .temperature(request.getTemperature())
            .stream(true)
            .build();

        // Token 统计
        final int[] promptTokens = {0};
        final int[] completionTokens = {0};

        // 创建流式回调
        StreamCallback callback = new StreamCallback() {
            private final StringBuilder contentBuilder = new StringBuilder();

            @Override
            public void onChunk(String chunk) {
                try {
                    String content = extractContent(chunk);
                    if (content != null && !content.isEmpty()) {
                        contentBuilder.append(content);
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
                    promptTokens[0] = estimatePromptTokens(messages);

                    emitter.send(SseEmitter.event()
                        .name("USAGE")
                        .data(new ExperienceChatEvent.UsageData(promptTokens[0], completionTokens[0])));

                    emitter.send(SseEmitter.event().name("DONE"));
                    emitter.complete();

                    log.info("Experience chat completed: promptTokens={}, completionTokens={}",
                        promptTokens[0], completionTokens[0]);
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
                    log.warn("Failed to send error event: {}", ex.getMessage());
                    emitter.completeWithError(t);
                }
            }
        };

        // 通过协议网关调用流式聊天
        protocolGateway.chatStream(llmRequest, baseUrl, config.apiKey, 60, callback);
    }

    /**
     * 解析配置
     *
     * <p>支持两种模式：</p>
     * <ol>
     *   <li>使用已保存配置：从数据库读取 Provider 和 API Key</li>
     *   <li>临时配置：直接使用请求中的配置</li>
     * </ol>
     */
    private ResolvedConfig resolveConfig(ExperienceChatRequest request) {
        if (request.useSavedConfig()) {
            // 从数据库读取配置
            Provider provider = providerGateway.findById(request.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("供应商不存在: " + request.getProviderId()));

            ProviderApiKey apiKey;
            if (request.getApiKeyId() != null) {
                apiKey = providerApiKeyGateway.findById(request.getApiKeyId())
                    .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + request.getApiKeyId()));
                if (!apiKey.getProviderId().equals(request.getProviderId())) {
                    throw new IllegalArgumentException("API Key 不属于该供应商");
                }
            } else {
                apiKey = providerApiKeyGateway.findDefaultKeyByProviderId(request.getProviderId())
                    .orElseThrow(() -> new IllegalArgumentException("供应商没有默认 API Key，请指定要使用的 Key"));
            }

            if (!apiKey.isAvailable()) {
                throw new IllegalArgumentException("API Key 不可用: " + apiKey.getState());
            }

            // 使用已保存配置：protocolName + providerId + apiKeyId
            return new ResolvedConfig(
                request.getProtocolName(),
                null,
                apiKey.getApiKey()
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
    private int estimatePromptTokens(List<LLMRequest.Message> messages) {
        int total = 0;
        for (LLMRequest.Message msg : messages) {
            String content = msg.getContent();
            if (content != null) {
                total += estimateTokens(content);
            }
        }
        return Math.max(1, total);
    }
}
