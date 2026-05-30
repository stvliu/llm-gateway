import { useMemo } from 'react';
import { Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { menuConfig } from '@/constants/menuConfig';
import type { MenuItemConfig } from '@/constants/menuConfig';
import type { MenuProps } from 'antd';
import type { Permission } from '@/constants/permissions';

interface SidebarProps {
  collapsed: boolean;
}

/** 递归过滤+翻译菜单项 */
function buildMenuItems(
  items: MenuItemConfig[],
  hasPermission: (p: Permission) => boolean,
  t: (key: string) => string
): MenuProps['items'] {
  return items
    .map((item) => {
      if (item.permission && !hasPermission(item.permission)) {
        return null;
      }
      if (item.children) {
        const filteredChildren = buildMenuItems(item.children, hasPermission, t);
        if (!filteredChildren || filteredChildren.length === 0) return null;
        return {
          key: item.key,
          icon: item.icon,
          label: t(item.label),
          children: filteredChildren,
        };
      }
      return {
        key: item.key,
        icon: item.icon,
        label: t(item.label),
      };
    })
    .filter(Boolean) as NonNullable<MenuProps['items']>;
}

export function Sidebar({ collapsed }: SidebarProps) {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const location = useLocation();
  const { hasPermission } = useAuthStore();

  const menuItems = useMemo(() => {
    return buildMenuItems(menuConfig, hasPermission, t);
  }, [menuConfig, hasPermission, t]);

  const openKeys = useMemo(() => {
    return menuConfig
      .filter((item) => item.children)
      .map((item) => item.key);
  }, []);

  const selectedKeys = [location.pathname];

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key);
  };

  return (
    <Menu
      mode="inline"
      inlineCollapsed={collapsed}
      defaultOpenKeys={openKeys}
      selectedKeys={selectedKeys}
      items={menuItems}
      onClick={handleMenuClick}
      style={{ height: '100%', borderRight: 0 }}
    />
  );
}
