/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
// 渠道生命周期 SSOT 测试
//
// 验证 CHANNEL_LIFECYCLE 数据结构的字段不变性 + selector helper 行为，
// 这是后续 ChannelStateTag / ChannelCard / 状态转换菜单等所有消费方的契约基础。
import { describe, it, expect } from 'vitest';
import {
  CHANNEL_LIFECYCLE,
  isRoutable,
  isBilling,
  allowedTransitions,
  canTransitionTo,
  buildStateTooltip,
} from '../lifecycle';
import { getAvailableTransitions } from '@/utils/stateTransitions';
import type { ChannelState } from '@/types/channel';

describe('CHANNEL_LIFECYCLE', () => {
  it('五条状态记录的字段不变性', () => {
    // 顺序与 design doc §1 一致，便于 UI 列表场景按顺序展示
    expect(Object.keys(CHANNEL_LIFECYCLE)).toEqual([
      'PENDING',
      'ACTIVE',
      'SUSPENDED',
      'DEPRECATED',
      'RETIRED',
    ]);
    expect(CHANNEL_LIFECYCLE.ACTIVE.isRoutable).toBe(true);
    expect(CHANNEL_LIFECYCLE.ACTIVE.isBilling).toBe(true);
    // 关键决策：DEPRECATED 仍参与流量分配
    expect(CHANNEL_LIFECYCLE.DEPRECATED.isRoutable).toBe(true);
    expect(CHANNEL_LIFECYCLE.SUSPENDED.isRoutable).toBe(false);
    expect(CHANNEL_LIFECYCLE.RETIRED.nextStates).toEqual([]);
    expect(CHANNEL_LIFECYCLE.RETIRED.visualStyle).toBe('strikethrough');
    // PENDING 黄色加深至 #d48806，与 #ffffff 背景对比度 ≥ 4.5:1（WCAG AA）
    expect(CHANNEL_LIFECYCLE.PENDING.color).toBe('#d48806');
  });

  it('selector helpers', () => {
    expect(isRoutable('ACTIVE')).toBe(true);
    expect(isRoutable('SUSPENDED')).toBe(false);
    expect(isBilling('SUSPENDED')).toBe(false);
    expect(isBilling('DEPRECATED')).toBe(true);
    expect(allowedTransitions('ACTIVE')).toEqual(['SUSPENDED', 'DEPRECATED']);
    expect(allowedTransitions('RETIRED')).toEqual([]);
    expect(canTransitionTo('PENDING', 'ACTIVE')).toBe(true);
    expect(canTransitionTo('RETIRED', 'ACTIVE')).toBe(false);
  });

  it('buildStateTooltip 注入 i18n 文案', () => {
    // 用 identity 翻译函数验证 tooltip 中所有 i18n key 都被注入
    const t = (k: string) => k;
    const tooltip = buildStateTooltip('ACTIVE', t);
    expect(tooltip).toContain('channel.state.activeDesc');
    expect(tooltip).toContain('channel.state.tooltipRoutable');
    expect(tooltip).toContain('channel.state.tooltipBilling');
    // ACTIVE 可后转 SUSPENDED / DEPRECATED，应展示 tooltipNext
    expect(tooltip).toContain('channel.state.tooltipNext');
  });

  it('buildStateTooltip 在终态展示 tooltipTerminal', () => {
    const t = (k: string) => k;
    const tooltip = buildStateTooltip('RETIRED', t);
    expect(tooltip).toContain('channel.state.tooltipTerminal');
    // 终态不再展示后续可转换，避免误导
    expect(tooltip).not.toContain('channel.state.tooltipNext');
  });

  it('allowedTransitions 与 stateTransitions getAvailableTransitions 一致（防漂移）', () => {
    const states: ChannelState[] = ['PENDING', 'ACTIVE', 'SUSPENDED', 'DEPRECATED', 'RETIRED'];
    for (const s of states) {
      expect([...allowedTransitions(s)]).toEqual(getAvailableTransitions(s));
    }
  });
});
