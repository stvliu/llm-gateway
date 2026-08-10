/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useState, useRef, useCallback, useEffect } from 'react';

/**
 * 保存反馈脉冲的状态。
 * - idle：无反馈
 * - success：保存成功，3 秒后自动归位 idle
 * - error：保存失败，错误态常驻直至下次 triggerSuccess / triggerError 调用
 */
export type PulseState = 'idle' | 'success' | 'error';

/**
 * Hook 返回值。
 * - state：当前状态
 * - className：根据 state 派生的 CSS 类名（idle → ''）
 * - errorMsg：错误态下的具体原因（成功态会被清空）
 * - triggerSuccess：标记保存成功
 * - triggerError：标记保存失败并附带错误原因
 */
export interface UseSavePulseResult {
  state: PulseState;
  className: string;
  errorMsg: string | undefined;
  triggerSuccess: () => void;
  triggerError: (msg: string) => void;
}

/**
 * 保存反馈脉冲 Hook。
 *
 * 用于在表单 / 行级编辑保存后给出位置上下文反馈：
 * - 成功 → 短暂高亮（3s 自动归位）；
 * - 失败 → 红框 + 错误原因常驻，直至用户触发下一次保存。
 *
 * 与全局 toast 互补：toast 提供全局可见性，本 Hook 在元素上提供位置上下文。
 *
 * 示例：
 * ```tsx
 * const pulse = useSavePulse();
 * <li className={pulse.className}>
 *   ...
 *   {pulse.state === 'success' && <span>✓ 已保存</span>}
 *   {pulse.state === 'error'   && <span>✗ {pulse.errorMsg}</span>}
 * </li>
 * ```
 *
 * @returns 状态、派生 className、错误原因，以及两个触发器
 */
export function useSavePulse(): UseSavePulseResult {
  const [state, setState] = useState<PulseState>('idle');
  const [errorMsg, setErrorMsg] = useState<string | undefined>(undefined);
  // 自动归位定时器引用，组件卸载或下一次 trigger 调用时需要清理
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  /** 清理已挂载的定时器，避免重复触发或卸载后回调 */
  const clearTimer = () => {
    if (timerRef.current !== null) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  };

  /** 标记保存成功：进入 success，3s 后归位 idle，并清空 errorMsg */
  const triggerSuccess = useCallback(() => {
    clearTimer();
    setState('success');
    setErrorMsg(undefined);
    timerRef.current = setTimeout(() => {
      setState('idle');
      timerRef.current = null;
    }, 3000);
  }, []);

  /** 标记保存失败：进入 error，errorMsg 常驻直至下次 trigger 调用 */
  const triggerError = useCallback((msg: string) => {
    clearTimer();
    setState('error');
    setErrorMsg(msg);
    // 错误态故意不自动清除，保证用户能看清失败原因
  }, []);

  // 组件卸载时清理定时器，防止 setState on unmounted component
  useEffect(() => {
    return () => {
      clearTimer();
    };
  }, []);

  // 由 state 派生 className（idle → 空串，便于直接用于 className={pulse.className}）
  const className =
    state === 'success'
      ? 'save-pulse-success'
      : state === 'error'
        ? 'save-pulse-error'
        : '';

  return { state, className, errorMsg, triggerSuccess, triggerError };
}
