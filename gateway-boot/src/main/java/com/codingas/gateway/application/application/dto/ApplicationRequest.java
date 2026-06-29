package com.codingas.gateway.application.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 应用创建/更新请求 DTO
 *
 * <p>承载应用聚合根可编辑字段：code（全局唯一）、name、description、resilienceProfileId。
 * state 由后端管理（创建时默认 ACTIVE），不通过此 DTO 修改。</p>
 *
 * <p>resilienceProfileId 可空：创建时不强制绑定容灾画像，后续可通过
 * {@code PUT /api/v1/applications/{id}/resilience} 独立绑定端点按需关联。
 * update 时透传 request 值（含 null 清空），与独立绑定端点语义保持一致。</p>
 */
@Data
public class ApplicationRequest {

    /** 应用编码，全局唯一 */
    @NotBlank(message = "应用编码不能为空")
    private String code;

    /** 应用名称 */
    @NotBlank(message = "应用名称不能为空")
    private String name;

    /** 应用描述 */
    private String description;

    /** 容灾画像 ID（可空：创建时不强制绑定，后续按需绑定） */
    private Long resilienceProfileId;
}
