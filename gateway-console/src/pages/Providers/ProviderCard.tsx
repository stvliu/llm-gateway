import { Card, Tag, Typography, Space, Tooltip, Dropdown } from 'antd';
import { GlobalOutlined, LinkOutlined, AppstoreOutlined, RobotOutlined, MoreOutlined, EyeOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { useTranslation } from 'react-i18next';
import { useChannels } from '@/services/query/useChannels';
import { useModelSpecs } from '@/services/query/useModelSpecs';
import { ProviderIcon } from '@/components/ui';
import type { Provider } from '@/types/provider';

const { Text, Paragraph } = Typography;

interface Props {
  provider: Provider;
  onView: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onViewChannels?: () => void;
}

export default function ProviderCard({ provider, onView, onEdit, onDelete, onViewChannels }: Props) {
  const { t } = useTranslation('providers');

  const providerId = provider?.id ?? 0;
  const { data: channels, isLoading } = useChannels(providerId);
  const { data: modelSpecs, isLoading: modelSpecsLoading } = useModelSpecs();

  const activeChannels = channels?.filter(c => c.state?.toUpperCase() === 'ACTIVE') || [];
  const displayChannels = activeChannels.slice(0, 3);
  const remainingCount = activeChannels.length - displayChannels.length;

  const activeModelSpecs = modelSpecs?.filter(s => s.state?.toUpperCase() === 'ACTIVE') || [];
  const displayModelSpecs = activeModelSpecs.slice(0, 3);
  const remainingModels = activeModelSpecs.length - displayModelSpecs.length;

  const menuItems: MenuProps['items'] = [
    {
      key: 'view',
      label: t('actions.view', { defaultValue: 'View' }),
      icon: <EyeOutlined />,
      onClick: (e) => {
        e.domEvent.stopPropagation();
        onView();
      },
    },
    {
      key: 'edit',
      label: t('actions.edit', { defaultValue: 'Edit' }),
      icon: <EditOutlined />,
      onClick: (e) => {
        e.domEvent.stopPropagation();
        onEdit();
      },
    },
    { type: 'divider' },
    {
      key: 'delete',
      label: t('actions.delete', { defaultValue: 'Delete' }),
      icon: <DeleteOutlined />,
      danger: true,
      onClick: (e) => {
        e.domEvent.stopPropagation();
        onDelete();
      },
    },
  ];

  const stateConfig: Record<string, { color: string; label: string }> = {
    ACTIVE: { color: 'success', label: t('state.active', { defaultValue: 'Active' }) },
    INACTIVE: { color: 'warning', label: t('state.inactive', { defaultValue: 'Inactive' }) },
  };
  const currentState = stateConfig[provider.state?.toUpperCase() as keyof typeof stateConfig] || { color: 'default', label: provider.state || 'Unknown' };

  return (
    <Card
      hoverable
      onClick={onView}
      style={{ height: '100%' }}
      title={
        <Space>
          <ProviderIcon providerId={provider.providerId} size={24} />
          <Text strong>{provider.providerName}</Text>
          <Tag color={currentState.color}>{currentState.label}</Tag>
        </Space>
      }
      extra={
        <Dropdown
          menu={{ items: menuItems }}
          trigger={['click']}
          placement="bottomRight"
        >
          <Tag
            onClick={(e) => e.stopPropagation()}
            style={{ cursor: 'pointer', marginRight: 0 }}
          >
            <MoreOutlined />
          </Tag>
        </Dropdown>
      }
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">

        {provider.description && (
          <Tooltip title={provider.description}>
            <Text type="secondary" ellipsis style={{ maxWidth: 200 }}>
              {provider.description}
            </Text>
          </Tooltip>
        )}

        {provider.websiteUrl && (
          <Tooltip title={provider.websiteUrl}>
            <Text type="secondary" ellipsis style={{ maxWidth: 200 }}>
              <GlobalOutlined /> {provider.websiteUrl}
            </Text>
          </Tooltip>
        )}

        {provider.apiDocUrl && (
          <Paragraph type="secondary" ellipsis style={{ maxWidth: 200, marginBottom: 0 }}>
            <LinkOutlined /> {provider.apiDocUrl}
          </Paragraph>
        )}

        {/* Channels section */}
        <div style={{ marginTop: 8 }}>
          <div style={{ marginBottom: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
            <AppstoreOutlined />
            <Text type="secondary">
              {t('card.channels', { defaultValue: 'Channels' })} ({activeChannels.length})
            </Text>
          </div>
          {isLoading ? (
            <Text type="secondary" style={{ fontSize: 12 }}>...</Text>
          ) : activeChannels.length === 0 ? (
            <Text type="secondary" style={{ fontSize: 12 }}>
              {t('card.noChannels', { defaultValue: 'No channels' })}
            </Text>
          ) : (
            <Space wrap size={[4, 4]}>
              {displayChannels.map((channel) => (
                <Tag
                  key={channel.id}
                  color="blue"
                  style={{ cursor: 'pointer' }}
                  onClick={(e) => {
                    e.stopPropagation();
                    onViewChannels?.();
                  }}
                >
                  {channel.name}
                </Tag>
              ))}
              {remainingCount > 0 && (
                <Tag>+{remainingCount}</Tag>
              )}
            </Space>
          )}
        </div>

        {/* Models section */}
        <div style={{ marginTop: 8 }}>
          <div style={{ marginBottom: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
            <RobotOutlined />
            <Text type="secondary">
              {t('card.models', { defaultValue: 'Models' })} ({activeModelSpecs.length})
            </Text>
          </div>
          {modelSpecsLoading ? (
            <Text type="secondary" style={{ fontSize: 12 }}>...</Text>
          ) : activeModelSpecs.length === 0 ? (
            <Text type="secondary" style={{ fontSize: 12 }}>
              {t('card.noModels', { defaultValue: 'No models' })}
            </Text>
          ) : (
            <Space wrap size={[4, 4]}>
              {displayModelSpecs.map((spec) => (
                <Tag key={spec.id} color="purple" style={{ cursor: 'pointer' }}>
                  {spec.displayName || spec.providerModelId}
                </Tag>
              ))}
              {remainingModels > 0 && (
                <Tag>+{remainingModels}</Tag>
              )}
            </Space>
          )}
        </div>
      </Space>
    </Card>
  );
}