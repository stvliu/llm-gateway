/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// 任务 9.7：ConnectivityTestPanel 改名为"预检工具"
//
// 验证目标：
// 1) 标题改为"预检工具"（i18n key: precheck.title）
// 2) 副标题"用于在创建渠道前测试 baseUrl + Key 的可用性"（precheck.subtitle）
//
// 注：当前 ConnectivityTestPanel 调用的是 /providers/test-connectivity（与 channelId 解耦），
// 后端按 PRECHECK 分支隐式跳过持久化；仅在请求载荷 / 注释里标记 source=PRECHECK 语义。
import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeAll } from 'vitest';
import { App as AntApp } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import i18n from '@/i18n';
import { ConnectivityTestPanel } from '@/pages/Channels/ConnectivityTestPanel';

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

function renderPanel() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <AntApp>
        <ConnectivityTestPanel providerCode="openai" />
      </AntApp>
    </QueryClientProvider>,
  );
}

describe('ConnectivityTestPanel 改名为"预检工具"（任务 9.7）', () => {
  it('应渲染新标题"预检工具"', () => {
    renderPanel();
    expect(screen.getByText(/预检工具/)).toBeInTheDocument();
  });

  it('应渲染副标题"用于在创建渠道前测试 baseUrl + Key 的可用性"', () => {
    renderPanel();
    expect(screen.getByText(/创建渠道前测试 baseUrl \+ Key 的可用性/)).toBeInTheDocument();
  });
});
