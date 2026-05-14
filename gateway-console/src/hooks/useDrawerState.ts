import { useState, useCallback } from 'react';

/**
 * 抽屉状态
 */
interface DrawerState<T> {
  /** 是否打开 */
  open: boolean;
  /** 实体数据 */
  entity: T | null;
  /** 模式 */
  mode: 'view' | 'edit';
  /** 是否有未保存的更改 */
  hasUnsavedChanges: boolean;
  /** 当前索引 */
  currentIndex: number;
  /** 总数量 */
  totalCount: number;
}

/**
 * 抽屉状态管理 Hook 返回值
 */
interface UseDrawerStateReturn<T> extends DrawerState<T> {
  /** 打开抽屉 */
  openDrawer: (entity: T, index?: number, total?: number) => void;
  /** 关闭抽屉 */
  closeDrawer: () => void;
  /** 设置模式 */
  setMode: (mode: 'view' | 'edit') => void;
  /** 设置未保存更改状态 */
  setHasUnsavedChanges: (value: boolean) => void;
  /** 导航到指定索引 */
  navigateTo: (index: number) => void;
  /** 导航到上一个 */
  goPrevious: () => void;
  /** 导航到下一个 */
  goNext: () => void;
}

/**
 * 抽屉状态管理 Hook
 * 用于管理实体详情抽屉的状态和导航
 */
export function useDrawerState<T>(
  entities?: T[],
  onNavigate?: (entity: T, index: number) => void
): UseDrawerStateReturn<T> {
  const [state, setState] = useState<DrawerState<T>>({
    open: false,
    entity: null,
    mode: 'view',
    hasUnsavedChanges: false,
    currentIndex: 0,
    totalCount: 0,
  });

  const openDrawer = useCallback((entity: T, index = 0, total = 0) => {
    setState({
      open: true,
      entity,
      mode: 'view',
      hasUnsavedChanges: false,
      currentIndex: index,
      totalCount: total,
    });
  }, []);

  const closeDrawer = useCallback(() => {
    setState((prev) => ({
      ...prev,
      open: false,
      mode: 'view',
      hasUnsavedChanges: false,
    }));
  }, []);

  const setMode = useCallback((mode: 'view' | 'edit') => {
    setState((prev) => ({ ...prev, mode }));
  }, []);

  const setHasUnsavedChanges = useCallback((value: boolean) => {
    setState((prev) => ({ ...prev, hasUnsavedChanges: value }));
  }, []);

  const navigateTo = useCallback((index: number) => {
    if (!entities || index < 0 || index >= entities.length) return;
    const newEntity = entities[index];
    setState((prev) => ({
      ...prev,
      entity: newEntity,
      currentIndex: index,
      mode: 'view',
      hasUnsavedChanges: false,
    }));
    onNavigate?.(newEntity, index);
  }, [entities, onNavigate]);

  const goPrevious = useCallback(() => {
    if (state.currentIndex > 0) {
      navigateTo(state.currentIndex - 1);
    }
  }, [state.currentIndex, navigateTo]);

  const goNext = useCallback(() => {
    if (state.currentIndex < state.totalCount - 1) {
      navigateTo(state.currentIndex + 1);
    }
  }, [state.currentIndex, state.totalCount, navigateTo]);

  return {
    ...state,
    openDrawer,
    closeDrawer,
    setMode,
    setHasUnsavedChanges,
    navigateTo,
    goPrevious,
    goNext,
  };
}