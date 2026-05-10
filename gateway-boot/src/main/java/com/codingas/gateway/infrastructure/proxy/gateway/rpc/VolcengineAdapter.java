package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.domain.model.entity.ProviderCapabilities;
import com.codingas.gateway.domain.model.enums.ProviderType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.Set;

/**
 * 火山引擎适配器
 *
 * <p>火山引擎使用 OpenAI 兼容格式，API 路径为 /v3/chat/completions。</p>
 * <p>Base URL: https://ark.cn-beijing.volces.com/api/v3</p>
 *
 * <p>关键差异：</p>
 * <ul>
 *   <li>API 路径: /v3/chat/completions (非 /v1/chat/completions)</li>
 *   <li>认证方式: Bearer Token (与 OpenAI 相同)</li>
 *   <li>模型 ID 格式: 使用火山引擎的 endpoint_id</li>
 * </ul>
 *
 * @see <a href="https://www.volcengine.com/docs/82379/1298454">火山引擎 API 文档</a>
 */
@Slf4j
public class VolcengineAdapter extends OpenAIAdapter {

    public static final String PROVIDER_CODE = "volcengine";

    /** 火山引擎默认 Base URL */
    public static final String DEFAULT_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    /** 火山引擎 API 路径 (base URL 已包含 /api/v3) */
    private static final String VOLCENGINE_CHAT_COMPLETIONS_URL = "/chat/completions";

    private final String baseUrl;
    private final String apiKey;

    /**
     * 构造函数
     *
     * @param httpClient    OkHttp 客户端
     * @param baseUrl       火山引擎 Base URL
     * @param apiKey        API Key
     * @param timeoutSeconds 超时时间（秒）
     */
    public VolcengineAdapter(OkHttpClient httpClient, String baseUrl, String apiKey, int timeoutSeconds) {
        super(httpClient, baseUrl, apiKey, timeoutSeconds);
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.apiKey = apiKey;
        log.info("VolcengineAdapter initialized with baseUrl: {}", this.baseUrl);
    }

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.VOLCENGINE;
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return new ProviderCapabilities(
                ProviderType.VOLCENGINE,
                true,   // supportsChatCompletion
                false,  // supportsMessages (Anthropic 格式)
                true,   // supportsEmbeddings
                true,   // supportsStreaming
                true,   // supportsFunctionCalling
                Set.of(
                    // 豆包系列模型
                    "doubao-pro-32k",
                    "doubao-pro-128k",
                    "doubao-lite-32k",
                    "doubao-lite-128k",
                    "doubao-seed-2-0-code-preview-260215",
                    // 混元系列
                    "hunyuan-lite",
                    "hunyuan-standard",
                    "hunyuan-pro",
                    // 其他模型
                    "skylark2-pro-4k",
                    "skylark2-pro-32k"
                )
        );
    }

    /**
     * 获取火山引擎 Chat Completions API 的完整 URL
     *
     * <p>火山引擎使用 /v3/chat/completions 路径，而非 OpenAI 的 /v1/chat/completions</p>
     *
     * @return 完整的 API URL
     */
    @Override
    protected String getChatCompletionsUrl() {
        return baseUrl + VOLCENGINE_CHAT_COMPLETIONS_URL;
    }

    /**
     * 添加火山引擎特有的请求头
     *
     * <p>火山引擎支持额外的请求头用于审计和追踪。</p>
     *
     * @param requestBuilder 请求构建器
     */
    @Override
    protected void addExtraHeaders(okhttp3.Request.Builder requestBuilder) {
        // 火山引擎支持 X-Request-Id 用于请求追踪
        requestBuilder.header("X-Request-Id", java.util.UUID.randomUUID().toString());
    }

    /**
     * 火山引擎健康检查
     *
     * <p>火山引擎不支持 /v1/models 端点，直接返回 isAvailable()</p>
     *
     * @return 是否可用
     */
    @Override
    public boolean checkConnection() {
        // 火山引擎不支持 models 端点，直接返回 isAvailable
        return isAvailable();
    }
}
