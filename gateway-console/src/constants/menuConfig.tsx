import {
  DashboardOutlined,
  CloudServerOutlined,
  TeamOutlined,
  SettingOutlined,
  DatabaseOutlined,
  LockOutlined,
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
    key: 'provider-management',
    icon: <CloudServerOutlined />,
    label: 'menu.providerManagement',
    children: [
      {
        key: '/providers',
        icon: <CloudServerOutlined />,
        label: 'menu.providers',
        permission: 'provider:read',
      },
    ],
  },
  {
    key: 'user-management',
    icon: <TeamOutlined />,
    label: 'menu.userManagement',
    children: [
      {
        key: '/users',
        icon: <TeamOutlined />,
        label: 'menu.users',
        permission: 'user:read',
      },
      {
        key: '/teams',
        icon: <TeamOutlined />,
        label: 'menu.teams',
        permission: 'user:read',
      },
    ],
  },
  {
    key: 'system-settings',
    icon: <SettingOutlined />,
    label: 'menu.systemSettings',
    children: [
      {
        key: '/metadata',
        icon: <DatabaseOutlined />,
        label: 'menu.metadata',
        permission: 'metadata:read',
      },
      {
        key: '/change-password',
        icon: <LockOutlined />,
        label: 'menu.changePassword',
      },
    ],
  },
];
