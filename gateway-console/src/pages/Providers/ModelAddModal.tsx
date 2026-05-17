import { useState, useCallback, useMemo } from 'react';
import { Modal, Table, Checkbox, Typography, Input, Button, Space, message, InputNumber, Row, Col, Collapse, Tag, Spin } from 'antd';
import { PlusOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModelMetadataByProvider, useModels, useCreateModel, useUpdateModel, useDeleteModel } from '@/services/query';
import { useConfirm } from '@/hooks/useConfirm';
import type { Provider } from '@/types/provider';
import type { Model } from '@/types/model';
import type { ModelMetadata } from '@/types/metadata';

const { Text } = Typography;

interface ModelAddModalProps {
  open: boolean;
  provider: Provider | null;
  editingModel?: Model | null;
  onClose: () => void;
  onSuccess: () => void;
}

/**
 * 模型添加/编辑弹窗
 * - 新增模式：表格批量选择 + 自定义模型 ID
 * - 编辑模式：表单编辑
 */
export function ModelAddModal({ open, provider, editingModel, onClose, onSuccess }: ModelAddModalProps) {
  const { t } = useTranslation('providers');
  const { confirm } = useConfirm();
  const createModelMutation = useCreateModel();
  const updateModelMutation = useUpdateModel();
  const deleteModelMutation = useDeleteModel();

  // 新增模式状态
  const [selectedModels, setSelectedModels] = useState<string[]>([]);
  const [searchText, setSearchText] = useState('');
  const [customModelId, setCustomModelId] = useState('');

  // 编辑模式状态
  const [editForm, setEditForm] = useState({
    displayName: '',
    contextWindow: undefined as number | undefined,
    inputPrice: undefined as number | undefined,
    outputPrice: undefined as number | undefined,
  });

  const isEditMode = !!editingModel?.id;

  // 获取元数据
  const providerType = provider?.providerType?.toLowerCase() || '';
  const { data: metadataList, isLoading: metadataLoading } = useModelMetadataByProvider(providerType);

  // 获取供应商已添加的模型
  const { data: existingModelsData, isLoading: existingLoading, refetch: refetchExisting } = useModels(
    { providerId: provider?.id, size: 100 },
    { enabled: open && !isEditMode && !!provider?.id }
  );
  const existingModels = existingModelsData?.items || [];
  const existingModelIds = useMemo(
    () => new Set(existingModels.map((m: Model) => m.providerModelId)),
    [existingModels]
  );

  // 可选模型（排除已添加的）
  const selectableModels = useMemo(() => {
    return (metadataList ?? [])
      .filter((m: ModelMetadata) => !existingModelIds.has(m.providerModelId))
      .filter((m: ModelMetadata) =>
        !searchText ||
        m.displayName?.toLowerCase().includes(searchText.toLowerCase()) ||
        m.providerModelId?.toLowerCase().includes(searchText.toLowerCase())
      );
  }, [metadataList, existingModelIds, searchText]);

  // 重置状态
  const resetState = useCallback(() => {
    setSelectedModels([]);
    setSearchText('');
    setCustomModelId('');
    setEditForm({
      displayName: '',
      contextWindow: undefined,
      inputPrice: undefined,
      outputPrice: undefined,
    });
  }, []);

  // 初始化编辑表单
  useMemo(() => {
    if (open && isEditMode && editingModel) {
      setEditForm({
        displayName: editingModel.displayName || '',
        contextWindow: editingModel.contextWindow,
        inputPrice: editingModel.inputPrice,
        outputPrice: editingModel.outputPrice,
      });
    }
  }, [open, isEditMode, editingModel]);

  // 全选/取消全选
  const handleSelectAll = useCallback((checked: boolean) => {
    setSelectedModels(checked ? selectableModels.map((m: ModelMetadata) => m.providerModelId) : []);
  }, [selectableModels]);

  // 单选
  const handleSelectOne = useCallback((modelId: string, checked: boolean) => {
    if (checked) {
      setSelectedModels(prev => [...prev, modelId]);
    } else {
      setSelectedModels(prev => prev.filter(id => id !== modelId));
    }
  }, []);

  // 添加自定义模型 ID
  const handleAddCustomModelId = useCallback(() => {
    if (!provider || !customModelId.trim()) return;

    const trimmedId = customModelId.trim();

    // 检查是否已存在于数据库
    if (existingModelIds.has(trimmedId)) {
      message.warning(t('message.modelAlreadyExists', { defaultValue: '该模型已添加，无需重复添加' }));
      return;
    }

    // 检查是否已在选择列表中
    if (selectedModels.includes(trimmedId)) {
      message.warning(t('message.modelAlreadySelected', { defaultValue: '该模型已在选择列表中' }));
      return;
    }

    // 添加到选择列表
    setSelectedModels(prev => [...prev, trimmedId]);
    setCustomModelId('');
    message.success(t('message.customModelAdded', { defaultValue: '已添加到待选列表' }));
  }, [provider, customModelId, selectedModels, existingModelIds, t]);

  // 删除已添加的模型
  const handleDeleteExisting = useCallback((model: Model) => {
    confirm({
      type: 'danger',
      entityName: model.displayName || model.providerModelId,
      onConfirm: async () => {
        await deleteModelMutation.mutateAsync(model.id);
        refetchExisting();
        message.success(t('message.modelDeleted', { defaultValue: '模型已删除' }));
      },
    });
  }, [confirm, deleteModelMutation, refetchExisting, t]);

  // 批量提交
  const handleSubmit = useCallback(async () => {
    if (!provider || selectedModels.length === 0) return;

    // 获取选中模型的元数据
    const modelMap = new Map<string, ModelMetadata>();
    (metadataList ?? []).forEach((m: ModelMetadata) => {
      modelMap.set(m.providerModelId, m);
    });

    let successCount = 0;
    let failCount = 0;

    for (const modelId of selectedModels) {
      const metadata = modelMap.get(modelId);
      try {
        await createModelMutation.mutateAsync({
          providerId: provider.id,
          providerModelId: modelId,
          displayName: metadata?.displayName || modelId,
          contextWindow: metadata?.contextWindow,
          inputPrice: metadata?.inputPrice,
          outputPrice: metadata?.outputPrice,
          capabilities: metadata?.capabilities,
        });
        successCount++;
      } catch {
        failCount++;
      }
    }

    if (successCount > 0) {
      message.success(t('message.modelsAdded', { defaultValue: `成功添加 ${successCount} 个模型` }));
      refetchExisting();
      onSuccess();
    }
    if (failCount > 0) {
      message.warning(t('message.modelsAddPartial', { defaultValue: `${failCount} 个模型添加失败` }));
    }

    resetState();
  }, [provider, selectedModels, metadataList, createModelMutation, t, refetchExisting, onSuccess, resetState]);

  // 编辑模式提交
  const handleEditSubmit = useCallback(async () => {
    if (!editingModel) return;

    try {
      await updateModelMutation.mutateAsync({
        id: editingModel.id,
        data: {
          displayName: editForm.displayName,
          contextWindow: editForm.contextWindow,
          inputPrice: editForm.inputPrice,
          outputPrice: editForm.outputPrice,
        },
      });
      message.success(t('message.modelUpdated', { defaultValue: '模型更新成功' }));
      onSuccess();
      onClose();
    } catch {
      message.error(t('message.modelSaveFailed', { defaultValue: '模型保存失败' }));
    }
  }, [editingModel, editForm, updateModelMutation, t, onSuccess, onClose]);

  // 关闭弹窗
  const handleClose = useCallback(() => {
    resetState();
    onClose();
  }, [resetState, onClose]);

  // 格式化函数
  const formatPrice = (price?: number) => {
    if (price == null) return '-';
    return `$${price}`;
  };

  const formatContext = (ctx?: number) => {
    if (ctx == null) return '-';
    if (ctx >= 1_000_000) return `${(ctx / 1_000_000).toFixed(1)}M`;
    if (ctx >= 1_000) return `${(ctx / 1_000).toFixed(0)}K`;
    return `${ctx}`;
  };

  // 表格列定义
  const columns = useMemo(() => [
    {
      title: (
        <Checkbox
          checked={selectableModels.length > 0 && selectedModels.length === selectableModels.length}
          indeterminate={selectedModels.length > 0 && selectedModels.length < selectableModels.length}
          onChange={e => handleSelectAll(e.target.checked)}
        />
      ),
      dataIndex: 'providerModelId',
      width: 48,
      render: (modelId: string) => (
        <Checkbox
          checked={selectedModels.includes(modelId)}
          onChange={e => handleSelectOne(modelId, e.target.checked)}
        />
      ),
    },
    {
      title: t('template.modelName', { defaultValue: '模型名称' }),
      dataIndex: 'displayName',
      render: (name: string, record: ModelMetadata) => (
        <div>
          <Text strong>{name}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 11 }}>{record.providerModelId}</Text>
        </div>
      ),
    },
    {
      title: t('template.contextWindow', { defaultValue: '上下文' }),
      dataIndex: 'contextWindow',
      width: 80,
      render: (ctx: number) => <Text>{formatContext(ctx)}</Text>,
    },
    {
      title: t('template.price', { defaultValue: '价格(/M)' }),
      width: 120,
      render: (_: unknown, record: ModelMetadata) => (
        <Text type="secondary" style={{ fontSize: 12 }}>
          {formatPrice(record.inputPrice)} → {formatPrice(record.outputPrice)}
        </Text>
      ),
    },
  ], [selectableModels, selectedModels, handleSelectAll, handleSelectOne, t]);

  // 已添加模型折叠面板
  const existingModelsPanel = useMemo(() => {
    if (existingModels.length === 0) return null;

    const items = [{
      key: 'existing',
      label: (
        <Space>
          <CheckCircleOutlined style={{ color: '#52c41a' }} />
          <Text>{t('template.existingModels', { defaultValue: '已添加模型' })}</Text>
          <Tag color="success">{existingModels.length}</Tag>
        </Space>
      ),
      children: (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
          {existingModels.map((model: Model) => (
            <Tag
              key={model.id}
              closable
              onClose={(e) => {
                e.preventDefault();
                handleDeleteExisting(model);
              }}
              style={{ margin: 0 }}
            >
              {model.displayName || model.providerModelId}
            </Tag>
          ))}
        </div>
      ),
    }];

    return (
      <Collapse
        items={items}
        ghost
        size="small"
        defaultActiveKey={['existing']}
        style={{ marginBottom: 12 }}
      />
    );
  }, [existingModels, handleDeleteExisting, t]);

  if (!provider) return null;

  // 编辑模式
  if (isEditMode) {
    return (
      <Modal
        title={t('editModel', { defaultValue: '编辑模型' })}
        open={open}
        onCancel={handleClose}
        onOk={handleEditSubmit}
        confirmLoading={updateModelMutation.isPending}
        okText={t('actions.save', { ns: 'common' })}
        cancelText={t('actions.cancel', { ns: 'common' })}
        width={480}
      >
        <div style={{ padding: '16px 0' }}>
          <div style={{ marginBottom: 16 }}>
            <Text type="secondary">{t('model.providerModelId', { defaultValue: '模型 ID' })}:</Text>
            <Text strong style={{ marginLeft: 8 }}>{editingModel?.providerModelId}</Text>
          </div>

          <div style={{ marginBottom: 16 }}>
            <Text type="secondary">{t('model.name', { defaultValue: '显示名称' })}</Text>
            <Input
              value={editForm.displayName}
              onChange={e => setEditForm(prev => ({ ...prev, displayName: e.target.value }))}
              placeholder={editingModel?.providerModelId}
              style={{ marginTop: 4 }}
            />
          </div>

          <Row gutter={16}>
            <Col span={12}>
              <Text type="secondary">{t('detail.contextWindow', { defaultValue: '上下文窗口' })}</Text>
              <InputNumber
                style={{ width: '100%', marginTop: 4 }}
                value={editForm.contextWindow}
                onChange={v => setEditForm(prev => ({ ...prev, contextWindow: v ?? undefined }))}
                formatter={v => v ? `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
                parser={v => v ? Number(v.replace(/,/g, '')) : 0}
                addonAfter="tokens"
              />
            </Col>
            <Col span={12}>
              <Text type="secondary">{t('detail.inputPrice', { defaultValue: '输入价格' })}</Text>
              <InputNumber
                style={{ width: '100%', marginTop: 4 }}
                value={editForm.inputPrice}
                onChange={v => setEditForm(prev => ({ ...prev, inputPrice: v ?? undefined }))}
                step={0.0001}
                precision={4}
                min={0}
                addonBefore="$"
                addonAfter="/M"
              />
            </Col>
          </Row>

          <div style={{ marginTop: 16 }}>
            <Text type="secondary">{t('detail.outputPrice', { defaultValue: '输出价格' })}</Text>
            <InputNumber
              style={{ width: '100%', marginTop: 4 }}
              value={editForm.outputPrice}
              onChange={v => setEditForm(prev => ({ ...prev, outputPrice: v ?? undefined }))}
              step={0.0001}
              precision={4}
              min={0}
              addonBefore="$"
              addonAfter="/M"
            />
          </div>
        </div>
      </Modal>
    );
  }

  // 新增模式
  return (
    <Modal
      title={t('addModel', { defaultValue: '添加模型' })}
      open={open}
      onCancel={handleClose}
      footer={null}
      width={640}
    >
      {/* 自定义模型 ID 输入区 */}
      <div style={{ marginBottom: 12 }}>
        <Space.Compact style={{ width: '100%' }}>
          <Input
            placeholder={t('template.customModelIdPlaceholder', { defaultValue: '输入自定义模型 ID...' })}
            value={customModelId}
            onChange={e => setCustomModelId(e.target.value)}
            onPressEnter={handleAddCustomModelId}
          />
          <Button type="primary" onClick={handleAddCustomModelId}>
            <PlusOutlined /> {t('actions.add', { ns: 'common' })}
          </Button>
        </Space.Compact>
      </div>

      {/* 已添加模型面板 */}
      {existingModelsPanel}

      {/* 搜索框 */}
      <Input.Search
        placeholder={t('template.searchModel', { defaultValue: '搜索模型...' })}
        value={searchText}
        onChange={e => setSearchText(e.target.value)}
        style={{ marginBottom: 12 }}
        allowClear
      />

      {/* 加载中 */}
      {metadataLoading || existingLoading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin />
        </div>
      ) : selectableModels.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 40, color: '#999' }}>
          {existingModels.length > 0
            ? t('template.allModelsAdded', { defaultValue: '所有模型已添加' })
            : t('template.noModels', { defaultValue: '暂无可选模型' })}
        </div>
      ) : (
        <Table
          dataSource={selectableModels}
          columns={columns}
          rowKey="providerModelId"
          size="small"
          pagination={false}
          scroll={{ y: 300 }}
        />
      )}

      {/* 底部操作栏 */}
      <div style={{ marginTop: 16 }}>
        {/* 已选模型标签 */}
        {selectedModels.length > 0 && (
          <div style={{ marginBottom: 12, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
            {selectedModels.map(id => {
              const metadata = metadataList?.find((m: ModelMetadata) => m.providerModelId === id);
              return (
                <Tag
                  key={id}
                  closable
                  onClose={() => handleSelectOne(id, false)}
                  style={{ margin: 0 }}
                >
                  {metadata?.displayName || id}
                </Tag>
              );
            })}
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text type="secondary">
            {t('template.selectedCount', { defaultValue: '已选择' })} {selectedModels.length} {t('template.models', { defaultValue: '个' })}
          </Text>
          <Space>
            <Button onClick={handleClose}>
              {t('actions.cancel', { ns: 'common' })}
            </Button>
            <Button
              type="primary"
              onClick={handleSubmit}
              loading={createModelMutation.isPending}
              disabled={selectedModels.length === 0}
            >
              {t('actions.add', { ns: 'common' })}
            </Button>
          </Space>
        </div>
      </div>
    </Modal>
  );
}
