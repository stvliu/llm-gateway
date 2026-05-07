import { api } from './client';
import type {
  ProviderTemplate,
  CreateTemplateRequest,
  UpdateTemplateRequest,
  ApplyTemplateRequest,
  ApplyTemplateResult,
  TemplateListParams,
} from '@/types/template';
import type { PageResponse, PageParams } from '@/types/api';

export const templateApi = {
  /** 获取模板列表 */
  list: (params?: TemplateListParams & PageParams) =>
    api.get<PageResponse<ProviderTemplate>>('/templates', { params }),

  /** 获取模板详情 */
  get: (id: number) =>
    api.get<ProviderTemplate>(`/templates/${id}`),

  /** 创建模板 */
  create: (data: CreateTemplateRequest) =>
    api.post<ProviderTemplate>('/templates', data),

  /** 更新模板 */
  update: (id: number, data: UpdateTemplateRequest) =>
    api.put<ProviderTemplate>(`/templates/${id}`, data),

  /** 删除模板 */
  delete: (id: number) =>
    api.delete<void>(`/templates/${id}`),

  /** 发布模板到公共市场 */
  publish: (id: number) =>
    api.post<void>(`/templates/${id}/publish`),

  /** 应用模板创建 Provider */
  apply: (id: number, data: ApplyTemplateRequest) =>
    api.post<ApplyTemplateResult>(`/templates/${id}/apply`, data),

  /** 导出单个模板 */
  exportTemplate: (id: number) =>
    api.get<void>(`/templates/${id}/export`),

  /** 批量导出模板 */
  exportBatch: (ids: number[]) =>
    api.get<void>('/templates/export/batch', { params: { ids } }),

  /** 导入模板 */
  import: (formData: FormData) =>
    api.post<ProviderTemplate[]>('/templates/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
};
