package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.proxy.entity.RoutingStrategy;
import com.codingas.gateway.domain.proxy.protocol.ProtocolRequest;
import com.codingas.gateway.domain.proxy.protocol.ProtocolResponse;
import com.codingas.gateway.domain.iam.valueobject.Identity;

import java.util.function.Consumer;

/**
 * 代理服务接口
 *
 * <p>Application 层统一入口，编排代理请求处理流程。</p>
 */
public interface ProxyService {

    /**
     * 代理转发非流式请求
     *
     * @param request 协议请求
     * @param identity 认证结果
     * @param strategy 路由策略
     * @return 协议响应
     */
    ProtocolResponse proxy(ProtocolRequest request, Identity identity, RoutingStrategy strategy);

    /**
     * 代理转发流式请求
     */
    void proxyStream(ProtocolRequest request, Identity identity, RoutingStrategy strategy,
                     Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError);
}