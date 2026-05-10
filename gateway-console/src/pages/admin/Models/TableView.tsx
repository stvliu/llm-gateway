import { useState, useMemo } from 'react';
import { Table, Button, Space, Tag, Modal, message, Dropdown, Badge } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  DownOutlined,
  InfoCircleOutlined,
  MessageOutlined,
  FileTextOutlined,
  AudioOutlined,
  PictureOutlined,
  ApiOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import type { Provider } from '@/types/provider';
import type { Model } from '@/types/model';
import type { SearchFilters } from '@/components/common/SearchFilterBar';
import { useProviders, useModels, useDeleteProvider, useDeleteModel } from '@/services/query';
import { StatusIndicator } from '@/components/common';

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

interface TableViewProps {
  filters: SearchFilters;
  onEditProvider: (provider: Provider) => void;
  onAddModel: (provider: Provider) => void;
  onEditModel: (model: Model) => void;
  onViewProviderDetail: (provider: Provider) => void;
}

interface TableRow {
  key: string;
  type: 'provider' | 'model';
  provider: Provider;
  model?: Model;
  children?: TableRow[];
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
 * 增强表格视图
 * 支持层级展示、批量操作、排序筛选
 */
export function TableView({
  filters,
  onEditProvider,
  onAddModel,
  onEditModel,
  onViewProviderDetail,
}: TableViewProps) {
  const { t } = useTranslation('models');
  const [expandedRowKeys, setExpandedRowKeys] = useState<string[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  const { data: providersData, isLoading: providersLoading } = useProviders({ size: 100 });
  const { data: modelsData, isLoading: modelsLoading } = useModels({ size: 100 });
  const deleteProviderMutation = useDeleteProvider();
  const deleteModelMutation = useDeleteModel();

  const providers = providersData?.items || [];
  const allModels = modelsData?.items || [];

  // 构建层级数据
  const tableData: TableRow[] = useMemo(() => {
    let filteredProviders = providers;

    // 筛选 Provider
    if (filters.keyword) {
      const keyword = filters.keyword.toLowerCase();
      filteredProviders = filteredProviders.filter(
        (p) =>
          p.providerName.toLowerCase().includes(keyword)
      );
    }
    if (filters.providerType) {
      filteredProviders = filteredProviders.filter((p) => p.providerType === filters.providerType);
    }
    if (filters.enabled) {
      filteredProviders = filteredProviders.filter((p) => p.state === 'ACTIVE');
    }

    return filteredProviders.map((provider) => {
      let providerModels = allModels.filter((m) => m.providerId === provider.id);

      // 筛选 Model
      if (filters.keyword) {
        const keyword = filters.keyword.toLowerCase();
        providerModels = providerModels.filter(
          (m) =>
            (m.displayName?.toLowerCase().includes(keyword)) ||
            m.providerModelId?.toLowerCase().includes(keyword)
        );
      }
      if (filters.modelType) {
        providerModels = providerModels.filter((m) => m.capabilities?.[filters.modelType!.toLowerCase()]);
      }

      return {
        key: `provider-${provider.id}`,
        type: 'provider' as const,
        provider,
        children: providerModels.map((model) => ({
          key: `model-${model.id}`,
          type: 'model' as const,
          provider,
          model,
        })),
      };
    });
  }, [providers, allModels, filters]);

  const handleDeleteProvider = (provider: Provider) => {
    Modal.confirm({
      title: t('confirm.delete', { ns: 'common' }),
      content: t('confirm.deleteProviderDesc', { name: provider.providerName }),
      onOk: async () => {
        await deleteProviderMutation.mutateAsync(provider.id);
        message.success(t('message.success', { ns: 'common' }));
      },
    });
  };

  const handleDeleteModel = (model: Model) => {
    Modal.confirm({
      title: t('confirm.delete', { ns: 'common' }),
      content: t('confirm.deleteModelDesc', { name: model.displayName || model.providerModelId || `Model ${model.id}` }),
      onOk: async () => {
        await deleteModelMutation.mutateAsync(model.id);
        message.success(t('message.success', { ns: 'common' }));
      },
    });
  };

  const columns: ColumnsType<TableRow> = [
    {
      title: t('provider.name'),
      dataIndex: 'name',
      key: 'name',
      render: (_, record) => {
        if (record.type === 'provider') {
          return (
            <Space>
              <StatusIndicator status={record.provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} showLabel={false} />
              <a onClick={() => onViewProviderDetail(record.provider)}>
                {record.provider.providerName}
              </a>
              <Tag color="blue">{t(`type.${record.provider.providerType}`, { ns: 'providers' })}</Tag>
            </Space>
          );
        }
        if (record.model) {
          const modelType = inferModelType(record.model);
          return (
            <Space style={{ paddingLeft: 24 }}>
              <Tag color={getModelTypeColor(modelType)} icon={getModelTypeIcon(modelType)}>
                {record.model.displayName || record.model.providerModelId || `Model ${record.model.id}`}
              </Tag>
            </Space>
          );
        }
        return null;
      },
    },
    {
      title: t('provider.type'),
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (_, record) => {
        if (record.type === 'provider') {
          return t(`type.${record.provider.providerType}`, { ns: 'providers' });
        }
        if (record.model) {
          const modelType = inferModelType(record.model);
          return (
            <Tag color={getModelTypeColor(modelType)}>
              {t(`type.${modelType}`)}
            </Tag>
          );
        }
        return null;
      },
    },
    {
      title: t('provider.state'),
      dataIndex: 'state',
      key: 'state',
      width: 100,
      render: (_, record) => {
        if (record.type === 'provider') {
          return <StatusIndicator status={record.provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} size="small" />;
        }
        if (record.model) {
          return <StatusIndicator status={record.model.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} size="small" />;
        }
        return null;
      },
    },
    {
      title: t('detail.modelCount'),
      key: 'modelCount',
      width: 100,
      render: (_, record) => {
        if (record.type === 'provider') {
          const count = allModels.filter((m) => m.providerId === record.provider.id).length;
          return <Badge count={count} showZero color="#1677ff" />;
        }
        return null;
      },
    },
    {
      title: '🔑 Api Key',
      key: 'keyStats',
      width: 100,
      render: (_, record) => {
        if (record.type === 'provider') {
          const stats = record.provider.keyStats;
          if (!stats) {
            return <span style={{ color: '#999' }}>-</span>;
          }
          const { activeCount, totalCount } = stats;
          const allActive = activeCount === totalCount;
          const noneActive = activeCount === 0;
          const color = noneActive ? '#ff4d4f' : allActive ? '#52c41a' : '#faad14';
          return (
            <span style={{ color }}>
              {activeCount}/{totalCount} {t('detail.keyActive', { defaultValue: '活跃' })}
            </span>
          );
        }
        return null;
      },
    },
    {
      title: t('actions.label', { ns: 'common' }),
      key: 'actions',
      width: 150,
      render: (_, record) => {
        if (record.type === 'provider') {
          const menuItems = [
            {
              key: 'view',
              label: t('detail.viewDetail'),
              icon: <InfoCircleOutlined />,
              onClick: () => onViewProviderDetail(record.provider),
            },
            {
              key: 'edit',
              label: t('actions.label', { ns: 'common' }),
              icon: <EditOutlined />,
              onClick: () => onEditProvider(record.provider),
            },
            {
              key: 'addModel',
              label: t('addModel'),
              icon: <PlusOutlined />,
              onClick: () => onAddModel(record.provider),
            },
            { type: 'divider' as const },
            {
              key: 'delete',
              label: t('actions.delete', { ns: 'common' }),
              icon: <DeleteOutlined />,
              danger: true,
              onClick: () => handleDeleteProvider(record.provider),
            },
          ];
          return (
            <Dropdown menu={{ items: menuItems }} trigger={['click']}>
              <Button type="text" icon={<EditOutlined />}>
                <DownOutlined />
              </Button>
            </Dropdown>
          );
        }
        if (record.model) {
          return (
            <Space>
              <Button type="text" icon={<EditOutlined />} onClick={() => onEditModel(record.model!)} />
              <Button
                type="text"
                danger
                icon={<DeleteOutlined />}
                onClick={() => handleDeleteModel(record.model!)}
              />
            </Space>
          );
        }
        return null;
      },
    },
  ];

  const rowSelection = {
    selectedRowKeys,
    onChange: (keys: React.Key[]) => setSelectedRowKeys(keys),
  };

  const isLoading = providersLoading || modelsLoading;

  return (
    <div>
      {selectedRowKeys.length > 0 && (
        <div style={{ marginBottom: 16 }}>
          <Space>
            <span>{t('table.selected', { count: selectedRowKeys.length })}</span>
            <Button danger onClick={() => setSelectedRowKeys([])}>
              {t('actions.clearSelection', { ns: 'common' })}
            </Button>
          </Space>
        </div>
      )}

      <Table
        columns={columns}
        dataSource={tableData}
        loading={isLoading}
        rowSelection={rowSelection}
        expandable={{
          expandedRowKeys,
          onExpandedRowsChange: (keys) => setExpandedRowKeys(keys as string[]),
          defaultExpandAllRows: true,
        }}
        pagination={{
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => t('table.total', { count: total }),
          pageSizeOptions: ['10', '20', '50', '100'],
        }}
        size="middle"
      />
    </div>
  );
}