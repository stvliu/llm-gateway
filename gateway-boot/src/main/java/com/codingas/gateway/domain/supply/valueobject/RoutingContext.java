package com.codingas.gateway.domain.supply.valueobject;

import com.codingas.gateway.domain.supply.enums.Protocol;

/**
 * 路由上下文值对象
 *
 * <p>携带请求路由所需的全部信息。</p>
 */
public record RoutingContext(
        Long channelId,
        Long channelEndpointId,
        String endpointUrl,
        Protocol upstreamProtocol,
        String providerApiKey,
        Integer timeout,
        boolean needsProtocolAdaptation
) {}
