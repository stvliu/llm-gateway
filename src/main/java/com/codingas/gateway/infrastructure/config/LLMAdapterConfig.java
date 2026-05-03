package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.infrastructure.proxy.gateway.rpc.AdapterRegistry;
import com.codingas.gateway.infrastructure.proxy.gateway.rpc.OpenAIAdapter;
import com.codingas.gateway.infrastructure.proxy.gateway.rpc.VolcengineAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * LLM 适配器配置
 *
 * <p>负责初始化所有 LLM Provider 适配器并注册到 AdapterRegistry。</p>
 *
 * <p>支持的配置项：</p>
 * <ul>
 *   <li>gateway.llm.timeout-seconds - 默认超时时间</li>
 *   <li>gateway.llm.openai.base-url - OpenAI Base URL</li>
 *   <li>gateway.llm.openai.api-key - OpenAI API Key</li>
 *   <li>gateway.llm.openai.enabled - 是否启用 OpenAI</li>
 *   <li>gateway.llm.volcengine.base-url - 火山引擎 Base URL</li>
 *   <li>gateway.llm.volcengine.api-key - 火山引擎 API Key</li>
 *   <li>gateway.llm.volcengine.enabled - 是否启用火山引擎</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LLMAdapterConfig {

    @Value("${gateway.llm.timeout-seconds:30}")
    private int defaultTimeoutSeconds;

    // OpenAI 配置
    @Value("${gateway.llm.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;

    @Value("${gateway.llm.openai.api-key:}")
    private String openaiApiKey;

    @Value("${gateway.llm.openai.enabled:false}")
    private boolean openaiEnabled;

    // 火山引擎配置
    @Value("${gateway.llm.volcengine.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String volcengineBaseUrl;

    @Value("${gateway.llm.volcengine.api-key:}")
    private String volcengineApiKey;

    @Value("${gateway.llm.volcengine.enabled:false}")
    private boolean volcengineEnabled;

    /**
     * 创建共享的 OkHttp 客户端
     *
     * <p>配置连接池、超时时间等参数。</p>
     *
     * @return OkHttpClient 实例
     */
    @Bean
    public OkHttpClient llmHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * 创建 OpenAI 适配器
     *
     * @param llmHttpClient 共享的 HTTP 客户端
     * @return OpenAI 适配器实例，如果未配置则返回 null
     */
    @Bean
    public OpenAIAdapter openaiAdapter(OkHttpClient llmHttpClient) {
        if (!openaiEnabled || openaiApiKey == null || openaiApiKey.isBlank()) {
            log.info("OpenAI adapter is disabled or not configured");
            return null;
        }

        log.info("Initializing OpenAI adapter with baseUrl: {}", openaiBaseUrl);
        return new OpenAIAdapter(llmHttpClient, openaiBaseUrl, openaiApiKey, defaultTimeoutSeconds);
    }

    /**
     * 创建火山引擎适配器
     *
     * @param llmHttpClient 共享的 HTTP 客户端
     * @return 火山引擎适配器实例，如果未配置则返回 null
     */
    @Bean
    public VolcengineAdapter volcengineAdapter(OkHttpClient llmHttpClient) {
        if (!volcengineEnabled || volcengineApiKey == null || volcengineApiKey.isBlank()) {
            log.info("Volcengine adapter is disabled or not configured");
            return null;
        }

        log.info("Initializing Volcengine adapter with baseUrl: {}", volcengineBaseUrl);
        return new VolcengineAdapter(llmHttpClient, volcengineBaseUrl, volcengineApiKey, defaultTimeoutSeconds);
    }

    /**
     * 创建适配器注册表并注册所有适配器
     *
     * @param openaiAdapter     OpenAI 适配器
     * @param volcengineAdapter 火山引擎适配器
     * @return 适配器注册表实例
     */
    @Bean
    public AdapterRegistry adapterRegistry(
            @Autowired(required = false) OpenAIAdapter openaiAdapter,
            @Autowired(required = false) VolcengineAdapter volcengineAdapter) {

        AdapterRegistry registry = new AdapterRegistry();
        int count = 0;

        if (openaiAdapter != null) {
            registry.register(openaiAdapter);
            count++;
            log.info("Registered OpenAI adapter");
        }

        if (volcengineAdapter != null) {
            registry.register(volcengineAdapter);
            count++;
            log.info("Registered Volcengine adapter");
        }

        log.info("Adapter registry initialized with {} adapters", count);

        return registry;
    }
}
