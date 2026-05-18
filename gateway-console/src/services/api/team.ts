import { api } from './client';
import type {
  Team,
  CreateTeamRequest,
  UpdateTeamRequest,
  AddTeamMemberRequest,
  UpdateMemberRoleRequest,
} from '@/types/team';

export const teamApi = {
  /** 获取团队列表 */
  list: () =>
    api.get<Team[]>('/teams'),

  /** 创建团队 */
  create: (data: CreateTeamRequest) =>
    api.post<Team>('/teams', data),

  /** 更新团队 */
  update: (id: number, data: UpdateTeamRequest) =>
    api.put<Team>(`/teams/${id}`, data),

  /** 删除团队 */
  delete: (id: number) =>
    api.delete<void>(`/teams/${id}`),

  /** 添加成员 */
  addMember: (teamId: number, data: AddTeamMemberRequest) =>
    api.post<void>(`/teams/${teamId}/members`, data),

  /** 移除成员 */
  removeMember: (teamId: number, userId: number) =>
    api.delete<void>(`/teams/${teamId}/members/${userId}`),

  /** 修改成员角色 */
  updateMemberRole: (teamId: number, userId: number, data: UpdateMemberRoleRequest) =>
    api.put<void>(`/teams/${teamId}/members/${userId}/role`, data),
};