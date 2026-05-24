package com.codingas.gateway.infrastructure.supply.gateway.protocol;

import com.codingas.gateway.domain.supply.gateway.ProtocolGateway;
import com.codingas.gateway.domain.supply.gateway.ProtocolGatewayFactory;
import com.codingas.gateway.infrastructure.supply.gateway.protocol.AnthropicProtocolGateway;
import com.codingas.gateway.infrastructure.supply.gateway.protocol.OpenAIProtocolGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 协议网关工厂实现
 */
@Component
public class ProtocolGatewayFactoryImpl implements ProtocolGatewayFactory {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ProtocolGatewayFactoryImpl(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProtocolGateway create(String protocol, String baseUrl, String apiKey, int timeoutSeconds) {
        return switch (protocol) {
            case "openai" -> new OpenAIProtocolGateway(httpClient, baseUrl, apiKey, timeoutSeconds, objectMapper);
            case "anthropic" -> new AnthropicProtocolGateway(httpClient, baseUrl, apiKey, timeoutSeconds, objectMapper);
            default -> throw new IllegalArgumentException("不支持的协议: " + protocol);
        };
    }

    @Override
    public List<String> getSupportedProtocols() {
        return List.of("openai", "anthropic");
    }
}