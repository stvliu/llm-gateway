import { api } from './client';
import type { User, CreateUserRequest, UpdateUserRequest } from '@/types/user';
import type { PageResponse, PageParams } from '@/types/api';

export const userApi = {
  /** 获取用户列表 */
  list: (params?: PageParams) =>
    api.get<PageResponse<User>>('/users', { params }),

  /** 获取用户详情 */
  get: (id: number) =>
    api.get<User>(`/users/${id}`),

  /** 创建用户 */
  create: (data: CreateUserRequest) =>
    api.post<User>('/users', data),

  /** 更新用户 */
  update: (id: number, data: UpdateUserRequest) =>
    api.put<User>(`/users/${id}`, data),

  /** 删除用户 */
  delete: (id: number) =>
    api.delete<void>(`/users/${id}`),

  /** 重置密码 */
  resetPassword: (id: number) =>
    api.post<void>(`/users/${id}/reset-password`),
};
