import { useState, useMemo, useCallback } from 'react';
import { Card, Button, Space, Segmented, Modal, Form, Input, Select, message } from 'antd';
import {
  AppstoreOutlined,
  UnorderedListOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '@/stores/themeStore';
import { SearchFilterBar, type SearchFilters } from '@/components/common';
import { CardView } from './CardView';
import { TableView } from './TableView';
import { ProviderDrawer } from './ProviderDrawer';
import { ModelDrawer } from './ModelDrawer';
import {
  useProviders,
  useCreateProvider,
  useUpdateProvider,
  useDeleteProvider,
  useCreateModel,
  useUpdateModel,
  useDeleteModel,
} from '@/services/query';
import type { Provider, CreateProviderRequest, UpdateProviderRequest } from '@/types/provider';
import type { Model, CreateModelRequest, UpdateModelRequest } from '@/types/model';

type ViewMode = 'card' | 'table';

/**
 * 模型管理页面
 * 支持卡片/表格双视图切换、搜索筛选、详情抽屉
 */
export default function AdminModels() {
  const { t } = useTranslation('models');
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  // 视图模式
  const [viewMode, setViewMode] = useState<ViewMode>('card');

  // 搜索筛选
  const [filters, setFilters] = useState<SearchFilters>({ keyword: '' });

  // 抽屉状态
  const [providerDrawerId, setProviderDrawerId] = useState<number | null>(null);
  const [modelDrawerId, setModelDrawerId] = useState<number | null>(null);

  // Provider 表单弹窗
  const [providerModalOpen, setProviderModalOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);
  const [providerForm] = Form.useForm();

  // Model 表单弹窗
  const [modelModalOpen, setModelModalOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<Model | null>(null);
  const [modelForm] = Form.useForm();

  // 当前选中的 Provider（用于添加 Model）

  // Queries
  const { data: providersData } = useProviders({ size: 100 });
  const providers = providersData?.items || [];

  // Mutations
  const createProviderMutation = useCreateProvider();
  const updateProviderMutation = useUpdateProvider();
  const deleteProviderMutation = useDeleteProvider();
  const createModelMutation = useCreateModel();
  const updateModelMutation = useUpdateModel();
  const deleteModelMutation = useDeleteModel();

  // 筛选选项
  const filterOptions = useMemo(
    () => [
      {
        key: 'providerType',
        label: t('search.filterByType'),
        options: [
          { value: 'OPENAI', label: t('type.OPENAI', { ns: 'providers' }) },
          { value: 'ANTHROPIC', label: t('type.ANTHROPIC', { ns: 'providers' }) },
          { value: 'GOOGLE', label: t('type.GEMINI', { ns: 'providers' }) },
          { value: 'AZURE', label: 'Azure' },
          { value: 'CUSTOM', label: t('type.OTHER', { ns: 'providers' }) },
        ],
      },
      {
        key: 'state',
        label: t('search.filterByStatus'),
        options: [
          { value: 'ACTIVE', label: t('state.active', { ns: 'common' }) },
          { value: 'DISABLED', label: t('state.disabled', { ns: 'common' }) },
        ],
      },
      {
        key: 'modelType',
        label: t('search.filterByModelType'),
        options: [
          { value: 'CHAT', label: t('type.CHAT') },
          { value: 'COMPLETION', label: t('type.COMPLETION') },
          { value: 'EMBEDDING', label: t('type.EMBEDDING') },
          { value: 'IMAGE', label: t('type.IMAGE') },
          { value: 'AUDIO', label: t('type.AUDIO') },
        ],
      },
    ],
    [t]
  );

  // Provider 操作
  const handleAddProvider = useCallback(() => {
    setEditingProvider(null);
    providerForm.resetFields();
    setProviderModalOpen(true);
  }, [providerForm]);

  const handleEditProvider = useCallback((provider: Provider) => {
    setEditingProvider(provider);
    providerForm.setFieldsValue(provider);
    setProviderModalOpen(true);
  }, [providerForm]);

  const handleDeleteProvider = useCallback((provider: Provider) => {
    Modal.confirm({
      title: t('confirm.delete', { ns: 'common' }),
      content: t('confirm.deleteProviderDesc', { name: provider.providerName }),
      onOk: async () => {
        await deleteProviderMutation.mutateAsync(provider.id);
        message.success(t('message.success', { ns: 'common' }));
        setProviderDrawerId(null);
      },
    });
  }, [t, deleteProviderMutation]);

  const handleProviderSubmit = useCallback(async (values: CreateProviderRequest | UpdateProviderRequest) => {
    if (editingProvider) {
      await updateProviderMutation.mutateAsync({
        id: editingProvider.id,
        data: values as UpdateProviderRequest,
      });
    } else {
      await createProviderMutation.mutateAsync(values as CreateProviderRequest);
    }
    message.success(t('message.success', { ns: 'common' }));
    setProviderModalOpen(false);
  }, [editingProvider, t, updateProviderMutation, createProviderMutation]);

  // Model 操作
  const handleAddModel = useCallback((provider?: Provider) => {
    setEditingModel(null);
    modelForm.resetFields();
    if (provider) {
      modelForm.setFieldsValue({ providerId: provider.id });
    }
    setModelModalOpen(true);
  }, [modelForm]);

  const handleEditModel = useCallback((model: Model) => {
    setEditingModel(model);
    modelForm.setFieldsValue(model);
    setModelModalOpen(true);
  }, [modelForm]);

  const handleDeleteModel = useCallback((model: Model) => {
    Modal.confirm({
      title: t('confirm.delete', { ns: 'common' }),
      content: t('confirm.deleteModelDesc', { name: model.displayName || model.providerModelId || `Model ${model.id}` }),
      onOk: async () => {
        await deleteModelMutation.mutateAsync(model.id);
        message.success(t('message.success', { ns: 'common' }));
        setModelDrawerId(null);
      },
    });
  }, [t, deleteModelMutation]);

  const handleModelSubmit = useCallback(async (values: CreateModelRequest | UpdateModelRequest) => {
    if (editingModel) {
      await updateModelMutation.mutateAsync({
        id: editingModel.id,
        data: values as UpdateModelRequest,
      });
    } else {
      await createModelMutation.mutateAsync(values as CreateModelRequest);
    }
    message.success(t('message.success', { ns: 'common' }));
    setModelModalOpen(false);
  }, [editingModel, t, updateModelMutation, createModelMutation]);

  // 查看详情
  const handleViewProviderDetail = useCallback((provider: Provider) => {
    setProviderDrawerId(provider.id);
  }, []);

  const handleViewModelDetail = useCallback((model: Model) => {
    setModelDrawerId(model.id);
  }, []);

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 工具栏 */}
      <Card
        style={{
          marginBottom: 16,
          border: 'none',
          boxShadow: isDark
            ? '0 2px 8px rgba(0, 0, 0, 0.3)'
            : '0 2px 8px rgba(0, 0, 0, 0.06)',
        }}
      >
        <div>
          {/* 第一行：搜索筛选栏 */}
          <SearchFilterBar
            placeholder={t('search.placeholder')}
            filters={filterOptions}
            onSearch={setFilters}
            onReset={() => setFilters({ keyword: '' })}
          />

          {/* 第二行：新增按钮（左）+ 视图切换（右） */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 16 }}>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAddProvider}>
              {t('addProvider')}
            </Button>
            <Segmented
              value={viewMode}
              onChange={(value) => setViewMode(value as ViewMode)}
              options={[
                {
                  value: 'card',
                  icon: <AppstoreOutlined />,
                  label: t('viewMode.card'),
                },
                {
                  value: 'table',
                  icon: <UnorderedListOutlined />,
                  label: t('viewMode.table'),
                },
              ]}
            />
          </div>
        </div>
      </Card>

      {/* 内容区域 */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        {viewMode === 'card' ? (
          <CardView
            filters={filters}
            onAddProvider={handleAddProvider}
            onEditProvider={handleEditProvider}
            onDeleteProvider={handleDeleteProvider}
            onAddModel={handleAddModel}
            onEditModel={handleEditModel}
            onDeleteModel={handleDeleteModel}
            onViewProviderDetail={handleViewProviderDetail}
            onViewModelDetail={handleViewModelDetail}
          />
        ) : (
          <TableView
            filters={filters}
            onEditProvider={handleEditProvider}
            onAddModel={handleAddModel}
            onEditModel={handleEditModel}
            onViewProviderDetail={handleViewProviderDetail}
          />
        )}
      </div>

      {/* Provider 详情抽屉 */}
      <ProviderDrawer
        providerId={providerDrawerId}
        onClose={() => setProviderDrawerId(null)}
        onEdit={handleEditProvider}
        onDelete={handleDeleteProvider}
        onAddModel={handleAddModel}
        onEditModel={handleEditModel}
        onViewModelDetail={handleViewModelDetail}
      />

      {/* Model 详情抽屉 */}
      <ModelDrawer
        modelId={modelDrawerId}
        onClose={() => setModelDrawerId(null)}
        onEdit={handleEditModel}
      />

      {/* Provider 表单弹窗 */}
      <Modal
        title={editingProvider ? t('actions.edit', { ns: 'common' }) : t('addProvider')}
        open={providerModalOpen}
        onCancel={() => setProviderModalOpen(false)}
        footer={null}
      >
        <Form
          form={providerForm}
          layout="vertical"
          onFinish={handleProviderSubmit}
        >
          <Form.Item name="providerName" label={t('provider.name')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="providerType" label={t('provider.type')} rules={[{ required: true }]}>
            <Select disabled={!!editingProvider}>
              <Select.Option value="OPENAI">OpenAI</Select.Option>
              <Select.Option value="ANTHROPIC">Anthropic</Select.Option>
              <Select.Option value="GOOGLE">Google</Select.Option>
              <Select.Option value="AZURE">Azure</Select.Option>
              <Select.Option value="CUSTOM">{t('type.OTHER', { ns: 'providers' })}</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="baseUrl" label={t('provider.baseUrl')}>
            <Input />
          </Form.Item>
          {editingProvider && (
            <Form.Item name="state" label={t('provider.state')}>
              <Select>
                <Select.Option value="ACTIVE">{t('state.active', { ns: 'common' })}</Select.Option>
                <Select.Option value="DISABLED">{t('state.disabled', { ns: 'common' })}</Select.Option>
              </Select>
            </Form.Item>
          )}
          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={createProviderMutation.isPending || updateProviderMutation.isPending}
              >
                {t('actions.save', { ns: 'common' })}
              </Button>
              <Button onClick={() => setProviderModalOpen(false)}>
                {t('actions.cancel', { ns: 'common' })}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Model 表单弹窗 */}
      <Modal
        title={editingModel ? t('actions.edit', { ns: 'common' }) : t('addModel')}
        open={modelModalOpen}
        onCancel={() => setModelModalOpen(false)}
        footer={null}
      >
        <Form form={modelForm} layout="vertical" onFinish={handleModelSubmit}>
          <Form.Item name="displayName" label={t('model.name')}>
            <Input />
          </Form.Item>
          <Form.Item name="providerModelId" label={t('model.providerModelId')} rules={[{ required: true }]}>
            <Input disabled={!!editingModel} />
          </Form.Item>
          <Form.Item name="providerId" label={t('model.provider')} rules={[{ required: true }]}>
            <Select disabled={!!editingModel}>
              {providers.map((p) => (
                <Select.Option key={p.id} value={p.id}>
                  {p.providerName}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="contextWindow" label={t('detail.contextWindow')}>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="inputPrice" label={t('detail.inputPrice')}>
            <Input type="number" step="0.01" />
          </Form.Item>
          <Form.Item name="outputPrice" label={t('detail.outputPrice')}>
            <Input type="number" step="0.01" />
          </Form.Item>
          {editingModel && (
            <Form.Item name="state" label={t('model.state')}>
              <Select>
                <Select.Option value="ACTIVE">{t('state.active', { ns: 'common' })}</Select.Option>
                <Select.Option value="DISABLED">{t('state.disabled', { ns: 'common' })}</Select.Option>
              </Select>
            </Form.Item>
          )}
          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                loading={createModelMutation.isPending || updateModelMutation.isPending}
              >
                {t('actions.save', { ns: 'common' })}
              </Button>
              <Button onClick={() => setModelModalOpen(false)}>
                {t('actions.cancel', { ns: 'common' })}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}