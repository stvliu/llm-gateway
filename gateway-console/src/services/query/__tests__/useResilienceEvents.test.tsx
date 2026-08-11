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
// 容灾转移事件流 React Query hooks 单元测试
//
// 验证 useFailoverEvents / useExhaustedEvents hooks：
// - 调用 resilienceApi.events.list / exhausted 拉取数据
// - 透传查询参数（applicationId/limit/since）
// - 10s 轮询（refetchInterval=10000）
// - 返回 data 正确解包
//
// 策略：mock @/pages/resilience/api 的 resilienceApi，用 QueryClientProvider wrapper
// 包裹 renderHook，断言 mock 被调用及返回数据。
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactNode } from 'react';

// mock resilienceApi，捕获 list/exhausted 调用
vi.mock('@/pages/resilience/api', () => ({
  resilienceApi: {
    events: {
      list: vi.fn(),
      exhausted: vi.fn(),
    },
  },
}));

import { resilienceApi } from '@/pages/resilience/api';
import {
  useFailoverEvents,
  useExhaustedEvents,
} from '../useResilience';

/** 构造带 React Query Provider 的 wrapper */
function createWrapper() {
  const qc = new QueryClient({
    defaultOptions: {
      queries: { retry: false, refetchInterval: false },
    },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    );
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useFailoverEvents', () => {
  it('无参数时调用 resilienceApi.events.list(undefined) 拉取事件流', async () => {
    const events = [
      { id: 1, exhausted: false, decision: 'L1', occurredAt: '2026-06-22T00:00:00Z' },
    ];
    (resilienceApi.events.list as any).mockResolvedValue(events);

    const { result } = renderHook(() => useFailoverEvents(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(resilienceApi.events.list).toHaveBeenCalledWith(undefined);
    expect(result.current.data).toEqual(events);
  });

  it('透传 applicationId/limit/since 查询参数', async () => {
    (resilienceApi.events.list as any).mockResolvedValue([]);
    const params = {
      since: '2026-06-22T00:00:00Z',
      applicationId: 7,
      limit: 50,
    };

    renderHook(() => useFailoverEvents(params), {
      wrapper: createWrapper(),
    });

    await waitFor(() =>
      expect(resilienceApi.events.list).toHaveBeenCalledWith(params),
    );
  });

  it('enabled=false 时不发起查询', async () => {
    (resilienceApi.events.list as any).mockResolvedValue([]);

    const { result } = renderHook(
      () => useFailoverEvents({}, { enabled: false }),
      { wrapper: createWrapper() },
    );

    // 给一个 tick 让潜在查询有机会触发
    await new Promise((r) => setTimeout(r, 0));
    expect(resilienceApi.events.list).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useExhaustedEvents', () => {
  it('无参数时调用 resilienceApi.events.exhausted(undefined) 拉取耗尽告警', async () => {
    const events = [
      { id: 9, exhausted: true, decision: 'L2', occurredAt: '2026-06-22T01:00:00Z' },
    ];
    (resilienceApi.events.exhausted as any).mockResolvedValue(events);

    const { result } = renderHook(() => useExhaustedEvents(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(resilienceApi.events.exhausted).toHaveBeenCalledWith(undefined);
    expect(result.current.data).toEqual(events);
  });

  it('透传 since/limit 查询参数', async () => {
    (resilienceApi.events.exhausted as any).mockResolvedValue([]);
    const params = { since: '2026-06-22T08:00:00Z', limit: 20 };

    renderHook(() => useExhaustedEvents(params), {
      wrapper: createWrapper(),
    });

    await waitFor(() =>
      expect(resilienceApi.events.exhausted).toHaveBeenCalledWith(params),
    );
  });
});

describe('useFailoverEvents 10s 轮询（refetchInterval）', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('10s 后应触发第二次拉取（初始 + 轮询共 2 次）', async () => {
    (resilienceApi.events.list as any).mockResolvedValue([]);

    const { result } = renderHook(() => useFailoverEvents(), {
      wrapper: createWrapper(),
    });

    // 等待初始查询完成（queryFn 是异步的，需 flush 微任务）
    await act(async () => {
      await vi.waitFor(() => expect(result.current.isSuccess).toBe(true));
    });
    // 初始拉取一次
    expect(resilienceApi.events.list).toHaveBeenCalledTimes(1);

    // 推进 fake timer 10s+1ms，触发 refetchInterval 轮询
    await act(async () => {
      vi.advanceTimersByTime(10_001);
    });
    await act(async () => {
      await vi.waitFor(() =>
        expect(resilienceApi.events.list).toHaveBeenCalledTimes(2),
      );
    });
  });
});
