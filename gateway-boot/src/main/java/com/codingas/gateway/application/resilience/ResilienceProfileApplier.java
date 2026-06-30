package com.codingas.gateway.application.resilience;

import com.codingas.gateway.domain.resilience.entity.ResilienceMode;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 容灾画像档位推导器
 *
 * <p>实现容灾模式档位 → 画像专家字段的自动推导（见 design.md D5）：
 * 管理员选档位时，按档位覆盖专家字段（mode/enableL2/degradationMaxDepth/timeout），
 * 其余字段（code/name 等）保留 base 画像原值。</p>
 *
 * <p>字段值与 V56 seed（{@code V56__seed_resilience_profiles.sql}）及
 * {@link ResilienceMode} 枚举 Javadoc 保持一致：
 * <ul>
 *   <li>STANDARD — enableL2=true, depth=2（浅降级）, timeout=0（用渠道默认）</li>
 *   <li>STRICT — enableL2=false, depth=0（L2 关闭）, timeout=60</li>
 *   <li>AGGRESSIVE — enableL2=true, depth=3（深降级）, timeout=15（短超时）</li>
 * </ul>
 * </p>
 *
 * <p>语义：apply 不修改 base（不可变），返回推导后的新 {@link ResilienceProfile}。
 * 与 Task 4.4 seed 的区别：4.4 是初始化档位画像入库；4.5 是运行时按 mode 应用档位
 * （管理员选档位时自动填充专家字段，base 可能是模板画像）。推导逻辑与 4.4 seed 一致。</p>
 */
@Component
@Slf4j
public class ResilienceProfileApplier {

    /** STANDARD 浅降级最大深度（浅降级，平衡可用性与质量） */
    private static final int STANDARD_DEGRADATION_DEPTH = 2;
    /** AGGRESSIVE 深降级最大深度（深降级，可用性优先） */
    private static final int AGGRESSIVE_DEGRADATION_DEPTH = 3;
    /** L2 关闭/禁用降级深度标记（STRICT 档位） */
    private static final int DEGRADATION_DISABLED_DEPTH = 0;
    /** STANDARD/AGGRESSIVE 用渠道默认超时标记（timeout=0 表示用渠道默认） */
    private static final int USE_CHANNEL_DEFAULT_TIMEOUT = 0;
    /** STRICT 档位超时秒数（宁可报错不可换模型，给上游足够时间） */
    private static final int STRICT_TIMEOUT_SECONDS = 60;
    /** AGGRESSIVE 档位短超时秒数（可用性优先，快速失败转移） */
    private static final int AGGRESSIVE_TIMEOUT_SECONDS = 15;

    /**
     * 按容灾模式档位推导画像专家字段
     *
     * <p>覆盖专家字段（mode/enableL2ModelDegradation/degradationMaxDepth/timeout），
     * 保留 base 的标识等非专家字段。不修改 base，返回新对象。</p>
     *
     * @param base 基础画像（提供 code/name 等保留字段）
     * @param mode 容灾模式档位（STANDARD/STRICT/AGGRESSIVE）
     * @return 按档位推导专家字段后的新画像
     */
    public ResilienceProfile apply(ResilienceProfile base, ResilienceMode mode) {
        // 复制 base 全部字段，保留标识等非专家字段
        ResilienceProfile result = copyBase(base);
        // 按档位覆盖专家字段
        result.setMode(mode);
        switch (mode) {
            case STANDARD -> {
                // 浅降级：L2 开启，深度 2，用渠道默认超时（与 V56 default 画像一致）
                result.setEnableL2ModelDegradation(true);
                result.setDegradationMaxDepth(STANDARD_DEGRADATION_DEPTH);
                result.setTimeout(USE_CHANNEL_DEFAULT_TIMEOUT);
            }
            case STRICT -> {
                // L2 关闭：不可降级，宁可报错不可换模型，给上游 60s（与 V56 strict 画像一致）
                result.setEnableL2ModelDegradation(false);
                result.setDegradationMaxDepth(DEGRADATION_DISABLED_DEPTH);
                result.setTimeout(STRICT_TIMEOUT_SECONDS);
            }
            case AGGRESSIVE -> {
                // 深降级 + 短超时：可用性优先，快速失败转移（与 V56 aggressive 画像一致）
                result.setEnableL2ModelDegradation(true);
                result.setDegradationMaxDepth(AGGRESSIVE_DEGRADATION_DEPTH);
                result.setTimeout(AGGRESSIVE_TIMEOUT_SECONDS);
            }
            default -> throw new IllegalArgumentException("不支持的容灾模式档位: " + mode);
        }
        log.debug("按档位推导画像专家字段: code={}, mode={}, enableL2={}, depth={}, timeout={}",
                base.getCode(), mode, result.isEnableL2ModelDegradation(),
                result.getDegradationMaxDepth(), result.getTimeout());
        return result;
    }

    /**
     * 复制 base 画像全部字段到新对象
     *
     * <p>浅拷贝 base 的标识/审计及专家字段（专家字段随后被档位覆盖）。
     * 审计字段（createdBy/createdAt/updatedBy/updatedAt）透传，由调用方决定是否重置。</p>
     *
     * @param base 基础画像
     * @return 字段完全复制的新画像对象
     */
    private ResilienceProfile copyBase(ResilienceProfile base) {
        ResilienceProfile copy = new ResilienceProfile();
        copy.setId(base.getId());
        copy.setCode(base.getCode());
        copy.setName(base.getName());
        copy.setMode(base.getMode());
        copy.setEnableL2ModelDegradation(base.isEnableL2ModelDegradation());
        copy.setDegradationMaxDepth(base.getDegradationMaxDepth());
        copy.setTimeout(base.getTimeout());
        copy.setCreatedBy(base.getCreatedBy());
        copy.setCreatedAt(base.getCreatedAt());
        copy.setUpdatedBy(base.getUpdatedBy());
        copy.setUpdatedAt(base.getUpdatedAt());
        return copy;
    }
}
