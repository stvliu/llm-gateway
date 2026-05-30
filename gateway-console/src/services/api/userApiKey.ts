import { api } from './client';
import type {
  UserApiKey,
  UserApiKeyDetail,
  CreateUserApiKeyRequest,
  CreateUserApiKeyResponse,
  UpdateUserApiKeyRequest,
} from '@/types/team';

/**
 * 用户 API Key 管理接口（用户维度）
 */
export const userApiKeyApi = {
  /** 获取指定用户的 API Key 列表 */
  listByUser: (userId: number) =>
    api.get<UserApiKey[]>(`/users/${userId}/api-keys`),

  /** 获取 API Key 详情 */
  getDetail: (id: number) =>
    api.get<UserApiKeyDetail>(`/user-api-keys/${id}/detail`),

  /** 创建用户 API Key */
  create: (data: CreateUserApiKeyRequest) =>
    api.post<CreateUserApiKeyResponse>('/user-api-keys', data),

  /** 更新用户 API Key */
  update: (id: number, data: UpdateUserApiKeyRequest) =>
    api.put<UserApiKey>(`/user-api-keys/${id}`, data),

  /** 轮换 API Key（生成新 Key，旧 Key 失效） */
  rotate: (id: number) =>
    api.post<CreateUserApiKeyResponse>(`/user-api-keys/${id}/rotate`),

  /** 删除用户 API Key */
  delete: (id: number) =>
    api.delete<void>(`/user-api-keys/${id}`),
};