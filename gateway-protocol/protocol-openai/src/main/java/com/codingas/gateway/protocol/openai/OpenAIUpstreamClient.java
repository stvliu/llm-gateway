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
package com.codingas.gateway.protocol.openai;

import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.StreamCallback;
import com.codingas.gateway.protocol.raw.OpenAIChatRequest;
import com.codingas.gateway.protocol.raw.OpenAIChatResponse;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.protocol.transport.ConnectivityTestResult;
import com.codingas.gateway.protocol.transport.ErrorClassificationStrategy;
import com.codingas.gateway.protocol.transport.SessionStartContext;
import com.codingas.gateway.protocol.transport.SessionStartHook;
import com.codingas.gateway.protocol.transport.UpstreamException;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 上游调用实现（协议插件自包含：格式转换 + 传输调用）
 */
public class OpenAIUpstreamClient implements UpstreamClient<OpenAIChatRequest> {

    private static final Logger log = LoggerFactory.getLogger(OpenAIUpstreamClient.class);

    private static final String CHAT_PATH = "/v1/chat/completions";
    private static final String MODELS_PATH = "/v1/models";

    private final OkHttpClient httpClient;
    private final String endpointUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final ErrorClassificationStrategy classifier;
    private final SessionStartHook sessionStartHook;

    public OpenAIUpstreamClient(OkHttpClient httpClient, String endpointUrl, String apiKey,
                                int timeoutSeconds, ObjectMapper objectMapper,
                                ErrorClassificationStrategy classifier) {
        this(httpClient, endpointUrl, apiKey, timeoutSeconds, objectMapper, classifier, null);
    }

    /**
     * 创建 OpenAI 上游客户端
     *
     * @param sessionStartHook 出站会话开始钩子（请求发出前触发）；null 时使用空实现
     */
    public OpenAIUpstreamClient(OkHttpClient httpClient, String endpointUrl, String apiKey,
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

    /**
     * 触发会话开始钩子（上游请求发出前）
     *
     * @param request 已调谐的出站请求
     * @param json    已序列化请求体
     * @param stream  是否流式调用
     */
    private void fireSessionStart(OpenAIChatRequest request, String json, boolean stream) {
        try {
            sessionStartHook.onSessionStart(new SessionStartContext(
                    request.getTraceId(), "openai", request.getModel(), endpointUrl, json,
                    json.getBytes(StandardCharsets.UTF_8).length, stream));
        } catch (RuntimeException e) {
            // hook 为审计/追踪类旁路：异常不阻断上游调用（fail-open），记录告警避免静默
            log.warn("SessionStart hook 执行失败（不阻断请求）: {}", e.getMessage());
        }
    }

    @Override
    public ProtocolResponse chat(OpenAIChatRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);

            OkHttpClient timedClient = httpClient.newBuilder()
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(endpointUrl + CHAT_PATH)
                    .addHeader("Authorization", "Bearer " + apiKey)
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
                return objectMapper.readValue(responseBody, OpenAIChatResponse.class);
            }
        } catch (IOException e) {
            ProviderErrorType errorType = e instanceof SocketTimeoutException
                    ? ProviderErrorType.TIMEOUT_ERROR
                    : ProviderErrorType.NETWORK_ERROR;
            throw new UpstreamException(errorType, "OpenAI API 调用异常", e);
        }
    }

    @Override
    public void chatStream(OpenAIChatRequest request, StreamCallback callback) {
        try {
            request.setStream(true);
            String json = objectMapper.writeValueAsString(request);

            OkHttpClient timedClient = httpClient.newBuilder()
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(endpointUrl + CHAT_PATH)
                    .addHeader("Authorization", "Bearer " + apiKey)
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
                    callback.onError(new UpstreamException(errorType, "OpenAI 网络异常: " + e.getMessage()));
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
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) {
                                    callback.onComplete();
                                    return;
                                }
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
                        callback.onError(new UpstreamException(errorType, "OpenAI 流读取异常: " + e.getMessage()));
                    } catch (Exception e) {
                        callback.onError(new UpstreamException(ProviderErrorType.UNKNOWN_ERROR, "OpenAI 流未知异常", e));
                    }
                }
            });
        } catch (IOException e) {
            ProviderErrorType errorType = e instanceof SocketTimeoutException
                    ? ProviderErrorType.TIMEOUT_ERROR
                    : ProviderErrorType.NETWORK_ERROR;
            callback.onError(new UpstreamException(errorType, "OpenAI 流式请求异常: " + e.getMessage()));
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
                    .addHeader("Authorization", "Bearer " + apiKey)
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
        return "openai";
    }
}
