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
// CredentialSection 删除 API Key 危险确认测试（任务 8.4）
//
// 验证目标：
// 1) 点击"删除"按钮 → 应弹出 Modal.confirm（含 description: keyMasked + "删除后无法恢复，使用此 Key 的请求将立即失败"）
// 2) 点击 OK → 触发 deleteCredential mutation
// 3) 在确认前不应直接调用删除 API
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { App as AntApp } from 'antd';
import i18n from '@/i18n';

beforeAll(() => {
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
});

const deleteCredentialMock = vi.fn();
vi.mock('@/services/api/channel', () => {
  return {
    channelApi: {
      createCredential: vi.fn(() => Promise.resolve()),
      updateCredential: vi.fn(() => Promise.resolve()),
      deleteCredential: (...args: unknown[]) => deleteCredentialMock(...args),
      testCredential: vi.fn(() => Promise.resolve({ success: true, latency: 100 })),
      list: vi.fn(() => Promise.resolve([])),
      get: vi.fn(() => Promise.resolve({})),
      listCredentials: vi.fn(() => Promise.resolve([])),
      listModels: vi.fn(() => Promise.resolve([])),
    },
  };
});

import { CredentialSection } from '../CredentialSection';

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
}

function renderWithProviders(ui: React.ReactElement) {
  const client = makeQueryClient();
  return render(
    <I18nextProvider i18n={i18n}>
      <QueryClientProvider client={client}>
        <AntApp>{ui}</AntApp>
      </QueryClientProvider>
    </I18nextProvider>
  );
}

const sampleCredential = {
  id: 100,
  channelId: 1,
  apiKeyPrefix: 'sk-abc123',
  apiKeyPlain: 'sk-abc1234567890',
  name: '',
  description: null,
  weight: 50,
  priority: 1,
  state: 'ACTIVE',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('CredentialSection 删除危险确认（任务 8.4）', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    deleteCredentialMock.mockReset();
    deleteCredentialMock.mockResolvedValue(undefined);
  });

  it('点击删除按钮 → 弹 Modal.confirm 含 keyMasked + "删除后无法恢复"，确认后才调 deleteCredential', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <CredentialSection channelId={1} credentials={[sampleCredential as never]} />
    );

    // 点击行尾的"删除"按钮（InlineEditableList 的 type=link danger 按钮）
    // v6: 图标按钮 accessible name 由中文"删除"变为英文 "delete"
    const delBtns = screen.getAllByRole('button', { name: /delete/i });
    // 取最后一个；前面可能有 MaskedKeyDisplay 之类的图标按钮
    await user.click(delBtns[delBtns.length - 1]);

    // 弹出 Modal.confirm，包含 keyMasked（取 prefix sk-abc123）+ 删除后无法恢复
    await waitFor(() => {
      expect(screen.getByText(/删除后无法恢复/)).toBeInTheDocument();
    });
    expect(screen.getByText(/sk-abc123/)).toBeInTheDocument();

    // 此时尚未调用删除接口
    expect(deleteCredentialMock).not.toHaveBeenCalled();

    // 点击 OK 按钮（Modal footer 的 danger OK 按钮，文案为中文"删除"）
    // v6: 行内删除按钮 accessible name 为英文 "delete"，与 Modal OK 中文"删除"不冲突；
    // className/容器结构在 v6 下已变，改用文案 name 定位
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);

    await waitFor(() => {
      expect(deleteCredentialMock).toHaveBeenCalled();
    });
  });
});
