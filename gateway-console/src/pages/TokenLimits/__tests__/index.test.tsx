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
// Token 限额页 smoke 测试：
// 1) 渲染列表（用户/模型/额度/已用/周期/超限动作）
// 2) 加载失败显示错误提示 + 重试
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { I18nextProvider } from 'react-i18next';
import { MemoryRouter } from 'react-router-dom';
import i18n from '@/i18n';
import TokenLimits from '@/pages/TokenLimits';
import type { PageResponse } from '@/types/api';

const { mockTokenLimits, mockRefetch, mockUsers, mockModels } = vi.hoisted(() => ({
  mockTokenLimits: vi.fn(),
  mockRefetch: vi.fn(),
  mockUsers: vi.fn(),
  mockModels: vi.fn(),
}));

vi.mock('@/services/query/useTokenLimits', () => ({
  useTokenLimits: () => mockTokenLimits(),
  useCreateTokenLimit: () => ({ mutateAsync: vi.fn().mockResolvedValue({}), isPending: false }),
  useUpdateTokenLimit: () => ({ mutateAsync: vi.fn().mockResolvedValue({}), isPending: false }),
  useDeleteTokenLimit: () => ({ mutateAsync: vi.fn().mockResolvedValue(undefined) }),
  useResetTokenLimitUsage: () => ({ mutateAsync: vi.fn().mockResolvedValue({}) }),
}));

vi.mock('@/services/query/useUsers', () => ({
  useUsers: () => ({ data: mockUsers() }),
}));

vi.mock('@/services/query/useModels', () => ({
  useModels: () => ({ data: mockModels() }),
}));

const mockPage: PageResponse<{
  id: number;
  userId: number;
  username?: string;
  modelName?: string;
  limitType: string;
  maxTokens: number;
  usedTokens: number;
  remainingTokens: number;
  periodType: string;
  exceededAction: string;
  enabled: boolean;
}> = {
  items: [
    {
      id: 1,
      userId: 1,
      username: 'admin',
      modelName: 'gpt-4o',
      limitType: 'USER_CUSTOM',
      maxTokens: 1000000,
      usedTokens: 250000,
      remainingTokens: 750000,
      periodType: 'MONTHLY',
      exceededAction: 'REJECT',
      enabled: true,
    },
  ],
  pagination: { page: 1, limit: 20, total: 1, totalPages: 1 },
};

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

beforeEach(() => {
  mockTokenLimits.mockReset();
  mockTokenLimits.mockReturnValue({
    data: mockPage,
    isLoading: false,
    isError: false,
    refetch: mockRefetch,
  });
  mockRefetch.mockReset();
  mockRefetch.mockResolvedValue(undefined);
  mockUsers.mockReturnValue({ items: [{ id: 1, username: 'admin' }] });
  mockModels.mockReturnValue({ items: [] });
});

function renderPage() {
  return render(
    <MemoryRouter>
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <TokenLimits />
        </AntApp>
      </I18nextProvider>
    </MemoryRouter>,
  );
}

describe('TokenLimits 页面', () => {
  it('渲染限额列表（用户/模型/额度/周期/超限动作）', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('admin')).toBeInTheDocument();
    });
    expect(screen.getByText('gpt-4o')).toBeInTheDocument();
    expect(screen.getByText('1,000,000')).toBeInTheDocument();
    expect(screen.getByText('250,000')).toBeInTheDocument();
    expect(screen.getByText('每月')).toBeInTheDocument();
    expect(screen.getByText('拒绝')).toBeInTheDocument();
  });

  it('加载失败显示错误提示，点击重试触发 refetch', async () => {
    mockTokenLimits.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      refetch: mockRefetch,
    });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('加载失败')).toBeInTheDocument();
    });
    // AntD Button 对两字按钮自动插入空格，实际文本为"重 试"，用正则匹配
    await userEvent.click(screen.getByRole('button', { name: /重\s*试/ }));
    expect(mockRefetch).toHaveBeenCalled();
  });
});
