package com.codingas.gateway.application.product.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 产品响应
 */
@Data
public class ProductResponse {

    private Long id;

    private Long providerId;

    private String providerName;

    private String name;

    private String productType;

    private Map<String, String> endpoints;

    private BigDecimal inputPrice;

    private BigDecimal outputPrice;

    private BigDecimal reasoningPrice;

    private BigDecimal cacheReadPrice;

    private BigDecimal cacheWritePrice;

    private BigDecimal inputAudioPrice;

    private BigDecimal outputAudioPrice;

    private Long quotaLimit;

    private String state;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
