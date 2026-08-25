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
import { describe, it, expect, afterEach, vi } from 'vitest';
import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';
import instance, { setForbiddenHandler } from '@/services/api/client';

/**
 * client 403 全局处理测试
 *
 * <p>响应拦截器收到 403 时应触发全局通知（notifyForbidden 的注册处理器）。</p>
 */
describe('client 403 全局处理', () => {
  const originalAdapter = instance.defaults.adapter;

  afterEach(() => {
    instance.defaults.adapter = originalAdapter;
    setForbiddenHandler(null);
  });

  /** 构造指定状态码的失败适配器 */
  function failWith(status: number) {
    instance.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      throw new AxiosError(
        `HTTP ${status}`,
        AxiosError.ERR_BAD_REQUEST,
        config,
        null,
        { status, statusText: 'Error', data: {}, headers: new AxiosHeaders(), config },
      );
    };
  }

  it('收到 403 响应触发全局通知处理器', async () => {
    const handler = vi.fn();
    setForbiddenHandler(handler);
    failWith(403);

    await expect(instance.get('/some-admin-resource')).rejects.toThrow();
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('非 403 错误不触发全局通知', async () => {
    const handler = vi.fn();
    setForbiddenHandler(handler);
    failWith(400);

    await expect(instance.get('/bad-request')).rejects.toThrow();
    expect(handler).not.toHaveBeenCalled();
  });

  it('未注册处理器时 403 不抛错（静默兜底）', async () => {
    failWith(403);
    await expect(instance.get('/no-handler')).rejects.toThrow();
  });
});
