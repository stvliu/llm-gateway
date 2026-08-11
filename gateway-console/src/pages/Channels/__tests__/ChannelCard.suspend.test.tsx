/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// ChannelCard 暂停操作二次确认测试（任务 8.3）
//
// 验证目标：
// 1) 用户点击"暂停"主按钮（primaryAction=SUSPENDED，图标 PauseCircleOutlined）→ 应弹出二次确认（含描述："暂停后该渠道不再分配流量，但保留配置"）
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

    // ACTIVE 状态下"暂停(SUSPENDED)"为 primaryAction 主按钮（图标 PauseCircleOutlined + Tooltip），
    // 不在 More Dropdown 菜单内（Dropdown 仅含"标记下线"=DEPRECATED 与"删除"）。
    // v6 下 Tooltip title 不再渲染进 DOM，findByText('暂停') 失效；改用图标 class 定位主按钮
    // （与原 .anticon-more 定位 Dropdown 触发按钮的既定模式一致）。
    const suspendBtn = screen.getAllByRole('button').find(
      (b) => b.querySelector('.anticon-pause-circle'),
    );
    expect(suspendBtn).toBeDefined();
    await user.click(suspendBtn!);

    // 应弹出二次确认（描述含"暂停后该渠道不再分配流量"）
    await waitFor(() => {
      expect(screen.getByText(/暂停后该渠道不再分配流量/)).toBeInTheDocument();
    });

    // 此时 onStateTransition 还未被调用
    expect(onStateTransition).not.toHaveBeenCalled();

    // 点击确认按钮（OK）——放宽 name 正则以兼容 v6 Modal 按钮文本渲染
    const confirmBtn = await screen.findByRole('button', { name: /确\s*定|OK|确\s*认/i });
    await user.click(confirmBtn);

    await waitFor(() => {
      expect(onStateTransition).toHaveBeenCalledWith(1, 'SUSPENDED', '');
    });
  });
});
