package com.codingas.gateway.application.provider.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新提供商请求
 */
@Data
public class ProviderUpdateRequest {

    @Size(max = 128, message = "Provider name must not exceed 128 characters")
    private String providerName;

    @Size(max = 512, message = "Website URL must not exceed 512 characters")
    private String websiteUrl;

    @Size(max = 512, message = "API doc URL must not exceed 512 characters")
    private String apiDocUrl;

    private Integer priority;

    /** 状态（ACTIVE / INACTIVE） */
    private String state;
}