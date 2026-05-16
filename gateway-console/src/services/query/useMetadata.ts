import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { providerMetadataApi, modelMetadataApi, metadataSyncApi } from '@/services/api/metadata';
import type {
  ProviderMetadataListParams,
  ModelMetadataListParams,
  CreateProviderMetadataRequest,
  UpdateProviderMetadataRequest,
  ApplyMetadataRequest,
} from '@/types/metadata';

const PROVIDER_KEY = 'provider-metadata';
const MODEL_KEY = 'model-metadata';

/** 供应商元数据查询 */
export function useProviderMetadata(params: ProviderMetadataListParams) {
  return useQuery({
    queryKey: [PROVIDER_KEY, 'list', params],
    queryFn: () => providerMetadataApi.list(params),
  });
}

/** 供应商元数据列表查询 */
export function useProviderMetadataList() {
  return useQuery({
    queryKey: [PROVIDER_KEY, 'list'],
    queryFn: () => providerMetadataApi.listAll(),
  });
}

/** 供应商元数据详情 */
export function useProviderMetadataDetail(id: number | null) {
  return useQuery({
    queryKey: [PROVIDER_KEY, 'detail', id],
    queryFn: () => providerMetadataApi.get(id!),
    enabled: id !== null,
  });
}

/** 创建供应商元数据 */
export function useCreateProviderMetadata() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateProviderMetadataRequest) =>
      providerMetadataApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PROVIDER_KEY] });
    },
  });
}

/** 更新供应商元数据 */
export function useUpdateProviderMetadata() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateProviderMetadataRequest }) =>
      providerMetadataApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PROVIDER_KEY] });
    },
  });
}

/** 删除供应商元数据 */
export function useDeleteProviderMetadata() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => providerMetadataApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PROVIDER_KEY] });
    },
  });
}

/** 应用元数据 */
export function useApplyMetadata() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: ApplyMetadataRequest }) =>
      providerMetadataApi.apply(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PROVIDER_KEY] });
    },
  });
}

/** 模型元数据查询 */
export function useModelMetadata(params: ModelMetadataListParams) {
  return useQuery({
    queryKey: [MODEL_KEY, 'list', params],
    queryFn: () => modelMetadataApi.list(params),
  });
}

/** 某供应商的模型元数据 */
export function useModelMetadataByProvider(providerId: string | null) {
  return useQuery({
    queryKey: [MODEL_KEY, 'provider', providerId],
    queryFn: () => modelMetadataApi.listByProviderId(providerId!),
    enabled: providerId !== null,
  });
}

/** 同步元数据 */
export function useSyncMetadata() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (type: 'all' | 'builtin' | 'models-dev') => {
      switch (type) {
        case 'all': return metadataSyncApi.syncAll();
        case 'builtin': return metadataSyncApi.syncBuiltin();
        case 'models-dev': return metadataSyncApi.syncModelsDev();
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PROVIDER_KEY] });
      queryClient.invalidateQueries({ queryKey: [MODEL_KEY] });
    },
  });
}