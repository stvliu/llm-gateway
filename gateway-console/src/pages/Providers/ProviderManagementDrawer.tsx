import { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import { Drawer, Button, Space, Tabs } from 'antd';
import {
  LeftOutlined,
  RightOutlined,
  SettingOutlined,
  ApiOutlined,
  RobotOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckOutlined,
  CloseOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { ProviderBasicInfoTab, ProviderBasicInfoTabHandle } from './ProviderBasicInfoTab';
import ProviderChannelTab from './ProviderChannelTab';
import ProviderModelSpecTab from './ProviderModelSpecTab';
import { DrawerSkeleton } from '@/components/ui/Drawer/DrawerSkeleton';
import { useConfirm } from '@/hooks/useConfirm';
import { useProvider, useDeleteProvider } from '@/services/query';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import type { Provider } from '@/types/provider';

interface ProviderManagementDrawerProps {
  providerId: number | null;
  providers: Provider[];
  onClose: () => void;
  onProviderChange: (providerId: number) => void;
  onProviderDeleted?: () => void;
  defaultTab?: 'basic' | 'channels' | 'modelSpecs';
  startEditing?: boolean;
}

/**
 * 供应商详情抽屉
 * 查看模式：标签页展示（基本信息 + 渠道 + 模型规格）
 * 编辑基本信息：隐藏其它标签页，只显示基本信息表单
 * 无 PROVIDER_WRITE 权限的用户隐藏编辑/删除按钮
 * 操作按钮放在标题栏右侧
 */
export function ProviderManagementDrawer({
  providerId,
  providers,
  onClose,
  onProviderChange,
  onProviderDeleted,
  defaultTab = 'basic',
  startEditing = false,
}: ProviderManagementDrawerProps) {
  const { t } = useTranslation('providers');
  const { confirm } = useConfirm();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);

  // 状态
  const [activeTab, setActiveTab] = useState(defaultTab);
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
      setActiveTab(defaultTab);
      setEditing(startEditing);
      setDirty(false);
    }
  }, [providerId, defaultTab, startEditing]);

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
      key: 'channels',
      label: t('detail.channels', { defaultValue: '渠道' }),
      icon: <ApiOutlined />,
    },
    {
      key: 'modelSpecs',
      label: t('detail.modelSpecs', { defaultValue: '模型规格' }),
      icon: <RobotOutlined />,
    },
  ];

  // 标题
  const title = provider?.providerName || t('detail.providerDetail', { defaultValue: '供应商详情' });

  // 标题栏操作按钮（无写入权限时隐藏编辑/删除）
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
  ) : canWrite ? (
    <Space>
      <Button icon={<EditOutlined />} onClick={handleEdit}>
        {t('actions.edit', { ns: 'common' })}
      </Button>
      <Button danger icon={<DeleteOutlined />} onClick={handleDelete}>
        {t('actions.delete', { ns: 'common' })}
      </Button>
    </Space>
  ) : null;

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
      width={720}
      placement="right"
      mask={{ closable: true }}
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
          onChange={(key) => setActiveTab(key as typeof activeTab)}
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

      {/* 渠道（编辑模式下隐藏） */}
      {!isLoading && !editing && activeTab === 'channels' && providerId && (
        <ProviderChannelTab providerId={providerId} editing={false} />
      )}

      {/* 模型规格（编辑模式下隐藏） */}
      {!isLoading && !editing && activeTab === 'modelSpecs' && providerId && (
        <ProviderModelSpecTab providerId={providerId} editing={false} />
      )}
    </Drawer>
  );
}

export type { ProviderManagementDrawerProps };