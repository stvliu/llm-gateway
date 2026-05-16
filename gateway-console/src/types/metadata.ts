/**
 * 元数据类型定义
 */

export type MetadataState = 'ACTIVE' | 'DISABLED' | 'DEPRECATED' | 'DELETED';
export type MetadataSource = 'BUILTIN' | 'MODELS_DEV' | 'PROVIDER_API' | 'MANUAL' | 'OVERRIDE';

export interface ProviderMetadata {
  id: number;
  providerId: string;
  providerName: string;
  providerType: string;
  providerConfig: Record<string, unknown>;
  iconUrl: string;
  description: string;
  tags: string[];
  state: MetadataState;
  createdAt: string;
  updatedAt: string;
  modelCount: number;
}

export interface ModelMetadata {
  id: number;
  providerId: string;
  providerModelId: string;
  displayName: string;
  modelFamily?: string;
  contextWindow?: number;
  maxInputTokens?: number;
  maxOutputTokens?: number;
  inputPrice?: number;
  outputPrice?: number;
  reasoningPrice?: number;
  cacheReadPrice?: number;
  cacheWritePrice?: number;
  inputAudioPrice?: number;
  outputAudioPrice?: number;
  knowledgeCutoff?: string;
  releaseDate?: string;
  openWeights?: boolean;
  modalities?: string[];
  capabilities?: Record<string, boolean>;
  source: MetadataSource;
  sourceSyncedAt?: string;
  state: MetadataState;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProviderMetadataRequest {
  providerId: string;
  providerName: string;
  providerType: string;
  providerConfig?: Record<string, unknown>;
  description?: string;
  iconUrl?: string;
  tags?: string[];
}

export interface UpdateProviderMetadataRequest {
  providerName?: string;
  providerConfig?: Record<string, unknown>;
  description?: string;
  iconUrl?: string;
  tags?: string[];
}

export interface ApplyMetadataRequest {
  apiKey: string;
  channelName?: string;
  channelPriority?: number;
}

export interface ApplyMetadataResult {
  providerId: number;
  providerName: string;
  modelIds: number[];
  modelNames: string[];
  createdAt: string;
}

export interface ProviderMetadataListParams {
  providerType?: string;
  keyword?: string;
  page?: number;
  size?: number;
}

export interface ModelMetadataListParams {
  providerId?: string;
  keyword?: string;
  source?: MetadataSource;
  page?: number;
  size?: number;
}

export interface MetadataSyncResult {
  syncedCount: number;
  addedCount: number;
  updatedCount: number;
  syncedAt: string;
}

/** Spring Data Page 响应格式 */
export interface SpringPage<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
