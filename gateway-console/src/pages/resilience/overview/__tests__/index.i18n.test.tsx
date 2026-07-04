// 任务 4.11c 技术债偿还：总览页 i18n 硬编码回归测试
//
// 验证目标：耗尽告警区域的硬编码中文应改走 i18n：
// - 事件级 exhausted 告警 message 应来自 i18n key（含插值 count）
// - 事件项文案「渠道」「候选耗尽」应走 i18n
// - 「…另有 N 起」提示应走 i18n
//
// 策略：mock react-i18next 的 t，让 t() 返回带 `i18n:` 前缀的标记串。
// 硬编码字符串不含前缀，i18n 调用含前缀，借此精确区分。
import { describe, it, expect, beforeAll, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { App as AntApp } from 'antd';
import type { FailoverEvent } from '@/types/resilience';

// mock react-i18next：t(key, opts) 返回 `i18n:<key>`，插值用 opts 中 count
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: Record<string, unknown>) => {
      if (opts && typeof opts.count === 'number') {
        return `i18n:${key}[count=${opts.count}]`;
      }
      return `i18n:${key}`;
    },
    i18n: { language: 'zh-CN' },
  }),
}));

// mock useResilience hooks：注入 exhausted 事件
vi.mock('@/services/query/useResilience', () => ({
  useFailoverEvents: vi.fn(() => ({
    data: [
      {
        id: 1,
        exhausted: false,
        decision: 'L1',
        fromChannelId: 10,
        toChannelId: 11,
        errorType: 'UPSTREAM_5XX',
        occurredAt: '2026-06-22T00:00:00Z',
        traceId: 'abcdef1234567890',
      } as FailoverEvent,
    ],
    isLoading: false,
    isFetching: false,
    refetch: vi.fn(),
  })),
  useExhaustedEvents: vi.fn(() => ({
    // 7 条 exhausted 事件，触发「…另有 2 起」分支（slice(0,5) 后剩 2）
    data: Array.from({ length: 7 }, (_, i) => ({
      id: 100 + i,
      exhausted: true,
      decision: 'L2',
      fromChannelId: 20 + i,
      occurredAt: '2026-06-22T01:00:00Z',
      traceId: 'trace' + i,
    })) as FailoverEvent[],
    isLoading: false,
  })),
}));

import OverviewPage from '@/pages/resilience/overview';

beforeAll(() => {
  if (!window.matchMedia) {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: (query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: () => {},
        removeListener: () => {},
        addEventListener: () => {},
        removeEventListener: () => {},
        dispatchEvent: () => false,
      }),
    });
  }
  if (!(globalThis as { ResizeObserver?: unknown }).ResizeObserver) {
    class ResizeObserverStub {
      observe(): void {}
      unobserve(): void {}
      disconnect(): void {}
    }
    (globalThis as unknown as { ResizeObserver: typeof ResizeObserverStub }).ResizeObserver = ResizeObserverStub;
  }
});

describe('OverviewPage 耗尽告警 i18n（技术债偿还）', () => {
  it('事件级 exhausted 告警 message 应走 i18n key（含 count 插值）', () => {
    render(
      <AntApp>
        <OverviewPage />
      </AntApp>,
    );
    // 7 条 exhausted → t('overview.exhaustedEventsAlert', { count: 7 })
    expect(
      screen.getByText('i18n:overview.exhaustedEventsAlert[count=7]'),
    ).toBeInTheDocument();
  });

  it('exhausted 事件项应走 i18n（渠道/候选耗尽/Trace 标记出现）', () => {
    const { container } = render(
      <AntApp>
        <OverviewPage />
      </AntApp>,
    );
    // 事件项文案应来自 i18n key overview.exhaustedEventItem
    const text = container.textContent ?? '';
    expect(text).toContain('i18n:overview.exhaustedEventItem');
    // 硬编码「渠道」单独成词不应出现（i18n 渲染后是标记串）
    expect(text).not.toMatch(/— 渠道 /);
  });

  it('「…另有 N 起」提示应走 i18n key（含 count 插值）', () => {
    render(
      <AntApp>
        <OverviewPage />
      </AntApp>,
    );
    // 7 - 5 = 2 → t('overview.moreExhaustedEvents', { count: 2 })
    expect(
      screen.getByText('i18n:overview.moreExhaustedEvents[count=2]'),
    ).toBeInTheDocument();
  });
});
