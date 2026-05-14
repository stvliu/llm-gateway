import { useState, useCallback } from 'react';
import { Segmented, theme } from 'antd';
import { AppstoreOutlined, UnorderedListOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { ProviderSidebar } from './ProviderSidebar';
import { ProviderManagementDrawer } from './ProviderManagementDrawer';
import { ModelsTableView } from './ModelsTableView';
import { useProviders } from '@/services/query';
import type { Provider } from '@/types/provider';

type ViewMode = 'card' | 'table';

/**
 * 模型管理页面
 * 卡片视图：一站式管理，左侧供应商列表，右侧抽屉管理详情
 * 表格视图：用户视角，使用 EntityTable + EntityDrawer 组件
 */
export default function AdminModels() {
  const { t } = useTranslation('models');
  const { token } = theme.useToken();

  // 视图模式
  const [viewMode, setViewMode] = useState<ViewMode>('table');

  // 卡片视图：选中的供应商
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);

  // 统一数据源
  const { data: providersData, isLoading: providersLoading } = useProviders({ size: 100 });
  const providers = providersData?.items || [];

  // 选择供应商
  const handleSelectProvider = useCallback((provider: Provider) => {
    setSelectedProviderId(provider.id);
  }, []);

  // 添加供应商
  const handleAddProvider = useCallback(() => {
    setSelectedProviderId(null);
  }, []);

  // 供应商创建成功
  const handleProviderCreated = useCallback((provider: Provider) => {
    setSelectedProviderId(provider.id);
  }, []);

  // 供应商删除成功
  const handleProviderDeleted = useCallback(() => {
    setSelectedProviderId(null);
  }, []);

  // 关闭抽屉
  const handleCloseDrawer = useCallback(() => {
    setSelectedProviderId(null);
  }, []);

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 视图切换 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          padding: 16,
          background: token.colorBgContainer,
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <Segmented
          value={viewMode}
          onChange={(value) => {
            setViewMode(value as ViewMode);
            if (value === 'table') {
              setSelectedProviderId(null);
            }
          }}
          options={[
            {
              value: 'table',
              icon: <UnorderedListOutlined />,
              label: t('viewMode.table', { defaultValue: '表格' }),
            },
            {
              value: 'card',
              icon: <AppstoreOutlined />,
              label: t('viewMode.card', { defaultValue: '卡片' }),
            },
          ]}
        />
      </div>

      {/* 内容区域 */}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {viewMode === 'table' ? (
          /* 表格视图：使用 EntityTable + EntityDrawer 组件 */
          <ModelsTableView providers={providers} providersLoading={providersLoading} />
        ) : (
          /* 卡片视图：左侧供应商列表 + 右侧抽屉管理 */
          <div style={{ display: 'flex', gap: 16, height: '100%', padding: 16 }}>
            <div style={{ width: 280, height: '100%' }}>
              <ProviderSidebar
                providers={providers}
                isLoading={providersLoading}
                selectedProviderId={selectedProviderId}
                onSelect={handleSelectProvider}
                onAdd={handleAddProvider}
              />
            </div>
          </div>
        )}
      </div>

      {/* 供应商管理抽屉（卡片视图） */}
      {viewMode === 'card' && (
        <ProviderManagementDrawer
          providerId={selectedProviderId}
          providers={providers}
          onClose={handleCloseDrawer}
          onProviderChange={setSelectedProviderId}
          onProviderCreated={handleProviderCreated}
          onProviderDeleted={handleProviderDeleted}
        />
      )}
    </div>
  );
}