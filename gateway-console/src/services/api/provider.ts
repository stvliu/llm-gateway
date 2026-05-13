import { api } from './client';
import type { Provider, CreateProviderRequest, UpdateProviderRequest, ProviderKeysResponse } from '@/types/provider';
import type { PageResponse, PageParams, ProviderTypeOption } from '@/types/api';

/** API Key 测试请求 */
interface TestApiKeyRequest {
  providerType: string;
  baseUrl?: string;
  apiKey: string;
}

/** API Key 测试结果 */
interface TestApiKeyResult {
  success: boolean;
  message?: string;
  models?: string[];
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

  /** 测试 API Key 连通性 */
  testApiKey: (data: TestApiKeyRequest) =>
    api.post<TestApiKeyResult>('/providers/test-api-key', data),
};
