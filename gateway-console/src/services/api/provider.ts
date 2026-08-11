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
import { api } from './client';
import type { Provider, CreateProviderRequest, UpdateProviderRequest, ProviderKeyStats, ConnectivityTestRequest, ConnectivityTestResult } from '@/types/provider';
import type { PageResponse, PageParams } from '@/types/api';

export const providerApi = {
  /** 获取供应商列表 */
  list: (params?: PageParams) =>
    api.get<PageResponse<Provider>>('/providers', { params }),

  /** 获取供应商详情 */
  get: (id: number) =>
    api.get<Provider>(`/providers/${id}`),

  /** 创建供应商 */
  create: (data: CreateProviderRequest) =>
    api.post<Provider>('/providers', data),

  /** 更新供应商 */
  update: (id: number, data: UpdateProviderRequest) =>
    api.put<Provider>(`/providers/${id}`, data),

  /** 删除供应商 */
  delete: (id: number) =>
    api.delete<void>(`/providers/${id}`),

  /** 获取 Provider 的 Key 信息 */
  getKeys: (id: number) =>
    api.get<ProviderKeyStats[]>(`/providers/${id}/keys`),

  /** 连通性测试 */
  testConnectivity: (data: ConnectivityTestRequest) =>
    api.post<ConnectivityTestResult>('/providers/test-connectivity', data),
};
