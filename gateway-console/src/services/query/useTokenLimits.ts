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
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { tokenLimitApi } from '@/services/api/tokenLimit';
import type { TokenLimitQueryParams } from '@/types/tokenLimit';

export const tokenLimitKeys = {
  all: ['tokenLimits'] as const,
  lists: () => [...tokenLimitKeys.all, 'list'] as const,
  list: (params?: TokenLimitQueryParams) => [...tokenLimitKeys.lists(), params] as const,
  detail: (id: number) => [...tokenLimitKeys.all, 'detail', id] as const,
};

/** 分页查询 Token 限额 */
export function useTokenLimits(params?: TokenLimitQueryParams) {
  return useQuery({
    queryKey: tokenLimitKeys.list(params),
    queryFn: () => tokenLimitApi.list(params),
  });
}

/** 创建 Token 限额 */
export function useCreateTokenLimit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Parameters<typeof tokenLimitApi.create>[0]) => tokenLimitApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}

/** 更新 Token 限额 */
export function useUpdateTokenLimit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof tokenLimitApi.update>[1] }) =>
      tokenLimitApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}

/** 删除 Token 限额 */
export function useDeleteTokenLimit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => tokenLimitApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}

/** 重置 Token 限额已使用量 */
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
