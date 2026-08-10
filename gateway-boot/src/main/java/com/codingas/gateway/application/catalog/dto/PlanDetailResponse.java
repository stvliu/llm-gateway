/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.catalog.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 套餐详情响应
 *
 * <p>含解析后的端点列表和定价信息。</p>
 */
@Getter
@Builder
public class PlanDetailResponse {

    /** 套餐编码 */
    private final String planCode;

    /** 所属供应商编码 */
    private final String providerCode;

    /** 套餐名称 */
    private final String planName;

    /** 计费模式 */
    private final String billingMode;

    /** 描述 */
    private final String description;

    /** 解析后的端点列表 */
    private final List<EndpointInfo> endpoints;

    /** 解析后的定价列表 */
    private final List<PricingInfo> pricing;

    /** 是否已物化 */
    private final Boolean materialized;

    /**
     * 端点信息
     */
    @Getter
    @Builder
    public static class EndpointInfo {

        /** 协议 */
        private final String protocol;

        /** 端点 URL */
        private final String url;
    }

    /**
     * 定价信息
     */
    @Getter
    @Builder
    public static class PricingInfo {

        /** 供应商模型标识 */
        private final String providerModelId;

        /** 输入价格（每百万 Token） */
        private final java.math.BigDecimal inputPrice;

        /** 输出价格（每百万 Token） */
        private final java.math.BigDecimal outputPrice;

        /** 缓存读取价格（每百万 Token） */
        private final java.math.BigDecimal cacheReadPrice;
    }
}