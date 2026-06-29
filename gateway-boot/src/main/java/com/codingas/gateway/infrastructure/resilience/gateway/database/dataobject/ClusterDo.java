package com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Cluster 故障域数据对象
 *
 * <p>对应 clusters 表；主键与审计字段（created_by/created_at/updated_by/updated_at）
 * 继承自 {@link BaseDo}，由 AuditingEntityListener 自动填充。</p>
 *
 * <p>health_status 字段以字符串存储（HEALTHY/DEGRADED/DOWN），由 Gateway 实现层
 * 在 DO↔Entity 转换时还原为 {@link com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus} 枚举。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "clusters")
public class ClusterDo extends BaseDo {

    /** 故障域编码，全局唯一 */
    @Column(name = "code", nullable = false, length = 64, unique = true)
    private String code;

    /** 故障域名称 */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** 归属供应商 ID（物理 ID） */
    @Column(name = "provider_id")
    private Long providerId;

    /** 区域标识 */
    @Column(name = "region", length = 32)
    private String region;

    /** 优先级（数值越小越优先） */
    @Column(name = "priority", nullable = false)
    private int priority;

    /** 域级健康聚合状态（HEALTHY/DEGRADED/DOWN） */
    @Column(name = "health_status", nullable = false, length = 16)
    private String healthStatus;
}
