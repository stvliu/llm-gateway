package com.codingas.gateway.domain.supply.catalog.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CatalogState 测试")
class CatalogStateTest {

    @Test
    @DisplayName("包含 ACTIVE 和 DEPRECATED 两个值")
    void hasExpectedValues() {
        assertThat(CatalogState.values()).containsExactly(CatalogState.ACTIVE, CatalogState.DEPRECATED);
    }
}
