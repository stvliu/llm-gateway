import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { providerApiKeyApi } from '@/services/api/providerApiKey';
import type { PageParams } from '@/types/api';
import type {
  CreateProviderApiKeyRequest,
  UpdateProviderApiKeyRequest,
} from '@/types/providerApiKey';

export const providerApiKeyKeys = {
  all: ['providerApiKeys'] as const,
  lists: () => [...providerApiKeyKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...providerApiKeyKeys.lists(), params] as const,
  details: () => [...providerApiKeyKeys.all, 'detail'] as const,
  detail: (id: number) => [...providerApiKeyKeys.details(), id] as const,
};

export function useProviderApiKeys(params?: PageParams & { providerId?: number }) {
  return useQuery({
    queryKey: providerApiKeyKeys.list(params),
    queryFn: () => providerApiKeyApi.list(params),
  });
}

export function useProviderApiKey(id: number) {
  return useQuery({
    queryKey: providerApiKeyKeys.detail(id),
    queryFn: () => providerApiKeyApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateProviderApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateProviderApiKeyRequest) => providerApiKeyApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.lists() });
    },
  });
}

export function useUpdateProviderApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateProviderApiKeyRequest }) =>
      providerApiKeyApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.lists() });
    },
  });
}

export function useDeleteProviderApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => providerApiKeyApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.lists() });
    },
  });
}

export function useSetEnabledProviderApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      providerApiKeyApi.setEnabled(id, enabled),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.lists() });
    },
  });
}
