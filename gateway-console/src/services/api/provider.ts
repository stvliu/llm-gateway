import { api } from './client';
import type { Provider, CreateProviderRequest, UpdateProviderRequest } from '@/types/provider';
import type { PageResponse, PageParams } from '@/types/api';

export const providerApi = {
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
};
