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
import { useQuery } from '@tanstack/react-query';
import { statsApi } from '@/services/api/stats';

export const statsKeys = {
  all: ['stats'] as const,
  detail: () => [...statsKeys.all, 'detail'] as const,
  trend: (days?: number) => [...statsKeys.all, 'trend', days] as const,
  modelUsage: (limit?: number) => [...statsKeys.all, 'modelUsage', limit] as const,
};

export function useStats() {
  return useQuery({
    queryKey: statsKeys.detail(),
    queryFn: () => statsApi.get(),
  });
}

/** 获取最近 N 天调用趋势 */
export function useStatsTrend(days = 7) {
  return useQuery({
    queryKey: statsKeys.trend(days),
    queryFn: () => statsApi.trend(days),
  });
}

/** 获取模型调用量分布（Top N） */
export function useStatsModelUsage(limit = 5) {
  return useQuery({
    queryKey: statsKeys.modelUsage(limit),
    queryFn: () => statsApi.modelUsage(limit),
  });
}
