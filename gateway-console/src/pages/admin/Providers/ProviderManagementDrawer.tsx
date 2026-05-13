import { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import { Drawer, Button, Space, message, Popconfirm, Tabs, Steps } from 'antd';
import {
  LeftOutlined,
  RightOutlined,
  EditOutlined,
  DeleteOutlined,
  SettingOutlined,
  ApiOutlined,
  AppstoreOutlined,
  CheckOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { BasicInfoStep } from './BasicInfoStep';
import { ApiKeySetupStep } from './ApiKeySetupStep';
import { ModelSetupStep } from './ModelSetupStep';
import { ProviderBasicInfoTab, type ProviderBasicInfoTabRef } from './ProviderBasicInfoTab';
import { ProviderApiKeysTab } from './ProviderApiKeysTab';
import { ProviderModelsTab } from './ProviderModelsTab';
import { UnsavedConfirm } from '@/components/ui/Drawer/UnsavedConfirm';
import { DrawerSkeleton } from '@/components/ui/Drawer/DrawerSkeleton';
import {
  useProvider,
  useCreateProvider,
  useUpdateProvider,
  useDeleteProvider,
} from '@/services/query';
import type { Provider, CreateProviderRequest, NestedApiKeyRequest, NestedModelRequest } from '@/types/provider';
import type { ProviderTemplate } from '@/types/template';

interface ProviderManagementDrawerProps {
  providerId: number | null;
  providers: Provider[];
  mode?: 'view' | 'edit' | 'create';
  onClose: () => void;
  onProviderChange: (providerId: number) => void;
  onProviderCreated?: (provider: Provider) => void;
  onProviderDeleted?: () => void;
}

type DrawerMode = 'view' | 'edit' | 'create';

/**
 * 检查供应商名称是否重复
 * @param name 供应商名称
 * @param providers 现有供应商列表
 * @param excludeId 排除的供应商 ID（编辑时排除自身）
 */
function isNameDuplicate(name: string, providers: Provider[], excludeId?: number): boolean {
  return providers.some(p =>
    p.id !== excludeId && p.providerName.toLowerCase() === name.toLowerCase()
  );
}

/**
 * 检查 API URL 是否重复
 * @param baseUrl API URL
 * @param providers 现有供应商列表
 * @param excludeId 排除的供应商 ID（编辑时排除自身）
 */
function isBaseUrlDuplicate(baseUrl: string, providers: Provider[], excludeId?: number): boolean {
  const normalizedUrl = baseUrl.toLowerCase().replace(/\/+$/, ''); // 移除末尾斜杠
  return providers.some(p =>
    p.id !== excludeId && p.baseUrl?.toLowerCase().replace(/\/+$/, '') === normalizedUrl
  );
}

/**
 * 供应商一站式管理抽屉
 * 支持查看、编辑、新增三种模式
 * 新增模式使用 4 步向导：模板选择 → 基本信息 → API Key → 模型配置
 */
export function ProviderManagementDrawer({
  providerId,
  providers,
  mode: initialMode = 'view',
  onClose,
  onProviderChange,
  onProviderCreated,
  onProviderDeleted,
}: ProviderManagementDrawerProps) {
  const { t } = useTranslation('providers');

  // 表单 ref
  const viewFormRef = useRef<ProviderBasicInfoTabRef>(null);

  // 状态
  const [mode, setMode] = useState<DrawerMode>('view');
  const [activeTab, setActiveTab] = useState('basic');
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [showUnsavedConfirm, setShowUnsavedConfirm] = useState(false);
  const [pendingAction, setPendingAction] = useState<(() => void) | null>(null);
  const [saving, setSaving] = useState(false);

  // 向导状态
  const [currentStep, setCurrentStep] = useState(0);
  const [selectedTemplate, setSelectedTemplate] = useState<ProviderTemplate | null>(null);
  const [basicInfo, setBasicInfo] = useState<CreateProviderRequest | null>(null);
  const [tempApiKeys, setTempApiKeys] = useState<NestedApiKeyRequest[]>([]);
  const [tempModels, setTempModels] = useState<NestedModelRequest[]>([]);

  // 判断是否是新增模式
  const isNewProvider = providerId === -1;

  // 查询数据
  const { data: provider, isLoading } = useProvider(providerId && providerId > 0 ? providerId : 0);

  // Mutations
  const createMutation = useCreateProvider();
  const updateMutation = useUpdateProvider();
  const deleteMutation = useDeleteProvider();

  // 向导步骤配置（3步：基本信息 → API Key → 模型配置）
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

  // 当 providerId 变化时重置状态
  useEffect(() => {
    if (providerId !== null) {
      if (isNewProvider) {
        setMode('create');
        setCurrentStep(0);
        setSelectedTemplate(null);
        setBasicInfo(null);
        setTempApiKeys([]);
        setTempModels([]);
      } else {
        setMode(initialMode);
        setActiveTab('basic');
      }
      setHasUnsavedChanges(false);
    }
  }, [providerId, initialMode, isNewProvider]);

  // 计算导航索引
  const currentIndex = useMemo(() => {
    if (!providerId || isNewProvider) return -1;
    return providers.findIndex((p) => p.id === providerId);
  }, [providers, providerId, isNewProvider]);

  const canGoPrevious = currentIndex > 0;
  const canGoNext = currentIndex < providers.length - 1 && currentIndex >= 0;

  // 检查未保存更改并执行操作
  const checkAndExecute = useCallback(
    (action: () => void) => {
      if (hasUnsavedChanges) {
        setPendingAction(() => action);
        setShowUnsavedConfirm(true);
      } else {
        action();
      }
    },
    [hasUnsavedChanges]
  );

  // 处理关闭
  const handleClose = useCallback(() => {
    checkAndExecute(onClose);
  }, [checkAndExecute, onClose]);

  // 处理上一个
  const handlePrevious = useCallback(() => {
    if (canGoPrevious && providers[currentIndex - 1]) {
      checkAndExecute(() => onProviderChange(providers[currentIndex - 1].id));
    }
  }, [canGoPrevious, currentIndex, providers, checkAndExecute, onProviderChange]);

  // 处理下一个
  const handleNext = useCallback(() => {
    if (canGoNext && providers[currentIndex + 1]) {
      checkAndExecute(() => onProviderChange(providers[currentIndex + 1].id));
    }
  }, [canGoNext, currentIndex, providers, checkAndExecute, onProviderChange]);

  // 切换到编辑模式
  const handleEdit = useCallback(() => {
    setMode('edit');
  }, []);

  // 切换到查看模式
  const handleCancelEdit = useCallback(() => {
    if (hasUnsavedChanges) {
      setPendingAction(() => setMode('view'));
      setShowUnsavedConfirm(true);
    } else {
      setMode('view');
    }
  }, [hasUnsavedChanges]);

  // 向导：上一步
  const handleStepPrevious = useCallback(() => {
    setCurrentStep((prev) => Math.max(0, prev - 1));
  }, []);

  // 向导：下一步
  const handleStepNext = useCallback(() => {
    // 第一步到第二步：验证基本信息
    if (currentStep === 0) {
      if (!basicInfo?.providerName || !basicInfo?.providerType || !basicInfo?.baseUrl) {
        message.warning(t('validation.basicInfoRequired', { defaultValue: '请填写必填信息' }));
        return;
      }
      // 检查名称唯一性
      if (isNameDuplicate(basicInfo.providerName, providers)) {
        message.warning(t('validation.nameDuplicate', { defaultValue: '供应商名称已存在，请使用其他名称' }));
        return;
      }
      // 检查 URL 唯一性
      if (isBaseUrlDuplicate(basicInfo.baseUrl, providers)) {
        message.warning(t('validation.baseUrlDuplicate', { defaultValue: 'API 地址已存在，请使用其他地址' }));
        return;
      }
    }
    // 第二步到第三步：检查是否有有效的 API Key
    if (currentStep === 1) {
      const validKeys = tempApiKeys.filter(k => k.apiKey);
      if (validKeys.length === 0) {
        message.warning(t('validation.apiKeyRequired', { defaultValue: '请至少配置一个有效的 API Key' }));
        return;
      }
    }
    setCurrentStep((prev) => Math.min(2, prev + 1));
  }, [currentStep, basicInfo, tempApiKeys, providers, t]);

  // 向导：完成创建
  const handleCreateProvider = useCallback(async () => {
    if (!basicInfo) {
      message.warning(t('validation.basicInfoRequired', { defaultValue: '请填写基本信息' }));
      return;
    }

    if (!basicInfo.providerName || !basicInfo.providerType || !basicInfo.baseUrl) {
      message.warning(t('validation.basicInfoRequired', { defaultValue: '请填写必填信息' }));
      return;
    }

    // 检查名称唯一性
    if (isNameDuplicate(basicInfo.providerName, providers)) {
      message.warning(t('validation.nameDuplicate', { defaultValue: '供应商名称已存在，请使用其他名称' }));
      return;
    }

    // 检查 URL 唯一性
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
      onProviderCreated?.(newProvider);
      onClose();
    } catch {
      // 错误已在 mutation 中处理
    } finally {
      setSaving(false);
    }
  }, [basicInfo, tempApiKeys, tempModels, providers, createMutation, t, onProviderCreated, onClose]);

  // 保存（编辑模式）
  const handleSave = useCallback(async (values: CreateProviderRequest) => {
    // 检查名称唯一性（排除当前供应商）
    if (isNameDuplicate(values.providerName, providers, providerId || undefined)) {
      message.warning(t('validation.nameDuplicate', { defaultValue: '供应商名称已存在，请使用其他名称' }));
      return;
    }

    // 检查 URL 唯一性（排除当前供应商）
    if (values.baseUrl && isBaseUrlDuplicate(values.baseUrl, providers, providerId || undefined)) {
      message.warning(t('validation.baseUrlDuplicate', { defaultValue: 'API 地址已存在，请使用其他地址' }));
      return;
    }

    setSaving(true);
    try {
      if (providerId) {
        await updateMutation.mutateAsync({
          id: providerId,
          data: values,
        });
        message.success(t('message.success', { ns: 'common' }));
        setMode('view');
        setHasUnsavedChanges(false);
      }
    } finally {
      setSaving(false);
    }
  }, [providerId, providers, updateMutation, t]);

  // 删除
  const handleDelete = useCallback(async () => {
    if (!providerId) return;
    try {
      await deleteMutation.mutateAsync(providerId);
      message.success(t('message.success', { ns: 'common' }));
      onProviderDeleted?.();
      onClose();
    } finally {
      // 关闭确认框由 Popconfirm 处理
    }
  }, [providerId, deleteMutation, t, onProviderDeleted, onClose]);

  // 放弃更改
  const handleDiscard = useCallback(() => {
    setShowUnsavedConfirm(false);
    pendingAction?.();
    setPendingAction(null);
    setHasUnsavedChanges(false);
  }, [pendingAction]);

  // 继续编辑
  const handleContinue = useCallback(() => {
    setShowUnsavedConfirm(false);
    setPendingAction(null);
  }, []);

  // 标签页配置
  const tabs = [
    {
      key: 'basic',
      label: t('detail.basicInfo', { defaultValue: '基本信息' }),
      icon: <SettingOutlined />,
    },
    {
      key: 'apiKeys',
      label: t('provider.apiKeys', { defaultValue: 'API Keys' }),
      icon: <ApiOutlined />,
    },
    {
      key: 'models',
      label: t('provider.models', { defaultValue: '模型' }),
      icon: <AppstoreOutlined />,
    },
  ];

  // 标题
  const title = mode === 'create'
    ? t('addProvider')
    : provider?.providerName || t('detail.providerDetail', { defaultValue: '供应商详情' });

  // 头部操作区
  const extra = (
    <Space>
      {mode === 'view' && providerId && (
        <>
          <Button icon={<EditOutlined />} onClick={handleEdit}>
            {t('actions.edit', { ns: 'common' })}
          </Button>
          <Popconfirm
            title={t('confirm.delete', { ns: 'common' })}
            description={t('confirm.deleteProviderDesc', { name: provider?.providerName })}
            onConfirm={handleDelete}
          >
            <Button danger icon={<DeleteOutlined />}>
              {t('actions.delete', { ns: 'common' })}
            </Button>
          </Popconfirm>
        </>
      )}
      {mode === 'edit' && (
        <>
          <Button onClick={handleCancelEdit}>
            {t('actions.cancel', { ns: 'common' })}
          </Button>
          <Button
            type="primary"
            onClick={() => viewFormRef.current?.submit()}
            loading={saving}
          >
            {t('actions.save', { ns: 'common' })}
          </Button>
        </>
      )}
    </Space>
  );

  // 底部导航
  const footer = mode !== 'create' && !isNewProvider && providers.length > 1 ? (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}
    >
      <Button
        type="text"
        icon={<LeftOutlined />}
        disabled={!canGoPrevious}
        onClick={handlePrevious}
      >
        {t('drawer.navigation.previous')}
      </Button>
      <Button
        type="text"
        icon={<RightOutlined />}
        disabled={!canGoNext}
        onClick={handleNext}
      >
        {t('drawer.navigation.next')}
      </Button>
    </div>
  ) : undefined;

    // 向导底部
  const wizardFooter = mode === 'create' ? (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
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
  ) : undefined;

  // 渲染向导步骤内容
  const renderWizardContent = () => {
    switch (currentStep) {
      case 0:
        // 步骤 1：基本信息（供应商类型在第一行，选择后自动填充）
        return (
          <BasicInfoStep
            basicInfo={basicInfo}
            onChange={setBasicInfo}
            onTemplateLoad={setSelectedTemplate}
            onModelsChange={setTempModels}
          />
        );

      case 1:
        // 步骤 2：API Key 配置
        return (
          <ApiKeySetupStep
            apiKeys={tempApiKeys}
            onChange={setTempApiKeys}
            providerType={basicInfo?.providerType}
            baseUrl={basicInfo?.baseUrl}
          />
        );

      case 2:
        // 步骤 3：模型配置
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
    <>
      <Drawer
        title={title}
        open={providerId !== null}
        onClose={handleClose}
        width={560}
        placement="right"
        maskClosable={mode === 'view'}
        extra={mode !== 'create' ? extra : undefined}
        styles={{
          body: { padding: 16 },
          footer: { padding: 16 },
        }}
        footer={mode === 'create' ? wizardFooter : footer}
      >
        {/* 创建向导模式 */}
        {mode === 'create' && (
          <>
            {/* 步骤指示器 */}
            <Steps
              current={currentStep}
              size="small"
              style={{ marginBottom: 24 }}
              items={wizardSteps}
            />

            {/* 步骤内容 */}
            {renderWizardContent()}
          </>
        )}

        {/* 查看/编辑模式 */}
        {mode !== 'create' && (
          <>
            {/* 标签页 */}
            <Tabs
              activeKey={activeTab}
              onChange={setActiveTab}
              items={tabs.map((tab) => ({
                key: tab.key,
                label: (
                  <Space size={4}>
                    {tab.icon}
                    {tab.label}
                  </Space>
                ),
              }))}
              style={{ marginBottom: 16 }}
            />

            {/* 加载状态 */}
            {isLoading && <DrawerSkeleton showTabs={false} />}

            {/* 内容 */}
            {!isLoading && activeTab === 'basic' && (
              <ProviderBasicInfoTab
                ref={viewFormRef}
                provider={provider || null}
                mode={mode}
                onValuesChange={setHasUnsavedChanges}
                onSubmit={handleSave}
              />
            )}
            {!isLoading && activeTab === 'apiKeys' && (
              <ProviderApiKeysTab
                provider={provider || null}
                mode={mode}
              />
            )}
            {!isLoading && activeTab === 'models' && (
              <ProviderModelsTab
                provider={provider || null}
                mode={mode}
              />
            )}
          </>
        )}
      </Drawer>

      {/* 未保存确认对话框 */}
      <UnsavedConfirm
        open={showUnsavedConfirm}
        onContinue={handleContinue}
        onDiscard={handleDiscard}
        onCancel={() => setShowUnsavedConfirm(false)}
      />
    </>
  );
}

export type { ProviderManagementDrawerProps };