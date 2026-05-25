import { api } from './client';
import type {
  Product,
  CreateProductRequest,
  UpdateProductRequest,
  ProductApiKey,
  CreateProductApiKeyRequest,
  CreateProductApiKeyResponse,
  UpdateProductApiKeyRequest,
} from '@/types/product';

/** API Key 测试响应 */
export interface ApiKeyTestResponse {
  success: boolean;
  latency: number | null;
  modelName: string | null;
  responsePreview: string | null;
  testedAt: string;
  error: {
    code: string;
    message: string;
  } | null;
}

export const productApi = {
  /** 获取所有产品列表 */
  listAll: () =>
    api.get<Product[]>('/products'),

  /** 获取供应商下的产品列表 */
  list: (providerId: number) =>
    api.get<Product[]>('/products', { params: { providerId } }),

  /** 获取产品详情 */
  get: (id: number) =>
    api.get<Product>(`/products/${id}`),

  /** 创建产品 */
  create: (data: CreateProductRequest) =>
    api.post<Product>('/products', data),

  /** 更新产品 */
  update: (id: number, data: UpdateProductRequest) =>
    api.put<Product>(`/products/${id}`, data),

  /** 删除产品 */
  delete: (id: number) =>
    api.delete<void>(`/products/${id}`),
};

/** 渠道凭证 API */
export const channelCredentialApi = {
  /** 获取渠道下的凭证列表 */
  list: (channelId: number) =>
    api.get<ProductApiKey[]>(`/channels/${channelId}/credentials`),

  /** 创建凭证 */
  create: (channelId: number, data: CreateProductApiKeyRequest) =>
    api.post<CreateProductApiKeyResponse>(`/channels/${channelId}/credentials`, data),

  /** 更新凭证 */
  update: (channelId: number, id: number, data: UpdateProductApiKeyRequest) =>
    api.put<ProductApiKey>(`/channels/${channelId}/credentials/${id}`, data),

  /** 删除凭证 */
  delete: (channelId: number, id: number) =>
    api.delete<void>(`/channels/${channelId}/credentials/${id}`),

  /** 测试凭证 */
  test: (channelId: number, id: number) =>
    api.post<ApiKeyTestResponse>(`/channels/${channelId}/credentials/${id}/test`),
};