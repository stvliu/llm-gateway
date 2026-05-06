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
    @DisplayName("创建认证结果成功")
    void create_validParams_success() {
        // when
        UserAuthResult result = new UserAuthResult(1L, "ADMIN", 100L);

        // then
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("ADMIN");
        assertThat(result.apiKeyId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("创建普通用户认证结果")
    void create_userRole_success() {
        // when
        UserAuthResult result = new UserAuthResult(2L, "USER", 101L);

        // then
        assertThat(result.role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("equals 和 hashCode 测试")
    void equals_sameValues_equal() {
        // given
        UserAuthResult result1 = new UserAuthResult(1L, "ADMIN", 100L);
        UserAuthResult result2 = new UserAuthResult(1L, "ADMIN", 100L);

        // then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    @DisplayName("toString 包含所有字段")
    void toString_containsAllFields() {
        // given
        UserAuthResult result = new UserAuthResult(1L, "ADMIN", 100L);

        // when
        String str = result.toString();

        // then
        assertThat(str).contains("userId=1");
        assertThat(str).contains("role=ADMIN");
    }
}
