import type { ChannelState } from '@/types/channel';
import { allowedTransitions, isRoutable, CHANNEL_LIFECYCLE } from '@/domain/channel/lifecycle';

/**
 * 获取渠道状态可用的合法转换目标列表
 *
 * <p>委托给 {@link allowedTransitions}（lifecycle.ts SSOT），保持前后端一致。</p>
 *
 * @param currentState 当前状态
 * @returns 合法的目标状态列表
 */
export function getAvailableTransitions(currentState: ChannelState): ChannelState[] {
  return [...allowedTransitions(currentState)];
}

/**
 * 判断状态是否为终态（不可再转换）
 */
export function isTerminalState(state: ChannelState): boolean {
  return CHANNEL_LIFECYCLE[state].nextStates.length === 0;
}

/**
 * 判断状态是否可路由（可参与流量分配）
 */
export function isRoutableState(state: ChannelState): boolean {
  return isRoutable(state);
}

/**
 * 状态转换操作按钮文本映射
 */
export const TRANSITION_ACTION_LABELS: Record<string, string> = {
  PENDING: '',
  ACTIVE: '',
  SUSPENDED: '',
  DEPRECATED: '',
  RETIRED: '',
};

// 初始化操作按钮文本
TRANSITION_ACTION_LABELS['PENDING_ACTIVE'] = '激活';
TRANSITION_ACTION_LABELS['ACTIVE_SUSPENDED'] = '暂停';
TRANSITION_ACTION_LABELS['ACTIVE_DEPRECATED'] = '标记下线';
TRANSITION_ACTION_LABELS['SUSPENDED_ACTIVE'] = '恢复';
TRANSITION_ACTION_LABELS['SUSPENDED_DEPRECATED'] = '标记下线';
TRANSITION_ACTION_LABELS['SUSPENDED_RETIRED'] = '退役';
TRANSITION_ACTION_LABELS['DEPRECATED_RETIRED'] = '退役';

/**
 * 获取转换操作按钮文本
 */
export function getTransitionActionLabel(from: ChannelState, to: ChannelState): string {
  return TRANSITION_ACTION_LABELS[`${from}_${to}`] ?? to;
}
