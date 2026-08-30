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
// CopyChannelModal 复制渠道对话框单元测试
//
// 验证：
// 1) 打开时预填源渠道 name，默认不勾选「同时复制 API Key」
// 2) 修改 name 后提交 → mock useCopyChannel 收到 { id, data: { name, copyCredentials: false } }
// 3) 勾选「同时复制 API Key」后提交 → copyCredentials: true
// 4) name 清空时提交被校验拦截，copy 不被调用
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import i18n from '@/i18n';
import { I18nextProvider } from 'react-i18next';
import CopyChannelModal from '@/pages/Channels/CopyChannelModal';
import type { ChannelCard } from '@/types/channel';

// mock TanStack Query mutation，避免引入 QueryClientProvider 与网络调用
const { mockCopy } = vi.hoisted(() => ({
  mockCopy: vi.fn().mockResolvedValue({ id: 2, name: 'ch-copy' }),
}));

vi.mock('@/services/query/useChannels', () => ({
  useCopyChannel: () => ({ mutateAsync: mockCopy, isPending: false }),
}));

/** 源渠道（与表格/卡片行内复制传入的 record 一致） */
const source: ChannelCard = {
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
};

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
  mockCopy.mockClear();
});

/** 渲染 Modal 并提供 i18n + antd App 上下文 */
function renderModal() {
  const onClose = vi.fn();
  render(
    <I18nextProvider i18n={i18n}>
      <AntApp>
        <CopyChannelModal open source={source} onClose={onClose} />
      </AntApp>
    </I18nextProvider>,
  );
  return { onClose };
}

/** 点击 antd Modal 的「确定/OK」按钮触发提交 */
async function clickOk() {
  const okButton = screen.getByRole('button', { name: /^确定$|^OK$/i });
  await userEvent.click(okButton);
}

describe('CopyChannelModal', () => {
  it('预填源渠道 name 并提交复制（默认不复制凭证）', async () => {
    renderModal();

    // 断言 name 预填源渠道值
    const nameInput = screen.getByLabelText('新渠道名称');
    expect(nameInput).toHaveValue('ch-1');

    // 修改 name → 点击确定
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, 'ch-copy');
    await clickOk();

    await waitFor(() => {
      expect(mockCopy).toHaveBeenCalledTimes(1);
      expect(mockCopy).toHaveBeenCalledWith({
        id: 1,
        data: { name: 'ch-copy', copyCredentials: false },
      });
    });
  });

  it('勾选复制凭证后提交带 copyCredentials=true', async () => {
    renderModal();

    await userEvent.click(screen.getByRole('checkbox'));
    await clickOk();

    await waitFor(() => {
      expect(mockCopy).toHaveBeenCalledWith({
        id: 1,
        data: { name: 'ch-1', copyCredentials: true },
      });
    });
  });

  it('name 为空时禁止提交', async () => {
    renderModal();

    const nameInput = screen.getByLabelText('新渠道名称');
    await userEvent.clear(nameInput);
    await clickOk();

    // 应显示必填校验提示，copy 不被调用
    await waitFor(() => {
      expect(screen.getByText('请输入新渠道名称')).toBeInTheDocument();
    });
    expect(mockCopy).not.toHaveBeenCalled();
  });
});
