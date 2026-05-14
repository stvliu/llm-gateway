import { useState, useCallback } from 'react';
import {
  Card,
  Button,
  Space,
  Tag,
  Empty,
  Spin,
  theme,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  AppstoreOutlined,
  MessageOutlined,
  ApiOutlined,
  PictureOutlined,
  AudioOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { StatusIndicator } from '@/components/common';
import { useConfirm } from '@/hooks/useConfirm';
import { ModelAddModal } from './ModelAddModal';
import {
  useModels,
  useDeleteModel,
} from '@/services/query';
import type { Provider } from '@/types/provider';
import type { Model } from '@/types/model';

interface ProviderModelsTabProps {
  provider: Provider | null;
  mode: 'view' | 'edit' | 'create';
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
 * 模型管理标签页
 * 卡片列表展示，支持增删改
 */
export function ProviderModelsTab({ provider, mode }: ProviderModelsTabProps) {
  const { t } = useTranslation('providers');
  const { token } = theme.useToken();
  const { confirm } = useConfirm();

  const [modelModalOpen, setModelModalOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<Model | null>(null);

  // 查询数据
  const { data: modelsData, isLoading } = useModels(
    { providerId: provider?.id, size: 100 },
    { enabled: !!provider && mode !== 'create' }
  );

  // Mutations
  const deleteModelMutation = useDeleteModel();

  const models = modelsData?.items || [];

  // 添加模型
  const handleAddModel = useCallback(() => {
    setEditingModel(null);
    setModelModalOpen(true);
  }, []);

  // 编辑模型
  const handleEditModel = useCallback((model: Model) => {
    setEditingModel(model);
    setModelModalOpen(true);
  }, []);

  // 删除模型
  const handleDeleteModel = useCallback((model: Model) => {
    confirm({
      type: 'danger',
      entityName: model.displayName || model.providerModelId,
      onConfirm: () => deleteModelMutation.mutateAsync(model.id),
    });
  }, [confirm, deleteModelMutation]);

  // 成功回调
  const handleSuccess = useCallback(() => {
    setModelModalOpen(false);
    setEditingModel(null);
  }, []);

  // 新增模式：暂不支持
  if (mode === 'create') {
    return (
      <Empty
        description={t('provider.addModelsAfterCreate', {
          defaultValue: '创建供应商后可添加模型',
        })}
      />
    );
  }

  // 无供应商
  if (!provider) {
    return (
      <Empty
        description={t('provider.selectHint', { defaultValue: '请选择一个供应商' })}
      />
    );
  }

  // 加载中
  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}>
        <Spin />
      </div>
    );
  }

  return (
    <div>
      {/* 头部 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 16,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <AppstoreOutlined />
          <span style={{ fontWeight: 600, fontSize: 15 }}>
            {t('provider.models', { defaultValue: '模型' })}
          </span>
          <Tag color="blue">{models.length}</Tag>
        </div>
        {mode === 'edit' && (
          <Button type="primary" ghost icon={<PlusOutlined />} onClick={handleAddModel}>
            {t('actions.add', { ns: 'common' })}
          </Button>
        )}
      </div>

      {/* 卡片列表 */}
      {models.length === 0 ? (
        <Empty
          description={t('provider.noModels', { defaultValue: '暂无模型' })}
        >
          {mode === 'edit' && (
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAddModel}>
              {t('actions.add', { ns: 'common' })}
            </Button>
          )}
        </Empty>
      ) : (
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          {models.map((model) => {
            const modelType = inferModelType(model);
            return (
              <Card
                key={model.id}
                size="small"
                style={{
                  background: token.colorFillAlter,
                  borderRadius: 8,
                }}
                styles={{ body: { padding: '12px 16px' } }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  {/* 左侧信息 */}
                  <Space>
                    <Tag
                      color={getModelTypeColor(modelType)}
                      icon={getModelTypeIcon(modelType)}
                    >
                      {model.displayName || model.providerModelId || `Model ${model.id}`}
                    </Tag>
                    {model.contextWindow && (
                      <span style={{ fontSize: 12, color: token.colorTextSecondary }}>
                        {formatContextWindow(model.contextWindow)} tokens
                      </span>
                    )}
                    {model.inputPrice !== undefined && model.outputPrice !== undefined && (
                      <span style={{ fontSize: 12, color: token.colorTextSecondary }}>
                        ${model.inputPrice}/${model.outputPrice} /M
                      </span>
                    )}
                    <StatusIndicator
                      status={model.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'}
                      showLabel={false}
                    />
                  </Space>

                  {/* 右侧操作 */}
                  {mode === 'edit' && (
                    <Space>
                      <Button
                        type="text"
                        size="small"
                        icon={<EditOutlined />}
                        onClick={() => handleEditModel(model)}
                      />
                      <Button
                          type="text"
                          size="small"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() => handleDeleteModel(model)}
                        />
                    </Space>
                  )}
                </div>
              </Card>
            );
          })}
        </Space>
      )}

      {/* 模型弹窗 */}
      <ModelAddModal
        open={modelModalOpen}
        provider={provider}
        editingModel={editingModel}
        onClose={() => {
          setModelModalOpen(false);
          setEditingModel(null);
        }}
        onSuccess={handleSuccess}
      />
    </div>
  );
}

export type { ProviderModelsTabProps };
