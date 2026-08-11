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
  Application,
  ApplicationChannelItem,
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

  /** 查询应用授权的渠道及其应用级转移优先级 */
  listChannels: (id: number) =>
    api.get<ApplicationChannelItem[]>(`/applications/${id}/channels`),

  /** 更新应用渠道授权（含 priority） */
  updateChannels: (id: number, channels: ApplicationChannelItem[]) =>
    api.put<void>(`/applications/${id}/channels`, { channels }),
};
