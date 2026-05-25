package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.enums.ProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderCatalog 测试")
class ProviderCatalogTest {

    @Test
    @DisplayName("默认 source 为 BUILTIN，state 为 ACTIVE")
    void defaultValues() {
        ProviderCatalog catalog = new ProviderCatalog();
        assertThat(catalog.getSource()).isEqualTo(CatalogSource.BUILTIN);
        assertThat(catalog.getState()).isEqualTo(CatalogState.ACTIVE);
    }

    @Test
    @DisplayName("设置和获取字段")
    void setAndGetFields() {
        ProviderCatalog catalog = new ProviderCatalog();
        catalog.setProviderCode("openai");
        catalog.setProviderName("OpenAI");
        catalog.setProviderType(ProviderType.INTERNATIONAL);
        catalog.setSource(CatalogSource.MODELS_DEV);
        catalog.setSyncedAt(Instant.now());

        assertThat(catalog.getProviderCode()).isEqualTo("openai");
        assertThat(catalog.getProviderType()).isEqualTo(ProviderType.INTERNATIONAL);
        assertThat(catalog.getSource()).isEqualTo(CatalogSource.MODELS_DEV);
    }
}
