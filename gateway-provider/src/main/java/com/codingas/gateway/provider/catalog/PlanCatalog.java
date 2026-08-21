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

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.provider.model.BillingMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 套餐目录实体
 *
 * <p>存储套餐的目录信息，包括计费模式、端点列表、定价信息等。</p>
 * <p>endpoints 和 pricing 在实体中存储为 JSON 字符串，转换逻辑在 Infrastructure 层处理。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PlanCatalog extends BaseEntity {

    /** 业务标识：volcengine_doubao_payg */
    private String planCode;

    /** 所属供应商代码 → ProviderCatalog */
    private String providerCode;

    /** 展示名 */
    private String planName;

    /** 计费模式：PAY_AS_YOU_GO / SUBSCRIPTION / PACKAGE */
    private BillingMode billingMode;

    /** 端点列表 JSON：[{protocol, url}, ...] */
    private String endpoints;

    /** 定价信息 JSON：[{providerModelId, inputPrice, outputPrice, ...}, ...] */
    private String pricing;

    /** 描述 */
    private String description;

    /** 目录状态，默认 ACTIVE */
    
}
