import { useState, useMemo, useCallback } from 'react';
import { Tag, Space, Button, Tooltip, message, Switch } from 'antd';
import {
  PlusOutlined,
  ReloadOutlined,
  FilterOutlined,
  DeleteOutlined,
  InfoCircleOutlined,
  MessageOutlined,
  PictureOutlined,
  AudioOutlined,
  ApiOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useConfirm } from '@/hooks/useConfirm';
import { PageHeader, EntityTable, EntityDrawer, InfoGroup, InfoGroupContainer, FilterPanel } from '@/components/ui';
import type { ColumnConfig, FilterCondition } from '@/components/ui';
import { StatusIndicator } from '@/components/common';
import { useModels, useModel, useDeleteModel, useSetEnabledModel } from '@/services/query';
import type { Model } from '@/types/model';
import type { Provider } from '@/types/provider';
import { ModelAddModal } from './ModelAddModal';

type ModelType = 'CHAT' | 'COMPLETION' | 'EMBEDDING' | 'IMAGE' | 'AUDIO';

interface ModelsTableViewProps {
  providers: Provider[];
  providersLoading: boolean;
}

/**
 * 根据模型能力推断类型
 */
function inferModelType(model: Model): ModelType {
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
function getModelTypeIcon(type: ModelType) {
  const iconMap: Record<ModelType, React.ReactNode> = {
    CHAT: <MessageOutlined />,
    COMPLETION: <MessageOutlined />,
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
 * 格式化上下文窗口
 */
function formatContextWindow(contextWindow?: number): string {
  if (!contextWindow) return '-';
  if (contextWindow >= 1000000) return `${(contextWindow / 1000000).toFixed(1)}M`;
  if (contextWindow >= 1000) return `${Math.round(contextWindow / 1000)}K`;
  return contextWindow.toString();
}

/**
 * 模型表格视图（集成新组件）
 * 使用 PageHeader + EntityTable + EntityDrawer + FilterPanel
 */
export function ModelsTableView({ providers }: ModelsTableViewProps) {
  const { t } = useTranslation('models');
  const { t: tc } = useTranslation('common');
  const { confirm } = useConfirm();

  // 分页状态
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // 数据查询（动态分页）
  const { data: modelsData, isLoading: modelsLoading, refetch } = useModels({ page, limit: pageSize });
  const models = modelsData?.items || [];
  const pagination = modelsData?.pagination;

  // 抽屉状态
  const [selectedModelId, setSelectedModelId] = useState<number | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);

  // 编辑弹窗状态
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<Model | null>(null);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);

  // 过滤器状态
  const [filterPanelOpen, setFilterPanelOpen] = useState(false);
  const [activeFilters, setActiveFilters] = useState<FilterCondition[]>([]);
  const [filterLogic, setFilterLogic] = useState<'and' | 'or'>('and');

  // 删除和启用/禁用
  const deleteModelMutation = useDeleteModel();
  const setEnabledMutation = useSetEnabledModel();

  // 单个模型查询（用于抽屉）
  const shouldFetchModel = selectedModelId !== null && selectedModelId > 0;
  const { data: selectedModel, isLoading: modelLoading, error: modelError } = useModel(
    shouldFetchModel ? selectedModelId : 0
  );

  // Provider 映射
  const providerMap = useMemo(() => {
    const map = new Map<number, Provider>();
    providers.forEach((p) => map.set(p.id, p));
    return map;
  }, [providers]);

  // 前端过滤（用于 FilterPanel 的本地过滤，后续可改为后端过滤）
  const filteredModels = useMemo(() => {
    if (activeFilters.length === 0) return models;

    return models.filter((model) => {
      const results = activeFilters.map((filter) => {
        const value = model[filter.field as keyof Model];
        const strValue = String(value ?? '');

        switch (filter.operator) {
          case 'eq':
            return strValue === filter.value;
          case 'ne':
            return strValue !== filter.value;
          case 'contains':
            return strValue.toLowerCase().includes(filter.value.toLowerCase());
          case 'notContains':
            return !strValue.toLowerCase().includes(filter.value.toLowerCase());
          case 'startsWith':
            return strValue.toLowerCase().startsWith(filter.value.toLowerCase());
          case 'endsWith':
            return strValue.toLowerCase().endsWith(filter.value.toLowerCase());
          default:
            return true;
        }
      });

      return filterLogic === 'and'
        ? results.every(Boolean)
        : results.some(Boolean);
    });
  }, [models, activeFilters, filterLogic]);

  // 列配置
  const columns: ColumnConfig[] = useMemo(() => [
    {
      key: 'displayName',
      title: t('model.name'),
      dataIndex: 'displayName',
      sortable: true,
      render: (value: unknown, record: unknown) => {
        const model = record as Model;
        const type = inferModelType(model);
        return (
          <Space>
            <Tag color={getModelTypeColor(type)} icon={getModelTypeIcon(type)}>
              {(value as string) || model.providerModelId || `Model ${model.id}`}
            </Tag>
          </Space>
        );
      },
    },
    {
      key: 'providerId',
      title: t('model.provider'),
      dataIndex: 'providerId',
      render: (value: unknown) => {
        const provider = providerMap.get(value as number);
        return provider?.providerName || '-';
      },
    },
    {
      key: 'capabilities',
      title: t('model.type', { defaultValue: '类型' }),
      render: (_: unknown, record: unknown) => {
        const model = record as Model;
        const capabilities = [];
        if (model.capabilities?.chat) capabilities.push('Chat');
        if (model.capabilities?.vision) capabilities.push('Vision');
        if (model.capabilities?.embedding) capabilities.push('Embedding');
        if (model.capabilities?.function_calling) capabilities.push('FC');
        return (
          <Space size={4}>
            {capabilities.map((cap) => (
              <Tag key={cap} style={{ fontSize: 11 }}>{cap}</Tag>
            ))}
          </Space>
        );
      },
    },
    {
      key: 'contextWindow',
      title: t('detail.contextWindow'),
      dataIndex: 'contextWindow',
      width: 120,
      render: (value: unknown) => formatContextWindow(value as number),
    },
    {
      key: 'price',
      title: t('detail.price', { defaultValue: '价格 ($/M)' }),
      width: 140,
      render: (_: unknown, record: unknown) => {
        const model = record as Model;
        if (model.inputPrice === undefined && model.outputPrice === undefined) return '-';
        return (
          <span style={{ fontSize: 12 }}>
            {model.inputPrice ?? '-'}/{model.outputPrice ?? '-'}
          </span>
        );
      },
    },
    {
      key: 'state',
      title: t('model.state'),
      dataIndex: 'state',
      width: 120,
      render: (value: unknown, record: unknown) => {
        const model = record as Model;
        const isActive = value === 'ACTIVE';
        return (
          <Switch
            checked={isActive}
            onChange={(checked) => handleToggleEnabled(model, checked)}
            checkedChildren={tc('state.enabled')}
            unCheckedChildren={tc('state.disabled')}
            size="small"
          />
        );
      },
    },
    {
      key: 'actions',
      title: tc('actions.label'),
      width: 100,
      render: (_: unknown, record: unknown) => {
        const model = record as Model;
        return (
          <Space className="table-action-cell">
            <Tooltip title={tc('actions.view', { defaultValue: '查看' })}>
              <Button
                type="text"
                size="small"
                icon={<InfoCircleOutlined />}
                onClick={(e) => {
                  e.stopPropagation();
                  setSelectedModelId(model.id);
                  setCurrentIndex(filteredModels.indexOf(model));
                  setDrawerOpen(true);
                }}
              />
            </Tooltip>
            <Tooltip title={tc('actions.delete')}>
              <Button
                type="text"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={(e) => {
                  e.stopPropagation();
                  handleDelete(model);
                }}
              />
            </Tooltip>
          </Space>
        );
      },
    },
  ], [t, tc, providerMap]);

  // 行点击处理
  const handleRowClick = useCallback((record: unknown, index: number) => {
    const model = record as Model;
    setSelectedModelId(model.id);
    setCurrentIndex(index);
    setDrawerOpen(true);
  }, []);

  // 抽屉导航
  const handlePrevious = useCallback(() => {
    if (currentIndex > 0) {
      const newIndex = currentIndex - 1;
      setCurrentIndex(newIndex);
      setSelectedModelId(filteredModels[newIndex].id);
    }
  }, [currentIndex, filteredModels]);

  const handleNext = useCallback(() => {
    if (currentIndex < filteredModels.length - 1) {
      const newIndex = currentIndex + 1;
      setCurrentIndex(newIndex);
      setSelectedModelId(filteredModels[newIndex].id);
    }
  }, [currentIndex, filteredModels]);

  // 编辑处理
  const handleEdit = useCallback((model: Model) => {
    const provider = providerMap.get(model.providerId);
    setEditingModel(model);
    setEditingProvider(provider || null);
    setEditModalOpen(true);
  }, [providerMap]);

  // 删除处理
  const handleDelete = useCallback((model: Model) => {
    confirm({
      type: 'danger',
      entityName: model.displayName || model.providerModelId,
      onConfirm: () => deleteModelMutation.mutateAsync(model.id).then(() => {
        if (selectedModelId === model.id) {
          setDrawerOpen(false);
          setSelectedModelId(null);
        }
      }),
    });
  }, [confirm, deleteModelMutation, selectedModelId]);

  // 启用/禁用处理
  const handleToggleEnabled = useCallback(async (model: Model, enabled: boolean) => {
    try {
      await setEnabledMutation.mutateAsync({ id: model.id, enabled });
      message.success(tc('message.success'));
    } catch {
      message.error(tc('message.error'));
    }
  }, [setEnabledMutation, tc]);

  // 过滤处理
  const handleApplyFilter = useCallback((conditions: FilterCondition[], logic: 'and' | 'or') => {
    setActiveFilters(conditions);
    setFilterLogic(logic);
  }, []);

  const handleClearFilters = useCallback(() => {
    setActiveFilters([]);
  }, []);

  // 过滤字段配置
  const filterFields = useMemo(() => [
    { key: 'displayName', label: t('model.name'), type: 'string' as const },
    { key: 'providerName', label: t('model.provider'), type: 'string' as const },
    { key: 'state', label: t('model.state'), type: 'string' as const },
  ], [t]);

  // 过滤标签
  const filterTags = useMemo(() => activeFilters.map((f) => ({
    key: f.key,
    label: `${f.field} ${f.operator} "${f.value}"`,
    onRemove: () => setActiveFilters(activeFilters.filter((af) => af.key !== f.key)),
  })), [activeFilters]);

  // 页面操作按钮
  const pageActions = useMemo(() => [
    {
      key: 'add',
      label: tc('actions.add'),
      type: 'primary' as const,
      icon: <PlusOutlined />,
      onClick: () => {
        setEditingModel(null);
        setEditingProvider(null);
        setEditModalOpen(true);
      },
    },
    {
      key: 'refresh',
      label: tc('actions.refresh', { defaultValue: '刷新' }),
      icon: <ReloadOutlined />,
      onClick: () => refetch(),
      loading: modelsLoading,
    },
    {
      key: 'filter',
      label: tc('filter.title'),
      icon: <FilterOutlined />,
      onClick: () => setFilterPanelOpen(true),
    },
  ], [tc, refetch, modelsLoading]);

  // 抽屉内容
  const drawerContent = selectedModel && (
    <InfoGroupContainer>
      <InfoGroup
        title={tc('drawer.info.basicInfo')}
        items={[
          { label: t('model.name'), value: selectedModel.displayName || selectedModel.providerModelId },
          { label: t('model.providerModelId'), value: <code>{selectedModel.providerModelId || '-'}</code> },
          { label: t('model.provider'), value: selectedModel.providerName },
          { label: t('detail.contextWindow'), value: formatContextWindow(selectedModel.contextWindow) },
          { label: t('detail.inputPrice'), value: selectedModel.inputPrice ? `$${selectedModel.inputPrice}/1K` : '-' },
          { label: t('detail.outputPrice'), value: selectedModel.outputPrice ? `$${selectedModel.outputPrice}/1K` : '-' },
          {
            label: t('model.state'),
            value: (
              <Space>
                <StatusIndicator status={selectedModel.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'} />
                <Switch
                  checked={selectedModel.state === 'ACTIVE'}
                  onChange={(checked) => handleToggleEnabled(selectedModel, checked)}
                  checkedChildren={tc('state.enabled')}
                  unCheckedChildren={tc('state.disabled')}
                  size="small"
                />
              </Space>
            ),
          },
          { label: t('detail.createdAt'), value: new Date(selectedModel.createdAt).toLocaleString() },
          { label: t('detail.updatedAt'), value: new Date(selectedModel.updatedAt).toLocaleString() },
        ]}
      />
      <InfoGroup
        title={t('detail.usageStats', { defaultValue: '使用统计' })}
        collapsible
        defaultCollapsed
        items={[
          { label: t('detail.totalRequests'), value: '-' },
          { label: t('detail.successRate'), value: '-' },
          { label: t('detail.avgLatency'), value: '-' },
          { label: t('detail.tokenUsage'), value: '-' },
        ]}
      />
    </InfoGroupContainer>
  );

  return (
    <div className="h-full flex flex-col">
      {/* 页面标题 */}
      <PageHeader
        title={t('title')}
        subtitle={t('subtitle')}
        actions={pageActions}
        filterTags={filterTags}
        onClearAllFilters={handleClearFilters}
      />

      {/* 表格内容 */}
      <div className="flex-1 overflow-auto p-4">
        <EntityTable
          dataSource={filteredModels}
          columns={columns}
          rowKey="id"
          loading={modelsLoading}
          selectedRowId={selectedModelId ?? undefined}
          onRowClick={handleRowClick}
          showColumnConfig
          showRefresh
          onRefresh={() => refetch()}
          pagination={{
            current: pagination?.page ?? page,
            pageSize: pagination?.limit ?? pageSize,
            total: pagination?.total ?? 0,
            onChange: (newPage, newPageSize) => {
              setPage(newPage);
              if (newPageSize !== pageSize) {
                setPageSize(newPageSize);
                setPage(1); // 切换每页条数时重置到第一页
              }
            },
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => tc('table.total', { count: total }),
          }}
        />
      </div>

      {/* 模型详情抽屉 */}
      <EntityDrawer<Model>
        open={drawerOpen}
        entity={selectedModel ?? null}
        mode="view"
        currentIndex={currentIndex}
        totalCount={filteredModels.length}
        onPrevious={handlePrevious}
        onNext={handleNext}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedModelId(null);
        }}
        onModeChange={(mode) => {
          if (mode === 'edit' && selectedModel) {
            handleEdit(selectedModel);
          }
        }}
        onDelete={async () => { if (selectedModel) handleDelete(selectedModel); }}
        loading={modelLoading}
        error={modelError}
        onRetry={() => refetch()}
        title={tc('drawer.title.details')}
      >
        {drawerContent}
      </EntityDrawer>

      {/* 过滤器面板 */}
      <FilterPanel
        open={filterPanelOpen}
        onClose={() => setFilterPanelOpen(false)}
        onApply={handleApplyFilter}
        fields={filterFields}
        initialConditions={activeFilters}
        initialLogic={filterLogic}
      />

      {/* 编辑弹窗 */}
      <ModelAddModal
        open={editModalOpen}
        provider={editingProvider}
        editingModel={editingModel}
        onClose={() => {
          setEditModalOpen(false);
          setEditingModel(null);
          setEditingProvider(null);
        }}
        onSuccess={() => {
          setEditModalOpen(false);
          setEditingModel(null);
          setEditingProvider(null);
          message.success(tc('message.success'));
          refetch();
        }}
      />
    </div>
  );
}