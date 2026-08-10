/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
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
