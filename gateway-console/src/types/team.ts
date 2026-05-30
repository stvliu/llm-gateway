/** 团队角色（小写字符串，与后端 TeamRole.getCode() 一致） */
export type TeamRole = 'owner' | 'admin' | 'member';

/** 团队信息（与后端 TeamResponse 一致） */
export interface Team {
  id: number;
  name: string;
  description: string;
  state: string;
  members: TeamMember[];
  createdAt: string;
  updatedAt: string;
}

/** 团队成员信息（与后端 TeamResponse.MemberResponse 一致） */
export interface TeamMember {
  userId: number;
  role: string;
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
}

/** 添加团队成员请求 */
export interface AddTeamMemberRequest {
  userId: number;
  role: TeamRole;
}

/** 更新成员角色请求 */
export interface UpdateMemberRoleRequest {
  role: TeamRole;
}

/** 用户 API Key */
export interface UserApiKey {
  id: number;
  teamId: number;
  userId: number;
  keyPrefix: string;
  name: string;
  models: string[];
  quotaLimit: number | null;
  state: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}

/** 用户 API Key 详情（含明文 Key） */
export interface UserApiKeyDetail extends UserApiKey {
  keyPlain: string;
  channels: ChannelBrief[];
}

/** 渠道简要信息 */
export interface ChannelBrief {
  id: number;
  name: string;
}

/** 创建用户 API Key 请求 */
export interface CreateUserApiKeyRequest {
  teamId?: number;
  userId: number;
  productIds?: number[];
  name: string;
  models?: string[];
  quotaLimit?: number | null;
}

/** 更新用户 API Key 请求 */
export interface UpdateUserApiKeyRequest {
  name?: string;
  models?: string[];
  quotaLimit?: number | null;
  state?: 'ACTIVE' | 'INACTIVE';
}

/** 创建用户 API Key 响应 */
export interface CreateUserApiKeyResponse {
  id: number;
  keyPrefix: string;
  keyPlain: string;
}