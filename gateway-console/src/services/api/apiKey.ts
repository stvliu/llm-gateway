import { api } from './client';
import type { ApiKey, ApiKeyUsage, CreateApiKeyRequest, CreateApiKeyResponse, UpdateApiKeyRequest } from '@/types/apiKey';
import type { PageResponse, PageParams } from '@/types/api';

export const apiKeyApi = {
  /** 获取 API Key 列表 */
  list: (params?: PageParams & { userId?: number }) =>
    api.get<PageResponse<ApiKey>>('/api-keys', { params }),

  /** 获取 API Key 详情 */
  get: (id: number) =>
    api.get<ApiKey>(`/api-keys/${id}`),

  /** 创建 API Key */
  create: (data: CreateApiKeyRequest) =>
    api.post<CreateApiKeyResponse>('/api-keys', data),

  /** 更新 API Key */
  update: (id: number, data: UpdateApiKeyRequest) =>
    api.put<ApiKey>(`/api-keys/${id}`, data),

  /** 删除 API Key */
  delete: (id: number) =>
    api.delete<void>(`/api-keys/${id}`),

  /** 启用/禁用 API Key */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<ApiKey>(`/api-keys/${id}/enabled`, null, { params: { enabled } }),

  /** 获取单个 API Key 用量统计 */
  getUsage: (id: number, params?: { startDate?: string; endDate?: string }) =>
    api.get<ApiKeyUsage>(`/api-keys/${id}/stats`, { params }),

  /** 批量获取 API Key 用量统计 */
  getUsageBatch: (params?: { startDate?: string; endDate?: string; userId?: number }) =>
    api.get<ApiKeyUsage[]>('/api-keys/stats', { params }),
};
