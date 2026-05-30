import { api } from './client';
import type {
  Channel,
  ChannelEndpointResponse,
  ChannelCredential,
  ChannelModel,
  CreateChannelRequest,
  UpdateChannelRequest,
  CreateChannelEndpointRequest,
  CreateChannelCredentialRequest,
  CreateChannelCredentialResponse,
  CreateChannelModelRequest,
  UpdateChannelCredentialRequest,
  ApiKeyTestResponse,
} from '@/types/channel';

/** 渠道 API */
export const channelApi = {
  /** 获取渠道列表 */
  list: (params?: Record<string, unknown>) =>
    api.get<Channel[]>('/channels', { params }),

  /** 获取渠道详情 */
  get: (id: number) =>
    api.get<Channel>(`/channels/${id}`),

  /** 创建渠道 */
  create: (data: CreateChannelRequest) =>
    api.post<Channel>('/channels', data),

  /** 更新渠道 */
  update: (id: number, data: UpdateChannelRequest) =>
    api.put<Channel>(`/channels/${id}`, data),

  /** 删除渠道 */
  delete: (id: number) =>
    api.delete<void>(`/channels/${id}`),

  /** ---- 渠道端点 ---- */

  /** 添加渠道端点 */
  addEndpoint: (channelId: number, data: CreateChannelEndpointRequest) =>
    api.post<ChannelEndpointResponse>(`/channels/${channelId}/endpoints`, data),

  /** 删除渠道端点 */
  removeEndpoint: (channelId: number, endpointId: number) =>
    api.delete<void>(`/channels/${channelId}/endpoints/${endpointId}`),

  /** 启用渠道端点 */
  enableEndpoint: (channelId: number, endpointId: number) =>
    api.put<ChannelEndpointResponse>(`/channels/${channelId}/endpoints/${endpointId}/enable`),

  /** 停用渠道端点 */
  disableEndpoint: (channelId: number, endpointId: number) =>
    api.put<ChannelEndpointResponse>(`/channels/${channelId}/endpoints/${endpointId}/disable`),

  /** ---- 渠道凭证 ---- */

  /** 获取渠道凭证列表 */
  listCredentials: (channelId: number) =>
    api.get<ChannelCredential[]>(`/channels/${channelId}/credentials`),

  /** 创建渠道凭证 */
  createCredential: (channelId: number, data: CreateChannelCredentialRequest) =>
    api.post<CreateChannelCredentialResponse>(`/channels/${channelId}/credentials`, data),

  /** 更新渠道凭证 */
  updateCredential: (channelId: number, credentialId: number, data: UpdateChannelCredentialRequest) =>
    api.put<ChannelCredential>(`/channels/${channelId}/credentials/${credentialId}`, data),

  /** 删除渠道凭证 */
  deleteCredential: (channelId: number, credentialId: number) =>
    api.delete<void>(`/channels/${channelId}/credentials/${credentialId}`),

  /** 测试渠道凭证 */
  testCredential: (channelId: number, credentialId: number) =>
    api.post<ApiKeyTestResponse>(`/channels/${channelId}/credentials/${credentialId}/test`),

  /** ---- 渠道模型映射 ---- */

  /** 获取渠道的模型映射列表 */
  listModels: (channelId: number) =>
    api.get<ChannelModel[]>(`/channels/${channelId}/models`),

  /** 创建渠道模型映射 */
  createModel: (channelId: number, data: CreateChannelModelRequest) =>
    api.post<ChannelModel>(`/channels/${channelId}/models`, data),

  /** 删除渠道模型映射 */
  deleteModel: (channelId: number, modelId: number) =>
    api.delete<void>(`/channels/${channelId}/models/${modelId}`),
};