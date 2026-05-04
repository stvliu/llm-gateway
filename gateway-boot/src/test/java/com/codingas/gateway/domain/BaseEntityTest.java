package com.codingas.gateway.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    @DisplayName("BaseEntity should have version field with default value 0")
    void shouldHaveVersionFieldWithDefaultValue() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("BaseEntity version should be mutable")
    void versionShouldBeMutable() {
        TestEntity entity = new TestEntity();
        entity.setVersion(5L);
        assertThat(entity.getVersion()).isEqualTo(5L);
    }

    // 测试实体类
    static class TestEntity extends BaseEntity {
        // 仅用于测试
    }
}
