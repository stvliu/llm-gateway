import { useMemo } from 'react';
import { Table, Tag, Space, Button, Tooltip, theme } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  MessageOutlined,
  PictureOutlined,
  AudioOutlined,
  ApiOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModels, useDeleteModel } from '@/services/query';
import { useConfirm } from '@/hooks/useConfirm';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import type { Model } from '@/types/model';
import type { Provider } from '@/types/provider';

interface ModelTableProps {
  providers: Provider[];
  onEditModel: (model: Model) => void;
  onDeleteModel: (model: Model) => void;
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
 * 模型表格视图
 * 一行一个模型，显示关键信息和操作按钮
 */
export function ModelTable({ providers, onEditModel, onDeleteModel }: ModelTableProps) {
  const { t } = useTranslation('models');
  const { token } = theme.useToken();
  const { confirm } = useConfirm();
  const { hasPermission } = useAuthStore();
  const { data: modelsData, isLoading } = useModels({ size: 100 });
  const deleteModelMutation = useDeleteModel();
  const canWrite = hasPermission(P.MODEL_WRITE);

  const models = modelsData?.items || [];

  // 构建 Provider ID -> Name 映射
  const providerMap = useMemo(() => {
    const map = new Map<number, Provider>();
    providers.forEach(p => map.set(p.id, p));
    return map;
  }, [providers]);

  const handleDelete = (model: Model) => {
    confirm({
      type: 'danger',
      entityName: model.displayName || model.providerModelId,
      onConfirm: () => deleteModelMutation.mutateAsync(model.id).then(() => onDeleteModel(model)),
    });
  };

  const columns = [
    {
      title: t('model.name'),
      dataIndex: 'displayName',
      key: 'displayName',
      render: (name: string, record: Model) => {
        return (
          <Space>
            <Tag color={getModelTypeColor(inferModelType(record))} icon={getModelTypeIcon(inferModelType(record))}>
              {name || record.providerModelId}
            </Tag>
          </Space>
        );
      },
    },
    {
      title: t('model.provider'),
      dataIndex: 'providerId',
      key: 'providerId',
      render: (providerId: number) => {
        const provider = providerMap.get(providerId);
        return provider?.providerName || '-';
      },
    },
    {
      title: t('model.type', { defaultValue: '类型' }),
      key: 'type',
      render: (_: unknown, record: Model) => {
        const capabilities = [];
        if (record.capabilities?.chat) capabilities.push('Chat');
        if (record.capabilities?.vision) capabilities.push('Vision');
        if (record.capabilities?.embedding) capabilities.push('Embedding');
        if (record.capabilities?.function_calling) capabilities.push('FC');
        return (
          <Space size={4}>
            {capabilities.map(cap => (
              <Tag key={cap} style={{ fontSize: 11 }}>{cap}</Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: t('detail.contextWindow'),
      dataIndex: 'contextWindow',
      key: 'contextWindow',
      width: 120,
      render: (ctx: number) => formatContextWindow(ctx),
    },
    {
      title: t('detail.price', { defaultValue: '价格 ($/M)' }),
      key: 'price',
      width: 140,
      render: (_: unknown, record: Model) => {
        if (record.inputPrice === undefined && record.outputPrice === undefined) return '-';
        return (
          <span style={{ fontSize: 12 }}>
            {record.inputPrice ?? '-'}/{record.outputPrice ?? '-'}
          </span>
        );
      },
    },
    {
      title: t('model.state'),
      dataIndex: 'state',
      key: 'state',
      width: 100,
      render: (state: string) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'default'}>
          {state === 'ACTIVE' ? t('state.active', { ns: 'common' }) : t('state.disabled', { ns: 'common' })}
        </Tag>
      ),
    },
    ...(canWrite ? [{
      title: t('actions.title', { ns: 'common' }),
      key: 'actions',
      width: 120,
      render: (_: unknown, record: Model) => (
        <Space>
          <Tooltip title={t('actions.edit', { ns: 'common' })}>
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => onEditModel(record)}
            />
          </Tooltip>
          <Tooltip title={t('actions.delete', { ns: 'common' })}>
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleDelete(record)}
            />
          </Tooltip>
        </Space>
      ),
    }] : []),
  ];

  return (
    <Table
      dataSource={models}
      columns={columns}
      rowKey="id"
      loading={isLoading}
      pagination={{
        pageSize: 20,
        showSizeChanger: true,
        showTotal: (total) => t('pagination.total', { ns: 'common', total, defaultValue: `共 ${total} 条` }),
      }}
      style={{ background: token.colorBgContainer, borderRadius: 8 }}
    />
  );
}