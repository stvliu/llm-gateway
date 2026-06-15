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
