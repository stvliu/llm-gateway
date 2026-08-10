/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
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
