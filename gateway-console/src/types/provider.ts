/** 提供商状态枚举 */
export type ProviderState = 'ACTIVE' | 'INACTIVE';

/** 供应商密钥统计 */
export interface ProviderKeyStats {
  activeCount: number;
  totalCount: number;
}

/** 供应商信息（与后端 ProviderResponse 一致） */
export interface Provider {
  id: number;
  /** 品牌标识（如 openai、anthropic），全局唯一 */
  providerId?: string;
  providerName: string;
  /** @deprecated 图标渲染已由 ProviderIcon 组件接管 */
  iconUrl?: string;
  description?: string;
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
  /** 品牌标识（如 openai、anthropic），全局唯一 */
  code: string;
  /** 供应商名称 */
  providerName: string;
  /** 供应商描述 */
  description?: string;
  /** 官网地址 */
  websiteUrl?: string;
  /** API 文档地址 */
  apiDocUrl?: string;
}

/** 更新供应商请求 */
export interface UpdateProviderRequest {
  providerName?: string;
  websiteUrl?: string;
  apiDocUrl?: string;
  priority?: number;
  state?: ProviderState;
}

/** 连通性测试请求 */
export interface ConnectivityTestRequest {
  protocolName: string;
  baseUrl?: string;
  apiKey: string;
  model?: string;
}

/** 连通性测试单级结果 */
export interface ConnectivityTestLevelResult {
  success: boolean;
  message: string;
  latencyMs: number | null;
  errorType: string | null;
  models: string[] | null;
}

/** 连通性测试结果 */
export interface ConnectivityTestResult {
  success: boolean;
  message: string;
  models: string[] | null;
  level1: ConnectivityTestLevelResult | null;
  level2: ConnectivityTestLevelResult | null;
  totalLatencyMs: number;
}