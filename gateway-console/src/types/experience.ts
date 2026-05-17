import type { ProviderType } from './api';

export type { ProviderType };

/** 聊天消息 */
export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

/** 体验聊天请求（支持两种模式） */
export interface ExperienceChatRequest {
  /** 使用已保存配置：供应商 ID */
  providerId?: number;
  /** 使用已保存配置：API Key ID（可选，不传则使用默认 Key） */
  apiKeyId?: number;
  /** 临时配置：供应商类型 */
  providerType?: ProviderType;
  /** 临时配置：Base URL */
  baseUrl?: string;
  /** 临时配置：API Key */
  apiKey?: string;
  /** 模型名称 */
  model: string;
  /** 对话消息列表 */
  messages: ChatMessage[];
  /** 最大输出 Token */
  maxTokens?: number;
  /** 温度参数 */
  temperature?: number;
}

/** 体验聊天 SSE 事件类型 */
export type ExperienceEventType = 'CONTENT' | 'USAGE' | 'ERROR' | 'DONE';

/** 内容数据 */
export interface ContentData {
  content: string;
}

/** 使用量数据 */
export interface UsageData {
  promptTokens: number;
  completionTokens: number;
}

/** 错误数据 */
export interface ErrorData {
  message: string;
}

/** 体验聊天 SSE 事件 */
export interface ExperienceChatEvent {
  type: ExperienceEventType;
  data: ContentData | UsageData | ErrorData | null;
}

/** 体验模型响应 */
export interface ExperienceModelResponse {
  id: number;
  providerModelId: string;
  displayName: string;
}

/** 体验状态（简化版，用于临时配置模式） */
export interface ExperienceState {
  /** 当前模式：saved=使用已保存配置，temp=临时配置 */
  mode?: 'saved' | 'temp';
  /** 供应商类型（临时模式） */
  providerType: ProviderType | null;
  /** API Key（临时模式） */
  apiKey: string;
  /** Base URL（临时模式） */
  baseUrl: string;
  /** 模型名称 */
  model: string;
  /** 对话消息列表 */
  messages: ChatMessage[];
  /** 是否正在加载 */
  isLoading: boolean;
  /** Token 使用量 */
  usage: {
    promptTokens: number;
    completionTokens: number;
  };
  /** 错误信息 */
  error: string | null;
}
