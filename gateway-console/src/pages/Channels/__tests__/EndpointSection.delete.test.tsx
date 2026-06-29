// EndpointSection 删除端点危险确认测试（任务 8.5）
//
// 验证目标：
// 1) 点击行内"删除"按钮 → 弹 Modal.confirm（含 baseUrl + "删除该端点后，路由到 ... 的流量将立即失败"）
// 2) 确认前不调 removeEndpoint mutation
// 3) 点击 OK 后才调用
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { App as AntApp } from 'antd';
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

const removeEndpointMock = vi.fn();
vi.mock('@/services/api/channel', () => {
  return {
    channelApi: {
      addEndpoint: vi.fn(() => Promise.resolve()),
      updateEndpoint: vi.fn(() => Promise.resolve()),
      removeEndpoint: (...args: unknown[]) => removeEndpointMock(...args),
      list: vi.fn(() => Promise.resolve([])),
      get: vi.fn(() => Promise.resolve({})),
      listCredentials: vi.fn(() => Promise.resolve([])),
      listModels: vi.fn(() => Promise.resolve([])),
    },
  };
});

import { EndpointSection } from '../EndpointSection';

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
      <QueryClientProvider client={client}>
        <AntApp>{ui}</AntApp>
      </QueryClientProvider>
    </I18nextProvider>
  );
}

const sampleEndpoint = {
  id: 1,
  protocol: 'openai',
  endpointUrl: 'https://api.example.com/v1',
};

describe('EndpointSection 删除危险确认（任务 8.5）', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    removeEndpointMock.mockReset();
    removeEndpointMock.mockResolvedValue(undefined);
  });

  it('点击删除 → 弹 Modal.confirm 含 baseUrl + "路由到...流量将立即失败"，确认后才调 removeEndpoint', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <EndpointSection channelId={1} endpoints={[sampleEndpoint as never]} />
    );

    // antd v6：行内删除按钮为 icon-only（DeleteOutlined），accessible name 来自图标 aria-label="delete"（英文）
    const delBtns = screen.getAllByRole('button', { name: /delete/i });
    await user.click(delBtns[delBtns.length - 1]);

    // 弹出 Modal.confirm，包含 baseUrl + 流量将立即失败
    await waitFor(() => {
      expect(screen.getByText(/流量将立即失败/)).toBeInTheDocument();
    });
    expect(screen.getAllByText(/api\.example\.com/).length).toBeGreaterThan(0);

    // 此时尚未调用删除接口
    expect(removeEndpointMock).not.toHaveBeenCalled();

    // 点击 modal footer 中的 OK 按钮（okText="删除"为中文，与行内英文 delete 不冲突）
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);

    await waitFor(() => {
      expect(removeEndpointMock).toHaveBeenCalled();
    });
  });
});
