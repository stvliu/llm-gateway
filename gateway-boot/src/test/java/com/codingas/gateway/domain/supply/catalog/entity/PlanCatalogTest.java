package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanCatalog 测试")
class PlanCatalogTest {

    @Test
    @DisplayName("默认 source 为 BUILTIN，state 为 ACTIVE")
    void defaultValues() {
        PlanCatalog catalog = new PlanCatalog();
        assertThat(catalog.getState()).isEqualTo(CatalogState.ACTIVE);
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
