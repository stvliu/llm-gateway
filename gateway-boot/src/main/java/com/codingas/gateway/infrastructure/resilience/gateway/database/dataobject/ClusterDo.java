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
 * <p><b>Task 6 变更</b>：删除 region/priority/health_status 列，新增 description 列
 * （共因特征说明），与 Cluster 实体字段瘦身保持一致。</p>
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

    /** 共因特征说明（可空） */
    @Column(name = "description", length = 512)
    private String description;

    /** 归属供应商 ID（物理 ID） */
    @Column(name = "provider_id")
    private Long providerId;
}
