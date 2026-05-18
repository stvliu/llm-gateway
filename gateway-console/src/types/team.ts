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