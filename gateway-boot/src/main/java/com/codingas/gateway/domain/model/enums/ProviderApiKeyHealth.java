package com.codingas.gateway.domain.model.enums;

/**
 * Provider API Key 健康状态枚举
 *
 * <p>运行时健康状态，由熔断器组件管理，不持久化到数据库。</p>
 *
 * <h3>状态说明</h3>
 * <ul>
 *   <li>HEALTHY：健康，正常使用</li>
 *   <li>RATE_LIMITED：速率限制中，等待自动恢复</li>
 *   <li>OVERQUOTA：配额超限，等待自动恢复</li>
 *   <li>DEGRADED：降级运行，部分可用</li>
 *   <li>UNHEALTHY：不健康，需要关注</li>
 * </ul>
 *
 * <h3>状态转换</h3>
 * <p>所有状态均可自动恢复到 HEALTHY，由熔断器根据配置的恢复策略管理。</p>
 */
public enum ProviderApiKeyHealth {
    /** 健康，正常使用 */
    HEALTHY,

    /** 速率限制中，等待自动恢复 */
    RATE_LIMITED,

    /** 配额超限，等待自动恢复 */
    OVERQUOTA,

    /** 降级运行，部分可用（如降级到备用模型） */
    DEGRADED,

    /** 不健康，需要关注 */
    UNHEALTHY;

    /**
     * 判断是否可以接受请求
     */
    public boolean canAcceptRequest() {
        return this == HEALTHY || this == DEGRADED;
    }

    /**
     * 判断是否需要自动恢复
     */
    public boolean needsRecovery() {
        return this != HEALTHY;
    }
}
