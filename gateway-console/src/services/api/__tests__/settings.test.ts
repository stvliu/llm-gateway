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
// 系统设置 API 单元测试——缓存清理（evictCache）：
// 1) 无 applicationId = 全清（POST /admin/cache/evict，不携带查询参数）
// 2) 带 applicationId 时作为查询参数传递（后端按应用失效）
// 策略：mock @/services/api/client 的 api 对象，断言 url/data/config，不发起真实请求。
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/services/api/client', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  },
}));

import { settingsApi } from '@/services/api/settings';
import { api } from '@/services/api/client';

const postMock = vi.mocked(api.post);

beforeEach(() => {
  postMock.mockReset();
  postMock.mockResolvedValue(undefined);
});

describe('settingsApi.evictCache', () => {
  it('调用 POST /admin/cache/evict（无 applicationId = 全清，不携带查询参数）', async () => {
    await settingsApi.evictCache();

    expect(postMock).toHaveBeenCalledTimes(1);
    const [url, data, config] = postMock.mock.calls[0];
    expect(url).toBe('/admin/cache/evict');
    expect(data).toBeUndefined();
    expect(config?.params).toBeUndefined();
  });

  it('带 applicationId 时作为查询参数传递（applicationId=100）', async () => {
    await settingsApi.evictCache(100);

    expect(postMock).toHaveBeenCalledTimes(1);
    const [url, data, config] = postMock.mock.calls[0];
    expect(url).toBe('/admin/cache/evict');
    expect(data).toBeUndefined();
    expect(config?.params).toEqual({ applicationId: 100 });
  });
});
