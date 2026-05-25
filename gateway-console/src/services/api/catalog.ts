import { api } from './client';
import type {
  ProviderCatalog,
  PlanCatalog,
  PlanDetail,
  ModelSpecCatalog,
  MaterializeResult,
  ProviderCatalogListParams,
  ModelSpecCatalogListParams,
} from '@/types/catalog';

const BASE_URL = '/catalog';

/** 供应商目录 API */
export const providerCatalogApi = {
  /** 列出供应商目录 */
  list: (params?: ProviderCatalogListParams) =>
    api.get<ProviderCatalog[]>(`${BASE_URL}/providers`, { params }),
};

/** 套餐目录 API */
export const planCatalogApi = {
  /** 列出套餐目录 */
  list: (params?: { providerCode?: string }) =>
    api.get<PlanCatalog[]>(`${BASE_URL}/plans`, { params }),

  /** 获取套餐详情 */
  getDetail: (planCode: string) =>
    api.get<PlanDetail>(`${BASE_URL}/plans/${planCode}`),
};

/** 模型规格目录 API */
export const modelSpecCatalogApi = {
  /** 列出模型规格目录 */
  list: (params?: ModelSpecCatalogListParams) =>
    api.get<ModelSpecCatalog[]>(`${BASE_URL}/model-specs`, { params }),
};

/** 物化 API */
export const catalogMaterializeApi = {
  /** 物化供应商 */
  materializeProvider: (providerCode: string) =>
    api.post<MaterializeResult>(`${BASE_URL}/materialize/provider/${providerCode}`),

  /** 物化套餐 */
  materializePlan: (planCode: string) =>
    api.post<MaterializeResult>(`${BASE_URL}/materialize/plan/${planCode}`),

  /** 物化模型规格 */
  materializeModelSpec: (providerModelId: string) =>
    api.post<MaterializeResult>(`${BASE_URL}/materialize/model-spec/${providerModelId}`),
};

/** 同步 API */
export const catalogSyncApi = {
  /** 同步 BUILTIN 目录数据 */
  syncBuiltin: () =>
    api.post<void>(`${BASE_URL}/sync/builtin`),

  /** 同步 Models.dev 数据 */
  syncModelsDev: () =>
    api.post<void>(`${BASE_URL}/sync/models-dev`),
};
