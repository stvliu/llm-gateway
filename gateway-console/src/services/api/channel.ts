/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { api } from './client';
import type {
  Channel,
  ChannelResponse,
  ChannelEndpointResponse,
  ChannelCredential,
  ChannelModel,
  ChannelHealthCheckResponse,
  ChannelHealthSource,
  CreateChannelRequest,
  UpdateChannelRequest,
  CreateChannelEndpointRequest,
  CreateChannelCredentialRequest,
  CreateChannelCredentialResponse,
  CreateChannelModelRequest,
  UpdateChannelCredentialRequest,
  UpdateChannelModelRequest,
  ApiKeyTestResponse,
} from '@/types/channel';
import type { AxiosRequestConfig } from 'axios';

/** 渠道 API */
export const channelApi = {
  /** 获取渠道列表（支持按 providerId 筛选，不传则返回全部） */
  list: (params?: Record<string, unknown>) =>
    api.get<Channel[]>('/channels', { params }),

  /** 获取供应商下的渠道列表（兼容旧版） */
  listByProvider: (providerId: number) =>
    api.get<Channel[]>('/channels', { params: { providerId } }),

  /** 获取渠道详情 */
  get: (id: number) =>
    api.get<Channel>(`/channels/${id}`),

  /** 创建渠道（仅基础属性，返回含 id） */
  create: (data: CreateChannelRequest) =>
    api.post<ChannelResponse>('/channels', data),

  /** 更新渠道 */
  update: (id: number, data: UpdateChannelRequest) =>
    api.put<ChannelResponse>(`/channels/${id}`, data),

  /** 渠道状态转换（由后端校验 canTransitionTo()） */
  transitionState: (id: number, targetState: string, reason?: string) =>
    api.put<void>(`/channels/${id}/state`, { targetState, reason }),

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

  /** 更新渠道端点 */
  updateEndpoint: (channelId: number, endpointId: number, data: CreateChannelEndpointRequest) =>
    api.put<ChannelEndpointResponse>(`/channels/${channelId}/endpoints/${endpointId}`, data),

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

  /**
   * 触发渠道健康检查（任务 9.5/9.6）。
   * <p>POST /channels/{id}/health-check 请求体含 source 字段（CARD/DRAWER/PRECHECK），
   * 返回矩阵 + 聚合状态。DRAWER/CARD 持久化健康字段；PRECHECK 不持久化。</p>
   * <p>可传入 axios config（典型用法：传 signal 以便 AbortController 取消请求）。</p>
   */
  healthCheck: (
    channelId: number,
    source: ChannelHealthSource,
    config?: AxiosRequestConfig,
  ) =>
    api.post<ChannelHealthCheckResponse>(`/channels/${channelId}/health-check`, { source }, config),

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

  /** 更新模型映射的上游模型名 */
  updateUpstreamModelName: (channelId: number, modelId: number, upstreamModelName: string) =>
    api.patch<void>(`/channels/${channelId}/models/${modelId}/upstream-model-name`, { upstreamModelName }),

  /** 模型实例状态转换（由后端校验 canTransitionTo()） */
  transitionModelState: (channelId: number, modelId: number, targetState: string) =>
    api.put<void>(`/channels/${channelId}/models/${modelId}/state`, { targetState }),

  /** 更新模型映射（支持修改 modelId 和 upstreamModelName） */
  updateModel: (channelId: number, modelId: number, data: UpdateChannelModelRequest) =>
    api.put<ChannelModel>(`/channels/${channelId}/models/${modelId}`, data),
};