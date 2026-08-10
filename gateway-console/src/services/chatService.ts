/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import type { ChatMessage } from '@/stores/chatStore';

/**
 * 聊天请求参数
 */
interface ChatRequest {
  model: string;
  messages: Array<{
    role: 'user' | 'assistant' | 'system';
    content: string;
  }>;
  stream?: boolean;
}

/**
 * 流式响应回调
 */
interface StreamCallbacks {
  onToken: (token: string) => void;
  onComplete: () => void;
  onError: (error: Error) => void;
}

/**
 * 聊天服务 - 调用 LLM Gateway API
 */
export const chatService = {
  /**
   * 流式聊天
   */
  async streamChat(
    messages: ChatMessage[],
    model: string,
    callbacks: StreamCallbacks
  ): Promise<void> {
    const apiUrl = '/v1/chat/completions';

    // 转换消息格式，过滤掉 streaming 状态的消息
    const apiMessages = messages
      .filter((msg) => !msg.isStreaming)
      .map((msg) => ({
        role: msg.role,
        content: msg.content,
      }));

    const requestBody: ChatRequest = {
      model,
      messages: apiMessages,
      stream: true,
    };

    try {
      const response = await fetch(apiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          // 使用当前用户的 API Key 或 token
          'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
        },
        body: JSON.stringify(requestBody),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error?.message || `HTTP ${response.status}`);
      }

      if (!response.body) {
        throw new Error('Response body is null');
      }

      // 处理 SSE 流
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          const trimmedLine = line.trim();
          if (!trimmedLine || !trimmedLine.startsWith('data: ')) continue;

          const data = trimmedLine.slice(6);
          if (data === '[DONE]') {
            callbacks.onComplete();
            return;
          }

          try {
            const json = JSON.parse(data);
            const content = json.choices?.[0]?.delta?.content;
            if (content) {
              callbacks.onToken(content);
            }
          } catch {
            // 忽略解析错误
          }
        }
      }

      callbacks.onComplete();
    } catch (error) {
      callbacks.onError(error instanceof Error ? error : new Error(String(error)));
    }
  },

  /**
   * 非流式聊天（备用）
   */
  async chat(messages: ChatMessage[], model: string): Promise<string> {
    const apiUrl = '/v1/chat/completions';

    const apiMessages = messages.map((msg) => ({
      role: msg.role,
      content: msg.content,
    }));

    const requestBody: ChatRequest = {
      model,
      messages: apiMessages,
      stream: false,
    };

    const response = await fetch(apiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
      },
      body: JSON.stringify(requestBody),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error?.message || `HTTP ${response.status}`);
    }

    const data = await response.json();
    return data.choices?.[0]?.message?.content || '';
  },
};
