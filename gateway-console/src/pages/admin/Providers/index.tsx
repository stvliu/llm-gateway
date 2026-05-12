import { useState, useCallback } from 'react';
import { Segmented, Button, theme } from 'antd';
import { AppstoreOutlined, UnorderedListOutlined, PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { ProviderCardView } from './ProviderCardView';
import { ProviderManagementDrawer } from './ProviderManagementDrawer';
import { useProviders } from '@/services/query';
import type { Provider } from '@/types/provider';

type ViewMode = 'card' | 'table';

// 特殊值：表示新增模式
const NEW_PROVIDER_ID = -1;

/**
 * 一站式供应商管理页面
 * 卡片视图：网格展示供应商卡片，抽屉管理详情
 * 表格视图：传统表格展示（保留原有功能）
 */
export default function AdminProviders() {
  const { t } = useTranslation('providers');
  const { token } = theme.useToken();

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
      {/* 头部工具栏 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: 16,
          background: token.colorBgContainer,
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddProvider}>
          {t('add', { defaultValue: '添加供应商' })}
        </Button>

        <Segmented
          value={viewMode}
          onChange={(value) => setViewMode(value as ViewMode)}
          options={[
            {
              value: 'card',
              icon: <AppstoreOutlined />,
              label: t('viewMode.card', { defaultValue: '卡片' }),
            },
            {
              value: 'table',
              icon: <UnorderedListOutlined />,
              label: t('viewMode.table', { defaultValue: '表格' }),
            },
          ]}
        />
      </div>

      {/* 内容区域 */}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {viewMode === 'card' ? (
          <ProviderCardView onProviderSelect={handleViewProvider} />
        ) : (
          // 表格视图：保留原有功能，后续可扩展
          <div style={{ padding: 16, textAlign: 'center', color: token.colorTextSecondary }}>
            {t('comingSoon', { defaultValue: '表格视图开发中...' })}
          </div>
        )}
      </div>

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
