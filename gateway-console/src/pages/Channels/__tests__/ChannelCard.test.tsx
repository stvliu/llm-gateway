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
// ChannelCard 视觉测试
//
// 任务 6.6 / 6.7：
// - RETIRED 渠道：渠道名应有 line-through + 灰色 #8c8c8c；卡片不再统一 opacity 0.5 降透
// - DEPRECATED 渠道：渠道名下方应展示副标题 "仍参与流量分配"
//
// jsdom 不实现 matchMedia / ResizeObserver，AntD Card / Dropdown 在挂载时使用 → beforeAll 补 stub。
import { render, screen } from '@testing-library/react';
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

/** 构造一份基础 mock channel；调用方按状态覆盖 state 字段 */
function buildMockChannel(overrides: Partial<ChannelCardType> = {}): ChannelCardType {
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

function renderCard(channel: ChannelCardType) {
  return render(
    <AntApp>
      <ChannelCard
        channel={channel}
        onClick={vi.fn()}
        onDelete={vi.fn()}
        onToggleState={vi.fn()}
        onTest={vi.fn()}
      />
    </AntApp>,
  );
}

describe('ChannelCard 视觉重设', () => {
  it('RETIRED 渠道名应有 line-through 且无 opacity 整体降透', () => {
    const channel = buildMockChannel({ state: 'RETIRED', name: 'retired-channel' });
    const { container } = renderCard(channel);

    // 名称带删除线 + 灰色
    const name = screen.getByText('retired-channel');
    expect(name).toHaveStyle({ textDecoration: 'line-through' });
    expect(name).toHaveStyle({ color: 'rgb(140, 140, 140)' }); // #8c8c8c

    // 卡片整体不再 opacity 0.5
    const card = container.querySelector('.ant-card') as HTMLElement | null;
    expect(card).not.toBeNull();
    expect(card).not.toHaveStyle({ opacity: '0.5' });
  });

  it('DEPRECATED 卡片应显示副标题"仍参与流量分配"', () => {
    const channel = buildMockChannel({ state: 'DEPRECATED', name: 'deprecated-channel' });
    renderCard(channel);

    // 副标题文案：来自 channels.json deprecatedSubtitle
    expect(screen.getByText(/仍参与流量分配/)).toBeInTheDocument();
  });

  it('ACTIVE 卡片不应展示 DEPRECATED 副标题，也不应有 line-through', () => {
    const channel = buildMockChannel({ state: 'ACTIVE', name: 'active-channel' });
    renderCard(channel);

    expect(screen.queryByText(/仍参与流量分配/)).not.toBeInTheDocument();
    const name = screen.getByText('active-channel');
    expect(name).not.toHaveStyle({ textDecoration: 'line-through' });
  });
});
