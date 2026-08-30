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
// Task 4 审查修复：Models 列表行内复制按钮测试
//
// 验证：点击行内复制按钮（CopyOutlined）后，复制对话框打开且源为当前行模型。
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/i18n';
import Models from '@/pages/Models';

// 一条模型数据渲染表格行
const { model, copyModalProps } = vi.hoisted(() => ({
  model: {
    id: 1,
    modelName: 'gpt-4',
    displayName: 'GPT-4',
    modelFamily: 'gpt-4',
    state: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  copyModalProps: {
    current: null as { open: boolean; source: { id: number; modelName: string } | null } | null,
  },
}));

vi.mock('@/services/query/useModels', () => ({
  useModels: () => ({
    data: { items: [model], pagination: { page: 1, limit: 20, total: 1, totalPages: 1 } },
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  }),
  useDeleteModel: () => ({ mutateAsync: vi.fn().mockResolvedValue(undefined), isPending: false }),
  useSetEnabledModel: () => ({
    mutateAsync: vi.fn().mockResolvedValue(undefined),
    isPending: false,
  }),
}));

// 记录 CopyModelModal 收到的 props，避免真实复制对话框及其依赖
vi.mock('@/pages/Models/CopyModelModal', () => ({
  __esModule: true,
  default: (props: { open: boolean; source: { id: number; modelName: string } | null }) => {
    copyModalProps.current = props;
    return props.open ? (
      <div data-testid="copy-modal" data-source-id={props.source?.id} />
    ) : null;
  },
}));

// 简化其它子弹窗，避免其自身 hook 依赖（createOpen/editOpen 默认关闭）
vi.mock('@/pages/Models/ModelCreateModal', () => ({
  __esModule: true,
  default: () => <div data-testid="create-modal" />,
}));
vi.mock('@/pages/Models/ModelEditDrawer', () => ({
  __esModule: true,
  default: () => <div data-testid="edit-drawer" />,
}));

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

describe('Models 列表行内复制按钮', () => {
  // 全页 Table 渲染较重，放宽超时（慢速 CI/Windows jsdom）
  it('点击复制按钮后打开复制对话框并携带当前行模型', async () => {
    render(
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <Models />
        </AntApp>
      </I18nextProvider>,
    );

    // 通过 CopyOutlined 图标的 aria-label 定位行内复制按钮
    const copyBtn = screen.getByRole('img', { name: 'copy' }).closest('button');
    expect(copyBtn).not.toBeNull();
    await userEvent.click(copyBtn!);

    // 复制对话框打开且源为当前行模型
    expect(screen.getByTestId('copy-modal')).toHaveAttribute('data-source-id', '1');
    expect(copyModalProps.current?.open).toBe(true);
    expect(copyModalProps.current?.source).toEqual(
      expect.objectContaining({ id: 1, modelName: 'gpt-4' }),
    );
  }, 20000);
});
