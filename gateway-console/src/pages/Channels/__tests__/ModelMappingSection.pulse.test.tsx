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
// ModelMappingSection 保存反馈脉冲——测试套件
//
// 验证目标：
// 1) 编辑 upstreamModelName 保存成功 → save-tip-ok 出现
// 2) 编辑保存失败 → save-tip-err + 容器红框 (save-pulse-error)
// 3) 乐观更新失败回滚到上一个 upstream 名（旧 upstream tag 仍可见）
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
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

const axiosLike500 = (msg: string) => ({
  isAxiosError: true,
  message: 'Request failed with status code 500',
  response: {
    status: 500,
    statusText: 'Internal Server Error',
    data: { message: msg },
  },
});

const updateUpstreamMock = vi.fn();
vi.mock('@/services/api/channel', () => {
  return {
    channelApi: {
      createModel: vi.fn(() => Promise.resolve()),
      deleteModel: vi.fn(() => Promise.resolve()),
      updateUpstreamModelName: (...args: unknown[]) => updateUpstreamMock(...args),
      // 生产代码经 useUpdateChannelModelFull 调用 updateModel（commit a9e54f1 后），
      // 绑定到同一 mock 以保持"保存被调用"断言语义不变
      updateModel: (...args: unknown[]) => updateUpstreamMock(...args),
      list: vi.fn(() => Promise.resolve([])),
      get: vi.fn(() => Promise.resolve({})),
      listCredentials: vi.fn(() => Promise.resolve([])),
      listModels: vi.fn(() => Promise.resolve([])),
    },
  };
});

import { ModelMappingSection } from '../ModelMappingSection';

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
      <QueryClientProvider client={client}>{ui}</QueryClientProvider>
    </I18nextProvider>
  );
}

const sampleMapping = {
  id: 200,
  channelId: 1,
  modelId: 1,
  modelName: 'gpt-4o',
  upstreamModelName: 'gpt-4o-original',
  state: 'ACTIVE',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('ModelMappingSection 保存反馈脉冲', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    updateUpstreamMock.mockReset();
  });

  it('编辑保存成功后应出现 ✓ 已保存（save-tip-ok）', async () => {
    updateUpstreamMock.mockResolvedValue({
      ...sampleMapping,
      upstreamModelName: 'gpt-4o-renamed',
    });

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <ModelMappingSection channelId={1} channelModels={[sampleMapping as never]} />
    );

    // 进入编辑模式
    const editBtns = screen.getAllByRole('button', { name: /edit/i });
    await user.click(editBtns[editBtns.length - 1]);
    // 修改 upstream name 输入
    const inputs = container.querySelectorAll('input');
    const upstreamInput = inputs[inputs.length - 1] as HTMLInputElement;
    await user.clear(upstreamInput);
    await user.type(upstreamInput, 'gpt-4o-renamed');
    // 保存
    const saveBtns = screen.getAllByRole('button', { name: /保\s*存/ });
    await user.click(saveBtns[0]);

    await waitFor(() => {
      expect(updateUpstreamMock).toHaveBeenCalled();
    });
    await waitFor(() => {
      const tip = container.querySelector('.save-tip-ok');
      expect(tip).not.toBeNull();
      expect(tip?.textContent || '').toMatch(/已保存/);
    });
  });

  it('编辑保存失败应出现 ✗ 错误且行加红框（save-pulse-error）', async () => {
    updateUpstreamMock.mockRejectedValue(axiosLike500('model update boom'));

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <ModelMappingSection channelId={1} channelModels={[sampleMapping as never]} />
    );

    const editBtns = screen.getAllByRole('button', { name: /edit/i });
    await user.click(editBtns[editBtns.length - 1]);
    const inputs = container.querySelectorAll('input');
    const upstreamInput = inputs[inputs.length - 1] as HTMLInputElement;
    await user.clear(upstreamInput);
    await user.type(upstreamInput, 'will-fail');
    const saveBtns = screen.getAllByRole('button', { name: /保\s*存/ });
    await user.click(saveBtns[0]);

    await waitFor(() => {
      expect(updateUpstreamMock).toHaveBeenCalled();
    });
    await waitFor(() => {
      const errTip = container.querySelector('.save-tip-err');
      expect(errTip).not.toBeNull();
      expect(errTip?.textContent || '').toMatch(/model update boom/);
    });
    expect(container.querySelector('.save-pulse-error')).not.toBeNull();
  });

  it('乐观更新失败应回滚：原 upstream 名仍可见', async () => {
    updateUpstreamMock.mockRejectedValue(axiosLike500('model update boom'));

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <ModelMappingSection channelId={1} channelModels={[sampleMapping as never]} />
    );

    const editBtns = screen.getAllByRole('button', { name: /edit/i });
    await user.click(editBtns[editBtns.length - 1]);
    const inputs = container.querySelectorAll('input');
    const upstreamInput = inputs[inputs.length - 1] as HTMLInputElement;
    await user.clear(upstreamInput);
    await user.type(upstreamInput, 'will-fail');
    const saveBtns = screen.getAllByRole('button', { name: /保\s*存/ });
    await user.click(saveBtns[0]);

    await waitFor(() => {
      expect(updateUpstreamMock).toHaveBeenCalled();
    });
    // 失败回退到展示态后，原 upstream 名 'gpt-4o-original' 仍在 DOM 中
    await waitFor(() => {
      expect(within(container).queryAllByText(/gpt-4o-original/).length).toBeGreaterThan(0);
    });
  });
});

export {};
