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
package com.codingas.gateway.providerhttp.upstream;

import com.codingas.gateway.protocol.*;
import com.codingas.gateway.protocol.contract.*;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.provider.vendor.ProviderException;
import com.codingas.gateway.provider.upstream.UpstreamClient;
import com.codingas.gateway.provider.upstream.ConnectivityTestResult;
import com.codingas.gateway.providerhttp.upstream.ErrorClassificationStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Anthropic 上游调用实现
 */
public class AnthropicUpstreamClient implements UpstreamClient {

    private static final String MESSAGES_PATH = "/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final OkHttpClient httpClient;
    private final String endpointUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final ErrorClassificationStrategy classifier;

    public AnthropicUpstreamClient(OkHttpClient httpClient, String endpointUrl, String apiKey,
                                   int timeoutSeconds, ObjectMapper objectMapper,
                                   ErrorClassificationStrategy classifier) {
        this.httpClient = httpClient;
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        this.classifier = classifier;
    }

    @Override
    public ProtocolResponse chat(ProtocolRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);

            OkHttpClient timedClient = httpClient.newBuilder()
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(endpointUrl + MESSAGES_PATH)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            try (Response response = timedClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    ProviderErrorType errorType = classifier.classify(response.code(), responseBody);
                    throw new ProviderException(errorType,
                            "Anthropic API 调用失败: " + response.code() + " - " + responseBody);
                }
                return objectMapper.readValue(responseBody, AnthropicMessagesResponse.class);
            }
        } catch (IOException e) {
            ProviderErrorType errorType = e instanceof SocketTimeoutException
                    ? ProviderErrorType.TIMEOUT_ERROR
                    : ProviderErrorType.NETWORK_ERROR;
            throw new ProviderException(errorType, "Anthropic API 调用异常", e);
        }
    }

    @Override
    public void chatStream(ProtocolRequest request, StreamCallback callback) {
        try {
            request.setStream(true);
            String json = objectMapper.writeValueAsString(request);

            OkHttpClient timedClient = httpClient.newBuilder()
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(endpointUrl + MESSAGES_PATH)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            timedClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    ProviderErrorType errorType = e instanceof SocketTimeoutException
                            ? ProviderErrorType.TIMEOUT_ERROR
                            : ProviderErrorType.NETWORK_ERROR;
                    callback.onError(new ProviderException(errorType, "Anthropic 网络异常: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (ResponseBody body = response.body()) {
                        if (!response.isSuccessful() || body == null) {
                            String errorBody = body != null ? body.string() : "no body";
                            ProviderErrorType errorType = classifier.classify(response.code(), errorBody);
                            callback.onError(new ProviderException(errorType,
                                    "Anthropic Stream 失败: " + response.code() + " - " + errorBody));
                            return;
                        }
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8));
                        String currentEvent = null;
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("event: ")) {
                                currentEvent = line.substring(7).trim();
                            } else if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                // message_stop 事件 → 流结束
                                if ("message_stop".equals(currentEvent) || data.contains("\"type\":\"message_stop\"")) {
                                    callback.onComplete();
                                    return;
                                }
                                // Anthropic 不使用 [DONE] 标记, 但保留兼容性判断
                                if (!data.isEmpty()) {
                                    callback.onChunk(data);
                                }
                                currentEvent = null;
                            }
                        }
                        // 流正常结束（无 message_stop 事件）
                        callback.onComplete();
                    } catch (IOException e) {
                        ProviderErrorType errorType = e instanceof SocketTimeoutException
                                ? ProviderErrorType.TIMEOUT_ERROR
                                : ProviderErrorType.NETWORK_ERROR;
                        callback.onError(new ProviderException(errorType, "Anthropic 流读取异常: " + e.getMessage()));
                    } catch (Exception e) {
                        callback.onError(new ProviderException(ProviderErrorType.UNKNOWN_ERROR, "Anthropic 流未知异常", e));
                    }
                }
            });
        } catch (IOException e) {
            ProviderErrorType errorType = e instanceof SocketTimeoutException
                    ? ProviderErrorType.TIMEOUT_ERROR
                    : ProviderErrorType.NETWORK_ERROR;
            callback.onError(new ProviderException(errorType, "Anthropic 流式请求异常: " + e.getMessage()));
        }
    }

    @Override
    public ConnectivityTestResult testConnectivity() {
        // Anthropic 没有 /models 端点，使用简单请求测试连通性
        try {
            OkHttpClient timedClient = httpClient.newBuilder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            // 发送一个最小请求来验证连通性
            String testJson = "{\"model\":\"claude-3-5-haiku-20241022\",\"max_tokens\":1,\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";
            Request httpRequest = new Request.Builder()
                    .url(endpointUrl + MESSAGES_PATH)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(testJson, MediaType.parse("application/json")))
                    .build();

            try (Response response = timedClient.newCall(httpRequest).execute()) {
                // 只要能连通就算成功（即使是 400 等错误，也说明网络可达且 API Key 被识别）
                if (response.code() < 500) {
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
}