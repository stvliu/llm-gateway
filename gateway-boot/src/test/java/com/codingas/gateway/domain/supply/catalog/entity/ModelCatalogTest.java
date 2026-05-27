package com.codingas.gateway.domain.supply.catalog.entity;

import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelCatalog 测试")
class ModelCatalogTest {

    @Test
    @DisplayName("默认 source 为 BUILTIN，state 为 ACTIVE")
    void defaultValues() {
        ModelCatalog catalog = new ModelCatalog();
        assertThat(catalog.getSource()).isEqualTo(CatalogSource.BUILTIN);
        assertThat(catalog.getState()).isEqualTo(CatalogState.ACTIVE);
    }

    @Test
    @DisplayName("设置和获取字段")
    void setAndGetFields() {
        ModelCatalog catalog = new ModelCatalog();
        catalog.setModelName("gpt-4o");
        catalog.setDisplayName("GPT-4o");
        catalog.setModelFamily("gpt-4");
        catalog.setContextWindow(128000);
        catalog.setMaxInputTokens(128000);
        catalog.setMaxOutputTokens(16384);
        catalog.setKnowledgeCutoff("2024-04");
        catalog.setCapabilities("{\"vision\":true,\"tool_use\":true,\"streaming\":true}");
        catalog.setModalities("[\"text\",\"image\"]");
        catalog.setSource(CatalogSource.MODELS_DEV);
        catalog.setSyncedAt(Instant.now());

        assertThat(catalog.getModelName()).isEqualTo("gpt-4o");
        assertThat(catalog.getContextWindow()).isEqualTo(128000);
        assertThat(catalog.getCapabilities()).contains("vision");
        assertThat(catalog.getModalities()).contains("text");
        assertThat(catalog.getSource()).isEqualTo(CatalogSource.MODELS_DEV);
    }
}
