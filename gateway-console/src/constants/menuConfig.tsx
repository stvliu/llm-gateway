/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
  DashboardOutlined,
  ApiOutlined,
  AppstoreOutlined,
  DatabaseOutlined,
  KeyOutlined,
  UserSwitchOutlined,
  BarChartOutlined,
  FileSearchOutlined,
  SafetyOutlined,
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
        key: '/applications',
        icon: <AppstoreOutlined />,
        label: 'menu.applications',
        permission: 'application:read',
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
        key: '/resilience/overview',
        icon: <SafetyOutlined />,
        label: 'menu.resilience',
        permission: 'resilience:read',
      },
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
