import type { Status } from './api';

/** API Key 信息 */
export interface ApiKey {
  id: number;
  name: string;
  key: string; // 脱敏后的 Key
  userId: number;
  userName: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建 API Key 请求 */
export interface CreateApiKeyRequest {
  name: string;
  userId: number;
}

/** 创建 API Key 响应（包含完整 Key） */
export interface CreateApiKeyResponse {
  id: number;
  name: string;
  key: string; // 完整 Key，仅创建时返回一次
  userId: number;
  createdAt: string;
}

/** 更新 API Key 请求 */
export interface UpdateApiKeyRequest {
  name?: string;
  status?: Status;
}
