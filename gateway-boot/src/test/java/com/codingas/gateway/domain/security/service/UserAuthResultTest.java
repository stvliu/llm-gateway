package com.codingas.gateway.domain.security.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserAuthResult 单元测试
 */
@DisplayName("UserAuthResult 测试")
class UserAuthResultTest {

    @Test
    @DisplayName("创建旧架构认证结果成功")
    void create_legacy_success() {
        // when
        UserAuthResult result = UserAuthResult.legacy(1L, "ADMIN", 100L);

        // then
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("ADMIN");
        assertThat(result.apiKeyId()).isEqualTo(100L);
        assertThat(result.newArchitecture()).isFalse();
        assertThat(result.productId()).isNull();
    }

    @Test
    @DisplayName("创建新架构认证结果成功")
    void create_newArch_success() {
        // when
        UserAuthResult result = UserAuthResult.newArch(2L, "USER", 101L, 200L, 101L, 300L);

        // then
        assertThat(result.userId()).isEqualTo(2L);
        assertThat(result.role()).isEqualTo("USER");
        assertThat(result.apiKeyId()).isEqualTo(101L);
        assertThat(result.productId()).isEqualTo(200L);
        assertThat(result.userApiKeyId()).isEqualTo(101L);
        assertThat(result.teamId()).isEqualTo(300L);
        assertThat(result.newArchitecture()).isTrue();
    }

    @Test
    @DisplayName("旧架构 equals 测试")
    void equals_sameLegacyValues_equal() {
        // given
        UserAuthResult result1 = UserAuthResult.legacy(1L, "ADMIN", 100L);
        UserAuthResult result2 = UserAuthResult.legacy(1L, "ADMIN", 100L);

        // then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    @DisplayName("新架构 equals 测试")
    void equals_sameNewArchValues_equal() {
        // given
        UserAuthResult result1 = UserAuthResult.newArch(1L, "ADMIN", 100L, 200L, 100L, 300L);
        UserAuthResult result2 = UserAuthResult.newArch(1L, "ADMIN", 100L, 200L, 100L, 300L);

        // then
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("toString 包含所有字段")
    void toString_containsAllFields() {
        // given
        UserAuthResult result = UserAuthResult.legacy(1L, "ADMIN", 100L);

        // when
        String str = result.toString();

        // then
        assertThat(str).contains("userId=1");
        assertThat(str).contains("role=ADMIN");
    }
}