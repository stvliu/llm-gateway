package com.codingas.gateway.infrastructure.proxy.gateway.protocol;

import com.codingas.gateway.domain.proxy.valueobject.ConnectivityTestResultVO;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.proxy.protocol.AnthropicMessagesRequest;
import com.codingas.gateway.domain.proxy.protocol.AnthropicMessagesResponse;
import com.codingas.gateway.domain.proxy.protocol.ProtocolRequest;
import com.codingas.gateway.domain.proxy.protocol.ProtocolResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Anthropic Messages 协议网关实现
 */
public class AnthropicProtocolGateway implements ProtocolGateway {

    private static final String MESSAGES_PATH = "/v1/messages";
    private static final String CONNECTIVITY_TEST_MODEL = "claude-3-5-haiku-20241022";

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;

    public AnthropicProtocolGateway(OkHttpClient httpClient, String baseUrl, String apiKey,
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
        AnthropicMessagesRequest anthropicRequest = (AnthropicMessagesRequest) request;
        try {
            String json = objectMapper.writeValueAsString(anthropicRequest);
            Request httpRequest = new Request.Builder()
                    .url(baseUrl + MESSAGES_PATH)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Anthropic API 调用失败: " + response.code() + " " + body);
                }
                return objectMapper.readValue(body, AnthropicMessagesResponse.class);
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException("Anthropic API 调用异常", e);
        }
    }

    @Override
    public void chatStream(ProtocolRequest request, StreamCallback callback) {
        AnthropicMessagesRequest anthropicRequest = (AnthropicMessagesRequest) request;
        try {
            String json = objectMapper.writeValueAsString(anthropicRequest);
            Request httpRequest = new Request.Builder()
                    .url(baseUrl + MESSAGES_PATH)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    callback.onError(new RuntimeException("Anthropic stream 失败: " + response.code() + " " + errorBody));
                    return;
                }
                if (response.body() != null) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
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
            AnthropicMessagesRequest testRequest = AnthropicMessagesRequest.builder()
                    .model(CONNECTIVITY_TEST_MODEL)
                    .messages(java.util.List.of(
                            AnthropicMessagesRequest.Message.builder().role("user").content("hi").build()))
                    .maxTokens(1)
                    .build();
            String json = objectMapper.writeValueAsString(testRequest);
            Request httpRequest = new Request.Builder()
                    .url(baseUrl + MESSAGES_PATH)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (response.isSuccessful()) {
                    return ConnectivityTestResultVO.success(null, 0);
                }
                return ConnectivityTestResultVO.failure("连接失败: HTTP " + response.code(), null, null);
            }
        } catch (Exception e) {
            return ConnectivityTestResultVO.failure("连接异常: " + e.getMessage(), null, null);
        }
    }
}