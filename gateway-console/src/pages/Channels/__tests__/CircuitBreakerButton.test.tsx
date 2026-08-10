/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// CircuitBreakerButton 端点熔断应急 UI 测试（任务 4.11b / 批次4 Task 11）
//
// 验证目标：
// 1) 三种熔断状态（CLOSED/OPEN/HALF_OPEN）正确渲染 Tag 文案与颜色
// 2) CLOSED/HALF_OPEN 显示一键熔断按钮（cb-force-open-{id}）；OPEN 显示一键恢复按钮（cb-force-close-{id}）
// 3) 点击应急按钮 → 弹 Popconfirm 二次确认 → 确认前未调 API → 确认后调用对应 mutation
// 4) channelId/endpointId 正确透传给 API
//
// Mock 策略：与 EndpointSection.delete.test 一致，vi.mock('@/pages/resilience/api')
// 在最底层 stub resilienceApi.circuitBreaker.*，让 useCircuitBreakerState/useForceOpen|CloseCircuitBreaker
// 走 mock 路径。不引入 MSW。
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { App as AntApp } from 'antd';
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

// 用 mock factory + 暴露的可变 fn 引用，让单测灵活切换状态/成功/失败
const forceOpenMock = vi.fn();
const forceCloseMock = vi.fn();
const getStateMock = vi.fn();
vi.mock('@/pages/resilience/api', () => ({
  resilienceApi: {
    circuitBreaker: {
      forceOpen: (...args: unknown[]) => forceOpenMock(...args),
      forceClose: (...args: unknown[]) => forceCloseMock(...args),
      getState: (...args: unknown[]) => getStateMock(...args),
    },
    events: {
      list: vi.fn(() => Promise.resolve([])),
      exhausted: vi.fn(() => Promise.resolve([])),
    },
  },
}));

import { CircuitBreakerButton } from '../CircuitBreakerButton';

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      // 关闭 retry 与轮询，避免测试用例间相互干扰
      queries: { retry: false, refetchInterval: false },
      mutations: { retry: false },
    },
  });
}

function renderWithProviders(ui: React.ReactElement) {
  const client = makeQueryClient();
  return render(
    <I18nextProvider i18n={i18n}>
      <QueryClientProvider client={client}>
        {/* CircuitBreakerButton 用 App.useApp() 取 message，必须包 AntApp */}
        <AntApp>{ui}</AntApp>
      </QueryClientProvider>
    </I18nextProvider>
  );
}

describe('CircuitBreakerButton 端点熔断应急 UI（任务 4.11b / Task 11）', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('zh-CN');
    forceOpenMock.mockReset();
    forceCloseMock.mockReset();
    getStateMock.mockReset();
  });

  it('CLOSED 状态：绿色 Tag "正常放行" + 显示一键熔断按钮，不显示一键恢复', async () => {
    getStateMock.mockResolvedValue({ state: 'CLOSED' });
    renderWithProviders(<CircuitBreakerButton channelId={1} endpointId={10} />);

    // 等待 query 解析并透传 channelId/endpointId
    await waitFor(() => {
      expect(getStateMock).toHaveBeenCalledWith(1, 10);
    });

    // Tag 文案 "正常放行"
    expect(screen.getByText('正常放行')).toBeInTheDocument();
    // forceOpen 按钮存在（CLOSED 时 !isOpen=true 显示）
    expect(screen.getByTestId('cb-force-open-10')).toBeInTheDocument();
    // forceClose 按钮不存在
    expect(screen.queryByTestId('cb-force-close-10')).not.toBeInTheDocument();
  });

  it('OPEN 状态：红色 Tag "熔断开启" + 显示一键恢复按钮，不显示一键熔断', async () => {
    getStateMock.mockResolvedValue({ state: 'OPEN' });
    renderWithProviders(<CircuitBreakerButton channelId={1} endpointId={10} />);

    await waitFor(() => {
      expect(screen.getByText('熔断开启')).toBeInTheDocument();
    });
    expect(screen.getByTestId('cb-force-close-10')).toBeInTheDocument();
    expect(screen.queryByTestId('cb-force-open-10')).not.toBeInTheDocument();
  });

  it('HALF_OPEN 状态：橙色 Tag "试探放行" + 显示一键熔断按钮', async () => {
    getStateMock.mockResolvedValue({ state: 'HALF_OPEN' });
    renderWithProviders(<CircuitBreakerButton channelId={1} endpointId={10} />);

    await waitFor(() => {
      expect(screen.getByText('试探放行')).toBeInTheDocument();
    });
    expect(screen.getByTestId('cb-force-open-10')).toBeInTheDocument();
    expect(screen.queryByTestId('cb-force-close-10')).not.toBeInTheDocument();
  });

  it('点击一键熔断 → 弹 Popconfirm 含"确定要强制熔断" → 确认前未调 API → 确认后调 forceOpen 透传 cid/eid', async () => {
    const user = userEvent.setup();
    getStateMock.mockResolvedValue({ state: 'CLOSED' });
    forceOpenMock.mockResolvedValue({ state: 'OPEN' });
    renderWithProviders(<CircuitBreakerButton channelId={1} endpointId={10} />);

    await waitFor(() => {
      expect(screen.getByTestId('cb-force-open-10')).toBeInTheDocument();
    });

    // 点击触发 Popconfirm
    await user.click(screen.getByTestId('cb-force-open-10'));

    // 弹出确认弹层，含二次确认文案
    await waitFor(() => {
      expect(screen.getByText(/确定要强制熔断该端点/)).toBeInTheDocument();
    });

    // 确认前未调用 API
    expect(forceOpenMock).not.toHaveBeenCalled();

    // 点击 Popconfirm 确认按钮（antd 默认 okText="OK"）
    const okBtn = await screen.findByRole('button', { name: /^OK$/i });
    await user.click(okBtn);

    // 确认后调用 forceOpen，透传 channelId/endpointId
    await waitFor(() => {
      expect(forceOpenMock).toHaveBeenCalledWith(1, 10);
    });
  });

  it('点击一键恢复 → 弹 Popconfirm 含"确定要强制恢复" → 确认后调 forceClose 透传 cid/eid', async () => {
    const user = userEvent.setup();
    getStateMock.mockResolvedValue({ state: 'OPEN' });
    forceCloseMock.mockResolvedValue({ state: 'CLOSED' });
    renderWithProviders(<CircuitBreakerButton channelId={1} endpointId={10} />);

    await waitFor(() => {
      expect(screen.getByTestId('cb-force-close-10')).toBeInTheDocument();
    });

    await user.click(screen.getByTestId('cb-force-close-10'));

    await waitFor(() => {
      expect(screen.getByText(/确定要强制恢复该端点/)).toBeInTheDocument();
    });

    expect(forceCloseMock).not.toHaveBeenCalled();

    const okBtn = await screen.findByRole('button', { name: /^OK$/i });
    await user.click(okBtn);

    await waitFor(() => {
      expect(forceCloseMock).toHaveBeenCalledWith(1, 10);
    });
  });
});
