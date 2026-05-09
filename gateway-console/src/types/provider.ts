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

/** 供应商信息（与后端 ProviderResponse 一致） */
export interface Provider {
  id: number;
  providerName: string;
  providerType: ProviderType;
  baseUrl: string;
  websiteUrl?: string;
  apiDocUrl?: string;
  priority?: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
  keyStats?: ProviderKeyStats;
}

/** 创建供应商请求 */
export interface CreateProviderRequest {
  providerName: string;
  providerType: ProviderType;
  baseUrl: string;
  websiteUrl?: string;
  apiDocUrl?: string;
  priority?: number;
}

/** 更新供应商请求 */
export interface UpdateProviderRequest {
  providerName?: string;
  baseUrl?: string;
  websiteUrl?: string;
  apiDocUrl?: string;
  priority?: number;
  enabled?: boolean;
}