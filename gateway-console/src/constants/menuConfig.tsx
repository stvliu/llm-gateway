import {
  DashboardOutlined,
  ApiOutlined,
  AppstoreOutlined,
  DatabaseOutlined,
  KeyOutlined,
  TeamOutlined,
  UserSwitchOutlined,
  BarChartOutlined,
  FileSearchOutlined,
} from '@ant-design/icons';
import type { Permission } from '@/constants/permissions';

/** 菜单项配置 */
export interface MenuItemConfig {
  key: string;
  icon: React.ReactNode;
  /** i18n key，namespace 为 common */
  label: string;
  /** 所需权限，无声明则所有登录用户可见 */
  permission?: Permission;
  /** 预留项，渲染为禁用状态 */
  reserved?: boolean;
}

/** 菜单分组配置 */
export interface MenuGroupConfig {
  key: string;
  /** i18n key，namespace 为 common */
  label: string;
  items: MenuItemConfig[];
}

/** 顶层独立菜单项（无分组） */
export const topLevelMenuItems: MenuItemConfig[] = [
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: 'menu.home',
  },
];

/** 菜单分组 */
export const menuGroups: MenuGroupConfig[] = [
  {
    key: 'supply',
    label: 'menu.group.supply',
    items: [
      {
        key: '/channels',
        icon: <ApiOutlined />,
        label: 'menu.channels',
        permission: 'channel:read',
      },
      {
        key: '/models',
        icon: <AppstoreOutlined />,
        label: 'menu.models',
        permission: 'model:read',
      },
      {
        key: '/catalog',
        icon: <DatabaseOutlined />,
        label: 'menu.catalog',
        permission: 'catalog:read',
      },
    ],
  },
  {
    key: 'identity',
    label: 'menu.group.identity',
    items: [
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
        key: '/users',
        icon: <UserSwitchOutlined />,
        label: 'menu.users',
        permission: 'user:read',
      },
    ],
  },
  {
    key: 'operations',
    label: 'menu.group.operations',
    items: [
      {
        key: '/stats',
        icon: <BarChartOutlined />,
        label: 'menu.stats',
        permission: 'dashboard:admin',
        reserved: true,
      },
      {
        key: '/audit-logs',
        icon: <FileSearchOutlined />,
        label: 'menu.auditLogs',
        permission: 'audit:read',
        reserved: true,
      },
    ],
  },
];
