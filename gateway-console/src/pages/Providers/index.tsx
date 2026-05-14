import { useState, useCallback } from 'react';
import { ProviderCardView } from './ProviderCardView';
import { ProvidersTableView } from './ProvidersTableView';
import { ProviderManagementDrawer } from './ProviderManagementDrawer';
import { useProviders } from '@/services/query';
import type { Provider } from '@/types/provider';

type ViewMode = 'card' | 'table';

// 特殊值：表示新增模式
const NEW_PROVIDER_ID = -1;

/**
 * 一站式供应商管理页面
 * 卡片视图：网格展示供应商卡片，抽屉管理详情
 * 表格视图：传统表格展示
 */
export default function AdminProviders() {
  // 视图模式
  const [viewMode, setViewMode] = useState<ViewMode>('card');

  // 抽屉状态：null 表示关闭，-1 表示新增，其他表示查看/编辑
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);

  // Queries
  const { data: providersData } = useProviders({ size: 100 });
  const providers = providersData?.items || [];

  // 添加供应商
  const handleAddProvider = useCallback(() => {
    setSelectedProviderId(NEW_PROVIDER_ID);
  }, []);

  // 查看供应商详情
  const handleViewProvider = useCallback((provider: Provider) => {
    setSelectedProviderId(provider.id);
  }, []);

  // 关闭抽屉
  const handleCloseDrawer = useCallback(() => {
    setSelectedProviderId(null);
  }, []);

  // 供应商创建成功
  const handleProviderCreated = useCallback((provider: Provider) => {
    setSelectedProviderId(provider.id);
  }, []);

  // 计算抽屉模式
  const drawerMode = selectedProviderId === NEW_PROVIDER_ID ? 'create' : 'view';

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 内容区域 */}
      {viewMode === 'card' ? (
        <ProviderCardView
          onProviderSelect={handleViewProvider}
          viewMode={viewMode}
          onViewModeChange={setViewMode}
          onAddProvider={handleAddProvider}
        />
      ) : (
        <ProvidersTableView
          viewMode={viewMode}
          onViewModeChange={setViewMode}
          onAddProvider={handleAddProvider}
          onProviderSelect={handleViewProvider}
        />
      )}

      {/* 供应商管理抽屉 */}
      <ProviderManagementDrawer
        providerId={selectedProviderId}
        providers={providers}
        mode={drawerMode}
        onClose={handleCloseDrawer}
        onProviderChange={setSelectedProviderId}
        onProviderCreated={handleProviderCreated}
      />
    </div>
  );
}
