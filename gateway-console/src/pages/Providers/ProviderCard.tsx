import { useState } from 'react';
import { Card, Button, Space, Tag, Badge, Dropdown, theme, Tooltip } from 'antd';
import {
  PlusOutlined,
  DownOutlined,
  RightOutlined,
  EyeOutlined,
  MoreOutlined,
  ApiOutlined,
  AppstoreOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '@/stores/themeStore';
import { StatusIndicator } from '@/components/common';
import type { Provider } from '@/types/provider';
import type { ProviderApiKey } from '@/types/providerApiKey';
import type { Model } from '@/types/model';

const { useToken } = theme;

interface ProviderCardProps {
  provider: Provider;
  apiKeys: ProviderApiKey[];
  models: Model[];
  onViewDetail: (provider: Provider) => void;
  onDelete: (provider: Provider) => void;
  onAddApiKey: (provider: Provider) => void;
  onAddModel: (provider: Provider) => void;
  onEditApiKey: (key: ProviderApiKey) => void;
  onEditModel: (model: Model) => void;
}

/**
 * 供应商卡片组件
 * 展示供应商信息、API Keys 和模型列表
 */
export function ProviderCard({
  provider,
  apiKeys,
  models,
  onViewDetail,
  onDelete,
  onAddApiKey,
  onAddModel,
  onEditApiKey,
  onEditModel,
}: ProviderCardProps) {
  const { t } = useTranslation('providers');
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';
  const { token } = useToken();
  const [isExpanded, setIsExpanded] = useState(true);

  const activeKeys = apiKeys.filter((k) => k.state === 'ACTIVE');
  const activeModels = models.filter((m) => m.state === 'ACTIVE');

  const dropdownItems = [
    {
      key: 'view',
      label: t('detail.viewDetail', { defaultValue: '查看详情' }),
      icon: <EyeOutlined />,
      onClick: () => onViewDetail(provider),
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'delete',
      label: t('actions.delete', { ns: 'common' }),
      icon: <DeleteOutlined />,
      danger: true,
      onClick: () => onDelete(provider),
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
        cursor: 'pointer',
      }}
      styles={{
        body: { padding: 0, display: 'flex', flexDirection: 'column', height: '100%' },
      }}
      onDoubleClick={() => onViewDetail(provider)}
    >
      {/* 头部：供应商信息 */}
      <div
        style={{
          padding: '16px 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: isExpanded ? `1px solid ${token.colorBorderSecondary}` : 'none',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0, flex: 1 }}>
          <StatusIndicator status={provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} showLabel={false} />
          <Tooltip title={provider.providerName.length > 20 ? provider.providerName : undefined}>
            <span
              style={{
                fontSize: 16,
                fontWeight: 600,
                cursor: 'pointer',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                flex: 1,
                minWidth: 0,
              }}
              onClick={() => onViewDetail(provider)}
            >
              {provider.providerName}
            </span>
          </Tooltip>
        </div>
        <Space style={{ flexShrink: 0 }}>
          <Badge count={activeModels.length} showZero color="#52c41a" />
          <Dropdown menu={{ items: dropdownItems }} trigger={['click']}>
            <Button type="text" icon={<MoreOutlined />} />
          </Dropdown>
        </Space>
      </div>

      {/* 统计信息行 */}
      <div
        style={{
          padding: '12px 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          fontSize: 13,
          color: token.colorTextSecondary,
          borderBottom: isExpanded ? `1px solid ${token.colorBorderSecondary}` : 'none',
        }}
      >
        <Space split={<span style={{ color: token.colorBorder }}>|</span>} size={8}>
          <span>
            <ApiOutlined style={{ marginRight: 4 }} />
            {t('detail.keyCount', { defaultValue: 'Keys' })}: {activeKeys.length}/{apiKeys.length}
          </span>
          <span>
            <AppstoreOutlined style={{ marginRight: 4 }} />
            {t('detail.modelCount', { defaultValue: '模型' })}: {models.length}
          </span>
        </Space>
        <Button
          type="text"
          icon={isExpanded ? <DownOutlined /> : <RightOutlined />}
          onClick={() => setIsExpanded(!isExpanded)}
        >
          {isExpanded ? t('detail.collapse', { defaultValue: '收起' }) : t('detail.expand', { defaultValue: '展开' })}
        </Button>
      </div>

      {/* 展开内容：API Keys 和模型 */}
      {isExpanded && (
        <div
          style={{
            flex: 1,
            padding: '16px 24px',
            background: isDark ? token.colorBgContainer : token.colorBgLayout,
            overflow: 'auto',
          }}
        >
          {/* API Keys 区块 */}
          <div style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
              <span style={{ fontWeight: 500, fontSize: 14 }}>
                <ApiOutlined style={{ marginRight: 4 }} />
                {t('detail.apiKeys', { defaultValue: 'API Keys' })}
              </span>
              <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => onAddApiKey(provider)}>
                {t('actions.add', { ns: 'common' })}
              </Button>
            </div>
            {apiKeys.length === 0 ? (
              <div style={{ color: token.colorTextSecondary, fontSize: 13 }}>
                {t('empty.noApiKey', { defaultValue: '暂无 API Key' })}
              </div>
            ) : (
              <Space wrap size={[4, 4]}>
                {apiKeys.slice(0, 3).map((key) => (
                  <Tag
                    key={key.id}
                    color={key.state === 'ACTIVE' ? 'green' : 'default'}
                    style={{ cursor: 'pointer' }}
                    onClick={() => onEditApiKey(key)}
                  >
                    {key.keyName}
                  </Tag>
                ))}
                {apiKeys.length > 3 && (
                  <Tag>+{apiKeys.length - 3}</Tag>
                )}
              </Space>
            )}
          </div>

          {/* 模型区块 */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
              <span style={{ fontWeight: 500, fontSize: 14 }}>
                <AppstoreOutlined style={{ marginRight: 4 }} />
                {t('detail.models', { defaultValue: '模型' })}
              </span>
              <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => onAddModel(provider)}>
                {t('actions.add', { ns: 'common' })}
              </Button>
            </div>
            {models.length === 0 ? (
              <div style={{ color: token.colorTextSecondary, fontSize: 13 }}>
                {t('empty.noModel', { defaultValue: '暂无模型' })}
              </div>
            ) : (
              <Space wrap size={[4, 4]}>
                {models.slice(0, 5).map((model) => (
                  <Tag
                    key={model.id}
                    color={model.state === 'ACTIVE' ? 'blue' : 'default'}
                    style={{ cursor: 'pointer' }}
                    onClick={() => onEditModel(model)}
                  >
                    {model.displayName || model.providerModelId}
                  </Tag>
                ))}
                {models.length > 5 && (
                  <Tag>+{models.length - 5}</Tag>
                )}
              </Space>
            )}
          </div>
        </div>
      )}
    </Card>
  );
}

export type { ProviderCardProps };
