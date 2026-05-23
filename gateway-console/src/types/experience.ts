/**
 * 模型体验相关类型定义
 */

/** 聊天消息 */
export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

/** 聊天请求 */
export interface ExperienceChatRequest {
  providerId: number;
  apiKeyId?: number;
  model: string;
  messages: ChatMessage[];
}

/** 模型响应 */
export interface ExperienceModelResponse {
  providerModelId: string;
  displayName?: string;
}

/** SSE 事件回调 */
export interface ExperienceStreamCallbacks {
  onContent: (chunk: string) => void;
  onUsage?: (promptTokens: number, completionTokens: number) => void;
  onError: (message: string) => void;
  onDone: () => void;
}
