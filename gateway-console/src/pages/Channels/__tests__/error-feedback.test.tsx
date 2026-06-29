// Channels Section 错误反馈兜底——测试套件
//
// Mock 策略（选 A）：
// 用 vi.mock('@/services/api/channel') 在最底层 stub channelApi，让 mutation 抛 AxiosError 形态的 500 错误。
// 之所以避开 MSW：第 4 章测试栈未安装 MSW；vi.mock 已能干净地把后端调用拦在 channelApi 边界，
// 不需要起 service worker，对组件的 React Query mutation 透明。
//
// 测试目标：每个 Section 在 mutation 失败时必须经由 message.error 输出错误反馈，
// 文案至少包含 "保存失败"（i18n: common.message.saveFailed）。
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { message } from 'antd';
import i18n from '@/i18n';

// jsdom 不实现 matchMedia，AntD Grid 在挂载时会调用，需要在挂载组件前补 stub
beforeAll(() => {
  // useDangerConfirm 在 onOk 抛错时会 re-throw 以阻止 modal 关闭，
  // antd 内部把它转为 unhandled rejection。本套件只关心 errorSpy 被调，
  // 故全局静默 axios 500 的 unhandled rejection，避免误判为退化。
  const originalHandler = process.listeners('unhandledRejection').slice();
  process.on('unhandledRejection', (reason: unknown) => {
    const r = reason as { isAxiosError?: boolean };
    if (r && r.isAxiosError) return; // 吃掉 axios 500
    // 其它意外仍交回原 handler 链
    for (const h of originalHandler) (h as (e: unknown) => void)(reason);
  });
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
    (globalThis as unknown as { ResizeObserver: typeof ResizeObserverStub }).ResizeObserver = ResizeObserverStub;
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
import { ModelMappingSection } from '../ModelMappingSection';
import { QuotaSettingsSection } from '../QuotaSettingsSection';

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

    // 进入编辑模式（antd v6 图标按钮 aria-label 为英文 edit）
    await user.click(screen.getByRole('button', { name: /edit/i }));
    // 触发保存（表单已有合法 url，提交将走到 mutateAsync 并被 mock 拒绝）
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalled();
    });
    // 断言文案包含"保存失败"或后端原因
    const calls = errorSpy.mock.calls.map((c: unknown[]) => String(c[0]));
    expect(
      calls.some((m: string) => m.includes('保存失败') && m.includes('endpoint update boom'))
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

    // InlineEditableList 的删除按钮 + useDangerConfirm 二次确认
    // antd v6 行内图标按钮 aria-label 为英文 delete；Modal OK 文案为中文"删除"
    const deleteBtns = screen.getAllByRole('button', { name: /delete/i });
    await user.click(deleteBtns[0]);
    // Modal 弹出后，footer 的 OK 按钮文案为 common.actions.delete = "删除"
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalled();
    });
    const calls = errorSpy.mock.calls.map((c: unknown[]) => String(c[0]));
    expect(calls.some((m: string) => m.includes('cred delete boom'))).toBe(true);
  });
});

describe('ModelMappingSection 错误反馈', () => {
  let errorSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    errorSpy = vi.spyOn(message, 'error').mockImplementation(() => ({} as never));
  });

  it('删除模型映射失败时应弹出含具体后端原因的 message.error', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ModelMappingSection
        channelId={1}
        channelModels={[
          {
            id: 200,
            channelId: 1,
            modelId: 1,
            modelName: 'gpt-4o',
            upstreamModelName: 'gpt-4o',
            state: 'ACTIVE',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          } as never,
        ]}
      />
    );

    // antd v6 行内图标按钮 aria-label 为英文 delete；Modal OK 文案为中文"删除"
    const deleteBtns = screen.getAllByRole('button', { name: /delete/i });
    await user.click(deleteBtns[0]);
    // useDangerConfirm OK 按钮文案为 common.actions.delete = "删除"
    const dangerOk = await screen.findByRole('button', { name: /^删\s*除$/ });
    await user.click(dangerOk);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalled();
    });
    const calls = errorSpy.mock.calls.map((c: unknown[]) => String(c[0]));
    expect(calls.some((m: string) => m.includes('model delete boom'))).toBe(true);
  });
});

describe('QuotaSettingsSection 错误反馈', () => {
  let errorSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    errorSpy = vi.spyOn(message, 'error').mockImplementation(() => ({} as never));
  });

  it('保存配额设置失败时应弹出含具体后端原因的 message.error', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <QuotaSettingsSection
        channel={{
          id: 1,
          providerId: 1,
          name: 'demo channel',
          state: 'ACTIVE',
          quotaLimit: 1000,
          timeout: 30000,
          maxRetries: 2,
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        } as never}
      />
    );

    // 进入编辑模式（antd v6 图标按钮 aria-label 为英文 edit）
    await user.click(screen.getByRole('button', { name: /edit/i }));
    // 触发保存
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalled();
    });
    const calls = errorSpy.mock.calls.map((c: unknown[]) => String(c[0]));
    expect(calls.some((m: string) => m.includes('channel update boom'))).toBe(true);
  });
});

// 让该文件被识别为模块
export {};
