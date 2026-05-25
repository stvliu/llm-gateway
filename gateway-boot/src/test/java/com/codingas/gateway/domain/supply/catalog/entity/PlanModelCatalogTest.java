package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanModelCatalog 测试")
class PlanModelCatalogTest {

    @Test
    @DisplayName("默认 source 为 BUILTIN，state 为 ACTIVE")
    void defaultValues() {
        PlanModelCatalog catalog = new PlanModelCatalog();
        assertThat(catalog.getSource()).isEqualTo(CatalogSource.BUILTIN);
        assertThat(catalog.getState()).isEqualTo(CatalogState.ACTIVE);
    }

    @Test
    @DisplayName("设置和获取字段")
    void setAndGetFields() {
        PlanModelCatalog catalog = new PlanModelCatalog();
        catalog.setPlanCode("volcengine_doubao_payg");
        catalog.setProviderModelId("doubao-pro-32k");
        catalog.setSource(CatalogSource.PROVIDER_API);
        catalog.setSyncedAt(Instant.now());

        assertThat(catalog.getPlanCode()).isEqualTo("volcengine_doubao_payg");
        assertThat(catalog.getProviderModelId()).isEqualTo("doubao-pro-32k");
        assertThat(catalog.getSource()).isEqualTo(CatalogSource.PROVIDER_API);
    }
}
