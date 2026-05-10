import { useState } from 'react';
import { Card, Button, Space, Tag, Badge, Dropdown, theme } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
  DownOutlined,
  RightOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '@/stores/themeStore';
import { StatusIndicator } from '@/components/common';
import { ModelTag } from './ModelTag';
import type { Provider } from '@/types/provider';
import type { Model } from '@/types/model';

const { useToken } = theme;

interface ProviderCardProps {
  provider: Provider;
  models: Model[];
  collapsed?: boolean;
  onEdit: (provider: Provider) => void;
  onDelete: (provider: Provider) => void;
  onAddModel: (provider: Provider) => void;
  onEditModel: (model: Model) => void;
  onDeleteModel: (model: Model) => void;
  onViewProviderDetail: (provider: Provider) => void;
  onViewModelDetail: (model: Model) => void;
}

/**
 * Provider 卡片组件
 * 展示 Provider 信息和关联的 Model 列表
 */
export function ProviderCard({
  provider,
  models,
  collapsed = false,
  onEdit,
  onDelete,
  onAddModel,
  onEditModel,
  onDeleteModel,
  onViewProviderDetail,
  onViewModelDetail,
}: ProviderCardProps) {
  const { t } = useTranslation('models');
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';
  const { token } = useToken();
  const [isExpanded, setIsExpanded] = useState(!collapsed);

  const activeModels = models.filter((m) => m.state === 'ACTIVE');

  const dropdownItems = [
    {
      key: 'view',
      label: t('detail.viewDetail'),
      icon: <InfoCircleOutlined />,
      onClick: () => onViewProviderDetail(provider),
    },
    {
      key: 'edit',
      label: t('actions.edit', { ns: 'common' }),
      icon: <EditOutlined />,
      onClick: () => onEdit(provider),
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
        marginBottom: 16,
        border: 'none',
        boxShadow: isDark
          ? '0 2px 8px rgba(0, 0, 0, 0.3)'
          : '0 2px 8px rgba(0, 0, 0, 0.06)',
        transition: 'all 0.3s',
      }}
      styles={{
        body: { padding: 0 },
      }}
    >
      {/* Provider 头部 */}
      <div
        style={{
          padding: '16px 20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: isExpanded ? `1px solid ${token.colorBorderSecondary}` : 'none',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <StatusIndicator status={provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} showLabel={false} />
          <span
            style={{
              fontSize: 16,
              fontWeight: 600,
              cursor: 'pointer',
            }}
            onClick={() => onViewProviderDetail(provider)}
          >
            {provider.providerName}
          </span>
          <Tag color="blue" style={{ marginLeft: 4 }}>
            {t(`type.${provider.providerType}`, { ns: 'providers' })}
          </Tag>
        </div>

        <Space>
          <Badge
            count={activeModels.length}
            showZero
            color="#52c41a"
            title={t('detail.activeModelCount')}
          />
          <Dropdown menu={{ items: dropdownItems }} trigger={['click']}>
            <Button type="text" icon={<EditOutlined />} />
          </Dropdown>
        </Space>
      </div>

      {/* Provider 详情行 */}
      <div
        style={{
          padding: '12px 20px',
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
            {t('provider.type')}: {t(`type.${provider.providerType}`, { ns: 'providers' })}
          </span>
          <span>
            {t('provider.state')}: <StatusIndicator status={provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} size="small" />
          </span>
          <span>
            {t('detail.modelCount')}: {models.length}
          </span>
          <span>
            🔑 Key: {provider.keyStats?.activeCount ?? 0}/{provider.keyStats?.totalCount ?? 0} {t('detail.keyActive', { defaultValue: '活跃' })}
          </span>
        </Space>

        <Button
          type="text"
          icon={isExpanded ? <DownOutlined /> : <RightOutlined />}
          onClick={() => setIsExpanded(!isExpanded)}
        >
          {isExpanded ? t('detail.collapse') : t('detail.expand')}
        </Button>
      </div>

      {/* Model 列表 */}
      {isExpanded && (
        <div
          style={{
            padding: '16px 20px',
            background: isDark ? token.colorBgContainer : token.colorBgLayout,
          }}
        >
          {models.length === 0 ? (
            <div
              style={{
                textAlign: 'center',
                padding: 24,
                color: token.colorTextSecondary,
              }}
            >
              <p style={{ margin: '0 0 16px' }}>{t('empty.noModel')}</p>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => onAddModel(provider)}
              >
                {t('addModel')}
              </Button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center' }}>
              {models.map((model) => (
                <ModelTag
                  key={model.id}
                  model={model}
                  onEdit={onEditModel}
                  onDelete={onDeleteModel}
                  onViewDetail={onViewModelDetail}
                />
              ))}
              <Button
                type="dashed"
                icon={<PlusOutlined />}
                size="small"
                style={{ margin: '4px' }}
                onClick={() => onAddModel(provider)}
              >
                {t('addModel')}
              </Button>
            </div>
          )}
        </div>
      )}
    </Card>
  );
}