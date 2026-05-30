import {
  DashboardOutlined,
  CloudServerOutlined,
  TeamOutlined,
  SettingOutlined,
  DatabaseOutlined,
  LockOutlined,
  KeyOutlined,
  CodeOutlined,
} from '@ant-design/icons';
import type { Permission } from '@/constants/permissions';

export interface MenuItemConfig {
  key: string;
  icon: React.ReactNode;
  /** i18n key，namespace 为 common */
  label: string;
  /** 所需权限，无声明则所有登录用户可见 */
  permission?: Permission;
  children?: MenuItemConfig[];
}

export const menuConfig: MenuItemConfig[] = [
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: 'menu.home',
  },
  {
    key: '/providers',
    icon: <CloudServerOutlined />,
    label: 'menu.providers',
    permission: 'provider:read',
  },
  {
    key: '/models',
    icon: <DatabaseOutlined />,
    label: 'menu.models',
    permission: 'model:read',
  },
  {
    key: '/keys',
    icon: <KeyOutlined />,
    label: 'menu.apiKeys',
    permission: 'key:read',
  },
  {
    key: '/teams',
    icon: <TeamOutlined />,
    label: 'menu.teams',
    permission: 'user:read',
  },
  {
    key: '/developer',
    icon: <CodeOutlined />,
    label: 'menu.developer',
    permission: 'developer:access',
  },
  {
    key: 'system-settings',
    icon: <SettingOutlined />,
    label: 'menu.systemSettings',
    children: [
      {
        key: '/catalog',
        icon: <DatabaseOutlined />,
        label: 'menu.catalog',
        permission: 'catalog:read',
      },
      {
        key: '/change-password',
        icon: <LockOutlined />,
        label: 'menu.changePassword',
      },
    ],
  },
];
