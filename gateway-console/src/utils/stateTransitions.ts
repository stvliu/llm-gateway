import type { ChannelState } from '@/types/channel';

/**
 * 获取渠道状态可用的合法转换目标列表
 *
 * <p>与后端 Channel.State.canTransitionTo() 逻辑一致。</p>
 *
 * @param currentState 当前状态
 * @returns 合法的目标状态列表
 */
export function getAvailableTransitions(currentState: ChannelState): ChannelState[] {
  switch (currentState) {
    case 'PENDING':
      return ['ACTIVE'];
    case 'ACTIVE':
      return ['SUSPENDED', 'DEPRECATED'];
    case 'SUSPENDED':
      return ['ACTIVE', 'DEPRECATED', 'RETIRED'];
    case 'DEPRECATED':
      return ['RETIRED'];
    case 'RETIRED':
      return [];
    default:
      return [];
  }
}

/**
 * 判断状态是否为终态（不可再转换）
 */
export function isTerminalState(state: ChannelState): boolean {
  return state === 'RETIRED';
}

/**
 * 判断状态是否可路由（可参与流量分配）
 */
export function isRoutableState(state: ChannelState): boolean {
  return state === 'ACTIVE' || state === 'DEPRECATED';
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
TRANSITION_ACTION_LABELS['SUSPENDED_RETIRED'] = '废弃';
TRANSITION_ACTION_LABELS['DEPRECATED_RETIRED'] = '废弃';

/**
 * 获取转换操作按钮文本
 */
export function getTransitionActionLabel(from: ChannelState, to: ChannelState): string {
  return TRANSITION_ACTION_LABELS[`${from}_${to}`] ?? to;
}
