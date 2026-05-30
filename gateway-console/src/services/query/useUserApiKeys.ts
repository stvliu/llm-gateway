import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApiKeyApi } from '@/services/api/userApiKey';
import type { CreateUserApiKeyRequest, UpdateUserApiKeyRequest } from '@/types/team';

export const userApiKeyKeys = {
  all: ['userApiKeys'] as const,
  list: () => [...userApiKeyKeys.all, 'list'] as const,
  byUser: (userId: number) => [...userApiKeyKeys.all, 'user', userId] as const,
  detail: (id: number) => [...userApiKeyKeys.all, 'detail', id] as const,
};

/** 获取指定用户的 API Key 列表 */
export function useUserApiKeys(userId: number) {
  return useQuery({
    queryKey: userApiKeyKeys.byUser(userId),
    queryFn: () => userApiKeyApi.listByUser(userId),
    enabled: userId > 0,
  });
}

/** 创建用户 API Key（用户维度） */
export function useCreateUserApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserApiKeyRequest) => userApiKeyApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userApiKeyKeys.all });
    },
  });
}

/** 更新用户 API Key */
export function useUpdateUserApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateUserApiKeyRequest }) =>
      userApiKeyApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userApiKeyKeys.all });
    },
  });
}

/** 轮换用户 API Key（生成新 Key，旧 Key 失效） */
export function useRotateUserApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => userApiKeyApi.rotate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userApiKeyKeys.all });
    },
  });
}

/** 删除用户 API Key */
export function useDeleteUserApiKey(userId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => userApiKeyApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userApiKeyKeys.byUser(userId) });
      queryClient.invalidateQueries({ queryKey: userApiKeyKeys.all });
    },
  });
}