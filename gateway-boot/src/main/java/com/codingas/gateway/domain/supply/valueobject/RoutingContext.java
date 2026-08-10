/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.valueobject;

import com.codingas.gateway.domain.application.enums.FailureStrategy;
import com.codingas.gateway.domain.supply.enums.Protocol;

/**
 * 路由上下文值对象
 *
 * <p>携带请求路由所需的全部信息。</p>
 *
 * @param channelId               渠道 ID
 * @param channelEndpointId       渠道端点 ID
 * @param endpointUrl             端点 URL
 * @param upstreamProtocol        上游协议
 * @param providerApiKey          提供商 API Key
 * @param timeout                 超时秒数
 * @param needsProtocolAdaptation 是否需要协议适配
 * @param modelName               模型名称
 * @param upstreamModelName       上游模型名
 * @param failureStrategy         应用级失败处理策略（控制 ChannelFailoverInvoker L0/L1 行为；
 *                                RoutingResolver 透传自 Application，避免每请求重复查 DB）
 */
public record RoutingContext(
        Long channelId,
        Long channelEndpointId,
        String endpointUrl,
        Protocol upstreamProtocol,
        String providerApiKey,
        Integer timeout,
        boolean needsProtocolAdaptation,
        String modelName,
        String upstreamModelName,
        FailureStrategy failureStrategy
) {}
