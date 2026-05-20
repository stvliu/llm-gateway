import { useState, useCallback } from 'react';
import { Button, Input } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useProviders } from '@/services/query';
import type { Provider } from '@/types/provider';
import ProviderCardView from './ProviderCardView';
import { ProviderManagementDrawer } from './ProviderManagementDrawer';
import { ProviderCreateModal } from './ProviderCreateModal';

export default function Providers() {
  const { t } = useTranslation('providers');
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);

  const { data: providersData } = useProviders();
  const providers = providersData?.items ?? [];
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Provider | null>(null);
  const [createOpen, setCreateOpen] = useState(false);

  const filtered = providers.filter((p) =>
    !search || p.providerName.toLowerCase().includes(search.toLowerCase())
  );

  const handleCreated = useCallback(() => {
    setCreateOpen(false);
  }, []);

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
        onSelect={setSelected}
      />

      <ProviderManagementDrawer
        providerId={selected?.id ?? null}
        providers={providers}
        onClose={() => setSelected(null)}
        onProviderChange={(id) => {
          const p = providers.find((pr) => pr.id === id);
          if (p) setSelected(p);
        }}
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