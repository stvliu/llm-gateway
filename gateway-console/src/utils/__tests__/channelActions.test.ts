/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { describe, it, expect } from 'vitest';
import { getActionBarConfig } from '@/utils/channelActions';

describe('getActionBarConfig', () => {
  it('PENDING → primaryAction=ACTIVE, dropdown 无 ACTIVE, delete 不禁用', () => {
    const config = getActionBarConfig('PENDING');
    expect(config.primaryAction).toBe('ACTIVE');
    expect(config.dropdownTransitions).not.toContain('ACTIVE');
    expect(config.deleteDisabled).toBe(false);
  });

  it('ACTIVE → primaryAction=SUSPENDED, delete 禁用', () => {
    const config = getActionBarConfig('ACTIVE');
    expect(config.primaryAction).toBe('SUSPENDED');
    expect(config.deleteDisabled).toBe(true);
    expect(config.dropdownTransitions).toEqual(['DEPRECATED']);
  });

  it('SUSPENDED → primaryAction=ACTIVE, dropdown 包含 DEPRECATED 和 RETIRED（按序）', () => {
    const config = getActionBarConfig('SUSPENDED');
    expect(config.primaryAction).toBe('ACTIVE');
    expect(config.dropdownTransitions).toEqual(['DEPRECATED', 'RETIRED']);
    expect(config.deleteDisabled).toBe(false);
  });

  it('DEPRECATED → primaryAction=null, dropdown 含 RETIRED', () => {
    const config = getActionBarConfig('DEPRECATED');
    expect(config.primaryAction).toBeNull();
    expect(config.dropdownTransitions).toEqual(['RETIRED']);
    expect(config.deleteDisabled).toBe(false);
  });

  it('RETIRED → primaryAction=null, dropdown 空, delete 不禁用', () => {
    const config = getActionBarConfig('RETIRED');
    expect(config.primaryAction).toBeNull();
    expect(config.dropdownTransitions).toEqual([]);
    expect(config.deleteDisabled).toBe(false);
  });

  it('dropdown 排序：DEPRECATED 在 RETIRED 之前', () => {
    const config = getActionBarConfig('SUSPENDED');
    const order = config.dropdownTransitions;
    expect(order.indexOf('DEPRECATED')).toBeLessThan(order.indexOf('RETIRED'));
  });
});
