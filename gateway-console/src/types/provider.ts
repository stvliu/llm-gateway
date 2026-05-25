/** 提供商状态枚举（简化设计，企业内部场景） */
export type ProviderState = 'ACTIVE' | 'INACTIVE';

/** Provider Key 统计信息 */
export interface ProviderKeyStats {
  providerId: number;
  totalCount: number;
  activeCount: number;
}

/** 供应商信息（与后端 ProviderResponse 一致） */
export interface Provider {
  id: number;
  /** 品牌标识（关联 ProviderCatalog.code） */
  providerId?: string;
  providerName: string;
  /** @deprecated 前端不再使用，图标渲染已由 ProviderIcon 组件接管 */
  iconUrl?: string;
  /** 描述信息 */
  description?: string;
  /** 标签 */
  tags?: unknown;
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
  websiteUrl?: string;
  apiDocUrl?: string;
  priority?: number;
}

/** 更新供应商请求 */
export interface UpdateProviderRequest {
  providerName?: string;
  websiteUrl?: string;
  apiDocUrl?: string;
  priority?: number;
  state?: ProviderState;
}