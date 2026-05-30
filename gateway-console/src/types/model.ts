/** 模型状态枚举 */
export type ModelState = 'ACTIVE' | 'INACTIVE';

/** 定价信息 */
export interface ModelPricing {
  /** 输入价格（每百万 Token，美元） */
  inputPricePerMillion?: number;
  /** 输出价格（每百万 Token，美元） */
  outputPricePerMillion?: number;
}

/** 模型信息（与后端 ModelResponse 一致） */
export interface Model {
  id: number;
  /** 供应商侧模型标识（如 gpt-4o、claude-3-opus） */
  modelName: string;
  /** 显示名称 */
  displayName?: string;
  /** 模型族（如 gpt-4、claude-3） */
  modelFamily?: string;
  /** 归属供应商 ID */
  providerId?: number;
  /** 归属供应商名称 */
  providerName?: string;
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
  /** 定价信息 */
  pricing?: ModelPricing;
  /** 状态 */
  state: ModelState;
  createdAt: string;
  updatedAt: string;
}

/** 创建模型请求 */
export interface CreateModelRequest {
  /** 供应商侧模型标识 */
  modelName: string;
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
}

/** 更新模型请求 */
export interface UpdateModelRequest {
  /** 供应商侧模型标识 */
  modelName?: string;
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
  /** 状态 */
  state?: ModelState;
}