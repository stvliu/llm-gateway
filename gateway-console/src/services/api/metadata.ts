import { api } from './client';
import type {
  ProviderMetadata,
  ModelMetadata,
  CreateProviderMetadataRequest,
  UpdateProviderMetadataRequest,
  ApplyMetadataRequest,
  ApplyMetadataResult,
  ProviderMetadataListParams,
  ModelMetadataListParams,
  MetadataSyncResult,
  SpringPage,
} from '@/types/metadata';

const BASE_URL = '/provider-metadata';
const MODEL_BASE_URL = '/model-metadata';
const SYNC_BASE_URL = '/metadata-sync';

/** 供应商元数据 API */
export const providerMetadataApi = {
  list: (params: ProviderMetadataListParams) =>
    api.get<SpringPage<ProviderMetadata>>(BASE_URL, { params }),

  get: (id: number) =>
    api.get<ProviderMetadata>(`${BASE_URL}/${id}`),

  listAll: () =>
    api.get<ProviderMetadata[]>(`${BASE_URL}/list`),

  create: (data: CreateProviderMetadataRequest) =>
    api.post<ProviderMetadata>(BASE_URL, data),

  update: (id: number, data: UpdateProviderMetadataRequest) =>
    api.put<ProviderMetadata>(`${BASE_URL}/${id}`, data),

  delete: (id: number) =>
    api.delete(`${BASE_URL}/${id}`),

  updateMarketStatus: (id: number, marketState: string) =>
    api.patch(`${BASE_URL}/${id}/market-state`, { marketState }),

  apply: (id: number, data: ApplyMetadataRequest) =>
    api.post<ApplyMetadataResult>(`${BASE_URL}/${id}/apply`, data),
};

/** 模型元数据 API */
export const modelMetadataApi = {
  list: (params: ModelMetadataListParams) =>
    api.get<SpringPage<ModelMetadata>>(MODEL_BASE_URL, { params }),

  get: (id: number) =>
    api.get<ModelMetadata>(`${MODEL_BASE_URL}/${id}`),

  listByProviderId: (providerId: string) =>
    api.get<ModelMetadata[]>(`${MODEL_BASE_URL}/providers/${providerId}`),

  create: (data: Partial<ModelMetadata>) =>
    api.post<ModelMetadata>(MODEL_BASE_URL, data),

  update: (id: number, data: Partial<ModelMetadata>) =>
    api.put<ModelMetadata>(`${MODEL_BASE_URL}/${id}`, data),

  delete: (id: number) =>
    api.delete(`${MODEL_BASE_URL}/${id}`),
};

/** 元数据同步 API */
export const metadataSyncApi = {
  syncAll: () =>
    api.post<MetadataSyncResult>(`${SYNC_BASE_URL}/all`),

  syncBuiltin: () =>
    api.post<MetadataSyncResult>(`${SYNC_BASE_URL}/builtin`),

  syncModelsDev: () =>
    api.post<MetadataSyncResult>(`${SYNC_BASE_URL}/models-dev`),
};