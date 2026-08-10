/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// Task 11：UserApiKeyModal 应用绑定改造单元测试
//
// 验证 tasks.md 5.3 核心点：
// 1) 删除「团队继承」Alert（权限说明文案不再出现）
// 2) 创建表单 Application 必填（未选时提交显示校验提示，create 不被调用）
// 3) 创建表单提交时透传 applicationId 给 userApiKeyApi.create
// 4) 列表渲染「所属应用」列（已绑定显示应用名，未绑定显示「未绑定」）
// 5) 补绑交互（核心）：编辑模式加载已存在 Key 的 applicationId 到表单，
//    提交时 update 调用传入 applicationId（支持补绑/转移）
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import i18n from '@/i18n';
import { I18nextProvider } from 'react-i18next';
import UserApiKeyModal from '@/pages/Users/UserApiKeyModal';
import type { UserApiKey } from '@/types/userApiKey';
import type { Application } from '@/types/application';

// mock API 层与 Query hooks，避免引入网络调用与 QueryClientProvider
const { mockCreate, mockUpdate, mockKeys, mockApplications } = vi.hoisted(() => ({
  mockCreate: vi.fn().mockResolvedValue({ id: 10, keyPrefix: 'sk-', keyPlain: 'sk-new-plain' }),
  mockUpdate: vi.fn().mockResolvedValue({}),
  mockKeys: vi.fn(),
  mockApplications: vi.fn(),
}));

vi.mock('@/services/api/userApiKey', () => ({
  userApiKeyApi: {
    create: mockCreate,
    update: mockUpdate,
  },
}));

vi.mock('@/services/query/useUserApiKeys', () => ({
  useUserApiKeys: () => ({ data: mockKeys(), isLoading: false }),
  useDeleteUserApiKey: () => ({ mutateAsync: vi.fn().mockResolvedValue(undefined) }),
}));

vi.mock('@/services/query/useApplications', () => ({
  useApplications: () => ({ data: mockApplications() }),
}));

// useConfirm 默认直接调用 onConfirm，跳过 Modal.confirm 交互
vi.mock('@/hooks/useConfirm', () => ({
  useConfirm: () => ({
    confirm: (opts: { onConfirm: () => void | Promise<void> }) => opts.onConfirm(),
  }),
}));

const appA: Application = {
  id: 1,
  code: 'APP-001',
  name: '应用A',
  description: '',
  state: 'ACTIVE',
  timeout: 30,
  failureStrategy: 'FAIL_RETRY',
  createdAt: '',
  updatedAt: '',
};

const appB: Application = {
  id: 2,
  code: 'APP-002',
  name: '应用B',
  description: '',
  state: 'ACTIVE',
  timeout: 30,
  failureStrategy: 'FAIL_RETRY',
  createdAt: '',
  updatedAt: '',
};

const keyBound: UserApiKey = {
  id: 100,
  userId: 1,
  applicationId: 1,
  keyPrefix: 'sk-aaa',
  keyPlain: 'sk-aaa-full',
  name: 'key-bound',
  deleted: false,
  createdAt: '',
  updatedAt: '',
};

const keyUnbound: UserApiKey = {
  id: 101,
  userId: 1,
  applicationId: null,
  keyPrefix: 'sk-bbb',
  keyPlain: 'sk-bbb-full',
  name: 'key-unbound',
  deleted: false,
  createdAt: '',
  updatedAt: '',
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
  mockKeys.mockReturnValue([keyBound, keyUnbound]);
  mockApplications.mockReturnValue([appA, appB]);
  mockCreate.mockClear();
  mockUpdate.mockClear();
});

/** 渲染 Modal 并提供 i18n + antd App 上下文 */
function renderModal() {
  return render(
    <I18nextProvider i18n={i18n}>
      <AntApp>
        <UserApiKeyModal open userId={1} username="alice" onClose={() => {}} />
      </AntApp>
    </I18nextProvider>,
  );
}

describe('UserApiKeyModal 应用绑定改造', () => {
  it('删除「团队继承」Alert：权限说明文案不再出现', async () => {
    renderModal();
    // 等待表格加载
    await waitFor(() => {
      expect(screen.getByText('key-bound')).toBeInTheDocument();
    });
    // 旧 Alert 文案不应存在
    expect(screen.queryByText('权限说明')).not.toBeInTheDocument();
    expect(screen.queryByText(/API Key 的渠道访问权限由用户所属团队决定/)).not.toBeInTheDocument();
  });

  it('列表渲染「所属应用」列：已绑定显示应用名，未绑定显示「未绑定」', async () => {
    renderModal();
    await waitFor(() => {
      // applicationId=1 的行显示应用名 + ID
      expect(screen.getByText('应用A (1)')).toBeInTheDocument();
      // applicationId=null 的行显示「未绑定」
      expect(screen.getByText('未绑定')).toBeInTheDocument();
    });
  });

  it('创建表单 Application 必填：未选应用直接提交显示校验提示，不调用 create', async () => {
    renderModal();
    // 点击「新增 API Key」按钮打开表单
    const addBtn = await screen.findByRole('button', { name: /新增 API Key/ });
    await userEvent.click(addBtn);

    // 仅填写 name，不选 Application
    await userEvent.type(screen.getByLabelText('名称'), 'new-key');

    // 点击表单内的「创建」按钮触发提交（antd autoInsertSpace 会给两中文字符加空格，用正则兼容）
    const submitBtn = screen.getByRole('button', { name: /创\s?建/ });
    await userEvent.click(submitBtn);

    // 应显示 Application 必填校验提示
    await waitFor(() => {
      expect(screen.getByText('请选择应用')).toBeInTheDocument();
    });
    // create 不应被调用
    expect(mockCreate).not.toHaveBeenCalled();
  });

  it('创建表单提交时透传 applicationId 给 userApiKeyApi.create', async () => {
    renderModal();
    const addBtn = await screen.findByRole('button', { name: /新增 API Key/ });
    await userEvent.click(addBtn);

    await userEvent.type(screen.getByLabelText('名称'), 'new-key');

    // 选择 Application：点击 Select 打开下拉，再点击选项（antd 5 用 .ant-select 作为点击容器）
    const selectEl = document.querySelector('.ant-select') as HTMLElement;
    expect(selectEl).toBeTruthy();
    await userEvent.click(selectEl);
    // 等待下拉选项出现并点击「应用A (1)」（用 selector 限定到下拉项，避免匹配选中项）
    const option = await screen.findByText('应用A (1)', { selector: '.ant-select-item-option-content' });
    await userEvent.click(option);

    const submitBtn = screen.getByRole('button', { name: /创\s?建/ });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockCreate).toHaveBeenCalledTimes(1);
      expect(mockCreate).toHaveBeenCalledWith(
        expect.objectContaining({ userId: 1, applicationId: 1, name: 'new-key' }),
      );
    });
  });

  it('补绑交互：编辑模式加载已存在 Key 的 applicationId，提交时 update 传回 applicationId', async () => {
    renderModal();
    // 等待表格加载，找到 key-bound 行的编辑按钮
    const boundRow = await screen.findByText('key-bound');
    const row = boundRow.closest('tr') as HTMLElement;
    expect(row).toBeTruthy();
    const editBtn = within(row).getByRole('button', { name: /edit|编辑|Edit/ });
    await userEvent.click(editBtn);

    // 等待表单出现：编辑模式下提交按钮文案为「保存」（antd autoInsertSpace 兼容用正则）
    const submitBtn = await screen.findByRole('button', { name: /保\s?存/ });
    expect(submitBtn).toBeInTheDocument();
    // Application Select 应已回填 applicationId=1（antd 5 用 .ant-select-content 显示选中值）
    await waitFor(() => {
      const content = document.querySelector('.ant-select-content');
      expect(content?.textContent).toContain('应用A');
    });

    // 直接提交（不修改），update 应传入从表单加载的 applicationId
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledTimes(1);
      expect(mockUpdate).toHaveBeenCalledWith(
        100,
        expect.objectContaining({ name: 'key-bound', applicationId: 1 }),
      );
    });
  });

  it('补绑转移：编辑时切换 applicationId，提交时 update 传新 applicationId', async () => {
    renderModal();
    const boundRow = await screen.findByText('key-bound');
    const row = boundRow.closest('tr') as HTMLElement;
    const editBtn = within(row).getByRole('button', { name: /edit|编辑|Edit/ });
    await userEvent.click(editBtn);

    await screen.findByRole('button', { name: /保\s?存/ });

    // 切换 Application：打开下拉，选择「应用B (2)」（antd 5 用 .ant-select 作为点击容器）
    const selectEl = document.querySelector('.ant-select') as HTMLElement;
    await userEvent.click(selectEl);
    const option = await screen.findByText('应用B (2)', { selector: '.ant-select-item-option-content' });
    await userEvent.click(option);

    const submitBtn = screen.getByRole('button', { name: /保\s?存/ });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledTimes(1);
      expect(mockUpdate).toHaveBeenCalledWith(
        100,
        expect.objectContaining({ applicationId: 2 }),
      );
    });
  });
});
