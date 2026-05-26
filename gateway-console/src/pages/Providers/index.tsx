import { useState, useCallback } from 'react';
import { Button, Input, Select, Segmented, Space } from 'antd';
import { PlusOutlined, AppstoreOutlined, UnorderedListOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useConfirm } from '@/hooks/useConfirm';
import { useProviders, useDeleteProvider } from '@/services/query';
import type { Provider } from '@/types/provider';
import ProviderCardView from './ProviderCardView';
import ProvidersTableView from './ProvidersTableView';
import { ProviderManagementDrawer } from './ProviderManagementDrawer';
import { ProviderCreateModal } from './ProviderCreateModal';

export default function Providers() {
  const { t } = useTranslation('providers');
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);
  const { confirm } = useConfirm();
  const deleteMutation = useDeleteProvider();

  const { data: providersData } = useProviders();
  const providers = providersData?.items ?? [];
  const [search, setSearch] = useState('');
  const [viewMode, setViewMode] = useState<'card' | 'table'>('card');
  const [stateFilter, setStateFilter] = useState<string | undefined>(undefined);
  const [selected, setSelected] = useState<Provider | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [defaultTab, setDefaultTab] = useState<'basic'>('basic');
  const [startEditing, setStartEditing] = useState(false);

  const filtered = providers.filter((p) => {
    // 关键字过滤
    if (search && !p.providerName.toLowerCase().includes(search.toLowerCase())) {
      return false;
    }
    // 状态过滤
    if (stateFilter && p.state !== stateFilter) {
      return false;
    }
    return true;
  });

  const handleCreated = useCallback(() => {
    setCreateOpen(false);
  }, []);

  const handleViewChannels = useCallback((provider: Provider) => {
    setSelected(provider);
    setDefaultTab('basic');
    setStartEditing(false);
  }, []);

  const handleSelect = useCallback((provider: Provider) => {
    setSelected(provider);
    setDefaultTab('basic');
    setStartEditing(false);
  }, []);

  const handleEdit = useCallback((provider: Provider) => {
    setSelected(provider);
    setDefaultTab('basic');
    setStartEditing(true);
  }, []);

  const handleDelete = useCallback((provider: Provider) => {
    confirm({
      type: 'danger',
      entityName: provider.providerName,
      onConfirm: () => deleteMutation.mutateAsync(provider.id),
    });
  }, [confirm, deleteMutation]);

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Space size="middle">
          <Input.Search
            placeholder={t('search', { defaultValue: '搜索供应商' })}
            style={{ width: 240 }}
            onSearch={setSearch}
            allowClear
          />
          <Select
            value={stateFilter}
            onChange={setStateFilter}
            allowClear
            placeholder={t('state', { defaultValue: '状态' })}
            style={{ width: 120 }}
            options={[
              { value: undefined, label: t('filter.all', { defaultValue: '全部' }) },
              { value: 'ACTIVE', label: t('state.active', { defaultValue: '启用' }) },
              { value: 'INACTIVE', label: t('state.inactive', { defaultValue: '停用' }) },
            ]}
          />
        </Space>
        <Space size="middle">
          <Segmented
            value={viewMode}
            onChange={(value) => setViewMode(value as 'card' | 'table')}
            options={[
              { value: 'card', icon: <AppstoreOutlined />, label: t('viewMode.card', { defaultValue: '卡片' }) },
              { value: 'table', icon: <UnorderedListOutlined />, label: t('viewMode.table', { defaultValue: '表格' }) },
            ]}
          />
          {canWrite && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
              {t('addProvider', { defaultValue: '新增供应商' })}
            </Button>
          )}
        </Space>
      </div>

      {viewMode === 'card' ? (
        <ProviderCardView
          providers={filtered}
          onSelect={handleSelect}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onViewChannels={handleViewChannels}
        />
      ) : (
        <ProvidersTableView
          providers={filtered}
          onSelect={handleSelect}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onViewChannels={handleViewChannels}
        />
      )}

      <ProviderManagementDrawer
        providerId={selected?.id ?? null}
        providers={providers}
        onClose={() => setSelected(null)}
        onProviderChange={(id) => {
          const p = providers.find((pr) => pr.id === id);
          if (p) setSelected(p);
        }}
        defaultTab={defaultTab}
        startEditing={startEditing}
      />

      <ProviderCreateModal
        open={createOpen}
        providers={providers}
        onClose={() => setCreateOpen(false)}
        onCreated={handleCreated}
      />
    </>
  );
}