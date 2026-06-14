// 任务 9.1：渠道卡片闪电图标行为改造
//
// 验证目标：
// 1) 点击闪电图标 → 触发 onTest(channel, { tab: 'credentials', highlightTestAll: true })
//    （父级负责打开详情抽屉到 Credentials Tab 并高亮"测试全部"按钮）
// 2) 不再就地弹 toast / 调 testCredential（卡片只透传一个明确意图，不做副作用）
//
// jsdom 不实现 matchMedia / ResizeObserver，AntD Card / Dropdown 在挂载时使用 → beforeAll 补 stub。
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, vi } from 'vitest';
import { App as AntApp } from 'antd';
import i18n from '@/i18n';
import { ChannelCard } from '@/pages/Channels/ChannelCard';
import type { ChannelCard as ChannelCardType } from '@/types/channel';

beforeAll(async () => {
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
  await i18n.changeLanguage('zh-CN');
});

/** 构造一份基础 mock channel（ACTIVE 默认 isRoutable=true 才能点测试按钮） */
function buildMockChannel(overrides: Partial<ChannelCardType> = {}): ChannelCardType {
  return {
    id: 42,
    providerId: 1,
    providerName: 'OpenAI',
    name: 'mock-channel',
    billingMode: 'pay_as_you_go',
    quotaLimit: null,
    priority: 1,
    weight: 1,
    timeout: null,
    maxRetries: null,
    state: 'ACTIVE',
    endpoints: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    stats: {
      endpointCount: 0,
      credentialCount: 0,
      modelCount: 0,
      avgResponseTime: null,
    },
    ...overrides,
  };
}

describe('ChannelCard 闪电图标（任务 9.1）', () => {
  it('点击闪电图标应触发 onTest，并携带 { tab: "credentials", highlightTestAll: true } 意图', async () => {
    const onTest = vi.fn();
    const onClick = vi.fn();
    const channel = buildMockChannel();

    render(
      <AntApp>
        <ChannelCard
          channel={channel}
          onClick={onClick}
          onDelete={vi.fn()}
          onToggleState={vi.fn()}
          onTest={onTest}
        />
      </AntApp>,
    );

    // 闪电图标用 Tooltip 包裹，按"连通性测试" title 寻找按钮
    // antd Button 的 aria-label 不一定有，转而用 tooltip 文案匹配 button
    const buttons = screen.getAllByRole('button');
    // 闪电图标按钮是第一个（测试按钮）；详情按钮、删除按钮在后
    const testButton = buttons[0];
    await userEvent.click(testButton);

    // 验证：onTest 被调用
    expect(onTest).toHaveBeenCalledTimes(1);
    // 验证：onTest 第一参为 channel；第二参为意图对象
    const [calledChannel, intent] = onTest.mock.calls[0];
    expect(calledChannel).toMatchObject({ id: 42 });
    // 关键不变量：明确指示父级要打开抽屉到 Credentials Tab + 高亮"测试全部"
    expect(intent).toMatchObject({ tab: 'credentials', highlightTestAll: true });

    // 闪电图标 click 不应等价于卡片整体 click（卡片整体 click 才走 onClick）
    expect(onClick).not.toHaveBeenCalled();
  });
});
