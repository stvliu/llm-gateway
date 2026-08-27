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
import type {
  TokenLimit,
  TokenLimitCreateRequest,
  TokenLimitUpdateRequest,
  TokenLimitQueryParams,
} from '@/types/tokenLimit';

/**
 * Token 限额 API（管理员配置）
 */
export const tokenLimitApi = {
  /** 分页查询 Token 限额 */
  list: (params?: TokenLimitQueryParams) =>
    api.get<PageResponse<TokenLimit>>('/token-limits', { params }),

  /** 获取 Token 限额详情 */
  get: (id: number) => api.get<TokenLimit>(`/token-limits/${id}`),

  /** 创建 Token 限额 */
  create: (data: TokenLimitCreateRequest) => api.post<TokenLimit>('/token-limits', data),

  /** 更新 Token 限额 */
  update: (id: number, data: TokenLimitUpdateRequest) =>
    api.put<TokenLimit>(`/token-limits/${id}`, data),

  /** 删除 Token 限额 */
  remove: (id: number) => api.delete<void>(`/token-limits/${id}`),

  /** 重置已使用量 */
  resetUsage: (id: number) => api.patch<TokenLimit>(`/token-limits/${id}/reset-usage`),
};
