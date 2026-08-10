/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// 任务 10：ApplicationFormModal 失败处理策略字段单元测试
//
// 验证：
// 1) 新建模式渲染「失败处理策略」label
// 2) 新建模式默认 failureStrategy=FAIL_RETRY（提交时透传给 createMutation）
// 3) 编辑模式回填 application.failureStrategy（提交时透传给 updateMutation）
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, vi } from 'vitest';
import { App as AntApp } from 'antd';
import i18n from '@/i18n';
import { I18nextProvider } from 'react-i18next';
import ApplicationFormModal from '@/pages/Applications/ApplicationFormModal';
import type { Application } from '@/types/application';

// mock TanStack Query mutation，避免引入 QueryClientProvider 与网络调用
const { mockCreate, mockUpdate } = vi.hoisted(() => ({
  mockCreate: vi.fn().mockResolvedValue({ id: 1 }),
  mockUpdate: vi.fn().mockResolvedValue({}),
}));

vi.mock('@/services/query/useApplications', () => ({
  useCreateApplication: () => ({ mutateAsync: mockCreate, isPending: false }),
  useUpdateApplication: () => ({ mutateAsync: mockUpdate, isPending: false }),
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

/** 渲染 Modal 并提供 i18n + antd App 上下文 */
function renderModal(application?: Application) {
  return render(
    <I18nextProvider i18n={i18n}>
      <AntApp>
        <ApplicationFormModal visible application={application} onClose={() => {}} />
      </AntApp>
    </I18nextProvider>,
  );
}

/** 点击 antd Modal 的「确定/OK」按钮触发提交 */
async function clickOk() {
  const okButton = screen.getByRole('button', { name: /^确定$|^OK$/i });
  await userEvent.click(okButton);
}

describe('ApplicationFormModal 失败处理策略字段', () => {
  it('新建模式渲染「失败处理策略」label', async () => {
    renderModal();
    await waitFor(() => {
      expect(screen.getByText('失败处理策略')).toBeInTheDocument();
    });
  });

  it('新建模式默认 failureStrategy=FAIL_RETRY，提交时透传给 createMutation', async () => {
    mockCreate.mockClear();
    renderModal();
    await waitFor(() => {
      expect(screen.getByText('失败处理策略')).toBeInTheDocument();
    });
    // 填写必填字段
    await userEvent.type(screen.getByLabelText('应用编码'), 'APP-001');
    await userEvent.type(screen.getByLabelText('应用名称'), '测试应用');
    await clickOk();

    await waitFor(() => {
      expect(mockCreate).toHaveBeenCalledTimes(1);
      expect(mockCreate).toHaveBeenCalledWith(
        expect.objectContaining({ failureStrategy: 'FAIL_RETRY' }),
      );
    });
  });

  it('编辑模式回填 application.failureStrategy，提交时透传给 updateMutation', async () => {
    mockUpdate.mockClear();
    const existing: Application = {
      id: 7,
      code: 'APP-007',
      name: '存量应用',
      description: '',
      state: 'ACTIVE',
      timeout: 30,
      failureStrategy: 'FAIL_OVER',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };
    renderModal(existing);
    // 等待 useEffect 回填
    await waitFor(() => {
      expect(screen.getByText('失败处理策略')).toBeInTheDocument();
    });
    await clickOk();

    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledTimes(1);
      expect(mockUpdate).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 7,
          data: expect.objectContaining({ failureStrategy: 'FAIL_OVER' }),
        }),
      );
    });
  });
});
