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
import { catalogSyncApi } from '@/services/api/catalogSync';
import { modelKeys } from '@/services/query/useModels';

/** 模型目录同步查询键 */
const CATALOG_SYNC_KEY = 'catalog-sync';

/** 最近一次同步状态查询 */
export function useCatalogSyncStatus() {
  return useQuery({
    queryKey: [CATALOG_SYNC_KEY, 'status'],
    queryFn: async () => {
      const data = await catalogSyncApi.status();
      // 后端 204 无记录时 axios 返回空字符串，统一归一化为 null，表示"尚未同步"
      return data || null;
    },
  });
}

/** 手工触发模型目录同步（成功后刷新同步状态、模型列表与套餐/供应商目录） */
export function useCatalogSync() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => catalogSyncApi.sync(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [CATALOG_SYNC_KEY] });
      queryClient.invalidateQueries({ queryKey: modelKeys.all });
      queryClient.invalidateQueries({ queryKey: ['plan-catalog'] });
      queryClient.invalidateQueries({ queryKey: ['provider-catalog'] });
    },
  });
}
