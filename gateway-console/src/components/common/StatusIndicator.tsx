import { Tooltip, theme } from 'antd';
import { useTranslation } from 'react-i18next';

export type StatusType = 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'EXPIRING' | 'EXPIRED';

interface StatusIndicatorProps {
  status: StatusType;
  showLabel?: boolean;
  size?: 'small' | 'default';
}

/**
 * 状态指示器组件
 */
export function StatusIndicator({
  status,
  showLabel = true,
  size = 'default',
}: StatusIndicatorProps) {
  const { t } = useTranslation('models');
  const { token } = theme.useToken();

  const getStatusConfig = () => {
    switch (status) {
      case 'ACTIVE':
        return {
          color: token.colorSuccess,
          label: t('state.active', { ns: 'common' }),
          pulse: true,
        };
      case 'INACTIVE':
        return {
          color: token.colorTextDisabled,
          label: t('state.disabled', { ns: 'common' }),
          pulse: false,
        };
      case 'LOCKED':
        return {
          color: token.colorError,
          label: t('state.locked', { ns: 'common' }),
          pulse: false,
        };
      case 'EXPIRING':
        return {
          color: token.colorWarning,
          label: t('state.expiring', { ns: 'common', defaultValue: '即将过期' }),
          pulse: true,
        };
      case 'EXPIRED':
        return {
          color: token.colorError,
          label: t('state.expired', { ns: 'common', defaultValue: '已过期' }),
          pulse: false,
        };
      default:
        return {
          color: token.colorTextDisabled,
          label: status,
          pulse: false,
        };
    }
  };

  const config = getStatusConfig();
  const dotSize = size === 'small' ? 6 : 8;

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
            backgroundColor: config.color,
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
