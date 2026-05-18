package com.codingas.gateway.application.product.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
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

    private List<String> models;

    private Map<String, String> endpoints;

    private Long quotaLimit;

    private String state;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
