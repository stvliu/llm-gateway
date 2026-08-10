/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { create } from 'zustand';

/**
 * 聊天消息类型
 */
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number;
  isStreaming?: boolean;
}

/**
 * 模型选项
 */
export interface ModelOption {
  id: string;
  name: string;
  provider: string;
}

/**
 * 聊天状态
 */
interface ChatState {
  messages: ChatMessage[];
  isOpen: boolean;
  currentModel: string;
  availableModels: ModelOption[];
  isLoading: boolean;

  // 操作
  addMessage: (message: Omit<ChatMessage, 'id' | 'timestamp'>) => void;
  updateLastMessage: (content: string) => void;
  clearMessages: () => void;
  togglePanel: () => void;
  setOpen: (open: boolean) => void;
  setCurrentModel: (model: string) => void;
  setLoading: (loading: boolean) => void;
  setStreaming: (isStreaming: boolean) => void;
}

/**
 * 生成唯一 ID
 */
const generateId = () => `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

/**
 * 默认可用模型列表
 */
const defaultModels: ModelOption[] = [
  { id: 'gpt-4o', name: 'GPT-4o', provider: 'OpenAI' },
  { id: 'gpt-4o-mini', name: 'GPT-4o Mini', provider: 'OpenAI' },
  { id: 'claude-sonnet-4-20250514', name: 'Claude Sonnet 4', provider: 'Anthropic' },
  { id: 'claude-3-5-haiku-20241022', name: 'Claude 3.5 Haiku', provider: 'Anthropic' },
];

/**
 * 聊天状态管理 Store
 */
export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  isOpen: false,
  currentModel: defaultModels[0].id,
  availableModels: defaultModels,
  isLoading: false,

  addMessage: (message) => {
    const newMessage: ChatMessage = {
      ...message,
      id: generateId(),
      timestamp: Date.now(),
    };
    set((state) => ({
      messages: [...state.messages, newMessage],
    }));
  },

  updateLastMessage: (content) => {
    set((state) => {
      const messages = [...state.messages];
      if (messages.length > 0) {
        const lastIndex = messages.length - 1;
        messages[lastIndex] = {
          ...messages[lastIndex],
          content,
          isStreaming: false,
        };
      }
      return { messages };
    });
  },

  clearMessages: () => {
    set({ messages: [] });
  },

  togglePanel: () => {
    set((state) => ({ isOpen: !state.isOpen }));
  },

  setOpen: (open) => {
    set({ isOpen: open });
  },

  setCurrentModel: (model) => {
    set({ currentModel: model });
  },

  setLoading: (loading) => {
    set({ isLoading: loading });
  },

  setStreaming: (isStreaming) => {
    set((state) => {
      const messages = [...state.messages];
      if (messages.length > 0) {
        const lastIndex = messages.length - 1;
        if (messages[lastIndex].role === 'assistant') {
          messages[lastIndex] = {
            ...messages[lastIndex],
            isStreaming,
          };
        }
      }
      return { messages };
    });
  },
}));
