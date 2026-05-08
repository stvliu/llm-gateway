import { api } from './client';
import type {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  UserStatusUpdateRequest,
  UserRoleAssignRequest,
} from '@/types/user';
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

  /** 更新用户状态 */
  updateStatus: (id: number, data: UserStatusUpdateRequest) =>
    api.patch<User>(`/users/${id}/status`, data),

  /** 分配用户角色 */
  assignRoles: (id: number, data: UserRoleAssignRequest) =>
    api.put<User>(`/users/${id}/roles`, data),

  /** 重置密码 */
  resetPassword: (id: number) =>
    api.post<void>(`/users/${id}/reset-password`),
};
