import type { Status } from './api';

/** Provider API Key 信息 */
export interface ProviderApiKey {
  id: number;
  providerId: number;
  providerName: string;
  name: string;
  apiKeyPreview: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建 Provider API Key 请求 */
export interface CreateProviderApiKeyRequest {
  providerId: number;
  name: string;
  apiKey: string;
}

/** 创建 Provider API Key 响应 */
export interface CreateProviderApiKeyResponse {
  id: number;
  providerId: number;
  name: string;
  apiKey: string;
}

/** 更新 Provider API Key 请求 */
export interface UpdateProviderApiKeyRequest {
  name?: string;
  status?: Status;
}
