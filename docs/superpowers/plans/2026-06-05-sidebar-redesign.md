# Sidebar 菜单重设计实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构 Sidebar 菜单为三域分组结构，补全缺失菜单项，调整路由和交互

**Architecture:** 采用 Ant Design Menu.ItemGroup 渲染分组，menuConfig 数据结构改为支持分组配置，Header 头像下拉菜单新增"修改密码"Modal

**Tech Stack:** React 18, Ant Design 5, TypeScript, i18next, Zustand

---

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|------|----------|------|
| 修改 | `src/constants/menuConfig.tsx` | 新数据结构，三域分组 |
| 修改 | `src/constants/permissions.ts` | `DEVELOPER` → `QUICKSTART` |
| 修改 | `src/components/layout/Sidebar.tsx` | 渲染 ItemGroup 分组 |
| 修改 | `src/components/layout/Header.tsx` | 头像下拉加修改密码，快速开始路由改 `/quickstart` |
| 新建 | `src/components/ChangePasswordModal.tsx` | 修改密码 Modal 组件 |
| 修改 | `src/router/index.tsx` | `/developer` → `/quickstart`，删除 `/change-password` |
| 修改 | `src/i18n.ts` | `developer` namespace → `quickstart` |
| 删除 | `src/pages/ChangePassword/index.tsx` | 改为 Modal |
| 重命名 | `src/pages/Developer/` → `src/pages/Quickstart/` | 目录重命名 |
| 重命名 | `src/locales/zh-CN/developer.json` → `quickstart.json` | i18n 文件重命名 |
| 重命名 | `src/locales/en-US/developer.json` → `quickstart.json` | i18n 文件重命名 |
| 修改 | `src/locales/zh-CN/common.json` | 新增菜单分组 i18n key |
| 修改 | `src/locales/en-US/common.json` | 新增菜单分组 i18n key |

---

### Task 1: 更新权限常量

**Files:**
- Modify: `src/constants/permissions.ts`

- [ ] **Step 1: 修改权限常量**

将 `DEVELOPER: 'developer:access'` 改为 `QUICKSTART: 'quickstart:access'`：

```typescript
/** 权限常量 */
export const P = {
  DASHBOARD: 'dashboard',
  DASHBOARD_ADMIN: 'dashboard:admin',
  MODEL_READ: 'model:read',
  MODEL_WRITE: 'model:write',
  PROVIDER_READ: 'provider:read',
  PROVIDER_WRITE: 'provider:write',
  CATALOG_READ: 'catalog:read',
  CATALOG_WRITE: 'catalog:write',
  USER_READ: 'user:read',
  USER_WRITE: 'user:write',
  SETTINGS_READ: 'settings:read',
  SETTINGS_WRITE: 'settings:write',
  KEY_READ: 'key:read',
  KEY_WRITE: 'key:write',
  CHANNEL_READ: 'channel:read',
  CHANNEL_WRITE: 'channel:write',
  QUICKSTART: 'quickstart:access',
  AUDIT_READ: 'audit:read',
} as const;

export type Permission = (typeof P)[keyof typeof P];
```

- [ ] **Step 2: 提交**

```bash
git add src/constants/permissions.ts
git commit -m "refactor(console): 权限常量 DEVELOPER 重命名为 QUICKSTART"
```

---

### Task 2: 更新 i18n 文件

**Files:**
- Modify: `src/locales/zh-CN/common.json`
- Modify: `src/locales/en-US/common.json`
- Rename: `src/locales/zh-CN/developer.json` → `src/locales/zh-CN/quickstart.json`
- Rename: `src/locales/en-US/developer.json` → `src/locales/en-US/quickstart.json`

- [ ] **Step 1: 更新中文 common.json**

在 `menu` 对象中新增分组和菜单项（只修改 `menu` 对象，保留其他字段不变）：

- 新增 key：`models`、`apiKeys`、`quickstart`、`users`、`stats`、`auditLogs`、`group.supply`、`group.identity`、`group.operations`
- 修改 key：`catalog` 值从"目录管理"改为"目录"
- 删除 key：`systemSettings`、`developer`

- [ ] **Step 2: 更新英文 common.json**

在 `menu` 对象中新增分组和菜单项（只修改 `menu` 对象，保留其他字段不变）：

- 新增 key：`models`、`apiKeys`、`quickstart`、`users`、`stats`、`auditLogs`、`group.supply`、`group.identity`、`group.operations`
- 修改 key：`catalog` 值从"Catalog"改为"Catalog"（不变）
- 删除 key：`systemSettings`、`developer`

- [ ] **Step 3: 重命名 developer.json 为 quickstart.json**

```bash
mv gateway-console/src/locales/zh-CN/developer.json gateway-console/src/locales/zh-CN/quickstart.json
mv gateway-console/src/locales/en-US/developer.json gateway-console/src/locales/en-US/quickstart.json
```

- [ ] **Step 4: 提交**

```bash
git add src/locales/
git commit -m "refactor(console): 更新菜单 i18n，新增分组标签，developer 重命名为 quickstart"
```

---

### Task 3: 更新 i18n 配置

**Files:**
- Modify: `src/i18n.ts`

- [ ] **Step 1: 修改 i18n 配置**

将所有 `developer` 引用改为 `quickstart`：

```typescript
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// 中文
import zhCNCommon from './locales/zh-CN/common.json';
import zhCNLogin from './locales/zh-CN/login.json';
import zhCNModels from './locales/zh-CN/models.json';
import zhCNUsers from './locales/zh-CN/users.json';
import zhCNDashboard from './locales/zh-CN/dashboard.json';
import zhCNProviders from './locales/zh-CN/providers.json';
import zhCNChat from './locales/zh-CN/chat.json';
import zhCNExperience from './locales/zh-CN/experience.json';
import zhCNTeams from './locales/zh-CN/teams.json';
import zhCNCatalog from './locales/zh-CN/catalog.json';
import zhCNChannels from './locales/zh-CN/channels.json';
import zhCNQuickstart from './locales/zh-CN/quickstart.json';
// 英文
import enUSCommon from './locales/en-US/common.json';
import enUSLogin from './locales/en-US/login.json';
import enUSModels from './locales/en-US/models.json';
import enUSUsers from './locales/en-US/users.json';
import enUSDashboard from './locales/en-US/dashboard.json';
import enUSProviders from './locales/en-US/providers.json';
import enUSChat from './locales/en-US/chat.json';
import enUSExperience from './locales/en-US/experience.json';
import enUSTeams from './locales/en-US/teams.json';
import enUSCatalog from './locales/en-US/catalog.json';
import enUSChannels from './locales/en-US/channels.json';
import enUSQuickstart from './locales/en-US/quickstart.json';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      'zh-CN': {
        common: zhCNCommon,
        login: zhCNLogin,
        models: zhCNModels,
        users: zhCNUsers,
        dashboard: zhCNDashboard,
        providers: zhCNProviders,
        chat: zhCNChat,
        experience: zhCNExperience,
        teams: zhCNTeams,
        catalog: zhCNCatalog,
        channels: zhCNChannels,
        quickstart: zhCNQuickstart,
      },
      'en-US': {
        common: enUSCommon,
        login: enUSLogin,
        models: enUSModels,
        users: enUSUsers,
        dashboard: enUSDashboard,
        providers: enUSProviders,
        chat: enUSChat,
        experience: enUSExperience,
        teams: enUSTeams,
        catalog: enUSCatalog,
        channels: enUSChannels,
        quickstart: enUSQuickstart,
      },
    },
    fallbackLng: 'zh-CN',
    defaultNS: 'common',
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
    },
  });

export default i18n;
```

- [ ] **Step 2: 提交**

```bash
git add src/i18n.ts
git commit -m "refactor(console): i18n 配置 developer namespace 重命名为 quickstart"
```

---

### Task 4: 重构 menuConfig 数据结构

**Files:**
- Modify: `src/constants/menuConfig.tsx`

- [ ] **Step 1: 重写 menuConfig**

采用新的分组数据结构：

```typescript
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
```

- [ ] **Step 2: 提交**

```bash
git add src/constants/menuConfig.tsx
git commit -m "refactor(console): 重构 menuConfig 为三域分组结构"
```

---

### Task 5: 重构 Sidebar 组件

**Files:**
- Modify: `src/components/layout/Sidebar.tsx`

- [ ] **Step 1: 重写 Sidebar 组件**

使用 ItemGroup 渲染分组：

```typescript
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
): MenuProps['items'][number] | null {
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
): MenuProps['items'][number] | null {
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

  const handleMenuClick = ({ key, item }: { key: string; item?: { props?: { disabled?: boolean } } }) => {
    // 禁用项不跳转
    if (item?.props?.disabled) return;
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
```

- [ ] **Step 2: 提交**

```bash
git add src/components/layout/Sidebar.tsx
git commit -m "refactor(console): Sidebar 组件支持 ItemGroup 分组渲染"
```

---

### Task 6: 创建 ChangePasswordModal 组件

**Files:**
- Create: `src/components/ChangePasswordModal.tsx`

- [ ] **Step 1: 创建 ChangePasswordModal 组件**

```typescript
import { useState } from 'react';
import { Modal, Form, Input, Button } from 'antd';
import { useTranslation } from 'react-i18next';
import { authApi } from '@/services/api/auth';
import { useMessage } from '@/hooks/useMessage';

interface ChangePasswordModalProps {
  open: boolean;
  onClose: () => void;
}

export function ChangePasswordModal({ open, onClose }: ChangePasswordModalProps) {
  const message = useMessage();
  const { t } = useTranslation('common');
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  const handleSubmit = async (values: { currentPassword: string; newPassword: string; confirmPassword: string }) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error(t('changePassword.passwordMismatch'));
      return;
    }

    setLoading(true);
    try {
      await authApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      message.success(t('message.success'));
      form.resetFields();
      onClose();
    } catch {
      message.error(t('message.error'));
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title={t('changePassword.title')}
      open={open}
      onCancel={handleCancel}
      footer={null}
      destroyOnClose
    >
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item
          name="currentPassword"
          label={t('changePassword.currentPassword')}
          rules={[{ required: true }]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label={t('changePassword.newPassword')}
          rules={[{ required: true, min: 6 }]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label={t('changePassword.confirmPassword')}
          rules={[{ required: true }]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Button onClick={handleCancel} style={{ marginRight: 8 }}>
            {t('actions.cancel')}
          </Button>
          <Button type="primary" htmlType="submit" loading={loading}>
            {t('actions.save')}
          </Button>
        </Form.Item>
      </Form>
    </Modal>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add src/components/ChangePasswordModal.tsx
git commit -m "feat(console): 新增 ChangePasswordModal 组件"
```

---

### Task 7: 更新 Header 组件

**Files:**
- Modify: `src/components/layout/Header.tsx`

- [ ] **Step 1: 修改 Header 组件**

在头像下拉菜单中添加"修改密码"，修改"快速开始"按钮路由：

```typescript
import { useState } from 'react';
import { Space, Dropdown, Button, Select, Avatar, theme, type MenuProps } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SunOutlined,
  MoonOutlined,
  LaptopOutlined,
  CodeOutlined,
  LogoutOutlined,
  LockOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { useThemeStore } from '@/stores/themeStore';
import { ChangePasswordModal } from '@/components/ChangePasswordModal';
import logoSvg from '@/assets/images/logo-full.svg';

interface LeftHeaderProps {
  collapsed: boolean;
}

interface RightHeaderProps {
  collapsed: boolean;
  onToggle: () => void;
}

/**
 * 左侧头部组件（侧边栏内）
 * 显示 Logo 和应用名称，颜色跟随全局主题
 */
export function LeftHeader({ collapsed }: LeftHeaderProps) {
  const { t } = useTranslation('common');
  const { token } = theme.useToken();

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        padding: '0 16px',
        height: 48,
        background: 'transparent',
      }}
    >
      <img
        src={logoSvg}
        alt={t('app.title')}
        style={{
          height: 28,
          width: 'auto',
          opacity: collapsed ? 0 : 1,
          transition: 'opacity 0.2s',
        }}
      />
      {!collapsed && (
        <span
          style={{
            marginLeft: 12,
            fontSize: 16,
            fontWeight: 600,
            color: token.colorText,
            whiteSpace: 'nowrap',
          }}
        >
          {t('app.title')}
        </span>
      )}
    </div>
  );
}

/**
 * 右侧头部组件
 * 显示折叠按钮、主题切换、语言切换、用户菜单
 */
export function RightHeader({ collapsed, onToggle }: RightHeaderProps) {
  const { t, i18n } = useTranslation('common');
  const navigate = useNavigate();
  const { user, logout, hasPermission } = useAuthStore();
  const { setMode, getEffectiveTheme } = useThemeStore();
  const { token } = theme.useToken();
  const isDark = getEffectiveTheme() === 'dark';
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);

  const headerStyle = {
    display: 'flex',
    alignItems: 'center',
    padding: '0 16px',
    height: 48,
    background: token.colorBgContainer,
    borderBottom: `1px solid ${token.colorBorderSecondary}`,
  };

  const themeItems: MenuProps['items'] = [
    {
      key: 'system',
      label: t('theme.system'),
      icon: <LaptopOutlined />,
      onClick: () => setMode('system'),
    },
    {
      key: 'light',
      label: t('theme.light'),
      icon: <SunOutlined />,
      onClick: () => setMode('light'),
    },
    {
      key: 'dark',
      label: t('theme.dark'),
      icon: <MoonOutlined />,
      onClick: () => setMode('dark'),
    },
  ];

  const userItems: MenuProps['items'] = [
    {
      key: 'change-password',
      label: t('menu.changePassword'),
      icon: <LockOutlined />,
      onClick: () => setPasswordModalOpen(true),
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      label: t('user.logout'),
      icon: <LogoutOutlined />,
      onClick: logout,
    },
  ];

  const handleLanguageChange = (lng: string) => {
    i18n.changeLanguage(lng);
  };

  const ThemeIcon = isDark ? MoonOutlined : SunOutlined;

  return (
    <>
      <div style={{ ...headerStyle, justifyContent: 'space-between' }}>
        <Button
          type="text"
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={onToggle}
        />

        <Space>
          <Dropdown menu={{ items: themeItems }} trigger={['click']}>
            <Button type="text" icon={<ThemeIcon />} />
          </Dropdown>

          <Select
            value={i18n.language || 'zh-CN'}
            onChange={handleLanguageChange}
            variant="borderless"
            options={[
              { value: 'zh-CN', label: t('language.zhCN') },
              { value: 'en-US', label: t('language.enUS') },
            ]}
          />

          {hasPermission('quickstart:access') && (
            <Button
              type="text"
              icon={<CodeOutlined />}
              onClick={() => navigate('/quickstart')}
            >
              {t('header.quickStart')}
            </Button>
          )}

          <Dropdown menu={{ items: userItems }} trigger={['click']}>
            <Space style={{ cursor: 'pointer' }}>
              <Avatar size="small" style={{ backgroundColor: token.colorPrimary }}>
                {user?.username?.charAt(0).toUpperCase()}
              </Avatar>
              <span style={{ color: token.colorText }}>
                {user?.username}
              </span>
            </Space>
          </Dropdown>
        </Space>
      </div>

      <ChangePasswordModal
        open={passwordModalOpen}
        onClose={() => setPasswordModalOpen(false)}
      />
    </>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add src/components/layout/Header.tsx
git commit -m "feat(console): Header 头像菜单新增修改密码，快速开始路由改为 /quickstart"
```

---

### Task 8: 更新路由配置

**Files:**
- Modify: `src/router/index.tsx`

- [ ] **Step 1: 修改路由配置**

将 `/developer` 改为 `/quickstart`，删除 `/change-password` 路由：

```typescript
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AuthGuard, PermissionGuard } from './guards';
import Login from '@/pages/Login';
import AppLayout from '@/components/layout/AppLayout';
import Dashboard from '@/pages/Dashboard';
import Models from '@/pages/Models';
import ApiKeys from '@/pages/ApiKeys';
import Quickstart from '@/pages/Quickstart';
import Channels from '@/pages/Channels';
import Catalog from '@/pages/Catalog';
import Users from '@/pages/Users';
import TeamsPage from '@/pages/Teams';
import { P } from '@/constants/permissions';

export const router = createBrowserRouter([
  // 公共路由
  {
    path: '/login',
    element: <Login />,
  },

  // 应用路由（统一入口）
  {
    path: '/',
    element: (
      <AuthGuard>
        <AppLayout />
      </AuthGuard>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <Dashboard /> },
      {
        path: 'channels',
        element: <PermissionGuard permission={P.CHANNEL_READ}><Channels /></PermissionGuard>,
      },
      {
        path: 'providers',
        element: <Navigate to="/channels" replace />,
      },
      {
        path: 'models',
        element: <PermissionGuard permission={P.MODEL_READ}><Models /></PermissionGuard>,
      },
      {
        path: 'keys',
        element: <PermissionGuard permission={P.KEY_READ}><ApiKeys /></PermissionGuard>,
      },
      {
        path: 'quickstart',
        element: <PermissionGuard permission={P.QUICKSTART}><Quickstart /></PermissionGuard>,
      },
      {
        path: 'catalog',
        element: <PermissionGuard permission={P.CATALOG_READ}><Catalog /></PermissionGuard>,
      },
      {
        path: 'users',
        element: <PermissionGuard permission={P.USER_READ}><Users /></PermissionGuard>,
      },
      {
        path: 'teams',
        element: <PermissionGuard permission={P.USER_READ}><TeamsPage /></PermissionGuard>,
      },
    ],
  },

  // 兼容旧路由重定向
  { path: '/admin/*', element: <Navigate to="/" replace /> },
  { path: '/user/*', element: <Navigate to="/" replace /> },
  { path: '/experience', element: <Navigate to="/dashboard" replace /> },
  { path: '/api-key-pool', element: <Navigate to="/providers" replace /> },
  { path: '/metadata', element: <Navigate to="/catalog" replace /> },
  { path: '/developer', element: <Navigate to="/quickstart" replace /> },
  { path: '/change-password', element: <Navigate to="/dashboard" replace /> },

  // 默认重定向
  { path: '*', element: <Navigate to="/dashboard" replace /> },
]);
```

- [ ] **Step 2: 提交**

```bash
git add src/router/index.tsx
git commit -m "refactor(console): 路由 /developer 改为 /quickstart，删除 /change-password"
```

---

### Task 9: 重命名 Developer 页面目录

**Files:**
- Rename: `src/pages/Developer/` → `src/pages/Quickstart/`

- [ ] **Step 1: 重命名目录**

```bash
mv gateway-console/src/pages/Developer gateway-console/src/pages/Quickstart
```

- [ ] **Step 2: 更新 Quickstart/index.tsx 中的 i18n namespace**

将 `useTranslation('developer')` 改为 `useTranslation('quickstart')`：

```typescript
// 在 Quickstart/index.tsx 中
const { t } = useTranslation('quickstart');
```

- [ ] **Step 3: 提交**

```bash
git add src/pages/Quickstart/
git commit -m "refactor(console): Developer 页面重命名为 Quickstart"
```

---

### Task 10: 删除 ChangePassword 页面

**Files:**
- Delete: `src/pages/ChangePassword/index.tsx`

- [ ] **Step 1: 删除目录**

```bash
rm -rf gateway-console/src/pages/ChangePassword
```

- [ ] **Step 2: 提交**

```bash
git add -A
git commit -m "refactor(console): 删除 ChangePassword 独立页面，改为 Modal 组件"
```

---

### Task 11: 验证构建

- [ ] **Step 1: 运行 TypeScript 编译检查**

```bash
cd gateway-console && npm run build
```

预期：编译成功，无 TypeScript 错误

- [ ] **Step 2: 运行开发服务器验证**

```bash
cd gateway-console && npm run dev
```

手动验证：
1. Sidebar 显示三域分组结构
2. 点击各菜单项可正常跳转
3. 头像下拉菜单显示"修改密码"和"退出登录"
4. 点击"修改密码"弹出 Modal
5. 点击"快速开始"按钮跳转到 `/quickstart`
6. 折叠 Sidebar 时分组标签隐藏，只显示图标

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "feat(console): Sidebar 菜单重设计完成

- 三域分组：模型供给/身份与权限/运营与系统
- 补全缺失菜单项：模型/API Key/用户管理
- /developer → /quickstart
- 修改密码改为 Modal 弹窗
- 预留统计/审计日志入口"
```
