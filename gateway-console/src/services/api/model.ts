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
import type { Model, CreateModelRequest, UpdateModelRequest, CopyModelRequest } from '@/types/model';
import type { PageResponse, PageParams } from '@/types/api';

/** 模型 API */
export const modelApi = {
  /** 获取模型列表 */
  list: (params?: PageParams) =>
    api.get<PageResponse<Model>>('/models', { params }),

  /** 获取模型详情 */
  get: (id: number) =>
    api.get<Model>(`/models/${id}`),

  /** 创建模型 */
  create: (data: CreateModelRequest) =>
    api.post<Model>('/models', data),

  /** 更新模型 */
  update: (id: number, data: UpdateModelRequest) =>
    api.put<Model>(`/models/${id}`, data),

  /** 删除模型 */
  delete: (id: number) =>
    api.delete<void>(`/models/${id}`),

  /** 启用/禁用模型 */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<Model>(`/models/${id}/state`, null, { params: { enabled } }),

  /** 清除字段人工锁定（恢复 models.dev 同步覆盖权限） */
  unlock: (id: number) =>
    api.post<Model>(`/models/${id}/unlock`),

  /** 复制模型（继承源规格生成新模型） */
  copy: (id: number, data: CopyModelRequest) =>
    api.post<Model>(`/models/${id}/copy`, data),
};