// 容灾总览转移事件展示辅助函数单元测试
//
// 任务 4.11c：验证事件流展示纯函数：
// - errorTypeMeta：按 ProviderErrorType 返回 Tag 颜色与文案 key
// - formatRoute：from→to 渠道路径（exhausted 时显示"耗尽无目标"）
// - decisionMeta：L1/L2 决策 Tag 颜色
//
// 策略：纯函数无副作用，直接断言映射结果。
import { describe, it, expect } from 'vitest';
import {
  errorTypeMeta,
  formatRoute,
  decisionMeta,
} from '../eventDisplay';
import type { FailoverEvent } from '@/types/resilience';

describe('errorTypeMeta', () => {
  it('限流/配额类错误映射为 orange', () => {
    expect(errorTypeMeta('RATE_LIMIT_ERROR').color).toBe('orange');
    expect(errorTypeMeta('QUOTA_EXCEEDED').color).toBe('orange');
  });

  it('认证/请求格式错误映射为 red', () => {
    expect(errorTypeMeta('AUTHENTICATION_ERROR').color).toBe('red');
    expect(errorTypeMeta('INVALID_REQUEST').color).toBe('red');
  });

  it('超时/网络/上游不可用映射为 volcano', () => {
    expect(errorTypeMeta('TIMEOUT_ERROR').color).toBe('volcano');
    expect(errorTypeMeta('NETWORK_ERROR').color).toBe('volcano');
    expect(errorTypeMeta('SERVICE_UNAVAILABLE').color).toBe('volcano');
    expect(errorTypeMeta('UPSTREAM_ERROR').color).toBe('volcano');
  });

  it('未知错误类型回退 default', () => {
    expect(errorTypeMeta('UNKNOWN_ERROR').color).toBe('default');
    expect(errorTypeMeta('SOMETHING_NEW').color).toBe('default');
    expect(errorTypeMeta(null).color).toBe('default');
    expect(errorTypeMeta(undefined).color).toBe('default');
  });
});

describe('formatRoute', () => {
  it('有 from 和 to 渠道时显示 from→to', () => {
    const ev: FailoverEvent = {
      id: 1,
      exhausted: false,
      occurredAt: '2026-06-22T00:00:00Z',
      fromChannelId: 10,
      toChannelId: 20,
    };
    expect(formatRoute(ev)).toBe('10 → 20');
  });

  it('exhausted 且无 to 渠道时显示耗尽无目标标记', () => {
    const ev: FailoverEvent = {
      id: 2,
      exhausted: true,
      occurredAt: '2026-06-22T00:00:00Z',
      fromChannelId: 10,
      toChannelId: null,
    };
    expect(formatRoute(ev)).toBe('exhaustedNoTarget');
  });

  it('exhausted 但有 to 渠道时仍显示 from→to', () => {
    const ev: FailoverEvent = {
      id: 3,
      exhausted: true,
      occurredAt: '2026-06-22T00:00:00Z',
      fromChannelId: 10,
      toChannelId: 30,
    };
    expect(formatRoute(ev)).toBe('10 → 30');
  });

  it('from 渠道缺失时显示占位', () => {
    const ev: FailoverEvent = {
      id: 4,
      exhausted: false,
      occurredAt: '2026-06-22T00:00:00Z',
      fromChannelId: null,
      toChannelId: 20,
    };
    expect(formatRoute(ev)).toBe('? → 20');
  });
});

describe('decisionMeta', () => {
  it('L1 决策映射为 blue', () => {
    expect(decisionMeta('L1').color).toBe('blue');
    expect(decisionMeta('L1').labelKey).toBe('overview.decisions.L1');
  });

  it('L2 决策映射为 purple', () => {
    expect(decisionMeta('L2').color).toBe('purple');
    expect(decisionMeta('L2').labelKey).toBe('overview.decisions.L2');
  });

  it('未知决策回退 default 且 labelKey 为原始值', () => {
    expect(decisionMeta(null).color).toBe('default');
    expect(decisionMeta('UNKNOWN').color).toBe('default');
    expect(decisionMeta('UNKNOWN').labelKey).toBe('UNKNOWN');
  });
});
