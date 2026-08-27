/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
  setAvailableModels: (models: ModelOption[]) => void;
  setLoading: (loading: boolean) => void;
  setStreaming: (isStreaming: boolean) => void;
}

/**
 * 生成唯一 ID
 */
const generateId = () => `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

/**
 * 聊天状态管理 Store
 *
 * <p>可用模型列表由 ChatPanel 从真实模型接口（useModels）加载后注入，
 * 不再内置硬编码模型。</p>
 */
export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  isOpen: false,
  currentModel: '',
  availableModels: [],
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

  setAvailableModels: (models) => {
    set({ availableModels: models });
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
