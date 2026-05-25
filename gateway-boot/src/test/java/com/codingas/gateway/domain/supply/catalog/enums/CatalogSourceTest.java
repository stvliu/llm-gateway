package com.codingas.gateway.domain.supply.catalog.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CatalogSource 测试")
class CatalogSourceTest {

    @Test
    @DisplayName("优先级顺序：BUILTIN < MODELS_DEV < PROVIDER_API < MANUAL < OVERRIDE")
    void priorityOrder() {
        assertThat(CatalogSource.BUILTIN.getPriority()).isLessThan(CatalogSource.MODELS_DEV.getPriority());
        assertThat(CatalogSource.MODELS_DEV.getPriority()).isLessThan(CatalogSource.PROVIDER_API.getPriority());
        assertThat(CatalogSource.PROVIDER_API.getPriority()).isLessThan(CatalogSource.MANUAL.getPriority());
        assertThat(CatalogSource.MANUAL.getPriority()).isLessThan(CatalogSource.OVERRIDE.getPriority());
    }

    @Test
    @DisplayName("高优先级可以覆盖低优先级")
    void canOverride_higherOverridesLower() {
        assertThat(CatalogSource.MODELS_DEV.canOverride(CatalogSource.BUILTIN)).isTrue();
        assertThat(CatalogSource.BUILTIN.canOverride(CatalogSource.MODELS_DEV)).isFalse();
        assertThat(CatalogSource.OVERRIDE.canOverride(CatalogSource.MANUAL)).isTrue();
    }

    @Test
    @DisplayName("同优先级可以互相覆盖")
    void canOverride_samePriority() {
        assertThat(CatalogSource.MODELS_DEV.canOverride(CatalogSource.MODELS_DEV)).isTrue();
    }
}
