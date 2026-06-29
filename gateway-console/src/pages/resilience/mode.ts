/**
 * 容灾档位推导逻辑（「选而非填」范式核心）
 *
 * <p>管理员只面对「容灾模式」三档位 + 「降级兜底」开关，其余字段由档位自动推导。
 * 依据 docs/容灾管理范式.md 第四节「档位 → ResilienceProfile 字段推导表」。</p>
 *
 * <p>本模块为纯函数，无副作用，便于单测与复用。</p>
 */
import type {
  ResilienceProfile,
  ResilienceMode,
  DegradationFallback,
} from '@/types/resilience';

/**
 * 档位 → 默认 ResilienceProfile 可编辑字段推导
 *
 * <p>管理员选档位后，前端用此函数预填表单默认值，管理员可再微调「降级兜底」深度。</p>
 *
 * @param mode 容灾模式档位
 * @returns 该档位对应的默认 profile 字段（不含 code/name/id 等标识字段）
 */
export function modeToProfileDefaults(
  mode: ResilienceMode,
): Pick<
  ResilienceProfile,
  | 'mode'
  | 'enableL2ModelDegradation'
  | 'degradationMaxDepth'
  | 'enableSessionAffinity'
  | 'sessionAffinityTtlMinutes'
  | 'enablePinnedModel'
  | 'pinnedModelId'
  | 'timeout'
> {
  switch (mode) {
    case 'STRICT':
      // 严格型：L2 关闭，宁可报错不可换模型；短超时 60s
      return {
        mode: 'STRICT',
        enableL2ModelDegradation: false,
        degradationMaxDepth: 0,
        enableSessionAffinity: false,
        sessionAffinityTtlMinutes: 0,
        enablePinnedModel: false,
        pinnedModelId: null,
        timeout: 60,
      };
    case 'AGGRESSIVE':
      // 激进型：L2 深降级深度3，短超时 15s，可用性优先
      return {
        mode: 'AGGRESSIVE',
        enableL2ModelDegradation: true,
        degradationMaxDepth: 3,
        enableSessionAffinity: false,
        sessionAffinityTtlMinutes: 0,
        enablePinnedModel: false,
        pinnedModelId: null,
        timeout: 15,
      };
    case 'STANDARD':
    default:
      // 标准型：L1 全开 + L2 浅降级深度2，平衡可用性与质量；超时用渠道默认(0)
      return {
        mode: 'STANDARD',
        enableL2ModelDegradation: true,
        degradationMaxDepth: 2,
        enableSessionAffinity: false,
        sessionAffinityTtlMinutes: 0,
        enablePinnedModel: false,
        pinnedModelId: null,
        timeout: 0,
      };
  }
}

/**
 * 从 ResilienceProfile 推导面向管理员的「降级兜底」两字段
 *
 * <p>「降级兜底」= { enabled, maxDepth }。enabled=false 等价于
 * enableL2ModelDegradation=false + degradationMaxDepth=0。</p>
 *
 * @param profile 容灾画像（或含降级字段的子集）
 * @returns 面向管理员的降级兜底展示字段
 */
export function deriveFallbackFromProfile(
  profile: Pick<ResilienceProfile, 'enableL2ModelDegradation' | 'degradationMaxDepth'>,
): DegradationFallback {
  if (!profile.enableL2ModelDegradation) {
    return { enabled: false, maxDepth: 0 };
  }
  return { enabled: true, maxDepth: profile.degradationMaxDepth };
}

/** 档位中文标签 */
export function modeLabel(mode: ResilienceMode): string {
  switch (mode) {
    case 'STRICT':
      return '严格';
    case 'AGGRESSIVE':
      return '激进';
    case 'STANDARD':
    default:
      return '标准';
  }
}

/** 档位 AntD Tag 颜色 */
export function modeColor(mode: ResilienceMode): string {
  switch (mode) {
    case 'STRICT':
      return 'red';
    case 'AGGRESSIVE':
      return 'orange';
    case 'STANDARD':
    default:
      return 'blue';
  }
}
