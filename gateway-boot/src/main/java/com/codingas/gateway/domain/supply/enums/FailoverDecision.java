package com.codingas.gateway.domain.supply.enums;

/**
 * 故障转移决策枚举
 *
 * <p>由 {@link com.codingas.gateway.application.proxy.failover.ErrorClassifier} 依据
 * {@link ProviderErrorType} 产出，指导故障转移策略层级选择。</p>
 *
 * <ul>
 *   <li>{@link #L1} — 换渠道共因故障转移：同一 Provider 内换 Key/Endpoint，解决认证/限流/配额/网络等共因故障</li>
 *   <li>{@link #L2} — 换模型降级：跨模型切换，解决模型能力问题（如未知错误、能力不支持）</li>
 *   <li>{@link #NONE} — 不转移：直接抛出原异常，适用于请求级错误（换哪都无效）或无法判定的错误</li>
 * </ul>
 */
public enum FailoverDecision {
    /** L1：换渠道共因故障转移（认证/限流/配额/网络/超时/上游错误等共因故障） */
    L1,

    /** L2：换模型降级（模型能力问题，如未知错误） */
    L2,

    /** NONE：不转移直接抛出原异常（请求级错误或无法判定错误） */
    NONE
}
