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
