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
import { useMemo } from 'react';
import { Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { topLevelMenuItems, menuGroups } from '@/constants/menuConfig';
import type { MenuItemConfig, MenuGroupConfig } from '@/constants/menuConfig';
import type { MenuProps } from 'antd';
import type { Permission } from '@/constants/permissions';

interface SidebarProps {
  collapsed: boolean;
}

/** 过滤单个菜单项 */
function filterMenuItem(
  item: MenuItemConfig,
  hasPermission: (p: Permission) => boolean,
  t: (key: string) => string
): NonNullable<MenuProps['items']>[number] | null {
  if (item.permission && !hasPermission(item.permission)) {
    return null;
  }
  return {
    key: item.key,
    icon: item.icon,
    label: t(item.label),
    disabled: item.reserved,
  };
}

/** 过滤菜单分组 */
function filterMenuGroup(
  group: MenuGroupConfig,
  hasPermission: (p: Permission) => boolean,
  t: (key: string) => string
): NonNullable<MenuProps['items']>[number] | null {
  const filteredItems = group.items
    .map((item) => filterMenuItem(item, hasPermission, t))
    .filter(Boolean);

  if (filteredItems.length === 0) return null;

  return {
    type: 'group' as const,
    key: group.key,
    label: t(group.label),
    children: filteredItems,
  };
}

export function Sidebar({ collapsed }: SidebarProps) {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const location = useLocation();
  const { hasPermission } = useAuthStore();

  const menuItems = useMemo(() => {
    const items: MenuProps['items'] = [];

    // 顶层独立菜单项
    topLevelMenuItems.forEach((item) => {
      const filtered = filterMenuItem(item, hasPermission, t);
      if (filtered) items.push(filtered);
    });

    // 分组菜单项
    menuGroups.forEach((group) => {
      const filtered = filterMenuGroup(group, hasPermission, t);
      if (filtered) items.push(filtered);
    });

    return items;
  }, [hasPermission, t]);

  const selectedKeys = [location.pathname];

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    navigate(key);
  };

  return (
    <Menu
      mode="inline"
      inlineCollapsed={collapsed}
      selectedKeys={selectedKeys}
      items={menuItems}
      onClick={handleMenuClick}
      style={{ height: '100%', borderRight: 0 }}
    />
  );
}
