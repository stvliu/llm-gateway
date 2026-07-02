/**
 * 容灾领域类型定义
 *
 * <p>与后端 simplify-resilience-architecture 变更后的 DTO 对齐：
 * <ul>
 *   <li>Cluster ← ClusterResponse / ClusterRequest（瘦身：删 region/priority/healthStatus，加 description）</li>
 *   <li>CircuitBreakerStateResponse ← ChannelController.CircuitBreakerStateResponse</li>
 *   <li>FailoverEvent ← FailoverEventResponse（新增 commonCauseSkip 共因跳过标记）</li>
 * </ul>
 * </p>
 *
 * <p>已退场概念（随 ResilienceProfile/L2/DomainHealth/PinnedModel/会话亲和删除）：
 * ResilienceProfile、ResilienceMode、ClusterHealthStatus、DegradationFallback、SwitchClusterRequest。
 * 紧切域（SwitchCluster）随域级亲和路由删除而退场。</p>
 */

/** 熔断器状态（与后端 CircuitBreakerState 枚举一致） */
export type CircuitBreakerState = 'CLOSED' | 'OPEN' | 'HALF_OPEN';

/**
 * 故障域（与后端 ClusterResponse 一致）
 *
 * <p>Cluster 是 Channel 的故障域分组，语义为「跨供应商故障独立性分组」：
 * 同组 Channel 共享共因特征，L1 转移阶段依据 clusterId 做共因跳过。
 * 分组可跨供应商，也可供应商内细分。</p>
 *
 * <p>字段瘦身（Task 6）：删除 region（就近路由未实现）、priority（转移顺序归
 * ApplicationChannel.priority）、healthStatus（域级聚合已删，不持久化）。</p>
 */
export interface Cluster {
  /** 故障域 ID */
  id: number;
  /** 故障域编码，全局唯一（如 openai-primary / azure-openai-shared） */
  code: string;
  /** 故障域名称 */
  name: string;
  /** 归属供应商 ID（物理 ID；与 clusterId 正交，不作共因依据） */
  providerId: number;
  /** 共因特征说明（可空，如「Azure-OpenAI 底层依赖 OpenAI 模型，共因」） */
  description?: string | null;
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
  /** 共因特征说明（可空） */
  description?: string | null;
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

/**
 * 转移事件响应（与后端 FailoverEventResponse 一致）
 *
 * <p>容灾总览页轮询渲染转移事件流与耗尽告警。errorType/decision 以枚举名字符串返回，
 * 前端按字符串展示，避免耦合后端枚举类型。</p>
 *
 * <p>Task 9：新增 commonCauseSkip（是否共因跳过）标记，区分「真实失败转移」与
 * 「同域共因跳过转移」。前端在转移事件流高亮共因跳过事件。</p>
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
  /** 转移决策（L1 枚举名） */
  decision?: string | null;
  /** 是否候选全部耗尽 */
  exhausted: boolean;
  /**
   * 是否共因跳过（Task 9 新增）
   *
   * <p>true 表示同域共因跳过（非真实失败，不计入 lastException）；
   * false 表示真实 L1 失败转移。前端高亮共因跳过事件。</p>
   *
   * <p>注：后端 FailoverEventResponse 透传该字段；未返回时按 false 处理。</p>
   */
  commonCauseSkip?: boolean;
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
