package com.codingas.gateway.application.resilience;

import com.codingas.gateway.domain.resilience.entity.ResilienceMode;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResilienceProfileApplier 单元测试
 *
 * <p>验证容灾模式档位 → 画像专家字段自动推导语义（见 design.md D5 与 V56 seed）：
 * 管理员选档位时，按档位覆盖专家字段（mode/enableL2/degradationMaxDepth/timeout），
 * 其余字段（code/name/sessionAffinity/pinnedModel 等）保留 base 画像原值。</p>
 *
 * <p>字段值与 V56 seed 一致：
 * <ul>
 *   <li>STANDARD — enableL2=true, depth=2（浅降级）, timeout=0（用渠道默认）</li>
 *   <li>STRICT — enableL2=false, depth=0（L2 关闭）, timeout=60</li>
 *   <li>AGGRESSIVE — enableL2=true, depth=3（深降级）, timeout=15（短超时）</li>
 * </ul>
 * </p>
 */
@DisplayName("ResilienceProfileApplier 测试")
class ResilienceProfileApplierTest {

    /** 被测组件无外部依赖，直接实例化 */
    private final ResilienceProfileApplier applier = new ResilienceProfileApplier();

    @Nested
    @DisplayName("apply 方法：按档位推导专家字段")
    class ApplyByModeTests {

        @Test
        @DisplayName("STANDARD 档位推导浅降级字段：enableL2=true/depth=2/timeout=0")
        void applyStandard_derivesShallowDegradation() {
            ResilienceProfile base = buildBaseProfile();

            ResilienceProfile result = applier.apply(base, ResilienceMode.STANDARD);

            assertThat(result.getMode()).isEqualTo(ResilienceMode.STANDARD);
            assertThat(result.isEnableL2ModelDegradation()).isTrue();
            assertThat(result.getDegradationMaxDepth()).isEqualTo(2);
            assertThat(result.getTimeout()).isEqualTo(0);
        }

        @Test
        @DisplayName("STRICT 档位推导 L2 关闭字段：enableL2=false/depth=0/timeout=60")
        void applyStrict_derivesL2Disabled() {
            ResilienceProfile base = buildBaseProfile();

            ResilienceProfile result = applier.apply(base, ResilienceMode.STRICT);

            assertThat(result.getMode()).isEqualTo(ResilienceMode.STRICT);
            assertThat(result.isEnableL2ModelDegradation()).isFalse();
            assertThat(result.getDegradationMaxDepth()).isEqualTo(0);
            assertThat(result.getTimeout()).isEqualTo(60);
        }

        @Test
        @DisplayName("AGGRESSIVE 档位推导深降级短超时字段：enableL2=true/depth=3/timeout=15")
        void applyAggressive_derivesDeepDegradationShortTimeout() {
            ResilienceProfile base = buildBaseProfile();

            ResilienceProfile result = applier.apply(base, ResilienceMode.AGGRESSIVE);

            assertThat(result.getMode()).isEqualTo(ResilienceMode.AGGRESSIVE);
            assertThat(result.isEnableL2ModelDegradation()).isTrue();
            assertThat(result.getDegradationMaxDepth()).isEqualTo(3);
            assertThat(result.getTimeout()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("apply 方法：保留 base 非专家字段")
    class PreserveBaseFieldsTests {

        @Test
        @DisplayName("档位推导后保留 base 的标识与会话亲和/锁定模型等非专家字段")
        void apply_preservesBaseNonExpertFields() {
            ResilienceProfile base = buildBaseProfile();

            ResilienceProfile result = applier.apply(base, ResilienceMode.AGGRESSIVE);

            // 标识字段保留 base
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getCode()).isEqualTo("custom-app");
            assertThat(result.getName()).isEqualTo("自定义应用画像");
            // 会话亲和字段保留 base（V56 seed 中不随档位变化）
            assertThat(result.isEnableSessionAffinity()).isTrue();
            assertThat(result.getSessionAffinityTtlMinutes()).isEqualTo(45);
            // 锁定模型字段保留 base
            assertThat(result.isEnablePinnedModel()).isTrue();
            assertThat(result.getPinnedModelId()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("apply 方法：不可变语义")
    class ImmutabilityTests {

        @Test
        @DisplayName("apply 不修改 base 对象（返回新对象，base 专家字段保持原值）")
        void apply_doesNotMutateBase() {
            ResilienceProfile base = buildBaseProfile();
            // base 调用前的专家字段占位值
            ResilienceMode baseModeBefore = base.getMode();
            boolean baseL2Before = base.isEnableL2ModelDegradation();
            int baseDepthBefore = base.getDegradationMaxDepth();
            int baseTimeoutBefore = base.getTimeout();

            applier.apply(base, ResilienceMode.AGGRESSIVE);

            // base 对象未被修改
            assertThat(base.getMode()).isEqualTo(baseModeBefore);
            assertThat(base.isEnableL2ModelDegradation()).isEqualTo(baseL2Before);
            assertThat(base.getDegradationMaxDepth()).isEqualTo(baseDepthBefore);
            assertThat(base.getTimeout()).isEqualTo(baseTimeoutBefore);
        }

        @Test
        @DisplayName("apply 返回新对象（非 base 同一引用）")
        void apply_returnsNewInstance() {
            ResilienceProfile base = buildBaseProfile();

            ResilienceProfile result = applier.apply(base, ResilienceMode.STANDARD);

            assertThat(result).isNotSameAs(base);
        }
    }

    // ===== Helper methods =====

    /**
     * 构造 base 画像：标识/会话亲和/锁定模型用特定值，专家字段用占位值
     * （便于验证被档位覆盖且 base 不被修改）。
     */
    private ResilienceProfile buildBaseProfile() {
        ResilienceProfile base = new ResilienceProfile();
        base.setId(10L);
        base.setCode("custom-app");
        base.setName("自定义应用画像");
        // 专家字段占位值（不同于任何档位推导值，便于断言被覆盖）
        base.setMode(ResilienceMode.STANDARD);
        base.setEnableL2ModelDegradation(false);
        base.setDegradationMaxDepth(0);
        base.setTimeout(999);
        // 非专家字段（应保留 base）
        base.setEnableSessionAffinity(true);
        base.setSessionAffinityTtlMinutes(45);
        base.setEnablePinnedModel(true);
        base.setPinnedModelId(7L);
        return base;
    }
}
