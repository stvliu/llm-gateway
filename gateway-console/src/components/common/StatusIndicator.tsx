import { Tooltip } from 'antd';
import { useTranslation } from 'react-i18next';

export type StatusType = 'ACTIVE' | 'DISABLED' | 'LOCKED';

interface StatusIndicatorProps {
  status: StatusType;
  showLabel?: boolean;
  size?: 'small' | 'default';
}

/**
 * 状态指示器组件
 * - 活跃：绿色脉动动画
 * - 禁用：灰色静止
 * - 锁定：红色静止
 */
export function StatusIndicator({
  status,
  showLabel = true,
  size = 'default',
}: StatusIndicatorProps) {
  const { t } = useTranslation('models');

  const getStatusConfig = () => {
    switch (status) {
      case 'ACTIVE':
        return {
          color: 'green',
          label: t('state.active', { ns: 'common' }),
          pulse: true,
        };
      case 'DISABLED':
        return {
          color: 'default',
          label: t('state.disabled', { ns: 'common' }),
          pulse: false,
        };
      case 'LOCKED':
        return {
          color: 'red',
          label: t('state.locked', { ns: 'common' }),
          pulse: false,
        };
      default:
        return {
          color: 'default',
          label: status,
          pulse: false,
        };
    }
  };

  const config = getStatusConfig();
  const dotSize = size === 'small' ? 6 : 8;

  const getColorCode = () => {
    switch (config.color) {
      case 'green':
        return '#52c41a';
      case 'red':
        return '#ff4d4f';
      default:
        return '#d9d9d9';
    }
  };

  return (
    <Tooltip title={showLabel ? undefined : config.label}>
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 6,
        }}
      >
        <span
          style={{
            width: dotSize,
            height: dotSize,
            borderRadius: '50%',
            backgroundColor: getColorCode(),
            animation: config.pulse ? 'pulse 2s infinite' : 'none',
          }}
        />
        {showLabel && (
          <span style={{ fontSize: size === 'small' ? 12 : 14 }}>
            {config.label}
          </span>
        )}
        <style>{`
          @keyframes pulse {
            0%, 100% {
              opacity: 1;
              transform: scale(1);
            }
            50% {
              opacity: 0.7;
              transform: scale(1.1);
            }
          }
        `}</style>
      </span>
    </Tooltip>
  );
}