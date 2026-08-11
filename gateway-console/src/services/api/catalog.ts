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
  PlanCatalog,
  PlanDetail,
  ProviderCatalog,
  ModelResponse,
  ProvisionResult,
  BatchProvisionResult,
  ProvisionRequest,
  BatchProvisionRequest,
} from '@/types/catalog';

/** 套餐目录 API */
export const planCatalogApi = {
  /** 列出供应商目录 */
  listProviders: (params?: { keyword?: string }) =>
    api.get<ProviderCatalog[]>('/plan-catalogs/providers', { params }),

  /** 列出套餐目录 */
  list: (params?: { providerCode?: string }) =>
    api.get<PlanCatalog[]>('/plan-catalogs', { params }),

  /** 获取套餐详情 */
  getDetail: (planCode: string) =>
    api.get<PlanDetail>(`/plan-catalogs/${planCode}`),

  /** 列出模型 */
  listModels: (params?: { keyword?: string; capability?: string }) =>
    api.get<ModelResponse[]>('/plan-catalogs/models', { params }),
};

/** 渠道开通 API */
export const provisionApi = {
  /** 从套餐创建渠道 */
  fromPlan: (planCode: string, data?: ProvisionRequest) =>
    api.post<ProvisionResult>(`/provision/from-plan/${planCode}`, data),

  /** 批量开通供应商 */
  batch: (providerCode: string, data?: BatchProvisionRequest) =>
    api.post<BatchProvisionResult>(`/provision/batch/${providerCode}`, data),

  /** 开通模型 */
  model: (modelName: string) =>
    api.post<ProvisionResult>(`/provision/model/${modelName}`),

  /** 同步 BUILTIN 目录数据 */
  syncBuiltin: () =>
    api.post<void>('/provision/sync/builtin'),
};
