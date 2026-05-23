package com.codingas.gateway.infrastructure.proxy.gateway.protocol;

import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 协议网关注册表实现
 *
 * <p>Spring 自动注入所有 ProtocolGateway 实现，按 protocolName 建立索引。</p>
 */
@Slf4j
@Component
public class ProtocolGatewayRegistryImpl implements ProtocolGatewayRegistry {

    private final Map<String, ProtocolGateway> gateways;

    public ProtocolGatewayRegistryImpl(List<ProtocolGateway> gatewayList) {
        this.gateways = gatewayList.stream()
            .collect(Collectors.toMap(ProtocolGateway::getProtocolName, Function.identity()));

        // 启动时校验无重复
        if (gateways.size() != gatewayList.size()) {
            throw new IllegalStateException("发现重复的 ProtocolGateway protocolName，请检查实现类");
        }

        log.info("已注册 {} 个协议网关: {}", gateways.size(), gateways.keySet());
    }

    @Override
    public Optional<ProtocolGateway> getGateway(String protocolName) {
        return Optional.ofNullable(gateways.get(protocolName));
    }

    @Override
    public List<ProtocolGateway> getAllGateways() {
        return List.copyOf(gateways.values());
    }
}
