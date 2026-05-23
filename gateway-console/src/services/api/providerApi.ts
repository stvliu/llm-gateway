import apiClient from './client';
import type {
  Provider,
  CreateProviderRequest,
  UpdateProviderRequest,
} from '../../types/provider';
import type { PageResponse } from '../../types/api';

const BASE_URL = '/providers';

export const providerApi = {
  /** 获取供应商列表 */
  list: async (params?: {
    keyword?: string;
    state?: string;
    page?: number;
    limit?: number;
  }): Promise<PageResponse<Provider>> => {
    const response = await apiClient.get(BASE_URL, { params });
    return response.data;
  },

  /** 获取供应商详情 */
  getById: async (id: number): Promise<Provider> => {
    const response = await apiClient.get(`${BASE_URL}/${id}`);
    return response.data;
  },

  /** 创建供应商 */
  create: async (data: CreateProviderRequest): Promise<Provider> => {
    const response = await apiClient.post(BASE_URL, data);
    return response.data;
  },

  /** 更新供应商 */
  update: async (id: number, data: UpdateProviderRequest): Promise<Provider> => {
    const response = await apiClient.put(`${BASE_URL}/${id}`, data);
    return response.data;
  },

  /** 删除供应商 */
  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`${BASE_URL}/${id}`);
  },

  /** 启用/禁用供应商 */
  setEnabled: async (id: number, enabled: boolean): Promise<Provider> => {
    const response = await apiClient.patch(`${BASE_URL}/${id}/state`, null, {
      params: { enabled },
    });
    return response.data;
  },

  /** 获取供应商名称列表 */
  getProviderNames: async (): Promise<string[]> => {
    const response = await apiClient.get(`${BASE_URL}/names`);
    return response.data;
  },
};