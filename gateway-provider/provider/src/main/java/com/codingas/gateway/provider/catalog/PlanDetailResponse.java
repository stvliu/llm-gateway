/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.catalog;

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