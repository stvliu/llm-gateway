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
import { allowedTransitions } from '@/domain/channel/lifecycle';
import type { ChannelState } from '@/types/channel';

const SEVERITY_ORDER: Record<string, number> = {
  ACTIVE: 1,
  SUSPENDED: 2,
  DEPRECATED: 3,
  RETIRED: 4,
};

export interface ChannelActionBarConfig {
  primaryAction: ChannelState | null;
  dropdownTransitions: ChannelState[];
  deleteDisabled: boolean;
  deleteDisabledTooltipKey: string;
}

export function getActionBarConfig(state: ChannelState): ChannelActionBarConfig {
  const transitions = [...allowedTransitions(state)];
  transitions.sort((a, b) => (SEVERITY_ORDER[a] ?? 99) - (SEVERITY_ORDER[b] ?? 99));

  let primaryAction: ChannelState | null = null;
  if (state === 'PENDING') primaryAction = 'ACTIVE';
  else if (state === 'ACTIVE') primaryAction = 'SUSPENDED';
  else if (state === 'SUSPENDED') primaryAction = 'ACTIVE';

  const dropdownTransitions = primaryAction
    ? transitions.filter(t => t !== primaryAction)
    : transitions;

  return {
    primaryAction,
    dropdownTransitions,
    deleteDisabled: state === 'ACTIVE',
    deleteDisabledTooltipKey: 'channel.action.deleteDisabledWhenActive',
  };
}
