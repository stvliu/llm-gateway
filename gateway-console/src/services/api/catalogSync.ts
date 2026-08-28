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
import type { CatalogSyncReport, CatalogSyncStatusResponse } from '@/types/catalog';

/**
 * 模型目录（models.dev）同步 API
 *
 * <p>说明：POST /catalog/sync 由后端统一包装为 ApiResponse，响应拦截器已自动解包，
 * 此处直接返回 CatalogSyncReport；GET /catalog/sync/status 返回 ResponseEntity，
 * 不经过 ApiResponse 包装，无记录时返回 204（响应体为空字符串）。</p>
 */
export const catalogSyncApi = {
  /** 手工触发模型目录同步 */
  sync: () => api.post<CatalogSyncReport>('/catalog/sync'),

  /** 查询最近一次同步状态（无记录时返回 204，data 为空字符串） */
  status: () => api.get<CatalogSyncStatusResponse>('/catalog/sync/status'),
};
