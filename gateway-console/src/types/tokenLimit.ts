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

/** Token 限额类型 */
export type TokenLimitType = 'SYSTEM_DEFAULT' | 'USER_CUSTOM';

/** 限额周期 */
export type TokenPeriodType = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'TOTAL';

/** 超限动作 */
export type TokenExceededAction = 'REJECT' | 'DOWNGRADE';

/** Token 限额条目（与后端 TokenLimitResponse 一致） */
export interface TokenLimit {
  id: number;
  userId: number;
  username?: string;
  providerId?: number;
  providerName?: string;
  modelId?: number;
  modelName?: string;
  limitType: TokenLimitType;
  maxTokens: number;
  usedTokens: number;
  remainingTokens: number;
  periodType: TokenPeriodType;
  periodDayOfWeek?: number;
  periodDayOfMonth?: number;
  exceededAction: TokenExceededAction;
  switchModelId?: number;
  switchModelName?: string;
  state: 'ACTIVE' | 'SUSPENDED';
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

/** 创建 Token 限额请求 */
export interface TokenLimitCreateRequest {
  userId: number;
  providerId?: number;
  modelId?: number;
  maxTokens: number;
  periodType?: TokenPeriodType;
  exceededAction?: TokenExceededAction;
  switchModelId?: number;
}

/** 更新 Token 限额请求 */
export interface TokenLimitUpdateRequest {
  maxTokens?: number;
  periodType?: TokenPeriodType;
  exceededAction?: TokenExceededAction;
  switchModelId?: number;
  enabled?: boolean;
}

/** Token 限额查询参数 */
export interface TokenLimitQueryParams {
  page?: number;
  limit?: number;
  keyword?: string;
  userId?: number;
  providerId?: number;
  modelId?: number;
  state?: string;
}
