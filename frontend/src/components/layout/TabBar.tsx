import { Tabs, type TabsProps } from 'antd';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useMemo } from 'react';

interface TabConfig {
  key: string;
  labelNs: string;
  labelKey: string;
}

const adminTabs: TabConfig[] = [
  { key: '/admin/models', labelNs: 'models', labelKey: 'title' },
  { key: '/admin/users', labelNs: 'users', labelKey: 'title' },
];

const userTabs: TabConfig[] = [
  { key: '/user/models', labelNs: 'models', labelKey: 'title' },
  { key: '/user/api-keys', labelNs: 'apiKeys', labelKey: 'title' },
];

interface TabBarProps {
  role: 'ADMIN' | 'USER';
}

export function TabBar({ role }: TabBarProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  const tabs = role === 'ADMIN' ? adminTabs : userTabs;

  const items: TabsProps['items'] = useMemo(
    () =>
      tabs.map((tab) => ({
        key: tab.key,
        label: t(tab.labelKey, { ns: tab.labelNs }),
      })),
    [tabs, t]
  );

  const activeKey = tabs.find((tab) => location.pathname.startsWith(tab.key))?.key || tabs[0]?.key;

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Tabs
        activeKey={activeKey}
        items={items}
        onChange={(key) => navigate(key)}
        style={{ padding: '0 16px', margin: 0 }}
      />
      <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
        <Outlet />
      </div>
    </div>
  );
}
