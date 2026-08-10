/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useQuery, useQueries, useMutation, useQueryClient } from '@tanstack/react-query';
import { channelApi } from '@/services/api/channel';
import type {
  CreateChannelRequest,
  UpdateChannelRequest,
  CreateChannelEndpointRequest,
  CreateChannelCredentialRequest,
  UpdateChannelCredentialRequest,
  CreateChannelModelRequest,
  UpdateChannelModelRequest,
} from '@/types/channel';

export const channelKeys = {
  all: ['channels'] as const,
  lists: () => [...channelKeys.all, 'list'] as const,
  list: (providerId?: number) => [...channelKeys.lists(), providerId] as const,
  allChannels: () => [...channelKeys.all, 'all'] as const,
  details: () => [...channelKeys.all, 'detail'] as const,
  detail: (id: number) => [...channelKeys.details(), id] as const,
  credentials: (channelId: number) => [...channelKeys.all, 'credentials', channelId] as const,
  endpoints: (channelId: number) => [...channelKeys.all, 'endpoints', channelId] as const,
};

/** 获取所有渠道列表（不带 providerId 筛选） */
export function useAllChannels() {
  return useQuery({
    queryKey: channelKeys.allChannels(),
    queryFn: () => channelApi.list(),
  });
}

/** 获取供应商下的渠道列表 */
export function useChannels(providerId?: number) {
  return useQuery({
    queryKey: channelKeys.list(providerId),
    queryFn: () => channelApi.list({ providerId }),
    enabled: providerId !== undefined,
  });
}

/** 批量获取多个供应商的渠道列表 */
export function useChannelsBatch(providerIds: number[]) {
  return useQueries({
    queries: providerIds.map((providerId) => ({
      queryKey: channelKeys.list(providerId),
      queryFn: () => channelApi.list({ providerId }),
      enabled: !!providerId,
    })),
  });
}

/** 获取渠道详情 */
export function useChannel(id: number) {
  return useQuery({
    queryKey: channelKeys.detail(id),
    queryFn: () => channelApi.get(id),
    enabled: !!id,
  });
}

/** 创建渠道（返回含 id 的 ChannelResponse） */
export function useCreateChannel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateChannelRequest) => channelApi.create(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.list(variables.providerId) });
      queryClient.invalidateQueries({ queryKey: channelKeys.allChannels() });
    },
  });
}

/** 更新渠道 */
export function useUpdateChannel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateChannelRequest }) =>
      channelApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: channelKeys.lists() });
    },
  });
}

/** 渠道状态转换 */
export function useTransitionChannelState() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, targetState, reason }: { id: number; targetState: string; reason?: string }) =>
      channelApi.transitionState(id, targetState, reason),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: channelKeys.lists() });
      queryClient.invalidateQueries({ queryKey: channelKeys.allChannels() });
    },
  });
}

/** 删除渠道 */
export function useDeleteChannel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: number; providerId: number }) =>
      channelApi.delete(id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.list(variables.providerId) });
      queryClient.invalidateQueries({ queryKey: channelKeys.allChannels() });
    },
  });
}

/** ---- 渠道端点 ---- */

/** 添加渠道端点 */
export function useAddChannelEndpoint() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, data }: { channelId: number; data: CreateChannelEndpointRequest }) =>
      channelApi.addEndpoint(channelId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.detail(variables.channelId) });
      queryClient.invalidateQueries({ queryKey: channelKeys.lists() });
    },
  });
}

/** 删除渠道端点 */
export function useRemoveChannelEndpoint() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, endpointId }: { channelId: number; endpointId: number }) =>
      channelApi.removeEndpoint(channelId, endpointId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.detail(variables.channelId) });
      queryClient.invalidateQueries({ queryKey: channelKeys.lists() });
    },
  });
}

/** 更新渠道端点 */
export function useUpdateChannelEndpoint() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, endpointId, data }: { channelId: number; endpointId: number; data: CreateChannelEndpointRequest }) =>
      channelApi.updateEndpoint(channelId, endpointId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.detail(variables.channelId) });
      queryClient.invalidateQueries({ queryKey: channelKeys.lists() });
    },
  });
}

/** ---- 渠道凭证 ---- */

/** 获取渠道下的凭证列表 */
export function useChannelCredentials(channelId: number) {
  return useQuery({
    queryKey: channelKeys.credentials(channelId),
    queryFn: () => channelApi.listCredentials(channelId),
    enabled: !!channelId,
  });
}

/** 批量获取多个渠道的凭证列表 */
export function useChannelCredentialsBatch(channelIds: number[]) {
  return useQueries({
    queries: channelIds.map((channelId) => ({
      queryKey: channelKeys.credentials(channelId),
      queryFn: () => channelApi.listCredentials(channelId),
      enabled: !!channelId,
    })),
  });
}

/** 创建渠道凭证 */
export function useCreateChannelCredential() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, data }: { channelId: number; data: CreateChannelCredentialRequest }) =>
      channelApi.createCredential(channelId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.credentials(variables.channelId) });
    },
  });
}

/** 更新渠道凭证 */
export function useUpdateChannelCredential() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, id, data }: { channelId: number; id: number; data: UpdateChannelCredentialRequest }) =>
      channelApi.updateCredential(channelId, id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.credentials(variables.channelId) });
    },
  });
}

/** 删除渠道凭证 */
export function useDeleteChannelCredential() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, id }: { channelId: number; id: number }) =>
      channelApi.deleteCredential(channelId, id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.credentials(variables.channelId) });
    },
  });
}

/** 测试渠道凭证 */
export function useTestChannelCredential() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, id }: { channelId: number; id: number }) =>
      channelApi.testCredential(channelId, id),
    onSuccess: (_, { channelId }) => {
      // 测试完成后刷新凭证列表（可能影响状态显示）
      queryClient.invalidateQueries({ queryKey: channelKeys.credentials(channelId) });
    },
  });
}

/** ---- 别名导出（兼容消费方命名） ---- */

/** 创建通道（接受 { providerId, data } 格式参数） */
export function useAddChannel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ data }: { providerId: number; data: CreateChannelRequest }) =>
      channelApi.create(data),
    onSuccess: (_, { providerId }) => {
      queryClient.invalidateQueries({ queryKey: channelKeys.list(providerId) });
      queryClient.invalidateQueries({ queryKey: channelKeys.allChannels() });
    },
  });
}

/** ---- 渠道模型映射 ---- */

/** 获取渠道的模型映射列表 */
export function useChannelModels(channelId: number) {
  return useQuery({
    queryKey: [...channelKeys.detail(channelId), 'models'],
    queryFn: () => channelApi.listModels(channelId),
    enabled: channelId > 0,
  });
}

/** 创建渠道模型映射 */
export function useCreateChannelModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, data }: { channelId: number; data: CreateChannelModelRequest }) =>
      channelApi.createModel(channelId, data),
    onSuccess: (_, { channelId }) => {
      queryClient.invalidateQueries({ queryKey: [...channelKeys.detail(channelId), 'models'] });
    },
  });
}

/** 删除渠道模型映射 */
export function useDeleteChannelModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, modelId }: { channelId: number; modelId: number }) =>
      channelApi.deleteModel(channelId, modelId),
    onSuccess: (_, { channelId }) => {
      queryClient.invalidateQueries({ queryKey: [...channelKeys.detail(channelId), 'models'] });
    },
  });
}

/** 更新模型映射的上游模型名 */
export function useUpdateChannelModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, modelId, upstreamModelName }: { channelId: number; modelId: number; upstreamModelName: string }) =>
      channelApi.updateUpstreamModelName(channelId, modelId, upstreamModelName),
    onSuccess: (_, { channelId }) => {
      queryClient.invalidateQueries({ queryKey: [...channelKeys.detail(channelId), 'models'] });
    },
  });
}

/** 更新模型映射（支持修改 modelId 和 upstreamModelName） */
export function useUpdateChannelModelFull() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, modelId, data }: { channelId: number; modelId: number; data: UpdateChannelModelRequest }) =>
      channelApi.updateModel(channelId, modelId, data),
    onSuccess: (_, { channelId }) => {
      queryClient.invalidateQueries({ queryKey: [...channelKeys.detail(channelId), 'models'] });
    },
  });
}

/** 模型实例状态转换 */
export function useTransitionChannelModelState() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, modelId, targetState }: { channelId: number; modelId: number; targetState: string }) =>
      channelApi.transitionModelState(channelId, modelId, targetState),
    onSuccess: (_, { channelId }) => {
      queryClient.invalidateQueries({ queryKey: [...channelKeys.detail(channelId), 'models'] });
    },
  });
}

/** 批量获取多个渠道的模型映射列表 */
export function useChannelModelsBatch(channelIds: number[]) {
  return useQueries({
    queries: channelIds.map((channelId) => ({
      queryKey: [...channelKeys.detail(channelId), 'models'],
      queryFn: () => channelApi.listModels(channelId),
      enabled: !!channelId,
    })),
  });
}