package com.codingas.gateway.application.resilience.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 故障域创建/更新请求 DTO
 *
 * <p>承载故障域聚合根可编辑字段。code 全局唯一；providerId 为归属供应商 ID（物理 ID）；
 * priority 数值越小越优先，用于跨域转移排序。</p>
 *
 * <p>不提供 delete：Cluster 故障域关联 Channel，删除需级联清理 clusterId，
 * 且 {@code ClusterGateway} 无 delete 方法，遵循既有模式不新增。</p>
 */
@Data
public class ClusterRequest {

    /** 故障域编码，全局唯一 */
    @NotBlank(message = "故障域编码不能为空")
    private String code;

    /** 故障域名称 */
    @NotBlank(message = "故障域名称不能为空")
    private String name;

    /** 归属供应商 ID（物理 ID） */
    @NotNull(message = "归属供应商 ID 不能为空")
    private Long providerId;

    /** 区域标识（如 'us-east' / 'sg'） */
    private String region;

    /** 优先级（数值越小越优先） */
    @Min(value = 0, message = "优先级不能为负数")
    private int priority;
}
