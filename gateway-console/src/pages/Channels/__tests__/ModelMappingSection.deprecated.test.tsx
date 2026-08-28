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
// ModelMappingSection DEPRECATED 状态标签测试（任务 11）
//
// 验证目标：
// 1) state === 'DEPRECATED' 的模型映射行显示黄色"即将废弃"标签
// 2) 非 DEPRECATED 状态（如 ACTIVE）不显示"即将废弃"标签
import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/i18n';

beforeAll(() => {
  if (!window.matchMedia) {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: (query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: () => {},
        removeListener: () => {},
        addEventListener: () => {},
        removeEventListener: () => {},
        dispatchEvent: () => false,
      }),
    });
  }
  if (!(globalThis as { ResizeObserver?: unknown }).ResizeObserver) {
    class ResizeObserverStub {
      observe(): void {}
      unobserve(): void {}
      disconnect(): void {}
    }
    (globalThis as unknown as { ResizeObserver: typeof ResizeObserverStub }).ResizeObserver =
      ResizeObserverStub;
  }
});

vi.mock('@/services/api/channel', () => {
  return {
    channelApi: {
      createModel: vi.fn(() => Promise.resolve()),
      deleteModel: vi.fn(() => Promise.resolve()),
      updateUpstreamModelName: vi.fn(() => Promise.resolve()),
      updateModel: vi.fn(() => Promise.resolve()),
      list: vi.fn(() => Promise.resolve([])),
      get: vi.fn(() => Promise.resolve({})),
      listCredentials: vi.fn(() => Promise.resolve([])),
      listModels: vi.fn(() => Promise.resolve([])),
    },
  };
});

import { ModelMappingSection } from '../ModelMappingSection';

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
}

function renderWithProviders(ui: React.ReactElement) {
  const client = makeQueryClient();
  return render(
    <I18nextProvider i18n={i18n}>
      <QueryClientProvider client={client}>{ui}</QueryClientProvider>
    </I18nextProvider>
  );
}

function makeMapping(overrides: Partial<{ id: number; modelId: number; state: string }>) {
  return {
    id: 200,
    channelId: 1,
    modelId: 100,
    modelName: 'gpt-4o',
    upstreamModelName: 'gpt-4o-upstream',
    state: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('ModelMappingSection DEPRECATED 状态标签（任务 11）', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
  });

  it('DEPRECATED 状态实例显示"即将废弃"标签', () => {
    renderWithProviders(
      <ModelMappingSection
        channelId={1}
        channelModels={[makeMapping({ id: 200, modelId: 100, state: 'DEPRECATED' })]}
      />
    );
    expect(screen.getByText('即将废弃')).toBeInTheDocument();
  });

  it('非 DEPRECATED 状态（ACTIVE）不显示"即将废弃"标签', () => {
    renderWithProviders(
      <ModelMappingSection
        channelId={1}
        channelModels={[makeMapping({ id: 200, modelId: 100, state: 'ACTIVE' })]}
      />
    );
    expect(screen.queryByText('即将废弃')).not.toBeInTheDocument();
  });
});

export {};
