import { useState, useCallback, useRef } from 'react';
import type { ChatMessage, ExperienceState, ProviderType } from '@/types/experience';
import { experienceApi } from '@/services/api/experience';

const initialState: ExperienceState = {
  providerType: null,
  apiKey: '',
  baseUrl: '',
  model: '',
  messages: [],
  isLoading: false,
  usage: { promptTokens: 0, completionTokens: 0 },
  error: null,
};

export function useExperienceChat() {
  const [state, setState] = useState<ExperienceState>(initialState);
  const abortControllerRef = useRef<AbortController | null>(null);

  /** 设置配置 */
  const setConfig = useCallback(
    (config: {
      providerType: ProviderType;
      apiKey: string;
      baseUrl?: string;
      model: string;
    }) => {
      setState((prev) => ({
        ...prev,
        providerType: config.providerType,
        apiKey: config.apiKey,
        baseUrl: config.baseUrl || '',
        model: config.model,
      }));
    },
    []
  );

  /** 发送消息 */
  const sendMessage = useCallback(
    async (content: string) => {
      if (!state.providerType || !state.apiKey || !state.model) {
        setState((prev) => ({ ...prev, error: '请先完成配置' }));
        return;
      }

      // 添加用户消息
      const userMessage: ChatMessage = { role: 'user', content };
      const newMessages = [...state.messages, userMessage];

      // 添加助手消息占位
      const assistantMessage: ChatMessage = { role: 'assistant', content: '' };
      const messagesWithPlaceholder = [...newMessages, assistantMessage];

      setState((prev) => ({
        ...prev,
        messages: messagesWithPlaceholder,
        isLoading: true,
        error: null,
        usage: { promptTokens: 0, completionTokens: 0 },
      }));

      // 取消之前的请求
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
      abortControllerRef.current = new AbortController();

      let accumulatedContent = '';

      await experienceApi.chatStream(
        {
          providerType: state.providerType,
          apiKey: state.apiKey,
          baseUrl: state.baseUrl || undefined,
          model: state.model,
          messages: newMessages,
        },
        {
          onContent: (chunk) => {
            accumulatedContent += chunk;
            setState((prev) => {
              const updatedMessages = [...prev.messages];
              // 更新最后一条消息（助手消息）
              if (updatedMessages.length > 0) {
                updatedMessages[updatedMessages.length - 1] = {
                  ...updatedMessages[updatedMessages.length - 1],
                  content: accumulatedContent,
                };
              }
              return { ...prev, messages: updatedMessages };
            });
          },
          onUsage: (promptTokens, completionTokens) => {
            setState((prev) => ({
              ...prev,
              usage: { promptTokens, completionTokens },
            }));
          },
          onError: (message) => {
            setState((prev) => {
              // 移除助手占位消息
              const updatedMessages = prev.messages.slice(0, -1);
              return {
                ...prev,
                messages: updatedMessages,
                isLoading: false,
                error: message,
              };
            });
          },
          onDone: () => {
            setState((prev) => ({ ...prev, isLoading: false }));
          },
        },
        abortControllerRef.current.signal
      );
    },
    [state]
  );

  /** 清空对话 */
  const clearMessages = useCallback(() => {
    setState((prev) => ({
      ...prev,
      messages: [],
      usage: { promptTokens: 0, completionTokens: 0 },
      error: null,
    }));
  }, []);

  /** 停止生成 */
  const stopGeneration = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setState((prev) => ({ ...prev, isLoading: false }));
  }, []);

  return {
    state,
    setConfig,
    sendMessage,
    clearMessages,
    stopGeneration,
  };
}
