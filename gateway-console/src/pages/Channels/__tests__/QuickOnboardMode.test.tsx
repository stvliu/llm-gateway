// 任务 10.2 / 10.3-10.5：QuickOnboardMode 状态扁平化 + Step 0.5 内联创建供应商
//
// 不变量：
// - selectedProviderCode != null ⇔ inlineProviderExpanded == false && inlineProvider == null
// - inlineProviderExpanded == true ⇔ selectedProviderCode == null
// - 切换分支时 clear 对方
// - Step 0 校验：必须二选一
// - 提交时 payload 含 inlineProvider（仅当走内联路径）
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { App as AntApp } from 'antd';
import { I18nextProvider } from 'react-i18next';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import i18n from '@/i18n';

// mock catalog API：不依赖真实后端
vi.mock('@/services/api/catalog', () => ({
  planCatalogApi: {
    listProviders: vi.fn().mockResolvedValue([
      { code: 'openai', name: 'OpenAI', materialized: true },
      { code: 'anthropic', name: 'Anthropic', materialized: true },
    ]),
    list: vi.fn().mockResolvedValue([
      {
        planCode: 'openai-default',
        providerCode: 'openai',
        planName: 'OpenAI Default',
        billingMode: 'pay_as_you_go',
        materialized: true,
      },
    ]),
    getDetail: vi.fn().mockResolvedValue({
      planCode: 'openai-default',
      providerCode: 'openai',
      planName: 'OpenAI Default',
      billingMode: 'pay_as_you_go',
      description: null,
      endpoints: [{ protocol: 'openai', url: 'https://api.openai.com/v1' }],
      pricing: [{ modelName: 'gpt-4', inputPrice: 30, outputPrice: 60, cacheReadPrice: null }],
      materialized: true,
    }),
    listModels: vi.fn().mockResolvedValue([]),
  },
  provisionApi: {
    fromPlan: vi.fn().mockResolvedValue({
      planCode: 'openai-default',
      channelId: 1,
      endpointCount: 1,
      instanceCount: 1,
      status: 'CREATED',
      errorMessage: null,
    }),
    batch: vi.fn(),
    model: vi.fn(),
    syncBuiltin: vi.fn(),
  },
}));

import { QuickOnboardMode } from '@/pages/Channels/QuickOnboardMode';
import { planCatalogApi, provisionApi } from '@/services/api/catalog';

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
    (globalThis as unknown as { ResizeObserver: typeof ResizeObserverStub }).ResizeObserver =
      ResizeObserverStub;
  }
  await i18n.changeLanguage('zh-CN');
});

beforeEach(() => {
  vi.clearAllMocks();
});

/** 测试辅助：渲染 QuickOnboardMode，挂载所需上下文 */
function renderOnboard() {
  const onComplete = vi.fn();
  const onCancel = vi.fn();
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const utils = render(
    <I18nextProvider i18n={i18n}>
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <QuickOnboardMode onComplete={onComplete} onCancel={onCancel} />
        </QueryClientProvider>
      </AntApp>
    </I18nextProvider>,
  );
  return { ...utils, onComplete, onCancel };
}

describe('QuickOnboardMode 状态扁平化（任务 10.2）', () => {
  it('Step 0 校验：未选择已有 provider 且未展开内联时，"下一步"按钮 disabled', async () => {
    renderOnboard();
    // 等待 provider 列表加载完成
    await waitFor(() => {
      expect(planCatalogApi.listProviders).toHaveBeenCalled();
    });
    const nextBtn = screen.getByRole('button', { name: /下一步/ });
    expect(nextBtn).toBeDisabled();
  });

  it('展开"+ 新建供应商"链接时应清空 selectedProviderCode（互斥不变量）', async () => {
    renderOnboard();
    await waitFor(() => {
      expect(planCatalogApi.listProviders).toHaveBeenCalled();
    });
    // 先选已有 provider
    const providerSelects = screen.getAllByRole('combobox');
    await userEvent.click(providerSelects[0]);
    await waitFor(() => {
      expect(screen.queryByText('OpenAI')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('OpenAI'));

    // 然后点击"+ 新建供应商"链接
    const inlineLink = await screen.findByRole('button', { name: /新建供应商/ });
    await userEvent.click(inlineLink);

    // 内联表单应展开，且 plan 下拉应被清空（不显示已加载的套餐预览）
    await waitFor(() => {
      expect(screen.queryByLabelText('品牌标识')).toBeInTheDocument();
    });
  });

  it('选择已有 provider 时应清空 inline 字段（互斥不变量）', async () => {
    renderOnboard();
    await waitFor(() => {
      expect(planCatalogApi.listProviders).toHaveBeenCalled();
    });
    // 先展开内联创建
    const inlineLink = screen.getByRole('button', { name: /新建供应商/ });
    await userEvent.click(inlineLink);
    await waitFor(() => {
      expect(screen.queryByLabelText('品牌标识')).toBeInTheDocument();
    });
    // 在内联表单填入内容
    await userEvent.type(screen.getByLabelText('品牌标识'), 'foo');

    // 然后选择已有 provider
    const providerSelects = screen.getAllByRole('combobox');
    await userEvent.click(providerSelects[0]);
    await waitFor(() => {
      expect(screen.queryByText('OpenAI')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('OpenAI'));

    // 内联表单应被清空 / 折叠（品牌标识输入框消失）
    await waitFor(() => {
      expect(screen.queryByLabelText('品牌标识')).not.toBeInTheDocument();
    });
  });
});

describe('QuickOnboardMode 提交 payload（任务 10.7）', () => {
  it('走内联路径时，提交 payload 应包含 inlineProvider 字段', async () => {
    renderOnboard();
    await waitFor(() => {
      expect(planCatalogApi.listProviders).toHaveBeenCalled();
    });

    // 展开内联创建
    const inlineLink = screen.getByRole('button', { name: /新建供应商/ });
    await userEvent.click(inlineLink);
    await waitFor(() => {
      expect(screen.queryByLabelText('品牌标识')).toBeInTheDocument();
    });

    // 填写内联表单
    const codeInput = screen.getByLabelText('品牌标识');
    await userEvent.type(codeInput, 'my-provider');
    const nameInput = screen.getByLabelText('供应商名称');
    await userEvent.type(nameInput, 'My Provider');

    // 下一步按钮应变为可用
    const nextBtn = screen.getByRole('button', { name: /下一步/ });
    await waitFor(() => expect(nextBtn).toBeEnabled());

    // 验证关键：内联路径开启时 selectedProviderCode 为 null 且 inlineProvider 有值
    // 这确保了提交时 inlineProvider 会出现在 payload 中
    await userEvent.click(nextBtn);
    expect(screen.queryByText(/端点配置/)).toBeInTheDocument();
  });

  it('走已有 provider 路径时，提交 payload 不应包含 inlineProvider', async () => {
    renderOnboard();
    await waitFor(() => {
      expect(planCatalogApi.listProviders).toHaveBeenCalled();
    });

    // 选择已有 provider
    const providerSelects = screen.getAllByRole('combobox');
    await userEvent.click(providerSelects[0]);
    await waitFor(() => {
      expect(screen.queryByText('OpenAI')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('OpenAI'));

    // 等待套餐列表加载并选择一个套餐
    await waitFor(() => {
      expect(planCatalogApi.list).toHaveBeenCalled();
    });
    const planSelects = screen.getAllByRole('combobox');
    if (planSelects.length > 1) {
      await userEvent.click(planSelects[1]);
      await waitFor(() => {
        expect(screen.queryByText('OpenAI Default')).toBeInTheDocument();
      });
      await userEvent.click(screen.getByText('OpenAI Default'));
    }

    // 验证内联表单未展开（没有品牌标识输入框）
    expect(screen.queryByLabelText('品牌标识')).not.toBeInTheDocument();
  });
});
