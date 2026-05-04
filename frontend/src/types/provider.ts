import type { Status } from './api';

/** 渠道类型 */
export type ProviderType = 'OPENAI' | 'ANTHROPIC' | 'GOOGLE' | 'AZURE' | 'CUSTOM';

/** 渠道信息 */
export interface Provider {
  id: number;
  name: string;
  code: string;
  type: ProviderType;
  baseUrl: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建渠道请求 */
export interface CreateProviderRequest {
  name: string;
  code: string;
  type: ProviderType;
  baseUrl: string;
}

/** 更新渠道请求 */
export interface UpdateProviderRequest {
  name?: string;
  baseUrl?: string;
  status?: Status;
}
