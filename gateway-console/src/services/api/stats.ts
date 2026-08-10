/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { api } from './client';

export interface StatsResponse {
  providerCount: number;
  channelCount: number;
  modelCount: number;
  userCount: number;
  todayRequests: number;
  tokenUsage: string;
}

export const statsApi = {
  /** 获取系统统计数据 */
  get: () => api.get<StatsResponse>('/stats'),
};
