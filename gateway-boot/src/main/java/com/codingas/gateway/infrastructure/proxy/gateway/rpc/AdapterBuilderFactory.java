package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.domain.model.enums.ProviderType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Adapter 构建器工厂
 *
 * <p>用于创建临时 Adapter 实例，支持测试未保存的 API Key。</p>
 */
@Slf4j
@Component
public class AdapterBuilderFactory {

    /** 默认超时时间（秒） */
    private static final int DEFAULT_TIMEOUT = 30;

    /** 共享的 HTTP 客户端 */
    private final OkHttpClient sharedHttpClient;

    public AdapterBuilderFactory() {
        this.sharedHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build();
    }

    /**
     * 创建临时 Adapter 实例
     *
     * @param providerType 供应商类型
     * @param baseUrl Base URL（可选，使用默认值）
     * @param apiKey API Key
     * @return Adapter 实例
     */
    public LLMAdapter createAdapter(ProviderType providerType, String baseUrl, String apiKey) {
        return createAdapter(providerType, baseUrl, apiKey, DEFAULT_TIMEOUT);
    }

    /**
     * 创建临时 Adapter 实例
     *
     * @param providerType 供应商类型
     * @param baseUrl Base URL（可选，使用默认值）
     * @param apiKey API Key
     * @param timeoutSeconds 超时时间（秒）
     * @return Adapter 实例
     */
    public LLMAdapter createAdapter(ProviderType providerType, String baseUrl, String apiKey, int timeoutSeconds) {
        log.debug("Creating temporary adapter for provider: {}", providerType);

        return switch (providerType) {
            case OPENAI -> new OpenAIAdapter(sharedHttpClient, baseUrl, apiKey, timeoutSeconds);
            case ANTHROPIC -> new AnthropicAdapter(sharedHttpClient, baseUrl, apiKey, null, timeoutSeconds);
            case VOLCENGINE -> new VolcengineAdapter(sharedHttpClient, baseUrl, apiKey, timeoutSeconds);
            case DEEPSEEK -> new OpenAIAdapter(sharedHttpClient,
                baseUrl != null ? baseUrl : "https://api.deepseek.com", apiKey, timeoutSeconds);
            case MOONSHOT -> new OpenAIAdapter(sharedHttpClient,
                baseUrl != null ? baseUrl : "https://api.moonshot.cn", apiKey, timeoutSeconds);
            case ZHIPU -> new OpenAIAdapter(sharedHttpClient,
                baseUrl != null ? baseUrl : "https://open.bigmodel.cn/api/paas", apiKey, timeoutSeconds);
            case BAICHUAN -> new OpenAIAdapter(sharedHttpClient,
                baseUrl != null ? baseUrl : "https://api.baichuan-ai.com", apiKey, timeoutSeconds);
            case MINIMAX -> new OpenAIAdapter(sharedHttpClient,
                baseUrl != null ? baseUrl : "https://api.minimax.chat", apiKey, timeoutSeconds);
            case QWEN -> new OpenAIAdapter(sharedHttpClient,
                baseUrl != null ? baseUrl : "https://dashscope.aliyuncs.com/compatible-mode", apiKey, timeoutSeconds);
            case GEMINI -> new OpenAIAdapter(sharedHttpClient,
                baseUrl != null ? baseUrl : "https://generativelanguage.googleapis.com", apiKey, timeoutSeconds);
            default -> {
                log.warn("Unknown provider type: {}, using OpenAI compatible adapter", providerType);
                yield new OpenAIAdapter(sharedHttpClient, baseUrl, apiKey, timeoutSeconds);
            }
        };
    }
}
