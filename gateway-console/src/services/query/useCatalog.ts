import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  providerCatalogApi,
  planCatalogApi,
  modelSpecCatalogApi,
  catalogMaterializeApi,
  catalogSyncApi,
} from '@/services/api/catalog';
import type {
  ProviderCatalogListParams,
  ModelSpecCatalogListParams,
} from '@/types/catalog';

const PROVIDER_CATALOG_KEY = 'provider-catalog';
const PLAN_CATALOG_KEY = 'plan-catalog';
const MODEL_SPEC_CATALOG_KEY = 'model-spec-catalog';

// ===== 供应商目录 =====

/** 供应商目录列表查询 */
export function useProviderCatalogs(params?: ProviderCatalogListParams) {
  return useQuery({
    queryKey: [PROVIDER_CATALOG_KEY, 'list', params],
    queryFn: () => providerCatalogApi.list(params),
  });
}

// ===== 套餐目录 =====

/** 套餐目录列表查询 */
export function usePlanCatalogs(providerCode?: string) {
  return useQuery({
    queryKey: [PLAN_CATALOG_KEY, 'list', providerCode],
    queryFn: () => planCatalogApi.list({ providerCode }),
  });
}

/** 套餐详情查询 */
export function usePlanDetail(planCode: string | null) {
  return useQuery({
    queryKey: [PLAN_CATALOG_KEY, 'detail', planCode],
    queryFn: () => planCatalogApi.getDetail(planCode!),
    enabled: planCode !== null,
  });
}

// ===== 模型规格目录 =====

/** 模型规格目录列表查询 */
export function useModelSpecCatalogs(params?: ModelSpecCatalogListParams) {
  return useQuery({
    queryKey: [MODEL_SPEC_CATALOG_KEY, 'list', params],
    queryFn: () => modelSpecCatalogApi.list(params),
  });
}

// ===== 物化操作 =====

/** 物化供应商 */
export function useMaterializeProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (providerCode: string) =>
      catalogMaterializeApi.materializeProvider(providerCode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PROVIDER_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: ['providers'] });
    },
  });
}

/** 物化套餐 */
export function useMaterializePlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (planCode: string) =>
      catalogMaterializeApi.materializePlan(planCode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PLAN_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: ['providers'] });
    },
  });
}

/** 物化模型规格 */
export function useMaterializeModelSpec() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (providerModelId: string) =>
      catalogMaterializeApi.materializeModelSpec(providerModelId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [MODEL_SPEC_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: ['models'] });
    },
  });
}

// ===== 同步操作 =====

/** 同步目录数据 */
export function useSyncCatalog() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (type: 'builtin' | 'models-dev') => {
      switch (type) {
        case 'builtin': return catalogSyncApi.syncBuiltin();
        case 'models-dev': return catalogSyncApi.syncModelsDev();
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PROVIDER_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: [PLAN_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: [MODEL_SPEC_CATALOG_KEY] });
    },
  });
}
