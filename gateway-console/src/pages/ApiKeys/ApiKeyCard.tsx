import { Card, Button, Space, Dropdown, Typography, theme, Tooltip, Tag } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  CopyOutlined,
  MoreOutlined,
  ApiOutlined,
  ClockCircleOutlined,
  BarChartOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '@/stores/themeStore';
import { StatusIndicator, type StatusType } from '@/components/common';
import type { ApiKey, ApiKeyUsage } from '@/types/apiKey';

const { Paragraph } = Typography;
const { useToken } = theme;

interface ApiKeyCardProps {
  apiKey: ApiKey;
  usage?: ApiKeyUsage;
  onEdit: (apiKey: ApiKey) => void;
  onDelete: (id: number) => void;
  onToggleEnabled: (id: number, enabled: boolean) => void;
}

/**
 * 计算密钥状态
 */
function getKeyStatus(apiKey: ApiKey): StatusType {
  if (apiKey.state !== 'ACTIVE') return 'DISABLED';
  if (apiKey.expiresAt) {
    const expiresAt = new Date(apiKey.expiresAt);
    const now = new Date();
    if (now > expiresAt) return 'EXPIRED';
    const sevenDaysLater = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    if (expiresAt < sevenDaysLater) return 'EXPIRING';
  }
  return 'ACTIVE';
}

/**
 * 计算剩余天数
 */
function getDaysRemaining(expiresAt: string): number {
  const expires = new Date(expiresAt);
  const now = new Date();
  const diffMs = expires.getTime() - now.getTime();
  return Math.ceil(diffMs / (1000 * 60 * 60 * 24));
}

/**
 * 格式化数字（带千分位）
 */
function formatNumber(num: number): string {
  return num.toLocaleString('zh-CN');
}

/**
 * API Key 卡片组件
 */
export function ApiKeyCard({
  apiKey,
  usage,
  onEdit,
  onDelete,
  onToggleEnabled,
}: ApiKeyCardProps) {
  const { t } = useTranslation('apiKeys');
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';
  const { token } = useToken();

  const status = getKeyStatus(apiKey);
  const isEnabled = apiKey.state === 'ACTIVE';

  const dropdownItems = [
    {
      key: 'toggle',
      label: isEnabled
        ? t('toggleDisabled', { defaultValue: '禁用' })
        : t('toggleEnabled', { defaultValue: '启用' }),
      onClick: () => onToggleEnabled(apiKey.id, !isEnabled),
    },
    {
      key: 'edit',
      label: t('actions.edit', { ns: 'common' }),
      icon: <EditOutlined />,
      onClick: () => onEdit(apiKey),
    },
    { type: 'divider' as const },
    {
      key: 'delete',
      label: t('actions.delete', { ns: 'common' }),
      icon: <DeleteOutlined />,
      danger: true,
      onClick: () => onDelete(apiKey.id),
    },
  ];

  return (
    <Card
      style={{
        height: '100%',
        border: 'none',
        boxShadow: isDark
          ? '0 2px 8px rgba(0, 0, 0, 0.3)'
          : '0 2px 8px rgba(0, 0, 0, 0.06)',
        transition: 'all 0.3s',
      }}
      styles={{
        body: { padding: 0, display: 'flex', flexDirection: 'column' },
      }}
    >
      {/* 头部：状态 + 名称 + 操作 */}
      <div
        style={{
          padding: '16px 20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0, flex: 1 }}>
          <StatusIndicator status={status} showLabel={false} />
          <Tooltip title={apiKey.name.length > 20 ? apiKey.name : undefined}>
            <span
              style={{
                fontSize: 15,
                fontWeight: 600,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                flex: 1,
                minWidth: 0,
              }}
            >
              {apiKey.name}
            </span>
          </Tooltip>
        </div>
        <Dropdown menu={{ items: dropdownItems }} trigger={['click']}>
          <Button type="text" icon={<MoreOutlined />} />
        </Dropdown>
      </div>

      {/* Key 值行 */}
      <div
        style={{
          padding: '12px 20px',
          background: isDark ? token.colorBgContainer : token.colorBgLayout,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        <ApiOutlined style={{ color: token.colorTextSecondary }} />
        <Paragraph
          copyable={apiKey.key ? {
            text: apiKey.key,
            tooltips: [t('copy', { defaultValue: '复制' }), t('copied', { defaultValue: '已复制' })],
            icon: [<CopyOutlined key="copy" />, <CopyOutlined key="copied" style={{ color: token.colorSuccess }} />],
          } : false}
          style={{
            margin: 0,
            fontFamily: 'monospace',
            fontSize: 13,
            color: token.colorTextSecondary,
            flex: 1,
          }}
          ellipsis
        >
          {apiKey.key || '-'}
        </Paragraph>
      </div>

      {/* 统计信息行 */}
      <div
        style={{
          padding: '12px 20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          fontSize: 13,
          color: token.colorTextSecondary,
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <Space split={<span style={{ color: token.colorBorder }}>|</span>} size={8}>
          <span>
            <BarChartOutlined style={{ marginRight: 4 }} />
            {usage ? formatNumber(usage.totalCalls) : 0} {t('usage.totalCalls', { defaultValue: '次调用' })}
          </span>
          <span>
            {usage ? formatNumber(usage.totalTokens) : 0} {t('usage.totalTokens', { defaultValue: 'Tokens' })}
          </span>
        </Space>
      </div>

      {/* 过期时间行 */}
      <div
        style={{
          padding: '12px 20px',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          fontSize: 13,
        }}
      >
        <ClockCircleOutlined style={{ color: token.colorTextSecondary }} />
        {apiKey.expiresAt ? (
          status === 'EXPIRED' ? (
            <Tag color="error">{t('expired', { defaultValue: '已过期' })}</Tag>
          ) : status === 'EXPIRING' ? (
            <Tag color="warning">
              {t('expiresIn', { defaultValue: '{{count}} 天后过期', count: getDaysRemaining(apiKey.expiresAt) })}
            </Tag>
          ) : (
            <span style={{ color: token.colorTextSecondary }}>
              {t('expiresIn', { defaultValue: '{{count}} 天后过期', count: getDaysRemaining(apiKey.expiresAt) })}
            </span>
          )
        ) : (
          <span style={{ color: token.colorTextSecondary }}>
            {t('expiresNever', { defaultValue: '永不过期' })}
          </span>
        )}
        {apiKey.lastUsedAt && (
          <span style={{ color: token.colorTextTertiary, marginLeft: 'auto', fontSize: 12 }}>
            {t('lastUsed', { defaultValue: '最后使用' })}: {new Date(apiKey.lastUsedAt).toLocaleDateString('zh-CN')}
          </span>
        )}
      </div>

      {/* 操作按钮行 */}
      <div
        style={{
          padding: '12px 20px',
          display: 'flex',
          justifyContent: 'flex-end',
          gap: 8,
        }}
      >
        <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(apiKey)}>
          {t('actions.edit', { ns: 'common' })}
        </Button>
        <Button size="small" danger icon={<DeleteOutlined />} onClick={() => onDelete(apiKey.id)}>
          {t('actions.delete', { ns: 'common' })}
        </Button>
      </div>
    </Card>
  );
}

export type { ApiKeyCardProps };
