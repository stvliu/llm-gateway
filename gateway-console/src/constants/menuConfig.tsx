import {
  DashboardOutlined,
  AppstoreOutlined,
  CloudServerOutlined,
  CloudDownloadOutlined,
  ApiOutlined,
  TeamOutlined,
  KeyOutlined,
  SettingOutlined,
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
    key: 'model-center',
    icon: <AppstoreOutlined />,
    label: 'menu.modelCenter',
    children: [
      {
        key: '/models',
        icon: <AppstoreOutlined />,
        label: 'menu.models',
        permission: 'model:read',
      },
      {
        key: '/providers',
        icon: <CloudServerOutlined />,
        label: 'menu.providers',
        permission: 'provider:read',
      },
      {
        key: '/templates',
        icon: <CloudDownloadOutlined />,
        label: 'menu.templates',
        permission: 'template:read',
      },
      {
        key: '/api-key-pool',
        icon: <ApiOutlined />,
        label: 'menu.apiKeyPool',
        permission: 'apikey-pool:read',
      },
    ],
  },
  {
    key: 'user-center',
    icon: <TeamOutlined />,
    label: 'menu.userCenter',
    children: [
      {
        key: '/users',
        icon: <TeamOutlined />,
        label: 'menu.users',
        permission: 'user:read',
      },
      {
        key: '/api-keys',
        icon: <KeyOutlined />,
        label: 'menu.apiKeys',
        permission: 'apikey:manage',
      },
    ],
  },
  {
    key: '/settings',
    icon: <SettingOutlined />,
    label: 'menu.settings',
    permission: 'settings:read',
  },
];