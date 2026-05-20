package com.codingas.gateway.infrastructure.proxy.gateway.protocol;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult.LevelResult;
import com.codingas.gateway.domain.model.enums.ProviderErrorType;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * OpenAI Chat Completions 协议网关
 *
 * <p>实现 OpenAI Chat Completions API 协议的请求构建、响应解析和连通性测试。</p>
 * <p>所有 OpenAI 兼容供应商（DeepSeek、Moonshot、智谱等）共享此协议。</p>
 */
@Slf4j
@Component
public class OpenAIProtocolGateway implements ProtocolGateway {

    private static final String PROTOCOL_NAME = "openai";
    private static final String PROTOCOL_LABEL = "OpenAI Chat Completions 协议";
    private static final String DEFAULT_TEST_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String MODELS_PATH = "/v1/models";

    private final OkHttpClient httpClient;

    public OpenAIProtocolGateway(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolLabel() {
        return PROTOCOL_LABEL;
    }

    @Override
    public boolean validateApiKeyFormat(String apiKey) {
        return apiKey != null && apiKey.startsWith("sk-");
    }

    @Override
    public LLMResponse chat(LLMRequest request, String baseUrl, String apiKey, int timeoutSeconds) {
        log.info("OpenAI chat request: model={}, stream=false", request.getModel());

        Map<String, Object> requestBody = buildRequestBody(request);

        try {
            String jsonBody = JsonUtils.toJson(requestBody);

            Request httpRequest = new Request.Builder()
                    .url(resolveBaseUrl(baseUrl) + CHAT_COMPLETIONS_PATH)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

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
    public void chatStream(LLMRequest request, String baseUrl, String apiKey, int timeoutSeconds, StreamCallback callback) {
        log.info("OpenAI chat stream request: model={}, stream=true", request.getModel());

        request.setStream(true);
        Map<String, Object> requestBody = buildRequestBody(request);

        try {
            String jsonBody = JsonUtils.toJson(requestBody);

            Request httpRequest = new Request.Builder()
                    .url(resolveBaseUrl(baseUrl) + CHAT_COMPLETIONS_PATH)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

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
    public String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    public String getDefaultTestModel() {
        return DEFAULT_TEST_MODEL;
    }

    @Override
    public ConnectivityTestResult testConnectivity(String apiKey, String baseUrl, String model) {
        long startTime = System.currentTimeMillis();
        String effectiveBaseUrl = resolveBaseUrl(baseUrl);

        log.info("Starting connectivity test for openai protocol: baseUrl={}", effectiveBaseUrl);

        // Level 1: GET /v1/models
        List<String> discoveredModels = new ArrayList<>();
        LevelResult level1 = testLevel1ModelsApi(effectiveBaseUrl, apiKey, discoveredModels);

        // Level 2: POST /v1/chat/completions（如果 Level 1 成功）
        LevelResult level2 = null;
        if (level1.success()) {
            String effectiveModel = resolveTestModel(model, discoveredModels);
            level2 = testLevel2ChatCompletion(effectiveBaseUrl, apiKey, effectiveModel);
        }

        long totalLatency = System.currentTimeMillis() - startTime;
        boolean success = level1.success() && (level2 == null || level2.success());

        log.info("Connectivity test completed for openai protocol: success={}, latency={}ms", success, totalLatency);

        return new ConnectivityTestResult(
            success,
            buildSummaryMessage(level1, level2, discoveredModels.size()),
            discoveredModels,
            level1,
            level2,
            totalLatency
        );
    }

    // ==================== 连通性测试 ====================

    private LevelResult testLevel1ModelsApi(String baseUrl, String apiKey, List<String> models) {
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
                    log.warn("Level 1 models API failed: {}", errorMsg);
                    return new LevelResult(false, errorMsg, latency,
                        classifyError(null, response.code()), null);
                }

                parseModelsFromResponse(response, models);
                String message = models.isEmpty() ? "认证成功" : "认证成功，发现 " + models.size() + " 个模型";
                return new LevelResult(true, message, latency, null, models);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("Level 1 models API failed: {}", e.getMessage());
            return new LevelResult(false, "连接失败: " + e.getMessage(), latency,
                classifyError(e, null), null);
        }
    }

    private LevelResult testLevel2ChatCompletion(String baseUrl, String apiKey, String model) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
        body.put("max_tokens", 1);

        try {
            String jsonBody = JsonUtils.toJson(body);

            Request request = new Request.Builder()
                .url(baseUrl + CHAT_COMPLETIONS_PATH)
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
                log.warn("Level 2 chat completion failed: {}", errorMsg);
                return new LevelResult(false, errorMsg, latency,
                    classifyError(null, response.code()), null);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("Level 2 chat completion failed: {}", e.getMessage());
            return new LevelResult(false, "模型验证失败: " + e.getMessage(), latency,
                classifyError(e, null), null);
        }
    }

    // ==================== 内部方法 ====================

    private String resolveBaseUrl(String userBaseUrl) {
        if (userBaseUrl != null && !userBaseUrl.isBlank()) {
            return userBaseUrl.endsWith("/")
                ? userBaseUrl.substring(0, userBaseUrl.length() - 1)
                : userBaseUrl;
        }
        return DEFAULT_BASE_URL;
    }

    private String resolveTestModel(String requestedModel, List<String> discoveredModels) {
        if (requestedModel != null && !requestedModel.isBlank()) {
            return requestedModel;
        }
        if (!discoveredModels.isEmpty()) {
            return selectCheapestModel(discoveredModels);
        }
        return DEFAULT_TEST_MODEL;
    }

    private String selectCheapestModel(List<String> models) {
        if (models.isEmpty()) {
            return DEFAULT_TEST_MODEL;
        }

        List<String> cheapKeywords = List.of(
            "mini", "lite", "flash", "haiku",
            "nano", "small", "turbo", "instant", "express"
        );

        List<String> expensiveKeywords = List.of(
            "pro", "opus", "max", "ultra", "premium", "advanced"
        );

        for (String keyword : cheapKeywords) {
            for (String model : models) {
                String lowerModel = model.toLowerCase();
                if (lowerModel.contains(keyword)) {
                    boolean isExpensiveVariant = expensiveKeywords.stream()
                        .anyMatch(exp -> lowerModel.contains(exp));
                    if (!isExpensiveVariant) {
                        return model;
                    }
                }
            }
        }

        for (String model : models) {
            String lowerModel = model.toLowerCase();
            boolean isExpensive = expensiveKeywords.stream()
                .anyMatch(exp -> lowerModel.contains(exp));
            if (!isExpensive) {
                return model;
            }
        }

        return models.get(0);
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
                    .provider(PROTOCOL_NAME)
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

    @SuppressWarnings("unchecked")
    private void parseModelsFromResponse(Response response, List<String> models) throws IOException {
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

    private String buildErrorMessage(Response response) throws IOException {
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

    private String classifyError(Exception exception, Integer statusCode) {
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

    private String buildSummaryMessage(LevelResult level1, LevelResult level2, int modelCount) {
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
}
