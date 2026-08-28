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
import { settingsApi } from '@/services/api/settings';
import { auditKeys } from '@/services/query/useAuditLogs';

/** 系统设置查询键 */
export const settingsKeys = {
  all: ['settings'] as const,
};

/** 模型目录同步状态查询键（与 useCatalogSync 保持一致） */
const CATALOG_SYNC_KEY = 'catalog-sync';

/** 查询全部系统配置 */
export function useSettings() {
  return useQuery({
    queryKey: settingsKeys.all,
    queryFn: () => settingsApi.getSettings(),
  });
}

/** 更新系统配置（成功后刷新配置列表与模型目录同步状态） */
export function useUpdateSetting() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) =>
      settingsApi.updateSetting(key, value),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: settingsKeys.all });
      queryClient.invalidateQueries({ queryKey: [CATALOG_SYNC_KEY] });
    },
  });
}

/** 手动清理审计日志（成功后刷新审计日志列表） */
export function useCleanupAuditLogs() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (days: number) => settingsApi.cleanupAuditLogs(days),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: auditKeys.all });
    },
  });
}
