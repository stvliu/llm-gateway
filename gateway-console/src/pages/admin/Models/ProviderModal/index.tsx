import { useState, useEffect, useCallback } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Button,
  Space,
  Divider,
  Collapse,
  List,
  Tag,
  Empty,
  message,
  Popconfirm,
} from 'antd';
import {
  PlusOutlined,
  ApiOutlined,
  AppstoreOutlined,
  SettingOutlined,
  StarFilled,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { ProviderTemplateSelector } from '../ProviderTemplateSelector';
import { ApiKeyFormModal, type ApiKeyFormItem } from './ApiKeyFormModal';
import { ModelFormModal, type ModelFormItem } from './ModelFormModal';
import {
  useCreateProvider,
  useUpdateProvider,
  useCreateProviderApiKey,
  useUpdateProviderApiKey,
  useDeleteProviderApiKey,
  useCreateModel,
  useUpdateModel,
  useDeleteModel,
  useProviderKeys,
  useModels,
} from '@/services/query';
import type { Provider, CreateProviderRequest } from '@/types/provider';
import type { ProviderTemplate } from '@/types/template';

interface ProviderModalProps {
  open: boolean;
  provider?: Provider | null;
  onClose: () => void;
  onSuccess: () => void;
}

type ModalStep = 'select-template' | 'fill-form';

/**
 * 供应商弹窗（新增/编辑共用）
 * 新增模式：先创建 Provider，再添加 API Keys 和 Models
 * 编辑模式：直接管理 API Keys 和 Models
 */
export function ProviderModal({ open, provider, onClose, onSuccess }: ProviderModalProps) {
  const { t } = useTranslation('models');
  const isEditMode = !!provider?.id;

  const [form] = Form.useForm();
  const [step, setStep] = useState<ModalStep>(isEditMode ? 'fill-form' : 'select-template');

  // API Keys 状态
  const [apiKeys, setApiKeys] = useState<ApiKeyFormItem[]>([]);
  const [apiKeyModalOpen, setApiKeyModalOpen] = useState(false);
  const [editingApiKey, setEditingApiKey] = useState<ApiKeyFormItem | null>(null);
  const [editingApiKeyIndex, setEditingApiKeyIndex] = useState<number | null>(null);

  // 模型状态
  const [models, setModels] = useState<ModelFormItem[]>([]);
  const [modelModalOpen, setModelModalOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelFormItem | null>(null);
  const [editingModelIndex, setEditingModelIndex] = useState<number | null>(null);
  const [modelModalProviderType, setModelModalProviderType] = useState<string>('');

  // 编辑模式：加载现有数据
  const { data: keysData } = useProviderKeys(provider?.id || 0, { enabled: isEditMode && open });
  const { data: modelsData } = useModels({ providerId: provider?.id, size: 100 }, { enabled: isEditMode && open });

  // Mutations
  const createProviderMutation = useCreateProvider();
  const updateProviderMutation = useUpdateProvider();
  const createApiKeyMutation = useCreateProviderApiKey();
  const updateApiKeyMutation = useUpdateProviderApiKey();
  const deleteApiKeyMutation = useDeleteProviderApiKey();
  const createModelMutation = useCreateModel();
  const updateModelMutation = useUpdateModel();
  const deleteModelMutation = useDeleteModel();

  // 初始化表单
  useEffect(() => {
    if (open) {
      if (isEditMode && provider) {
        // 编辑模式：填充现有数据
        form.setFieldsValue(provider);
        setStep('fill-form');
        setModelModalProviderType(provider.providerType);
      } else {
        // 新增模式：重置
        form.resetFields();
        setStep('select-template');
        setApiKeys([]);
        setModels([]);
        setModelModalProviderType('');
      }
    }
  }, [open, isEditMode, provider, form]);

  // 编辑模式：加载 Keys
  useEffect(() => {
    if (isEditMode && keysData?.keys) {
      const keyItems: ApiKeyFormItem[] = keysData.keys.map(key => ({
        id: key.id,
        keyName: key.keyName,
        keyHint: key.keyHint,
        apiKey: '', // 不显示实际 Key
        priority: key.priority,
        weight: key.weight,
        isDefault: key.isDefault,
        state: key.state,
      }));
      setApiKeys(keyItems);
    }
  }, [isEditMode, keysData]);

  // 编辑模式：加载模型
  useEffect(() => {
    if (isEditMode && modelsData?.items) {
      const modelItems: ModelFormItem[] = modelsData.items
        .filter(model => model.providerModelId)
        .map(model => ({
          id: model.id,
          providerModelId: model.providerModelId!,
          displayName: model.displayName || undefined,
          contextWindow: model.contextWindow || undefined,
          inputPrice: model.inputPrice || undefined,
          outputPrice: model.outputPrice || undefined,
          capabilities: model.capabilities || undefined,
          state: model.state,
        }));
      setModels(modelItems);
    }
  }, [isEditMode, modelsData]);

  // 选择模板
  const handleTemplateSelect = useCallback((template: ProviderTemplate) => {
    const config = template.providerConfig as Record<string, unknown>;
    form.setFieldsValue({
      providerName: config.provider_name || template.templateName,
      providerType: template.providerType,
      baseUrl: config.base_url || '',
      websiteUrl: config.website_url || '',
      apiDocUrl: config.api_doc_url || '',
    });
    setModelModalProviderType(template.providerType);
    setStep('fill-form');
  }, [form]);

  // 自定义供应商
  const handleCustomAdd = useCallback(() => {
    form.resetFields();
    setStep('fill-form');
  }, [form]);

  // API Key 操作
  const handleAddApiKey = useCallback(() => {
    setEditingApiKey(null);
    setEditingApiKeyIndex(null);
    setApiKeyModalOpen(true);
  }, []);

  const handleEditApiKey = useCallback((index: number) => {
    setEditingApiKey(apiKeys[index]);
    setEditingApiKeyIndex(index);
    setApiKeyModalOpen(true);
  }, [apiKeys]);

  const handleDeleteApiKey = useCallback((index: number) => {
    setApiKeys(prev => prev.filter((_, i) => i !== index));
  }, []);

  const handleApiKeySubmit = useCallback((values: ApiKeyFormItem) => {
    if (editingApiKeyIndex !== null) {
      // 编辑
      setApiKeys(prev => prev.map((item, i) => i === editingApiKeyIndex ? values : item));
    } else {
      // 新增
      setApiKeys(prev => [...prev, values]);
    }
    setApiKeyModalOpen(false);
  }, [editingApiKeyIndex]);

  // Model 操作
  const handleAddModel = useCallback(() => {
    setEditingModel(null);
    setEditingModelIndex(null);
    setModelModalOpen(true);
  }, []);

  const handleEditModel = useCallback((index: number) => {
    setEditingModel(models[index]);
    setEditingModelIndex(index);
    setModelModalOpen(true);
  }, [models]);

  const handleDeleteModel = useCallback((index: number) => {
    setModels(prev => prev.filter((_, i) => i !== index));
  }, []);

  const handleModelSubmit = useCallback((values: ModelFormItem) => {
    if (editingModelIndex !== null) {
      // 编辑
      setModels(prev => prev.map((item, i) => i === editingModelIndex ? values : item));
    } else {
      // 新增
      setModels(prev => [...prev, values]);
    }
    setModelModalOpen(false);
  }, [editingModelIndex]);

  // 提交表单
  const handleSubmit = useCallback(async (values: CreateProviderRequest) => {
    try {
      if (isEditMode && provider) {
        // 编辑模式：更新基本信息，Keys 和 Models 单独处理
        await updateProviderMutation.mutateAsync({
          id: provider.id,
          data: values,
        });

        // 处理 API Keys 的变更
        const newKeyIds = new Set(apiKeys.filter(k => k.id).map(k => k.id));

        // 删除不再存在的 Keys
        for (const key of keysData?.keys || []) {
          if (!newKeyIds.has(key.id)) {
            await deleteApiKeyMutation.mutateAsync(key.id);
          }
        }

        // 新增或更新 Keys
        for (const key of apiKeys) {
          if (key.id) {
            await updateApiKeyMutation.mutateAsync({
              id: key.id,
              data: {
                keyName: key.keyName,
                priority: key.priority,
                weight: key.weight,
                isDefault: key.isDefault,
              },
            });
          } else {
            await createApiKeyMutation.mutateAsync({
              providerId: provider.id,
              keyName: key.keyName,
              apiKey: key.apiKey,
              priority: key.priority,
              weight: key.weight,
              isDefault: key.isDefault,
            });
          }
        }

        // 处理模型的变更
        const newModelIds = new Set(models.filter(m => m.id).map(m => m.id));

        // 删除不再存在的模型
        for (const model of modelsData?.items || []) {
          if (!newModelIds.has(model.id)) {
            await deleteModelMutation.mutateAsync(model.id);
          }
        }

        // 新增或更新模型
        for (const model of models) {
          if (model.id) {
            await updateModelMutation.mutateAsync({
              id: model.id,
              data: {
                displayName: model.displayName,
                contextWindow: model.contextWindow,
                inputPrice: model.inputPrice,
                outputPrice: model.outputPrice,
                capabilities: model.capabilities,
              },
            });
          } else {
            await createModelMutation.mutateAsync({
              providerId: provider.id,
              providerModelId: model.providerModelId,
              displayName: model.displayName,
              contextWindow: model.contextWindow,
              inputPrice: model.inputPrice,
              outputPrice: model.outputPrice,
              capabilities: model.capabilities,
            });
          }
        }

        message.success(t('message.success', { ns: 'common' }));
        onSuccess();
        onClose();
      } else {
        // 新增模式：一次性创建 Provider + Keys + Models
        const createRequest: CreateProviderRequest = {
          ...values,
          apiKeys: apiKeys.map(key => ({
            keyName: key.keyName,
            apiKey: key.apiKey,
            priority: key.priority,
            weight: key.weight,
            isDefault: key.isDefault,
          })),
          models: models.map(model => ({
            providerModelId: model.providerModelId,
            displayName: model.displayName,
            contextWindow: model.contextWindow,
            inputPrice: model.inputPrice,
            outputPrice: model.outputPrice,
            capabilities: model.capabilities,
          })),
        };

        await createProviderMutation.mutateAsync(createRequest);
        message.success(t('message.success', { ns: 'common' }));
        onSuccess();
        onClose();
      }
    } catch (error) {
      console.error('Failed to save provider:', error);
    }
  }, [
    isEditMode, provider, apiKeys, models, keysData, modelsData,
    updateProviderMutation, createProviderMutation,
    createApiKeyMutation, updateApiKeyMutation, deleteApiKeyMutation,
    createModelMutation, updateModelMutation, deleteModelMutation,
    t, onSuccess, onClose,
  ]);

  const isLoading = createProviderMutation.isPending || updateProviderMutation.isPending;

  return (
    <Modal
      title={isEditMode ? t('actions.edit', { ns: 'common' }) + ' - ' + provider?.providerName : t('addProvider')}
      open={open}
      onCancel={onClose}
      footer={null}
      width={720}
      styles={{ body: { maxHeight: '70vh', overflowY: 'auto' } }}
    >
      {/* 新增模式：自定义供应商链接 */}
      {!isEditMode && step === 'select-template' && (
        <div style={{ marginBottom: 16, textAlign: 'right' }}>
          <a onClick={handleCustomAdd} style={{ fontSize: 13 }}>
            {t('provider.customAdd', { defaultValue: '自定义供应商' })}
          </a>
        </div>
      )}

      {/* 新增模式：模板选择 */}
      {!isEditMode && step === 'select-template' && (
        <ProviderTemplateSelector onSelect={handleTemplateSelect} />
      )}

      {/* 表单模式 */}
      {(isEditMode || step === 'fill-form') && (
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          {/* 基本信息 */}
          <Collapse
            defaultActiveKey={['basic']}
            items={[
              {
                key: 'basic',
                label: (
                  <Space>
                    <SettingOutlined />
                    {t('detail.basicInfo', { defaultValue: '基本信息' })}
                  </Space>
                ),
                children: (
                  <>
                    <Form.Item name="providerName" label={t('provider.name')} rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                    <Form.Item name="providerType" label={t('provider.type')} rules={[{ required: true }]}>
                      <Select disabled={isEditMode} onChange={(value) => setModelModalProviderType(value)}>
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
                    {isEditMode && (
                      <Form.Item name="state" label={t('provider.state')}>
                        <Select>
                          <Select.Option value="ACTIVE">{t('state.active', { ns: 'common' })}</Select.Option>
                          <Select.Option value="DISABLED">{t('state.disabled', { ns: 'common' })}</Select.Option>
                        </Select>
                      </Form.Item>
                    )}
                  </>
                ),
              },
            ]}
          />

          <Divider />

          {/* API Keys 管理 */}
          <Collapse
            defaultActiveKey={isEditMode ? ['apiKeys'] : []}
            items={[
              {
                key: 'apiKeys',
                label: (
                  <Space>
                    <ApiOutlined />
                    {t('provider.apiKeys', { defaultValue: 'API Keys' })}
                    {apiKeys.length > 0 && <Tag color="blue">{apiKeys.length}</Tag>}
                  </Space>
                ),
                extra: (
                  <Button
                    type="link"
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={(e) => { e.stopPropagation(); handleAddApiKey(); }}
                  >
                    {t('actions.add', { ns: 'common' })}
                  </Button>
                ),
                children: apiKeys.length > 0 ? (
                  <List
                    size="small"
                    dataSource={apiKeys}
                    renderItem={(item, index) => (
                      <List.Item
                        actions={[
                          <Button key="edit" type="link" size="small" onClick={() => handleEditApiKey(index)}>
                            {t('actions.edit', { ns: 'common' })}
                          </Button>,
                          <Popconfirm
                            key="delete"
                            title={t('confirm.delete', { ns: 'common' })}
                            onConfirm={() => handleDeleteApiKey(index)}
                          >
                            <Button type="link" size="small" danger>
                              {t('actions.delete', { ns: 'common' })}
                            </Button>
                          </Popconfirm>,
                        ]}
                      >
                        <Space>
                          {item.isDefault && <StarFilled style={{ color: '#faad14' }} />}
                          <span>{item.keyName}</span>
                          {item.keyHint && <Tag>{item.keyHint}</Tag>}
                        </Space>
                      </List.Item>
                    )}
                  />
                ) : (
                  <Empty description={t('provider.noApiKeys', { defaultValue: '暂无 API Key，点击上方按钮添加' })} />
                ),
              },
            ]}
          />

          <Divider />

          {/* 模型管理 */}
          <Collapse
            defaultActiveKey={isEditMode ? ['models'] : []}
            items={[
              {
                key: 'models',
                label: (
                  <Space>
                    <AppstoreOutlined />
                    {t('provider.models', { defaultValue: '模型' })}
                    {models.length > 0 && <Tag color="blue">{models.length}</Tag>}
                  </Space>
                ),
                extra: (
                  <Button
                    type="link"
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={(e) => { e.stopPropagation(); handleAddModel(); }}
                  >
                    {t('actions.add', { ns: 'common' })}
                  </Button>
                ),
                children: models.length > 0 ? (
                  <List
                    size="small"
                    dataSource={models}
                    renderItem={(item, index) => (
                      <List.Item
                        actions={[
                          <Button key="edit" type="link" size="small" onClick={() => handleEditModel(index)}>
                            {t('actions.edit', { ns: 'common' })}
                          </Button>,
                          <Popconfirm
                            key="delete"
                            title={t('confirm.delete', { ns: 'common' })}
                            onConfirm={() => handleDeleteModel(index)}
                          >
                            <Button type="link" size="small" danger>
                              {t('actions.delete', { ns: 'common' })}
                            </Button>
                          </Popconfirm>,
                        ]}
                      >
                        <Space>
                          <span>{item.displayName || item.providerModelId}</span>
                          {item.contextWindow && <Tag>{item.contextWindow.toLocaleString()} tokens</Tag>}
                        </Space>
                      </List.Item>
                    )}
                  />
                ) : (
                  <Empty description={t('provider.noModels', { defaultValue: '暂无模型，点击上方按钮添加' })} />
                ),
              },
            ]}
          />

          <Divider />

          {/* 操作按钮 */}
          <Form.Item style={{ marginBottom: 0 }}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              {!isEditMode && step === 'fill-form' && (
                <Button onClick={() => setStep('select-template')}>
                  {t('actions.back', { ns: 'common', defaultValue: '返回' })}
                </Button>
              )}
              <Button onClick={onClose}>
                {t('actions.cancel', { ns: 'common' })}
              </Button>
              <Button type="primary" htmlType="submit" loading={isLoading}>
                {t('actions.save', { ns: 'common' })}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      )}

      {/* API Key 表单弹窗 */}
      <ApiKeyFormModal
        open={apiKeyModalOpen}
        editingKey={editingApiKey}
        onClose={() => setApiKeyModalOpen(false)}
        onSubmit={handleApiKeySubmit}
      />

      {/* Model 表单弹窗 */}
      <ModelFormModal
        open={modelModalOpen}
        editingModel={editingModel}
        providerType={modelModalProviderType}
        onClose={() => setModelModalOpen(false)}
        onSubmit={handleModelSubmit}
        onShowTemplateSelector={() => setModelModalOpen(false)}
      />
    </Modal>
  );
}
