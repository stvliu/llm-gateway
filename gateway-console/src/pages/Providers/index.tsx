import { useState, useCallback } from 'react';
import { ProviderCardView } from './ProviderCardView';
import { ProvidersTableView } from './ProvidersTableView';
import { ProviderManagementDrawer } from './ProviderManagementDrawer';
import { ProviderCreateModal } from './ProviderCreateModal';
import { ProviderExperienceModal } from './ProviderExperienceModal';
import { useProviders } from '@/services/query';
import type { Provider } from '@/types/provider';

type ViewMode = 'card' | 'table';

/**
 * 一站式供应商管理页面
 * 卡片视图：网格展示供应商卡片，抽屉管理详情
 * 表格视图：传统表格展示
 * 创建供应商：独立 Modal 向导
 */
export default function AdminProviders() {
  // 视图模式
  const [viewMode, setViewMode] = useState<ViewMode>('card');

  // 抽屉状态：null 表示关闭，其他表示查看详情
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);

  // 创建 Modal 状态
  const [createModalOpen, setCreateModalOpen] = useState(false);

  // 体验弹窗状态
  const [experienceModalOpen, setExperienceModalOpen] = useState(false);
  const [experienceProvider, setExperienceProvider] = useState<Provider | null>(null);

  // Queries
  const { data: providersData } = useProviders({ size: 100 });
  const providers = providersData?.items || [];

  // 添加供应商：打开创建 Modal
  const handleAddProvider = useCallback(() => {
    setCreateModalOpen(true);
  }, []);

  // 查看供应商详情
  const handleViewProvider = useCallback((provider: Provider) => {
    setSelectedProviderId(provider.id);
  }, []);

  // 体验供应商模型
  const handleExperienceProvider = useCallback((provider: Provider) => {
    setExperienceProvider(provider);
    setExperienceModalOpen(true);
  }, []);

  // 关闭抽屉
  const handleCloseDrawer = useCallback(() => {
    setSelectedProviderId(null);
  }, []);

  // 关闭创建 Modal
  const handleCloseCreateModal = useCallback(() => {
    setCreateModalOpen(false);
  }, []);

  // 供应商创建成功：关闭 Modal，刷新列表
  const handleProviderCreated = useCallback(() => {
    setCreateModalOpen(false);
  }, []);

  // 关闭体验弹窗
  const handleCloseExperienceModal = useCallback(() => {
    setExperienceModalOpen(false);
    setExperienceProvider(null);
  }, []);

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 内容区域 */}
      {viewMode === 'card' ? (
        <ProviderCardView
          onProviderSelect={handleViewProvider}
          onProviderExperience={handleExperienceProvider}
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

      {/* 供应商详情抽屉 */}
      <ProviderManagementDrawer
        providerId={selectedProviderId}
        providers={providers}
        onClose={handleCloseDrawer}
        onProviderChange={setSelectedProviderId}
      />

      {/* 供应商创建 Modal */}
      <ProviderCreateModal
        open={createModalOpen}
        providers={providers}
        onClose={handleCloseCreateModal}
        onCreated={handleProviderCreated}
      />

      {/* 供应商模型体验弹窗 */}
      <ProviderExperienceModal
        open={experienceModalOpen}
        provider={experienceProvider}
        onClose={handleCloseExperienceModal}
      />
    </div>
  );
}