import { api } from './client';
import type {
  UserApiKey,
  UserApiKeyDetail,
  CreateUserApiKeyRequest,
  CreateUserApiKeyResponse,
  UpdateUserApiKeyRequest,
} from '@/types/userApiKey';

/**
 * 用户 API Key 管理接口（用户维度）
 */
export const userApiKeyApi = {
  /** 获取指定用户的 API Key 列表 */
  listByUser: (userId: number) =>
    api.get<UserApiKey[]>(`/users/${userId}/api-keys`),

  /** 查询所有 API Key（管理员用） */
  listAll: () =>
    api.get<UserApiKey[]>('/user-api-keys'),

  /** 按应用查询 API Key（应用详情页/筛选用） */
  listByApplication: (applicationId: number) =>
    api.get<UserApiKey[]>(`/applications/${applicationId}/api-keys`),

  /** 获取 API Key 详情 */
  getDetail: (id: number) =>
    api.get<UserApiKeyDetail>(`/user-api-keys/${id}/detail`),

  /** 创建用户 API Key */
  create: (data: CreateUserApiKeyRequest) =>
    api.post<CreateUserApiKeyResponse>('/user-api-keys', data),

  /** 更新用户 API Key */
  update: (id: number, data: UpdateUserApiKeyRequest) =>
    api.put<UserApiKey>(`/user-api-keys/${id}`, data),

  /** 删除用户 API Key */
  delete: (id: number) =>
    api.delete<void>(`/user-api-keys/${id}`),
};