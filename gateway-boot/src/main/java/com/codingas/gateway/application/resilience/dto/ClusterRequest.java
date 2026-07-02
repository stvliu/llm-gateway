package com.codingas.gateway.application.resilience.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 故障域创建/更新请求 DTO
 *
 * <p>承载故障域聚合根可编辑字段。code 全局唯一；providerId 为归属供应商 ID（物理 ID）；
 * description 为共因特征说明（可空）。</p>
 *
 * <p><b>Task 6 变更</b>：删除 region/priority 字段，新增 description 字段
 * （Cluster 语义改造为跨供应商故障独立性分组并瘦身字段）。</p>
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

    /** 共因特征说明（可空，描述同组 Channel 共享的共因特征） */
    private String description;
}
