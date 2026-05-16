package com.codingas.gateway.application.metadata.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 供应商元数据响应 DTO
 */
@Data
@Builder
public class ProviderMetadataResponse {

    private Long id;
    private String providerId;
    private String providerName;
    private String providerType;
    private Object providerConfig;
    private String iconUrl;
    private String description;
    private Object tags;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer modelCount;
}
