/** Gateway API Key 状态枚举（简化设计，企业内部场景） */
export type GatewayApiKeyState = 'ACTIVE' | 'DISABLED' | 'DELETED';

/** API Key 信息 */
export interface ApiKey {
  id: number;
  name: string;
  key: string; // 脱敏后的 Key
  userId: number;
  username: string;
  state: GatewayApiKeyState;
  lastUsedAt?: string;
  ipWhitelist?: string[];
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
  rawKey: string; // 完整 Key，仅创建时返回一次
  userId: number;
  username: string;
  state: GatewayApiKeyState;
  createdAt: string;
}

/** 更新 API Key 请求 */
export interface UpdateApiKeyRequest {
  name?: string;
  ipWhitelist?: string[];
  state?: GatewayApiKeyState;
}