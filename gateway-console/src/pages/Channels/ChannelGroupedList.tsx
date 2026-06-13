import { useState } from 'react';
import type { ChannelGroup, ChannelCard as ChannelCardType } from '@/types/channel';
import { ProviderGroupHeader } from './ProviderGroupHeader';
import { ChannelCard } from './ChannelCard';
import type { FC } from 'react';

export interface ChannelGroupedListProps {
  groups: ChannelGroup[];
  onChannelClick: (channel: ChannelCardType) => void;
  onChannelDelete: (id: number) => void;
  onChannelToggleState: (id: number, enabled: boolean) => void;
  onTestChannel: (channel: ChannelCardType) => void;
  onStateTransition?: (id: number, targetState: string, reason?: string) => void;
  onEditProvider?: (providerId: number) => void;
  onToggleProviderEnabled?: (providerId: number) => void;
  onTestProviderConnectivity?: (providerId: number) => void;
  onExportProvider?: (providerId: number) => void;
}

/**
 * 按供应商分组的渠道列表
 * 使用 useState 管理折叠状态
 */
export const ChannelGroupedList: FC<ChannelGroupedListProps> = ({
  groups,
  onChannelClick,
  onChannelDelete,
  onChannelToggleState,
  onTestChannel,
  onStateTransition,
  onEditProvider,
  onToggleProviderEnabled,
  onTestProviderConnectivity,
  onExportProvider,
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
              providerName={group.provider.providerName}
              providerCode={group.provider.providerId}
              channelCount={group.channels.length}
              endpointCount={totalEndpoints}
              credentialCount={totalCredentials}
              modelCount={totalModels}
              collapsed={collapsed}
              onToggle={() => toggleCollapse(group.provider.id)}
              onEdit={onEditProvider ? () => onEditProvider(group.provider.id) : undefined}
              onBatchSuspend={onToggleProviderEnabled ? () => onToggleProviderEnabled(group.provider.id) : undefined}
              onBatchResume={onToggleProviderEnabled ? () => onToggleProviderEnabled(group.provider.id) : undefined}
              onTestConnectivity={onTestProviderConnectivity ? () => onTestProviderConnectivity(group.provider.id) : undefined}
              onExport={onExportProvider ? () => onExportProvider(group.provider.id) : undefined}
            />

            {/* 渠道卡片列表（折叠时隐藏） */}
            {!collapsed && (
              <div>
                {group.channels.map((channel) => (
                  <ChannelCard
                    key={channel.id}
                    channel={channel}
                    onClick={onChannelClick}
                    onDelete={onChannelDelete}
                    onToggleState={onChannelToggleState}
                    onTest={onTestChannel}
                    onStateTransition={onStateTransition}
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
