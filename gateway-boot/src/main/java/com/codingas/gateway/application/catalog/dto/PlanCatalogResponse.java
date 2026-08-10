/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.catalog.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 套餐目录响应
 */
@Getter
@Builder
public class PlanCatalogResponse {

    /** 套餐编码 */
    private final String planCode;

    /** 所属供应商编码 */
    private final String providerCode;

    /** 套餐名称 */
    private final String planName;

    /** 计费模式 */
    private final String billingMode;

    /** 是否已物化 */
    private final Boolean materialized;
}