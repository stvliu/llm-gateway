import { useState, useMemo, useCallback } from 'react';
import { Card, Button, Space, Segmented, Modal, Form, Input, Select, message, InputNumber, Row, Col, Checkbox } from 'antd';
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
import { ModelTemplateSelector, type ModelTemplate } from './ModelTemplateSelector';
import { ProviderTemplateSelector } from './ProviderTemplateSelector';
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
import type { ProviderTemplate } from '@/types/template';

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
  const [providerModalStep, setProviderModalStep] = useState<'select-template' | 'fill-form'>('select-template');

  // Model 表单弹窗
  const [modelModalOpen, setModelModalOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<Model | null>(null);
  const [modelForm] = Form.useForm();
  const [currentProvider, setCurrentProvider] = useState<Provider | null>(null);
  const [showCustomForm, setShowCustomForm] = useState(false);

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
    setProviderModalStep('select-template');
    setProviderModalOpen(true);
  }, [providerForm]);

  const handleEditProvider = useCallback((provider: Provider) => {
    setEditingProvider(provider);
    providerForm.setFieldsValue(provider);
    setProviderModalStep('fill-form');
    setProviderModalOpen(true);
  }, [providerForm]);

  // 从模板选择供应商配置
  const handleProviderTemplateSelect = useCallback((template: ProviderTemplate) => {
    const config = template.providerConfig as Record<string, unknown>;
    providerForm.setFieldsValue({
      providerName: config.provider_name || template.templateName,
      providerType: template.providerType,
      baseUrl: config.base_url || '',
      websiteUrl: config.website_url || '',
      apiDocUrl: config.api_doc_url || '',
    });
    setProviderModalStep('fill-form');
  }, [providerForm]);

  // 切换到自定义供应商表单
  const handleCustomProviderAdd = useCallback(() => {
    providerForm.resetFields();
    setProviderModalStep('fill-form');
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
    setShowCustomForm(false);
    if (provider) {
      modelForm.setFieldsValue({ providerId: provider.id });
      setCurrentProvider(provider);
    } else {
      setCurrentProvider(null);
    }
    setModelModalOpen(true);
  }, [modelForm]);

  const handleEditModel = useCallback((model: Model) => {
    setEditingModel(model);
    setShowCustomForm(true);
    modelForm.setFieldsValue({
      ...model,
      capabilities: model.capabilities
        ? Object.entries(model.capabilities)
            .filter(([, v]) => v)
            .map(([k]) => k)
        : [],
    });
    const provider = providers.find(p => p.id === model.providerId);
    setCurrentProvider(provider || null);
    setModelModalOpen(true);
  }, [modelForm, providers]);

  // 从模板快速添加模型
  const handleTemplateSelect = useCallback(async (template: ModelTemplate) => {
    if (!currentProvider) return;

    try {
      await createModelMutation.mutateAsync({
        providerId: currentProvider.id,
        providerModelId: template.id,
        displayName: template.displayName,
        contextWindow: template.contextWindow,
        inputPrice: template.inputPrice,
        outputPrice: template.outputPrice,
        capabilities: template.capabilities,
      });
      message.success(t('message.modelAdded', { defaultValue: '模型添加成功' }));
      setModelModalOpen(false);
    } catch (error) {
      // 如果快速添加失败，回退到表单模式并记录错误
      console.error('Failed to add model from template:', error);
      setShowCustomForm(true);
      modelForm.setFieldsValue({
        providerModelId: template.id,
        displayName: template.displayName,
        contextWindow: template.contextWindow,
        inputPrice: template.inputPrice,
        outputPrice: template.outputPrice,
        capabilities: template.capabilities
          ? Object.entries(template.capabilities)
              .filter(([, v]) => v)
              .map(([k]) => k)
          : [],
      });
    }
  }, [currentProvider, createModelMutation, modelForm, t]);

  // 切换到自定义表单模式
  const handleCustomAdd = useCallback(() => {
    setShowCustomForm(true);
  }, []);

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
        title={
          editingProvider
            ? t('actions.edit', { ns: 'common' })
            : (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingRight: 32 }}>
                <span>{t('addProvider')}</span>
                {providerModalStep === 'select-template' && (
                  <a
                    onClick={handleCustomProviderAdd}
                    style={{ fontSize: 13, fontWeight: 'normal' }}
                  >
                    {t('provider.customAdd', { defaultValue: '自定义供应商' })}
                  </a>
                )}
              </div>
            )
        }
        open={providerModalOpen}
        onCancel={() => setProviderModalOpen(false)}
        footer={null}
        width={editingProvider ? 520 : 680}
      >
        {/* 新增模式：显示模板选择器 */}
        {!editingProvider && providerModalStep === 'select-template' && (
          <ProviderTemplateSelector
            onSelect={handleProviderTemplateSelect}
          />
        )}

        {/* 编辑模式或表单模式：显示表单 */}
        {(editingProvider || providerModalStep === 'fill-form') && (
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
                <Select.Option value="DEEPSEEK">DeepSeek</Select.Option>
                <Select.Option value="QWEN">Qwen</Select.Option>
                <Select.Option value="ZHIPU">Zhipu</Select.Option>
                <Select.Option value="MOONSHOT">Moonshot</Select.Option>
                <Select.Option value="BAICHUAN">Baichuan</Select.Option>
                <Select.Option value="MINIMAX">MiniMax</Select.Option>
                <Select.Option value="WENXIN">Wenxin</Select.Option>
                <Select.Option value="VOLCENGINE">Volcengine</Select.Option>
                <Select.Option value="TENCENT">Tencent</Select.Option>
                <Select.Option value="XUNFEI">Xunfei</Select.Option>
                <Select.Option value="CUSTOM">{t('type.OTHER', { ns: 'providers' })}</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item name="baseUrl" label={t('provider.baseUrl')}>
              <Input placeholder="https://api.example.com" />
            </Form.Item>
            <Form.Item name="websiteUrl" label={t('provider.websiteUrl', { defaultValue: '官网地址' })}>
              <Input placeholder="https://example.com" />
            </Form.Item>
            <Form.Item name="apiDocUrl" label={t('provider.apiDocUrl', { defaultValue: 'API 文档' })}>
              <Input placeholder="https://docs.example.com" />
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
                {!editingProvider && (
                  <Button onClick={() => setProviderModalStep('select-template')}>
                    {t('actions.back', { ns: 'common', defaultValue: '返回' })}
                  </Button>
                )}
                <Button onClick={() => setProviderModalOpen(false)}>
                  {t('actions.cancel', { ns: 'common' })}
                </Button>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={createProviderMutation.isPending || updateProviderMutation.isPending}
                >
                  {t('actions.save', { ns: 'common' })}
                </Button>
              </Space>
            </Form.Item>
          </Form>
        )}
      </Modal>

      {/* Model 表单弹窗 */}
      <Modal
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingRight: 24 }}>
            <span>{editingModel ? t('actions.edit', { ns: 'common' }) : t('addModel')}</span>
            {!editingModel && currentProvider && !showCustomForm && (
              <Button type="link" size="small" onClick={handleCustomAdd}>
                {t('template.customAdd', { defaultValue: '自定义模型' })}
              </Button>
            )}
          </div>
        }
        open={modelModalOpen}
        onCancel={() => setModelModalOpen(false)}
        footer={null}
        width={560}
      >
        {/* 新增模式：显示模板选择器 */}
        {!editingModel && currentProvider && !showCustomForm && (
          <ModelTemplateSelector
            providerType={currentProvider.providerType}
            onSelect={handleTemplateSelect}
          />
        )}

        {/* 编辑模式或自定义模式：显示完整表单 */}
        {(editingModel || showCustomForm) && (
          <Form form={modelForm} layout="vertical" onFinish={handleModelSubmit}>
            <Form.Item name="providerId" label={t('model.provider')} rules={[{ required: true }]} hidden>
              <Input />
            </Form.Item>

            <Form.Item name="providerModelId" label={t('model.providerModelId')} rules={[{ required: true }]}>
              <Input disabled={!!editingModel} placeholder="gpt-4o" />
            </Form.Item>

            <Form.Item name="displayName" label={t('model.name')}>
              <Input placeholder="GPT-4o" />
            </Form.Item>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="contextWindow" label={t('detail.contextWindow')}>
                  <InputNumber
                    style={{ width: '100%' }}
                    formatter={(v) => v ? `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
                    parser={(v) => v!.replace(/,/g, '') as any}
                    addonAfter="tokens"
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="capabilities" label={t('model.capabilities', { defaultValue: '能力' })}>
                  <Checkbox.Group>
                    <Space direction="vertical">
                      <Checkbox value="chat">Chat</Checkbox>
                      <Checkbox value="vision">Vision</Checkbox>
                      <Checkbox value="embedding">Embedding</Checkbox>
                    </Space>
                  </Checkbox.Group>
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="inputPrice" label={t('detail.inputPrice')}>
                  <InputNumber
                    style={{ width: '100%' }}
                    step="0.0001"
                    precision={4}
                    min={0}
                    addonBefore="$"
                    addonAfter="/M"
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="outputPrice" label={t('detail.outputPrice')}>
                  <InputNumber
                    style={{ width: '100%' }}
                    step="0.0001"
                    precision={4}
                    min={0}
                    addonBefore="$"
                    addonAfter="/M"
                  />
                </Form.Item>
              </Col>
            </Row>

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
        )}

        {/* 无 Provider 时的提示 */}
        {!editingModel && !currentProvider && (
          <div style={{ textAlign: 'center', padding: 24 }}>
            <p style={{ color: '#999' }}>{t('model.selectProvider', { defaultValue: '请先选择供应商' })}</p>
          </div>
        )}
      </Modal>
    </div>
  );
}