import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { applicationApi } from '@/services/api/application';
import type {
  CreateApplicationRequest,
  UpdateApplicationRequest,
} from '@/types/application';

/** 应用 Query Keys */
export const applicationKeys = {
  all: ['applications'] as const,
  lists: () => [...applicationKeys.all, 'list'] as const,
  details: () => [...applicationKeys.all, 'detail'] as const,
  detail: (id: number) => [...applicationKeys.details(), id] as const,
  channels: (id: number) => [...applicationKeys.detail(id), 'channels'] as const,
};

/** 应用列表 */
export function useApplications() {
  return useQuery({
    queryKey: applicationKeys.lists(),
    queryFn: () => applicationApi.list(),
  });
}

/** 创建应用 */
export function useCreateApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateApplicationRequest) => applicationApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: applicationKeys.lists() });
    },
  });
}

/** 更新应用 */
export function useUpdateApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateApplicationRequest }) =>
      applicationApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: applicationKeys.lists() });
    },
  });
}

/** 删除应用 */
export function useDeleteApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => applicationApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: applicationKeys.lists() });
    },
  });
}

/**
 * 绑定（或解绑）应用的容灾画像
 *
 * <p>调用 PUT /applications/{id}/resilience；resilienceProfileId=null 解绑。
 * 成功后刷新应用列表使画像列展示最新绑定。</p>
 */
export function useBindResilienceProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, resilienceProfileId }: { id: number; resilienceProfileId: number | null }) =>
      applicationApi.bindResilienceProfile(id, resilienceProfileId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: applicationKeys.lists() });
    },
  });
}
