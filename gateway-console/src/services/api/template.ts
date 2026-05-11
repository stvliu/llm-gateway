import { api } from './client';
import type {
  ProviderTemplate,
  CreateTemplateRequest,
  UpdateTemplateRequest,
  ApplyTemplateRequest,
  ApplyTemplateResult,
  TemplateListParams,
  MarketStatus,
  SpringPage,
} from '@/types/template';
import type { PageResponse, PageParams } from '@/types/api';

/** 转换 Spring Data Page 为 PageResponse */
function adaptPage<T>(page: SpringPage<T>): PageResponse<T> {
  return {
    items: page.content,
    pagination: {
      page: page.pageable.pageNumber,
      limit: page.pageable.pageSize,
      total: page.totalElements,
      totalPages: page.totalPages,
    },
  };
}

export const templateApi = {
  /** 获取模板列表 */
  list: async (params?: TemplateListParams & PageParams): Promise<PageResponse<ProviderTemplate>> => {
    const page = await api.get<SpringPage<ProviderTemplate>>('/templates', { params });
    return adaptPage(page);
  },

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

  /** 更新模板市场状态 */
  updateMarketState: (id: number, marketState: MarketStatus) =>
    api.patch<ProviderTemplate>(`/templates/${id}/market-state`, { marketState }),

  /** 应用模板创建 Provider */
  apply: (id: number, data: ApplyTemplateRequest) =>
    api.post<ApplyTemplateResult>(`/templates/${id}/apply`, data),

  /** 导出单个模板 */
  exportTemplate: (id: number) =>
    api.get<void>(`/templates/${id}/export`),

  /** 批量导出模板 */
  exportBatch: (ids: number[]) =>
    api.get<void>('/templates/export', { params: { ids: ids.join(',') } }),

  /** 导入模板 */
  import: (formData: FormData) =>
    api.post<ProviderTemplate[]>('/templates/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
};
