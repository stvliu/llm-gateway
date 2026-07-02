package com.codingas.gateway.application.resilience.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 故障域响应 DTO
 *
 * <p>返回故障域聚合根的完整字段，含主键与审计字段。</p>
 *
 * <p><b>Task 6 变更</b>：删除 region/priority/healthStatus 字段，新增 description 字段
 * （Cluster 语义改造为跨供应商故障独立性分组并瘦身字段）。</p>
 */
@Data
public class ClusterResponse {

    /** 故障域 ID */
    private Long id;

    /** 故障域编码，全局唯一 */
    private String code;

    /** 故障域名称 */
    private String name;

    /** 归属供应商 ID（物理 ID） */
    private Long providerId;

    /** 共因特征说明（可空） */
    private String description;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;
}
