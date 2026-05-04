/** 分页请求参数 */
export interface PageParams {
  page?: number;
  size?: number;
}

/** 分页响应 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/** API 响应包装 */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

/** 通用状态枚举 */
export type Status = 'ENABLED' | 'DISABLED';
