package com.codingas.gateway.infrastructure.supply.gateway.protocol;

import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResultVO;
import com.codingas.gateway.domain.supply.gateway.ProtocolGateway;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * OpenAI Chat Completions 协议网关实现
 */
public class OpenAIProtocolGateway implements ProtocolGateway {

    private static final String CHAT_PATH = "/v1/chat/completions";
    private static final String MODELS_PATH = "/v1/models";

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    public OpenAIProtocolGateway(OkHttpClient httpClient, String baseUrl, String apiKey,
                                  int timeoutSeconds, ObjectMapper objectMapper) {
        this.httpClient = httpClient.newBuilder()
                .callTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProtocolResponse chat(ProtocolRequest request) {
        OpenAIChatRequest openaiRequest = (OpenAIChatRequest) request;
        try {
            String json = objectMapper.writeValueAsString(openaiRequest);
            Request httpRequest = new Request.Builder()
                    .url(baseUrl + CHAT_PATH)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenAI API 调用失败: " + response.code() + " " + body);
                }
                return objectMapper.readValue(body, OpenAIChatResponse.class);
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException("OpenAI API 调用异常", e);
        }
    }

    @Override
    public void chatStream(ProtocolRequest request, StreamCallback callback) {
        OpenAIChatRequest openaiRequest = (OpenAIChatRequest) request;
        try {
            String json = objectMapper.writeValueAsString(openaiRequest);
            Request httpRequest = new Request.Builder()
                    .url(baseUrl + CHAT_PATH)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    callback.onError(new RuntimeException("OpenAI stream 失败: " + response.code() + " " + errorBody));
                    return;
                }
                if (response.body() != null) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8));
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
                }
                callback.onComplete();
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public ConnectivityTestResultVO testConnectivity() {
        try {
            Request httpRequest = new Request.Builder()
                    .url(baseUrl + MODELS_PATH)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (response.isSuccessful()) {
                    return ConnectivityTestResultVO.success(null, 0);
                }
                return ConnectivityTestResultVO.failure(null, "连接失败: HTTP " + response.code());
            }
        } catch (Exception e) {
            return ConnectivityTestResultVO.failure(null, "连接异常: " + e.getMessage());
        }
    }
}