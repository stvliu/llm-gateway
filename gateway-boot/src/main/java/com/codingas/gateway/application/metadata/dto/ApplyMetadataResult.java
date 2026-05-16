package com.codingas.gateway.application.metadata.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 应用元数据结果 DTO
 */
@Data
@Builder
public class ApplyMetadataResult {

    private Long providerId;
    private String providerName;
    private List<Long> modelIds;
    private List<String> modelNames;
    private Instant createdAt;
}