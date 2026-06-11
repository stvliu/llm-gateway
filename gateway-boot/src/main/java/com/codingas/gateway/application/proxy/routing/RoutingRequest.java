package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.enums.RoutingStrategy;

/**
 * 路由请求上下文 — 携带 RouterChain 各环节所需的信息
 */
public class RoutingRequest {

    private final Long modelId;
    private final Long userId;
    private final String role;
    private final RoutingStrategy strategy;

    public RoutingRequest(Long modelId, Long userId, String role) {
        this(modelId, userId, role, RoutingStrategy.WEIGHTED);
    }

    public RoutingRequest(Long modelId, Long userId, String role, RoutingStrategy strategy) {
        this.modelId = modelId;
        this.userId = userId;
        this.role = role;
        this.strategy = strategy;
    }

    public Long getModelId() { return modelId; }
    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public RoutingStrategy getStrategy() { return strategy; }
}
