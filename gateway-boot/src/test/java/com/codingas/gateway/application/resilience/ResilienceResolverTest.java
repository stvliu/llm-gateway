package com.codingas.gateway.application.resilience;

import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.application.entity.Application;
import com.codingas.gateway.domain.application.entity.ApplicationState;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import com.codingas.gateway.domain.resilience.entity.ResilienceMode;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.resilience.gateway.ResilienceProfileGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ResilienceResolver 单元测试
 *
 * <p>验证容灾画像解析链 Application → Global 的语义（见 design.md D5）：
 * Application 挂画像优先；无画像或画像被删时回退全局 default 画像；
 * Application 不存在或 default 画像缺失时 fail-fast 抛异常。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResilienceResolver 测试")
class ResilienceResolverTest {

    /** 全局 default 画像编码 */
    private static final String DEFAULT_PROFILE_CODE = "default";

    @Mock
    private ApplicationGateway applicationGateway;

    @Mock
    private ResilienceProfileGateway resilienceProfileGateway;

    @InjectMocks
    private ResilienceResolver resilienceResolver;

    @Nested
    @DisplayName("resolve 方法测试")
    class ResolveTests {

        @Test
        @DisplayName("Application 挂画像时返回应用级画像")
        void applicationHasProfile_returnsAppProfile() {
            // Application 挂 profile=5
            Application app = buildApplication(1L, 5L);
            when(applicationGateway.findById(1L)).thenReturn(app);
            ResilienceProfile appProfile = buildProfile(5L, "claude-code", ResilienceMode.STRICT);
            when(resilienceProfileGateway.findById(5L)).thenReturn(appProfile);

            ResilienceProfile result = resilienceResolver.resolve(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getCode()).isEqualTo("claude-code");
            // 不应回退 default
            verify(resilienceProfileGateway, never()).findByCode(eq(DEFAULT_PROFILE_CODE));
        }

        @Test
        @DisplayName("Application 无画像时回退全局 default 画像")
        void applicationNoProfile_returnsGlobalDefault() {
            // Application 未挂画像
            Application app = buildApplication(1L, null);
            when(applicationGateway.findById(1L)).thenReturn(app);
            ResilienceProfile defaultProfile = buildProfile(100L, DEFAULT_PROFILE_CODE, ResilienceMode.STANDARD);
            when(resilienceProfileGateway.findByCode(DEFAULT_PROFILE_CODE)).thenReturn(defaultProfile);

            ResilienceProfile result = resilienceResolver.resolve(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getCode()).isEqualTo(DEFAULT_PROFILE_CODE);
            // 不应按 ID 查应用级画像
            verify(resilienceProfileGateway, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Application 挂画像但画像被删时回退 default 画像")
        void applicationProfileDeleted_fallsBackToDefault() {
            Application app = buildApplication(1L, 5L);
            when(applicationGateway.findById(1L)).thenReturn(app);
            // 画像已被删除
            when(resilienceProfileGateway.findById(5L)).thenReturn(null);
            ResilienceProfile defaultProfile = buildProfile(100L, DEFAULT_PROFILE_CODE, ResilienceMode.STANDARD);
            when(resilienceProfileGateway.findByCode(DEFAULT_PROFILE_CODE)).thenReturn(defaultProfile);

            ResilienceProfile result = resilienceResolver.resolve(1L);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(DEFAULT_PROFILE_CODE);
        }

        @Test
        @DisplayName("Application 不存在时抛 GatewayRequestException")
        void applicationNotFound_throwsException() {
            when(applicationGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> resilienceResolver.resolve(999L))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("999");
            // 不应查画像
            verify(resilienceProfileGateway, never()).findById(anyLong());
            verify(resilienceProfileGateway, never()).findByCode(eq(DEFAULT_PROFILE_CODE));
        }

        @Test
        @DisplayName("default 画像不存在时抛 GatewayRequestException")
        void defaultProfileMissing_throwsException() {
            Application app = buildApplication(1L, null);
            when(applicationGateway.findById(1L)).thenReturn(app);
            when(resilienceProfileGateway.findByCode(DEFAULT_PROFILE_CODE)).thenReturn(null);

            assertThatThrownBy(() -> resilienceResolver.resolve(1L))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("default");
        }
    }

    // ===== Helper methods =====

    /** 构造测试用 Application，挂指定画像 ID */
    private Application buildApplication(Long id, Long resilienceProfileId) {
        Application app = new Application();
        app.setId(id);
        app.setCode("APP-" + id);
        app.setName("测试应用" + id);
        app.setState(ApplicationState.ACTIVE);
        app.setResilienceProfileId(resilienceProfileId);
        return app;
    }

    /** 构造测试用 ResilienceProfile */
    private ResilienceProfile buildProfile(Long id, String code, ResilienceMode mode) {
        ResilienceProfile profile = new ResilienceProfile();
        profile.setId(id);
        profile.setCode(code);
        profile.setName("画像" + code);
        profile.setMode(mode);
        return profile;
    }
}
