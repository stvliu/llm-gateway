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