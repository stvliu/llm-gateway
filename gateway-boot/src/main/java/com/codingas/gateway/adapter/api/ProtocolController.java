package com.codingas.gateway.adapter.api;

import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Map;

/**
 * 协议列表 REST API
 *
 * <p>提供已注册协议的名称列表，供前端动态获取可选协议。</p>
 */
@RestController
@RequestMapping("/api/v1/protocols")
@RequiredArgsConstructor
public class ProtocolController {

    private final ProtocolGatewayFactory protocolGatewayFactory;

    private static final Map<String, String> PROTOCOL_LABELS = Map.of(
            "openai", "OpenAI Chat Completions",
            "anthropic", "Anthropic Messages"
    );

    /**
     * 获取所有已注册协议
     */
    @GetMapping
    public List<Map<String, String>> listProtocols() {
        return protocolGatewayFactory.getSupportedProtocols().stream()
            .map(protocol -> Map.of("name", protocol, "label", PROTOCOL_LABELS.getOrDefault(protocol, protocol)))
            .toList();
    }
}