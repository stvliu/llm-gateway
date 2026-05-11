import { useState, useCallback } from 'react';
import { Drawer, Button, Space, Tabs } from 'antd';
import {
  LeftOutlined,
  RightOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { UnsavedConfirm } from './UnsavedConfirm';
import { DrawerSkeleton } from './DrawerSkeleton';

export interface DrawerTab {
  key: string;
  label: string;
  content: ReactNode;
  /** Tab 图标 */
  icon?: ReactNode;
  /** 是否禁用 */
  disabled?: boolean;
}

export interface EntityDrawerProps<T> {
  open: boolean;
  entity: T | null;
  mode: 'view' | 'edit';
  currentIndex?: number;
  totalCount?: number;
  onPrevious?: () => void;
  onNext?: () => void;
  onClose: () => void;
  onModeChange?: (mode: 'view' | 'edit') => void;
  onSave?: (entity: T) => Promise<void>;
  onDelete?: (entity: T) => Promise<void>;
  tabs?: DrawerTab[];
  defaultTab?: string;
  loading?: boolean;
  error?: Error | null;
  onRetry?: () => void;
  title?: string;
  children?: ReactNode;
  /** 是否有未保存更改 */
  hasUnsavedChanges?: boolean;
  /** 抽屉宽度 */
  width?: number;
  /** 骨架屏配置 */
  skeletonConfig?: {
    showTabs?: boolean;
    groupCount?: number;
    itemsPerGroup?: number;
  };
}

export function EntityDrawer<T>({
  open,
  entity,
  mode,
  currentIndex = 0,
  totalCount = 0,
  onPrevious,
  onNext,
  onClose,
  onModeChange,
  onSave,
  onDelete,
  tabs,
  defaultTab = 'details',
  loading = false,
  error,
  onRetry,
  title,
  children,
  hasUnsavedChanges = false,
  width = 480,
  skeletonConfig,
}: EntityDrawerProps<T>) {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState(defaultTab);
  const [showUnsavedConfirm, setShowUnsavedConfirm] = useState(false);
  const [pendingAction, setPendingAction] = useState<(() => void) | null>(null);
  const [saving, setSaving] = useState(false);

  const hasNavigation = totalCount > 1;
  const canGoPrevious = currentIndex > 0;
  const canGoNext = currentIndex < totalCount - 1;

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
    if (onPrevious) {
      checkAndExecute(onPrevious);
    }
  }, [checkAndExecute, onPrevious]);

  // 处理下一个
  const handleNext = useCallback(() => {
    if (onNext) {
      checkAndExecute(onNext);
    }
  }, [checkAndExecute, onNext]);

  // 处理保存
  const handleSave = async () => {
    if (onSave && entity) {
      setSaving(true);
      try {
        await onSave(entity);
      } finally {
        setSaving(false);
      }
    }
  };

  // 放弃更改
  const handleDiscard = () => {
    setShowUnsavedConfirm(false);
    pendingAction?.();
    setPendingAction(null);
  };

  // 抽屉头部右侧操作区
  const extra = (
    <Space>
      {mode === 'view' && onModeChange && (
        <Button icon={<EditOutlined />} onClick={() => onModeChange('edit')}>
          {t('actions.edit')}
        </Button>
      )}
      {mode === 'edit' && onSave && entity && (
        <Button type="primary" onClick={handleSave} loading={saving}>
          {t('actions.save')}
        </Button>
      )}
      {onDelete && entity && (
        <Button danger icon={<DeleteOutlined />} onClick={() => onDelete(entity)}>
          {t('actions.delete')}
        </Button>
      )}
    </Space>
  );

  // 底部导航栏
  const footer = hasNavigation ? (
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
          total: totalCount,
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
        title={title || t('drawer.title.details')}
        open={open}
        onClose={handleClose}
        width={width}
        placement="right"
        maskClosable={true}
        extra={extra}
        styles={{
          body: { padding: 16 },
          footer: { padding: 16 },
        }}
        footer={footer}
      >
        {/* Tab 切换 */}
        {tabs && tabs.length > 0 && (
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            items={tabs.map((tab) => ({
              key: tab.key,
              label: tab.label,
              icon: tab.icon,
              disabled: tab.disabled,
            }))}
            style={{ marginBottom: 16 }}
          />
        )}

        {/* 加载状态 */}
        {loading && <DrawerSkeleton showTabs={!!tabs} {...skeletonConfig} />}

        {/* 错误状态 */}
        {error && (
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              height: 200,
              gap: 16,
            }}
          >
            <span style={{ fontSize: 32 }}>⚠️</span>
            <h3 style={{ fontSize: 16, fontWeight: 500, margin: 0 }}>
              {t('drawer.error.loadFailed')}
            </h3>
            <p style={{ color: '#999', margin: 0 }}>
              {error.message}
            </p>
            {onRetry && (
              <Button type="primary" onClick={onRetry}>
                {t('drawer.error.retry')}
              </Button>
            )}
          </div>
        )}

        {/* 正常内容 */}
        {!loading && !error && entity && (
          tabs && tabs.length > 0
            ? tabs.find((tab) => tab.key === activeTab)?.content
            : children
        )}
      </Drawer>

      {/* 未保存确认对话框 */}
      <UnsavedConfirm
        open={showUnsavedConfirm}
        onContinue={() => {
          setShowUnsavedConfirm(false);
          setPendingAction(null);
        }}
        onDiscard={handleDiscard}
        onCancel={() => setShowUnsavedConfirm(false)}
      />
    </>
  );
}
