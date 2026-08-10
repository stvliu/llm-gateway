/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// CredentialSection 保存反馈脉冲——测试套件
//
// 验证目标：
// 1) 编辑保存成功 → save-tip-ok 出现
// 2) 编辑保存失败 → save-tip-err + 容器红框 (save-pulse-error)
// 3) 乐观更新失败回滚到上一个值（旧 priority/weight 仍显示）
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

const updateCredentialMock = vi.fn();
vi.mock('@/services/api/channel', () => {
  return {
    channelApi: {
      createCredential: vi.fn(() => Promise.resolve()),
      updateCredential: (...args: unknown[]) => updateCredentialMock(...args),
      deleteCredential: vi.fn(() => Promise.resolve()),
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
      <QueryClientProvider client={client}>{ui}</QueryClientProvider>
    </I18nextProvider>
  );
}

const sampleCredential = {
  id: 100,
  channelId: 1,
  apiKeyPrefix: 'sk-abcdef',
  apiKeyPlain: 'sk-abcdefghijk',
  name: '',
  description: null,
  weight: 50,
  priority: 1,
  state: 'ACTIVE',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('CredentialSection 保存反馈脉冲', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    updateCredentialMock.mockReset();
  });

  it('编辑保存成功后应出现 ✓ 已保存（save-tip-ok）', async () => {
    updateCredentialMock.mockResolvedValue({ ...sampleCredential, priority: 2 });

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <CredentialSection channelId={1} credentials={[sampleCredential as never]} />
    );

    // 进入编辑模式（InlineEditableList 提供的"编辑"按钮位于 MaskedKeyDisplay 编辑图标之后）
    const editBtns = screen.getAllByRole('button', { name: /edit/i });
    await user.click(editBtns[editBtns.length - 1]);
    // 触发保存
    const saveBtns = screen.getAllByRole('button', { name: /保\s*存/ });
    await user.click(saveBtns[0]);

    await waitFor(() => {
      expect(updateCredentialMock).toHaveBeenCalled();
    });
    await waitFor(() => {
      const tip = container.querySelector('.save-tip-ok');
      expect(tip).not.toBeNull();
      expect(tip?.textContent || '').toMatch(/已保存/);
    });
  });

  it('编辑保存失败应出现 ✗ 错误且行加红框（save-pulse-error）', async () => {
    updateCredentialMock.mockRejectedValue(axiosLike500('cred update boom'));

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <CredentialSection channelId={1} credentials={[sampleCredential as never]} />
    );

    const editBtns = screen.getAllByRole('button', { name: /edit/i });
    await user.click(editBtns[editBtns.length - 1]);
    const saveBtns = screen.getAllByRole('button', { name: /保\s*存/ });
    await user.click(saveBtns[0]);

    await waitFor(() => {
      expect(updateCredentialMock).toHaveBeenCalled();
    });
    await waitFor(() => {
      const errTip = container.querySelector('.save-tip-err');
      expect(errTip).not.toBeNull();
      expect(errTip?.textContent || '').toMatch(/cred update boom/);
    });
    expect(container.querySelector('.save-pulse-error')).not.toBeNull();
  });

  it('乐观更新失败应回滚：原 priority 标签仍可见', async () => {
    updateCredentialMock.mockRejectedValue(axiosLike500('cred update boom'));

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <CredentialSection channelId={1} credentials={[sampleCredential as never]} />
    );

    const editBtns = screen.getAllByRole('button', { name: /edit/i });
    await user.click(editBtns[editBtns.length - 1]);
    // 修改 priority 输入
    const priorityInput = container.querySelector(
      'input[id$="priority"]'
    ) as HTMLInputElement;
    expect(priorityInput).not.toBeNull();
    await user.clear(priorityInput);
    await user.type(priorityInput, '5');
    const saveBtns = screen.getAllByRole('button', { name: /保\s*存/ });
    await user.click(saveBtns[0]);

    await waitFor(() => {
      expect(updateCredentialMock).toHaveBeenCalled();
    });
    // 失败回退到展示态后，原始 P1 标签仍可见
    await waitFor(() => {
      expect(within(container).queryAllByText(/P1/).length).toBeGreaterThan(0);
    });
  });
});

export {};
