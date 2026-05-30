import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { teamApi } from '@/services/api/team';
import type {
  CreateTeamRequest,
  UpdateTeamRequest,
  AddTeamMemberRequest,
  UpdateMemberRoleRequest,
  CreateUserApiKeyRequest,
  UpdateUserApiKeyRequest,
} from '@/types/team';

/** 团队 Query Keys */
export const teamKeys = {
  all: ['teams'] as const,
  lists: () => [...teamKeys.all, 'list'] as const,
  list: (filters?: Record<string, unknown>) => [...teamKeys.lists(), filters] as const,
  details: () => [...teamKeys.all, 'detail'] as const,
  detail: (id: number) => [...teamKeys.details(), id] as const,
  apiKeys: (teamId: number) => [...teamKeys.detail(teamId), 'api-keys'] as const,
  myApiKeys: ['me', 'api-keys'] as const,
  userApiKeys: (userId: number) => ['users', userId, 'api-keys'] as const,
};

/** 团队列表 */
export function useTeams() {
  return useQuery({
    queryKey: teamKeys.lists(),
    queryFn: () => teamApi.list(),
  });
}

/** 创建团队 */
export function useCreateTeam() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTeamRequest) => teamApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: teamKeys.lists() });
    },
  });
}

/** 更新团队 */
export function useUpdateTeam() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateTeamRequest }) =>
      teamApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: teamKeys.lists() });
    },
  });
}

/** 删除团队 */
export function useDeleteTeam() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => teamApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: teamKeys.lists() });
    },
  });
}

/** 添加成员 */
export function useAddTeamMember() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, data }: { teamId: number; data: AddTeamMemberRequest }) =>
      teamApi.addMember(teamId, data),
    onSuccess: (_data, { teamId }) => {
      qc.invalidateQueries({ queryKey: teamKeys.detail(teamId) });
      qc.invalidateQueries({ queryKey: teamKeys.lists() });
    },
  });
}

/** 移除成员 */
export function useRemoveTeamMember() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, userId }: { teamId: number; userId: number }) =>
      teamApi.removeMember(teamId, userId),
    onSuccess: (_data, { teamId }) => {
      qc.invalidateQueries({ queryKey: teamKeys.detail(teamId) });
      qc.invalidateQueries({ queryKey: teamKeys.lists() });
    },
  });
}

/** 修改成员角色 */
export function useUpdateMemberRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, userId, data }: { teamId: number; userId: number; data: UpdateMemberRoleRequest }) =>
      teamApi.updateMemberRole(teamId, userId, data),
    onSuccess: (_data, { teamId }) => {
      qc.invalidateQueries({ queryKey: teamKeys.detail(teamId) });
      qc.invalidateQueries({ queryKey: teamKeys.lists() });
    },
  });
}

// ---- UserApiKey Hooks ----

/** 团队下的 API Key 列表 */
export function useTeamApiKeys(teamId: number) {
  return useQuery({
    queryKey: teamKeys.apiKeys(teamId),
    queryFn: () => teamApi.listApiKeys(teamId),
    enabled: teamId > 0,
  });
}

/** 创建团队维度的用户 API Key */
export function useCreateTeamUserApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, data }: { teamId: number; data: CreateUserApiKeyRequest }) =>
      teamApi.createApiKey(teamId, data),
    onSuccess: (_data, { teamId }) => {
      qc.invalidateQueries({ queryKey: teamKeys.apiKeys(teamId) });
    },
  });
}

/** 更新团队维度的用户 API Key */
export function useUpdateTeamUserApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, id, data }: { teamId: number; id: number; data: UpdateUserApiKeyRequest }) =>
      teamApi.updateApiKey(teamId, id, data),
    onSuccess: (_data, { teamId }) => {
      qc.invalidateQueries({ queryKey: teamKeys.apiKeys(teamId) });
    },
  });
}

/** 删除团队维度的用户 API Key */
export function useDeleteTeamUserApiKey() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, id }: { teamId: number; id: number }) =>
      teamApi.deleteApiKey(teamId, id),
    onSuccess: (_data, { teamId }) => {
      qc.invalidateQueries({ queryKey: teamKeys.apiKeys(teamId) });
    },
  });
}
