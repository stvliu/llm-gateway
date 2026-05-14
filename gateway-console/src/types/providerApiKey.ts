/** Provider API Key 状态枚举（简化设计，企业内部场景）
 * 运行时健康状态由熔断器管理，不在此定义
 */
export type ProviderApiKeyState = 'ACTIVE' | 'DISABLED' | 'DELETED';

/** Provider API Key 信息 */
export interface ProviderApiKey {
  id: number;
  providerId: number;
  keyName: string;
  apiKey?: string;
  keyHint: string;
  priority?: number;
  weight?: number;
  isDefault: boolean;
  state: ProviderApiKeyState;
  rpmLimit?: number;
  tpmLimit?: number;
  lastUsedAt?: string;
  createdAt: string;
  updatedAt: string;
}

/** 创建 Provider API Key 请求 */
export interface CreateProviderApiKeyRequest {
  providerId: number;
  keyName: string;
  apiKey: string;
  priority?: number;
  weight?: number;
  isDefault?: boolean;
}

/** 创建 Provider API Key 响应 */
export interface CreateProviderApiKeyResponse {
  id: number;
  providerId: number;
  keyName: string;
  apiKey: string;
  state: ProviderApiKeyState;
}

/** 更新 Provider API Key 请求 */
export interface UpdateProviderApiKeyRequest {
  keyName?: string;
  apiKey?: string;
  priority?: number;
  weight?: number;
  isDefault?: boolean;
  state?: ProviderApiKeyState;
  rpmLimit?: number;
  tpmLimit?: number;
}