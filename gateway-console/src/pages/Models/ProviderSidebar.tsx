import { useMemo } from 'react';
import { Card, Button, Space, Tag, Empty, Spin, theme } from 'antd';
import { PlusOutlined, ApiOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { StatusIndicator } from '@/components/common';
import type { Provider } from '@/types/provider';

interface ProviderSidebarProps {
  providers: Provider[];
  isLoading: boolean;
  selectedProviderId: number | null;
  onSelect: (provider: Provider) => void;
  onAdd: () => void;
}

/**
 * 供应商侧边栏
 */
export function ProviderSidebar({ providers, isLoading, selectedProviderId, onSelect, onAdd }: ProviderSidebarProps) {
  const { token } = theme.useToken();
  const { t } = useTranslation('models');

  // 按状态排序：活跃优先
  const sortedProviders = useMemo(() => {
    return [...providers].sort((a, b) => {
      if (a.state === 'ACTIVE' && b.state !== 'ACTIVE') return -1;
      if (a.state !== 'ACTIVE' && b.state === 'ACTIVE') return 1;
      return a.providerName.localeCompare(b.providerName);
    });
  }, [providers]);

  if (isLoading) {
    return (
      <Card
        style={{ height: '100%', border: 'none' }}
        styles={{ body: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' } }}
      >
        <Spin />
      </Card>
    );
  }

  return (
    <Card
      style={{
        height: '100%',
        border: 'none',
        boxShadow: token.boxShadow,
      }}
      styles={{ body: { padding: 0, height: '100%', display: 'flex', flexDirection: 'column' } }}
    >
      {/* 标题 */}
      <div
        style={{
          padding: '16px 20px',
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
          fontWeight: 600,
        }}
      >
        {t('provider.list', { defaultValue: '供应商列表' })}
      </div>

      {/* 供应商列表 */}
      <div style={{ flex: 1, overflow: 'auto', padding: '12px' }}>
        {sortedProviders.length === 0 ? (
          <Empty
            description={t('empty.noProvider')}
            style={{ padding: 40 }}
          />
        ) : (
          <Space direction="vertical" style={{ width: '100%' }} size={8}>
            {sortedProviders.map((provider) => {
              const isSelected = selectedProviderId === provider.id;
              const isActive = provider.state === 'ACTIVE';
              const keyCount = provider.keyStats?.totalCount || 0;

              return (
                <Card
                  key={provider.id}
                  style={{
                    cursor: 'pointer',
                    border: isSelected ? `2px solid ${token.colorPrimary}` : '1px solid transparent',
                    boxShadow: isSelected
                      ? token.boxShadowSecondary
                      : token.boxShadowTertiary,
                    transition: 'all 0.2s',
                  }}
                  styles={{ body: { padding: '12px 16px' } }}
                  onClick={() => onSelect(provider)}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <StatusIndicator status={isActive ? 'ACTIVE' : 'DISABLED'} showLabel={false} />
                        <span style={{ fontWeight: 500 }}>{provider.providerName}</span>
                      </div>
                      <Tag color="blue" style={{ marginTop: 8 }}>
                        {t(`type.${provider.providerType}`, { ns: 'providers', defaultValue: provider.providerType })}
                      </Tag>
                    </div>
                    <Space direction="vertical" size={4} align="end">
                      <Space size={4}>
                        <ApiOutlined style={{ fontSize: 12, color: token.colorTextSecondary }} />
                        <span style={{ fontSize: 12, color: token.colorTextSecondary }}>{keyCount} Keys</span>
                      </Space>
                    </Space>
                  </div>
                </Card>
              );
            })}
          </Space>
        )}
      </div>

      {/* 底部添加按钮 */}
      <div
        style={{
          padding: '16px 20px',
          borderTop: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <Button
          type="primary"
          icon={<PlusOutlined />}
          block
          onClick={onAdd}
        >
          {t('addProvider')}
        </Button>
      </div>
    </Card>
  );
}