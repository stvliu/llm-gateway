/**
 * 容灾领域类型定义
 *
 * <p>与后端 4.11a 交付的 DTO 对齐：
 * <ul>
 *   <li>ResilienceProfile ← ResilienceProfileResponse / ResilienceProfileRequest</li>
 *   <li>Cluster ← ClusterResponse / ClusterRequest</li>
 *   <li>CircuitBreakerStateResponse ← ChannelController.CircuitBreakerStateResponse</li>
 * </ul>
 * </p>
 */

/** 容灾模式档位（与后端 ResilienceProfile.mode 枚举一致） */
export type ResilienceMode = 'STANDARD' | 'STRICT' | 'AGGRESSIVE';

/** 熔断器状态（与后端 CircuitBreakerState 枚举一致） */
export type CircuitBreakerState = 'CLOSED' | 'OPEN' | 'HALF_OPEN';

/** 故障域健康聚合状态（与后端 ClusterHealthStatus 枚举一致） */
export type ClusterHealthStatus = 'HEALTHY' | 'DEGRADED' | 'DOWN';

/**
 * 容灾画像（与后端 ResilienceProfileResponse 一致）
 *
 * <p>容灾画像聚合根：管理员面向「容灾模式」档位 + 「降级兜底」开关，
 * 专家字段（会话亲和/模型锁定/超时）默认折叠。</p>
 */
export interface ResilienceProfile {
  /** 画像 ID */
  id: number;
  /** 画像编码，全局唯一 */
  code: string;
  /** 画像名称 */
  name: string;
  /** 容灾模式档位（STANDARD/STRICT/AGGRESSIVE） */
  mode: ResilienceMode;
  /** 是否启用 L2 模型级降级兜底 */
  enableL2ModelDegradation: boolean;
  /** L2 降级最大深度（0 表示禁用降级） */
  degradationMaxDepth: number;
  /** 是否启用会话亲和 */
  enableSessionAffinity: boolean;
  /** 会话亲和 TTL（分钟） */
  sessionAffinityTtlMinutes: number;
  /** 是否启用模型锁定 */
  enablePinnedModel: boolean;
  /** 锁定模型 ID（可空） */
  pinnedModelId: number | null;
  /** 请求超时秒数（0 表示用渠道默认） */
  timeout: number;
  /** 创建时间 */
  createdAt?: string;
  /** 更新时间 */
  updatedAt?: string;
}

/** 创建/更新容灾画像请求（与后端 ResilienceProfileRequest 一致） */
export interface ResilienceProfileRequest {
  /** 画像编码，全局唯一 */
  code: string;
  /** 画像名称 */
  name: string;
  /** 容灾模式档位 */
  mode: ResilienceMode;
  /** 是否启用 L2 模型级降级兜底 */
  enableL2ModelDegradation: boolean;
  /** L2 降级最大深度 */
  degradationMaxDepth: number;
  /** 是否启用会话亲和 */
  enableSessionAffinity: boolean;
  /** 会话亲和 TTL（分钟） */
  sessionAffinityTtlMinutes: number;
  /** 是否启用模型锁定 */
  enablePinnedModel: boolean;
  /** 锁定模型 ID（可空） */
  pinnedModelId: number | null;
  /** 请求超时秒数（0 表示用渠道默认） */
  timeout: number;
}

/**
 * 故障域（与后端 ClusterResponse 一致）
 *
 * <p>Cluster 是 Channel 的故障域分组，同组 Channel 共享共因特征。
 * 容灾转移规则：故障域内优先 → 整域故障才跨域。</p>
 */
export interface Cluster {
  /** 故障域 ID */
  id: number;
  /** 故障域编码，全局唯一 */
  code: string;
  /** 故障域名称 */
  name: string;
  /** 归属供应商 ID（物理 ID） */
  providerId: number;
  /** 区域标识（如 'us-east' / 'sg'） */
  region?: string | null;
  /** 优先级（数值越小越优先） */
  priority: number;
  /** 域级健康聚合状态 */
  healthStatus: ClusterHealthStatus;
  /** 创建时间 */
  createdAt?: string;
  /** 更新时间 */
  updatedAt?: string;
}

/** 创建/更新故障域请求（与后端 ClusterRequest 一致） */
export interface ClusterRequest {
  /** 故障域编码，全局唯一 */
  code: string;
  /** 故障域名称 */
  name: string;
  /** 归属供应商 ID */
  providerId: number;
  /** 区域标识 */
  region?: string | null;
  /** 优先级 */
  priority: number;
}

/**
 * 熔断器状态响应（与后端 ChannelController.CircuitBreakerStateResponse 一致）
 *
 * <p>force-open / force-close / state 端点均返回此结构。</p>
 */
export interface CircuitBreakerStateResponse {
  /** 熔断器状态名（CLOSED/OPEN/HALF_OPEN） */
  state: CircuitBreakerState;
}

/** 紧切域请求体（与后端 SwitchClusterRequest 一致） */
export interface SwitchClusterRequest {
  /** 目标故障域 ID */
  clusterId: number;
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
  /** 冗余：失败候选所属故障域 ID（可空） */
  fromClusterId?: number | null;
  /** 冗余：转移目标所属故障域 ID（可空） */
  toClusterId?: number | null;
  /** 触发转移的上游错误类型（ProviderErrorType 枚举名） */
  errorType?: string | null;
  /** 转移决策（L1/L2 枚举名） */
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
  /** 故障域 ID 过滤（可选） */
  clusterId?: number;
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

/**
 * 面向管理员的「降级兜底」两字段
 *
 * <p>由 ResilienceProfile 的 enableL2ModelDegradation + degradationMaxDepth 推导。
 * enabled=false 等价于关闭 L2 降级。</p>
 */
export interface DegradationFallback {
  /** 是否启用降级兜底 */
  enabled: boolean;
  /** 降级最大深度（1-5，enabled=false 时为 0） */
  maxDepth: number;
}
