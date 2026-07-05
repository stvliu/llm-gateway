// 容灾 API 封装层单元测试
//
// 验证 resilienceApi 封装层对后端端点的调用契约：
// - 熔断应急：force-open / force-close / state
// - 转移事件流：events.list / events.exhausted
//
// 策略：mock @/services/api/client 的 api 对象，断言调用的 url/method/body 与后端契约一致。
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { resilienceApi } from '../api';

// mock api client，捕获调用参数
vi.mock('@/services/api/client', () => {
  const api = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  };
  return { api };
});

import { api } from '@/services/api/client';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('resilienceApi.circuitBreaker 应急操作', () => {
  it('forceOpen 调用 POST .../circuit-breaker/force-open 返回状态响应', async () => {
    (api.post as any).mockResolvedValue({ state: 'OPEN' });
    const res = await resilienceApi.circuitBreaker.forceOpen(1, 2);
    expect(api.post).toHaveBeenCalledWith(
      '/channels/1/endpoints/2/circuit-breaker/force-open',
    );
    expect(res).toEqual({ state: 'OPEN' });
  });

  it('forceClose 调用 POST .../circuit-breaker/force-close 返回状态响应', async () => {
    (api.post as any).mockResolvedValue({ state: 'CLOSED' });
    const res = await resilienceApi.circuitBreaker.forceClose(1, 2);
    expect(api.post).toHaveBeenCalledWith(
      '/channels/1/endpoints/2/circuit-breaker/force-close',
    );
    expect(res).toEqual({ state: 'CLOSED' });
  });

  it('getState 调用 GET .../circuit-breaker/state 返回状态响应', async () => {
    (api.get as any).mockResolvedValue({ state: 'HALF_OPEN' });
    const res = await resilienceApi.circuitBreaker.getState(1, 2);
    expect(api.get).toHaveBeenCalledWith(
      '/channels/1/endpoints/2/circuit-breaker/state',
    );
    expect(res).toEqual({ state: 'HALF_OPEN' });
  });
});

describe('resilienceApi.events 转移事件流', () => {
  it('list 无参数时调用 GET /resilience/events 不带 query', async () => {
    (api.get as any).mockResolvedValue([]);
    await resilienceApi.events.list();
    expect(api.get).toHaveBeenCalledWith('/resilience/events', { params: {} });
  });

  it('list 透传 since/applicationId/limit 到 params', async () => {
    (api.get as any).mockResolvedValue([]);
    const sinceIso = '2026-06-22T00:00:00Z';
    await resilienceApi.events.list({
      since: sinceIso,
      applicationId: 7,
      limit: 50,
    });
    expect(api.get).toHaveBeenCalledWith('/resilience/events', {
      params: {
        since: sinceIso,
        applicationId: 7,
        limit: 50,
      },
    });
  });

  it('list 省略未传字段不污染 params', async () => {
    (api.get as any).mockResolvedValue([]);
    await resilienceApi.events.list({ limit: 25 });
    expect(api.get).toHaveBeenCalledWith('/resilience/events', {
      params: { limit: 25 },
    });
  });

  it('list 返回事件响应数组', async () => {
    const events = [{ id: 1, traceId: 't1', exhausted: false, decision: 'L1' }];
    (api.get as any).mockResolvedValue(events);
    const res = await resilienceApi.events.list();
    expect(res).toEqual(events);
  });

  it('exhausted 无参数时调用 GET /resilience/events/exhausted 空 params', async () => {
    (api.get as any).mockResolvedValue([]);
    await resilienceApi.events.exhausted();
    expect(api.get).toHaveBeenCalledWith('/resilience/events/exhausted', { params: {} });
  });

  it('exhausted 透传 since/limit 到 params', async () => {
    (api.get as any).mockResolvedValue([]);
    const sinceIso = '2026-06-22T08:00:00Z';
    await resilienceApi.events.exhausted({ since: sinceIso, limit: 20 });
    expect(api.get).toHaveBeenCalledWith('/resilience/events/exhausted', {
      params: { since: sinceIso, limit: 20 },
    });
  });

  it('exhausted 返回耗尽事件数组', async () => {
    const events = [{ id: 9, exhausted: true, decision: 'L2' }];
    (api.get as any).mockResolvedValue(events);
    const res = await resilienceApi.events.exhausted();
    expect(res).toEqual(events);
  });
});
