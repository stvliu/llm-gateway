import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiKeyApi } from '@/services/api/apiKey';
import type { CreateApiKeyRequest, UpdateApiKeyRequest } from '@/types/apiKey';

export const apiKeyKeys = {
  all: ['apiKeys'] as const,
  lists: () => [...apiKeyKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...apiKeyKeys.lists(), params] as const,
  details: () => [...apiKeyKeys.all, 'detail'] as const,
  detail: (id: number) => [...apiKeyKeys.details(), id] as const,
  usage: (id: number) => [...apiKeyKeys.all, 'usage', id] as const,
  usageBatch: (params?: Record<string, unknown>) => [...apiKeyKeys.all, 'usage-batch', params] as const,
};

export function useApiKeys(params?: { page?: number; size?: number; userId?: number }) {
  return useQuery({
    queryKey: apiKeyKeys.list(params),
    queryFn: () => apiKeyApi.list(params),
  });
}

export function useApiKey(id: number) {
  return useQuery({
    queryKey: apiKeyKeys.detail(id),
    queryFn: () => apiKeyApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateApiKeyRequest) => apiKeyApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.lists() });
    },
  });
}

export function useUpdateApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateApiKeyRequest }) =>
      apiKeyApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.lists() });
    },
  });
}

export function useDeleteApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiKeyApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.lists() });
    },
  });
}

export function useSetEnabledApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      apiKeyApi.setEnabled(id, enabled),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.lists() });
    },
  });
}

export function useApiKeyUsage(id: number, params?: { startDate?: string; endDate?: string }) {
  return useQuery({
    queryKey: apiKeyKeys.usage(id),
    queryFn: async () => {
      try {
        return await apiKeyApi.getUsage(id, params);
      } catch {
        return null;
      }
    },
    enabled: id > 0,
  });
}

export function useApiKeyUsageBatch(params?: { startDate?: string; endDate?: string; userId?: number }) {
  return useQuery({
    queryKey: apiKeyKeys.usageBatch(params),
    queryFn: async () => {
      try {
        return await apiKeyApi.getUsageBatch(params);
      } catch {
        return [];
      }
    },
  });
}
