// 容灾总览成员渠道分组纯函数单元测试
//
// 任务 4.11b 补全 2：验证按 clusterId 分组渠道逻辑（后端 ChannelResponse 已透传 clusterId）：
// - 同 clusterId 的渠道聚合到同一数组
// - clusterId 为 null/undefined 的渠道归到「未分组」键（key=null）
// - 空输入返回空 Map
//
// 策略：纯函数无副作用，直接断言分组结果。
import { describe, it, expect } from 'vitest';
import { groupChannelsByCluster } from '../grouping';
import type { Channel } from '@/types/channel';

/** 构造最小 Channel（仅含分组所需字段，其余字段用占位值） */
function makeChannel(id: number, clusterId: number | null | undefined): Channel {
  return {
    id,
    providerId: 1,
    providerName: 'p',
    name: `ch-${id}`,
    billingMode: 'pay_as_you_go',
    quotaLimit: null,
    priority: 1,
    weight: 1,
    timeout: null,
    maxRetries: null,
    state: 'ACTIVE',
    endpoints: [],
    createdAt: '',
    updatedAt: '',
    clusterId,
  } as Channel;
}

describe('groupChannelsByCluster', () => {
  it('空数组返回空 Map', () => {
    const m = groupChannelsByCluster([]);
    expect(m.size).toBe(0);
  });

  it('同 clusterId 渠道聚合到同一数组', () => {
    const m = groupChannelsByCluster([
      makeChannel(1, 10),
      makeChannel(2, 10),
      makeChannel(3, 20),
    ]);
    expect(m.get(10)?.map((c) => c.id)).toEqual([1, 2]);
    expect(m.get(20)?.map((c) => c.id)).toEqual([3]);
  });

  it('clusterId=null 的渠道归到 null 键（未分组）', () => {
    const m = groupChannelsByCluster([
      makeChannel(1, null),
      makeChannel(2, 10),
    ]);
    expect(m.get(null)?.map((c) => c.id)).toEqual([1]);
    expect(m.get(10)?.map((c) => c.id)).toEqual([2]);
  });

  it('clusterId=undefined 视同 null 归到未分组', () => {
    const m = groupChannelsByCluster([makeChannel(1, undefined)]);
    expect(m.get(null)?.map((c) => c.id)).toEqual([1]);
  });
});
