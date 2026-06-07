import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  planCatalogApi,
  provisionApi,
} from '@/services/api/catalog';
import type {
  ProvisionRequest,
  BatchProvisionRequest,
} from '@/types/catalog';

const PLAN_CATALOG_KEY = 'plan-catalog';
const PROVIDER_CATALOG_KEY = 'provider-catalog';
const MODEL_KEY = 'model';

// ===== 供应商目录 =====

/** 供应商目录列表查询 */
export function useProviderCatalogs(keyword?: string) {
  return useQuery({
    queryKey: [PROVIDER_CATALOG_KEY, 'list', keyword],
    queryFn: () => planCatalogApi.listProviders({ keyword }),
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

// ===== 模型 =====

/** 模型列表查询 */
export function useModels(params?: { keyword?: string; capability?: string }) {
  return useQuery({
    queryKey: [MODEL_KEY, 'list', params],
    queryFn: () => planCatalogApi.listModels(params),
  });
}

// ===== 开通操作 =====

/** 从套餐创建渠道 */
export function useProvisionFromPlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planCode, data }: { planCode: string; data?: ProvisionRequest }) =>
      provisionApi.fromPlan(planCode, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PLAN_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: [PROVIDER_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: ['channels'] });
    },
  });
}

/** 批量开通供应商 */
export function useProvisionBatch() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ providerCode, data }: { providerCode: string; data?: BatchProvisionRequest }) =>
      provisionApi.batch(providerCode, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PLAN_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: [PROVIDER_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: ['channels'] });
    },
  });
}

/** 开通模型 */
export function useProvisionModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (modelName: string) => provisionApi.model(modelName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [MODEL_KEY] });
      queryClient.invalidateQueries({ queryKey: ['models'] });
    },
  });
}

/** 同步目录数据 */
export function useSyncCatalog() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => provisionApi.syncBuiltin(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [PLAN_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: [PROVIDER_CATALOG_KEY] });
      queryClient.invalidateQueries({ queryKey: [MODEL_KEY] });
    },
  });
}