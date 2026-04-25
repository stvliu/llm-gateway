package com.codingas.gateway.common.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseEntity 单元测试
 */
@DisplayName("BaseEntity 测试")
class BaseEntityTest {

    /**
     * 测试实现类能够正常创建
     */
    @Test
    @DisplayName("子类能正常实例化")
    void subclass_shouldBeInstantiable() {
        TestEntity entity = new TestEntity();
        assertThat(entity).isNotNull();
    }

    /**
     * 测试审计字段默认值
     */
    @Test
    @DisplayName("审计字段默认值正确")
    void auditFields_haveCorrectDefaults() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getCreatedBy()).isEqualTo(0L);
        assertThat(entity.getVersion()).isEqualTo(0);
    }

    /**
     * 测试 isDeleted 方法 - 未删除状态
     */
    @Test
    @DisplayName("isDeleted 返回 false 当 deletedAt 为 null")
    void isDeleted_whenDeletedAtIsNull_returnsFalse() {
        TestEntity entity = new TestEntity();
        assertThat(entity.isDeleted()).isFalse();
    }

    /**
     * 测试 isDeleted 方法 - 已删除状态
     */
    @Test
    @DisplayName("isDeleted 返回 true 当 deletedAt 不为 null")
    void isDeleted_whenDeletedAtIsNotNull_returnsTrue() {
        TestEntity entity = new TestEntity();
        entity.setDeletedAt(Instant.now());
        assertThat(entity.isDeleted()).isTrue();
    }


    /**
     * 测试 Getter/Setter
     */
    @Test
    @DisplayName("Getter 和 Setter 正常工作")
    void gettersAndSetters_workCorrectly() {
        TestEntity entity = new TestEntity();
        Long userId = 42L;
        entity.setCreatedBy(userId);

        assertThat(entity.getCreatedBy()).isEqualTo(userId);
    }

    /**
     * 测试版本号字段
     */
    @Test
    @DisplayName("版本号默认为 0")
    void version_defaultIsZero() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getVersion()).isEqualTo(0);
    }

    /**
     * 用于测试的 BaseEntity 子类
     */
    static class TestEntity extends BaseEntity {
    }
}
