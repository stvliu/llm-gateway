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
/** 计费模式（与后端 BillingMode 枚举 code 对齐，小写 snake_case） */
export type BillingMode = 'pay_as_you_go' | 'subscription' | 'hybrid' | 'prepaid_package';

/** 渠道生命周期状态（与后端 Channel.State 枚举一致） */
export type ChannelState = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'DEPRECATED' | 'RETIRED';

/** 渠道健康状态（任务 9.x；与后端 ChannelHealthStatus 枚举一致） */
export type ChannelHealthStatus = 'HEALTHY' | 'DEGRADED' | 'FAILED' | 'UNKNOWN';

/** 渠道健康检查触发来源（任务 9.x；与后端 ChannelHealthSource 枚举一致） */
export type ChannelHealthSource = 'CARD' | 'DRAWER' | 'PRECHECK';

/** 健康检查矩阵单行（与后端 HealthCheckMatrixRow 一致） */
export interface ChannelHealthMatrixRow {
  /** 凭证 ID */
  credentialId: number;
  /** 脱敏后的 Key（如 sk-***wxyz） */
  keyMasked: string;
  /** 认证结果 */
  auth: 'PASS' | 'FAIL';
  /** 失败时的错误信息（如 401） */
  authError?: string | null;
  /** 可用模型清单（认证通过时返回） */
  availableModels?: string[] | null;
  /** 延迟毫秒；FAIL 时可能为 null */
  latencyMs?: number | null;
}

/** 健康检查响应（与后端 ChannelHealthCheckResponse 一致） */
export interface ChannelHealthCheckResponse {
  /** 聚合状态 */
  aggregateStatus: ChannelHealthStatus;
  /** 矩阵详情：每个凭证一行 */
  matrix: ChannelHealthMatrixRow[];
  /** 最后一次检查时间 */
  lastHealthCheckAt?: string | null;
  /** 触发来源 */
  source?: ChannelHealthSource;
}

/** 渠道端点响应（与后端 ChannelEndpointResponse 一致） */
export interface ChannelEndpointResponse {
  id: number;
  channelId: number;
  protocol: string;
  endpointUrl: string;
  createdAt: string;
  updatedAt: string;
}

/** 创建渠道端点请求 */
export interface CreateChannelEndpointRequest {
  protocol: string;
  endpointUrl: string;
}

/** 渠道信息（与后端 ChannelResponse 一致） */
export interface Channel {
  id: number;
  providerId: number;
  providerName: string;
  name: string;
  billingMode: string;
  quotaLimit: number | null;
  priority: number;
  weight: number;
  timeout: number | null;
  maxRetries: number | null;
  state: ChannelState;
  endpoints: ChannelEndpointResponse[];
  createdAt: string;
  updatedAt: string;
  /** 最后一次健康检查时间（ISO 字符串）。任务 9.x：后端 ChannelResponse 已透传 */
  lastHealthCheckAt?: string | null;
  /** 最后一次健康检查的聚合状态。任务 9.x */
  lastHealthStatus?: ChannelHealthStatus | null;
  /** 最后一次健康检查触发来源（CARD/DRAWER/PRECHECK）。任务 9.x */
  lastHealthSource?: ChannelHealthSource | null;
}

/** 创建渠道请求（与后端 ChannelRequest 一致，仅基础属性） */
export interface CreateChannelRequest {
  providerId: number;
  name: string;
  billingMode: string;
  quotaLimit?: number | null;
  priority?: number;
  weight?: number;
  timeout?: number | null;
  maxRetries?: number | null;
}

/** 渠道创建/更新响应（与后端 ChannelResponse 一致，包含 id） */
export interface ChannelResponse {
  id: number;
  providerId: number;
  providerName: string;
  name: string;
  billingMode: string;
  quotaLimit: number | null;
  priority: number;
  weight: number;
  timeout: number | null;
  maxRetries: number | null;
  state: ChannelState;
  endpoints: ChannelEndpointResponse[];
  createdAt: string;
  updatedAt: string;
}

/** 更新渠道请求（与后端 ChannelRequest 一致，PUT 使用同一请求体） */
export interface UpdateChannelRequest {
  providerId?: number;
  name?: string;
  billingMode?: BillingMode;
  quotaLimit?: number | null;
  priority?: number;
  weight?: number;
  timeout?: number | null;
  maxRetries?: number | null;
}

/** 渠道凭证（与后端 ChannelCredentialResponse 一致） */
export interface ChannelCredential {
  id: number;
  channelId: number;
  apiKeyPrefix: string;
  apiKeyPlain: string;  // 新增：脱敏格式
  name: string;
  description: string | null;
  weight: number;
  priority: number;
  /** 凭证状态，后端返回（如 ACTIVE/INACTIVE） */
  state?: string;
  createdAt: string;
  updatedAt: string;
}

/** 创建渠道凭证请求（与后端 ChannelCredentialCreateRequest 一致，channelId 在 URL 路径中） */
export interface CreateChannelCredentialRequest {
  apiKey: string;
  priority?: number;
  weight?: number;
  description?: string;
}

/** 创建渠道凭证响应（与后端 ChannelCredentialCreateResponse 一致） */
export interface CreateChannelCredentialResponse {
  id: number;
  apiKeyPlain: string;
}

/** 更新渠道凭证请求（与后端 ChannelCredentialUpdateRequest 一致） */
export interface UpdateChannelCredentialRequest {
  apiKey?: string;  // 新增：可选，传值则替换
  priority?: number;
  weight?: number;
  description?: string;
}

/** API Key 测试响应 */
export interface ApiKeyTestResponse {
  success: boolean;
  latency: number | null;
  modelName: string | null;
  responsePreview: string | null;
  testedAt: string | null;
  error: {
    code: string;
    message: string;
  } | null;
}

/** 渠道模型映射（与后端 ChannelModelResponse 一致） */
export interface ChannelModel {
  id: number;
  channelId: number;
  modelId: number;
  /** 供应商侧模型名称（如 gpt-4o） */
  modelName?: string;
  /** 模型展示名称 */
  displayName?: string;
  /** 模型系列 */
  modelFamily?: string;
  /** 上游模型名，null 表示与 modelName 相同 */
  upstreamModelName?: string | null;
  /** 关联状态 */
  state: string;
}

/** 创建渠道模型映射请求体（与后端 ChannelModelCreateRequest 一致） */
export interface CreateChannelModelRequest {
  modelId: number;
  /** 上游模型名，为空表示与 Model.modelName 相同 */
  upstreamModelName?: string;
}

/** 更新渠道模型映射请求体（与后端 ModelInstanceUpdateRequest 一致） */
export interface UpdateChannelModelRequest {
  /** 新模型 ID，不传表示不更新 */
  modelId?: number;
  /**
   * 新上游模型名
   * - 不传（undefined）：不更新该字段
   * - null：清除上游模型名，走默认值
   * - 字符串：设置新值
   */
  upstreamModelName?: string | null;
}

/** 渠道聚合统计（前端计算） */
export interface ChannelStats {
  /** 端点数量 */
  endpointCount: number;
  /** 凭证数量 */
  credentialCount: number;
  /** 模型映射数量 */
  modelCount: number;
  /** 平均响应时间（毫秒），未测试为 null */
  avgResponseTime: number | null;
}

/** 渠道卡片数据（Channel + Stats） */
export interface ChannelCard extends Channel {
  stats: ChannelStats;
}

/** 供应商分组（用于渠道列表） */
export interface ChannelGroup {
  provider: {
    id: number;
    providerId?: string;
    providerName: string;
  };
  channels: ChannelCard[];
}