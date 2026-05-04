import type { Status } from './api';

/** 模型类型 */
export type ModelType = 'CHAT' | 'COMPLETION' | 'EMBEDDING' | 'IMAGE' | 'AUDIO';

/** 模型信息 */
export interface Model {
  id: number;
  name: string;
  code: string;
  providerId: number;
  providerName: string;
  type: ModelType;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建模型请求 */
export interface CreateModelRequest {
  name: string;
  code: string;
  providerId: number;
  type: ModelType;
}

/** 更新模型请求 */
export interface UpdateModelRequest {
  name?: string;
  status?: Status;
}
