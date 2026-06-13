import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { providerApi } from '@/services/api/provider';
import type { CreateProviderRequest, UpdateProviderRequest, ConnectivityTestRequest } from '@/types/provider';

export const providerKeys = {
  all: ['providers'] as const,
  lists: () => [...providerKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...providerKeys.lists(), params] as const,
  details: () => [...providerKeys.all, 'detail'] as const,
  detail: (id: number) => [...providerKeys.details(), id] as const,
  keys: (id: number) => [...providerKeys.all, 'keys', id] as const,
};

export function useProviders(params?: { page?: number; size?: number; limit?: number }) {
  // 支持 size 或 limit 参数，统一转换为 limit
  const limit = params?.limit ?? params?.size;
  return useQuery({
    queryKey: providerKeys.list({ ...params, limit }),
    queryFn: () => providerApi.list({ ...params, limit }),
  });
}

export function useProvider(id: number) {
  return useQuery({
    queryKey: providerKeys.detail(id),
    queryFn: () => providerApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateProviderRequest) => providerApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: providerKeys.lists() });
    },
  });
}

export function useUpdateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateProviderRequest }) =>
      providerApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: providerKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: providerKeys.lists() });
    },
  });
}

export function useDeleteProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => providerApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: providerKeys.lists() });
    },
  });
}

export function useTestConnectivity() {
  return useMutation({
    mutationFn: (data: ConnectivityTestRequest) => providerApi.testConnectivity(data),
  });
}