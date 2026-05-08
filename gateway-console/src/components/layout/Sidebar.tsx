import { useState } from 'react';
import { Menu } from 'antd';
import {
  DashboardOutlined,
  AppstoreOutlined,
  TeamOutlined,
  SettingOutlined,
  KeyOutlined,
  CloudServerOutlined,
  ApiOutlined,
  CloudDownloadOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import type { MenuProps } from 'antd';
import type { UserRole } from '@/types/user';

interface SidebarProps {
  collapsed: boolean;
  role: UserRole;
}

export function Sidebar({ collapsed, role }: SidebarProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  // 默认展开所有子菜单
  const allOpenKeys = ['model-center', 'user-center'];
  const [openKeys, setOpenKeys] = useState<string[]>(allOpenKeys);

  const adminItems: MenuProps['items'] = [
    {
      key: '/admin/dashboard',
      icon: <DashboardOutlined />,
      label: t('menu.home', { ns: 'common' }),
    },
    {
      key: 'model-center',
      icon: <AppstoreOutlined />,
      label: t('menu.modelCenter', { ns: 'common' }),
      children: [
        {
          key: '/admin/models',
          icon: <AppstoreOutlined />,
          label: t('menu.models', { ns: 'common' }),
        },
        {
          key: '/admin/providers',
          icon: <CloudServerOutlined />,
          label: t('menu.providers', { ns: 'common' }),
        },
        {
          key: '/admin/templates',
          icon: <CloudDownloadOutlined />,
          label: t('menu.templates', { ns: 'common' }),
        },
        {
          key: '/admin/api-key-pool',
          icon: <ApiOutlined />,
          label: t('menu.apiKeyPool', { ns: 'common' }),
        },
      ],
    },
    {
      key: 'user-center',
      icon: <TeamOutlined />,
      label: t('menu.userCenter', { ns: 'common' }),
      children: [
        {
          key: '/admin/users',
          icon: <TeamOutlined />,
          label: t('menu.users', { ns: 'common' }),
        },
        {
          key: '/admin/api-keys',
          icon: <KeyOutlined />,
          label: t('menu.apiKeys', { ns: 'common' }),
        },
      ],
    },
    {
      key: '/admin/settings',
      icon: <SettingOutlined />,
      label: t('menu.settings', { ns: 'common' }),
    },
  ];

  const userItems: MenuProps['items'] = [
    {
      key: '/user/dashboard',
      icon: <DashboardOutlined />,
      label: t('menu.home', { ns: 'common' }),
    },
    {
      key: '/user/models',
      icon: <AppstoreOutlined />,
      label: t('menu.models', { ns: 'common' }),
    },
    {
      key: '/user/api-keys',
      icon: <KeyOutlined />,
      label: t('menu.myApiKeys', { ns: 'common' }),
    },
    {
      key: '/user/settings',
      icon: <SettingOutlined />,
      label: t('menu.settings', { ns: 'common' }),
    },
  ];

  const items = role === 'ADMIN' ? adminItems : userItems;

  return (
    <Menu
      mode="inline"
      selectedKeys={[location.pathname]}
      openKeys={openKeys}
      onOpenChange={(keys) => setOpenKeys(keys as string[])}
      items={items}
      onClick={({ key }) => {
        if (!key.startsWith('/')) return; // 忽略父菜单点击
        navigate(key);
      }}
      inlineCollapsed={collapsed}
      style={{ height: '100%', borderRight: 0 }}
    />
  );
}
