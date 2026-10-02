package com.codingas.gateway.iam.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorizationService（统一授权门面）测试")
class AuthorizationServiceTest {

    private AuthorizationService service;

    /** 管理面身份（真实角色，来自 users.role） */
    private Identity identity(String role) {
        return Identity.of(1L, role, null, null);
    }

    @BeforeEach
    void setUp() {
        service = new AuthorizationService();
    }

    // ---------- 控制面：PUBLIC ----------

    @Test
    @DisplayName("PUBLIC 规则（auth/login）：未登录也放行")
    void publicRule_login_passesWithoutIdentity() {
        assertThat(service.checkControl(null, "POST", "/api/v1/auth/login")).isTrue();
    }

    @Test
    @DisplayName("PUBLIC 规则（auth/logout）：未登录与已登录均放行（与认证拦截器公开路径语义一致，幂等）")
    void publicRule_logout_passesWithoutAndWithIdentity() {
        assertThat(service.checkControl(null, "POST", "/api/v1/auth/logout")).isTrue();
        assertThat(service.checkControl(identity("USER"), "POST", "/api/v1/auth/logout")).isTrue();
    }

    @Test
    @DisplayName("publicPathPatterns：返回 PUBLIC scope 规则路径（单一事实源）")
    void publicPathPatterns_returnsPublicRules() {
        assertThat(service.publicPathPatterns()).containsExactly(
                "/api/v1/auth/login", "/api/v1/auth/logout");
        // 约束可执行化：PUBLIC 规则必须为精确路径（无 Ant 通配符），认证侧按精确匹配消费
        assertThat(service.publicPathPatterns())
                .allSatisfy(p -> assertThat(p).doesNotContain("*", "?"));
    }

    // ---------- 控制面：LOGIN_ONLY ----------

    @Test
    @DisplayName("LOGIN_ONLY 规则（me/api-keys）：登录放行，未登录拒绝")
    void loginOnlyRule_loggedIn_passes() {
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/me/api-keys")).isTrue();
        assertThat(service.checkControl(null, "GET", "/api/v1/me/api-keys")).isFalse();
    }

    @Test
    @DisplayName("LOGIN_ONLY 规则（auth/me、protocols）")
    void loginOnlyRules_allVariants() {
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/auth/me")).isTrue();
        assertThat(service.checkControl(identity("USER"), "PATCH", "/api/v1/auth/me/password")).isTrue();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/protocols")).isTrue();
    }

    // ---------- 控制面：USER 白名单 ----------

    @Test
    @DisplayName("USER 白名单（models 读/experience/applications 读）：USER 与 ADMIN 均放行")
    void userRules_userAndAdmin_pass() {
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/models")).isTrue();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/applications")).isTrue();
        assertThat(service.checkControl(identity("USER"), "POST", "/api/v1/experience/chat")).isTrue();
        assertThat(service.checkControl(identity("ADMIN"), "GET", "/api/v1/models")).isTrue();
    }

    @Test
    @DisplayName("USER 白名单（user-api-keys 单条与写操作）")
    void userRules_apiKeyVariants() {
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/user-api-keys/1")).isTrue();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/user-api-keys/1/detail")).isTrue();
        assertThat(service.checkControl(identity("USER"), "POST", "/api/v1/user-api-keys")).isTrue();
        assertThat(service.checkControl(identity("USER"), "PUT", "/api/v1/user-api-keys/1")).isTrue();
        assertThat(service.checkControl(identity("USER"), "DELETE", "/api/v1/user-api-keys/1")).isTrue();
    }

    @Test
    @DisplayName("USER 白名单路径：未登录与未知角色拒绝")
    void userRules_notLoggedIn_orUnknownRole_rejected() {
        assertThat(service.checkControl(null, "GET", "/api/v1/models")).isFalse();
        assertThat(service.checkControl(identity("OTHER"), "GET", "/api/v1/models")).isFalse();
    }

    // ---------- 控制面：默认拒绝（仅 ADMIN） ----------

    @Test
    @DisplayName("未匹配规则的管理路径：ADMIN 放行，USER 与未登录拒绝")
    void unmatchedManagedPath_adminOnly() {
        assertThat(service.checkControl(identity("ADMIN"), "DELETE", "/api/v1/users/1")).isTrue();
        assertThat(service.checkControl(identity("ADMIN"), "GET", "/api/v1/channels")).isTrue();
        assertThat(service.checkControl(identity("USER"), "DELETE", "/api/v1/users/1")).isFalse();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/channels")).isFalse();
        assertThat(service.checkControl(null, "GET", "/api/v1/channels")).isFalse();
    }

    @Test
    @DisplayName("USER 白名单外管理端点（providers 写/stats）拒绝 USER")
    void unmatchedManagedPath_otherEndpoints() {
        assertThat(service.checkControl(identity("USER"), "POST", "/api/v1/providers")).isFalse();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/stats")).isFalse();
    }
}
