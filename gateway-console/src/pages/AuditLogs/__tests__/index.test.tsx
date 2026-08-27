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
// 审计日志页单元测试：
// 1) 渲染审计记录（时间/操作人/动作/资源/结果/IP）
// 2) 结果筛选触发查询参数更新
// 3) 加载失败显示错误提示 + 重试
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { I18nextProvider } from 'react-i18next';
import { MemoryRouter } from 'react-router-dom';
import i18n from '@/i18n';
import AuditLogs from '@/pages/AuditLogs';
import type { PageResponse } from '@/types/api';

// vi.hoisted 提升 mock 引用
const {
  mockAuditLogs,
  mockRefetch,
  mockUseAuditLogsParams,
} = vi.hoisted(() => {
  const mockRefetch = vi.fn();
  const mockUseAuditLogsParams: { current: unknown } = { current: null };
  return {
    mockAuditLogs: vi.fn(),
    mockRefetch,
    mockUseAuditLogsParams,
  };
});

// mock useAuditLogs：记录调用参数供断言
vi.mock('@/services/query/useAuditLogs', () => ({
  useAuditLogs: (params: unknown) => {
    mockUseAuditLogsParams.current = params;
    return mockAuditLogs();
  },
}));

// mock useUsers：固定返回两个用户
vi.mock('@/services/query/useUsers', () => ({
  useUsers: () => ({
    data: {
      items: [
        { id: 1, username: 'admin', email: 'admin@test.com' },
        { id: 2, username: 'dev', email: 'dev@test.com' },
      ],
    } as { items: Array<{ id: number; username: string; email: string }> },
    isLoading: false,
  }),
}));

const mockPage: PageResponse<{
  id: number;
  userId: number;
  action: string;
  resource: string;
  result: string;
  ipAddress: string;
  createdAt: string;
}> = {
  items: [
    {
      id: 1,
      userId: 1,
      action: 'POST /api/v1/channels',
      resource: '/api/v1/channels',
      result: 'SUCCESS',
      ipAddress: '192.168.1.1',
      createdAt: '2026-08-27T10:00:00Z',
    },
    {
      id: 2,
      userId: 0,
      action: 'POST /api/v1/auth/login',
      resource: '/api/v1/auth/login',
      result: 'FAILURE',
      ipAddress: '10.0.0.5',
      createdAt: '2026-08-27T09:30:00Z',
    },
  ],
  pagination: { page: 1, limit: 20, total: 2, totalPages: 1 },
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
  mockAuditLogs.mockReset();
  mockAuditLogs.mockReturnValue({
    data: mockPage,
    isLoading: false,
    isError: false,
    isFetching: false,
    refetch: mockRefetch,
  });
  mockRefetch.mockReset();
  mockRefetch.mockResolvedValue(undefined);
  mockUseAuditLogsParams.current = null;
});

function renderPage() {
  return render(
    <MemoryRouter>
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <AuditLogs />
        </AntApp>
      </I18nextProvider>
    </MemoryRouter>,
  );
}

describe('AuditLogs 页面', () => {
  it('渲染审计记录（操作人映射/动作/结果/时间/IP）', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('POST /api/v1/channels')).toBeInTheDocument();
    });
    // 操作人映射：userId=1 → admin
    expect(screen.getByText(/admin \(1\)/)).toBeInTheDocument();
    // 未认证主体（userId=0）
    expect(screen.getByText('未认证')).toBeInTheDocument();
    // 结果 Tag
    expect(screen.getByText('成功')).toBeInTheDocument();
    expect(screen.getByText('失败')).toBeInTheDocument();
    // IP
    expect(screen.getByText('192.168.1.1')).toBeInTheDocument();
    // 时间已格式化（10:00Z 无论何本地时区均落在 2026-08-27 当日，用正则避免时区依赖）
    expect(screen.getAllByText(/2026-08-27/).length).toBeGreaterThan(0);
  });

  it('结果筛选触发查询参数更新', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('POST /api/v1/channels')).toBeInTheDocument();
    });
    // 打开结果筛选下拉并选择"失败"
    await userEvent.click(screen.getByTestId('result-filter-select'));
    const failureOption = await screen.findByTitle('失败');
    await userEvent.click(failureOption);
    // 断言 useAuditLogs 收到的参数包含 result=FAILURE 且 page 重置为 1
    await waitFor(() => {
      const params = mockUseAuditLogsParams.current as { result?: string; page?: number };
      expect(params?.result).toBe('FAILURE');
      expect(params?.page).toBe(1);
    });
  });

  it('加载失败显示错误提示，点击重试触发 refetch', async () => {
    mockAuditLogs.mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      isFetching: false,
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
