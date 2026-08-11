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
import { api } from './client';
import type {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  UserStateUpdateRequest,
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
  updateState: (id: number, data: UserStateUpdateRequest) =>
    api.patch<User>(`/users/${id}/state`, data),

  /** 分配用户角色 */
  assignRoles: (id: number, data: UserRoleAssignRequest) =>
    api.put<User>(`/users/${id}/roles`, data),

  /** 重置密码（返回一次性明文） */
  resetPassword: (id: number) =>
    api.post<{ newPassword: string }>(`/users/${id}/reset-password`),
};
