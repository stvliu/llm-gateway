package com.codingas.gateway.domain.proxy.service;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.proxy.gateway.LLMGateway;
import com.codingas.gateway.domain.proxy.gateway.LLMGatewayRegistry;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * 代理领域服务
 *
 * <p>负责代理转发的核心业务逻辑。</p>
 * <p>只依赖 proxy 域内的 Gateway，不跨域访问。</p>
 *
 * <p>职责：</p>
 * <ul>
 *   <li>LLM Gateway 选择</li>
 *   <li>LLM 请求转发</li>
 *   <li>流式响应处理</li>
 *   <li>Gateway 可用性检查</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyDomainService {

    private final LLMGatewayRegistry gatewayRegistry;

    /**
     * 根据提供商类型选择可用的 Gateway
     *
     * <p>封装 Gateway 选择逻辑，检查可用性。</p>
     *
     * @param providerType 提供商类型
     * @return 可用的 LLM Gateway
     * @throws NoSuchElementException 如果没有可用的 Gateway
     * @throws IllegalStateException 如果 Gateway 不可用
     */
    public LLMGateway selectGateway(ProviderType providerType) {
        LLMGateway gateway = gatewayRegistry.getGateway(providerType)
                .orElseThrow(() -> new NoSuchElementException(
                        "No gateway available for provider type: " + providerType));

        if (!gateway.isAvailable()) {
            throw new IllegalStateException("Gateway not available: " + gateway.getProviderCode());
        }

        log.debug("Selected gateway: {} for provider type: {}", gateway.getProviderCode(), providerType);
        return gateway;
    }

    /**
     * 转发非流式请求
     *
     * @param gateway LLM Gateway
     * @param request LLM 请求
     * @return LLM 响应
     */
    public LLMResponse forward(LLMGateway gateway, LLMRequest request) {
        log.debug("Forwarding request to provider: {}", gateway.getProviderCode());

        if (!gateway.isAvailable()) {
            throw new IllegalStateException("Gateway not available: " + gateway.getProviderCode());
        }

        return gateway.chat(request);
    }

    /**
     * 转发流式请求
     *
     * @param gateway LLM Gateway
     * @param request LLM 请求
     * @param callback 流式回调
     */
    public void forwardStream(LLMGateway gateway, LLMRequest request, StreamCallback callback) {
        log.debug("Forwarding stream request to provider: {}", gateway.getProviderCode());

        if (!gateway.isAvailable()) {
            throw new IllegalStateException("Gateway not available: " + gateway.getProviderCode());
        }

        gateway.chatStream(request, callback);
    }

    /**
     * 检查指定类型的 Gateway 是否可用
     *
     * @param providerType 提供商类型
     * @return true 如果 Gateway 存在且可用
     */
    public boolean isGatewayAvailable(ProviderType providerType) {
        return gatewayRegistry.getGateway(providerType)
                .map(LLMGateway::isAvailable)
                .orElse(false);
    }
}
