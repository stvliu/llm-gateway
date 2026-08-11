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
package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.domain.supply.enums.BillingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanCatalog 测试")
class PlanCatalogTest {

    @Test
    @DisplayName("默认 source 为 BUILTIN")
    void defaultValues() {
        PlanCatalog catalog = new PlanCatalog();
    }

    @Test
    @DisplayName("设置和获取字段")
    void setAndGetFields() {
        PlanCatalog catalog = new PlanCatalog();
        catalog.setPlanCode("volcengine_doubao_payg");
        catalog.setProviderCode("volcengine");
        catalog.setPlanName("豆包按量付费");
        catalog.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        catalog.setEndpoints("[{\"protocol\":\"openai\",\"url\":\"https://ark.cn-beijing.volces.com\"}]");
        catalog.setPricing("[{\"providerModelId\":\"doubao-pro\",\"inputPrice\":0.0008,\"outputPrice\":0.002}]");

        assertThat(catalog.getPlanCode()).isEqualTo("volcengine_doubao_payg");
        assertThat(catalog.getBillingMode()).isEqualTo(BillingMode.PAY_AS_YOU_GO);
        assertThat(catalog.getEndpoints()).contains("openai");

    }
}
