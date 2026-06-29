package com.codingas.gateway.domain.resilience.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Cluster 故障域聚合根实体
 *
 * <p>Cluster 是 Channel 的故障域分组，同组 Channel 共享共因特征
 * （同供应商/同账号/同区域/同专线）。容灾转移规则：故障域内优先→整域故障才跨域
 * （design.md D10、容灾方案设计.md 第三节）。</p>
 *
 * <p>域级健康聚合：healthStatus 由域内 Channel 健康状态聚合得出，
 * 由后续 Task 的 HealthAggregator 维护（域内任一 Channel half-open 成功→解除 DOWN）。</p>
 *
 * <p>领域模型纯洁：仅含 Getter/Setter，不含业务逻辑。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>code — 故障域编码，全局唯一（如 'openai-us' / 'claude-bedrock'）</li>
 *   <li>name — 故障域名称</li>
 *   <li>providerId — 归属供应商 ID（物理 ID，无 FK 约束，遵循项目外键物理 ID 约定）</li>
 *   <li>region — 区域标识（如 'us-east' / 'sg'），用于就近路由</li>
 *   <li>priority — 优先级（数值越小越优先，用于跨域转移排序）</li>
 *   <li>healthStatus — 域级健康聚合状态（HEALTHY/DEGRADED/DOWN）</li>
 *   <li>id, createdBy, createdAt, updatedBy, updatedAt — 主键与审计字段，继承自 {@link BaseEntity}</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class Cluster extends BaseEntity {

    /** 故障域编码，全局唯一 */
    private String code;

    /** 故障域名称 */
    private String name;

    /** 归属供应商 ID（物理 ID） */
    private Long providerId;

    /** 区域标识 */
    private String region;

    /** 优先级（数值越小越优先） */
    private int priority;

    /** 域级健康聚合状态 */
    private ClusterHealthStatus healthStatus;
}
