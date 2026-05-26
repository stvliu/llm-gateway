/** 模型规格状态枚举 */
export type ModelSpecState = 'ACTIVE' | 'INACTIVE';

/** 模型规格信息（与后端 ModelSpecResponse 一致） */
export interface ModelSpec {
  id: number;
  /** 所属供应商 ID */
  providerId: number;
  /** 供应商模型标识（如 gpt-4o、claude-3-opus） */
  providerModelId: string;
  /** 显示名称 */
  displayName?: string;
  /** 模型族（如 gpt-4、claude-3） */
  modelFamily?: string;
  /** 上下文窗口大小 */
  contextWindow?: number;
  /** 最大输入 Token 数 */
  maxInputTokens?: number;
  /** 最大输出 Token 数 */
  maxOutputTokens?: number;
  /** 模型能力（如 vision、function_calling） */
  capabilities?: Record<string, boolean>;
  /** 支持的模态（如 text、image、audio） */
  modalities?: string[];
  /** 状态 */
  state: ModelSpecState;
  /** 优先级 */
  priority?: number;
  /** 权重 */
  weight?: number;
  createdAt: string;
  updatedAt: string;
}

/** 创建模型规格请求 */
export interface CreateModelSpecRequest {
  /** 所属供应商 ID */
  providerId: number;
  /** 供应商模型标识 */
  providerModelId: string;
  /** 显示名称 */
  displayName?: string;
  /** 模型族 */
  modelFamily?: string;
  /** 上下文窗口大小 */
  contextWindow?: number;
  /** 最大输入 Token 数 */
  maxInputTokens?: number;
  /** 最大输出 Token 数 */
  maxOutputTokens?: number;
  /** 模型能力 */
  capabilities?: Record<string, boolean>;
  /** 支持的模态 */
  modalities?: string[];
  /** 优先级 */
  priority?: number;
  /** 权重 */
  weight?: number;
}

/** 更新模型规格请求 */
export interface UpdateModelSpecRequest {
  /** 供应商模型标识 */
  providerModelId?: string;
  /** 显示名称 */
  displayName?: string;
  /** 模型族 */
  modelFamily?: string;
  /** 上下文窗口大小 */
  contextWindow?: number;
  /** 最大输入 Token 数 */
  maxInputTokens?: number;
  /** 最大输出 Token 数 */
  maxOutputTokens?: number;
  /** 模型能力 */
  capabilities?: Record<string, boolean>;
  /** 支持的模态 */
  modalities?: string[];
  /** 优先级 */
  priority?: number;
  /** 权重 */
  weight?: number;
  /** 状态 */
  state?: ModelSpecState;
}
