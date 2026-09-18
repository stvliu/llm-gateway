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
  FileSearchOutlined,
  SafetyOutlined,
  AccountBookOutlined,
  SettingOutlined,
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

/**
 * 菜单分组：镜像 Gitee wiki 侧边栏「管理员指南」单节结构。
 * wiki 顶层 Home → 快速开始 → 管理员指南 → 开发者指南 → 参考；
 * 管理台仅 Home（仪表盘，见 topLevelMenuItems）与「管理员指南」有对应页面。
 */
export const menuGroups: MenuGroupConfig[] = [
  {
    key: 'adminGuide',
    label: 'menu.group.adminGuide',
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
      {
        key: '/token-limits',
        icon: <AccountBookOutlined />,
        label: 'menu.tokenLimits',
        permission: 'token-limit:manage',
      },
      {
        key: '/resilience/overview',
        icon: <SafetyOutlined />,
        label: 'menu.resilience',
        permission: 'resilience:read',
      },
      {
        key: '/audit-logs',
        icon: <FileSearchOutlined />,
        label: 'menu.auditLogs',
        permission: 'audit:read',
      },
      {
        key: '/settings',
        icon: <SettingOutlined />,
        label: 'menu.settings',
        permission: 'settings:read',
      },
    ],
  },
];
