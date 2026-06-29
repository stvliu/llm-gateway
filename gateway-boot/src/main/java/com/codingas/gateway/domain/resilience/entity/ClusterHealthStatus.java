package com.codingas.gateway.domain.resilience.entity;

/**
 * Cluster 故障域健康状态枚举
 *
 * <p>域级健康聚合状态，由域内 Channel 的健康状态聚合得出（design.md D10）。
 * 聚合规则：域内全部渠道健康→HEALTHY；部分故障→DEGRADED；全部故障→DOWN。
 * 恢复机制：域内任一 Channel half-open 探测成功→Cluster 解除 DOWN
 * （容灾方案设计.md 第七节恢复机制）。</p>
 *
 * <ul>
 *   <li>HEALTHY：域内全部渠道健康，可正常承接流量</li>
 *   <li>DEGRADED：域内部分渠道故障，仍可承接流量但容量受损</li>
 *   <li>DOWN：域内全部渠道故障（共因故障），整域不可用，需跨域转移</li>
 * </ul>
 *
 * <p>与 {@link com.codingas.gateway.domain.supply.enums.ChannelHealthStatus} 的区别：
 * 渠道级用 FAILED 表示失败，域级按设计文档术语用 DOWN 表示整域宕机；
 * 域级不设 UNKNOWN 态，Cluster 建立即有初始健康状态（默认 HEALTHY）。</p>
 */
public enum ClusterHealthStatus {
    /** 健康：域内全部渠道健康 */
    HEALTHY,
    /** 降级：域内部分渠道故障，容量受损 */
    DEGRADED,
    /** 宕机：域内全部渠道故障，需跨域转移 */
    DOWN
}
