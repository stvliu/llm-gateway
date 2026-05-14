import { useState, useCallback } from 'react';
import { Modal, Button, Space, Steps, message } from 'antd';
import {
  SettingOutlined,
  ApiOutlined,
  AppstoreOutlined,
  CheckOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { BasicInfoStep } from './BasicInfoStep';
import { ApiKeySetupStep } from './ApiKeySetupStep';
import { ModelSetupStep } from './ModelSetupStep';
import { useCreateProvider } from '@/services/query';
import type { Provider, CreateProviderRequest, NestedApiKeyRequest, NestedModelRequest } from '@/types/provider';
import type { ProviderTemplate } from '@/types/template';

/**
 * 检查供应商名称是否重复
 */
function isNameDuplicate(name: string, providers: Provider[], excludeId?: number): boolean {
  return providers.some(p =>
    p.id !== excludeId && p.providerName.toLowerCase() === name.toLowerCase()
  );
}

/**
 * 检查 API URL 是否重复
 */
function isBaseUrlDuplicate(baseUrl: string, providers: Provider[], excludeId?: number): boolean {
  const normalizedUrl = baseUrl.toLowerCase().replace(/\/+$/, '');
  return providers.some(p =>
    p.id !== excludeId && p.baseUrl?.toLowerCase().replace(/\/+$/, '') === normalizedUrl
  );
}

interface ProviderCreateModalProps {
  open: boolean;
  providers: Provider[];
  onClose: () => void;
  onCreated: (provider: Provider) => void;
}

/**
 * 供应商创建 Modal
 * 3 步向导：基本信息 → API Key → 模型配置
 */
export function ProviderCreateModal({
  open,
  providers,
  onClose,
  onCreated,
}: ProviderCreateModalProps) {
  const { t } = useTranslation('providers');

  // 向导状态
  const [currentStep, setCurrentStep] = useState(0);
  const [selectedTemplate, setSelectedTemplate] = useState<ProviderTemplate | null>(null);
  const [basicInfo, setBasicInfo] = useState<CreateProviderRequest | null>(null);
  const [tempApiKeys, setTempApiKeys] = useState<NestedApiKeyRequest[]>([]);
  const [tempModels, setTempModels] = useState<NestedModelRequest[]>([]);
  const [saving, setSaving] = useState(false);

  // Mutations
  const createMutation = useCreateProvider();

  // 向导步骤配置
  const wizardSteps = [
    {
      title: t('wizard.basicInfo', { defaultValue: '基本信息' }),
      icon: <SettingOutlined />,
    },
    {
      title: t('wizard.apiKeys', { defaultValue: 'API Key' }),
      icon: <ApiOutlined />,
    },
    {
      title: t('wizard.models', { defaultValue: '模型配置' }),
      icon: <AppstoreOutlined />,
    },
  ];

  // 关闭时重置状态
  const handleClose = useCallback(() => {
    setCurrentStep(0);
    setSelectedTemplate(null);
    setBasicInfo(null);
    setTempApiKeys([]);
    setTempModels([]);
    onClose();
  }, [onClose]);

  // 上一步
  const handleStepPrevious = useCallback(() => {
    setCurrentStep((prev) => Math.max(0, prev - 1));
  }, []);

  // 下一步
  const handleStepNext = useCallback(() => {
    if (currentStep === 0) {
      if (!basicInfo?.providerName || !basicInfo?.providerType || !basicInfo?.baseUrl) {
        message.warning(t('validation.basicInfoRequired', { defaultValue: '请填写必填信息' }));
        return;
      }
      if (isNameDuplicate(basicInfo.providerName, providers)) {
        message.warning(t('validation.nameDuplicate', { defaultValue: '供应商名称已存在，请使用其他名称' }));
        return;
      }
      if (isBaseUrlDuplicate(basicInfo.baseUrl, providers)) {
        message.warning(t('validation.baseUrlDuplicate', { defaultValue: 'API 地址已存在，请使用其他地址' }));
        return;
      }
    }
    if (currentStep === 1) {
      const validKeys = tempApiKeys.filter(k => k.apiKey);
      if (validKeys.length === 0) {
        message.warning(t('validation.apiKeyRequired', { defaultValue: '请至少配置一个有效的 API Key' }));
        return;
      }
    }
    setCurrentStep((prev) => Math.min(2, prev + 1));
  }, [currentStep, basicInfo, tempApiKeys, providers, t]);

  // 完成创建
  const handleCreateProvider = useCallback(async () => {
    if (!basicInfo) {
      message.warning(t('validation.basicInfoRequired', { defaultValue: '请填写基本信息' }));
      return;
    }

    if (!basicInfo.providerName || !basicInfo.providerType || !basicInfo.baseUrl) {
      message.warning(t('validation.basicInfoRequired', { defaultValue: '请填写必填信息' }));
      return;
    }

    if (isNameDuplicate(basicInfo.providerName, providers)) {
      message.warning(t('validation.nameDuplicate', { defaultValue: '供应商名称已存在，请使用其他名称' }));
      return;
    }

    if (isBaseUrlDuplicate(basicInfo.baseUrl, providers)) {
      message.warning(t('validation.baseUrlDuplicate', { defaultValue: 'API 地址已存在，请使用其他地址' }));
      return;
    }

    const validKeys = tempApiKeys.filter(k => k.apiKey);
    if (validKeys.length === 0) {
      message.warning(t('validation.apiKeyRequired', { defaultValue: '请至少配置一个有效的 API Key' }));
      return;
    }

    try {
      setSaving(true);
      const request: CreateProviderRequest = {
        ...basicInfo,
        apiKeys: validKeys,
        models: tempModels,
      };

      const newProvider = await createMutation.mutateAsync(request);
      message.success(t('message.createSuccess', { defaultValue: '供应商创建成功' }));
      onCreated(newProvider);
      handleClose();
    } catch {
      // 错误已在 mutation 中处理
    } finally {
      setSaving(false);
    }
  }, [basicInfo, tempApiKeys, tempModels, providers, createMutation, t, onCreated, handleClose]);

  // 渲染向导步骤内容
  const renderStepContent = () => {
    switch (currentStep) {
      case 0:
        return (
          <BasicInfoStep
            basicInfo={basicInfo}
            onChange={setBasicInfo}
            onTemplateLoad={setSelectedTemplate}
            onModelsChange={setTempModels}
          />
        );
      case 1:
        return (
          <ApiKeySetupStep
            apiKeys={tempApiKeys}
            onChange={setTempApiKeys}
            providerType={basicInfo?.providerType}
            baseUrl={basicInfo?.baseUrl}
          />
        );
      case 2:
        return (
          <ModelSetupStep
            providerType={basicInfo?.providerType || selectedTemplate?.providerType || 'OPENAI'}
            selectedModels={tempModels}
            onChange={setTempModels}
            selectedTemplate={selectedTemplate}
          />
        );
      default:
        return null;
    }
  };

  return (
    <Modal
      title={t('addProvider')}
      open={open}
      onCancel={handleClose}
      width={640}
      footer={null}
      destroyOnClose
      styles={{ body: { padding: '16px 24px' } }}
    >
      <Steps
        current={currentStep}
        size="small"
        style={{ marginBottom: 24 }}
        items={wizardSteps}
      />

      {renderStepContent()}

      {/* 底部按钮 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginTop: 24,
          paddingTop: 16,
          borderTop: '1px solid var(--ant-color-border-secondary)',
        }}
      >
        <Button
          disabled={currentStep === 0}
          onClick={handleStepPrevious}
        >
          {t('wizard.previous', { defaultValue: '上一步' })}
        </Button>
        <Space>
          {currentStep === 2 ? (
            <Button
              type="primary"
              icon={<CheckOutlined />}
              onClick={handleCreateProvider}
              loading={saving}
            >
              {t('wizard.create', { defaultValue: '完成创建' })}
            </Button>
          ) : (
            <Button type="primary" onClick={handleStepNext}>
              {t('wizard.next', { defaultValue: '下一步' })}
            </Button>
          )}
        </Space>
      </div>
    </Modal>
  );
}

export type { ProviderCreateModalProps };