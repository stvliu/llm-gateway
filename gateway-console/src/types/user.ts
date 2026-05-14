import type { Permission } from '@/constants/permissions';

/** 用户角色（兼容未来新角色） */
export type UserRole = 'ADMIN' | 'USER' | string;

/** 用户状态枚举 */
export type UserState = 'ACTIVE' | 'DISABLED' | 'LOCKED';

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