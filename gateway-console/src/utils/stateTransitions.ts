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
 * 状态转换操作按钮 i18n key 映射
 *
 * <p>调用方需用 t() 翻译。</p>
 */
const TRANSITION_ACTION_I18N_KEYS: Record<string, string> = {
  PENDING_ACTIVE: 'channel.action.activate',
  ACTIVE_SUSPENDED: 'channel.action.suspend',
  ACTIVE_DEPRECATED: 'channel.action.deprecate',
  SUSPENDED_ACTIVE: 'channel.action.enable',
  SUSPENDED_DEPRECATED: 'channel.action.deprecate',
  SUSPENDED_RETIRED: 'channel.action.retire',
  DEPRECATED_RETIRED: 'channel.action.retire',
};

/**
 * 获取状态转换操作对应的 i18n key
 *
 * <p>调用方需用 t() 翻译。</p>
 *
 * @param from 当前状态
 * @param to 目标状态
 * @returns i18n key
 */
export function getTransitionActionLabel(from: ChannelState, to: ChannelState): string {
  return TRANSITION_ACTION_I18N_KEYS[`${from}_${to}`] ?? to;
}
