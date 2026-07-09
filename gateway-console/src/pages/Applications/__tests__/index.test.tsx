// Task 12：Applications 页查看 Key 入口 + 删除冲突提示单元测试
//
// 验证 tasks.md 6.1 + 6.2 核心点：
// 1) 行操作「查看 Key」按钮点击后跳转 /keys?applicationId=<id>（触发 DownstreamKeysTable 初始筛选）
// 2) 删除应用遇后端 APPLICATION_HAS_API_KEYS（有 Key 引用）时，message.error 展示后端冲突信息
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { I18nextProvider } from 'react-i18next';
import { MemoryRouter } from 'react-router-dom';
import i18n from '@/i18n';
import ApplicationsPage from '@/pages/Applications';
import type { Application } from '@/types/application';

// vi.hoisted 提升 mock 引用，保证 vi.mock 工厂能访问
const {
  mockApplications,
  mockDelete,
  mockCreate,
  mockUpdate,
  mockNavigate,
} = vi.hoisted(() => ({
  mockApplications: vi.fn(),
  mockDelete: vi.fn().mockResolvedValue(undefined),
  mockCreate: vi.fn().mockResolvedValue({ id: 1 }),
  mockUpdate: vi.fn().mockResolvedValue(undefined),
  mockNavigate: vi.fn(),
}));

// mock useNavigate，断言跳转目标
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

// mock TanStack Query hooks：ApplicationsPage 与 ApplicationFormModal 均会调用
vi.mock('@/services/query/useApplications', () => ({
  useApplications: () => ({ data: mockApplications(), isLoading: false }),
  useDeleteApplication: () => ({ mutateAsync: mockDelete }),
  useCreateApplication: () => ({ mutateAsync: mockCreate, isPending: false }),
  useUpdateApplication: () => ({ mutateAsync: mockUpdate, isPending: false }),
}));

// mock authStore：给足 APPLICATION_WRITE 权限，确保删除按钮渲染
vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({ hasPermission: () => true }),
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
  mockApplications.mockReturnValue([testApplication]);
  mockDelete.mockReset();
  mockDelete.mockResolvedValue(undefined);
  mockNavigate.mockClear();
});

/** 渲染页面并提供 Router + i18n + antd App 上下文（App.useApp().message 依赖 AntApp 包裹） */
function renderPage() {
  return render(
    <MemoryRouter>
      <I18nextProvider i18n={i18n}>
        <AntApp>
          <ApplicationsPage />
        </AntApp>
      </I18nextProvider>
    </MemoryRouter>,
  );
}

/** 定位行内删除按钮：现有删除按钮为 icon-only（无 aria-label），用 antd danger class 稳定定位 */
function getDeleteButton(): HTMLElement {
  const btn = document.querySelector('button.ant-btn-dangerous');
  if (!btn) throw new Error('未找到删除按钮（.ant-btn-dangerous）');
  return btn as HTMLElement;
}

/** 点击 Popconfirm 的确认按钮 */
async function confirmDelete() {
  const confirmBtn = await screen.findByRole('button', { name: /^确\s*定$|^OK$/i });
  await userEvent.click(confirmBtn);
}

/** 构造类 AxiosError 对象：后端 400 + ApiResponse{success,error:{code,message}} */
function axiosLike400(code: string, message: string) {
  return {
    isAxiosError: true,
    response: {
      status: 400,
      data: { success: false, error: { code, message } },
    },
  };
}

describe('Applications 页 Task 12 改造', () => {
  it('行操作「查看 Key」按钮点击后跳转 /keys?applicationId=<id>', async () => {
    renderPage();
    // 等待表格渲染出应用名
    await waitFor(() => {
      expect(screen.getByText('测试应用')).toBeInTheDocument();
    });
    // 定位「查看 Key」按钮（aria-label 提供可访问名）
    const viewKeysBtn = screen.getByRole('button', { name: '查看 Key' });
    await userEvent.click(viewKeysBtn);
    expect(mockNavigate).toHaveBeenCalledWith('/keys?applicationId=1');
  });

  it('删除遇 APPLICATION_HAS_API_KEYS 时展示后端冲突信息', async () => {
    mockDelete.mockRejectedValueOnce(
      axiosLike400(
        'APPLICATION_HAS_API_KEYS',
        '应用下还有 API Key，请先转移或删除',
      ),
    );
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('测试应用')).toBeInTheDocument();
    });
    await userEvent.click(getDeleteButton());
    await confirmDelete();

    // message.error 渲染到 DOM（AntApp 提供 message 上下文）
    await waitFor(() => {
      expect(screen.getByText('应用下还有 API Key，请先转移或删除')).toBeInTheDocument();
    });
  });

  it('删除成功显示成功提示', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('测试应用')).toBeInTheDocument();
    });
    await userEvent.click(getDeleteButton());
    await confirmDelete();

    await waitFor(() => {
      expect(screen.getByText('应用已删除')).toBeInTheDocument();
    });
  });
});
