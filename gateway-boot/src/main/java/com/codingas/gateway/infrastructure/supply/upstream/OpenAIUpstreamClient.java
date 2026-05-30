package com.codingas.gateway.infrastructure.supply.upstream;

import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 上游调用实现
 */
public class OpenAIUpstreamClient implements UpstreamClient {

    private static final String CHAT_PATH = "/v1/chat/completions";
    private static final String MODELS_PATH = "/v1/models";

    private final OkHttpClient httpClient;
    private final String endpointUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    public OpenAIUpstreamClient(OkHttpClient httpClient, String endpointUrl, String apiKey,
                                int timeoutSeconds, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProtocolResponse chat(ProtocolRequest request) {
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

            try (Response response = timedClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new ProviderException("UPSTREAM_ERROR", "OpenAI API 调用失败: " + response.code() + " - " + responseBody);
                }
                return objectMapper.readValue(responseBody, OpenAIChatResponse.class);
            }
        } catch (IOException e) {
            throw new ProviderException("UPSTREAM_ERROR", "OpenAI API 调用异常", e);
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
                    .url(endpointUrl + CHAT_PATH)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            timedClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (ResponseBody body = response.body()) {
                        if (!response.isSuccessful() || body == null) {
                            String errorBody = body != null ? body.string() : "no body";
                            callback.onError(new ProviderException("UPSTREAM_ERROR", "OpenAI Stream 失败: " + response.code() + " - " + errorBody));
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
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                }
            });
        } catch (IOException e) {
            callback.onError(e);
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
}