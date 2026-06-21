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
