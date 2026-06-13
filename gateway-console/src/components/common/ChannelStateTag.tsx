import React from 'react';
import { useTranslation } from 'react-i18next';
import type { ChannelState } from '@/types/channel';
import { CHANNEL_LIFECYCLE } from '@/domain/channel/lifecycle';

interface ChannelStateTagProps {
  state: ChannelState;
}

/**
 * 渠道状态标签组件
 *
 * <p>从 SSOT `CHANNEL_LIFECYCLE` 派生颜色与文案；状态字段、文案、视觉风格
 * 全部从 lifecycle 模块读取，禁止在本组件内重新定义状态映射。</p>
 *
 * <p>Tooltip 将在任务 6.4 中加入；本步骤仅完成 SSOT 替换，保持现有视觉。</p>
 *
 * @param state 渠道状态枚举
 */
const ChannelStateTag: React.FC<ChannelStateTagProps> = ({ state }) => {
  const { t } = useTranslation('channels');
  const meta = CHANNEL_LIFECYCLE[state] ?? CHANNEL_LIFECYCLE.SUSPENDED;
  // 背景色：在主色基础上加 26 ≈ 15% 透明度（保持原视觉风格）
  const bg = `${meta.color}1a`;

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
        color: meta.color,
        backgroundColor: bg,
        border: `1px solid ${meta.color}33`,
        fontWeight: 500,
      }}
    >
      {t(meta.label)}
    </span>
  );
};

export default ChannelStateTag;
