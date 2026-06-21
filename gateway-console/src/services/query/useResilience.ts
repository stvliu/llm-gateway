/**
 * 容灾管理 React Query hooks
 *
 * <p>遵循 useApplications 既有模式：queryKey 分层 + mutation 后 invalidate 列表。
 * 覆盖容灾画像、故障域 CRUD 与渠道应急操作（熔断/恢复/紧切域）。</p>
 */
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { resilienceApi } from '@/pages/resilience/api';
import { channelKeys } from '@/services/query/useChannels';
import type {
  ResilienceProfileRequest,
  ClusterRequest,
  FailoverEventQuery,
  ExhaustedEventQuery,
} from '@/types/resilience';

/** 容灾画像 Query Keys */
export const resilienceProfileKeys = {
  all: ['resilience-profiles'] as const,
  lists: () => [...resilienceProfileKeys.all, 'list'] as const,
  details: () => [...resilienceProfileKeys.all, 'detail'] as const,
  detail: (id: number) => [...resilienceProfileKeys.details(), id] as const,
};

/** 故障域 Query Keys */
export const clusterKeys = {
  all: ['resilience-clusters'] as const,
  lists: () => [...clusterKeys.all, 'list'] as const,
  details: () => [...clusterKeys.all, 'detail'] as const,
  detail: (id: number) => [...clusterKeys.details(), id] as const,
};

/** 转移事件流 Query Keys */
export const failoverEventKeys = {
  all: ['resilience-events'] as const,
  /** 事件流列表（按查询参数区分缓存） */
  list: (params?: FailoverEventQuery) =>
    [...failoverEventKeys.all, 'list', params ?? {}] as const,
  /** 耗尽告警列表（按查询参数区分缓存） */
  exhausted: (params?: ExhaustedEventQuery) =>
    [...failoverEventKeys.all, 'exhausted', params ?? {}] as const,
};

/** 转移事件流 10s 轮询间隔（容灾可观测性，回答「现在稳不稳」） */
const FAILOVER_EVENT_REFETCH_INTERVAL = 10_000;

// ============ 容灾画像 ============

/** 容灾画像列表 */
export function useResilienceProfiles() {
  return useQuery({
    queryKey: resilienceProfileKeys.lists(),
    queryFn: () => resilienceApi.profiles.list(),
  });
}

/** 容灾画像详情 */
export function useResilienceProfile(id: number) {
  return useQuery({
    queryKey: resilienceProfileKeys.detail(id),
    queryFn: () => resilienceApi.profiles.getById(id),
    enabled: !!id,
  });
}

/** 创建容灾画像 */
export function useCreateResilienceProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: ResilienceProfileRequest) => resilienceApi.profiles.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: resilienceProfileKeys.lists() });
    },
  });
}

/** 更新容灾画像 */
export function useUpdateResilienceProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: ResilienceProfileRequest }) =>
      resilienceApi.profiles.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: resilienceProfileKeys.lists() });
    },
  });
}

// ============ 故障域 ============

/** 故障域列表 */
export function useClusters() {
  return useQuery({
    queryKey: clusterKeys.lists(),
    queryFn: () => resilienceApi.clusters.list(),
  });
}

/** 故障域详情 */
export function useCluster(id: number) {
  return useQuery({
    queryKey: clusterKeys.detail(id),
    queryFn: () => resilienceApi.clusters.getById(id),
    enabled: !!id,
  });
}

/** 创建故障域 */
export function useCreateCluster() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: ClusterRequest) => resilienceApi.clusters.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: clusterKeys.lists() });
    },
  });
}

/** 更新故障域 */
export function useUpdateCluster() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: ClusterRequest }) =>
      resilienceApi.clusters.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: clusterKeys.lists() });
    },
  });
}

// ============ 渠道应急操作 ============

/** 一键强制熔断端点 */
export function useForceOpenCircuitBreaker() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, endpointId }: { channelId: number; endpointId: number }) =>
      resilienceApi.circuitBreaker.forceOpen(channelId, endpointId),
    onSuccess: () => {
      // 熔断状态变更后刷新渠道详情（端点状态可能影响展示）
      qc.invalidateQueries({ queryKey: channelKeys.allChannels() });
    },
  });
}

/** 一键强制恢复端点 */
export function useForceCloseCircuitBreaker() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, endpointId }: { channelId: number; endpointId: number }) =>
      resilienceApi.circuitBreaker.forceClose(channelId, endpointId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: channelKeys.allChannels() });
    },
  });
}

/** 查询端点熔断器状态 */
export function useCircuitBreakerState(channelId: number, endpointId: number) {
  return useQuery({
    queryKey: ['circuit-breaker-state', channelId, endpointId] as const,
    queryFn: () => resilienceApi.circuitBreaker.getState(channelId, endpointId),
    enabled: !!channelId && !!endpointId,
  });
}

/** 紧急切换渠道到目标故障域 */
export function useSwitchCluster() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, clusterId }: { channelId: number; clusterId: number }) =>
      resilienceApi.switchCluster(channelId, clusterId),
    onSuccess: () => {
      // 紧切域后刷新渠道详情与故障域列表（域健康可能变化）
      qc.invalidateQueries({ queryKey: channelKeys.allChannels() });
      qc.invalidateQueries({ queryKey: clusterKeys.lists() });
    },
  });
}

// ============ 转移事件流（容灾可观测性，10s 轮询） ============

/**
 * 转移事件流查询 hook
 *
 * <p>容灾总览页轮询渲染转移事件流，10s 自动刷新，回答「最近发生了什么转移」。
 * enabled=false 时暂停轮询（如切到其他 Tab）。</p>
 *
 * @param params 查询参数（since/applicationId/clusterId/limit），可不传
 * @param options.enabled 是否启用查询（默认 true）
 */
export function useFailoverEvents(
  params?: FailoverEventQuery,
  options?: { enabled?: boolean },
) {
  return useQuery({
    queryKey: failoverEventKeys.list(params),
    queryFn: () => resilienceApi.events.list(params),
    refetchInterval: FAILOVER_EVENT_REFETCH_INTERVAL,
    enabled: options?.enabled ?? true,
  });
}

/**
 * 耗尽告警查询 hook
 *
 * <p>独立轮询 exhausted=true 事件，10s 自动刷新。空列表表示「无耗尽告警」绿色提示。</p>
 *
 * @param params 查询参数（since/limit），可不传
 * @param options.enabled 是否启用查询（默认 true）
 */
export function useExhaustedEvents(
  params?: ExhaustedEventQuery,
  options?: { enabled?: boolean },
) {
  return useQuery({
    queryKey: failoverEventKeys.exhausted(params),
    queryFn: () => resilienceApi.events.exhausted(params),
    refetchInterval: FAILOVER_EVENT_REFETCH_INTERVAL,
    enabled: options?.enabled ?? true,
  });
}
