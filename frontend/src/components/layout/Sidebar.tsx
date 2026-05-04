import { Menu } from 'antd';
import {
  AppstoreOutlined,
  TeamOutlined,
  SettingOutlined,
  KeyOutlined,
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

  const adminItems: MenuProps['items'] = [
    {
      key: '/admin/models',
      icon: <AppstoreOutlined />,
      label: t('models:title', { ns: 'models' }),
    },
    {
      key: '/admin/users',
      icon: <TeamOutlined />,
      label: t('users:title', { ns: 'users' }),
    },
    {
      key: '/admin/settings',
      icon: <SettingOutlined />,
      label: t('settings:title', { ns: 'common' }),
    },
  ];

  const userItems: MenuProps['items'] = [
    {
      key: '/user/models',
      icon: <AppstoreOutlined />,
      label: t('models:title', { ns: 'models' }),
    },
    {
      key: '/user/api-keys',
      icon: <KeyOutlined />,
      label: t('apiKeys:title', { ns: 'apiKeys' }),
    },
    {
      key: '/user/settings',
      icon: <SettingOutlined />,
      label: t('settings:title', { ns: 'common' }),
    },
  ];

  const items = role === 'ADMIN' ? adminItems : userItems;

  return (
    <Menu
      mode="inline"
      selectedKeys={[location.pathname]}
      items={items}
      onClick={({ key }) => navigate(key)}
      inlineCollapsed={collapsed}
      style={{ height: '100%', borderRight: 0 }}
    />
  );
}
