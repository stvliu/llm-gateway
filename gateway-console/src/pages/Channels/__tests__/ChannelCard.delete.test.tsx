/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// ChannelCard 删除整个渠道危险确认测试（任务 8.7）
//
// 验证目标：
// 1) 点击 ChannelCard 行内红色删除按钮 → 弹出 useDangerConfirm 风格的 Modal.confirm
// 2) Modal 描述文案与 RETIRED 风格对齐：包含"不再参与任何流量分配"+"无法恢复"等业务影响
//    而不是过去的简单"此操作不可撤销"
// 3) 确认前不调 onDelete；点击确认按钮后才调 onDelete(channel.id)
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
    (globalThis as unknown as { ResizeObserver: typeof ResizeObserverStub }).ResizeObserver =
      ResizeObserverStub;
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

describe('ChannelCard 删除整个渠道危险确认（任务 8.7）', () => {
  it('点击删除按钮 → 弹出 Modal.confirm 含"不再参与任何流量分配"等业务影响文案', async () => {
    const onDelete = vi.fn();
    const user = userEvent.setup();
    render(
      <AntApp>
        <ChannelCard
          channel={buildMockChannel('SUSPENDED')}
          onClick={vi.fn()}
          onDelete={onDelete}
          onToggleState={vi.fn()}
          onTest={vi.fn()}
          onStateTransition={vi.fn()}
        />
      </AntApp>
    );

    // 删除入口在 Dropdown 菜单内：点开 MoreOutlined 触发器 → 点"删除"菜单项
    const allBtns = screen.getAllByRole('button');
    const dropdownTrigger = allBtns.find((b) => b.querySelector('.anticon-more'));
    expect(dropdownTrigger).toBeDefined();
    await user.click(dropdownTrigger!);
    const deleteItem = await screen.findByRole('menuitem', { name: /删\s*除/ });
    await user.click(deleteItem);

    // 弹出 Modal.confirm：description 应包含与 RETIRED 对齐的业务影响文案
    // 关键短语："不再参与任何流量分配" 或 "无法恢复"
    await waitFor(() => {
      expect(screen.getByText(/不再参与任何流量分配|无法恢复/)).toBeInTheDocument();
    });

    // 确认前 onDelete 不被调用
    expect(onDelete).not.toHaveBeenCalled();

    // 点击 modal footer 中 dangerous OK 按钮（useDangerConfirm okText = "删除"）
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);

    await waitFor(() => {
      expect(onDelete).toHaveBeenCalledWith(1);
    });
  });
});
