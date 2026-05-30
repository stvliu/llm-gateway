import { api } from './client';
import type { ChannelModel, CreateChannelModelRequest, UpdateUpstreamModelNameRequest } from '@/types/channelModel';

/** 渠道模型关联 API */
export const channelModelApi = {
  /** 获取渠道下的模型关联列表 */
  list: (channelId: number) =>
    api.get<ChannelModel[]>(`/channels/${channelId}/models`),

  /** 创建渠道模型关联 */
  create: (channelId: number, data: CreateChannelModelRequest) =>
    api.post<ChannelModel>(`/channels/${channelId}/models`, data),

  /** 删除渠道模型关联 */
  delete: (channelId: number, id: number) =>
    api.delete<void>(`/channels/${channelId}/models/${id}`),

  /** 启用/禁用渠道模型关联 */
  setEnabled: (channelId: number, id: number, enabled: boolean) =>
    api.patch<void>(`/channels/${channelId}/models/${id}/state`, null, { params: { enabled } }),

  /** 更新上游模型名 */
  updateUpstreamModelName: (channelId: number, id: number, data: UpdateUpstreamModelNameRequest) =>
    api.patch<void>(`/channels/${channelId}/models/${id}/upstream-model-name`, data),
};