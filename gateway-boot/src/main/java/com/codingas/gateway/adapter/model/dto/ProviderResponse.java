package com.codingas.gateway.adapter.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 提供商响应 DTO
 */
@Data
@Builder
public class ProviderResponse {

    private Long id;
    private String providerCode;
    private String providerName;
    private String providerType;
    private String baseUrl;
    private String websiteUrl;
    private String apiDocUrl;
    private String status;
    private Integer priority;
    private String createdAt;
    private String updatedAt;
}
