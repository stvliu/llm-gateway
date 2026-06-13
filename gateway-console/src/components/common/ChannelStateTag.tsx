import React from 'react';
import type { ChannelState } from '@/types/channel';

/** 状态配置：颜色、图标、文字 */
const STATE_CONFIG: Record<ChannelState, { color: string; bg: string; label: string }> = {
  PENDING: {
    color: '#faad14',
    bg: '#fffbe6',
    label: '待激活',
  },
  ACTIVE: {
    color: '#52c41a',
    bg: '#f6ffed',
    label: '运行中',
  },
  SUSPENDED: {
    color: '#d9d9d9',
    bg: '#fafafa',
    label: '已暂停',
  },
  DEPRECATED: {
    color: '#fa8c16',
    bg: '#fff7e6',
    label: '已下线',
  },
  RETIRED: {
    color: '#ff4d4f',
    bg: '#fff2f0',
    label: '已废弃',
  },
};

interface ChannelStateTagProps {
  state: ChannelState;
}

/**
 * 渠道状态标签组件
 *
 * <p>根据状态展示不同配色 + 文字的 Tag。</p>
 * <ul>
 *   <li>PENDING：黄色「待激活」</li>
 *   <li>ACTIVE：绿色「运行中」</li>
 *   <li>SUSPENDED：灰色「已暂停」</li>
 *   <li>DEPRECATED：橙色「已下线」</li>
 *   <li>RETIRED：红色「已废弃」</li>
 * </ul>
 */
const ChannelStateTag: React.FC<ChannelStateTagProps> = ({ state }) => {
  const config = STATE_CONFIG[state] ?? STATE_CONFIG.SUSPENDED;

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 4,
        padding: '2px 8px',
        fontSize: 12,
        lineHeight: '18px',
        borderRadius: 4,
        color: config.color,
        backgroundColor: config.bg,
        border: `1px solid ${config.color}20`,
        fontWeight: 500,
      }}
    >
      {config.label}
    </span>
  );
};

export { STATE_CONFIG };
export default ChannelStateTag;
