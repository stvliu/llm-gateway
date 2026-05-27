/** 模型类型 */
export type ModelType = 'CHAT' | 'COMPLETION' | 'EMBEDDING' | 'IMAGE' | 'AUDIO';

/** 模型状态枚举 */
export type ModelState = 'ACTIVE' | 'INACTIVE';

/** 模型信息（与后端 ModelResponse 一致，定价信息已迁移到 Product） */
export interface Model {
  id: number;
  providerId: number;
  providerName: string;
  providerModelId?: string;
  displayName?: string;
  contextWindow?: number;
  capabilities?: Record<string, boolean>;
  state: ModelState;
  createdAt: string;
  updatedAt: string;
}

/** 创建模型请求 */
export interface CreateModelRequest {
  providerId: number;
  providerModelId?: string;
  displayName?: string;
  contextWindow?: number;
  capabilities?: Record<string, boolean>;
}

/** 更新模型请求 */
export interface UpdateModelRequest {
  displayName?: string;
  contextWindow?: number;
  capabilities?: Record<string, boolean>;
  state?: ModelState;
}