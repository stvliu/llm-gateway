import { api } from './client';
import type {
  ProviderApiKey,
  CreateProviderApiKeyRequest,
  CreateProviderApiKeyResponse,
  UpdateProviderApiKeyRequest,
} from '@/types/providerApiKey';
import type { PageResponse, PageParams } from '@/types/api';

export const providerApiKeyApi = {
  /** 获取 Provider API Key 列表 */
  list: (params?: PageParams & { providerId?: number }) =>
    api.get<PageResponse<ProviderApiKey>>('/provider-api-keys', { params }),

  /** 获取 Provider API Key 详情 */
  get: (id: number) =>
    api.get<ProviderApiKey>(`/provider-api-keys/${id}`),

  /** 创建 Provider API Key */
  create: (data: CreateProviderApiKeyRequest) =>
    api.post<CreateProviderApiKeyResponse>('/provider-api-keys', data),

  /** 更新 Provider API Key */
  update: (id: number, data: UpdateProviderApiKeyRequest) =>
    api.put<ProviderApiKey>(`/provider-api-keys/${id}`, data),

  /** 删除 Provider API Key */
  delete: (id: number) =>
    api.delete<void>(`/provider-api-keys/${id}`),

  /** 启用/禁用 Provider API Key */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<ProviderApiKey>(`/provider-api-keys/${id}/enabled`, null, { params: { enabled } }),
};