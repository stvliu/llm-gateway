// ChannelCard 暂停操作二次确认测试（任务 8.3）
//
// 验证目标：
// 1) 用户从 Dropdown 菜单选择 "暂停" → 应弹出二次确认（含描述："暂停后该渠道不再分配流量，但保留配置"）
// 2) 确认前不应直接调用 onStateTransition；点击确认按钮后才调用 targetState=SUSPENDED
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeAll } from 'vitest';
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

function buildMockChannel(state: ChannelCardType['state']): ChannelCardType {
  return {
    id: 1,
    providerId: 1,
    providerName: 'OpenAI',
    name: 'mock-channel',
    billingMode: 'pay_as_you_go',
    quotaLimit: null,
    priority: 1,
    weight: 1,
    timeout: null,
    maxRetries: null,
    state,
    endpoints: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    stats: { endpointCount: 0, credentialCount: 0, modelCount: 0, avgResponseTime: null },
  };
}

describe('ChannelCard 暂停操作二次确认', () => {
  it('从 Dropdown 选择"暂停"应弹出二次确认含描述，且需点击确认才触发 onStateTransition', async () => {
    const onStateTransition = vi.fn();
    render(
      <AntApp>
        <ChannelCard
          channel={buildMockChannel('ACTIVE')}
          onClick={vi.fn()}
          onDelete={vi.fn()}
          onToggleState={vi.fn()}
          onTest={vi.fn()}
          onStateTransition={onStateTransition}
        />
      </AntApp>
    );

    const user = userEvent.setup();

    // 点开 More Dropdown（trigger=click）。Dropdown 触发按钮无名称，根据 .ant-dropdown-trigger 定位
    // 这里用 MoreOutlined 图标按钮（最后一个 size=small 的 type=text 按钮）
    const moreBtns = screen.getAllByRole('button');
    // 找到含有 anticon-more 子节点的按钮
    const dropdownTrigger = moreBtns.find((b) => b.querySelector('.anticon-more'));
    expect(dropdownTrigger).toBeDefined();
    await user.click(dropdownTrigger!);

    // 等待菜单展开后选 "暂停"
    const suspendItem = await screen.findByText('暂停');
    await user.click(suspendItem);

    // 应弹出二次确认（描述含"暂停后该渠道不再分配流量"）
    await waitFor(() => {
      expect(screen.getByText(/暂停后该渠道不再分配流量/)).toBeInTheDocument();
    });

    // 此时 onStateTransition 还未被调用
    expect(onStateTransition).not.toHaveBeenCalled();

    // 点击确认按钮（OK）
    const confirmBtns = screen.getAllByRole('button', { name: /^确定$|^OK$|^确认$/ });
    expect(confirmBtns.length).toBeGreaterThan(0);
    await user.click(confirmBtns[confirmBtns.length - 1]);

    await waitFor(() => {
      expect(onStateTransition).toHaveBeenCalledWith(1, 'SUSPENDED', '');
    });
  });
});
