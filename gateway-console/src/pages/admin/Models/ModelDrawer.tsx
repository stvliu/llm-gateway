import { Drawer, Descriptions, Tag, Button, Space, Divider, Spin, Empty } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  MessageOutlined,
  FileTextOutlined,
  AudioOutlined,
  PictureOutlined,
  ApiOutlined,
  AppstoreOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModel, useDeleteModel } from '@/services/query';
import { StatusIndicator } from '@/components/common';
import type { Model } from '@/types/model';

interface ModelDrawerProps {
  modelId: number | null;
  onClose: () => void;
  onEdit: (model: Model) => void;
}

type ModelType = 'CHAT' | 'COMPLETION' | 'EMBEDDING' | 'IMAGE' | 'AUDIO';

/**
 * 根据模型能力推断类型
 */
function inferModelType(model: Model): ModelType {
  if (model.capabilities) {
    if (model.capabilities.chat) return 'CHAT';
    if (model.capabilities.completion) return 'COMPLETION';
    if (model.capabilities.embedding) return 'EMBEDDING';
    if (model.capabilities.image) return 'IMAGE';
    if (model.capabilities.audio) return 'AUDIO';
  }
  return 'CHAT';
}

/**
 * 获取模型类型图标
 */
function getModelTypeIcon(type: ModelType) {
  const iconMap: Record<ModelType, React.ReactNode> = {
    CHAT: <MessageOutlined />,
    COMPLETION: <FileTextOutlined />,
    EMBEDDING: <ApiOutlined />,
    IMAGE: <PictureOutlined />,
    AUDIO: <AudioOutlined />,
  };
  return iconMap[type] || <ApiOutlined />;
}

/**
 * 获取模型类型颜色
 */
function getModelTypeColor(type: ModelType): string {
  const colorMap: Record<ModelType, string> = {
    CHAT: 'blue',
    COMPLETION: 'cyan',
    EMBEDDING: 'purple',
    IMAGE: 'orange',
    AUDIO: 'green',
  };
  return colorMap[type] || 'default';
}

/**
 * Model 详情抽屉
 */
export function ModelDrawer({
  modelId,
  onClose,
  onEdit,
}: ModelDrawerProps) {
  const { t } = useTranslation('models');

  const { data: model, isLoading } = useModel(modelId || 0);
  const deleteMutation = useDeleteModel();

  const handleDelete = () => {
    if (!model) return;
    deleteMutation.mutateAsync(model.id);
    onClose();
  };

  if (!modelId) {
    return null;
  }

  return (
    <Drawer
      title={
        <Space>
          <AppstoreOutlined />
          {t('detail.modelDetail')}
        </Space>
      }
      placement="right"
      width={480}
      onClose={onClose}
      open={!!modelId}
      extra={
        <Space>
          <Button
            icon={<EditOutlined />}
            onClick={() => model && onEdit(model)}
          >
            {t('actions.edit', { ns: 'common' })}
          </Button>
          <Button
            danger
            icon={<DeleteOutlined />}
            onClick={handleDelete}
            loading={deleteMutation.isPending}
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
      ) : model ? (
        <>
          {/* 模型头部 */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              marginBottom: 24,
              padding: 16,
              background: '#fafafa',
              borderRadius: 8,
            }}
          >
            <Tag
              color={getModelTypeColor(inferModelType(model))}
              icon={getModelTypeIcon(inferModelType(model))}
              style={{ fontSize: 14, padding: '4px 12px' }}
            >
              {model.displayName || model.providerModelId || `Model ${model.id}`}
            </Tag>
            <StatusIndicator status={model.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} />
          </div>

          {/* 基本信息 */}
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label={t('model.name')}>
              {model.displayName || '-'}
            </Descriptions.Item>
            <Descriptions.Item label={t('model.providerModelId')}>
              <code>{model.providerModelId || '-'}</code>
            </Descriptions.Item>
            <Descriptions.Item label={t('model.provider')}>
              {model.providerName}
            </Descriptions.Item>
            <Descriptions.Item label={t('detail.contextWindow')}>
              {model.contextWindow ? `${model.contextWindow.toLocaleString()} tokens` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label={t('detail.inputPrice')}>
              {model.inputPrice ? `$${model.inputPrice}/1K tokens` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label={t('detail.outputPrice')}>
              {model.outputPrice ? `$${model.outputPrice}/1K tokens` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label={t('model.state')}>
              <StatusIndicator status={model.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} />
            </Descriptions.Item>
            <Descriptions.Item label={t('detail.createdAt')}>
              {new Date(model.createdAt).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label={t('detail.updatedAt')}>
              {new Date(model.updatedAt).toLocaleString()}
            </Descriptions.Item>
          </Descriptions>

          <Divider>{t('detail.usageStats')}</Divider>

          {/* 使用统计（占位） */}
          <Descriptions column={2} size="small">
            <Descriptions.Item label={t('detail.totalRequests')}>
              <span style={{ fontWeight: 600 }}>-</span>
            </Descriptions.Item>
            <Descriptions.Item label={t('detail.successRate')}>
              <span style={{ fontWeight: 600, color: '#52c41a' }}>-</span>
            </Descriptions.Item>
            <Descriptions.Item label={t('detail.avgLatency')}>
              <span style={{ fontWeight: 600 }}>-</span>
            </Descriptions.Item>
            <Descriptions.Item label={t('detail.tokenUsage')}>
              <span style={{ fontWeight: 600 }}>-</span>
            </Descriptions.Item>
          </Descriptions>
        </>
      ) : (
        <Empty description={t('detail.notFound')} />
      )}
    </Drawer>
  );
}