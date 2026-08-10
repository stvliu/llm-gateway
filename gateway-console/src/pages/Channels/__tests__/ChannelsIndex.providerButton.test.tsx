/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// 任务 10.6：主页面移除独立"+ 新增供应商"按钮
//
// 验证：渲染 Channels 主页后，不应存在 name 包含"新增供应商"的按钮。
import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeAll, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { I18nextProvider } from 'react-i18next';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import i18n from '@/i18n';

// mock 数据钩子，避免真实网络
vi.mock('@/services/query/useChannels', () => {
  const noopQuery = { data: undefined, isLoading: false, isError: false };
  const noopMutation = {
    mutate: vi.fn(),
    mutateAsync: vi.fn().mockResolvedValue(undefined),
    isPending: false,
  };
  const fakeHook = () => ({ ...noopQuery, ...noopMutation });
  return {
    channelKeys: { all: ['channels'] },
    useAllChannels: () => ({ data: [], isLoading: false }),
    useChannels: fakeHook,
    useChannelsBatch: () => [],
    useChannel: fakeHook,
    useCreateChannel: () => noopMutation,
    useUpdateChannel: () => noopMutation,
    useTransitionChannelState: () => noopMutation,
    useTransitionChannelModelState: () => noopMutation,
    useDeleteChannel: () => noopMutation,
    useAddChannelEndpoint: () => noopMutation,
    useRemoveChannelEndpoint: () => noopMutation,
    useUpdateChannelEndpoint: () => noopMutation,
    useChannelCredentials: fakeHook,
    useChannelCredentialsBatch: () => [],
    useCreateChannelCredential: () => noopMutation,
    useUpdateChannelCredential: () => noopMutation,
    useDeleteChannelCredential: () => noopMutation,
    useTestChannelCredential: () => noopMutation,
    useAddChannel: () => noopMutation,
    useChannelModels: fakeHook,
    useCreateChannelModel: () => noopMutation,
    useDeleteChannelModel: () => noopMutation,
    useUpdateChannelModel: () => noopMutation,
    useChannelModelsBatch: () => [],
  };
});

vi.mock('@/services/query/useProviders', () => {
  const noopQuery = { data: undefined, isLoading: false };
  const noopMutation = { mutate: vi.fn(), mutateAsync: vi.fn().mockResolvedValue(undefined), isPending: false };
  return {
    providerKeys: { all: ['providers'] },
    useProviders: () => ({ data: { items: [] }, isLoading: false }),
    useProvider: () => noopQuery,
    useCreateProvider: () => noopMutation,
    useUpdateProvider: () => noopMutation,
    useDeleteProvider: () => noopMutation,
    useTestConnectivity: () => noopMutation,
  };
});

// 解决 ProviderIcon 的 lobehub 子模块解析问题
vi.mock('@/components/ui', () => ({
  ProviderIcon: ({ providerId }: { providerId: string }) => (
    <span data-testid={`provider-icon-${providerId}`} />
  ),
}));

import Channels from '@/pages/Channels';

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

describe('Channels 主页面 (任务 10.6)', () => {
  it('主页面不应有独立的"+ 新增供应商"按钮、Dropdown 菜单项或底部新增卡片', async () => {
    // 用一条假的渠道数据让 grouped 视图分支生效，
    // 这样底部"新增供应商"虚线卡片若仍存在就会被渲染出来。
    const realUseChannels = await import('@/services/query/useChannels');
    vi.spyOn(realUseChannels, 'useAllChannels').mockReturnValue({
      data: [
        {
          id: 1,
          providerId: 1,
          providerName: 'OpenAI',
          name: 'mock-channel',
          state: 'ACTIVE' as const,
          billingMode: 'pay_as_you_go',
          quotaLimit: null,
          priority: 1,
          weight: 1,
          timeout: null,
          maxRetries: null,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          endpoints: [],
          lastHealthStatus: 'UNKNOWN',
          lastHealthCheckAt: null,
          lastHealthSource: null,
        },
      ],
      isLoading: false,
    } as never);
    const realUseProviders = await import('@/services/query/useProviders');
    vi.spyOn(realUseProviders, 'useProviders').mockReturnValue({
      data: { items: [{ id: 1, providerId: 'openai', providerName: 'OpenAI', state: 'ACTIVE' }], total: 1 },
      isLoading: false,
    } as any);

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { container } = render(
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <QueryClientProvider client={queryClient}>
            <MemoryRouter>
              <Channels />
            </MemoryRouter>
          </QueryClientProvider>
        </AntApp>
      </I18nextProvider>,
    );

    // 不应有任何含"新增供应商"文案的可点击元素（按钮 / 链接 / 菜单项 / 卡片）
    expect(screen.queryByRole('button', { name: /新增供应商/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('menuitem', { name: /新增供应商/ })).not.toBeInTheDocument();
    // 底部"新增供应商"虚线卡片：通过文案兜底检查
    const allText = container.textContent ?? '';
    expect(allText).not.toContain('新增供应商');
  });
});
