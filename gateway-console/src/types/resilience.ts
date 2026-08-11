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
/**
 * 容灾领域类型定义
 *
 * <p>与后端 simplify-resilience-architecture 变更后的 DTO 对齐：
 * <ul>
 *   <li>CircuitBreakerStateResponse ← ChannelController.CircuitBreakerStateResponse</li>
 *   <li>FailoverEvent ← FailoverEventResponse</li>
 * </ul>
 * </p>
 *
 * <p>已退场概念（随 ResilienceProfile/L2/DomainHealth/PinnedModel/会话亲和/故障域删除）：
 * ResilienceProfile、ResilienceMode、ClusterHealthStatus、DegradationFallback、
 * SwitchClusterRequest、Cluster、ClusterRequest。
 * 紧切域（SwitchCluster）随域级亲和路由删除而退场；
 * 故障域（Cluster）随应用级失败策略删除而退场。</p>
 */

/** 熔断器状态（与后端 CircuitBreakerState 枚举一致） */
export type CircuitBreakerState = 'CLOSED' | 'OPEN' | 'HALF_OPEN';

/**
 * 熔断器状态响应（与后端 ChannelController.CircuitBreakerStateResponse 一致）
 *
 * <p>force-open / force-close / state 端点均返回此结构。</p>
 */
export interface CircuitBreakerStateResponse {
  /** 熔断器状态名（CLOSED/OPEN/HALF_OPEN） */
  state: CircuitBreakerState;
}

/**
 * 转移事件响应（与后端 FailoverEventResponse 一致）
 *
 * <p>容灾总览页轮询渲染转移事件流与耗尽告警。errorType/decision 以枚举名字符串返回，
 * 前端按字符串展示，避免耦合后端枚举类型。</p>
 */
export interface FailoverEvent {
  /** 事件 ID */
  id: number;
  /** OpenTelemetry Trace ID */
  traceId?: string | null;
  /** 应用 ID */
  applicationId?: number | null;
  /** 失败候选的渠道 ID */
  fromChannelId?: number | null;
  /** 失败候选的端点 ID */
  fromEndpointId?: number | null;
  /** 转移目标候选的渠道 ID（exhausted 时为 null） */
  toChannelId?: number | null;
  /** 转移目标候选的端点 ID（exhausted 时为 null） */
  toEndpointId?: number | null;
  /** 触发转移的上游错误类型（ProviderErrorType 枚举名） */
  errorType?: string | null;
  /** 转移决策（L1 枚举名） */
  decision?: string | null;
  /** 是否候选全部耗尽 */
  exhausted: boolean;
  /** 转移发生时间（ISO-8601 Instant） */
  occurredAt: string;
}

/** 转移事件流查询参数（GET /resilience/events） */
export interface FailoverEventQuery {
  /** 起始时间过滤（ISO-8601 Instant，可选） */
  since?: string;
  /** 应用 ID 过滤（可选） */
  applicationId?: number;
  /** 返回条数（默认 100，上限 500） */
  limit?: number;
}

/** 耗尽告警查询参数（GET /resilience/events/exhausted） */
export interface ExhaustedEventQuery {
  /** 起始时间过滤（ISO-8601 Instant，可选，默认最近 1 小时） */
  since?: string;
  /** 返回条数（默认 50） */
  limit?: number;
}
