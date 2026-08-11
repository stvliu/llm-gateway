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
// 任务 9.3：ChannelCard 嵌入 HealthDot
//
// 验证目标：ChannelCard 在状态 Tag 右侧应渲染 HealthDot；
// 卡片传入 lastHealthStatus="HEALTHY" 时圆点 backgroundColor=#52c41a。
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

describe('ChannelCard 嵌入 HealthDot（任务 9.3）', () => {
  it('应在卡片中渲染 HealthDot 元素（data-testid=health-dot）', () => {
    const channel = buildMockChannel({
      lastHealthStatus: 'HEALTHY',
      lastHealthCheckAt: '2026-06-13T10:00:00Z',
      lastHealthSource: 'DRAWER',
    });

    render(
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

    const dot = screen.getByTestId('health-dot');
    expect(dot).toBeInTheDocument();
    // HEALTHY → 绿
    expect((dot as HTMLElement).style.backgroundColor).toBe('rgb(82, 196, 26)');
  });

  it('未提供健康字段时仍应渲染 HealthDot（UNKNOWN 空心）', () => {
    const channel = buildMockChannel();
    render(
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
    const dot = screen.getByTestId('health-dot');
    expect(dot).toBeInTheDocument();
    // UNKNOWN：transparent 在 jsdom 上序列化为空字符串
    const bg = (dot as HTMLElement).style.backgroundColor;
    expect(bg === '' || bg === 'transparent' || bg === 'rgba(0, 0, 0, 0)').toBe(true);
  });
});
