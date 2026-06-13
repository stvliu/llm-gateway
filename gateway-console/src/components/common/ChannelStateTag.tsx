import React from 'react';
import { Tooltip } from 'antd';
import { useTranslation } from 'react-i18next';
import type { ChannelState } from '@/types/channel';
import { CHANNEL_LIFECYCLE, buildStateTooltip } from '@/domain/channel/lifecycle';

interface ChannelStateTagProps {
  state: ChannelState;
}

/**
 * 渠道状态标签组件
 *
 * <p>从 SSOT `CHANNEL_LIFECYCLE` 派生颜色与文案；状态字段、文案、视觉风格
 * 全部从 lifecycle 模块读取，禁止在本组件内重新定义状态映射。</p>
 *
 * <p>外层包裹 antd Tooltip，hover 展示由 buildStateTooltip 派生的多行说明：
 * 描述 + 是否参与流量 + 是否计费 + 可后继状态 / 终态文案。</p>
 *
 * @param state 渠道状态枚举
 */
const ChannelStateTag: React.FC<ChannelStateTagProps> = ({ state }) => {
  const { t } = useTranslation('channels');
  const meta = CHANNEL_LIFECYCLE[state] ?? CHANNEL_LIFECYCLE.SUSPENDED;
  // 背景色：在主色基础上叠加 ~10% 透明度（与原 STATE_CONFIG 视觉风格保持一致）
  const bg = `${meta.color}1a`;
  // Tooltip 文本：多行字符串（含 \n），渲染时用 white-space: pre-line 保留换行
  const tooltipText = buildStateTooltip(state, t);

  return (
    <Tooltip
      title={<span style={{ whiteSpace: 'pre-line' }}>{tooltipText}</span>}
    >
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
          cursor: 'help',
        }}
      >
        {t(meta.label)}
      </span>
    </Tooltip>
  );
};

export default ChannelStateTag;
