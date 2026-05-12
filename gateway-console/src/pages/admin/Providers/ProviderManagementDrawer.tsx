import { useState, useCallback, useMemo, useEffect } from 'react';
import { Drawer, Button, Space, Tabs, message, Popconfirm } from 'antd';
import {
  LeftOutlined,
  RightOutlined,
  EditOutlined,
  DeleteOutlined,
  SettingOutlined,
  ApiOutlined,
  AppstoreOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { ProviderBasicInfoTab } from './ProviderBasicInfoTab';
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
import type { Provider, CreateProviderRequest } from '@/types/provider';

interface ProviderManagementDrawerProps {
  providerId: number | null; // null 表示新增模式
  providers: Provider[];
  mode?: 'view' | 'edit' | 'create';  // 初始模式
  onClose: () => void;
  onProviderChange: (providerId: number) => void;
  onProviderCreated?: (provider: Provider) => void;
  onProviderDeleted?: () => void;
}

type DrawerMode = 'view' | 'edit' | 'create';

/**
 * 供应商一站式管理抽屉
 * 支持查看、编辑、新增三种模式
 * 包含三个标签页：基本信息、API Keys、模型
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

  // 状态
  const [mode, setMode] = useState<DrawerMode>(
    providerId ? initialMode : 'create'
  );
  const [activeTab, setActiveTab] = useState('basic');
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [showUnsavedConfirm, setShowUnsavedConfirm] = useState(false);
  const [pendingAction, setPendingAction] = useState<(() => void) | null>(null);
  const [saving, setSaving] = useState(false);

  // 查询数据
  const { data: provider, isLoading } = useProvider(providerId || 0);

  // Mutations
  const createMutation = useCreateProvider();
  const updateMutation = useUpdateProvider();
  const deleteMutation = useDeleteProvider();

  // 当 providerId 变化时重置状态
  useEffect(() => {
    if (providerId) {
      setMode('view');
      setActiveTab('basic');
      setHasUnsavedChanges(false);
    } else {
      setMode('create');
      setActiveTab('basic');
      setHasUnsavedChanges(false);
    }
  }, [providerId]);

  // 计算导航索引
  const currentIndex = useMemo(() => {
    if (!providerId) return -1;
    return providers.findIndex((p) => p.id === providerId);
  }, [providers, providerId]);

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

  // 保存
  const handleSave = useCallback(async (values: CreateProviderRequest) => {
    setSaving(true);
    try {
      if (mode === 'create') {
        const newProvider = await createMutation.mutateAsync(values);
        message.success(t('message.success', { ns: 'common' }));
        onProviderCreated?.(newProvider);
        onClose();
      } else if (providerId) {
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
  }, [mode, providerId, createMutation, updateMutation, t, onProviderCreated, onClose]);

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
    : provider?.providerName || t('detail.providerDetail');

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
            onClick={() => {
              // 触发表单提交（通过 ref 或其他方式）
              // 这里简化处理，实际需要与 BasicInfoTab 集成
            }}
            loading={saving}
          >
            {t('actions.save', { ns: 'common' })}
          </Button>
        </>
      )}
      {mode === 'create' && (
        <>
          <Button onClick={handleClose}>
            {t('actions.cancel', { ns: 'common' })}
          </Button>
          <Button
            type="primary"
            onClick={() => {
              // 触发表单提交
            }}
            loading={saving}
          >
            {t('actions.save', { ns: 'common' })}
          </Button>
        </>
      )}
    </Space>
  );

  // 底部导航（仅查看模式和编辑模式显示）
  const showNavigation = mode !== 'create' && providers.length > 1;
  const footer = showNavigation ? (
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
      <span>
        {t('drawer.navigation.position', {
          current: currentIndex + 1,
          total: providers.length,
        })}
      </span>
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

  return (
    <>
      <Drawer
        title={title}
        open={providerId !== null || mode === 'create'}
        onClose={handleClose}
        width={560}
        placement="right"
        maskClosable={mode === 'view'}
        extra={extra}
        styles={{
          body: { padding: 16 },
          footer: { padding: 16 },
        }}
        footer={footer}
      >
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
        {isLoading && mode !== 'create' && <DrawerSkeleton showTabs={false} />}

        {/* 内容 */}
        {!isLoading && activeTab === 'basic' && (
          <ProviderBasicInfoTab
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
