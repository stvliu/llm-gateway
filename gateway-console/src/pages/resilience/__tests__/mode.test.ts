// 容灾档位推导逻辑单元测试
//
// 任务 4.11b：验证「选而非填」范式的核心纯函数：
// - modeToProfileDefaults：三档位 → 默认 ResilienceProfile 字段（管理员选档位即得默认配置）
// - deriveFallbackFromProfile：从 profile 推导面向管理员的「降级兜底」两字段
// - modeLabel/modeColor：档位展示元数据
//
// 依据：docs/容灾管理范式.md 第四节「档位 → ResilienceProfile 字段推导表」
import { describe, it, expect } from 'vitest';
import {
  modeToProfileDefaults,
  deriveFallbackFromProfile,
  modeLabel,
  modeColor,
} from '../mode';
import type { ResilienceMode } from '@/types/resilience';

describe('modeToProfileDefaults', () => {
  it('STANDARD 档位：L2 浅降级 深度2', () => {
    const defaults = modeToProfileDefaults('STANDARD');
    expect(defaults.enableL2ModelDegradation).toBe(true);
    expect(defaults.degradationMaxDepth).toBe(2);
  });

  it('STRICT 档位：L2 关闭 深度0', () => {
    const defaults = modeToProfileDefaults('STRICT');
    expect(defaults.enableL2ModelDegradation).toBe(false);
    expect(defaults.degradationMaxDepth).toBe(0);
  });

  it('AGGRESSIVE 档位：L2 深降级 深度3', () => {
    const defaults = modeToProfileDefaults('AGGRESSIVE');
    expect(defaults.enableL2ModelDegradation).toBe(true);
    expect(defaults.degradationMaxDepth).toBe(3);
  });

  it('所有档位 timeout 合理 STANDARD无限制 AGGRESSIVE短超时', () => {
    expect(modeToProfileDefaults('STANDARD').timeout).toBe(0);
    expect(modeToProfileDefaults('STRICT').timeout).toBe(60);
    expect(modeToProfileDefaults('AGGRESSIVE').timeout).toBe(15);
  });

  it('所有档位默认不开启会话亲和与模型锁定', () => {
    (['STANDARD', 'STRICT', 'AGGRESSIVE'] as ResilienceMode[]).forEach((mode) => {
      const d = modeToProfileDefaults(mode);
      expect(d.enableSessionAffinity).toBe(false);
      expect(d.enablePinnedModel).toBe(false);
      expect(d.sessionAffinityTtlMinutes).toBe(0);
    });
  });
});

describe('deriveFallbackFromProfile', () => {
  it('enableL2ModelDegradation=false 时兜底关闭 maxDepth=0', () => {
    const fallback = deriveFallbackFromProfile({
      enableL2ModelDegradation: false,
      degradationMaxDepth: 5,
    });
    expect(fallback.enabled).toBe(false);
    expect(fallback.maxDepth).toBe(0);
  });

  it('enableL2ModelDegradation=true 时兜底开启 maxDepth 取 profile 值', () => {
    const fallback = deriveFallbackFromProfile({
      enableL2ModelDegradation: true,
      degradationMaxDepth: 3,
    });
    expect(fallback.enabled).toBe(true);
    expect(fallback.maxDepth).toBe(3);
  });
});

describe('档位展示元数据', () => {
  it('modeLabel 返回三档位中文标签', () => {
    expect(modeLabel('STANDARD')).toBe('标准');
    expect(modeLabel('STRICT')).toBe('严格');
    expect(modeLabel('AGGRESSIVE')).toBe('激进');
  });

  it('modeColor 返回档位颜色标识', () => {
    expect(modeColor('STANDARD')).toBe('blue');
    expect(modeColor('STRICT')).toBe('red');
    expect(modeColor('AGGRESSIVE')).toBe('orange');
  });
});
