/**
 * 容灾管理 API 封装
 *
 * <p>对接后端 simplify-resilience-architecture 变更后的端点：
 * <ul>
 *   <li>故障域 CRUD：/api/v1/resilience/clusters（Cluster 瘦身，字段 code/name/providerId/description）</li>
 *   <li>渠道端点熔断应急：/api/v1/channels/{cid}/endpoints/{eid}/circuit-breaker/*</li>
 *   <li>转移事件流：/api/v1/resilience/events、/api/v1/resilience/events/exhausted</li>
 * </ul>
 * </p>
 *
 * <p>已退场端点（本 change 删除）：
 * <ul>
 *   <li>容灾画像 CRUD（/resilience/profiles）—— ResilienceProfile 实体退场</li>
 *   <li>紧切域（PUT /channels/{id}/cluster）—— 域级亲和路由（ClusterAffinityRouter）删除</li>
 * </ul>
 * </p>
 *
 * <p>遵循 applicationApi 既有模式：基于 ./client 的 api 对象，每方法中文注释。</p>
 */
import { api } from '@/services/api/client';
import type {
  Cluster,
  ClusterRequest,
  CircuitBreakerStateResponse,
  FailoverEvent,
  FailoverEventQuery,
  ExhaustedEventQuery,
} from '@/types/resilience';

/** 容灾管理 API */
export const resilienceApi = {
  /** 故障域 */
  clusters: {
    /** 获取故障域列表 */
    list: () => api.get<Cluster[]>('/resilience/clusters'),

    /** 获取故障域详情 */
    getById: (id: number) => api.get<Cluster>(`/resilience/clusters/${id}`),

    /** 创建故障域 */
    create: (data: ClusterRequest) =>
      api.post<Cluster>('/resilience/clusters', data),

    /** 更新故障域 */
    update: (id: number, data: ClusterRequest) =>
      api.put<Cluster>(`/resilience/clusters/${id}`, data),
  },

  /** 渠道端点熔断器应急操作 */
  circuitBreaker: {
    /** 一键强制熔断端点（摘流量） */
    forceOpen: (channelId: number, endpointId: number) =>
      api.post<CircuitBreakerStateResponse>(
        `/channels/${channelId}/endpoints/${endpointId}/circuit-breaker/force-open`,
      ),

    /** 一键强制恢复端点（解除手动熔断） */
    forceClose: (channelId: number, endpointId: number) =>
      api.post<CircuitBreakerStateResponse>(
        `/channels/${channelId}/endpoints/${endpointId}/circuit-breaker/force-close`,
      ),

    /** 查询端点熔断器当前状态 */
    getState: (channelId: number, endpointId: number) =>
      api.get<CircuitBreakerStateResponse>(
        `/channels/${channelId}/endpoints/${endpointId}/circuit-breaker/state`,
      ),
  },

  /** 转移事件流（对接后端 ResilienceEventController） */
  events: {
    /**
     * 查询转移事件流（按 occurredAt 倒序）
     *
     * <p>未传字段不放入 params，避免后端收到 undefined 字符串。</p>
     */
    list: (params?: FailoverEventQuery) => {
      const query = pickDefined(params);
      return api.get<FailoverEvent[]>('/resilience/events', { params: query });
    },

    /**
     * 查询耗尽告警事件（exhausted=true，按 occurredAt 倒序）
     *
     * <p>since 不传时由后端 Service 层补默认窗口（最近 1 小时）。</p>
     */
    exhausted: (params?: ExhaustedEventQuery) => {
      const query = pickDefined(params);
      return api.get<FailoverEvent[]>('/resilience/events/exhausted', {
        params: query,
      });
    },
  },
};

/**
 * 从可选 params 对象中剔除 undefined 字段，仅保留已传字段
 *
 * <p>避免 axios 把 { since: undefined } 序列化成 since=undefined 污染请求。</p>
 */
function pickDefined<T extends object>(params?: T): Partial<T> {
  if (!params) return {};
  const result: Partial<T> = {};
  for (const key of Object.keys(params) as (keyof T)[]) {
    if (params[key] !== undefined) {
      result[key] = params[key];
    }
  }
  return result;
}
