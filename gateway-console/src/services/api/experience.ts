import { api } from './client';
import type { ExperienceChatRequest, ExperienceModelResponse, ExperienceStreamCallbacks } from '@/types/experience';

export const experienceApi = {
  /**
   * 获取供应商可用模型列表
   */
  async getProviderModels(providerId: number): Promise<ExperienceModelResponse[]> {
    return api.get<ExperienceModelResponse[]>(`/providers/${providerId}/models`);
  },

  /**
   * 流式聊天
   */
  async chatStream(
    request: ExperienceChatRequest,
    callbacks: ExperienceStreamCallbacks,
    signal?: AbortSignal
  ): Promise<void> {
    const response = await fetch('/api/experience/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify(request),
      signal,
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Request failed' }));
      callbacks.onError(error.message || 'Request failed');
      return;
    }

    const reader = response.body?.getReader();
    if (!reader) {
      callbacks.onError('No response body');
      return;
    }

    const decoder = new TextDecoder();
    let buffer = '';

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6);
            if (data === '[DONE]') {
              callbacks.onDone();
              return;
            }

            try {
              const event = JSON.parse(data);

              if (event.type === 'content') {
                callbacks.onContent(event.content);
              } else if (event.type === 'usage') {
                callbacks.onUsage?.(event.promptTokens, event.completionTokens);
              } else if (event.type === 'error') {
                callbacks.onError(event.message);
              }
            } catch {
              // 忽略解析错误
            }
          }
        }
      }
    } catch (err) {
      if ((err as Error).name === 'AbortError') {
        callbacks.onDone();
      } else {
        callbacks.onError((err as Error).message);
      }
    }

    callbacks.onDone();
  },
};
