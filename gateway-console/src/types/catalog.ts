/**
 * 目录类型定义 — 替代旧 metadata.ts
 *
 * <p>类型字段与后端 CatalogController 返回的 DTO 响应对齐。</p>
 */

/** 目录状态 */
export type CatalogState = 'ACTIVE' | 'DEPRECATED';

/** 数据来源 */
export type CatalogSource = 'BUILTIN' | 'MODELS_DEV' | 'PROVIDER_API' | 'MANUAL' | 'OVERRIDE';

/** 供应商类型 */
export type ProviderType = 'INTERNATIONAL' | 'DOMESTIC';

/** 计费模式 */
export type BillingMode = 'PAY_AS_YOU_GO' | 'SUBSCRIPTION' | 'PACKAGE';

/** 物化状态 */
export type MaterializeStatus = 'CREATED' | 'SKIPPED';

/** 物化类型 */
export type MaterializeType = 'PROVIDER' | 'PLAN' | 'MODEL';

/** 供应商目录（与后端 ProviderCatalogResponse 对齐） */
export interface ProviderCatalog {
  code: string;
  name: string;
  providerType: ProviderType;
  source: CatalogSource;
  materialized: boolean;
}

/** 套餐目录（与后端 PlanCatalogResponse 对齐） */
export interface PlanCatalog {
  planCode: string;
  providerCode: string;
  planName: string;
  billingMode: BillingMode;
  source: CatalogSource;
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
  source: CatalogSource;
  endpoints: PlanEndpoint[];
  pricing: PlanPricing[];
  materialized: boolean;
}

/** 模型目录（与后端 ModelCatalogResponse 对齐） */
export interface ModelCatalog {
  modelName: string;
  providerCode: string;
  capabilities: string[] | null;
  contextWindow: number | null;
  maxOutputTokens: number | null;
  source: CatalogSource;
  materialized: boolean;
}

/** 物化结果（与后端 MaterializeResult 对齐） */
export interface MaterializeResult {
  type: MaterializeType;
  code: string;
  entityId: number | null;
  status: MaterializeStatus;
}

/** 供应商目录查询参数 */
export interface ProviderCatalogListParams {
  providerType?: ProviderType;
  keyword?: string;
}

/** 模型目录查询参数 */
export interface ModelCatalogListParams {
  keyword?: string;
  capability?: string;
}
