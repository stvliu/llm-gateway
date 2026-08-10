/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// 端点熔断状态大盘组件测试
//
// Task 12 Step 6：验证 CircuitBreakerDashboard：
// - 拉取全部渠道端点（useAllChannels），逐端点展示熔断状态 + 应急操作入口
// - 表格列：渠道名 / 端点 URL / 协议 / 熔断状态 + 应急按钮（复用 CircuitBreakerButton）
// - 空渠道列表显示 Empty；loading 显示 Spin
//
// 策略：mock useAllChannels 注入渠道数据，mock CircuitBreakerButton 捕获 props，
// 验证渠道 × 端点展开正确、CircuitBreakerButton 接收正确的 channelId/endpointId。
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { App as AntApp } from 'antd';
import type { Channel } from '@/types/channel';

// 用 vi.hoisted 提升 mock 函数，避免 vi.mock 工厂引用顶层变量（hoisting 限制），
// 同时让 mockReturnValue 参数类型宽松（vi.fn() 无固定签名）
const { useAllChannelsMock } = vi.hoisted(() => ({
  useAllChannelsMock: vi.fn(),
}));

// mock useAllChannels：默认注入 2 个渠道共 3 个端点
vi.mock('@/services/query/useChannels', () => ({
  useAllChannels: useAllChannelsMock,
}));

// mock CircuitBreakerButton：捕获传入的 channelId/endpointId
vi.mock('@/pages/Channels/CircuitBreakerButton', () => ({
  CircuitBreakerButton: ({ channelId, endpointId }: { channelId: number; endpointId: number }) => (
    <div data-testid={`cb-btn-${channelId}-${endpointId}`}>
      cb-{channelId}-{endpointId}
    </div>
  ),
}));

import { CircuitBreakerDashboard } from '../CircuitBreakerDashboard';

/** 默认渠道数据：2 渠道 / 3 端点 */
const defaultChannels: Channel[] = [
  {
    id: 10,
    name: 'OpenAI 主渠道',
    providerId: 1,
    providerName: 'OpenAI',
    billingMode: 'pay_as_you_go',
    quotaLimit: null,
    priority: 1,
    weight: 100,
    timeout: null,
    maxRetries: null,
    state: 'ACTIVE',
    endpoints: [
      { id: 101, channelId: 10, protocol: 'openai', endpointUrl: 'https://api.openai.com/v1', createdAt: '', updatedAt: '' },
      { id: 102, channelId: 10, protocol: 'openai', endpointUrl: 'https://backup.openai.com/v1', createdAt: '', updatedAt: '' },
    ],
    createdAt: '',
    updatedAt: '',
  },
  {
    id: 20,
    name: 'Anthropic 渠道',
    providerId: 2,
    providerName: 'Anthropic',
    billingMode: 'pay_as_you_go',
    quotaLimit: null,
    priority: 1,
    weight: 100,
    timeout: null,
    maxRetries: null,
    state: 'ACTIVE',
    endpoints: [
      { id: 201, channelId: 20, protocol: 'anthropic', endpointUrl: 'https://api.anthropic.com', createdAt: '', updatedAt: '' },
    ],
    createdAt: '',
    updatedAt: '',
  },
];

beforeAll(() => {
  // antd Table 需要 matchMedia / ResizeObserver
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

beforeEach(() => {
  useAllChannelsMock.mockReturnValue({ data: defaultChannels, isLoading: false });
});

const t = (key: string) => `i18n:${key}`;

describe('CircuitBreakerDashboard', () => {
  it('展开所有渠道端点为表格行，每行渲染 CircuitBreakerButton', () => {
    render(
      <AntApp>
        <CircuitBreakerDashboard t={t} />
      </AntApp>,
    );
    // 2 个渠道共 3 个端点 → 3 个 CircuitBreakerButton
    expect(screen.getByTestId('cb-btn-10-101')).toBeInTheDocument();
    expect(screen.getByTestId('cb-btn-10-102')).toBeInTheDocument();
    expect(screen.getByTestId('cb-btn-20-201')).toBeInTheDocument();
  });

  it('展示渠道名、端点 URL、协议', () => {
    render(
      <AntApp>
        <CircuitBreakerDashboard t={t} />
      </AntApp>,
    );
    // 渠道 10 有 2 个端点 → 渠道名出现 2 次；渠道 20 有 1 个端点
    expect(screen.getAllByText('OpenAI 主渠道')).toHaveLength(2);
    expect(screen.getAllByText('Anthropic 渠道')).toHaveLength(1);
    // 端点 URL 唯一
    expect(screen.getByText('https://api.openai.com/v1')).toBeInTheDocument();
    expect(screen.getByText('https://api.anthropic.com')).toBeInTheDocument();
    // 协议
    expect(screen.getAllByText('openai')).toHaveLength(2);
    expect(screen.getByText('anthropic')).toBeInTheDocument();
  });

  it('空渠道列表显示 Empty 提示', () => {
    useAllChannelsMock.mockReturnValueOnce({ data: [], isLoading: false });
    render(
      <AntApp>
        <CircuitBreakerDashboard t={t} />
      </AntApp>,
    );
    // i18n key overview.noEndpoints 经 t() 返回 i18n:overview.noEndpoints
    expect(screen.getByText('i18n:overview.noEndpoints')).toBeInTheDocument();
  });

  it('loading 时显示 Spin', () => {
    useAllChannelsMock.mockReturnValueOnce({ data: undefined, isLoading: true });
    const { container } = render(
      <AntApp>
        <CircuitBreakerDashboard t={t} />
      </AntApp>,
    );
    // antd Spin 渲染为 .ant-spin
    expect(container.querySelector('.ant-spin')).toBeInTheDocument();
  });
});
