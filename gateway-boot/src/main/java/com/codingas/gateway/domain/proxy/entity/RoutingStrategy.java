package com.codingas.gateway.domain.proxy.entity;

/**
 * 路由策略枚举
 *
 * <p>定义模型请求的路由策略。</p>
 */
public enum RoutingStrategy {
    /** 随机路由 */
    RANDOM,
    /** 加权路由 */
    WEIGHTED,
    /** 故障转移路由 */
    FAILOVER,
    /** 成本优化路由 */
    COST_OPTIMIZED,
    /** 延迟优化路由 */
    LATENCY_OPTIMIZED
}
