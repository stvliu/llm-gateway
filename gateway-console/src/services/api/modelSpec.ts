import { api } from './client';
import type { ModelSpec, CreateModelSpecRequest, UpdateModelSpecRequest } from '@/types/modelSpec';

/** 模型规格 API */
export const modelSpecApi = {
  /** 获取模型规格列表 */
  list: (params?: Record<string, unknown>) =>
    api.get<ModelSpec[]>('/model-specs', { params }),

  /** 获取模型规格详情 */
  get: (id: number) =>
    api.get<ModelSpec>(`/model-specs/${id}`),

  /** 创建模型规格 */
  create: (data: CreateModelSpecRequest) =>
    api.post<ModelSpec>('/model-specs', data),

  /** 更新模型规格 */
  update: (id: number, data: UpdateModelSpecRequest) =>
    api.put<ModelSpec>(`/model-specs/${id}`, data),

  /** 删除模型规格 */
  delete: (id: number) =>
    api.delete<void>(`/model-specs/${id}`),

  /** 启用/禁用模型规格 */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<ModelSpec>(`/model-specs/${id}/state`, null, { params: { enabled } }),
};
