import type { Status } from './api';

/** Provider API Key 信息 */
export interface ProviderApiKey {
  id: number;
  providerId: number;
  keyName: string;
  keyHint: string;
  priority?: number;
  weight?: number;
  isDefault: boolean;
  status: Status;
  rpmLimit?: number;
  tpmLimit?: number;
  lastUsedAt?: string;
  expiresAt?: string;
  createdAt: string;
  updatedAt: string;
}

/** 创建 Provider API Key 请求 */
export interface CreateProviderApiKeyRequest {
  providerId: number;
  keyName: string;
  apiKey: string;
}

/** 创建 Provider API Key 响应 */
export interface CreateProviderApiKeyResponse {
  id: number;
  providerId: number;
  keyName: string;
  apiKey: string;
}

/** 更新 Provider API Key 请求 */
export interface UpdateProviderApiKeyRequest {
  keyName?: string;
  apiKey?: string;
  priority?: number;
  weight?: number;
  isDefault?: boolean;
  status?: Status;
  rpmLimit?: number;
  tpmLimit?: number;
  expiresAt?: string;
}
