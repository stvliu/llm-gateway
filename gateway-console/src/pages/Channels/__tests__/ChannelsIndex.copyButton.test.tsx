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
// Channels 列表行内复制按钮测试
//
// 验证：表格视图下点击行内复制按钮（CopyOutlined）后，复制对话框打开且源为当前行渠道。
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/i18n';
import Channels from '@/pages/Channels';
import type { ChannelCard } from '@/types/channel';

// 一条渠道数据渲染表格行
const { channel, copyModalProps } = vi.hoisted(() => ({
  channel: {
    id: 1,
    providerId: 10,
    providerName: 'OpenAI',
    name: 'ch-1',
    billingMode: 'pay_as_you_go',
    quotaLimit: null,
    priority: 100,
    weight: 100,
    timeout: null,
    maxRetries: null,
    state: 'ACTIVE',
    endpoints: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    stats: { endpointCount: 0, credentialCount: 0, modelCount: 0, avgResponseTime: null },
  } as ChannelCard,
  copyModalProps: {
    current: null as { open: boolean; source: { id: number; name: string } | null } | null,
  },
}));

vi.mock('@/services/query/useChannels', () => {
  const noopMutation = { mutate: vi.fn(), mutateAsync: vi.fn(), isPending: false };
  return {
    channelKeys: { all: ['channels'] },
    useAllChannels: () => ({ data: [channel], isLoading: false }),
    useChannels: () => ({ data: [], isLoading: false }),
    useChannel: () => ({ data: undefined, isLoading: false }),
    useChannelModelsBatch: () => [],
    useChannelCredentialsBatch: () => [],
    useDeleteChannel: () => noopMutation,
    useTransitionChannelState: () => noopMutation,
    useTestChannelCredential: () => noopMutation,
  };
});

vi.mock('@/services/query/useProviders', () => ({
  providerKeys: { all: ['providers'] },
  useProviders: () => ({
    data: { items: [{ id: 10, providerId: 'openai', providerName: 'OpenAI' }], pagination: { page: 1, limit: 100, total: 1, totalPages: 1 } },
    isLoading: false,
  }),
}));

// 记录 CopyChannelModal 收到的 props，避免真实复制对话框及其依赖
vi.mock('@/pages/Channels/CopyChannelModal', () => ({
  __esModule: true,
  default: (props: { open: boolean; source: { id: number; name: string } | null }) => {
    copyModalProps.current = props;
    return props.open ? (
      <div data-testid="copy-modal" data-source-id={props.source?.id} />
    ) : null;
  },
}));

// 简化其它子组件，避免其自身 hook 依赖
vi.mock('@/pages/Channels/ChannelDetailDrawer', () => ({
  __esModule: true,
  ChannelDetailDrawer: () => <div data-testid="drawer" />,
}));
vi.mock('@/pages/Channels/ChannelCreateWizard', () => ({
  __esModule: true,
  ChannelCreateWizard: () => <div data-testid="wizard" />,
}));
vi.mock('@/pages/Channels/ProviderEditModal', () => ({
  __esModule: true,
  ProviderEditModal: () => <div data-testid="provider-edit" />,
}));
vi.mock('@/pages/Channels/BatchImportModal', () => ({
  __esModule: true,
  default: () => <div data-testid="batch-import" />,
}));
vi.mock('@/pages/Channels/BatchExportButton', () => ({
  __esModule: true,
  BatchExportButton: () => <div data-testid="batch-export" />,
}));
vi.mock('@/pages/Channels/ConnectivityTestPanel', () => ({
  __esModule: true,
  ConnectivityTestPanel: () => <div data-testid="connectivity" />,
}));

// ProviderIcon 走 @lobehub/ui 链（jsdom 下 ESM 目录导入不兼容），与既有 index 测试一致用 stub
vi.mock('@/components/ui', () => ({
  ProviderIcon: ({ providerId }: { providerId: string }) => (
    <span data-testid={`provider-icon-${providerId}`} />
  ),
}));

beforeAll(async () => {
  // antd 依赖 matchMedia / ResizeObserver，jsdom 默认不提供
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

beforeEach(() => {
  // 预设表格视图（默认分组视图不渲染 ChannelTableView）
  localStorage.setItem('channel-view-mode', 'table');
  copyModalProps.current = null;
});

describe('Channels 列表行内复制按钮', () => {
  // 全页 Table 渲染较重，放宽超时（慢速 CI/Windows jsdom）
  it('点击复制按钮后打开复制对话框并携带当前行渠道', async () => {
    render(
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <Channels />
        </AntApp>
      </I18nextProvider>,
    );

    // 通过 CopyOutlined 图标的 aria-label 定位行内复制按钮
    const copyBtn = screen.getByRole('img', { name: 'copy' }).closest('button');
    expect(copyBtn).not.toBeNull();
    await userEvent.click(copyBtn!);

    // 复制对话框打开且源为当前行渠道
    expect(screen.getByTestId('copy-modal')).toHaveAttribute('data-source-id', '1');
    expect(copyModalProps.current?.open).toBe(true);
    expect(copyModalProps.current?.source).toEqual(
      expect.objectContaining({ id: 1, name: 'ch-1' }),
    );
  }, 20000);
});
