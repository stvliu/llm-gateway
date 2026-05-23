import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApiKeyApi } from '@/services/api/userApiKey';
import type {
  CreateUserApiKeyRequest,
  UpdateUserApiKeyRequest,
} from '@/types/team';

/** 用户 API Key Query Keys */
export const userApiKeyKeys = {
  all: ['user-api-keys'] as const,
  byUser: (userId: number) => [...userApiKeyKeys.all, 'user', userId] as const,
  detail: (id: number) => [...userApiKeyKeys.all, 'detail', id] as const,
};

/** 查询指定用户的 API Key 列表 */
export function useUserApiKeys(userId: number) {
  return useQuery({
    queryKey: userApiKeyKeys.byUser(userId),
    queryFn: () => userApiKeyApi.listByUser(userId),
    enabled: userId > 0,
  });
}

/** 创建用户 API Key */
export function useCreateUserApiKey(userId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserApiKeyRequest) => userApiKeyApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: userApiKeyKeys.byUser(userId) });
    },
  });
}

/** 更新用户 API Key */
export function useUpdateUserApiKey(userId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateUserApiKeyRequest }) =>
      userApiKeyApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: userApiKeyKeys.byUser(userId) });
    },
  });
}

/** 删除用户 API Key */
export function useDeleteUserApiKey(userId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => userApiKeyApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: userApiKeyKeys.byUser(userId) });
    },
  });
}