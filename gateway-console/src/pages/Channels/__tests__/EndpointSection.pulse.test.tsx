// EndpointSection 保存反馈脉冲——测试套件
//
// Mock 策略：与第 5 章保持一致，通过 vi.mock('@/services/api/channel') 在最底层 stub channelApi。
// 不引入 MSW。每个用例独立设置 mock 行为（成功 / 失败）。
//
// 验证目标：
// 1) 保存成功后，对应行容器应显示 ✓ 已保存（save-tip-ok）
// 2) 保存失败应显示 ✗ 错误（save-tip-err），且行容器 className 含 save-pulse-error
// 3) 乐观更新失败应回滚到上一个值（旧 URL 仍在 DOM 中可见）
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import i18n from '@/i18n';

// jsdom matchMedia / ResizeObserver stub（AntD 组件挂载所需）
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

// 类 AxiosError 工厂
const axiosLike500 = (msg: string) => ({
  isAxiosError: true,
  message: 'Request failed with status code 500',
  response: {
    status: 500,
    statusText: 'Internal Server Error',
    data: { message: msg },
  },
});

// 用 mock factory + 暴露的可变 fn 引用，让单测灵活切换成功 / 失败
const updateEndpointMock = vi.fn();
vi.mock('@/services/api/channel', () => {
  return {
    channelApi: {
      addEndpoint: vi.fn(() => Promise.resolve()),
      updateEndpoint: (...args: unknown[]) => updateEndpointMock(...args),
      removeEndpoint: vi.fn(() => Promise.resolve()),
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
      <QueryClientProvider client={client}>{ui}</QueryClientProvider>
    </I18nextProvider>
  );
}

const sampleEndpoint = {
  id: 10,
  channelId: 1,
  protocol: 'openai',
  endpointUrl: 'https://api.example.com/v1',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('EndpointSection 保存反馈脉冲', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    updateEndpointMock.mockReset();
  });

  it('保存成功后行尾应出现 ✓ 已保存（save-tip-ok）', async () => {
    updateEndpointMock.mockResolvedValue({
      ...sampleEndpoint,
      endpointUrl: 'https://api.example.com/v2',
    });

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <EndpointSection channelId={1} endpoints={[sampleEndpoint as never]} />
    );

    await user.click(screen.getByRole('button', { name: /edit/i }));
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      expect(updateEndpointMock).toHaveBeenCalled();
    });

    // 行尾出现 save-tip-ok
    await waitFor(() => {
      const tip = container.querySelector('.save-tip-ok');
      expect(tip).not.toBeNull();
      expect(tip?.textContent || '').toMatch(/已保存/);
    });
  });

  it('保存失败应出现 ✗ 错误且行加红框（save-pulse-error）', async () => {
    updateEndpointMock.mockRejectedValue(axiosLike500('endpoint update boom'));

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <EndpointSection channelId={1} endpoints={[sampleEndpoint as never]} />
    );

    await user.click(screen.getByRole('button', { name: /edit/i }));
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      expect(updateEndpointMock).toHaveBeenCalled();
    });

    // save-tip-err 应包含错误原因
    await waitFor(() => {
      const errTip = container.querySelector('.save-tip-err');
      expect(errTip).not.toBeNull();
      expect(errTip?.textContent || '').toMatch(/endpoint update boom/);
    });
    // 行容器 className 应含 save-pulse-error
    expect(container.querySelector('.save-pulse-error')).not.toBeNull();
  });

  it('乐观更新失败应回滚到上一个值（旧 URL 仍在 DOM）', async () => {
    updateEndpointMock.mockRejectedValue(axiosLike500('endpoint update boom'));

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <EndpointSection channelId={1} endpoints={[sampleEndpoint as never]} />
    );

    await user.click(screen.getByRole('button', { name: /edit/i }));
    // 输入新的 URL
    const input = container.querySelector('input[id$="endpointUrl"]') as HTMLInputElement;
    expect(input).not.toBeNull();
    await user.clear(input);
    await user.type(input, 'https://api.example.com/changed');
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      expect(updateEndpointMock).toHaveBeenCalled();
    });

    // 失败后应回滚：原始 URL 应仍可见，新 URL 不应作为展示态出现
    await waitFor(() => {
      // 退出编辑态后，原 URL 应在展示文本中
      const monoSpans = within(container).queryAllByText('https://api.example.com/v1');
      expect(monoSpans.length).toBeGreaterThan(0);
    });
  });
});

export {};
