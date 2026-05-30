import { api } from './client';
import type {
  Team,
  CreateTeamRequest,
  UpdateTeamRequest,
  AddTeamMemberRequest,
  UpdateMemberRoleRequest,
  UserApiKey,
  UserApiKeyDetail,
  CreateUserApiKeyRequest,
  CreateUserApiKeyResponse,
  UpdateUserApiKeyRequest,
} from '@/types/team';

export const teamApi = {
  /** 获取团队列表 */
  list: () =>
    api.get<Team[]>('/teams'),

  /** 创建团队 */
  create: (data: CreateTeamRequest) =>
    api.post<Team>('/teams', data),

  /** 更新团队 */
  update: (id: number, data: UpdateTeamRequest) =>
    api.put<Team>(`/teams/${id}`, data),

  /** 删除团队 */
  delete: (id: number) =>
    api.delete<void>(`/teams/${id}`),

  /** 添加成员 */
  addMember: (teamId: number, data: AddTeamMemberRequest) =>
    api.post<void>(`/teams/${teamId}/members`, data),

  /** 移除成员 */
  removeMember: (teamId: number, userId: number) =>
    api.delete<void>(`/teams/${teamId}/members/${userId}`),

  /** 修改成员角色 */
  updateMemberRole: (teamId: number, userId: number, data: UpdateMemberRoleRequest) =>
    api.put<void>(`/teams/${teamId}/members/${userId}/role`, data),

  // ---- UserApiKey（团队子资源） ----

  /** 获取团队下的 API Key 列表 */
  listApiKeys: (teamId: number) =>
    api.get<UserApiKey[]>(`/teams/${teamId}/api-keys`),

  /** 获取单个 API Key 详情 */
  getApiKeyDetail: (teamId: number, id: number) =>
    api.get<UserApiKeyDetail>(`/teams/${teamId}/api-keys/${id}`),

  /** 创建用户 API Key */
  createApiKey: (teamId: number, data: CreateUserApiKeyRequest) =>
    api.post<CreateUserApiKeyResponse>(`/teams/${teamId}/api-keys`, data),

  /** 更新用户 API Key */
  updateApiKey: (teamId: number, id: number, data: UpdateUserApiKeyRequest) =>
    api.put<UserApiKey>(`/teams/${teamId}/api-keys/${id}`, data),

  /** 删除用户 API Key */
  deleteApiKey: (teamId: number, id: number) =>
    api.delete<void>(`/teams/${teamId}/api-keys/${id}`),

  // ---- UserApiKey（用户维度） ----

  /** 获取当前用户的 API Key 列表 */
  listMyApiKeys: () =>
    api.get<UserApiKey[]>('/me/api-keys'),

  /** 获取指定用户的 API Key 列表 */
  listUserApiKeys: (userId: number) =>
    api.get<UserApiKey[]>(`/users/${userId}/api-keys`),

  // ---- 团队渠道管理 ----

  /** 查询团队的渠道列表 */
  listChannels: (teamId: number) =>
    api.get<number[]>(`/teams/${teamId}/channels`),

  /** 更新团队的渠道列表 */
  updateChannels: (teamId: number, channelIds: number[]) =>
    api.put<void>(`/teams/${teamId}/channels`, { channelIds }),

  // ---- 团队模型可见性 ----

  /** 查询团队可见的模型 ID 列表 */
  listAllowedModels: (teamId: number) =>
    api.get<number[]>(`/teams/${teamId}/allowed-models`),

  /** 更新团队可见的模型列表 */
  updateAllowedModels: (teamId: number, modelIds: number[]) =>
    api.put<void>(`/teams/${teamId}/allowed-models`, { modelIds }),
};
