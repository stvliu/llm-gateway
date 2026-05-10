import { Drawer, Descriptions, Tag, Button, Space, Divider, List, Spin, Empty, Card } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
  ApiOutlined,
  CloudServerOutlined,
  StarOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProvider, useModels, useProviderKeys } from '@/services/query';
import { StatusIndicator } from '@/components/common';
import type { Provider } from '@/types/provider';
import type { Model } from '@/types/model';
import type { ProviderApiKey } from '@/types/providerApiKey';

interface ProviderDrawerProps {
  providerId: number | null;
  onClose: () => void;
  onEdit: (provider: Provider) => void;
  onDelete: (provider: Provider) => void;
  onAddModel: (provider: Provider) => void;
  onEditModel: (model: Model) => void;
  onViewModelDetail: (model: Model) => void;
}

/**
 * Provider 详情抽屉
 */
export function ProviderDrawer({
  providerId,
  onClose,
  onEdit,
  onDelete,
  onAddModel,
  onEditModel,
  onViewModelDetail,
}: ProviderDrawerProps) {
  const { t } = useTranslation('models');

  const { data: provider, isLoading: providerLoading } = useProvider(providerId || 0);
  const { data: modelsData, isLoading: modelsLoading } = useModels(
    providerId ? { providerId, size: 100 } : { size: 100 }
  );
  const { data: keysData, isLoading: keysLoading } = useProviderKeys(providerId || 0);

  const models = modelsData?.items || [];
  const defaultKey = keysData?.defaultKey;
  const keys = keysData?.keys || [];

  if (!providerId) {
    return null;
  }

  const isLoading = providerLoading || modelsLoading || keysLoading;

  /**
   * 渲染 Key 状态
   */
  const renderKeyStatus = (key: ProviderApiKey) => {
    const statusMap: Record<string, { color: string; text: string }> = {
      ACTIVE: { color: '#52c41a', text: t('state.active', { ns: 'common' }) },
      DISABLED: { color: '#ff4d4f', text: t('state.disabled', { ns: 'common' }) },
      DELETED: { color: '#999', text: t('state.deleted', { ns: 'common' }) },
    };
    const status = statusMap[key.state] || { color: '#999', text: key.state };
    return <Tag color={status.color}>{status.text}</Tag>;
  };

  return (
    <Drawer
      title={
        <Space>
          <CloudServerOutlined />
          {t('detail.providerDetail')}
        </Space>
      }
      placement="right"
      width={480}
      onClose={onClose}
      open={!!providerId}
      extra={
        <Space>
          <Button icon={<EditOutlined />} onClick={() => provider && onEdit(provider)}>
            {t('actions.edit', { ns: 'common' })}
          </Button>
          <Button
            danger
            icon={<DeleteOutlined />}
            onClick={() => provider && onDelete(provider)}
          >
            {t('actions.delete', { ns: 'common' })}
          </Button>
        </Space>
      }
    >
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin />
        </div>
      ) : provider ? (
        <>
          {/* 基本信息 */}
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label={t('provider.name')}>
              <Space>
                <StatusIndicator status={provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} showLabel={false} />
                {provider.providerName}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label={t('provider.type')}>
              <Tag color="blue">{t(`type.${provider.providerType}`, { ns: 'providers' })}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label={t('provider.state')}>
              <StatusIndicator status={provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} />
            </Descriptions.Item>
            <Descriptions.Item label={t('provider.baseUrl')}>
              <code>{provider.baseUrl || '-'}</code>
            </Descriptions.Item>
            <Descriptions.Item label={t('detail.createdAt')}>
              {new Date(provider.createdAt).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label={t('detail.updatedAt')}>
              {new Date(provider.updatedAt).toLocaleString()}
            </Descriptions.Item>
          </Descriptions>

          <Divider>
            <Space>
              <ApiOutlined />
              {t('detail.defaultKey', { defaultValue: '默认 Key' })}
            </Space>
          </Divider>

          {/* 默认 Key 信息 */}
          {defaultKey ? (
            <Card size="small" style={{ marginBottom: 16 }}>
              <Space>
                <StarOutlined style={{ color: '#faad14' }} />
                <span style={{ fontWeight: 500 }}>{defaultKey.keyName}</span>
                {renderKeyStatus(defaultKey)}
                <span style={{ color: '#999', fontSize: 12 }}>
                  {defaultKey.keyHint}
                </span>
              </Space>
            </Card>
          ) : (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={t('detail.noDefaultKey', { defaultValue: '未设置默认 Key' })}
            />
          )}

          <Divider>
            <Space>
              <ApiOutlined />
              {t('detail.keyList', { defaultValue: 'Key 列表' })} ({keys.length})
            </Space>
          </Divider>

          {/* Key 列表 */}
          {keys.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={t('detail.noKey', { defaultValue: '暂无 Key' })}
            />
          ) : (
            <List
              dataSource={keys}
              renderItem={(key) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={
                      key.isDefault ? (
                        <StarOutlined style={{ color: '#faad14', fontSize: 16 }} />
                      ) : null
                    }
                    title={
                      <Space>
                        <span>{key.keyName}</span>
                        {renderKeyStatus(key)}
                      </Space>
                    }
                    description={
                      <Space split={<span style={{ color: '#d9d9d9' }}>|</span>} size={4}>
                        <span style={{ fontSize: 12, color: '#999' }}>{key.keyHint}</span>
                        {key.priority && <span style={{ fontSize: 12 }}>P{key.priority}</span>}
                        {key.rpmLimit && <span style={{ fontSize: 12 }}>{key.rpmLimit} RPM</span>}
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          )}

          <Divider>
            <Space>
              <ApiOutlined />
              {t('detail.modelList')} ({models.length})
            </Space>
          </Divider>

          {/* Model 列表 */}
          {models.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={t('empty.noModel')}
            >
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => onAddModel(provider)}
              >
                {t('addModel')}
              </Button>
            </Empty>
          ) : (
            <List
              dataSource={models}
              renderItem={(model) => (
                <List.Item
                  actions={[
                    <Button
                      key="edit"
                      type="link"
                      size="small"
                      onClick={() => onEditModel(model)}
                    >
                      {t('actions.edit', { ns: 'common' })}
                    </Button>,
                    <Button
                      key="view"
                      type="link"
                      size="small"
                      onClick={() => onViewModelDetail(model)}
                    >
                      {t('detail.viewDetail')}
                    </Button>,
                  ]}
                >
                  <List.Item.Meta
                    avatar={<StatusIndicator status={model.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} showLabel={false} />}
                    title={
                      <a onClick={() => onViewModelDetail(model)}>{model.displayName || model.providerModelId || `Model ${model.id}`}</a>
                    }
                    description={
                      <Space split={<span style={{ color: '#d9d9d9' }}>|</span>} size={4}>
                        <span>{model.providerModelId || '-'}</span>
                        <Tag color="blue" style={{ margin: 0 }}>
                          {model.providerName}
                        </Tag>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          )}

          <Divider />

          <Button
            type="dashed"
            block
            icon={<PlusOutlined />}
            onClick={() => onAddModel(provider)}
          >
            {t('addModel')}
          </Button>
        </>
      ) : (
        <Empty description={t('detail.notFound')} />
      )}
    </Drawer>
  );
}