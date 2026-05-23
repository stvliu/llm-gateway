import { useState, useCallback } from 'react';
import { Button, Input } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useConfirm } from '@/hooks/useConfirm';
import { useProviders, useDeleteProvider } from '@/services/query';
import type { Provider } from '@/types/provider';
import ProviderCardView from './ProviderCardView';
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
  const [selected, setSelected] = useState<Provider | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [defaultTab, setDefaultTab] = useState<'basic' | 'products'>('basic');
  const [startEditing, setStartEditing] = useState(false);

  const filtered = providers.filter((p) =>
    !search || p.providerName.toLowerCase().includes(search.toLowerCase())
  );

  const handleCreated = useCallback(() => {
    setCreateOpen(false);
  }, []);

  const handleViewProducts = useCallback((provider: Provider) => {
    setSelected(provider);
    setDefaultTab('products');
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
        <Input.Search
          placeholder={t('search', { defaultValue: '搜索供应商' })}
          style={{ width: 300 }}
          onSearch={setSearch}
          allowClear
        />
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            {t('addProvider', { defaultValue: '新增供应商' })}
          </Button>
        )}
      </div>

      <ProviderCardView
        providers={filtered}
        onSelect={handleSelect}
        onEdit={handleEdit}
        onDelete={handleDelete}
        onViewProducts={handleViewProducts}
      />

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