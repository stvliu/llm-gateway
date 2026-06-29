// 应用 API 封装层单元测试
//
// 任务 4.11b 补全 1：验证 applicationApi.bindResilienceProfile 对后端契约的调用：
// - 绑定：PUT /applications/{id}/resilience body { resilienceProfileId }
// - 解绑：resilienceProfileId 为 null 时仍走同一端点
//
// 策略：mock @/services/api/client 的 api 对象，断言调用的 url/method/body 与后端契约一致。
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { applicationApi } from '../application';

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

describe('applicationApi.bindResilienceProfile', () => {
  it('绑定画像：调用 PUT /applications/{id}/resilience 带 {resilienceProfileId}', async () => {
    (api.put as any).mockResolvedValue({ id: 1, resilienceProfileId: 7 });
    const res = await applicationApi.bindResilienceProfile(1, 7);
    expect(api.put).toHaveBeenCalledWith('/applications/1/resilience', {
      resilienceProfileId: 7,
    });
    expect(res).toEqual({ id: 1, resilienceProfileId: 7 });
  });

  it('解绑画像：resilienceProfileId=null 仍调用同一端点', async () => {
    (api.put as any).mockResolvedValue({ id: 1, resilienceProfileId: null });
    await applicationApi.bindResilienceProfile(1, null);
    expect(api.put).toHaveBeenCalledWith('/applications/1/resilience', {
      resilienceProfileId: null,
    });
  });
});
