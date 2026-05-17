import { api } from './index';
import type { ExperienceChatRequest, ExperienceChatEvent, ExperienceModelResponse } from '@/types/experience';

/** SSE 事件回调 */
export interface StreamCallbacks {
  onContent: (content: string) => void;
  onUsage: (promptTokens: number, completionTokens: number) => void;
  onError: (message: string) => void;
  onDone: () => void;
}

export const experienceApi = {
  /**
   * 流式聊天体验
   * 使用 fetch + ReadableStream 处理 SSE
   */
  chatStream: async (
    request: ExperienceChatRequest,
    callbacks: StreamCallbacks,
    signal?: AbortSignal
  ): Promise<void> => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
    const url = `${baseUrl}/api/v1/experience/chat`;

    // 获取认证 Token
    const token = localStorage.getItem('token');
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(request),
        signal,
      });

      if (!response.ok) {
        const errorText = await response.text();
        callbacks.onError(errorText || `HTTP ${response.status}`);
        return;
      }

      const reader = response.body?.getReader();
      if (!reader) {
        callbacks.onError('No response body');
        return;
      }

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // 解析 SSE 事件
        const lines = buffer.split('\n');
        buffer = lines.pop() || ''; // 保留未完成的行

        let eventType = '';
        let eventData = '';

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventType = line.substring(6).trim();
          } else if (line.startsWith('data:')) {
            eventData = line.substring(5).trim();
          } else if (line === '' && eventType) {
            // 空行表示事件结束，eventData 可选（如 DONE 事件）
            try {
              const event: ExperienceChatEvent = {
                type: eventType as ExperienceChatEvent['type'],
                data: eventData ? JSON.parse(eventData) : null,
              };

              switch (event.type) {
                case 'CONTENT':
                  callbacks.onContent((event.data as { content: string }).content);
                  break;
                case 'USAGE':
                  const usage = event.data as { promptTokens: number; completionTokens: number };
                  callbacks.onUsage(usage.promptTokens, usage.completionTokens);
                  break;
                case 'ERROR':
                  callbacks.onError((event.data as { message: string }).message);
                  break;
                case 'DONE':
                  callbacks.onDone();
                  break;
              }
            } catch {
              // SSE 事件解析失败，通知调用方
              callbacks.onError('响应格式解析失败，请重试');
            }
            eventType = '';
            eventData = '';
          }
        }
      }

    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        // 请求被取消，正常情况
        return;
      }
      callbacks.onError(error instanceof Error ? error.message : 'Unknown error');
    }
  },

  /**
   * 获取供应商的模型列表
   */
  getProviderModels: async (providerId: number): Promise<ExperienceModelResponse[]> => {
    return api.get(`/experience/providers/${providerId}/models`);
  },
};
