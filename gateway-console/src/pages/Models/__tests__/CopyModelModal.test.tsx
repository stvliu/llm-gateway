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
// Task 4：CopyModelModal 复制模型对话框单元测试
//
// 验证：
// 1) 打开时预填源模型的 modelName/displayName/modelFamily
// 2) 修改 modelName 后提交 → mock useCopyModel 收到 { id, data }，onCopied 被调
// 3) modelName 清空时提交被校验拦截（显示「请输入模型标识」），copy 不被调用
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import i18n from '@/i18n';
import { I18nextProvider } from 'react-i18next';
import CopyModelModal from '@/pages/Models/CopyModelModal';
import type { Model } from '@/types/model';

// mock TanStack Query mutation，避免引入 QueryClientProvider 与网络调用
const { mockCopy } = vi.hoisted(() => ({
  mockCopy: vi.fn().mockResolvedValue({ id: 2, modelName: 'gpt-4-copy' }),
}));

vi.mock('@/services/query/useModels', () => ({
  useCopyModel: () => ({ mutateAsync: mockCopy, isPending: false }),
}));

/** 源模型（与列表行内复制传入的 record 一致） */
const source: Model = {
  id: 1,
  modelName: 'gpt-4',
  displayName: 'GPT-4',
  modelFamily: 'gpt-4',
  state: 'ACTIVE',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

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
  mockCopy.mockClear();
});

/** 渲染 Modal 并提供 i18n + antd App 上下文 */
function renderModal() {
  const onCopied = vi.fn();
  render(
    <I18nextProvider i18n={i18n}>
      <AntApp>
        <CopyModelModal open source={source} onClose={() => {}} onCopied={onCopied} />
      </AntApp>
    </I18nextProvider>,
  );
  return { onCopied };
}

/** 点击 antd Modal 的「确定/OK」按钮触发提交 */
async function clickOk() {
  const okButton = screen.getByRole('button', { name: /^确定$|^OK$/i });
  await userEvent.click(okButton);
}

describe('CopyModelModal', () => {
  it('预填源模型字段并提交复制', async () => {
    const { onCopied } = renderModal();

    // 断言三个字段预填源模型值
    const modelNameInput = screen.getByLabelText('模型标识');
    expect(modelNameInput).toHaveValue('gpt-4');
    expect(screen.getByLabelText('显示名称')).toHaveValue('GPT-4');
    expect(screen.getByLabelText('模型族')).toHaveValue('gpt-4');

    // 修改 modelName → 点击确定
    await userEvent.clear(modelNameInput);
    await userEvent.type(modelNameInput, 'gpt-4-copy');
    await clickOk();

    await waitFor(() => {
      expect(mockCopy).toHaveBeenCalledTimes(1);
      expect(mockCopy).toHaveBeenCalledWith({
        id: 1,
        data: { modelName: 'gpt-4-copy', displayName: 'GPT-4', modelFamily: 'gpt-4' },
      });
    });
    // 成功后 onCopied 携带新模型
    expect(onCopied).toHaveBeenCalledTimes(1);
    expect(onCopied).toHaveBeenCalledWith(expect.objectContaining({ id: 2 }));
  });

  it('modelName 为空时禁止提交', async () => {
    renderModal();

    // 清空 modelName → 点击确定
    const modelNameInput = screen.getByLabelText('模型标识');
    await userEvent.clear(modelNameInput);
    await clickOk();

    // 应显示必填校验提示，copy 不被调用
    await waitFor(() => {
      expect(screen.getByText('请输入模型标识')).toBeInTheDocument();
    });
    expect(mockCopy).not.toHaveBeenCalled();
  });
});
