import { useMemo, useCallback } from 'react';
import { Card, Button, Space, Tag, Empty, Spin, Popconfirm, List, Divider, message } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
  ApiOutlined,
  AppstoreOutlined,
  StarFilled,
  MessageOutlined,
  PictureOutlined,
  AudioOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '@/stores/themeStore';
import { StatusIndicator } from '@/components/common';
import {
  useProviderKeys,
  useModels,
  useDeleteProviderApiKey,
  useDeleteModel,
} from '@/services/query';
import type { Provider } from '@/types/provider';
import type { Model } from '@/types/model';

interface ProviderDetailProps {
  provider: Provider | null;
  onEditProvider: (provider: Provider) => void;
  onDeleteProvider: (provider: Provider) => void;
  onAddApiKey: () => void;
  onEditApiKey: (key: { id: number; keyName: string; priority: number; weight: number; isDefault: boolean }) => void;
  onAddModel: () => void;
  onEditModel: (model: Model) => void;
}

/**
 * 根据模型能力推断类型
 */
function inferModelType(model: Model): string {
  if (model.capabilities) {
    if (model.capabilities.chat) return 'CHAT';
    if (model.capabilities.embedding) return 'EMBEDDING';
    if (model.capabilities.image) return 'IMAGE';
    if (model.capabilities.audio) return 'AUDIO';
  }
  return 'CHAT';
}

/**
 * 获取模型类型图标
 */
function getModelTypeIcon(type: string) {
  const iconMap: Record<string, React.ReactNode> = {
    CHAT: <MessageOutlined />,
    EMBEDDING: <ApiOutlined />,
    IMAGE: <PictureOutlined />,
    AUDIO: <AudioOutlined />,
  };
  return iconMap[type] || <ApiOutlined />;
}

/**
 * 获取模型类型颜色
 */
function getModelTypeColor(type: string): string {
  const colorMap: Record<string, string> = {
    CHAT: 'blue',
    EMBEDDING: 'purple',
    IMAGE: 'orange',
    AUDIO: 'green',
  };
  return colorMap[type] || 'default';
}

/**
 * 格式化上下文窗口
 */
function formatContextWindow(contextWindow?: number): string {
  if (!contextWindow) return '-';
  if (contextWindow >= 1000000) return `${(contextWindow / 1000000).toFixed(1)}M`;
  if (contextWindow >= 1000) return `${Math.round(contextWindow / 1000)}K`;
  return contextWindow.toString();
}

/**
 * 供应商详情面板
 * 显示选中供应商的 API Keys 和模型列表
 */
export function ProviderDetail({
  provider,
  onEditProvider,
  onDeleteProvider,
  onAddApiKey,
  onEditApiKey,
  onAddModel,
  onEditModel,
}: ProviderDetailProps) {
  const { t } = useTranslation('models');
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  // 查询数据
  const { data: keysData, isLoading: keysLoading } = useProviderKeys(provider?.id || 0, { enabled: !!provider });
  const { data: modelsData, isLoading: modelsLoading } = useModels({ providerId: provider?.id, size: 100 }, { enabled: !!provider });

  // Mutations
  const deleteKeyMutation = useDeleteProviderApiKey();
  const deleteModelMutation = useDeleteModel();

  const keys = keysData?.keys || [];
  const models = modelsData?.items || [];

  // 按默认和优先级排序 Keys
  const sortedKeys = useMemo(() => {
    return [...keys].sort((a, b) => {
      if (a.isDefault && !b.isDefault) return -1;
      if (!a.isDefault && b.isDefault) return 1;
      return (b.priority || 0) - (a.priority || 0);
    });
  }, [keys]);

  // 删除 API Key
  const handleDeleteKey = useCallback(async (keyId: number) => {
    await deleteKeyMutation.mutateAsync(keyId);
    message.success(t('message.success', { ns: 'common' }));
  }, [deleteKeyMutation, t]);

  // 删除模型
  const handleDeleteModel = useCallback(async (modelId: number) => {
    await deleteModelMutation.mutateAsync(modelId);
    message.success(t('message.success', { ns: 'common' }));
  }, [deleteModelMutation, t]);

  if (!provider) {
    return (
      <Card
        style={{
          height: '100%',
          border: 'none',
          boxShadow: isDark ? '0 2px 8px rgba(0, 0, 0, 0.3)' : '0 2px 8px rgba(0, 0, 0, 0.06)',
        }}
        styles={{ body: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' } }}
      >
        <Empty description={t('provider.selectHint', { defaultValue: '请选择一个供应商' })} />
      </Card>
    );
  }

  const isLoading = keysLoading || modelsLoading;

  return (
    <Card
      style={{
        height: '100%',
        border: 'none',
        boxShadow: isDark ? '0 2px 8px rgba(0, 0, 0, 0.3)' : '0 2px 8px rgba(0, 0, 0, 0.06)',
      }}
      styles={{ body: { padding: 0, height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' } }}
    >
      {/* 头部：供应商名称和操作 */}
      <div
        style={{
          padding: '16px 24px',
          borderBottom: `1px solid ${isDark ? '#303030' : '#f0f0f0'}`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <StatusIndicator status={provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} showLabel={false} />
          <span style={{ fontSize: 18, fontWeight: 600 }}>{provider.providerName}</span>
          <Tag color="blue">
            {t(`type.${provider.providerType}`, { ns: 'providers', defaultValue: provider.providerType })}
          </Tag>
        </div>
        <Space>
          <Button icon={<EditOutlined />} onClick={() => onEditProvider(provider)}>
            {t('actions.edit', { ns: 'common' })}
          </Button>
          <Popconfirm
            title={t('confirm.delete', { ns: 'common' })}
            description={t('confirm.deleteProviderDesc', { name: provider.providerName })}
            onConfirm={() => onDeleteProvider(provider)}
          >
            <Button danger icon={<DeleteOutlined />} loading={false}>
              {t('actions.delete', { ns: 'common' })}
            </Button>
          </Popconfirm>
        </Space>
      </div>

      {/* 内容区域 */}
      <div style={{ flex: 1, overflow: 'auto', padding: 24 }}>
        {isLoading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}>
            <Spin />
          </div>
        ) : (
          <>
            {/* API Keys 区域 */}
            <div style={{ marginBottom: 24 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <ApiOutlined />
                  <span style={{ fontWeight: 600, fontSize: 15 }}>
                    {t('provider.apiKeys', { defaultValue: 'API Keys' })}
                  </span>
                  <Tag color="blue">{keys.length}</Tag>
                </div>
                <Button type="primary" ghost icon={<PlusOutlined />} onClick={onAddApiKey}>
                  {t('actions.add', { ns: 'common' })}
                </Button>
              </div>

              {keys.length === 0 ? (
                <Empty description={t('provider.noApiKeys', { defaultValue: '暂无 API Key' })} />
              ) : (
                <List
                  size="small"
                  dataSource={sortedKeys}
                  renderItem={(key) => (
                    <List.Item
                      style={{
                        padding: '12px 16px',
                        background: isDark ? '#1f1f1f' : '#fafafa',
                        borderRadius: 8,
                        marginBottom: 8,
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
                        <Space>
                          {key.isDefault && <StarFilled style={{ color: '#faad14' }} />}
                          <span style={{ fontWeight: 500 }}>{key.keyName}</span>
                          {key.keyHint && <Tag>{key.keyHint}</Tag>}
                          <Tag color={key.state === 'ACTIVE' ? 'green' : 'default'}>
                            {key.state === 'ACTIVE' ? t('state.active', { ns: 'common' }) : t('state.disabled', { ns: 'common' })}
                          </Tag>
                        </Space>
                        <Space>
                          <span style={{ fontSize: 12, color: '#999' }}>
                            {t('provider.priority', { defaultValue: '优先级' })}: {key.priority || 100}
                          </span>
                          <Button
                            type="text"
                            size="small"
                            icon={<EditOutlined />}
                            onClick={() => onEditApiKey({
                              id: key.id,
                              keyName: key.keyName,
                              priority: key.priority || 100,
                              weight: key.weight || 100,
                              isDefault: key.isDefault || false,
                            })}
                          />
                          <Popconfirm
                            title={t('confirm.delete', { ns: 'common' })}
                            onConfirm={() => handleDeleteKey(key.id)}
                          >
                            <Button
                              type="text"
                              size="small"
                              danger
                              icon={<DeleteOutlined />}
                            />
                          </Popconfirm>
                        </Space>
                      </div>
                    </List.Item>
                  )}
                />
              )}
            </div>

            <Divider />

            {/* 模型列表区域 */}
            <div>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <AppstoreOutlined />
                  <span style={{ fontWeight: 600, fontSize: 15 }}>
                    {t('provider.models', { defaultValue: '模型' })}
                  </span>
                  <Tag color="blue">{models.length}</Tag>
                </div>
                <Button type="primary" ghost icon={<PlusOutlined />} onClick={onAddModel}>
                  {t('actions.add', { ns: 'common' })}
                </Button>
              </div>

              {models.length === 0 ? (
                <Empty description={t('provider.noModels', { defaultValue: '暂无模型' })} />
              ) : (
                <List
                  size="small"
                  dataSource={models}
                  renderItem={(model) => {
                    const modelType = inferModelType(model);
                    return (
                      <List.Item
                        style={{
                          padding: '12px 16px',
                          background: isDark ? '#1f1f1f' : '#fafafa',
                          borderRadius: 8,
                          marginBottom: 8,
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
                          <Space>
                            <Tag color={getModelTypeColor(modelType)} icon={getModelTypeIcon(modelType)}>
                              {model.displayName || model.providerModelId}
                            </Tag>
                            {model.contextWindow && (
                              <span style={{ fontSize: 12, color: '#999' }}>
                                {formatContextWindow(model.contextWindow)} tokens
                              </span>
                            )}
                            {model.inputPrice !== undefined && model.outputPrice !== undefined && (
                              <span style={{ fontSize: 12, color: '#999' }}>
                                ${model.inputPrice}/${model.outputPrice} /M
                              </span>
                            )}
                          </Space>
                          <Space>
                            <Button
                              type="text"
                              size="small"
                              icon={<EditOutlined />}
                              onClick={() => onEditModel(model)}
                            />
                            <Popconfirm
                              title={t('confirm.delete', { ns: 'common' })}
                              onConfirm={() => handleDeleteModel(model.id)}
                            >
                              <Button
                                type="text"
                                size="small"
                                danger
                                icon={<DeleteOutlined />}
                              />
                            </Popconfirm>
                          </Space>
                        </div>
                      </List.Item>
                    );
                  }}
                />
              )}
            </div>
          </>
        )}
      </div>
    </Card>
  );
}