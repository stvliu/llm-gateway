package com.codingas.gateway.application.metadata.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 模型元数据响应 DTO
 */
@Data
@Builder
public class ModelMetadataResponse {

    private Long id;
    private String providerId;
    private String providerModelId;
    private String displayName;
    private String modelFamily;
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private BigDecimal reasoningPrice;
    private BigDecimal cacheReadPrice;
    private BigDecimal cacheWritePrice;
    private BigDecimal inputAudioPrice;
    private BigDecimal outputAudioPrice;
    private String knowledgeCutoff;
    private LocalDate releaseDate;
    private Boolean openWeights;
    private List<String> modalities;
    private Map<String, Boolean> capabilities;
    private String source;
    private Instant sourceSyncedAt;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
}