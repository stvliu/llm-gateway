package com.codingas.gateway.domain.resilience.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Cluster 故障域聚合根实体
 *
 * <p>Cluster 是 Channel 的<b>跨供应商故障独立性分组</b>：同组 Channel 共享共因特征
 * （同供应商/同账号/同专线等），整组故障才跨组转移。Cluster 与 providerId 共存正交
 * （spec cluster-failover）。</p>
 *
 * <p><b>Task 6 变更</b>：语义从「供应商内分组（region/priority/healthStatus 膨胀）」
 * 改造为「跨供应商故障独立性分组」，瘦身字段。删除 region/priority/healthStatus
 * （就近路由与跨域转移排序交还给应用层，域级健康聚合随 DomainHealth 路由器在 Task 5 移除），
 * 保留 code/name/providerId + 审计，新增 description（共因特征说明）。</p>
 *
 * <p>领域模型纯洁：仅含 Getter/Setter，不含业务逻辑。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>code — 故障域编码，全局唯一（如 'openai-us' / 'claude-bedrock'）</li>
 *   <li>name — 故障域名称</li>
 *   <li>description — 共因特征说明（可空，描述同组 Channel 共享的共因特征）</li>
 *   <li>providerId — 归属供应商 ID（物理 ID，无 FK 约束，遵循项目外键物理 ID 约定）</li>
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

    /** 共因特征说明（可空，描述同组 Channel 共享的共因特征） */
    private String description;

    /** 归属供应商 ID（物理 ID） */
    private Long providerId;
}
