import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { modelApi } from '@/services/api/model';
import type { CreateModelRequest, UpdateModelRequest } from '@/types/model';

export const modelKeys = {
  all: ['models'] as const,
  lists: () => [...modelKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...modelKeys.lists(), params] as const,
  details: () => [...modelKeys.all, 'detail'] as const,
  detail: (id: number) => [...modelKeys.details(), id] as const,
};

export function useModels(params?: { page?: number; size?: number; limit?: number; providerId?: number }, options?: { enabled?: boolean }) {
  // 支持 size 或 limit 参数，统一转换为 limit
  const limit = params?.limit ?? params?.size;
  return useQuery({
    queryKey: modelKeys.list({ ...params, limit }),
    queryFn: () => modelApi.list({ ...params, limit }),
    enabled: options?.enabled ?? true,
  });
}

export function useModel(id: number) {
  return useQuery({
    queryKey: modelKeys.detail(id),
    queryFn: () => modelApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateModelRequest) => modelApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}

export function useUpdateModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateModelRequest }) =>
      modelApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: modelKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}

export function useDeleteModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => modelApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}

export function useSetEnabledModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      modelApi.setEnabled(id, enabled),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: modelKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}
