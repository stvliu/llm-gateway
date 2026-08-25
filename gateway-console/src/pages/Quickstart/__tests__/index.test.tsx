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
// Quickstart 页统一请求配置条后的单元测试
//
// 验证核心点：
// 1) API Key/模型/协议渲染在统一配置条中，代码语言以 Segmented 位于代码示例头部
// 2) 模型自动选中第一个活跃模型，代码示例联动
// 3) 切换模型/协议/语言后代码示例同步更新
// 4) 非活跃模型不出现在模型下拉中
// 5) 无 Key 时处于阻塞态（Key 警示、复制禁用、试玩禁用）
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { I18nextProvider } from 'react-i18next';
import { MemoryRouter } from 'react-router-dom';
import i18n from '@/i18n';
import Quickstart from '@/pages/Quickstart';
import type { Model } from '@/types/model';

// vi.hoisted 提升 mock 引用，保证 vi.mock 工厂能访问
const { mockModels } = vi.hoisted(() => ({
  mockModels: vi.fn(),
}));

// mock 数据查询：useModels 返回可控模型列表
vi.mock('@/services/query/useModels', () => ({
  useModels: () => ({ data: mockModels(), isLoading: false }),
}));

// ApiKeySelector 的 Key 列表（无 Key 时展示阻塞态，不触发详情加载）
vi.mock('@/services/query/useUserApiKeys', () => ({
  useUserApiKeys: () => ({ data: [], isLoading: false }),
  useCreateUserApiKey: () => ({ mutateAsync: vi.fn() }),
}));

vi.mock('@/services/api/userApiKey', () => ({
  userApiKeyApi: {
    listByUser: vi.fn().mockResolvedValue([]),
    getDetail: vi.fn(),
    create: vi.fn(),
  },
}));

// KeyGenerateModal 依赖应用列表（弹窗未打开时仅空列表即可）
vi.mock('@/services/query/useApplications', () => ({
  useApplications: () => ({ data: [] }),
}));

// authStore：提供当前用户（ApiKeySelector 与 KeyGenerateModal 均依赖）
vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({ user: { id: 1, username: 'tester' } }),
}));

const testModels: Model[] = [
  { id: 1, modelName: 'gpt-4o', displayName: 'GPT-4o', state: 'ACTIVE', createdAt: '', updatedAt: '' },
  { id: 2, modelName: 'claude-3-5-sonnet', displayName: 'Claude 3.5 Sonnet', state: 'ACTIVE', createdAt: '', updatedAt: '' },
  { id: 3, modelName: 'gemini-2.0-flash', displayName: 'Gemini 2.0 Flash', state: 'INACTIVE', createdAt: '', updatedAt: '' },
];

beforeAll(async () => {
  // antd 依赖 matchMedia / ResizeObserver，jsdom 默认不提供
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
  await i18n.changeLanguage('zh-CN');
});

beforeEach(() => {
  mockModels.mockReturnValue({ items: testModels, total: testModels.length });
});

/** 渲染 Quickstart 页并提供 i18n + antd App 上下文（App.useApp().message 依赖 AntApp 包裹） */
function renderPage() {
  return render(
    <MemoryRouter>
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <Quickstart />
        </AntApp>
      </I18nextProvider>
    </MemoryRouter>,
  );
}

/** 读取代码示例中的 model 字段值（`"model": "<xxx>"`） */
function codeModelValue(): string {
  const code = document.querySelector('pre code');
  if (!code) throw new Error('未找到代码示例 <pre><code>');
  const match = code.textContent?.match(/"model": "([^"]+)"/);
  return match?.[1] ?? '';
}

/** 读取代码示例全文 */
function codeText(): string {
  const code = document.querySelector('pre code');
  if (!code) throw new Error('未找到代码示例 <pre><code>');
  return code.textContent ?? '';
}

describe('Quickstart 页统一请求配置条', () => {
  it('渲染配置条三条件与代码示例头部语言切换', () => {
    renderPage();
    expect(screen.getByText('请求配置')).toBeInTheDocument();
    expect(screen.getByText('API Key')).toBeInTheDocument();
    expect(screen.getByText('模型')).toBeInTheDocument();
    expect(screen.getByText('协议')).toBeInTheDocument();
    expect(screen.getByTestId('key-select')).toBeInTheDocument();
    expect(screen.getByTestId('model-select')).toBeInTheDocument();
    expect(screen.getByTestId('protocol-select')).toBeInTheDocument();
    // 代码语言以 Segmented 按钮组位于代码示例头部
    expect(screen.getByText('代码示例')).toBeInTheDocument();
    expect(screen.getByText('cURL')).toBeInTheDocument();
    expect(screen.getByText('Python')).toBeInTheDocument();
  });

  it('自动选中第一个活跃模型，代码示例联动', async () => {
    renderPage();
    await waitFor(() => {
      expect(codeModelValue()).toBe('gpt-4o');
    });
  });

  it('切换模型下拉后代码示例同步更新', async () => {
    renderPage();
    await waitFor(() => {
      expect(codeModelValue()).toBe('gpt-4o');
    });
    await userEvent.click(screen.getByTestId('model-select'));
    await userEvent.click(await screen.findByTitle('Claude 3.5 Sonnet'));
    await waitFor(() => {
      expect(codeModelValue()).toBe('claude-3-5-sonnet');
    });
  });

  it('切换协议为 Anthropic 后代码示例切换端点', async () => {
    renderPage();
    await waitFor(() => {
      expect(codeModelValue()).toBe('gpt-4o');
    });
    await userEvent.click(screen.getByTestId('protocol-select'));
    await userEvent.click(await screen.findByText('Anthropic'));
    await waitFor(() => {
      expect(codeText()).toContain('/anthropic/v1/messages');
    });
  });

  it('切换语言为 Python 后代码示例更新', async () => {
    renderPage();
    await waitFor(() => {
      expect(codeModelValue()).toBe('gpt-4o');
    });
    // 点击代码示例头部 Segmented 的 Python 选项
    await userEvent.click(screen.getByText('Python'));
    await waitFor(() => {
      expect(codeText()).toContain('import requests');
    });
  });

  it('非活跃模型不出现在模型下拉中', async () => {
    renderPage();
    await waitFor(() => {
      expect(codeModelValue()).toBe('gpt-4o');
    });
    await userEvent.click(screen.getByTestId('model-select'));
    await screen.findByTitle('Claude 3.5 Sonnet');
    expect(screen.queryByTitle('Gemini 2.0 Flash')).not.toBeInTheDocument();
  });

  it('无 Key 时处于阻塞态：Key 警示、复制禁用、试玩禁用并提示', async () => {
    renderPage();
    await waitFor(() => {
      expect(codeModelValue()).toBe('gpt-4o');
    });
    // Key 下拉为警示态
    expect(screen.getByTestId('key-select')).toHaveClass('ant-select-status-warning');
    // 复制 Key 按钮禁用
    expect(screen.getByRole('button', { name: '复制 Key' })).toBeDisabled();
    // 试玩区提示无 Key 且发送禁用
    expect(screen.getByText('请先创建 API Key')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /发送/ })).toBeDisabled();
  });
});
