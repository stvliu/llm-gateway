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

export interface StatsResponse {
  providerCount: number;
  channelCount: number;
  modelCount: number;
  userCount: number;
  todayRequests: number;
  tokenUsage: string;
}

/** 按天调用趋势（与后端 StatsTrendResponse 一致） */
export interface StatsTrendItem {
  date: string;
  requestCount: number;
  tokenCount: number;
}

/** 模型用量分布（与后端 StatsModelUsageResponse 一致） */
export interface StatsModelUsageItem {
  model: string;
  requestCount: number;
}

export const statsApi = {
  /** 获取系统统计数据 */
  get: () => api.get<StatsResponse>('/stats'),

  /** 获取最近 N 天调用趋势 */
  trend: (days = 7) => api.get<StatsTrendItem[]>('/stats/trend', { params: { days } }),

  /** 获取模型调用量分布（Top N） */
  modelUsage: (limit = 5) =>
    api.get<StatsModelUsageItem[]>('/stats/model-usage', { params: { limit } }),
};
