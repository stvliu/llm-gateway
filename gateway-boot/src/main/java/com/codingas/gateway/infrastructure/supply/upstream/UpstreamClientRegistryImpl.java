package com.codingas.gateway.infrastructure.supply.upstream;

import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 上游调用注册表实现
 */
@Component
public class UpstreamClientRegistryImpl implements UpstreamClientRegistry {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public UpstreamClientRegistryImpl(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public UpstreamClient getClient(String protocol, String endpointUrl, String apiKey, int timeoutSeconds) {
        return switch (protocol) {
            case "openai" -> new OpenAIUpstreamClient(httpClient, endpointUrl, apiKey, timeoutSeconds, objectMapper);
            case "anthropic" -> new AnthropicUpstreamClient(httpClient, endpointUrl, apiKey, timeoutSeconds, objectMapper);
            default -> throw new IllegalArgumentException("不支持的协议: " + protocol);
        };
    }

    @Override
    public List<String> getSupportedProtocols() {
        return List.of("openai", "anthropic");
    }
}