/** 模型类型 */
export type ModelType = 'CHAT' | 'COMPLETION' | 'EMBEDDING' | 'IMAGE' | 'AUDIO';

/** 模型状态枚举 */
export type ModelState = 'ACTIVE' | 'DISABLED' | 'DELETED';

/** 模型信息（与后端 ModelResponse 一致） */
export interface Model {
  id: number;
  providerId: number;
  providerName: string;
  providerModelId?: string;
  displayName?: string;
  contextWindow?: number;
  inputPrice?: number;
  outputPrice?: number;
  capabilities?: Record<string, boolean>;
  state: ModelState;
  /** 渠道优先级（用于 FAILOVER 策略，值越小越优先） */
  priority?: number;
  /** 渠道权重（用于 WEIGHTED 策略，加权随机选择） */
  weight?: number;
  createdAt: string;
  updatedAt: string;
}

/** 创建模型请求 */
export interface CreateModelRequest {
  providerId: number;
  providerModelId?: string;
  displayName?: string;
  contextWindow?: number;
  inputPrice?: number;
  outputPrice?: number;
  capabilities?: Record<string, boolean>;
  /** 渠道优先级（用于 FAILOVER 策略，值越小越优先，默认 100） */
  priority?: number;
  /** 渠道权重（用于 WEIGHTED 策略，加权随机选择，默认 100） */
  weight?: number;
}

/** 更新模型请求 */
export interface UpdateModelRequest {
  displayName?: string;
  contextWindow?: number;
  inputPrice?: number;
  outputPrice?: number;
  capabilities?: Record<string, boolean>;
  state?: ModelState;
  /** 渠道优先级（用于 FAILOVER 策略，值越小越优先） */
  priority?: number;
  /** 渠道权重（用于 WEIGHTED 策略，加权随机选择） */
  weight?: number;
}