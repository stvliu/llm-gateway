import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { channelModelApi } from '@/services/api/channelModel';
import type { CreateChannelModelRequest, UpdateUpstreamModelNameRequest } from '@/types/channelModel';

export const channelModelKeys = {
  all: ['channel-models'] as const,
  lists: () => [...channelModelKeys.all, 'list'] as const,
  list: (channelId: number) => [...channelModelKeys.lists(), channelId] as const,
};

/** 获取渠道下的模型关联列表 */
export function useChannelModels(channelId: number) {
  return useQuery({
    queryKey: channelModelKeys.list(channelId),
    queryFn: () => channelModelApi.list(channelId),
    enabled: !!channelId,
  });
}

/** 创建渠道模型关联 */
export function useCreateChannelModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, data }: { channelId: number; data: CreateChannelModelRequest }) =>
      channelModelApi.create(channelId, data),
    onSuccess: (_, { channelId }) => {
      queryClient.invalidateQueries({ queryKey: channelModelKeys.list(channelId) });
    },
  });
}

/** 删除渠道模型关联 */
export function useDeleteChannelModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, id }: { channelId: number; id: number }) =>
      channelModelApi.delete(channelId, id),
    onSuccess: (_, { channelId }) => {
      queryClient.invalidateQueries({ queryKey: channelModelKeys.list(channelId) });
    },
  });
}

/** 启用/禁用渠道模型关联 */
export function useSetEnabledChannelModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, id, enabled }: { channelId: number; id: number; enabled: boolean }) =>
      channelModelApi.setEnabled(channelId, id, enabled),
    onSuccess: (_, { channelId }) => {
      queryClient.invalidateQueries({ queryKey: channelModelKeys.list(channelId) });
    },
  });
}

/** 更新上游模型名 */
export function useUpdateUpstreamModelName() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, id, data }: { channelId: number; id: number; data: UpdateUpstreamModelNameRequest }) =>
      channelModelApi.updateUpstreamModelName(channelId, id, data),
    onSuccess: (_, { channelId }) => {
      queryClient.invalidateQueries({ queryKey: channelModelKeys.list(channelId) });
    },
  });
}