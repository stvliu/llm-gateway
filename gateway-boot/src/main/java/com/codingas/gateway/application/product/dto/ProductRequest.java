package com.codingas.gateway.application.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
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

    private List<String> models;

    private Map<String, String> endpoints;

    private Long quotaLimit;
}
