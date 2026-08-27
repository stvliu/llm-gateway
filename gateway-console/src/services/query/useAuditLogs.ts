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
import { useQuery } from '@tanstack/react-query';
import { auditApi } from '@/services/api/audit';
import type { AuditLogQueryParams } from '@/types/audit';

export const auditKeys = {
  all: ['auditLogs'] as const,
  list: (params?: AuditLogQueryParams) => [...auditKeys.all, 'list', params] as const,
};

/** 分页查询审计日志 */
export function useAuditLogs(params?: AuditLogQueryParams) {
  return useQuery({
    queryKey: auditKeys.list(params),
    queryFn: () => auditApi.list(params),
  });
}
