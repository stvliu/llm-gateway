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
import {
  planCatalogApi,
  provisionApi,
} from '@/services/api/catalog';
import type {
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