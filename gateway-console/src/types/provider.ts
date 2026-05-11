import type { ProviderType } from './api';
import type { ProviderApiKey } from './providerApiKey';

export type { ProviderType };

/** Provider Key 统计信息 */
export interface ProviderKeyStats {
  providerId: number;
  totalCount: number;
  activeCount: number;
}

/** Provider Key 信息响应 */
export interface ProviderKeysResponse {
  defaultKey: ProviderApiKey | null;
  keys: ProviderApiKey[];
}

/** 提供商状态枚举（简化设计，企业内部场景） */
export type ProviderState = 'ACTIVE' | 'DISABLED' | 'DELETED';

/** 供应商信息（与后端 ProviderResponse 一致） */
export interface Provider {
  id: number;
  providerName: string;
  providerType: ProviderType;
  baseUrl: string;
  websiteUrl?: string;
  apiDocUrl?: string;
  priority?: number;
  state: ProviderState;
  createdAt: string;
  updatedAt: string;
  keyStats?: ProviderKeyStats;
}

/** 嵌套的 API Key 请求（创建供应商时使用） */
export interface NestedApiKeyRequest {
  keyName: string;
  apiKey: string;
  priority?: number;
  weight?: number;
  isDefault?: boolean;
}

/** 嵌套的模型请求（创建供应商时使用） */
export interface NestedModelRequest {
  providerModelId: string;
  displayName?: string;
  contextWindow?: number;
  inputPrice?: number;
  outputPrice?: number;
  capabilities?: Record<string, boolean>;
}

/** 创建供应商请求 */
export interface CreateProviderRequest {
  providerName: string;
  providerType: ProviderType;
  baseUrl: string;
  websiteUrl?: string;
  apiDocUrl?: string;
  priority?: number;
  /** 嵌套的 API Key 列表（可选） */
  apiKeys?: NestedApiKeyRequest[];
  /** 嵌套的模型列表（可选） */
  models?: NestedModelRequest[];
}

/** 更新供应商请求 */
export interface UpdateProviderRequest {
  providerName?: string;
  baseUrl?: string;
  websiteUrl?: string;
  apiDocUrl?: string;
  priority?: number;
  state?: ProviderState;
}