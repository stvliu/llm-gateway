package com.codingas.gateway.application.metadata.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 产品元数据响应
 */
@Data
@Builder
public class ProductMetadataResponse {

    private Long id;
    private String providerId;
    private String productName;
    private String productType;
    private String description;
    private Map<String, String> endpoints;
    private Boolean isDefault;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private BigDecimal reasoningPrice;
    private BigDecimal cacheReadPrice;
    private BigDecimal cacheWritePrice;
    private BigDecimal inputAudioPrice;
    private BigDecimal outputAudioPrice;
    private String state;
    private String source;
    private Instant createdAt;
    private Instant updatedAt;
}