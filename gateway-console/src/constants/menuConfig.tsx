import {
  DashboardOutlined,
  ThunderboltOutlined,
  TeamOutlined,
  SettingOutlined,
  DatabaseOutlined,
  LockOutlined,
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
    key: '/channels',
    icon: <ThunderboltOutlined />,
    label: 'menu.channels',
    permission: 'channel:read',
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
