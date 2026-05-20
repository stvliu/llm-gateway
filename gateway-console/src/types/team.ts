import type { PageResponse } from './api';

/** 团队角色 */
export type TeamRole = 'owner' | 'admin' | 'member';

/** 团队状态 */
export type TeamState = 'active' | 'inactive' | 'deleted';

/** 团队成员 */
export interface TeamMember {
  userId: number;
  role: TeamRole;
}

/** 团队 */
export interface Team {
  id: number;
  name: string;
  description: string;
  state: TeamState;
  members: TeamMember[];
  createdAt: string;
  updatedAt: string;
}

/** 创建团队请求 */
export interface CreateTeamRequest {
  name: string;
  description?: string;
}

/** 更新团队请求 */
export interface UpdateTeamRequest {
  name?: string;
  description?: string;
  state?: TeamState;
}

/** 添加成员请求 */
export interface AddTeamMemberRequest {
  userId: number;
  role: TeamRole;
}

/** 更新成员角色请求 */
export interface UpdateMemberRoleRequest {
  role: TeamRole;
}

/** 团队分页结果 */
export type TeamPageResult = PageResponse<Team>;

/** 用户 API Key 状态 */
export type UserApiKeyState = 'ACTIVE' | 'INACTIVE' | 'DELETED';

/** 用户 API Key */
export interface UserApiKey {
  id: number;
  teamId: number;
  productId: number;
  keyPrefix: string;
  name: string;
  models: string[];
  quotaLimit: number | null;
  state: UserApiKeyState;
  createdAt: string;
  updatedAt: string;
}

/** 用户 API Key 详情（含明文） */
export interface UserApiKeyDetail {
  id: number;
  teamId: number;
  productId: number;
  keyPrefix: string;
  keyPlain: string;
  name: string;
  models: string[];
  quotaLimit: number | null;
  state: UserApiKeyState;
  createdAt: string;
  updatedAt: string;
}

/** 创建用户 API Key 请求 */
export interface CreateUserApiKeyRequest {
  teamId: number;
  productId: number;
  name: string;
  models?: string[];
  quotaLimit?: number;
}

/** 创建用户 API Key 响应 */
export interface CreateUserApiKeyResponse {
  id: number;
  keyPrefix: string;
  apiKeyPlain: string;
}

/** 更新用户 API Key 请求 */
export interface UpdateUserApiKeyRequest {
  name?: string;
  models?: string[];
  quotaLimit?: number;
  state?: UserApiKeyState;
}