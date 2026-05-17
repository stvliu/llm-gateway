import { api } from './client';
import type { Provider, CreateProviderRequest, UpdateProviderRequest, ProviderKeysResponse } from '@/types/provider';
import type { PageResponse, PageParams, ProviderTypeOption } from '@/types/api';

/** 连通性测试层级结果 */
interface LevelResult {
  success: boolean;
  message?: string;
  latencyMs?: number;
  errorType?: string;
  models?: string[];
}

/** 连通性测试结果 */
export interface ConnectivityTestResult {
  success: boolean;
  message?: string;
  models?: string[];
  level1?: LevelResult;
  level2?: LevelResult;
  totalLatencyMs?: number;
}

/** 连通性测试请求 */
export interface ConnectivityTestRequest {
  providerType: string;
  baseUrl?: string;
  apiKey: string;
  model?: string;
}

export const providerApi = {
  /** 获取支持的供应商类型列表 */
  getProviderTypes: () =>
    api.get<ProviderTypeOption[]>('/providers/types'),

  /** 获取渠道列表 */
  list: (params?: PageParams) =>
    api.get<PageResponse<Provider>>('/providers', { params }),

  /** 获取渠道详情 */
  get: (id: number) =>
    api.get<Provider>(`/providers/${id}`),

  /** 创建渠道 */
  create: (data: CreateProviderRequest) =>
    api.post<Provider>('/providers', data),

  /** 更新渠道 */
  update: (id: number, data: UpdateProviderRequest) =>
    api.put<Provider>(`/providers/${id}`, data),

  /** 删除渠道 */
  delete: (id: number) =>
    api.delete<void>(`/providers/${id}`),

  /** 启用/禁用渠道 */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<Provider>(`/providers/${id}/enabled`, null, { params: { enabled } }),

  /** 获取 Provider 的 Key 信息 */
  getKeys: (id: number) =>
    api.get<ProviderKeysResponse>(`/providers/${id}/keys`),

  /** 测试连通性 */
  testConnectivity: (data: ConnectivityTestRequest) =>
    api.post<ConnectivityTestResult>('/providers/connectivity-test', data),
};
