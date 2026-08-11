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
import type { Status } from './api';

/** Token 限额类型 */
export type TokenLimitScope = 'USER' | 'API_KEY';

/** Token 限额信息 */
export interface TokenLimit {
  id: number;
  scope: TokenLimitScope;
  targetId: number;
  targetName: string;
  inputLimit: number;
  outputLimit: number;
  inputUsed: number;
  outputUsed: number;
  periodStart: string;
  periodEnd: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建 Token 限额请求 */
export interface CreateTokenLimitRequest {
  scope: TokenLimitScope;
  targetId: number;
  inputLimit: number;
  outputLimit: number;
}

/** 更新 Token 限额请求 */
export interface UpdateTokenLimitRequest {
  inputLimit?: number;
  outputLimit?: number;
  status?: Status;
}
