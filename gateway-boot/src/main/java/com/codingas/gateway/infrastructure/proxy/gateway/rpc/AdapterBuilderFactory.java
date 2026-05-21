package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 适配器构建工厂
 *
 * <p>旧架构兼容组件，新架构下由 ProtocolGateway 体系替代。</p>
 * <p>优先从 ProtocolGateway 查找，降级到 AdapterRegistry。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterBuilderFactory {

    private final AdapterRegistry adapterRegistry;
    private final ProtocolGatewayRegistry protocolGatewayRegistry;

    /**
     * 根据供应商名称获取适配器
     *
     * @param providerName 供应商名称
     * @return 适配器实例
     */
    public Optional<LLMAdapter> getAdapter(String providerName) {
        return adapterRegistry.getAdapterByName(providerName);
    }

    /**
     * 创建临时适配器（旧架构兼容）
     *
     * @param protocolName 协议名称
     * @param baseUrl Base URL
     * @param apiKey API Key
     * @return 适配器实例
     */
    public LLMAdapter createAdapter(String protocolName, String baseUrl, String apiKey) {
        // 优先从 ProtocolGateway 查找
        Optional<ProtocolGateway> protocolOpt = protocolGatewayRegistry.getGateway(protocolName);
        if (protocolOpt.isPresent()) {
            ProtocolGateway protocol = protocolOpt.get();
            log.debug("Creating adapter via ProtocolGateway for: {}", protocolName);
            return new ProtocolGatewayAdapter(protocol, baseUrl, apiKey);
        }

        // 降级到 AdapterRegistry
        Optional<LLMAdapter> adapterOpt = adapterRegistry.getAdapterByName(protocolName);
        if (adapterOpt.isPresent()) {
            log.debug("Using registered adapter for: {}", protocolName);
            return adapterOpt.get();
        }

        throw new IllegalArgumentException("不支持的协议类型: " + protocolName);
    }
}