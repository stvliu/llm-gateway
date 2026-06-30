package com.codingas.gateway.integration;

import com.codingas.gateway.application.resilience.ResilienceProfileApplier;
import com.codingas.gateway.application.resilience.ResilienceResolver;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 容灾画像解析链端到端集成测试（Task 4.10）
 *
 * <p>验证解析链 Application → Global、档位推导、画像继承与两对照场景在真实 Spring 上下文
 * 装配下的端到端串联行为。与既有单元测试（{@code ResilienceResolverTest}、
 * {@code ResilienceProfileApplierTest}，均用 {@code @ExtendWith(MockitoExtension)} 纯 mock）
 * 的区别：本集成测试 {@link Autowired} 真实 {@link ResilienceResolver} 与
 * {@link ResilienceProfileApplier} bean（Spring 装配），仅 mock 边界 Gateway
 * （{@link ApplicationGateway} / {@link ResilienceProfileGateway}，控制 Application 与画像的返回），
 * 验证「真实 Resolver 串联真实 Applier + 真实 Gateway 装配」的端到端语义，而非单组件分支。</p>
 *
 * <p><b>真实/mock 边界</b>：继承 {@link FullContextIntegrationTestBase} 借用 Spring 上下文，
 * 但基类未 mock {@link ApplicationGateway} / {@link ResilienceProfileGateway}（这两个 Gateway
 * 在 integration-test profile 下会装配 JPA 实现，依赖 H2 seed 数据）。为隔离解析链逻辑、
 * 不耦合 seed 数据，本测试用 {@link MockBean} 覆盖这两个 Gateway，返回受控的 Application 与画像。</p>
 *
 * <p><b>两对照场景</b>（design.md 第 82 行）：
 * <ul>
 *   <li>Claude Code 禁降级：STRICT 档位，enableL2=false，depth=0，timeout=60</li>
 *   <li>客服全开：AGGRESSIVE 档位，enableL2=true，depth=3，timeout=15</li>
 * </ul>
 * </p>
 */
@DisplayName("容灾画像解析链端到端集成测试")
class ResilienceProfileIntegrationTest extends FullContextIntegrationTestBase {

    /** 真实解析器 bean（Spring 装配，串联 ApplicationGateway + ResilienceProfileGateway） */
    @Autowired
    private ResilienceResolver realResolver;

    /** 真实档位推导器 bean（Spring 装配，按 mode 推导专家字段并保留 base 非专家字段） */
    @Autowired
    private ResilienceProfileApplier realApplier;

    /** mock 应用 Gateway（覆盖 JPA 实现，控制 Application 返回） */
    @MockBean
    private ApplicationGateway applicationGateway;

    /** mock 画像 Gateway（覆盖 JPA 实现，控制画像返回） */
    @MockBean
    private ResilienceProfileGateway resilienceProfileGateway;

    /** 全局 default 画像编码（与 ResilienceResolver 常量一致） */
    private static final String DEFAULT_PROFILE_CODE = "default";

    // ==================== 解析链 Application → Global ====================

    @Nested
    @DisplayName("解析链 Application → Global")
    class ResolutionChainTests {

        @Test
        @DisplayName("Application 挂画像时返回应用级画像，不回退 default")
        void applicationHasProfile_returnsAppProfile_notFallback() {
            // Application 挂 profileId=5
            Application app = buildApplication(1L, 5L);
            when(applicationGateway.findById(1L)).thenReturn(app);
            ResilienceProfile appProfile = buildProfile(5L, "claude-code", ResilienceMode.STRICT);
            when(resilienceProfileGateway.findById(5L)).thenReturn(appProfile);

            ResilienceProfile result = realResolver.resolve(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getCode()).isEqualTo("claude-code");
            // 语义断言：命中应用级画像，不应回退查 default
            org.mockito.Mockito.verify(resilienceProfileGateway, org.mockito.Mockito.never())
                    .findByCode(eq(DEFAULT_PROFILE_CODE));
        }

        @Test
        @DisplayName("Application 未挂画像时回退全局 default 画像")
        void applicationNoProfile_fallsBackToGlobalDefault() {
            Application app = buildApplication(1L, null);
            when(applicationGateway.findById(1L)).thenReturn(app);
            ResilienceProfile defaultProfile = buildProfile(100L, DEFAULT_PROFILE_CODE, ResilienceMode.STANDARD);
            when(resilienceProfileGateway.findByCode(DEFAULT_PROFILE_CODE)).thenReturn(defaultProfile);

            ResilienceProfile result = realResolver.resolve(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getCode()).isEqualTo(DEFAULT_PROFILE_CODE);
        }

        @Test
        @DisplayName("Application 挂画像但画像被删时回退 default（解析链鲁棒性）")
        void applicationProfileDeleted_fallsBackToDefault() {
            Application app = buildApplication(1L, 5L);
            when(applicationGateway.findById(1L)).thenReturn(app);
            // 画像已被删除（findById 返回 null）
            when(resilienceProfileGateway.findById(5L)).thenReturn(null);
            ResilienceProfile defaultProfile = buildProfile(100L, DEFAULT_PROFILE_CODE, ResilienceMode.STANDARD);
            when(resilienceProfileGateway.findByCode(DEFAULT_PROFILE_CODE)).thenReturn(defaultProfile);

            ResilienceProfile result = realResolver.resolve(1L);

            assertThat(result.getCode()).isEqualTo(DEFAULT_PROFILE_CODE);
        }

        @Test
        @DisplayName("Application 不存在时 fail-fast 抛 APPLICATION_NOT_FOUND")
        void applicationNotFound_throwsFailFast() {
            when(applicationGateway.findById(999L)).thenReturn(null);

            assertThatThrownBy(() -> realResolver.resolve(999L))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("default 画像缺失时 fail-fast 抛 RESILIENCE_DEFAULT_PROFILE_MISSING")
        void defaultProfileMissing_throwsFailFast() {
            Application app = buildApplication(1L, null);
            when(applicationGateway.findById(1L)).thenReturn(app);
            when(resilienceProfileGateway.findByCode(DEFAULT_PROFILE_CODE)).thenReturn(null);

            assertThatThrownBy(() -> realResolver.resolve(1L))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("default");
        }
    }

    // ==================== 档位推导 + 画像继承 ====================

    @Nested
    @DisplayName("档位推导 + 画像继承（真实 Applier bean）")
    class ModeDerivationTests {

        @Test
        @DisplayName("STANDARD 档位：enableL2=true, depth=2, timeout=0（用渠道默认）")
        void standardMode_derivesShallowDegradation() {
            ResilienceProfile base = buildProfile(1L, "base", ResilienceMode.STANDARD);

            ResilienceProfile result = realApplier.apply(base, ResilienceMode.STANDARD);

            assertThat(result.getMode()).isEqualTo(ResilienceMode.STANDARD);
            assertThat(result.isEnableL2ModelDegradation()).isTrue();
            assertThat(result.getDegradationMaxDepth()).isEqualTo(2);
            assertThat(result.getTimeout()).isEqualTo(0);
        }

        @Test
        @DisplayName("STRICT 档位：enableL2=false, depth=0, timeout=60")
        void strictMode_derivesNoDegradation() {
            ResilienceProfile base = buildProfile(1L, "base", ResilienceMode.STANDARD);

            ResilienceProfile result = realApplier.apply(base, ResilienceMode.STRICT);

            assertThat(result.getMode()).isEqualTo(ResilienceMode.STRICT);
            assertThat(result.isEnableL2ModelDegradation()).isFalse();
            assertThat(result.getDegradationMaxDepth()).isEqualTo(0);
            assertThat(result.getTimeout()).isEqualTo(60);
        }

        @Test
        @DisplayName("AGGRESSIVE 档位：enableL2=true, depth=3, timeout=15")
        void aggressiveMode_derivesDeepDegradation() {
            ResilienceProfile base = buildProfile(1L, "base", ResilienceMode.STANDARD);

            ResilienceProfile result = realApplier.apply(base, ResilienceMode.AGGRESSIVE);

            assertThat(result.getMode()).isEqualTo(ResilienceMode.AGGRESSIVE);
            assertThat(result.isEnableL2ModelDegradation()).isTrue();
            assertThat(result.getDegradationMaxDepth()).isEqualTo(3);
            assertThat(result.getTimeout()).isEqualTo(15);
        }

        @Test
        @DisplayName("画像继承：apply 保留 base 非专家字段（code/name）")
        void apply_preservesBaseNonExpertFields() {
            // base 画像携带非专家字段
            ResilienceProfile base = buildProfile(1L, "claude-code", ResilienceMode.STANDARD);

            ResilienceProfile result = realApplier.apply(base, ResilienceMode.STRICT);

            // 专家字段被 STRICT 覆盖
            assertThat(result.isEnableL2ModelDegradation()).isFalse();
            assertThat(result.getDegradationMaxDepth()).isEqualTo(0);
            // 非专家字段保留 base 原值
            assertThat(result.getCode()).isEqualTo("claude-code");
        }

        @Test
        @DisplayName("apply 不修改 base（不可变语义）")
        void apply_doesNotMutateBase() {
            ResilienceProfile base = buildProfile(1L, "base", ResilienceMode.STANDARD);
            base.setEnableL2ModelDegradation(true);
            base.setDegradationMaxDepth(2);
            base.setTimeout(0);

            realApplier.apply(base, ResilienceMode.AGGRESSIVE);

            // base 保持原值，未被 apply 修改
            assertThat(base.getMode()).isEqualTo(ResilienceMode.STANDARD);
            assertThat(base.isEnableL2ModelDegradation()).isTrue();
            assertThat(base.getDegradationMaxDepth()).isEqualTo(2);
            assertThat(base.getTimeout()).isEqualTo(0);
        }
    }

    // ==================== 两对照场景端到端（解析链 + 档位推导串联） ====================

    @Nested
    @DisplayName("两对照场景端到端：Claude Code 禁降级 / 客服全开")
    class TwoContrastScenariosTests {

        @Test
        @DisplayName("Claude Code 禁降级：解析链命中 STRICT 画像 → enableL2=false 不触发模型降级")
        void claudeCodeScenario_strictProfile_l2Disabled() {
            // 场景：Claude Code 应用挂 STRICT 画像，解析链返回 STRICT，档位推导保持 enableL2=false
            Application app = buildApplication(1L, 5L);
            when(applicationGateway.findById(1L)).thenReturn(app);
            ResilienceProfile strictProfile = buildProfile(5L, "claude-code", ResilienceMode.STRICT);
            strictProfile.setEnableL2ModelDegradation(false);
            strictProfile.setDegradationMaxDepth(0);
            strictProfile.setTimeout(60);
            when(resilienceProfileGateway.findById(5L)).thenReturn(strictProfile);

            // 端到端：解析链 → 画像
            ResilienceProfile resolved = realResolver.resolve(1L);
            // 端到端：画像 → 档位推导（管理员选 STRICT 档位时重新推导专家字段）
            ResilienceProfile derived = realApplier.apply(resolved, ResilienceMode.STRICT);

            // Claude Code 禁降级断言：L2 关闭，宁可报错不可换模型
            assertThat(derived.isEnableL2ModelDegradation()).isFalse();
            assertThat(derived.getDegradationMaxDepth()).isEqualTo(0);
            assertThat(derived.getTimeout()).isEqualTo(60);
        }

        @Test
        @DisplayName("客服全开：解析链回退 default → AGGRESSIVE 档位推导深降级")
        void customerServiceScenario_aggressiveMode_deepDegradation() {
            // 场景：客服应用未挂画像，解析链回退 default（STANDARD），管理员切 AGGRESSIVE 档位深降级
            Application app = buildApplication(2L, null);
            when(applicationGateway.findById(2L)).thenReturn(app);
            ResilienceProfile defaultProfile = buildProfile(100L, DEFAULT_PROFILE_CODE, ResilienceMode.STANDARD);
            when(resilienceProfileGateway.findByCode(DEFAULT_PROFILE_CODE)).thenReturn(defaultProfile);

            // 端到端：解析链回退 default → 档位推导 AGGRESSIVE
            ResilienceProfile resolved = realResolver.resolve(2L);
            ResilienceProfile derived = realApplier.apply(resolved, ResilienceMode.AGGRESSIVE);

            // 客服全开断言：L2 深降级，可用性优先
            assertThat(derived.isEnableL2ModelDegradation()).isTrue();
            assertThat(derived.getDegradationMaxDepth()).isEqualTo(3);
            assertThat(derived.getTimeout()).isEqualTo(15);
            // 画像继承：default 的 code 保留
            assertThat(derived.getCode()).isEqualTo(DEFAULT_PROFILE_CODE);
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
