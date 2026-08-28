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
import type { SystemSetting, AuditCleanupResult } from '@/types/settings';

/**
 * 系统设置 API
 *
 * <p>GET /settings 与 PUT /settings/{key} 由后端统一包装为 ApiResponse，
 * 响应拦截器已自动解包为 SystemSetting 列表 / 单个 SystemSetting；
 * DELETE /audit-logs 同样被包装为 {data: {deleted: N}}，解包后直接返回删除条数对象。</p>
 */
export const settingsApi = {
  /** 查询全部系统配置 */
  getSettings: () => api.get<SystemSetting[]>('/settings'),

  /** 更新指定配置项（非法值后端返回 400，error.message 可展示） */
  updateSetting: (key: string, value: string) =>
    api.put<SystemSetting>(`/settings/${key}`, { value }),

  /** 手动清理审计日志（保留 N 天前，返回删除条数） */
  cleanupAuditLogs: (days: number) =>
    api.delete<AuditCleanupResult>('/audit-logs', { params: { days } }),
};
