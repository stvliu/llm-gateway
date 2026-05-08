/**
 * Provider 模板类型定义
 */

export type TemplateType = 'OFFICIAL' | 'USER';
export type MarketStatus = 'PRIVATE' | 'PENDING' | 'PUBLISHED' | 'REJECTED';

export interface ProviderTemplate {
  id: number;
  templateCode: string;
  templateName: string;
  templateType: TemplateType;
  providerType: string;
  providerConfig: Record<string, unknown>;
  modelsConfig: ModelConfig[];
  authorId: number | null;
  authorName: string | null;
  marketStatus: MarketStatus;
  publishAt: string | null;
  downloadCount: number;
  tags: string[];
  description: string;
  iconUrl: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  modelCount: number;
}

export interface ModelConfig {
  provider_model_id: string;
  display_name: string;
  context_window?: number;
  input_price?: number;
  output_price?: number;
  capabilities?: Record<string, boolean>;
}

export interface CreateTemplateRequest {
  templateCode: string;
  templateName: string;
  providerType: string;
  providerConfig: Record<string, unknown>;
  modelsConfig: ModelConfig[];
  description?: string;
  iconUrl?: string;
  tags?: string[];
}

export interface UpdateTemplateRequest {
  templateName?: string;
  providerConfig?: Record<string, unknown>;
  modelsConfig?: ModelConfig[];
  description?: string;
  iconUrl?: string;
  tags?: string[];
}

export interface ApplyTemplateRequest {
  apiKey: string;
  channelName?: string;
  channelPriority?: number;
}

export interface ApplyTemplateResult {
  providerId: number;
  providerName: string;
  channelId: number;
  channelName: string;
  modelIds: number[];
  modelNames: string[];
  createdAt: string;
}

export interface TemplateListParams {
  type?: TemplateType;
  providerType?: string;
  keyword?: string;
  marketStatus?: MarketStatus;
  page?: number;
  limit?: number;
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
