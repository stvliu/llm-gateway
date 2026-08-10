/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
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
