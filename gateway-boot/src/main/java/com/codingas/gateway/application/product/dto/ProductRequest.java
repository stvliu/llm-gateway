package com.codingas.gateway.application.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 产品创建/更新请求
 */
@Data
public class ProductRequest {

    @NotNull(message = "供应商 ID 不能为空")
    private Long providerId;

    @NotBlank(message = "产品名称不能为空")
    private String name;

    @NotBlank(message = "产品类型不能为空")
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
}
