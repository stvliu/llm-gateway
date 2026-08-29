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
import { modelApi } from '@/services/api/model';
import type { CreateModelRequest, UpdateModelRequest, CopyModelRequest } from '@/types/model';
import type { PageParams } from '@/types/api';

export const modelKeys = {
  all: ['models'] as const,
  lists: () => [...modelKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...modelKeys.lists(), params] as const,
  details: () => [...modelKeys.all, 'detail'] as const,
  detail: (id: number) => [...modelKeys.details(), id] as const,
};

/** 获取模型列表（服务端分页，返回完整 PageResponse） */
export function useModels(params?: PageParams) {
  return useQuery({
    queryKey: modelKeys.list({ ...params }),
    queryFn: () => modelApi.list(params),
  });
}

/** 创建模型 */
export function useCreateModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateModelRequest) => modelApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}

/** 更新模型 */
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

/** 删除模型 */
export function useDeleteModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => modelApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}

/** 清除模型字段人工锁定（恢复 models.dev 同步覆盖权限） */
export function useUnlockModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => modelApi.unlock(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: modelKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}

/** 复制模型（继承源规格生成新模型，成功后刷新模型列表） */
export function useCopyModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: CopyModelRequest }) =>
      modelApi.copy(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}

/** 启用/禁用模型 */
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