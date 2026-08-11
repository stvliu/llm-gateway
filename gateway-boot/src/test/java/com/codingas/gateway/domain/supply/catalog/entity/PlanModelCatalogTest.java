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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanModelCatalog 测试")
class PlanModelCatalogTest {

    @Test
    @DisplayName("默认 source 为 BUILTIN")
    void defaultValues() {
        PlanModelCatalog catalog = new PlanModelCatalog();

    }

    @Test
    @DisplayName("设置和获取字段")
    void setAndGetFields() {
        PlanModelCatalog catalog = new PlanModelCatalog();
        catalog.setPlanCode("volcengine_doubao_payg");
        catalog.setModelName("doubao-pro-32k");

        assertThat(catalog.getPlanCode()).isEqualTo("volcengine_doubao_payg");
        assertThat(catalog.getModelName()).isEqualTo("doubao-pro-32k");
    }
}
