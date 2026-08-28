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
// 模型目录（models.dev）同步 React Query hooks 单元测试
//
// 验证 useCatalogSyncStatus / useCatalogSync hooks：
// - 调用 catalogSyncApi.status / sync 拉取状态与触发同步
// - 后端 204 无记录（空字符串）时归一化为 null，表示"尚未同步"
// - 同步成功后使同步状态与模型/目录查询失效
//
// 策略：mock @/services/api/catalogSync 的 catalogSyncApi，用 QueryClientProvider wrapper
// 包裹 renderHook，断言 mock 被调用及返回数据。
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactNode } from 'react';

// mock catalogSyncApi，捕获 sync/status 调用
vi.mock('@/services/api/catalogSync', () => ({
  catalogSyncApi: {
    sync: vi.fn(),
    status: vi.fn(),
  },
}));

import { catalogSyncApi } from '@/services/api/catalogSync';
import { useCatalogSync, useCatalogSyncStatus } from '../useCatalogSync';
import type { CatalogSyncStatusResponse } from '@/types/catalog';

const { status: statusMock, sync: syncMock } = vi.mocked(catalogSyncApi);

/** 构造带 React Query Provider 的 wrapper（可注入自定义 client 用于断言） */
function createWrapper(client?: QueryClient) {
  const qc =
    client ??
    new QueryClient({
      defaultOptions: {
        queries: { retry: false, refetchInterval: false },
      },
    });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useCatalogSyncStatus', () => {
  it('调用 catalogSyncApi.status() 拉取最近同步状态', async () => {
    const status: CatalogSyncStatusResponse = {
      result: 'SUCCESS',
      addedCount: 3,
      updatedCount: 5,
      skippedCount: 1,
      failedCount: 0,
      message: 'ok',
      syncedAt: '2026-08-28T10:00:00Z',
    };
    statusMock.mockResolvedValue(status);

    const { result } = renderHook(() => useCatalogSyncStatus(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(statusMock).toHaveBeenCalledWith();
    expect(result.current.data).toEqual(status);
  });

  it('后端 204 无记录（空字符串）时归一化为 null', async () => {
    statusMock.mockResolvedValue('' as never);

    const { result } = renderHook(() => useCatalogSyncStatus(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBeNull();
  });
});

describe('useCatalogSync', () => {
  it('调用 catalogSyncApi.sync() 触发同步并返回同步报告', async () => {
    const report = {
      success: true,
      addedCount: 10,
      updatedCount: 2,
      skippedCount: 3,
      failedCount: 0,
      messages: ['ok'],
      syncedAt: '2026-08-28T10:00:00Z',
    };
    syncMock.mockResolvedValue(report);

    const { result } = renderHook(() => useCatalogSync(), {
      wrapper: createWrapper(),
    });

    await act(async () => {
      await expect(result.current.mutateAsync()).resolves.toEqual(report);
    });
    expect(syncMock).toHaveBeenCalledWith();
  });

  it('同步成功后使同步状态与模型/目录查询失效', async () => {
    syncMock.mockResolvedValue({
      success: true,
      addedCount: 1,
      updatedCount: 0,
      skippedCount: 0,
      failedCount: 0,
      messages: [],
      syncedAt: '2026-08-28T10:00:00Z',
    });
    const qc = new QueryClient({
      defaultOptions: { queries: { retry: false, refetchInterval: false } },
    });
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries');

    const { result } = renderHook(() => useCatalogSync(), {
      wrapper: createWrapper(qc),
    });

    await act(async () => {
      await result.current.mutateAsync();
    });
    expect(invalidateSpy).toHaveBeenCalled();
  });
});
