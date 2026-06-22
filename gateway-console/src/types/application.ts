/**
 * 应用信息（与后端 ApplicationResponse 一致）
 *
 * Application 是权限+行为双聚合根，承载 Key 归属与渠道可见性。
 */
export interface Application {
  id: number;
  /** 应用编码，全局唯一 */
  code: string;
  /** 应用名称 */
  name: string;
  /** 应用描述 */
  description: string;
  /** 应用生命周期状态（ACTIVE/INACTIVE） */
  state: string;
  /** 容灾画像 ID（预留） */
  resilienceProfileId?: number | null;
  /** 配额预算 ID（预留） */
  quotaBudgetId?: number | null;
  /** 看板 ID（预留） */
  dashboardId?: number | null;
  createdAt: string;
  updatedAt: string;
}

/** 创建应用请求 */
export interface CreateApplicationRequest {
  code: string;
  name: string;
  description?: string;
}

/** 更新应用请求 */
export type UpdateApplicationRequest = CreateApplicationRequest;
