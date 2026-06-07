/**
 * 目录类型定义
 *
 * <p>类型字段与后端 PlanCatalogController 返回的 DTO 响应对齐。</p>
 */

/** 目录状态 */
export type CatalogState = 'ACTIVE' | 'DEPRECATED';

/** 计费模式（与后端 BillingMode 枚举 code 对齐，小写 snake_case） */
export type BillingMode = 'pay_as_you_go' | 'subscription' | 'hybrid' | 'prepaid_package';

/** 开通结果状态 */
export type ProvisionStatus = 'CREATED' | 'SKIPPED' | 'FAILED';

/** 供应商目录（与后端 ProviderCatalogResponse 对齐） */
export interface ProviderCatalog {
  code: string;
  name: string;
  materialized: boolean;
}

/** 套餐目录（与后端 PlanCatalogResponse 对齐） */
export interface PlanCatalog {
  planCode: string;
  providerCode: string;
  planName: string;
  billingMode: BillingMode;
  materialized: boolean;
}

/** 套餐端点信息 */
export interface PlanEndpoint {
  protocol: string;
  url: string;
}

/** 套餐定价信息 */
export interface PlanPricing {
  modelName: string;
  inputPrice: number | null;
  outputPrice: number | null;
  cacheReadPrice: number | null;
}

/** 套餐详情（与后端 PlanDetailResponse 对齐） */
export interface PlanDetail {
  planCode: string;
  providerCode: string;
  planName: string;
  billingMode: BillingMode;
  description: string | null;
  endpoints: PlanEndpoint[];
  pricing: PlanPricing[];
  materialized: boolean;
}

/** 模型响应（与后端 ModelResponse 对齐） */
export interface ModelResponse {
  modelName: string;
  providerCode: string | null;
  displayName: string | null;
  modelFamily: string | null;
  contextWindow: number | null;
  maxOutputTokens: number | null;
  capabilities: string[] | null;
  materialized: boolean;
}

/** 开通结果（与后端 ProvisionResult 对齐） */
export interface ProvisionResult {
  planCode: string;
  channelId: number | null;
  endpointCount: number;
  instanceCount: number;
  status: ProvisionStatus;
  errorMessage: string | null;
}

/** 批量开通结果（与后端 BatchProvisionResult 对齐） */
export interface BatchProvisionResult {
  providerCode: string;
  totalCount: number;
  successCount: number;
  skippedCount: number;
  failedCount: number;
  results: ProvisionResult[];
}

/** 开通请求 */
export interface ProvisionRequest {
  apiKeys?: string[];
}

/** 批量开通请求 */
export interface BatchProvisionRequest {
  planCodes?: string[];
}
