package com.codingas.gateway.application.resilience.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 故障域响应 DTO
 *
 * <p>返回故障域聚合根的完整字段，含主键与审计字段。</p>
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

    /** 区域标识 */
    private String region;

    /** 优先级（数值越小越优先） */
    private int priority;

    /** 域级健康聚合状态（HEALTHY/DEGRADED/DOWN） */
    private String healthStatus;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;
}
