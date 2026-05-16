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
import { useModelMetadataByProvider } from '@/services/query/useMetadata';
import type { Provider, CreateProviderRequest, NestedApiKeyRequest, NestedModelRequest } from '@/types/provider';
import type { ProviderMetadata, ModelMetadata } from '@/types/metadata';

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

/**
 * 将 ModelMetadata 转换为 NestedModelRequest
 */
function toNestedModelRequest(m: ModelMetadata): NestedModelRequest {
  return {
    providerModelId: m.providerModelId,
    displayName: m.displayName,
    contextWindow: m.contextWindow,
    inputPrice: m.inputPrice,
    outputPrice: m.outputPrice,
    capabilities: m.capabilities,
  };
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
  const [selectedMetadata, setSelectedMetadata] = useState<ProviderMetadata | null>(null);
  const [basicInfo, setBasicInfo] = useState<CreateProviderRequest | null>(null);
  const [tempApiKeys, setTempApiKeys] = useState<NestedApiKeyRequest[]>([]);
  const [selectedModelIds, setSelectedModelIds] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  // 获取当前供应商的模型元数据
  const providerId = selectedMetadata?.providerId || basicInfo?.providerType?.toLowerCase() || null;
  const { data: modelMetadataList } = useModelMetadataByProvider(providerId);

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
    setSelectedMetadata(null);
    setBasicInfo(null);
    setTempApiKeys([]);
    setSelectedModelIds([]);
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

    // 从 ModelMetadata 映射为 NestedModelRequest
    const models: NestedModelRequest[] = (modelMetadataList ?? [])
      .filter(m => selectedModelIds.includes(m.providerModelId))
      .map(toNestedModelRequest);

    try {
      setSaving(true);
      const request: CreateProviderRequest = {
        ...basicInfo,
        apiKeys: validKeys,
        models,
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
  }, [basicInfo, tempApiKeys, selectedModelIds, modelMetadataList, providers, createMutation, t, onCreated, handleClose]);

  // 渲染向导步骤内容
  const renderStepContent = () => {
    switch (currentStep) {
      case 0:
        return (
          <BasicInfoStep
            basicInfo={basicInfo}
            onChange={setBasicInfo}
            onMetadataLoad={setSelectedMetadata}
            onSelectedModelIdsChange={setSelectedModelIds}
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
            providerId={providerId || ''}
            selectedModels={selectedModelIds}
            onSelectedModelsChange={setSelectedModelIds}
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