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
import type {
  TokenLimit,
  CreateTokenLimitRequest,
  UpdateTokenLimitRequest,
} from '@/types/tokenLimit';
import type { PageResponse, PageParams } from '@/types/api';

export const tokenLimitApi = {
  /** 获取 Token 限额列表 */
  list: (params?: PageParams & { scope?: 'USER' | 'API_KEY'; targetId?: number }) =>
    api.get<PageResponse<TokenLimit>>('/token-limits', { params }),

  /** 获取 Token 限额详情 */
  get: (id: number) =>
    api.get<TokenLimit>(`/token-limits/${id}`),

  /** 创建 Token 限额 */
  create: (data: CreateTokenLimitRequest) =>
    api.post<TokenLimit>('/token-limits', data),

  /** 更新 Token 限额 */
  update: (id: number, data: UpdateTokenLimitRequest) =>
    api.put<TokenLimit>(`/token-limits/${id}`, data),

  /** 删除 Token 限额 */
  delete: (id: number) =>
    api.delete<void>(`/token-limits/${id}`),

  /** 重置已使用量 */
  resetUsage: (id: number) =>
    api.patch<TokenLimit>(`/token-limits/${id}/reset-usage`),
};