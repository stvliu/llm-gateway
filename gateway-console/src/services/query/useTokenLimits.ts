import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { tokenLimitApi } from '@/services/api/tokenLimit';
import type { PageParams } from '@/types/api';
import type {
  CreateTokenLimitRequest,
  UpdateTokenLimitRequest,
} from '@/types/tokenLimit';

export const tokenLimitKeys = {
  all: ['tokenLimits'] as const,
  lists: () => [...tokenLimitKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...tokenLimitKeys.lists(), params] as const,
  details: () => [...tokenLimitKeys.all, 'detail'] as const,
  detail: (id: number) => [...tokenLimitKeys.details(), id] as const,
};

export function useTokenLimits(params?: PageParams & { scope?: 'USER' | 'API_KEY'; targetId?: number }) {
  return useQuery({
    queryKey: tokenLimitKeys.list(params as Record<string, unknown>),
    queryFn: () => tokenLimitApi.list(params),
  });
}

export function useTokenLimit(id: number) {
  return useQuery({
    queryKey: tokenLimitKeys.detail(id),
    queryFn: () => tokenLimitApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateTokenLimit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTokenLimitRequest) => tokenLimitApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}

export function useUpdateTokenLimit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateTokenLimitRequest }) =>
      tokenLimitApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}

export function useDeleteTokenLimit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => tokenLimitApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}

export function useResetTokenLimitUsage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => tokenLimitApi.resetUsage(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}
