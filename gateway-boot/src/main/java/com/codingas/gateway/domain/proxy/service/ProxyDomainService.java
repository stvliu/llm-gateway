package com.codingas.gateway.domain.proxy.service;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyDomainService {

    private final LLMGatewayRegistry gatewayRegistry;

    /**
     * 根据协议名称选择可用的 Gateway
     *
     * @param protocolName 协议名称
     * @return 可用的 LLM Gateway
     * @throws NoSuchElementException 如果没有可用的 Gateway
     */
    public LLMGateway selectGateway(String protocolName) {
        return gatewayRegistry.getGateway(protocolName)
                .orElseThrow(() -> new NoSuchElementException(
                        "No gateway available for protocol: " + protocolName));
    }

    /**
     * 转发非流式请求
     *
     * @param gateway LLM Gateway
     * @param request LLM 请求
     * @param baseUrl 端点 URL
     * @param apiKey API Key
     * @param timeoutSeconds 超时秒数
     * @return LLM 响应
     */
    public LLMResponse forward(LLMGateway gateway, LLMRequest request, String baseUrl, String apiKey, int timeoutSeconds) {
        log.debug("Forwarding request to provider: {}", gateway.getProviderName());
        return gateway.chat(request, baseUrl, apiKey, timeoutSeconds);
    }

    /**
     * 转发流式请求
     *
     * @param gateway LLM Gateway
     * @param request LLM 请求
     * @param baseUrl 端点 URL
     * @param apiKey API Key
     * @param timeoutSeconds 超时秒数
     * @param callback 流式回调
     */
    public void forwardStream(LLMGateway gateway, LLMRequest request, String baseUrl, String apiKey, int timeoutSeconds, StreamCallback callback) {
        log.debug("Forwarding stream request to provider: {}", gateway.getProviderName());
        gateway.chatStream(request, baseUrl, apiKey, timeoutSeconds, callback);
    }

    /**
     * 检查指定协议的 Gateway 是否存在
     *
     * @param protocolName 协议名称
     * @return true 如果 Gateway 存在
     */
    public boolean isGatewayAvailable(String protocolName) {
        return gatewayRegistry.getGateway(protocolName).isPresent();
    }
}