package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.domain.model.entity.ProviderCapabilities;
import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult.LevelResult;
import com.codingas.gateway.domain.model.enums.ProviderErrorType;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.*;

/**
 * OpenAI 兼容接口适配器
 *
 * <p>实现 OpenAI API 兼容端点的适配器。</p>
 * <p>使用 OkHttp 进行 HTTP 通信。</p>
 */
@Slf4j
public class OpenAIAdapter implements LLMAdapter {

    public static final String PROVIDER_CODE = "openai";

    /**
     * 默认测试模型
     *
     * <p>仅作为 fallback 使用。正常情况下，Level 1 会返回模型列表，
     * 然后 selectCheapestModel() 会从中选择最便宜的模型。</p>
     */
    protected static final String DEFAULT_TEST_MODEL = "gpt-4o-mini";

    /** 默认 Base URL */
    protected static final String DEFAULT_BASE_URL = "https://api.openai.com";

    /** OpenAI Chat Completions API 路径 */
    protected static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    /** OpenAI Models API 路径 */
    protected static final String MODELS_PATH = "/v1/models";

    protected final OkHttpClient httpClient;
    protected final String baseUrl;
    protected final String apiKey;
    private final int timeoutSeconds;

    public OpenAIAdapter(OkHttpClient httpClient, String baseUrl, String apiKey, int timeoutSeconds) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
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
    public LLMResponse chat(LLMRequest request) {
        log.info("OpenAI chat request: model={}, stream=false", request.getModel());

        Map<String, Object> requestBody = buildRequestBody(request);

        try {
            String jsonBody = JsonUtils.toJson(requestBody);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(getChatCompletionsUrl())
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json");

            // 子类可覆盖添加额外头部
            addExtraHeaders(requestBuilder);

            requestBuilder.post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

            Request httpRequest = requestBuilder.build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenAI chat request failed: " + response);
                }

                String responseBody = response.body().string();
                LLMResponse llmResponse = parseResponse(responseBody);
                log.info("OpenAI chat response: id={}, model={}", llmResponse.getId(), llmResponse.getModel());
                return llmResponse;
            }
        } catch (IOException e) {
            log.error("OpenAI chat error: model={}, error={}", request.getModel(), e.getMessage(), e);
            throw new RuntimeException("OpenAI chat request failed", e);
        }
    }

    @Override
    public void chatStream(LLMRequest request, StreamCallback callback) {
        log.info("OpenAI chat stream request: model={}, stream=true", request.getModel());

        request.setStream(true);
        Map<String, Object> requestBody = buildRequestBody(request);

        try {
            String jsonBody = JsonUtils.toJson(requestBody);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(getChatCompletionsUrl())
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream");

            // 子类可覆盖添加额外头部
            addExtraHeaders(requestBuilder);

            requestBuilder.post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

            Request httpRequest = requestBuilder.build();

            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("OpenAI stream error: {}", e.getMessage(), e);
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody body = response.body()) {
                        if (body == null) {
                            callback.onError(new RuntimeException("Empty response body"));
                            return;
                        }

                        java.io.BufferedReader reader = new java.io.BufferedReader(body.charStream());
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.isEmpty() && line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if (!"[DONE]".equals(data)) {
                                    log.debug("OpenAI stream chunk: {}", data);
                                    callback.onChunk(data);
                                }
                            }
                        }
                        log.info("OpenAI stream completed");
                        callback.onComplete();
                    } catch (Exception e) {
                        log.error("OpenAI stream error: {}", e.getMessage(), e);
                        callback.onError(e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("OpenAI stream error: {}", e.getMessage(), e);
            callback.onError(e);
        }
    }

    @Override
    public LLMResponse messages(LLMRequest request) {
        throw new UnsupportedOperationException(
                "OpenAI adapter does not support Anthropic messages format. Use chat() instead.");
    }

    @Override
    public void messagesStream(LLMRequest request, StreamCallback callback) {
        throw new UnsupportedOperationException(
                "OpenAI adapter does not support Anthropic messages format. Use chatStream() instead.");
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
            log.warn("OpenAI health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkConnection() {
        // 发送最小 chat 请求验证连通性
        Map<String, Object> body = new HashMap<>();
        body.put("model", DEFAULT_TEST_MODEL);
        body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
        body.put("max_tokens", 1);

        try {
            String jsonBody = JsonUtils.toJson(body);

            Request request = new Request.Builder()
                    .url(getChatCompletionsUrl())
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.warn("OpenAI connection check failed: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 连通性测试 ====================

    @Override
    public ConnectivityTestResult testConnectivity(String testApiKey, String testBaseUrl, String testModel) {
        long startTime = System.currentTimeMillis();
        String effectiveBaseUrl = resolveBaseUrl(testBaseUrl);

        log.info("Starting connectivity test for {}: baseUrl={}", getProviderType(), effectiveBaseUrl);

        // Level 1: GET /v1/models
        List<String> discoveredModels = new ArrayList<>();
        LevelResult level1 = testLevel1ModelsApi(effectiveBaseUrl, testApiKey, discoveredModels);

        // Level 2: POST /v1/chat/completions（如果 Level 1 成功）
        LevelResult level2 = null;
        if (level1.success()) {
            String effectiveModel = resolveTestModel(testModel, discoveredModels);
            level2 = testLevel2ChatCompletion(effectiveBaseUrl, testApiKey, effectiveModel);
        }

        long totalLatency = System.currentTimeMillis() - startTime;
        boolean success = level1.success() && (level2 == null || level2.success());

        log.info("Connectivity test completed for {}: success={}, latency={}ms",
            getProviderType(), success, totalLatency);

        return new ConnectivityTestResult(
            success,
            buildSummaryMessage(level1, level2, discoveredModels.size()),
            discoveredModels,
            level1,
            level2,
            totalLatency
        );
    }

    /**
     * Level 1: GET /v1/models 获取模型列表
     */
    protected LevelResult testLevel1ModelsApi(String baseUrl, String apiKey, List<String> models) {
        long startTime = System.currentTimeMillis();

        try {
            Request request = new Request.Builder()
                .url(baseUrl + MODELS_PATH)
                .header("Authorization", "Bearer " + apiKey)
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                long latency = System.currentTimeMillis() - startTime;

                if (!response.isSuccessful()) {
                    String errorMsg = buildErrorMessage(response);
                    log.warn("Level 1 models API failed for {}: {}", getProviderType(), errorMsg);
                    return new LevelResult(false, errorMsg, latency,
                        classifyError(null, response.code()), null);
                }

                // 解析模型列表
                parseModelsFromResponse(response, models);
                String message = models.isEmpty() ? "认证成功" : "认证成功，发现 " + models.size() + " 个模型";
                return new LevelResult(true, message, latency, null, models);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("Level 1 models API failed for {}: {}", getProviderType(), e.getMessage());
            return new LevelResult(false, "连接失败: " + e.getMessage(), latency,
                classifyError(e, null), null);
        }
    }

    /**
     * Level 2: POST /v1/chat/completions 验证模型可用性
     */
    protected LevelResult testLevel2ChatCompletion(String baseUrl, String apiKey, String model) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
        body.put("max_tokens", 1);

        try {
            String jsonBody = JsonUtils.toJson(body);

            Request request = new Request.Builder()
                .url(baseUrl + getChatCompletionPath())
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                long latency = System.currentTimeMillis() - startTime;

                if (response.isSuccessful()) {
                    return new LevelResult(true, "模型 " + model + " 可用", latency, null, null);
                }

                String errorMsg = buildErrorMessage(response);
                log.warn("Level 2 chat completion failed for {}: {}", getProviderType(), errorMsg);
                return new LevelResult(false, errorMsg, latency,
                    classifyError(null, response.code()), null);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("Level 2 chat completion failed for {}: {}", getProviderType(), e.getMessage());
            return new LevelResult(false, "模型验证失败: " + e.getMessage(), latency,
                classifyError(e, null), null);
        }
    }

    @Override
    public String getDefaultTestModel() {
        return DEFAULT_TEST_MODEL;
    }

    @Override
    public String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    /**
     * 获取 Chat Completions API 路径（子类可覆盖）
     */
    protected String getChatCompletionPath() {
        return CHAT_COMPLETIONS_PATH;
    }

    /**
     * 解析 Base URL
     */
    protected String resolveBaseUrl(String userBaseUrl) {
        if (userBaseUrl != null && !userBaseUrl.isBlank()) {
            return userBaseUrl.endsWith("/")
                ? userBaseUrl.substring(0, userBaseUrl.length() - 1)
                : userBaseUrl;
        }
        return getDefaultBaseUrl();
    }

    /**
     * 解析测试模型
     *
     * <p>选择策略：</p>
     * <ol>
     *   <li>用户指定的模型优先</li>
     *   <li>从发现的模型列表中选择便宜的模型（mini/lite/flash 等）</li>
     *   <li>最后使用默认测试模型</li>
     * </ol>
     */
    protected String resolveTestModel(String requestedModel, List<String> discoveredModels) {
        // 用户指定模型优先
        if (requestedModel != null && !requestedModel.isBlank()) {
            return requestedModel;
        }

        // 从发现的模型中选择便宜的模型
        if (!discoveredModels.isEmpty()) {
            return selectCheapestModel(discoveredModels);
        }

        // 回退到默认模型
        return getDefaultTestModel();
    }

    /**
     * 从模型列表中选择最便宜的模型
     *
     * <p>优先级（从高到低）：</p>
     * <ul>
     *   <li>mini, lite, flash, haiku - 最便宜</li>
     *   <li>nano, small, turbo, instant - 较便宜</li>
     *   <li>排除 pro, opus, max, ultra 等昂贵模型</li>
     * </ul>
     */
    protected String selectCheapestModel(List<String> models) {
        if (models.isEmpty()) {
            return getDefaultTestModel();
        }

        // 定义便宜模型的关键词（优先级从高到低）
        List<String> cheapKeywords = List.of(
            "mini", "lite", "flash", "haiku",
            "nano", "small", "turbo", "instant", "express"
        );

        // 定义昂贵模型的关键词（排除）
        List<String> expensiveKeywords = List.of(
            "pro", "opus", "max", "ultra", "premium", "advanced"
        );

        // 第一轮：寻找包含便宜关键词的模型
        for (String keyword : cheapKeywords) {
            for (String model : models) {
                String lowerModel = model.toLowerCase();
                if (lowerModel.contains(keyword)) {
                    // 确保不是昂贵模型的变体（如 pro-mini）
                    boolean isExpensiveVariant = expensiveKeywords.stream()
                        .anyMatch(exp -> lowerModel.contains(exp));
                    if (!isExpensiveVariant) {
                        log.debug("Selected cheapest model: {} (matched keyword: {})", model, keyword);
                        return model;
                    }
                }
            }
        }

        // 第二轮：寻找不包含昂贵关键词的模型
        for (String model : models) {
            String lowerModel = model.toLowerCase();
            boolean isExpensive = expensiveKeywords.stream()
                .anyMatch(exp -> lowerModel.contains(exp));
            if (!isExpensive) {
                log.debug("Selected non-expensive model: {}", model);
                return model;
            }
        }

        // 第三轮：直接选择第一个模型
        log.debug("Fallback to first model: {}", models.get(0));
        return models.get(0);
    }

    /**
     * 构建错误消息
     */
    protected String buildErrorMessage(Response response) throws IOException {
        String errorMsg = "HTTP " + response.code();
        if (response.body() != null) {
            String body = response.body().string();
            if (body.length() > 200) {
                body = body.substring(0, 200);
            }
            errorMsg += ": " + body;
        }
        return errorMsg;
    }

    /**
     * 解析模型列表
     */
    @SuppressWarnings("unchecked")
    protected void parseModelsFromResponse(Response response, List<String> models) throws IOException {
        if (response.body() != null) {
            try {
                String body = response.body().string();
                Map<String, Object> result = JsonUtils.toMap(body);
                List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                if (data != null) {
                    for (Map<String, Object> model : data) {
                        String id = (String) model.get("id");
                        if (id != null) {
                            models.add(id);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse models list, skipping", e);
            }
        }
    }

    /**
     * 错误分类
     */
    protected String classifyError(Exception exception, Integer statusCode) {
        if (statusCode != null) {
            return switch (statusCode) {
                case 401, 403 -> ProviderErrorType.AUTHENTICATION_ERROR.name();
                case 429 -> ProviderErrorType.RATE_LIMIT_ERROR.name();
                case 402 -> ProviderErrorType.QUOTA_EXCEEDED.name();
                case 400 -> ProviderErrorType.INVALID_REQUEST.name();
                case 500, 502, 503 -> ProviderErrorType.UPSTREAM_ERROR.name();
                default -> ProviderErrorType.UNKNOWN_ERROR.name();
            };
        }

        if (exception != null) {
            if (exception instanceof java.net.SocketTimeoutException) {
                return ProviderErrorType.TIMEOUT_ERROR.name();
            }
            if (exception instanceof java.net.ConnectException ||
                exception instanceof java.net.UnknownHostException) {
                return ProviderErrorType.NETWORK_ERROR.name();
            }
        }

        return ProviderErrorType.UNKNOWN_ERROR.name();
    }

    /**
     * 构建摘要消息
     */
    protected String buildSummaryMessage(LevelResult level1, LevelResult level2, int modelCount) {
        if (!level1.success()) {
            return "认证失败: " + level1.message();
        }

        if (level2 == null) {
            return modelCount > 0
                ? "验证成功，发现 " + modelCount + " 个模型"
                : "验证成功";
        }

        if (!level2.success()) {
            return "认证成功，但模型验证失败: " + level2.message();
        }

        return modelCount > 0
            ? "验证成功，发现 " + modelCount + " 个模型"
            : "验证成功";
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
        Map<String, Object> body = new HashMap<>();
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
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools());
        }
        if (request.getToolChoice() != null) {
            body.put("tool_choice", request.getToolChoice());
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private LLMResponse parseResponse(String responseBody) {
        try {
            Map<String, Object> response = JsonUtils.toMap(responseBody);
            if (response == null) {
                throw new RuntimeException("Empty response body");
            }
            return LLMResponse.builder()
                    .provider(PROVIDER_CODE)
                    .id((String) response.get("id"))
                    .model((String) response.get("model"))
                    .created(response.get("created") != null ? ((Number) response.get("created")).longValue() : null)
                    .content(parseContent(response))
                    .usage(parseUsage(response))
                    .finishReason(extractFinishReason(response))
                    .stream(false)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    /**
     * 安全地提取 finish_reason
     */
    @SuppressWarnings("unchecked")
    private String extractFinishReason(Map<String, Object> response) {
        var choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return (String) choices.get(0).get("finish_reason");
    }

    @SuppressWarnings("unchecked")
    private LLMResponse.Content parseContent(Map<String, Object> response) {
        var choices = (List<Map<String, Object>>) response.get("choices");
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
                .toolCalls(parseToolCalls(message))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<LLMResponse.ToolCall> parseToolCalls(Map<String, Object> message) {
        var toolCalls = (List<Map<String, Object>>) message.get("tool_calls");
        if (toolCalls == null || toolCalls.isEmpty()) {
            return null;
        }
        return toolCalls.stream().map(tc -> {
            var function = (Map<String, Object>) tc.get("function");
            return LLMResponse.ToolCall.builder()
                    .id((String) tc.get("id"))
                    .type((String) tc.get("type"))
                    .function(LLMResponse.FunctionCall.builder()
                            .name((String) function.get("name"))
                            .arguments((String) function.get("arguments"))
                            .build())
                    .build();
        }).toList();
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

    /**
     * 获取 Chat Completions API 的完整 URL
     *
     * <p>子类可覆盖此方法以支持不同的 API 路径。</p>
     *
     * @return 完整的 API URL
     */
    protected String getChatCompletionsUrl() {
        return baseUrl + CHAT_COMPLETIONS_PATH;
    }

    /**
     * 添加额外的请求头
     *
     * <p>子类可覆盖此方法以添加额外的请求头。</p>
     *
     * @param requestBuilder 请求构建器
     */
    protected void addExtraHeaders(Request.Builder requestBuilder) {
        // 默认无操作，子类可覆盖
    }
}
