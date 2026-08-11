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
 * 容灾管理 React Query hooks
 *
 * <p>遵循 useApplications 既有模式：queryKey 分层 + mutation 后 invalidate 列表。
 * 覆盖渠道端点熔断应急操作（force-open / force-close）与转移事件流轮询。</p>
 *
 * <p>已退场（本 change 删除）：容灾画像 hooks（ResilienceProfile 退场）、
 * useSwitchCluster（紧切域，域级亲和路由删除）、
 * 故障域 hooks（Cluster 退场，应用级失败策略替代）。</p>
 */
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { resilienceApi } from '@/pages/resilience/api';
import { channelKeys } from '@/services/query/useChannels';
import type {
  FailoverEventQuery,
  ExhaustedEventQuery,
} from '@/types/resilience';

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

// ============ 转移事件流（容灾可观测性，10s 轮询） ============

/**
 * 转移事件流查询 hook
 *
 * <p>容灾总览页轮询渲染转移事件流，10s 自动刷新，回答「最近发生了什么转移」。
 * enabled=false 时暂停轮询（如切到其他 Tab）。</p>
 *
 * @param params 查询参数（since/applicationId/limit），可不传
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
