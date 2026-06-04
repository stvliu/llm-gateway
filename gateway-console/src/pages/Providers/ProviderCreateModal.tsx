import { useState, useCallback, useEffect } from 'react';
import { Modal, Steps, App, Button, Space, Card, Tag, Spin, Typography, Form, Input } from 'antd';
import { RightOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviderCatalogs, useMaterializeProvider } from '@/services/query/useCatalog';
import { useCreateProvider } from '@/services/query/useProviders';
import { useAddChannel, useCreateChannelCredential, useCreateChannelModel } from '@/services/query/useChannels';
import { ProviderIcon } from '@/components/ui';
import CredentialStep from './CredentialStep';
import { ModelSetupStep } from './ModelSetupStep';
import type { ProviderCatalog } from '@/types/catalog';
import type { CredentialEntry } from './CredentialStep';

const { Text, Paragraph } = Typography;

const DRAFT_KEY = 'provider-create-draft';

interface DraftData {
  step: number;
  selectedCatalogCode: string | null;
  isCustomMode: boolean;
  credentials: CredentialEntry[];
  selectedModels: string[];
  customFormValues: Record<string, string>;
}

function saveDraft(draft: DraftData) {
  try { sessionStorage.setItem(DRAFT_KEY, JSON.stringify(draft)); } catch { /* ignore */ }
}

function loadDraft(): DraftData | null {
  try {
    const raw = sessionStorage.getItem(DRAFT_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch { return null; }
}

function clearDraft() {
  try { sessionStorage.removeItem(DRAFT_KEY); } catch { /* ignore */ }
}

interface Props {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}

/**
 * 供应商创建弹窗 — 三步向导
 *
 * Step 1: 选择供应商（从模板或自定义创建）
 * Step 2: 配置 API Key
 * Step 3: 选择模型
 */
export function ProviderCreateModal({ open, onClose, onCreated }: Props) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();
  const materializeMutation = useMaterializeProvider();
  const createMutation = useCreateProvider();
  const addChannelMutation = useAddChannel();
  const createCredentialMutation = useCreateChannelCredential();
  const createModelMutation = useCreateChannelModel();
  const { data: catalogList, isLoading: catalogLoading } = useProviderCatalogs();
  const [customForm] = Form.useForm();

  const [currentStep, setCurrentStep] = useState(0);
  const [selectedCatalog, setSelectedCatalog] = useState<ProviderCatalog | null>(null);
  const [isCustomMode, setIsCustomMode] = useState(false);
  const [credentials, setCredentials] = useState<CredentialEntry[]>([]);
  const [selectedModels, setSelectedModels] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  // 恢复草稿
  useEffect(() => {
    if (open) {
      const draft = loadDraft();
      if (draft) {
        setCurrentStep(draft.step);
        setIsCustomMode(draft.isCustomMode);
        setCredentials(draft.credentials);
        setSelectedModels(draft.selectedModels);
        if (draft.customFormValues) {
          customForm.setFieldsValue(draft.customFormValues);
        }
        if (draft.selectedCatalogCode && catalogList) {
          const cat = catalogList.find((c) => c.code === draft.selectedCatalogCode);
          if (cat) setSelectedCatalog(cat);
        }
      }
    }
  }, [open, catalogList, customForm]);

  // 自动保存草稿
  useEffect(() => {
    if (open) {
      saveDraft({
        step: currentStep,
        selectedCatalogCode: selectedCatalog?.code ?? null,
        isCustomMode,
        credentials,
        selectedModels,
        customFormValues: customForm.getFieldsValue(),
      });
    }
  }, [open, currentStep, selectedCatalog, isCustomMode, credentials, selectedModels, customForm]);

  const handleClose = useCallback(() => {
    clearDraft();
    setCurrentStep(0);
    setSelectedCatalog(null);
    setIsCustomMode(false);
    setCredentials([]);
    setSelectedModels([]);
    customForm.resetFields();
    onClose();
  }, [onClose, customForm]);

  /** Step 1: 选择目录模板 */
  const handleSelectCatalog = useCallback((catalog: ProviderCatalog) => {
    setSelectedCatalog(catalog);
    setIsCustomMode(false);
    setCurrentStep(1);
  }, []);

  /** Step 1: 切换到自定义创建 */
  const handleSwitchToCustom = useCallback(() => {
    setSelectedCatalog(null);
    setIsCustomMode(true);
    setCurrentStep(1);
  }, []);

  /** 下一步 */
  const handleNext = useCallback(() => {
    setCurrentStep((s) => Math.min(s + 1, 2));
  }, []);

  /** 上一步 */
  const handlePrev = useCallback(() => {
    setCurrentStep((s) => Math.max(s - 1, 0));
  }, []);

  /** 完成创建：供应商 → 通道 → 凭证 → 模型关联 */
  const handleFinish = useCallback(async () => {
    setSaving(true);
    try {
      if (selectedCatalog && !isCustomMode) {
        // 模板模式：物化创建
        await materializeMutation.mutateAsync(selectedCatalog.code);
      } else {
        // 自定义模式：先创建供应商
        const values = await customForm.validateFields();
        const provider = await createMutation.mutateAsync(values);

        // 创建默认通道
        const channel = await addChannelMutation.mutateAsync({
          providerId: provider.id,
          data: {
            name: `${provider.providerName} 默认通道`,
            providerId: provider.id,
            billingMode: 'pay_as_you_go',
          },
        });

        // 保存 API Key 凭证
        const validKeys = credentials.filter((c) => c.value.trim());
        for (const cred of validKeys) {
          await createCredentialMutation.mutateAsync({
            channelId: channel.id,
            data: { apiKey: cred.value.trim() },
          });
        }

        // 关联选中的模型
        for (const modelName of selectedModels) {
          await createModelMutation.mutateAsync({
            channelId: channel.id,
            data: { modelName, upstreamModelName: modelName },
          });
        }
      }
      message.success(t('createSuccess', { defaultValue: '供应商创建成功' }));
      clearDraft();
      onCreated();
      handleClose();
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      const errMsg = error instanceof Error ? error.message : '';
      message.error(errMsg || t('createFailed', { defaultValue: '创建失败' }));
    } finally {
      setSaving(false);
    }
  }, [selectedCatalog, isCustomMode, materializeMutation, createMutation, customForm, credentials, selectedModels, addChannelMutation, createCredentialMutation, createModelMutation, message, t, onCreated, handleClose]);

  // ========== 步骤内容 ==========

  /** Step 1: 选择供应商 */
  const renderStep1 = () => (
    <div>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        {t('template.selectHint', { defaultValue: '选择模板可快速创建供应商和模型' })}
      </Paragraph>

      <Spin spinning={catalogLoading}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 12, maxHeight: 360, overflow: 'auto' }}>
          {catalogList?.map((catalog) => (
            <Card
              key={catalog.code}
              hoverable
              size="small"
              onClick={() => handleSelectCatalog(catalog)}
              style={{ cursor: 'pointer', border: selectedCatalog?.code === catalog.code ? '2px solid #1677ff' : undefined }}
            >
              <Card.Meta
                avatar={
                  <ProviderIcon providerId={catalog.code} size={24} />
                }
                title={catalog.name}
                description={
                  <Space direction="vertical" size={4}>
                    <Text type="secondary" style={{ fontSize: 12 }}>{catalog.code}</Text>
                    <Tag color={catalog.materialized ? 'green' : 'blue'} style={{ fontSize: 11 }}>
                      {catalog.materialized
                        ? t('template.materialized', { defaultValue: '已物化' })
                        : t('template.notMaterialized', { defaultValue: '未物化' })
                      }
                    </Tag>
                  </Space>
                }
              />
            </Card>
          ))}
        </div>
      </Spin>

      {(!catalogList || catalogList.length === 0) && !catalogLoading && (
        <div style={{ textAlign: 'center', padding: 32 }}>
          <Text type="secondary">{t('template.noTemplate', { defaultValue: '暂无模板' })}</Text>
        </div>
      )}

      <div style={{ marginTop: 16, textAlign: 'center' }}>
        <Button type="link" onClick={handleSwitchToCustom}>
          {t('template.customCreate', { defaultValue: '自定义创建' })} <RightOutlined />
        </Button>
      </div>
    </div>
  );

  /** Step 2: 配置接入（自定义模式显示表单，模板模式显示 CredentialStep） */
  const renderStep2 = () => {
    if (isCustomMode) {
      return (
        <Form
          form={customForm}
          layout="vertical"
          autoComplete="off"
        >
          <Form.Item
            name="code"
            label={t('fields.code', { defaultValue: '品牌标识' })}
            rules={[
              { required: true, message: t('fields.codeRequired', { defaultValue: '请输入品牌标识' }) },
              { pattern: /^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$/, message: t('fields.codePattern', { defaultValue: '只能包含小写字母、数字和中划线，长度3-64' }) },
            ]}
            extra={t('fields.codeExtra', { defaultValue: '如 openai、anthropic，全局唯一，创建后不可修改' })}
          >
            <Input placeholder="openai" />
          </Form.Item>
          <Form.Item
            name="providerName"
            label={t('fields.providerName', { defaultValue: '供应商名称' })}
            rules={[{ required: true, message: t('fields.providerNameRequired', { defaultValue: '请输入供应商名称' }) }]}
          >
            <Input placeholder="OpenAI" />
          </Form.Item>
          <Form.Item
            name="websiteUrl"
            label={t('fields.websiteUrl', { defaultValue: '官网地址' })}
          >
            <Input placeholder="https://openai.com" />
          </Form.Item>
          <Form.Item
            name="apiDocUrl"
            label={t('fields.apiDocUrl', { defaultValue: 'API 文档地址' })}
          >
            <Input placeholder="https://platform.openai.com/docs" />
          </Form.Item>
          <Form.Item
            name="description"
            label={t('fields.description', { defaultValue: '描述' })}
          >
            <Input.TextArea rows={3} placeholder={t('fields.descriptionPlaceholder', { defaultValue: '供应商描述信息' })} />
          </Form.Item>
        </Form>
      );
    }
    return <CredentialStep credentials={credentials} onChange={setCredentials} />;
  };

  /** Step 3: 选择模型 */
  const renderStep3 = () => {
    const providerCode = selectedCatalog?.code || customForm.getFieldValue('code');
    return (
      <ModelSetupStep
        providerCode={providerCode}
        selectedModels={selectedModels}
        onSelectedModelsChange={setSelectedModels}
      />
    );
  };

  const steps = [
    { title: t('wizard.selectProvider', { defaultValue: '选择供应商' }), content: renderStep1() },
    { title: t('wizard.configureKey', { defaultValue: '配置接入' }), content: renderStep2() },
    { title: t('wizard.selectModels', { defaultValue: '选择模型' }), content: renderStep3() },
  ];

  return (
    <Modal
      title={t('wizard.quickAdd', { defaultValue: '快速接入供应商' })}
      open={open}
      onCancel={handleClose}
      footer={
        <Space>
          {currentStep > 0 && (
            <Button onClick={handlePrev}>
              {t('wizard.previous', { defaultValue: '上一步' })}
            </Button>
          )}
          {currentStep < steps.length - 1 ? (
            <Button type="primary" onClick={handleNext}>
              {t('wizard.next', { defaultValue: '下一步' })}
            </Button>
          ) : (
            <Button type="primary" onClick={handleFinish} loading={saving}>
              {t('wizard.create', { defaultValue: '完成创建' })}
            </Button>
          )}
        </Space>
      }
      width={currentStep === 0 ? 720 : 640}
      destroyOnHidden
    >
      <Steps current={currentStep} items={steps.map((s) => ({ title: s.title }))} style={{ marginBottom: 24 }} />
      {steps[currentStep].content}
    </Modal>
  );
}