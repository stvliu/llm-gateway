// QuotaSettingsSection 保存反馈脉冲——测试套件
//
// QuotaSettings 是"编辑模式 + 批量提交"，无乐观更新需求。
// 仅在保存成功 / 失败时对编辑区容器触发同款脉冲。
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
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

const axiosLike500 = (msg: string) => ({
  isAxiosError: true,
  message: 'Request failed with status code 500',
  response: {
    status: 500,
    statusText: 'Internal Server Error',
    data: { message: msg },
  },
});

const updateChannelMock = vi.fn();
vi.mock('@/services/api/channel', () => {
  return {
    channelApi: {
      update: (...args: unknown[]) => updateChannelMock(...args),
      list: vi.fn(() => Promise.resolve([])),
      get: vi.fn(() => Promise.resolve({})),
      listCredentials: vi.fn(() => Promise.resolve([])),
      listModels: vi.fn(() => Promise.resolve([])),
    },
  };
});

import { QuotaSettingsSection } from '../QuotaSettingsSection';

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

const sampleChannel = {
  id: 1,
  providerId: 1,
  name: 'demo channel',
  state: 'ACTIVE',
  quotaLimit: 1000,
  timeout: 30000,
  maxRetries: 2,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

describe('QuotaSettingsSection 保存反馈脉冲', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    updateChannelMock.mockReset();
  });

  it('保存成功后编辑区容器应出现 ✓ 已保存（save-tip-ok）', async () => {
    updateChannelMock.mockResolvedValue({ ...sampleChannel });

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <QuotaSettingsSection channel={sampleChannel as never} />
    );

    // 进入编辑模式
    await user.click(screen.getByRole('button', { name: /编辑设置/ }));
    // 触发保存
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      expect(updateChannelMock).toHaveBeenCalled();
    });
    await waitFor(() => {
      const tip = container.querySelector('.save-tip-ok');
      expect(tip).not.toBeNull();
      expect(tip?.textContent || '').toMatch(/已保存/);
    });
  });

  it('保存失败应出现 ✗ 错误且编辑区加红框（save-pulse-error）', async () => {
    updateChannelMock.mockRejectedValue(axiosLike500('channel update boom'));

    const user = userEvent.setup();
    const { container } = renderWithProviders(
      <QuotaSettingsSection channel={sampleChannel as never} />
    );

    await user.click(screen.getByRole('button', { name: /编辑设置/ }));
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => {
      expect(updateChannelMock).toHaveBeenCalled();
    });
    await waitFor(() => {
      const errTip = container.querySelector('.save-tip-err');
      expect(errTip).not.toBeNull();
      expect(errTip?.textContent || '').toMatch(/channel update boom/);
    });
    expect(container.querySelector('.save-pulse-error')).not.toBeNull();
  });
});

export {};
