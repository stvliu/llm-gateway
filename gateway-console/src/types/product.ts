/** 协议类型（与后端 ProtocolGateway.getProtocolName() 对应） */
export type EndpointProtocol = 'openai' | 'anthropic';

/** 协议信息（从 /api/protocols 获取） */
export interface ProtocolInfo {
  name: string;
  label: string;
}

/** 产品状态 */
export type ProductState = 'ACTIVE' | 'DISABLED' | 'DELETED';

/** 产品信息（与后端 ProductResponse 一致） */
export interface Product {
  id: number;
  providerId: number;
  providerName: string;
  name: string;
  productType: string;
  endpoints: Record<string, string>;
  inputPrice: number | null;
  outputPrice: number | null;
  reasoningPrice: number | null;
  cacheReadPrice: number | null;
  cacheWritePrice: number | null;
  inputAudioPrice: number | null;
  outputAudioPrice: number | null;
  quotaLimit: number | null;
  state: ProductState;
  createdAt: string;
  updatedAt: string;
}

/** 创建产品请求 */
export interface CreateProductRequest {
  providerId: number;
  name: string;
  productType?: string;
  endpoints?: Record<string, string>;
  inputPrice?: number | null;
  outputPrice?: number | null;
}

/** 更新产品请求 */
export interface UpdateProductRequest {
  name?: string;
  endpoints?: Record<string, string>;
}

/** 产品 API Key 状态 */
export type ProductApiKeyState = 'ACTIVE' | 'INACTIVE' | 'DELETED';

/** 产品 API Key */
export interface ProductApiKey {
  id: number;
  productId: number;
  name: string;
  apiKeyPrefix: string;
  priority: number;
  weight: number;
  description?: string;
  state: ProductApiKeyState;
  createdAt: string;
  updatedAt: string;
}

/** 创建产品 API Key 请求 */
export interface CreateProductApiKeyRequest {
  productId: number;
  apiKey: string;
  priority?: number;
  weight?: number;
  description?: string;
}

/** 创建产品 API Key 响应 */
export interface CreateProductApiKeyResponse {
  id: number;
  apiKeyPrefix: string;
  apiKeyPlain: string;
}

/** 更新产品 API Key 请求 */
export interface UpdateProductApiKeyRequest {
  priority?: number;
  weight?: number;
  description?: string;
  state?: ProductApiKeyState;
}