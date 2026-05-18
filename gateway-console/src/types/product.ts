import type { PageResponse } from './api';

/** 产品类型 */
export type ProductType = 'pay_as_you_go' | 'subscription_coding' | 'subscription_token';

/** 产品状态 */
export type ProductState = 'active' | 'inactive' | 'deleted';

/** 产品 API Key 状态 */
export type ProductApiKeyState = 'active' | 'inactive' | 'deleted';

/** 产品 */
export interface Product {
  id: number;
  providerId: number;
  name: string;
  productType: ProductType;
  models: string[];
  endpoints: Record<string, string>;
  quotaLimit: number | null;
  state: ProductState;
  createdAt: string;
  updatedAt: string;
}

/** 产品 API Key（后端 ProductApiKeyResponse） */
export interface ProductApiKey {
  id: number;
  productId: number;
  apiKeyMasked: string;
  baseUrl: string;
  priority: number;
  weight: number;
  state: ProductApiKeyState;
  description: string;
  createdAt: string;
  updatedAt: string;
}

/** 产品 API Key 创建请求 */
export interface CreateProductApiKeyRequest {
  productId: number;
  apiKey: string;
  baseUrl?: string;
  priority?: number;
  weight?: number;
  description?: string;
}

/** 产品 API Key 创建响应 */
export interface CreateProductApiKeyResponse {
  id: number;
  apiKeyMasked: string;
  apiKeyPlain: string;
}

/** 产品 API Key 更新请求 */
export interface UpdateProductApiKeyRequest {
  baseUrl?: string;
  priority?: number;
  weight?: number;
  state?: ProductApiKeyState;
  description?: string;
}

/** 创建产品请求 */
export interface CreateProductRequest {
  providerId: number;
  name: string;
  productType: ProductType;
  models: string[];
  endpoints: Record<string, string>;
  quotaLimit?: number;
}

/** 更新产品请求 */
export interface UpdateProductRequest {
  name?: string;
  productType?: ProductType;
  models?: string[];
  endpoints?: Record<string, string>;
  quotaLimit?: number;
  state?: ProductState;
}

/** 产品分页结果 */
export type ProductPageResult = PageResponse<Product>;