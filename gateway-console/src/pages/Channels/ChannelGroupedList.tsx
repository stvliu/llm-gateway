import { useState } from 'react';
import type { ChannelGroup, ChannelCard as ChannelCardType } from '@/types/channel';
import { ProviderGroupHeader } from './ProviderGroupHeader';
import { ChannelCard } from './ChannelCard';
import type { FC } from 'react';

export interface ChannelGroupedListProps {
  /** 按供应商分组的渠道数据 */
  groups: ChannelGroup[];
  /** 渠道点击回调 */
  onChannelClick: (channel: ChannelCardType) => void;
}

/**
 * 按供应商分组的渠道列表
 * 使用 useState 管理折叠状态
 */
export const ChannelGroupedList: FC<ChannelGroupedListProps> = ({
  groups,
  onChannelClick,
}) => {
  // 管理每个供应商的折叠状态
  const [collapsedMap, setCollapsedMap] = useState<Record<number, boolean>>({});

  const toggleCollapse = (providerId: number) => {
    setCollapsedMap((prev) => ({
      ...prev,
      [providerId]: !prev[providerId],
    }));
  };

  return (
    <div>
      {groups.map((group) => {
        const collapsed = collapsedMap[group.provider.id] ?? false;

        // 计算该供应商的聚合统计
        const totalEndpoints = group.channels.reduce(
          (sum, ch) => sum + ch.stats.endpointCount,
          0
        );
        const totalCredentials = group.channels.reduce(
          (sum, ch) => sum + ch.stats.credentialCount,
          0
        );
        const totalModels = group.channels.reduce(
          (sum, ch) => sum + ch.stats.modelCount,
          0
        );

        return (
          <div key={group.provider.id} style={{ marginBottom: '24px' }}>
            {/* 供应商分组头 */}
            <ProviderGroupHeader
              providerId={group.provider.providerId}
              providerName={group.provider.providerName}
              channelCount={group.channels.length}
              endpointCount={totalEndpoints}
              credentialCount={totalCredentials}
              modelCount={totalModels}
              collapsed={collapsed}
              onToggle={() => toggleCollapse(group.provider.id)}
            />

            {/* 渠道卡片列表（折叠时隐藏） */}
            {!collapsed && (
              <div>
                {group.channels.map((channel) => (
                  <ChannelCard
                    key={channel.id}
                    channel={channel}
                    onClick={onChannelClick}
                  />
                ))}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};
