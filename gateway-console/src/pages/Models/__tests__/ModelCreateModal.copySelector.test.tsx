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
// Task 4 审查修复：新增弹窗「从已有模型复制」选择器测试
//
// 验证：
// 1) 选择器按 modelFamily 分组渲染（OptionGroup 分组标题存在）
// 2) 选中源模型后打开复制对话框（CopyModelModal 收到 open=true 与正确的 source）
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/i18n';
import ModelCreateModal from '@/pages/Models/ModelCreateModal';

// 多族模型数据：gpt-4 组两条 + claude-3 组一条
const { models } = vi.hoisted(() => ({
  models: [
    { id: 1, modelName: 'gpt-4', displayName: 'GPT-4', modelFamily: 'gpt-4' },
    { id: 2, modelName: 'gpt-4o', displayName: 'GPT-4o', modelFamily: 'gpt-4' },
    { id: 3, modelName: 'claude-3-opus', displayName: 'Claude 3 Opus', modelFamily: 'claude-3' },
  ],
}));

// 记录 CopyModelModal 收到的 props，避免真实复制对话框及其依赖
const { copyModalProps } = vi.hoisted(() => ({
  copyModalProps: {
    current: null as { open: boolean; source: { id: number; modelName: string } | null } | null,
  },
}));

vi.mock('@/pages/Models/CopyModelModal', () => ({
  __esModule: true,
  default: (props: { open: boolean; source: { id: number; modelName: string } | null }) => {
    copyModalProps.current = props;
    return props.open ? (
      <div data-testid="copy-modal" data-source-id={props.source?.id} />
    ) : null;
  },
}));

vi.mock('@/services/query/useModels', () => ({
  useModels: () => ({
    data: { items: models, pagination: { page: 1, limit: 1000, total: 3, totalPages: 1 } },
    isLoading: false,
  }),
  useCreateModel: () => ({ mutateAsync: vi.fn().mockResolvedValue({ id: 9 }), isPending: false }),
}));

vi.mock('@/services/query/useProviders', () => ({
  useProviders: () => ({ data: { items: [] }, isLoading: false }),
}));

vi.mock('@/services/query/useChannels', () => ({
  useChannels: () => ({ data: [] }),
  useCreateChannelModel: () => ({ mutateAsync: vi.fn().mockResolvedValue(undefined), isPending: false }),
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

describe('ModelCreateModal 复制选择器', () => {
  it('按 modelFamily 分组渲染，选中源模型后打开复制对话框', async () => {
    render(
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <ModelCreateModal open onClose={() => {}} />
        </AntApp>
      </I18nextProvider>,
    );

    // 打开复制选择器下拉（placeholder 为 pointer-events:none 的 span，需点击容器内搜索 input）
    const selectPlaceholder = screen.getByText('选择源模型（可选）');
    const selectInput = selectPlaceholder.closest('.ant-select')!.querySelector('input')!;
    await userEvent.click(selectInput);

    // 两个分组标题（OptionGroup）按 modelFamily 渲染
    const groupGpt4 = await screen.findByText('gpt-4');
    expect(groupGpt4.className).toContain('ant-select-item-group');
    expect(screen.getByText('claude-3').className).toContain('ant-select-item-group');

    // 选中 gpt-4 组下的源模型
    await userEvent.click(screen.getByText('gpt-4 (GPT-4)'));

    // 复制对话框打开且携带正确源模型
    expect(screen.getByTestId('copy-modal')).toHaveAttribute('data-source-id', '1');
    expect(copyModalProps.current?.open).toBe(true);
    expect(copyModalProps.current?.source).toEqual(
      expect.objectContaining({ id: 1, modelName: 'gpt-4' }),
    );
  });
});
