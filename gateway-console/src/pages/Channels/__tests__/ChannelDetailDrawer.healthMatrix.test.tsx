// 任务 9.5 / 9.6：详情抽屉矩阵 Table + AbortController 取消语义
//
// 验证目标：
// 1) 点击抽屉 extra 中"连通性测试"按钮 → 调用 channelApi.healthCheck(channelId, 'DRAWER', config)
//    config.signal 应为 AbortController 的 signal
// 2) 返回 matrix 后，应渲染 Table（行=Key，列：脱敏Key/认证/可用模型/延迟/错误）
// 3) 关闭抽屉触发 onClose 时，AbortController.abort 应被调用
//
// mock 策略与第 5/7/8 章一致：vi.mock channelApi
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, vi, beforeEach } from 'vitest';
import { App as AntApp } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import i18n from '@/i18n';
import type { ChannelCard as ChannelCardType, ChannelHealthCheckResponse } from '@/types/channel';

// 必须在 import ChannelDetailDrawer 前 mock
vi.mock('@/services/api/channel', () => ({
  channelApi: {
    list: vi.fn(() => Promise.resolve([])),
    listByProvider: vi.fn(() => Promise.resolve([])),
    get: vi.fn(() => Promise.resolve(null)),
    create: vi.fn(),
    update: vi.fn(),
    transitionState: vi.fn(),
    delete: vi.fn(),
    addEndpoint: vi.fn(),
    removeEndpoint: vi.fn(),
    updateEndpoint: vi.fn(),
    // 至少有 1 个 credential，触发 healthCheck 而非 noCredentials 早返回
    listCredentials: vi.fn(() =>
      Promise.resolve([
        {
          id: 1,
          channelId: 99,
          apiKeyPrefix: 'sk-***',
          apiKeyPlain: 'sk-***wxyz',
          name: 'k1',
          description: null,
          weight: 1,
          priority: 1,
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      ]),
    ),
    createCredential: vi.fn(),
    updateCredential: vi.fn(),
    deleteCredential: vi.fn(),
    testCredential: vi.fn(),
    listModels: vi.fn(() => Promise.resolve([])),
    createModel: vi.fn(),
    deleteModel: vi.fn(),
    updateUpstreamModelName: vi.fn(),
    transitionModelState: vi.fn(),
    healthCheck: vi.fn(),
  },
}));

// ProviderEditModal 间接依赖 @lobehub 系列，避开
vi.mock('@/pages/Channels/ProviderEditModal', () => ({
  ProviderEditModal: () => null,
}));

vi.mock('@/services/query/useProviders', async () => {
  const actual = await vi.importActual<typeof import('@/services/query/useProviders')>(
    '@/services/query/useProviders',
  );
  return {
    ...actual,
    useProvider: () => ({ data: null }),
  };
});

// 子组件简化为占位，专注 Drawer extra 区域 + 矩阵 Table
vi.mock('@/pages/Channels/EndpointSection', () => ({
  EndpointSection: () => <div>EndpointSectionStub</div>,
}));
vi.mock('@/pages/Channels/CredentialSection', () => ({
  CredentialSection: () => <div>CredentialSectionStub</div>,
}));
vi.mock('@/pages/Channels/ModelMappingSection', () => ({
  ModelMappingSection: () => <div>ModelMappingSectionStub</div>,
}));
vi.mock('@/pages/Channels/QuotaSettingsSection', () => ({
  QuotaSettingsSection: () => <div>QuotaSettingsSectionStub</div>,
}));
vi.mock('@/pages/Channels/ChannelOverviewTab', () => ({
  ChannelOverviewTab: () => <div>OverviewStub</div>,
}));

import { ChannelDetailDrawer } from '@/pages/Channels/ChannelDetailDrawer';
import { channelApi } from '@/services/api/channel';

beforeAll(async () => {
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
    (globalThis as unknown as { ResizeObserver: typeof ResizeObserverStub }).ResizeObserver = ResizeObserverStub;
  }
  await i18n.changeLanguage('zh-CN');
});

beforeEach(() => {
  vi.clearAllMocks();
});

function buildMockChannel(): ChannelCardType {
  return {
    id: 99,
    providerId: 1,
    providerName: 'OpenAI',
    name: 'mock-channel',
    billingMode: 'pay_as_you_go',
    quotaLimit: null,
    priority: 1,
    weight: 1,
    timeout: null,
    maxRetries: null,
    state: 'ACTIVE',
    endpoints: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    stats: {
      endpointCount: 0,
      credentialCount: 0,
      modelCount: 0,
      avgResponseTime: null,
    },
  };
}

function renderDrawer(props: Partial<React.ComponentProps<typeof ChannelDetailDrawer>> = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <AntApp>
        <ChannelDetailDrawer
          channel={buildMockChannel()}
          open={true}
          onClose={() => {}}
          {...props}
        />
      </AntApp>
    </QueryClientProvider>,
  );
}

describe('ChannelDetailDrawer 健康检查矩阵（任务 9.5/9.6）', () => {
  it('点击"连通性测试"按钮 → 渲染矩阵 Table（脱敏Key/认证/可用模型/延迟）', async () => {
    const mockResponse: ChannelHealthCheckResponse = {
      aggregateStatus: 'DEGRADED',
      matrix: [
        {
          credentialId: 1,
          keyMasked: 'sk-***wxyz',
          auth: 'PASS',
          availableModels: ['gpt-4', 'gpt-3.5-turbo'],
          latencyMs: 230,
        },
        {
          credentialId: 2,
          keyMasked: 'sk-***abcd',
          auth: 'FAIL',
          authError: '401',
          latencyMs: null,
        },
      ],
    };
    (channelApi.healthCheck as ReturnType<typeof vi.fn>).mockResolvedValueOnce(mockResponse);

    renderDrawer();

    // 点击 extra 中的"连通性测试"按钮
    const testBtn = await screen.findByRole('button', { name: /api/i });
    await userEvent.click(testBtn);

    // 1) channelApi.healthCheck 应被调用：channelId=99, source='DRAWER'
    await waitFor(() => {
      expect(channelApi.healthCheck).toHaveBeenCalledTimes(1);
    });
    const [calledId, calledSource, calledConfig] = (channelApi.healthCheck as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(calledId).toBe(99);
    expect(calledSource).toBe('DRAWER');
    // 2) 第三参 config 应携带 AbortSignal
    expect(calledConfig).toBeDefined();
    expect(calledConfig.signal).toBeDefined();
    expect(typeof calledConfig.signal.aborted).toBe('boolean');

    // 3) 矩阵 Table 应渲染：脱敏 Key + 延迟 + 错误
    await waitFor(() => {
      expect(screen.getByText('sk-***wxyz')).toBeInTheDocument();
      expect(screen.getByText('sk-***abcd')).toBeInTheDocument();
    });
    expect(screen.getByText(/230\s*ms/)).toBeInTheDocument();
    expect(screen.getByText(/401/)).toBeInTheDocument();
  });

  it('关闭抽屉应中止进行中的 healthCheck 请求（AbortController.abort 被调用）', async () => {
    // healthCheck 返回一个永远 pending 的 Promise，模拟进行中的请求
    let abortObserved = false;
    (channelApi.healthCheck as ReturnType<typeof vi.fn>).mockImplementationOnce((_id, _src, config) => {
      // 监听 signal 的 abort 事件
      config.signal.addEventListener('abort', () => {
        abortObserved = true;
      });
      return new Promise(() => {
        /* never resolves */
      });
    });

    const { rerender } = renderDrawer({ open: true });

    const testBtn = await screen.findByRole('button', { name: /api/i });
    await userEvent.click(testBtn);

    // 等待 healthCheck 被调用
    await waitFor(() => {
      expect(channelApi.healthCheck).toHaveBeenCalledTimes(1);
    });

    // 关闭抽屉（open=false 触发卸载/清理）
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    rerender(
      <QueryClientProvider client={queryClient}>
        <AntApp>
          <ChannelDetailDrawer channel={buildMockChannel()} open={false} onClose={() => {}} />
        </AntApp>
      </QueryClientProvider>,
    );

    // 让 React 处理 effects
    await act(async () => {
      await new Promise((r) => setTimeout(r, 30));
    });

    expect(abortObserved).toBe(true);
  });
});
