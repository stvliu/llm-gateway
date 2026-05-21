package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult.LevelResult;
import com.codingas.gateway.common.util.JsonUtils;
import com.codingas.gateway.domain.model.entity.ProviderCapabilities;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 火山引擎适配器
 *
 * <p>火山引擎使用 OpenAI 兼容格式，API 路径为 /chat/completions（baseUrl 已包含 /api/v3）。</p>
 * <p>Base URL: https://ark.cn-beijing.volces.com/api/v3</p>
 *
 * <p>关键差异：</p>
 * <ul>
 *   <li>API 路径: /chat/completions (baseUrl 已包含 /api/v3)</li>
 *   <li>认证方式: Bearer Token (与 OpenAI 相同)</li>
 *   <li>模型 ID 格式: 使用火山引擎的 endpoint_id（必须用户提供）</li>
 * </ul>
 *
 * @see <a href="https://www.volcengine.com/docs/82379/1298454">火山引擎 API 文档</a>
 */
@Slf4j
public class VolcengineAdapter extends OpenAIAdapter {

    public static final String PROVIDER_CODE = "volcengine";

    /** 火山引擎默认 Base URL */
    public static final String VOLCENGINE_DEFAULT_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    /** 火山引擎 API 路径 (base URL 已包含 /api/v3) */
    private static final String VOLCENGINE_CHAT_PATH = "/chat/completions";

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
        this.baseUrl = baseUrl != null ? baseUrl : VOLCENGINE_DEFAULT_BASE_URL;
        this.apiKey = apiKey;
        log.info("VolcengineAdapter initialized with baseUrl: {}", this.baseUrl);
    }

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public String getProviderName() {
        return "volcengine";
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return new ProviderCapabilities(
                true,   // supportsChatCompletion
                false,  // supportsMessages (Anthropic 格式)
                true,   // supportsEmbeddings
                true,   // supportsStreaming
                true,   // supportsFunctionCalling
                Set.of(
                    // 豆包系列模型（实际使用 endpoint_id）
                    "doubao-pro-32k",
                    "doubao-pro-128k",
                    "doubao-lite-32k",
                    "doubao-lite-128k",
                    "doubao-1-5-pro-32k",
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

    // ==================== 连通性测试 ====================

    @Override
    public ConnectivityTestResult testConnectivity(String testApiKey, String testBaseUrl, String testModel) {
        long startTime = System.currentTimeMillis();
        String effectiveBaseUrl = resolveBaseUrl(testBaseUrl);

        log.info("Starting connectivity test for {}: baseUrl={}", getProviderName(), effectiveBaseUrl);

        // 火山引擎必须提供 endpoint_id
        if (testModel == null || testModel.isBlank()) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("Volcengine connectivity test requires endpoint_id");
            return new ConnectivityTestResult(
                false,
                "火山引擎连通性测试需要提供 Endpoint ID（推理接入点 ID）",
                Collections.emptyList(),
                new LevelResult(false, "火山引擎需要 Endpoint ID", latency, "ENDPOINT_ID_REQUIRED", null),
                null,
                latency
            );
        }

        // Level 1: POST /chat/completions（同时验证认证和模型可用性）
        LevelResult level1 = testLevel1ChatCompletion(effectiveBaseUrl, testApiKey, testModel);

        long totalLatency = System.currentTimeMillis() - startTime;

        log.info("Connectivity test completed for {}: success={}, latency={}ms",
            getProviderName(), level1.success(), totalLatency);

        return new ConnectivityTestResult(
            level1.success(),
            level1.message(),
            Collections.emptyList(),
            level1,
            null, // 火山引擎 Level 1 已验证模型可用性，无需 Level 2
            totalLatency
        );
    }

    /**
     * Level 1: POST /chat/completions 最小请求
     */
    private LevelResult testLevel1ChatCompletion(String baseUrl, String apiKey, String endpointId) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> body = new HashMap<>();
        body.put("model", endpointId);
        body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
        body.put("max_tokens", 1);

        try {
            String jsonBody = JsonUtils.toJson(body);

            Request.Builder requestBuilder = new Request.Builder()
                .url(baseUrl + VOLCENGINE_CHAT_PATH)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

            // 添加火山引擎特有头部
            addExtraHeaders(requestBuilder);

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                long latency = System.currentTimeMillis() - startTime;

                if (response.isSuccessful()) {
                    return new LevelResult(true, "认证成功，模型可用", latency, null, null);
                }

                String errorMsg = buildErrorMessage(response);
                log.warn("Volcengine Level 1 test failed: {}", errorMsg);
                return new LevelResult(false, errorMsg, latency,
                    classifyError(null, response.code()), null);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("Volcengine Level 1 test failed: {}", e.getMessage());
            return new LevelResult(false, "连接失败: " + e.getMessage(), latency,
                classifyError(e, null), null);
        }
    }

    @Override
    public String getDefaultTestModel() {
        return null; // 火山引擎必须用户提供 endpoint_id
    }

    @Override
    public boolean requiresUserProvidedModel() {
        return true; // 火山引擎必须提供 endpoint_id
    }

    @Override
    public String getDefaultBaseUrl() {
        return VOLCENGINE_DEFAULT_BASE_URL;
    }

    @Override
    protected String getChatCompletionPath() {
        return VOLCENGINE_CHAT_PATH;
    }

    /**
     * 获取火山引擎 Chat Completions API 的完整 URL
     */
    @Override
    protected String getChatCompletionsUrl() {
        return baseUrl + VOLCENGINE_CHAT_PATH;
    }

    /**
     * 添加火山引擎特有的请求头
     */
    @Override
    protected void addExtraHeaders(Request.Builder requestBuilder) {
        // 火山引擎支持 X-Request-Id 用于请求追踪
        requestBuilder.header("X-Request-Id", java.util.UUID.randomUUID().toString());
    }

    /**
     * 火山引擎连通性检查
     *
     * <p>注意：此方法无法正常工作，因为火山引擎需要 endpoint_id。</p>
     * <p>请使用 testConnectivity() 方法并传入模型参数。</p>
     *
     * @return 始终返回 false
     */
    @Override
    public boolean checkConnection() {
        log.warn("Volcengine checkConnection() called without endpoint_id, returning false. Use testConnectivity() instead.");
        return false;
    }
}
