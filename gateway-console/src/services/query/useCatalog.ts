import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  providerCatalogApi,
  planCatalogApi,
  modelCatalogApi,
  catalogMaterializeApi,
  catalogSyncApi,
} from '@/services/api/catalog';
import type {
  ProviderCatalogListParams,
  ModelCatalogListParams,
  MaterializePlanRequest,
} from '@/types/catalog';

const PROVIDER_CATALOG_KEY = 'provider-catalog';
const PLAN_CATALOG_KEY = 'plan-catalog';
const MODEL_CATALOG_KEY = 'model-catalog';

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

// ===== 模型目录 =====

/** 模型目录列表查询 */
export function useModelCatalogs(params?: ModelCatalogListParams) {
  return useQuery({
    queryKey: [MODEL_CATALOG_KEY, 'list', params],
    queryFn: () => modelCatalogApi.list(params),
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

/** 级联物化供应商 */
export function useMaterializeProviderWithPlans() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ providerCode, data }: { providerCode: string; data?: { planCodes?: string[] } }) =>
      catalogMaterializeApi.materializeProviderWithPlans(providerCode, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PROVIDER_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: [PLAN_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: ['providers'] });
    },
  });
}

/** 物化套餐（支持传入端点、模型、API Key 等配置） */
export function useMaterializePlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planCode, data }: { planCode: string; data?: MaterializePlanRequest }) =>
      catalogMaterializeApi.materializePlan(planCode, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PLAN_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: ['providers'] });
      queryClient.invalidateQueries({ queryKey: ['channels'] });
    },
  });
}

/** 物化模型 */
export function useMaterializeModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (modelName: string) =>
      catalogMaterializeApi.materializeModel(modelName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [MODEL_CATALOG_KEY] });
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
      queryClient.invalidateQueries({ queryKey: [MODEL_CATALOG_KEY] });
    },
  });
}