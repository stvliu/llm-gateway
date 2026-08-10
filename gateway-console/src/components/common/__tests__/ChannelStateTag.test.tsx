/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// ChannelStateTag 状态语义可视化测试
//
// 验证 hover Tag 时 Tooltip 内容由 buildStateTooltip 派生：
// - ACTIVE：流量/计费均"是"，可后转 "已暂停"/"已下线"
// - RETIRED：终态文案，无后续状态
//
// jsdom 不实现 matchMedia，AntD Tooltip 在挂载时会调用 → 在 beforeAll 补 stub。
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll } from 'vitest';
import i18n from '@/i18n';
import ChannelStateTag from '@/components/common/ChannelStateTag';

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
  // jsdom 不实现 ResizeObserver，AntD Tooltip 内部 trigger 在挂载时使用
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

describe('ChannelStateTag tooltip', () => {
  it('hover ACTIVE 应显示流量/计费/可转换至说明', async () => {
    render(<ChannelStateTag state="ACTIVE" />);
    const user = userEvent.setup();
    await user.hover(screen.getByText('运行中'));

    await waitFor(() => {
      // Tooltip 文本节点：包含描述文案
      const tooltips = screen.getAllByRole('tooltip');
      expect(tooltips.length).toBeGreaterThan(0);
      const text = tooltips.map((el) => el.textContent ?? '').join(' ');
      // 描述：渠道运行正常，正在参与流量分配并计费
      expect(text).toContain('参与流量');
      expect(text).toContain('参与计费');
      // 可后继状态包含"已暂停"与"已下线"
      expect(text).toContain('已暂停');
      expect(text).toContain('已下线');
    });
  });

  it('hover RETIRED 应显示终态文案', async () => {
    render(<ChannelStateTag state="RETIRED" />);
    const user = userEvent.setup();
    await user.hover(screen.getByText('已废弃'));

    await waitFor(() => {
      const tooltips = screen.getAllByRole('tooltip');
      expect(tooltips.length).toBeGreaterThan(0);
      const text = tooltips.map((el) => el.textContent ?? '').join(' ');
      expect(text).toContain('终态');
      // 终态不应展示 "可转换至" 列表
      expect(text).not.toContain('可转换至');
    });
  });

  it('PENDING 颜色应为加深的 #d48806（4.5:1 对比度）', () => {
    render(<ChannelStateTag state="PENDING" />);
    const tag = screen.getByText('待激活');
    // 行内样式 color: #d48806（jsdom 输出小写）
    expect(tag).toHaveStyle({ color: '#d48806' });
  });
});
