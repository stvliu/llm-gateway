// Channels Section 错误反馈兜底——测试套件
//
// Mock 策略（选 A）：
// 用 vi.mock('@/services/api/channel') 在最底层 stub channelApi，让 mutation 抛 AxiosError 形态的 500 错误。
// 之所以避开 MSW：第 4 章测试栈未安装 MSW；vi.mock 已能干净地把后端调用拦在 channelApi 边界，
// 不需要起 service worker，对组件的 React Query mutation 透明。
//
// 测试目标：每个 Section 在 mutation 失败时必须经由 message.error 输出错误反馈，
// 文案至少包含 "保存失败"（i18n: common.message.saveFailed）。
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { message } from 'antd';
import i18n from '@/i18n';

// jsdom 不实现 matchMedia，AntD Grid 在挂载时会调用，需要在挂载组件前补 stub
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
  // jsdom 不实现 ResizeObserver，AntD InlineEditableList 等组件在挂载时使用
  if (!(globalThis as { ResizeObserver?: unknown }).ResizeObserver) {
    class ResizeObserverStub {
      observe(): void {}
      unobserve(): void {}
      disconnect(): void {}
    }
    (globalThis as { ResizeObserver: typeof ResizeObserverStub }).ResizeObserver = ResizeObserverStub;
  }
});

// 在 import 被测组件之前 mock 掉底层 channelApi，
// 这样 useMutation 的 mutationFn 在 await 时会拒绝并触发 onError / catch 分支。
vi.mock('@/services/api/channel', () => {
  // 构造一个类 AxiosError 的对象：含 isAxiosError 标志位 + response.data.message
  const axiosLike500 = (msg: string) => ({
    isAxiosError: true,
    message: 'Request failed with status code 500',
    response: {
      status: 500,
      statusText: 'Internal Server Error',
      data: { message: msg },
    },
  });
  return {
    channelApi: {
      // 端点
      addEndpoint: vi.fn(() => Promise.reject(axiosLike500('endpoint add boom'))),
      updateEndpoint: vi.fn(() => Promise.reject(axiosLike500('endpoint update boom'))),
      removeEndpoint: vi.fn(() => Promise.reject(axiosLike500('endpoint remove boom'))),
      // 凭证
      createCredential: vi.fn(() => Promise.reject(axiosLike500('cred create boom'))),
      updateCredential: vi.fn(() => Promise.reject(axiosLike500('cred update boom'))),
      deleteCredential: vi.fn(() => Promise.reject(axiosLike500('cred delete boom'))),
      testCredential: vi.fn(() => Promise.reject(axiosLike500('cred test boom'))),
      // 模型映射
      createModel: vi.fn(() => Promise.reject(axiosLike500('model create boom'))),
      deleteModel: vi.fn(() => Promise.reject(axiosLike500('model delete boom'))),
      updateUpstreamModelName: vi.fn(() => Promise.reject(axiosLike500('model update boom'))),
      // 渠道更新
      update: vi.fn(() => Promise.reject(axiosLike500('channel update boom'))),
      // 列表 / 详情：不会被本测试触发，但保留以防引用
      list: vi.fn(() => Promise.resolve([])),
      get: vi.fn(() => Promise.resolve({})),
      listCredentials: vi.fn(() => Promise.resolve([])),
      listModels: vi.fn(() => Promise.resolve([])),
    },
  };
});

import { EndpointSection } from '../EndpointSection';
import { CredentialSection } from '../CredentialSection';

/** 创建一个不重试的 QueryClient，避免错误路径被默认重试策略掩盖 */
function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
}

/** 通用 wrapper：注入 QueryClient + i18n */
function renderWithProviders(ui: React.ReactElement) {
  const client = makeQueryClient();
  return render(
    <I18nextProvider i18n={i18n}>
      <QueryClientProvider client={client}>{ui}</QueryClientProvider>
    </I18nextProvider>
  );
}

describe('EndpointSection 错误反馈', () => {
  let errorSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    // 强制使用中文，断言中文文案稳定
    await i18n.changeLanguage('zh-CN');
    // 监听 antd 单例 message.error 的调用
    errorSpy = vi.spyOn(message, 'error').mockImplementation(() => ({} as never));
  });

  it('保存编辑端点失败时应弹出含错误原因的 message.error', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <EndpointSection
        channelId={1}
        endpoints={[
          {
            id: 10,
            channelId: 1,
            protocol: 'openai',
            endpointUrl: 'https://api.example.com/v1',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          } as never,
        ]}
      />
    );

    // 进入编辑模式（AntD 中文模式下两字按钮文本被插入空格，故用宽松匹配）
    await user.click(screen.getByRole('button', { name: /编\s*辑/ }));
    // 触发保存（表单已有合法 url，提交将走到 mutateAsync 并被 mock 拒绝）
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalled();
    });
    // 断言文案包含"保存失败"或后端原因
    const calls = errorSpy.mock.calls.map((c) => String(c[0]));
    expect(
      calls.some((m) => m.includes('保存失败') && m.includes('endpoint update boom'))
    ).toBe(true);
  });
});

describe('CredentialSection 错误反馈', () => {
  let errorSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    errorSpy = vi.spyOn(message, 'error').mockImplementation(() => ({} as never));
  });

  it('删除凭证失败时应弹出含具体后端原因的 message.error', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <CredentialSection
        channelId={1}
        credentials={[
          {
            id: 100,
            channelId: 1,
            apiKeyPrefix: 'sk-abcdef',
            apiKeyPlain: 'sk-abcdefghijk',
            name: '',
            description: null,
            weight: 50,
            priority: 1,
            state: 'ACTIVE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          } as never,
        ]}
      />
    );

    // InlineEditableList 的删除按钮 + Popconfirm 二次确认
    const deleteBtns = screen.getAllByRole('button', { name: /删\s*除/ });
    await user.click(deleteBtns[0]);
    const confirmBtn = await screen.findByRole('button', { name: /确\s*定|OK/i });
    await user.click(confirmBtn);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalled();
    });
    const calls = errorSpy.mock.calls.map((c) => String(c[0]));
    expect(calls.some((m) => m.includes('cred delete boom'))).toBe(true);
  });
});

// 让该文件被识别为模块
export {};
