/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.gateway;

/**
 * 韧性客户端工厂
 *
 * <p>为原始 UpstreamClient 包装熔断器 + 重试策略，返回带韧性保护的 UpstreamClient。</p>
 * <p>接口定义在 domain 层，实现在 infrastructure 层，避免 application 层直接依赖 infrastructure 实现类。</p>
 */
public interface ResilientClientFactory {

    /**
     * 为原始 UpstreamClient 包装韧性保护
     *
     * @param rawClient       原始上游客户端
     * @param channelEndpointId 端点 ID（用于获取对应的熔断器）
     * @return 带韧性保护的 UpstreamClient
     */
    UpstreamClient wrap(UpstreamClient rawClient, Long channelEndpointId);
}