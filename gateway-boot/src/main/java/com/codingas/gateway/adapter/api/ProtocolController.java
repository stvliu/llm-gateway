package com.codingas.gateway.adapter.api;

import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 协议列表 REST API
 *
 * <p>提供已注册协议的名称和显示标签，供前端动态获取可选协议列表。</p>
 */
@RestController
@RequestMapping("/api/protocols")
@RequiredArgsConstructor
public class ProtocolController {

    private final ProtocolGatewayRegistry registry;

    /**
     * 获取所有已注册协议
     *
     * @return 协议列表（name + label）
     */
    @GetMapping
    public List<Map<String, String>> listProtocols() {
        return registry.getAllGateways().stream()
            .map(gw -> Map.of("name", gw.getProtocolName(), "label", gw.getProtocolLabel()))
            .toList();
    }
}