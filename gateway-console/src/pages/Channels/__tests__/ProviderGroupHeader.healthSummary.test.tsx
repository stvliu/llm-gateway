// 任务 9.4：ProviderGroupHeader N/M 健康聚合
//
// 验证目标：
// - 给 ProviderGroupHeader 传入 channels 数组（含 lastHealthStatus）
// - 应渲染"3/5 健康"小字（i18n key: provider.healthSummary）
// - 0 个渠道时不渲染聚合
import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeAll, vi } from 'vitest';
import i18n from '@/i18n';

// 任务 9.4：ProviderGroupHeader 间接依赖 @lobehub/icons -> @lobehub/ui，
// 后者在 vitest 环境的子目录依赖解析失败；此处 mock 掉 ProviderIcon 以解耦。
vi.mock('@/components/ui', () => ({
  ProviderIcon: ({ providerId }: { providerId: string }) => (
    <span data-testid={`provider-icon-${providerId}`} />
  ),
}));

import { ProviderGroupHeader } from '@/pages/Channels/ProviderGroupHeader';
import type { ChannelCard as ChannelCardType } from '@/types/channel';

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

function buildChannel(id: number, status?: 'HEALTHY' | 'DEGRADED' | 'FAILED' | 'UNKNOWN'): ChannelCardType {
  return {
    id,
    providerId: 1,
    providerName: 'OpenAI',
    name: `ch-${id}`,
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
    lastHealthStatus: status,
    stats: {
      endpointCount: 0,
      credentialCount: 0,
      modelCount: 0,
      avgResponseTime: null,
    },
  };
}

describe('ProviderGroupHeader N/M 健康聚合（任务 9.4）', () => {
  it('5 个渠道中 3 个 HEALTHY 应显示"3/5 健康"', () => {
    const channels: ChannelCardType[] = [
      buildChannel(1, 'HEALTHY'),
      buildChannel(2, 'HEALTHY'),
      buildChannel(3, 'HEALTHY'),
      buildChannel(4, 'FAILED'),
      buildChannel(5, 'DEGRADED'),
    ];
    render(
      <ProviderGroupHeader
        providerName="OpenAI"
        channelCount={5}
        endpointCount={5}
        credentialCount={5}
        modelCount={5}
        collapsed={false}
        onToggle={vi.fn()}
        channels={channels}
      />,
    );

    // i18n key: provider.healthSummary = "{{healthy}}/{{total}} 健康"
    expect(screen.getByText(/3\/5 健康/)).toBeInTheDocument();
  });

  it('未提供 channels 时不渲染聚合', () => {
    render(
      <ProviderGroupHeader
        providerName="OpenAI"
        channelCount={5}
        endpointCount={5}
        credentialCount={5}
        modelCount={5}
        collapsed={false}
        onToggle={vi.fn()}
      />,
    );
    expect(screen.queryByText(/健康$/)).not.toBeInTheDocument();
  });
});
