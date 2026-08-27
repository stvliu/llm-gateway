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
import { api } from './client';
import type { PageResponse } from '@/types/api';
import type { AuditLogItem, AuditLogQueryParams } from '@/types/audit';

/**
 * 审计日志 API（管理操作审计查询，仅 ADMIN 可见）
 */
export const auditApi = {
  /** 分页查询审计日志 */
  list: (params?: AuditLogQueryParams) =>
    api.get<PageResponse<AuditLogItem>>('/audit-logs', { params }),
};
