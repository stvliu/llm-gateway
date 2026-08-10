/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// useSavePulse Hook 单元测试
//
// 验证保存反馈脉冲 Hook 的状态机：
// - triggerSuccess 进入 success 态，3s 后自动归位 idle
// - triggerError 进入 error 态，10s 后仍保持 error，errorMsg 保留
// - 先 error 再 success 应清空 errorMsg
// - 卸载时 cleanup 定时器，不抛错
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useSavePulse } from '../useSavePulse';

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('useSavePulse', () => {
  it('triggerSuccess 后 className=save-pulse-success，3s 后自动归位 idle', () => {
    const { result } = renderHook(() => useSavePulse());

    expect(result.current.state).toBe('idle');
    expect(result.current.className).toBe('');

    act(() => {
      result.current.triggerSuccess();
    });
    expect(result.current.state).toBe('success');
    expect(result.current.className).toBe('save-pulse-success');

    act(() => {
      vi.advanceTimersByTime(3001);
    });
    expect(result.current.state).toBe('idle');
    expect(result.current.className).toBe('');
  });

  it('triggerError 应保持 error 状态不自动清除（10s 后仍是 error）', () => {
    const { result } = renderHook(() => useSavePulse());

    act(() => {
      result.current.triggerError('boom');
    });
    expect(result.current.state).toBe('error');
    expect(result.current.className).toBe('save-pulse-error');
    expect(result.current.errorMsg).toBe('boom');

    act(() => {
      vi.advanceTimersByTime(10000);
    });
    expect(result.current.state).toBe('error');
    expect(result.current.errorMsg).toBe('boom');
  });

  it('triggerError 后再 triggerSuccess 应清空 errorMsg', () => {
    const { result } = renderHook(() => useSavePulse());

    act(() => {
      result.current.triggerError('first error');
    });
    expect(result.current.errorMsg).toBe('first error');

    act(() => {
      result.current.triggerSuccess();
    });
    expect(result.current.state).toBe('success');
    expect(result.current.errorMsg).toBeUndefined();
  });

  it('卸载时 cleanup 定时器（不抛错）', () => {
    const { result, unmount } = renderHook(() => useSavePulse());

    act(() => {
      result.current.triggerSuccess();
    });

    expect(() => {
      unmount();
      vi.advanceTimersByTime(3001);
    }).not.toThrow();
  });
});
