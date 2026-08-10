/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { api } from './client';
import type { Model, CreateModelRequest, UpdateModelRequest } from '@/types/model';
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
};