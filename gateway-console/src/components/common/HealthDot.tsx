/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useTranslation } from 'react-i18next';
import { Popover } from 'antd';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import type { ChannelHealthStatus, ChannelHealthSource } from '@/domain/channel/healthTypes';

dayjs.extend(relativeTime);

/**
 * 健康状态 → 填充色 / 边框色映射（design doc §4）。
 */
const STATUS_COLOR: Record<ChannelHealthStatus, { fill: string; border: string }> = {
  HEALTHY: { fill: '#52c41a', border: '#52c41a' },
  DEGRADED: { fill: '#faad14', border: '#faad14' },
  FAILED: { fill: '#ff4d4f', border: '#ff4d4f' },
  // UNKNOWN：空心，仅描边浅灰
  UNKNOWN: { fill: 'transparent', border: '#d9d9d9' },
};

/**
 * HealthDot 组件参数。
 */
export interface HealthDotProps {
  /** 健康状态；null/undefined 时按 UNKNOWN 渲染 */
  status?: ChannelHealthStatus | null;
  /** 最后一次健康检查时间（ISO 字符串） */
  lastCheckAt?: string | null;
  /** 触发来源（用于 Popover 显示翻译） */
  source?: ChannelHealthSource | null;
  /** 圆点直径（px），默认 8 */
  size?: number;
}

/**
 * 健康状态指示点。
 *
 * <p>渲染一个小圆点，颜色根据 status 派生：</p>
 * <ul>
 *   <li>HEALTHY → 绿（#52c41a）</li>
 *   <li>DEGRADED → 黄（#faad14）</li>
 *   <li>FAILED → 红（#ff4d4f）</li>
 *   <li>UNKNOWN / null → 空心（背景透明 + 浅灰描边）</li>
 * </ul>
 *
 * <p>悬浮 Popover 显示最后一次测试时间（dayjs 相对时间） + 来源中文翻译。
 * 若 lastCheckAt 为空，提示"尚未测试"。</p>
 */
export function HealthDot({
  status,
  lastCheckAt,
  source,
  size = 8,
}: HealthDotProps) {
  const { t, i18n } = useTranslation('channels');
  const effectiveStatus: ChannelHealthStatus = status ?? 'UNKNOWN';
  const color = STATUS_COLOR[effectiveStatus] ?? STATUS_COLOR.UNKNOWN;

  // Popover 内容：lastCheckAt + source 翻译；无时间则提示尚未测试
  const popoverContent = (() => {
    if (!lastCheckAt) {
      return <span>{t('health.unknown')}</span>;
    }
    const locale = i18n.language?.toLowerCase().startsWith('zh') ? 'zh-cn' : 'en';
    const relative = dayjs(lastCheckAt).locale(locale).fromNow();
    return (
      <div style={{ minWidth: 160 }}>
        <div>{t('health.lastCheckAt', { time: relative })}</div>
        {source && (
          <div style={{ color: '#8c8c8c', fontSize: 12, marginTop: 4 }}>
            {t(`health.source.${source}`)}
          </div>
        )}
      </div>
    );
  })();

  // border 同时用 backgroundColor=transparent 来表达空心
  const dotStyle: React.CSSProperties = {
    display: 'inline-block',
    width: size,
    height: size,
    borderRadius: '50%',
    backgroundColor: color.fill,
    border: `1px solid ${color.border}`,
    boxSizing: 'border-box',
    verticalAlign: 'middle',
    cursor: 'help',
  };

  return (
    <Popover content={popoverContent} placement="top" trigger={['hover', 'focus']}>
      <span data-testid="health-dot" tabIndex={0} style={dotStyle} />
    </Popover>
  );
}

export default HealthDot;
