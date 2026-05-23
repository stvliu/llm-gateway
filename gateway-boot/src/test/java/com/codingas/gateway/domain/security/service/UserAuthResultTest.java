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
    @DisplayName("创建新架构认证结果成功")
    void create_newArch_success() {
        UserAuthResult result = UserAuthResult.newArch(1L, "USER", 101L, 200L);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("USER");
        assertThat(result.userApiKeyId()).isEqualTo(101L);
        assertThat(result.teamId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("record equals 测试")
    void equals_sameValues_equal() {
        UserAuthResult result1 = UserAuthResult.newArch(1L, "ADMIN", 100L, 200L);
        UserAuthResult result2 = UserAuthResult.newArch(1L, "ADMIN", 100L, 200L);

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    @DisplayName("record 不同值不相等")
    void equals_differentValues_notEqual() {
        UserAuthResult result1 = UserAuthResult.newArch(1L, "ADMIN", 100L, 200L);
        UserAuthResult result2 = UserAuthResult.newArch(2L, "USER", 101L, 300L);

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    @DisplayName("toString 包含所有字段")
    void toString_containsAllFields() {
        UserAuthResult result = UserAuthResult.newArch(1L, "ADMIN", 100L, 200L);

        String str = result.toString();

        assertThat(str).contains("userId=1");
        assertThat(str).contains("role=ADMIN");
        assertThat(str).contains("userApiKeyId=100");
        assertThat(str).contains("teamId=200");
    }
}
