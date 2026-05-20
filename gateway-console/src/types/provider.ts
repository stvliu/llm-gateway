/** 供应商类型 */
export type ProviderType = 'OPENAI' | 'ANTHROPIC';

/** Provider Key 统计信息 */
export interface ProviderKeyStats {
  providerId: number;
  totalCount: number;
  activeCount: number;
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
  state?: ProviderState;
}