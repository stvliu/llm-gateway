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
// 系统设置页单元测试——缓存清理分组：
// 1) 渲染"缓存清理"分组（全部清理 + 按应用清理入口）
// 2) 全部清理：Popconfirm 确认后无参触发 useEvictCache（清空全部缓存），成功提示
// 3) 按应用清理：输入应用 ID 后确认，以该 ID 触发缓存失效
// 策略：mock useSettings / useCatalogSync 两组 hooks，捕获 mutateAsync 调用参数。
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { I18nextProvider } from 'react-i18next';
import { MemoryRouter } from 'react-router-dom';
import i18n from '@/i18n';
import SettingsPage from '@/pages/Settings';

// vi.hoisted 提升 mock 引用（useEvictCache 的 mutateAsync 捕获调用参数）
const { mockEvictMutate } = vi.hoisted(() => ({
  mockEvictMutate: vi.fn(),
}));

// mock 系统设置 hooks：固定返回加载完成态 + 捕获缓存清理调用
vi.mock('@/services/query/useSettings', () => ({
  useSettings: () => ({ data: [], isLoading: false }),
  useUpdateSetting: () => ({ isPending: false, mutateAsync: vi.fn() }),
  useCleanupAuditLogs: () => ({ isPending: false, mutateAsync: vi.fn() }),
  useEvictCache: () => ({ isPending: false, mutateAsync: mockEvictMutate }),
}));

// mock 模型目录同步 hooks（Settings 页引用，与本测试无关）
vi.mock('@/services/query/useCatalogSync', () => ({
  useCatalogSync: () => ({ isPending: false, mutateAsync: vi.fn() }),
  useCatalogSyncStatus: () => ({ data: null, isLoading: false }),
}));

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
  mockEvictMutate.mockReset();
  mockEvictMutate.mockResolvedValue(undefined);
});

function renderPage() {
  return render(
    <MemoryRouter>
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <SettingsPage />
        </AntApp>
      </I18nextProvider>
    </MemoryRouter>,
  );
}

describe('Settings 页缓存清理分组', () => {
  it('渲染缓存清理分组（全部清理 + 按应用清理入口）', () => {
    renderPage();
    expect(screen.getByText('缓存清理')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /全\s*部\s*清\s*理/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /按\s*应\s*用\s*清\s*理/ })).toBeInTheDocument();
  });

  it('全部清理：确认后无参触发缓存清理并提示成功', async () => {
    renderPage();
    await userEvent.click(screen.getByRole('button', { name: /全\s*部\s*清\s*理/ }));
    // Popconfirm 确认按钮（antd 对两字按钮自动插空格，用正则匹配）
    await userEvent.click(await screen.findByRole('button', { name: /确\s*认/ }));
    await waitFor(() => {
      expect(mockEvictMutate).toHaveBeenCalledTimes(1);
    });
    expect(mockEvictMutate).toHaveBeenCalledWith(undefined);
    // 成功提示
    expect(await screen.findByText('缓存已清理')).toBeInTheDocument();
  });

  it('按应用清理：输入应用 ID 100 后确认，以 100 触发缓存清理', async () => {
    renderPage();
    const appIdInput = screen.getByRole('spinbutton', { name: /应\s*用\s*ID/ });
    await userEvent.type(appIdInput, '100');
    await userEvent.click(screen.getByRole('button', { name: /按\s*应\s*用\s*清\s*理/ }));
    await userEvent.click(await screen.findByRole('button', { name: /确\s*认/ }));
    await waitFor(() => {
      expect(mockEvictMutate).toHaveBeenCalledTimes(1);
    });
    expect(mockEvictMutate).toHaveBeenCalledWith(100);
  });
});
