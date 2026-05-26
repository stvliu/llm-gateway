import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { modelSpecApi } from '@/services/api/modelSpec';
import type { CreateModelSpecRequest, UpdateModelSpecRequest } from '@/types/modelSpec';

export const modelSpecKeys = {
  all: ['modelSpecs'] as const,
  lists: () => [...modelSpecKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...modelSpecKeys.lists(), params] as const,
  details: () => [...modelSpecKeys.all, 'detail'] as const,
  detail: (id: number) => [...modelSpecKeys.details(), id] as const,
};

/** 获取模型规格列表（可按供应商筛选） */
export function useModelSpecs(providerId?: number) {
  return useQuery({
    queryKey: modelSpecKeys.list({ providerId }),
    queryFn: () => modelSpecApi.list({ providerId }),
  });
}

/** 获取模型规格详情 */
export function useModelSpec(id: number) {
  return useQuery({
    queryKey: modelSpecKeys.detail(id),
    queryFn: () => modelSpecApi.get(id),
    enabled: id > 0,
  });
}

/** 创建模型规格 */
export function useCreateModelSpec() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateModelSpecRequest) => modelSpecApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.lists() });
    },
  });
}

/** 更新模型规格 */
export function useUpdateModelSpec() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateModelSpecRequest }) =>
      modelSpecApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.lists() });
    },
  });
}

/** 删除模型规格 */
export function useDeleteModelSpec() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => modelSpecApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.lists() });
    },
  });
}

/** 启用/禁用模型规格 */
export function useSetEnabledModelSpec() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      modelSpecApi.setEnabled(id, enabled),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.lists() });
    },
  });
}
