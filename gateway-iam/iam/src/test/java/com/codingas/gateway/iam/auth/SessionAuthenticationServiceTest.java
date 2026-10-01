package com.codingas.gateway.iam.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iam.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionAuthenticationService（会话认证服务）测试")
class SessionAuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    private SessionAuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new SessionAuthenticationService(userRepository);
    }

    @Test
    @DisplayName("已登录：返回携带真实角色的身份（管理面凭据字段为 null）")
    void loggedIn_returnsIdentityWithRole() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(42L);
            User user = mock(User.class);
            when(user.getRole()).thenReturn("ADMIN");
            when(userRepository.findById(42L)).thenReturn(Optional.of(user));

            Optional<Identity> result = service.authenticateSession();

            assertThat(result).isPresent();
            Identity identity = result.get();
            assertThat(identity.userId()).isEqualTo(42L);
            assertThat(identity.role()).isEqualTo("ADMIN");
            assertThat(identity.credentialId()).isNull();
            assertThat(identity.applicationId()).isNull();
        }
    }

    @Test
    @DisplayName("未登录：返回空")
    void notLoggedIn_returnsEmpty() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(false);

            Optional<Identity> result = service.authenticateSession();

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }
    }

    @Test
    @DisplayName("会话存在但用户已删除：返回角色为 null 的身份（由授权层拒绝）")
    void sessionWithoutUser_returnsIdentityWithNullRole() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(42L);
            when(userRepository.findById(42L)).thenReturn(Optional.empty());

            Optional<Identity> result = service.authenticateSession();

            assertThat(result).isPresent();
            assertThat(result.get().role()).isNull();
        }
    }
}
