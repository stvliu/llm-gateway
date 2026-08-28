/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
  /** 模型描述（来自数据源） */
  description?: string;
  /** 发布日期 */
  releaseDate?: string;
  /** 数据源最后更新日期 */
  lastUpdated?: string;
  /** 许可证（如 MIT） */
  license?: string;
  /** 是否开源权重 */
  openWeights?: boolean;
  /** 基准测试分数 [{name, score, metric, source}] */
  benchmarks?: Array<Record<string, unknown>>;
  /** 权重/模型卡片链接 [{label, url}] */
  weights?: Array<Record<string, unknown>>;
  /** 数据来源：MODELS_DEV / BUILTIN / MANUAL */
  source?: string;
  /** 数据源外部 ID（如 openai/gpt-4o），同步幂等匹配键 */
  externalId?: string;
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