/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.protocol.gemini;

import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.StreamCallback;
import com.codingas.gateway.protocol.transport.ConnectivityTestResult;
import com.codingas.gateway.protocol.transport.ErrorClassificationStrategy;
import com.codingas.gateway.protocol.transport.SessionStartContext;
import com.codingas.gateway.protocol.transport.SessionStartHook;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.codingas.gateway.protocol.transport.UpstreamException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Gemini 上游调用实现（协议插件自包含：格式转换 + 传输调用）
 *
 * <p>调用 Gemini {@code generateContent} / {@code streamGenerateContent} 端点，
 * 认证头 {@code x-goog-api-key}；流式走 {@code :streamGenerateContent?alt=sse}，
 * Gemini SSE 无 {@code event:} 行与 {@code [DONE]} 标记，逐 {@code data:} 行透传。</p>
 */
public class GeminiUpstreamClient implements UpstreamClient<GeminiChatRequest> {

    private static final Logger log = LoggerFactory.getLogger(GeminiUpstreamClient.class);

    private static final String GENERATE_CONTENT_PATH = "/v1beta/models/%s:generateContent";
    private static final String STREAM_GENERATE_CONTENT_PATH = "/v1beta/models/%s:streamGenerateContent?alt=sse";
    private static final String MODELS_PATH = "/v1beta/models";

    private final OkHttpClient httpClient;
    private final String endpointUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final ErrorClassificationStrategy classifier;
    private final SessionStartHook sessionStartHook;

    public GeminiUpstreamClient(OkHttpClient httpClient, String endpointUrl, String apiKey,
                                int timeoutSeconds, ObjectMapper objectMapper,
                                ErrorClassificationStrategy classifier) {
        this(httpClient, endpointUrl, apiKey, timeoutSeconds, objectMapper, classifier, null);
    }

    /**
     * 创建 Gemini 上游客户端
     *
     * @param sessionStartHook 出站会话开始钩子（请求发出前触发）；null 时使用空实现
     */
    public GeminiUpstreamClient(OkHttpClient httpClient, String endpointUrl, String apiKey,
                                int timeoutSeconds, ObjectMapper objectMapper,
                                ErrorClassificationStrategy classifier, SessionStartHook sessionStartHook) {
        this.httpClient = httpClient;
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        this.classifier = classifier;
        this.sessionStartHook = sessionStartHook != null ? sessionStartHook : SessionStartHook.NOOP;
    }

    @Override
    public ProtocolResponse chat(GeminiChatRequest request) {
        try {
            String json = serializeRequest(request);

            OkHttpClient timedClient = httpClient.newBuilder()
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(endpointUrl + String.format(GENERATE_CONTENT_PATH, request.getModel()))
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            // 会话开始钩子：请求发出前触发（初始化审计/用量/追踪上下文）
            fireSessionStart(request, json, false);

            try (Response response = timedClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    ProviderErrorType errorType = classifier.classify(response.code(), responseBody);
                    throw new UpstreamException(errorType, responseBody,
                            response.code(), null, null, null, null, null);
                }
                return parseResponse(responseBody, request.getModel());
            }
        } catch (JsonProcessingException e) {
            // 请求序列化或上游 200 响应体畸形：非网络问题，归类 UNKNOWN_ERROR
            throw new UpstreamException(ProviderErrorType.UNKNOWN_ERROR, "Gemini 响应解析异常", e);
        } catch (IOException e) {
            ProviderErrorType errorType = e instanceof SocketTimeoutException
                    ? ProviderErrorType.TIMEOUT_ERROR
                    : ProviderErrorType.NETWORK_ERROR;
            throw new UpstreamException(errorType, "Gemini API 调用异常", e);
        }
    }

    @Override
    public void chatStream(GeminiChatRequest request, StreamCallback callback) {
        try {
            request.setStream(true);
            String json = serializeRequest(request);

            OkHttpClient timedClient = httpClient.newBuilder()
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(endpointUrl + String.format(STREAM_GENERATE_CONTENT_PATH, request.getModel()))
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            // 会话开始钩子：流式请求发出前触发
            fireSessionStart(request, json, true);

            timedClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    ProviderErrorType errorType = e instanceof SocketTimeoutException
                            ? ProviderErrorType.TIMEOUT_ERROR
                            : ProviderErrorType.NETWORK_ERROR;
                    callback.onError(new UpstreamException(errorType, "Gemini 网络异常: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (ResponseBody body = response.body()) {
                        if (!response.isSuccessful() || body == null) {
                            String errorBody = body != null ? body.string() : "no body";
                            ProviderErrorType errorType = classifier.classify(response.code(), errorBody);
                            callback.onError(new UpstreamException(errorType, errorBody,
                                    response.code(), null, null, null, null, null));
                            return;
                        }
                        // Gemini SSE 无 event: 行与 [DONE] 标记：逐 data: 行透传，EOF 视为流结束
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if (!data.isEmpty()) {
                                    callback.onChunk(data);
                                }
                            }
                        }
                        callback.onComplete();
                    } catch (IOException e) {
                        ProviderErrorType errorType = e instanceof SocketTimeoutException
                                ? ProviderErrorType.TIMEOUT_ERROR
                                : ProviderErrorType.NETWORK_ERROR;
                        callback.onError(new UpstreamException(errorType, "Gemini 流读取异常: " + e.getMessage()));
                    } catch (Exception e) {
                        callback.onError(new UpstreamException(ProviderErrorType.UNKNOWN_ERROR, "Gemini 流未知异常", e));
                    }
                }
            });
        } catch (JsonProcessingException e) {
            // 请求序列化失败（非网络问题）：归类 UNKNOWN_ERROR 走回调错误通道
            callback.onError(new UpstreamException(ProviderErrorType.UNKNOWN_ERROR, "Gemini 流式请求序列化异常", e));
        } catch (IOException e) {
            ProviderErrorType errorType = e instanceof SocketTimeoutException
                    ? ProviderErrorType.TIMEOUT_ERROR
                    : ProviderErrorType.NETWORK_ERROR;
            callback.onError(new UpstreamException(errorType, "Gemini 流式请求异常: " + e.getMessage()));
        }
    }

    @Override
    public ConnectivityTestResult testConnectivity() {
        try {
            OkHttpClient timedClient = httpClient.newBuilder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(endpointUrl + MODELS_PATH)
                    .addHeader("x-goog-api-key", apiKey)
                    .get()
                    .build();

            try (Response response = timedClient.newCall(httpRequest).execute()) {
                if (response.isSuccessful()) {
                    return new ConnectivityTestResult(true, null, null, 0);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    return new ConnectivityTestResult(false, null,
                            "HTTP " + response.code() + ": " + errorBody, 0);
                }
            }
        } catch (Exception e) {
            return new ConnectivityTestResult(false, null, e.getMessage(), 0);
        }
    }

    @Override
    public String supportedProvider() {
        return "gemini";
    }

    /**
     * 触发会话开始钩子（上游请求发出前）
     *
     * @param request 已调谐的出站请求
     * @param json    已序列化请求体
     * @param stream  是否流式调用
     */
    private void fireSessionStart(GeminiChatRequest request, String json, boolean stream) {
        try {
            sessionStartHook.onSessionStart(new SessionStartContext(
                    request.getTraceId(), "gemini", request.getModel(), endpointUrl, json,
                    json.getBytes(StandardCharsets.UTF_8).length, stream));
        } catch (RuntimeException e) {
            // hook 为审计/追踪类旁路：异常不阻断上游调用（fail-open），记录告警避免静默
            log.warn("SessionStart hook 执行失败（不阻断请求）: {}", e.getMessage());
        }
    }

    /**
     * 将 {@link GeminiChatRequest} 序列化为 Gemini 原生 JSON
     *
     * <p>映射：messages → contents（parts 文本）、system → systemInstruction、
     * maxTokens → generationConfig.maxOutputTokens、temperature → generationConfig.temperature。
     * 流式由 URL 端点（streamGenerateContent）决定，请求体不含 stream 字段。</p>
     */
    private String serializeRequest(GeminiChatRequest request) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();

        if (request.getMessages() != null) {
            ArrayNode contents = root.putArray("contents");
            for (GeminiChatRequest.Message m : request.getMessages()) {
                ObjectNode content = contents.addObject();
                content.put("role", m.role());
                content.putArray("parts").addObject().put("text", m.content());
            }
        }
        if (request.getSystem() != null && !request.getSystem().isBlank()) {
            root.putObject("systemInstruction").putArray("parts").addObject().put("text", request.getSystem());
        }
        ObjectNode generationConfig = root.putObject("generationConfig");
        if (request.getMaxTokens() != null) {
            generationConfig.put("maxOutputTokens", request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            generationConfig.put("temperature", request.getTemperature());
        }
        return objectMapper.writeValueAsString(root);
    }

    /**
     * 解析 Gemini 非流式响应为 {@link GeminiChatResponse}
     *
     * <p>文本取 candidates[0].content.parts[].text 拼接；usage 取
     * usageMetadata.promptTokenCount / candidatesTokenCount。Gemini 响应无 id 字段，
     * model 回显为请求模型名。</p>
     */
    private GeminiChatResponse parseResponse(String responseBody, String model) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(responseBody);

        String text = null;
        JsonNode candidates = node.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode part : parts) {
                    String t = part.path("text").asText(null);
                    if (t != null) {
                        sb.append(t);
                    }
                }
                if (sb.length() > 0) {
                    text = sb.toString();
                }
            }
        }

        Integer inputTokens = null;
        Integer outputTokens = null;
        JsonNode usage = node.path("usageMetadata");
        if (usage.isObject()) {
            if (usage.path("promptTokenCount").isNumber()) {
                inputTokens = usage.path("promptTokenCount").asInt();
            }
            if (usage.path("candidatesTokenCount").isNumber()) {
                outputTokens = usage.path("candidatesTokenCount").asInt();
            }
        }

        String finishReason = null;
        if (candidates.isArray() && !candidates.isEmpty()) {
            finishReason = candidates.get(0).path("finishReason").asText(null);
        }

        return new GeminiChatResponse(null, model, text, inputTokens, outputTokens, finishReason);
    }
}
