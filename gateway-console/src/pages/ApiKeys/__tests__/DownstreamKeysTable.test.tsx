// Task 10：DownstreamKeysTable 应用绑定改造单元测试
//
// 验证 tasks.md 5.3 核心点：
// 1) 列表渲染「所属应用」列（已绑定显示应用名，未绑定显示「未绑定」）
// 2) 创建表单 Application 必填（未选时提交显示校验提示，createMutation 不被调用）
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { MemoryRouter } from 'react-router-dom';
import i18n from '@/i18n';
import { I18nextProvider } from 'react-i18next';
import DownstreamKeysTable from '@/pages/ApiKeys/DownstreamKeysTable';
import type { UserApiKey } from '@/types/userApiKey';
import type { Application } from '@/types/application';
import type { User } from '@/types/user';

// mock TanStack Query hooks，避免引入 QueryClientProvider 与网络调用
const {
  mockKeys,
  mockUsers,
  mockApplications,
  mockCreate,
  mockDelete,
} = vi.hoisted(() => ({
  mockKeys: vi.fn(),
  mockUsers: vi.fn(),
  mockApplications: vi.fn(),
  mockCreate: vi.fn().mockResolvedValue({ id: 10, keyPrefix: 'sk-', keyPlain: 'sk-new-plain' }),
  mockDelete: vi.fn().mockResolvedValue(undefined),
}));

vi.mock('@/services/query/useUserApiKeys', () => ({
  useAllUserApiKeys: () => ({ data: mockKeys(), isLoading: false }),
  useDeleteUserApiKey: () => ({ mutateAsync: mockDelete }),
  useCreateUserApiKey: () => ({ mutateAsync: mockCreate }),
}));

vi.mock('@/services/query/useUsers', () => ({
  useUsers: () => ({ data: mockUsers() }),
}));

vi.mock('@/services/query/useApplications', () => ({
  useApplications: () => ({ data: mockApplications() }),
}));

// 简化 MaskedKeyDisplay，避免其内部交互逻辑干扰表格断言
vi.mock('@/components/MaskedKeyDisplay', () => ({
  MaskedKeyDisplay: ({ keyPlain }: { keyPlain: string }) => <span>{keyPlain}</span>,
}));

const testApplication: Application = {
  id: 1,
  code: 'APP-001',
  name: '测试应用',
  description: '',
  state: 'ACTIVE',
  timeout: 30,
  failureStrategy: 'FAIL_RETRY',
  createdAt: '',
  updatedAt: '',
};

const testUser: User = {
  id: 1,
  username: 'alice',
  email: 'alice@example.com',
  role: 'ADMIN',
  state: 'ACTIVE',
  createdAt: '',
  updatedAt: '',
};

const testKeys: UserApiKey[] = [
  {
    id: 1,
    userId: 1,
    applicationId: 1,
    keyPrefix: 'sk-',
    keyPlain: 'sk-aaa',
    name: 'key1',
    deleted: false,
    createdAt: '',
    updatedAt: '',
  },
  {
    id: 2,
    userId: 1,
    applicationId: null,
    keyPrefix: 'sk-',
    keyPlain: 'sk-bbb',
    name: 'key2',
    deleted: false,
    createdAt: '',
    updatedAt: '',
  },
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
  mockKeys.mockReturnValue(testKeys);
  mockUsers.mockReturnValue({ items: [testUser] });
  mockApplications.mockReturnValue([testApplication]);
  mockCreate.mockClear();
});

/** 渲染表格并提供 Router + i18n + antd App 上下文 */
function renderTable() {
  return render(
    <MemoryRouter>
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <DownstreamKeysTable />
        </AntApp>
      </I18nextProvider>
    </MemoryRouter>,
  );
}

describe('DownstreamKeysTable 应用绑定改造', () => {
  it('列表渲染「所属应用」列：已绑定显示应用名，未绑定显示「未绑定」', async () => {
    renderTable();
    await waitFor(() => {
      // applicationId=1 的行显示应用名 + ID
      expect(screen.getByText('测试应用 (1)')).toBeInTheDocument();
      // applicationId=null 的行显示「未绑定」
      expect(screen.getByText('未绑定')).toBeInTheDocument();
    });
  });

  it('创建表单 Application 必填：未选应用直接提交显示校验提示，不调用 createMutation', async () => {
    renderTable();
    // 等待表格加载后点击「创建 API Key」按钮打开 Modal
    // 注：antd Button 带 PlusOutlined icon，accessible name 含 icon 的 aria-label，用正则子串匹配
    const openBtn = await screen.findByRole('button', { name: /创建 API Key/ });
    await userEvent.click(openBtn);

    // 等待 Modal 打开：创建表单内 Application Select 的 placeholder 文本出现
    // 注：antd Select 的 placeholder 渲染在 .ant-select-selection-placeholder 内（非 input placeholder）
    await waitFor(() => {
      expect(screen.getByText('搜索并选择应用')).toBeInTheDocument();
    });

    // 点击 Modal footer 的 OK 按钮触发提交
    // 注：antd Modal footer OK 按钮在 .ant-modal-footer 内，用 querySelector 定位更稳定
    const okButton = document.querySelector('.ant-modal-footer .ant-btn-primary') as HTMLElement;
    expect(okButton).toBeTruthy();
    await userEvent.click(okButton);

    // 应显示 Application 必填校验提示
    await waitFor(() => {
      expect(screen.getByText('请选择应用')).toBeInTheDocument();
    });
    // createMutation 不应被调用
    expect(mockCreate).not.toHaveBeenCalled();
  });
});
