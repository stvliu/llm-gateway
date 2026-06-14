// 任务 9.2：HealthDot 组件单元测试
//
// 验证目标（plan 行 1297-1322）：
// 1) 4 状态色：HEALTHY=#52c41a / DEGRADED=#faad14 / FAILED=#ff4d4f
// 2) UNKNOWN 应空心：背景 transparent + 有 border
// 3) null status 当作 UNKNOWN 处理
// 4) hover 应显示 lastCheckAt + source（DRAWER 翻译为"详情"）
//
// jsdom 不实现 matchMedia / ResizeObserver，AntD Popover 在挂载时使用 → beforeAll 补 stub。
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeAll } from 'vitest';
import i18n from '@/i18n';
import { HealthDot } from '@/components/common/HealthDot';

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

describe('HealthDot', () => {
  it.each([
    ['HEALTHY', 'rgb(82, 196, 26)'], // #52c41a
    ['DEGRADED', 'rgb(250, 173, 20)'], // #faad14
    ['FAILED', 'rgb(255, 77, 79)'], // #ff4d4f
  ])('%s 状态应填充对应颜色 %s', (status, color) => {
    render(<HealthDot status={status as 'HEALTHY' | 'DEGRADED' | 'FAILED'} />);
    const dot = screen.getByTestId('health-dot');
    expect(dot).toHaveStyle({ backgroundColor: color });
  });

  it('UNKNOWN 应空心：背景 transparent + 有 border', () => {
    render(<HealthDot status="UNKNOWN" />);
    const dot = screen.getByTestId('health-dot');
    // jsdom 对 backgroundColor: transparent 的行为：会序列化为 ''（视为初始值），
    // 这里直接读取 inline style 字段判定
    const bg = (dot as HTMLElement).style.backgroundColor;
    expect(bg === '' || bg === 'transparent' || bg === 'rgba(0, 0, 0, 0)').toBe(true);
    // border 不应为 none / 0
    const borderStyle = (dot as HTMLElement).style.border;
    expect(borderStyle).not.toBe('');
    expect(borderStyle).not.toBe('none');
  });

  it('null status 应当作 UNKNOWN 处理（空心 + 透明）', () => {
    render(<HealthDot status={null} />);
    const dot = screen.getByTestId('health-dot');
    const bg = (dot as HTMLElement).style.backgroundColor;
    expect(bg === '' || bg === 'transparent' || bg === 'rgba(0, 0, 0, 0)').toBe(true);
    const borderStyle = (dot as HTMLElement).style.border;
    expect(borderStyle).not.toBe('');
  });

  it('hover 应显示 lastCheckAt + source（DRAWER 翻译为"详情"）', async () => {
    render(
      <HealthDot
        status="HEALTHY"
        lastCheckAt="2026-06-13T10:00:00Z"
        source="DRAWER"
      />,
    );
    const dot = screen.getByTestId('health-dot');
    await userEvent.hover(dot);
    await waitFor(() => {
      // Popover content：包含"最后一次测试"文案
      expect(screen.getByText(/最后一次测试/)).toBeInTheDocument();
      // source=DRAWER 翻译为"详情"
      expect(screen.getByText(/详情/)).toBeInTheDocument();
    });
  });

  it('未提供 lastCheckAt 时 hover 应显示"尚未测试"', async () => {
    render(<HealthDot status="UNKNOWN" />);
    const dot = screen.getByTestId('health-dot');
    await userEvent.hover(dot);
    await waitFor(() => {
      expect(screen.getByText(/尚未测试/)).toBeInTheDocument();
    });
  });
});
