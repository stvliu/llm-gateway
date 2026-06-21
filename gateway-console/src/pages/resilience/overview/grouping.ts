/**
 * 容灾总览成员渠道分组纯函数
 *
 * <p>后端 ChannelResponse 已透传 clusterId（4.11b 补全 2）。
 * 本函数按 clusterId 将渠道聚合到 Map，供总览页 Cluster 卡片渲染成员渠道列表。</p>
 *
 * <p>clusterId 为 null/undefined 的渠道归到 null 键（未分组），不丢失。</p>
 *
 * <p>纯函数无副作用，便于单测。</p>
 */
import type { Channel } from '@/types/channel';

/**
 * 按 clusterId 分组渠道
 *
 * @param channels 渠道列表
 * @returns Map<clusterId | null, Channel[]>，键为 null 表示未分组
 */
export function groupChannelsByCluster(
  channels: Channel[],
): Map<number | null, Channel[]> {
  const m = new Map<number | null, Channel[]>();
  for (const ch of channels) {
    // null 与 undefined 统一归到 null 键（未分组）
    const key = ch.clusterId ?? null;
    const arr = m.get(key);
    if (arr) {
      arr.push(ch);
    } else {
      m.set(key, [ch]);
    }
  }
  return m;
}
