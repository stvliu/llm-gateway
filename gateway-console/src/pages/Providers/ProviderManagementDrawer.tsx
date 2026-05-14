import { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import { Drawer, Button, Space, Tabs } from 'antd';
import {
  LeftOutlined,
  RightOutlined,
  SettingOutlined,
  ApiOutlined,
  AppstoreOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckOutlined,
  CloseOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { ProviderBasicInfoTab, ProviderBasicInfoTabHandle } from './ProviderBasicInfoTab';
import { ProviderApiKeysTab } from './ProviderApiKeysTab';
import { ProviderModelsTab } from './ProviderModelsTab';
import { DrawerSkeleton } from '@/components/ui/Drawer/DrawerSkeleton';
import { useConfirm } from '@/hooks/useConfirm';
import { useProvider, useDeleteProvider } from '@/services/query';
import type { Provider } from '@/types/provider';

interface ProviderManagementDrawerProps {
  providerId: number | null;
  providers: Provider[];
  onClose: () => void;
  onProviderChange: (providerId: number) => void;
  onProviderDeleted?: () => void;
}

/**
 * 供应商详情抽屉
 * 查看模式：标签页展示（基本信息只读 + API Keys 可操作 + Models 可操作）
 * 编辑基本信息：隐藏其它标签页，只显示基本信息表单
 * 操作按钮放在标题栏右侧
 */
export function ProviderManagementDrawer({
  providerId,
  providers,
  onClose,
  onProviderChange,
  onProviderDeleted,
}: ProviderManagementDrawerProps) {
  const { t } = useTranslation('providers');
  const { confirm } = useConfirm();

  // 状态
  const [activeTab, setActiveTab] = useState('basic');
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);

  // Ref
  const basicInfoRef = useRef<ProviderBasicInfoTabHandle>(null);

  // 查询数据
  const { data: provider, isLoading } = useProvider(providerId || 0);
  const deleteMutation = useDeleteProvider();

  // 当 providerId 变化时重置状态
  useEffect(() => {
    if (providerId !== null) {
      setActiveTab('basic');
      setEditing(false);
      setDirty(false);
    }
  }, [providerId]);

  // 计算导航索引
  const currentIndex = useMemo(() => {
    if (!providerId) return -1;
    return providers.findIndex((p) => p.id === providerId);
  }, [providers, providerId]);

  const canGoPrevious = currentIndex > 0;
  const canGoNext = currentIndex < providers.length - 1 && currentIndex >= 0;

  // 进入编辑模式
  const handleEdit = useCallback(() => {
    setEditing(true);
  }, []);

  // 应用更改
  const handleApplyChanges = useCallback(async () => {
    if (!basicInfoRef.current) return;
    setSaving(true);
    const success = await basicInfoRef.current.submit();
    setSaving(false);
    if (success) {
      setEditing(false);
      setDirty(false);
    }
  }, []);

  // 撤消更改
  const handleRevertChanges = useCallback(() => {
    basicInfoRef.current?.resetFields();
    setEditing(false);
    setDirty(false);
  }, []);

  // 删除供应商
  const handleDelete = useCallback(() => {
    if (!provider) return;
    confirm({
      type: 'danger',
      entityName: provider.providerName,
      onConfirm: () => deleteMutation.mutateAsync(provider.id).then(() => {
        onProviderDeleted?.();
        onClose();
      }),
    });
  }, [confirm, provider, deleteMutation, onProviderDeleted, onClose]);

  // 处理关闭
  const handleClose = useCallback(() => {
    onClose();
  }, [onClose]);

  // 处理上一个
  const handlePrevious = useCallback(() => {
    if (canGoPrevious && providers[currentIndex - 1]) {
      onProviderChange(providers[currentIndex - 1].id);
    }
  }, [canGoPrevious, currentIndex, providers, onProviderChange]);

  // 处理下一个
  const handleNext = useCallback(() => {
    if (canGoNext && providers[currentIndex + 1]) {
      onProviderChange(providers[currentIndex + 1].id);
    }
  }, [canGoNext, currentIndex, providers, onProviderChange]);

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
  const title = provider?.providerName || t('detail.providerDetail', { defaultValue: '供应商详情' });

  // 标题栏操作按钮
  const extra = editing ? (
    <Space>
      <Button
        type="primary"
        icon={<CheckOutlined />}
        onClick={handleApplyChanges}
        loading={saving}
        disabled={!dirty}
      >
        {t('actions.applyChanges', { defaultValue: '应用更改' })}
      </Button>
      <Button
        icon={<CloseOutlined />}
        onClick={handleRevertChanges}
      >
        {t('actions.revertChanges', { defaultValue: '撤消更改' })}
      </Button>
    </Space>
  ) : (
    <Space>
      <Button icon={<EditOutlined />} onClick={handleEdit}>
        {t('actions.edit', { ns: 'common' })}
      </Button>
      <Button danger icon={<DeleteOutlined />} onClick={handleDelete}>
        {t('actions.delete', { ns: 'common' })}
      </Button>
    </Space>
  );

  // 底部导航
  const footer = providers.length > 1 ? (
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

  return (
    <Drawer
      title={title}
      extra={extra}
      open={providerId !== null}
      onClose={handleClose}
      width={560}
      placement="right"
      maskClosable
      styles={{
        body: { padding: 16 },
        footer: { padding: 16 },
      }}
      footer={footer}
    >
      {/* 标签页：编辑基本信息时隐藏 */}
      {!editing && (
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
      )}

      {/* 加载状态 */}
      {isLoading && <DrawerSkeleton showTabs={false} />}

      {/* 基本信息 */}
      {!isLoading && (editing || activeTab === 'basic') && (
        <ProviderBasicInfoTab
          ref={basicInfoRef}
          provider={provider || null}
          providers={providers}
          editing={editing}
          onDirtyChange={setDirty}
        />
      )}

      {/* API Keys：编辑基本信息时隐藏 */}
      {!isLoading && !editing && activeTab === 'apiKeys' && (
        <ProviderApiKeysTab
          provider={provider || null}
        />
      )}

      {/* Models：编辑基本信息时隐藏 */}
      {!isLoading && !editing && activeTab === 'models' && (
        <ProviderModelsTab
          provider={provider || null}
        />
      )}
    </Drawer>
  );
}

export type { ProviderManagementDrawerProps };