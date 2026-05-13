/** 分页请求参数 */
export interface PageParams {
  page?: number;
  limit?: number;
}

/** 分页响应（与后端 PageResponse 一致） */
export interface PageResponse<T> {
  items: T[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
}

/** API 响应包装（与后端 ApiResponse 一致） */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: {
    code: string;
    message: string;
    details?: unknown;
  };
  traceId: string;
  timestamp: string;
}

/** 通用状态枚举 */
export type Status = 'ENABLED' | 'DISABLED';

/** 供应商类型 */
export type ProviderType =
  | 'OPENAI'
  | 'ANTHROPIC'
  | 'GEMINI'
  | 'DEEPSEEK'
  | 'MOONSHOT'
  | 'ZHIPU'
  | 'BAICHUAN'
  | 'MINIMAX'
  | 'VOLCENGINE'
  | 'QWEN'
  | 'WENXIN'
  | 'TENCENT'
  | 'XUNFEI'
  | 'OTHER';

/** 供应商类型选项 */
export interface ProviderTypeOption {
  value: ProviderType;
  label: string;
}
