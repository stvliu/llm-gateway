package com.codingas.gateway.application.experience;

import com.codingas.gateway.application.experience.dto.ExperienceChatEvent;
import com.codingas.gateway.application.experience.dto.ExperienceChatRequest;
import com.codingas.gateway.application.experience.dto.ExperienceModelResponse;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
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
 *   <li>使用已保存配置：传入 channelId，可选 credentialId</li>
 *   <li>临时配置：传入 protocolName(协议名称), apiKey, baseUrl(可选)</li>
 * </ol>
 */
@Slf4j
@Service
public class ModelExperienceService {

    private final UpstreamClientRegistry upstreamClientRegistry;
    private final ProviderGateway providerGateway;
    private final ChannelGateway channelGateway;
    private final ChannelModelGateway channelModelGateway;
    private final ChannelCredentialGateway channelCredentialGateway;
    private final ModelSpecGateway modelSpecGateway;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ObjectMapper objectMapper;

    public ModelExperienceService(UpstreamClientRegistry upstreamClientRegistry,
                                  ProviderGateway providerGateway,
                                  ChannelGateway channelGateway,
                                  ChannelModelGateway channelModelGateway,
                                  ChannelCredentialGateway channelCredentialGateway,
                                  ModelSpecGateway modelSpecGateway,
                                  ObjectMapper objectMapper) {
        this.upstreamClientRegistry = upstreamClientRegistry;
        this.providerGateway = providerGateway;
        this.channelGateway = channelGateway;
        this.channelModelGateway = channelModelGateway;
        this.channelCredentialGateway = channelCredentialGateway;
        this.modelSpecGateway = modelSpecGateway;
        this.objectMapper = objectMapper;
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
     * <p>通过 Channel → ChannelModel → ModelSpec 关联路径查询。</p>
     *
     * @param providerId 供应商 ID
     * @return 模型列表
     */
    public List<ExperienceModelResponse> getModelsByProviderId(Long providerId) {
        List<Long> channelIds = channelGateway.findByProviderId(providerId)
                .stream().map(Channel::getId).toList();
        if (channelIds.isEmpty()) return List.of();

        List<Long> modelSpecIds = channelIds.stream()
                .flatMap(chId -> channelModelGateway.findActiveByChannelId(chId).stream())
                .map(ChannelModel::getModelSpecId)
                .distinct()
                .toList();
        if (modelSpecIds.isEmpty()) return List.of();

        return modelSpecGateway.findByIds(modelSpecIds).stream()
            .filter(ModelSpec::isAvailable)
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
                    .data(new ExperienceChatEvent.ErrorData("无效的请求：使用已保存配置时需提供 channelId，临时配置时需提供 protocolName 和 apiKey")));
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
        UpstreamClient client = upstreamClientRegistry.getClient(config.protocolName, baseUrl, config.apiKey, 60);

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

        client.chatStream(protocolRequest, callback);
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
     *   <li>使用已保存配置：从数据库读取 Channel 和 ChannelCredential</li>
     *   <li>临时配置：直接使用请求中的配置</li>
     * </ol>
     */
    private ResolvedConfig resolveConfig(ExperienceChatRequest request) {
        if (request.useSavedConfig()) {
            // 从数据库读取配置
            var channel = channelGateway.findById(request.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("渠道不存在: " + request.getChannelId()));

            ChannelCredential credential;
            if (request.getCredentialId() != null) {
                credential = channelCredentialGateway.findById(request.getCredentialId())
                    .orElseThrow(() -> new IllegalArgumentException("凭证不存在: " + request.getCredentialId()));
                if (!credential.getChannelId().equals(request.getChannelId())) {
                    throw new IllegalArgumentException("凭证不属于该渠道");
                }
            } else {
                credential = channelCredentialGateway.findDefaultByChannelId(request.getChannelId())
                    .orElseThrow(() -> new IllegalArgumentException("渠道没有默认凭证，请指定要使用的凭证"));
            }

            if (!credential.isAvailable()) {
                throw new IllegalArgumentException("凭证不可用");
            }

            // TODO: endpointUrl 和 protocol 已下沉到 ChannelEndpoint，将在后续 Task 中通过 ChannelEndpointGateway 获取
            String protocolName = request.getProtocolName() != null ? request.getProtocolName() : "openai";

            return new ResolvedConfig(
                protocolName,
                null,
                credential.getApiKeyPlain()
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
     * 估算 Token 数量
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return Math.max(1, text.length() / 4);
    }
}