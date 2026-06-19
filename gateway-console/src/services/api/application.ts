import { api } from './client';
import type {
  Application,
  CreateApplicationRequest,
  UpdateApplicationRequest,
} from '@/types/application';

/**
 * 应用管理接口
 *
 * Application 是权限+行为双聚合根，提供 CRUD 与渠道授权绑定 API。
 */
export const applicationApi = {
  /** 获取应用列表 */
  list: () =>
    api.get<Application[]>('/applications'),

  /** 获取应用详情 */
  getById: (id: number) =>
    api.get<Application>(`/applications/${id}`),

  /** 创建应用 */
  create: (data: CreateApplicationRequest) =>
    api.post<Application>('/applications', data),

  /** 更新应用 */
  update: (id: number, data: UpdateApplicationRequest) =>
    api.put<Application>(`/applications/${id}`, data),

  /** 删除应用 */
  delete: (id: number) =>
    api.delete<void>(`/applications/${id}`),

  /** 查询应用授权的渠道 ID 列表 */
  listChannels: (id: number) =>
    api.get<number[]>(`/applications/${id}/channels`),

  /** 更新应用渠道授权 */
  updateChannels: (id: number, channelIds: number[]) =>
    api.put<void>(`/applications/${id}/channels`, { channelIds }),
};
