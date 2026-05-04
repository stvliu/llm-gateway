import type { Status } from './api';

/** 用户角色 */
export type UserRole = 'ADMIN' | 'USER';

/** 用户信息 */
export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  status: Status;
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
  status?: Status;
}

/** 登录请求 */
export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

/** 登录响应 */
export interface LoginResponse {
  user: User;
  token?: string;
}

/** 当前用户信息 */
export interface CurrentUser {
  id: number;
  username: string;
  email: string;
  role: UserRole;
}
