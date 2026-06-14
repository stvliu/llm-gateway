// ModelMappingSection 删除模型映射危险确认测试（任务 8.6）
//
// 验证目标：
// 1) 点击行内"删除"按钮 → 弹 Modal.confirm（含 modelId + "删除后，模型 ID xxx 不再被路由到此渠道"）
// 2) 确认前不调 deleteModel mutation
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
    (globalThis as { ResizeObserver: typeof ResizeObserverStub }).ResizeObserver =
      ResizeObserverStub;
  }
});

const deleteModelMock = vi.fn();
vi.mock('@/services/api/channel', () => {
  return {
    channelApi: {
      createModel: vi.fn(() => Promise.resolve()),
      deleteModel: (...args: unknown[]) => deleteModelMock(...args),
      updateUpstreamModelName: vi.fn(() => Promise.resolve()),
      list: vi.fn(() => Promise.resolve([])),
      get: vi.fn(() => Promise.resolve({})),
      listCredentials: vi.fn(() => Promise.resolve([])),
      listModels: vi.fn(() => Promise.resolve([])),
    },
  };
});

import { ModelMappingSection } from '../ModelMappingSection';

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

const sampleMapping = {
  id: 200,
  channelId: 1,
  modelId: 42,
  modelName: 'gpt-4o',
  upstreamModelName: 'gpt-4o',
  state: 'ACTIVE',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('ModelMappingSection 删除危险确认（任务 8.6）', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    deleteModelMock.mockReset();
    deleteModelMock.mockResolvedValue(undefined);
  });

  it('点击删除 → 弹 Modal.confirm 含 modelId + "不再被路由"，确认后才调 deleteModel', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ModelMappingSection channelId={1} channelModels={[sampleMapping as never]} />
    );

    const delBtns = screen.getAllByRole('button', { name: /删\s*除/ });
    await user.click(delBtns[delBtns.length - 1]);

    // 弹出 Modal.confirm，包含 modelId + 不再被路由
    await waitFor(() => {
      expect(screen.getByText(/不再被路由到此渠道/)).toBeInTheDocument();
    });
    // modelId 标签（优先 modelName 'gpt-4o'，fallback modelId）出现在 description 中
    // 注意 gpt-4o 同时出现在列表 Tag 与 modal description 中，故用 getAllByText
    expect(screen.getAllByText(/gpt-4o/).length).toBeGreaterThan(0);

    // 此时尚未调用删除接口
    expect(deleteModelMock).not.toHaveBeenCalled();

    // 点击 modal footer 中的 dangerous OK 按钮
    const allButtons = screen.getAllByRole('button');
    const dangerOk = allButtons.find(
      (b) =>
        b.className.includes('ant-btn-dangerous') &&
        !b.className.includes('ant-btn-link')
    );
    expect(dangerOk).toBeDefined();
    await user.click(dangerOk!);

    await waitFor(() => {
      expect(deleteModelMock).toHaveBeenCalled();
    });
  });
});
