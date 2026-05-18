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

export const productApi = {
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

export const productApiKeyApi = {
  /** 获取产品下的 API Key 列表 */
  list: (productId: number) =>
    api.get<ProductApiKey[]>(`/product-api-keys`, { params: { productId } }),

  /** 创建 API Key */
  create: (data: CreateProductApiKeyRequest) =>
    api.post<CreateProductApiKeyResponse>('/product-api-keys', data),

  /** 更新 API Key */
  update: (id: number, data: UpdateProductApiKeyRequest) =>
    api.put<ProductApiKey>(`/product-api-keys/${id}`, data),

  /** 删除 API Key */
  delete: (id: number) =>
    api.delete<void>(`/product-api-keys/${id}`),
};