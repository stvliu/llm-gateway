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
import type { Permission } from '@/constants/permissions';

/** 用户角色（兼容未来新角色） */
export type UserRole = 'ADMIN' | 'USER' | string;

/** 用户状态枚举 */
export type UserState = 'ACTIVE' | 'INACTIVE' | 'LOCKED';

/** 用户信息（完整，含审计字段） */
export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  state: UserState;
  createdAt: string;
  updatedAt: string;
}

/** 创建用户请求 */
export interface CreateUserRequest {
  username: string;
  password: string;
  email: string;
  role: UserRole;
}

/** 更新用户请求 */
export interface UpdateUserRequest {
  email?: string;
  role?: UserRole;
  state?: UserState;
}

/** 登录请求 */
export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

/** 登录响应数据 */
export interface LoginResponseData {
  user: LoginUserResponse;
  token?: string;
}

/** 登录响应（包装在 ApiResponse 中） */
export type LoginResponse = LoginResponseData;

/** 登录返回的用户信息（后端原始结构，不含 permissions） */
export interface LoginUserResponse {
  id: number;
  username: string;
  email: string;
  role: UserRole;
}

/** 当前用户信息（前端使用，含推导的 permissions） */
export interface CurrentUser {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  /** 前端根据 role 推导，非后端返回 */
  permissions: Permission[];
}

/** 用户状态更新请求 */
export interface UserStateUpdateRequest {
  state: UserState;
}

/** 用户角色分配请求 */
export interface UserRoleAssignRequest {
  roleCodes: UserRole[];
}