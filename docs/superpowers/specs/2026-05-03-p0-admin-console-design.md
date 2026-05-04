# P0 管理控制台设计文档

## 概述

LLM-Gateway P0 阶段管理控制台，支持管理员和普通用户两种角色，提供模型中心、用户管理、API Key 管理等功能。

## 技术选型

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | React + TypeScript | 19.x / 5.x |
| 构建 | Vite | 6.x |
| UI 组件库 | Ant Design | 5.x |
| 状态管理 | Zustand | 5.x |
| 服务端状态 | TanStack Query | 5.x |
| 路由 | React Router | 7.x |
| HTTP 客户端 | Axios | 1.x |
| 国际化 | react-i18next | 14.x |

## 部署方案

**标准版**：前端构建产物打包进 Spring Boot JAR，通过 `static/` 资源目录提供静态文件服务。

**企业版**：前后端分离部署，前端独立部署至 Nginx/CDN。

## 角色权限模型

| 功能 | 管理员 | 普通用户 |
|------|--------|----------|
| 渠道管理 | 增删改查、启用/禁用 | 只读 |
| 模型管理 | 增删改查、启用/禁用 | 只读 |
| 用户管理 | 全部用户 CRUD | 无权限 |
| API Key 管理（管理员） | 全部 Key CRUD | 无权限 |
| API Key 管理（用户） | 无权限 | 仅自己的 Key CRUD |
| 个人设置 | 修改自己密码 | 修改自己密码 |

## 路由设计

### 管理员路由 `/admin/*`

| 路径 | 页面 | 功能 |
|------|------|------|
| `/admin/models` | 模型中心 | 左侧渠道 + 右侧模型，可编辑 |
| `/admin/users` | 用户管理 | 左侧用户 + 右侧 API Key |
| `/admin/settings` | 个人设置 | 修改密码 |

### 普通用户路由 `/user/*`

| 路径 | 页面 | 功能 |
|------|------|------|
| `/user/models` | 可用模型 | 只读表格，查看可用模型 |
| `/user/api-keys` | 我的 API Key | 管理自己的 Key |
| `/user/settings` | 个人设置 | 修改密码 |

### 公共路由

| 路径 | 页面 |
|------|------|
| `/login` | 登录页 |

### 登录后重定向

- 管理员 → `/admin/models`
- 普通用户 → `/user/models`

## 页面设计

### 0. 主布局框架

**设计参考**：Jmix B2B CRM 主界面风格

**布局**：可折叠侧边栏 + 标签页工作区

```
┌─────────────────────────────────────────────────────────────────┐
│  ☰  LLM Gateway                    🌙  🌐  👤 admin  🚪       │  ← 顶部栏
├────────────────┬────────────────────────────────────────────────┤
│                │                                                │
│  📦 模型中心    │   [模型中心 ×] [用户管理 ×] [+]                │  ← 标签页
│  👥 用户管理    │  ─────────────────────────────────────────────│
│  ⚙️ 个人设置    │                                                │
│                │                                                │
│                │                 工作区                          │
│                │              (当前标签页内容)                    │
│                │                                                │
│                │                                                │
│                │                                                │
│                │                                                │
│                │                                                │
├────────────────┴────────────────────────────────────────────────┤
│  © 2024 LLM Gateway                                             │  ← 底部栏（可选）
└─────────────────────────────────────────────────────────────────┘
```

**顶部栏元素**（从左到右）：

| 元素 | 说明 |
|------|------|
| ☰ 折叠按钮 | 展开/收起侧边栏 |
| Logo + 名称 | 品牌标识 |
| 主题切换 🌙 | 下拉菜单：跟随系统、暗色、亮色 |
| 语言切换 🌐 | 下拉菜单：简体中文、English |
| 用户头像+名称 👤 | 显示当前登录用户 |
| 退出按钮 🚪 | 退出登录 |

**主题切换**：

```typescript
type ThemeMode = 'system' | 'dark' | 'light';

// 跟随系统
const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches
  ? 'dark'
  : 'light';
```

**语言切换**：

| 选项 | 值 |
|------|------|
| 简体中文 | zh-CN |
| English | en-US |

**侧边栏**：

- 宽度：展开 200px，收起 64px
- 收起时只显示图标，悬停显示提示
- 当前页面高亮

**标签页工作区**：

- 支持多标签页同时打开
- 标签页可关闭
- 标签页超出时滚动或下拉

### 1. 登录页 `/login`

**设计参考**：Jmix B2B CRM 登录页风格

**布局**：居中卡片式表单 + 全屏背景

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│                   [背景图片/渐变色]                          │
│                                                             │
│         ┌─────────────────────────────┐                    │
│         │      🤖 LLM Gateway         │                    │
│         │                             │                    │
│         │  欢迎使用 LLM Gateway       │                    │
│         │                             │                    │
│         │  用户名                     │                    │
│         │  ┌───────────────────────┐  │                    │
│         │  │                       │  │                    │
│         │  └───────────────────────┘  │                    │
│         │                             │                    │
│         │  密码                       │                    │
│         │  ┌───────────────────────┐  │                    │
│         │  │ ••••••••              │  │                    │
│         │  └───────────────────────┘  │                    │
│         │                             │                    │
│         │  ☑ 记住我    语言: [简体中文▼]│                    │
│         │                             │                    │
│         │  ┌───────────────────────┐  │                    │
│         │  │       登  录          │  │                    │
│         │  └───────────────────────┘  │                    │
│         │                             │                    │
│         │  登录失败: 用户名或密码错误   │  ← 错误提示        │
│         │                             │                    │
│         └─────────────────────────────┘                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**视觉设计**：

| 元素 | 规格 |
|------|------|
| 背景 | 深色渐变或科技感图片，降低亮度（0.3-0.5 透明度遮罩） |
| 登录卡片 | 白色背景，圆角 8px，阴影 `0 4px 24px rgba(0,0,0,0.15)` |
| Logo | 顶部居中，品牌色，高度 48px |
| 标题 | 24px，粗体，深灰色 `#1f1f1f` |
| 输入框 | 高度 40px，圆角 4px，带边框 |
| 登录按钮 | 全宽，主色调，高度 44px，圆角 4px |
| 错误提示 | 红色背景 `#fff2f0`，红色边框，红色文字 |

**功能**：
- 用户名/密码登录
- 记住我选项（Session-Cookie 模式）
- 语言切换（简体中文/English）
- 登录失败提示（居中显示在表单内）
- 响应式布局（移动端卡片宽度自适应）

**交互**：
- 表单验证（必填、格式校验）
- 登录成功后根据角色重定向
- 登录失败显示错误提示（红色背景条）
- 回车键提交表单
- 登录按钮 loading 状态

### 2. 模型中心 `/admin/models`

**布局**：左右分栏

```
┌────────────────┬────────────────────────────────────────┐
│  渠道列表       │  模型列表                              │
│  ────────────  │  ────────────────────────────────────  │
│  ➤ OpenAI      │  模型名称 │ 类型 │ 状态 │ 操作         │
│    Anthropic   │  gpt-4   │ chat │ 启用 │ 编辑 删除    │
│    Google      │  ...     │ ...  │ ...  │ ...          │
│                │                                        │
│  [+ 新增渠道]   │  [+ 新增模型]  [启用] [禁用] [删除]    │
└────────────────┴────────────────────────────────────────┘
```

**渠道列表**：
- 表格列：名称、类型、状态
- 操作：新增、编辑、删除、启用/禁用
- 点击渠道筛选模型

**模型列表**：
- 表格列：名称、类型、状态、操作
- 操作：新增、编辑、删除、启用/禁用
- 顶部搜索筛选

### 3. 用户管理 `/admin/users`

**布局**：左右分栏

```
┌────────────────┬────────────────────────────────────────┐
│  用户列表       │  API Key 列表                          │
│  ────────────  │  ────────────────────────────────────  │
│  ➤ admin       │  Key名称 │ Key值(脱敏) │ 状态 │ 操作    │
│    operator    │  生产Key │ sk-xxx...   │ 启用 │ 复制   │
│    viewer      │  ...     │ ...         │ ...  │ ...    │
│                │                                        │
│  [+ 新增用户]   │  [+ 新增 Key]                          │
└────────────────┴────────────────────────────────────────┘
```

**用户列表**：
- 表格列：用户名、邮箱、角色、状态
- 操作：新增、编辑、删除、启用/禁用、重置密码

**API Key 列表**：
- 表格列：名称、Key 值（脱敏）、状态、创建时间
- 操作：新增、复制、删除、启用/禁用
- 创建时完整显示 Key 一次，后续脱敏

### 4. 可用模型 `/user/models`

**布局**：单表格，只读

```
┌────────────────────────────────────────────────────────┐
│  搜索: [________]  渠道: [全部 ▼]                       │
├────────────────────────────────────────────────────────┤
│  模型名称     │ 渠道    │ 类型  │ 状态                  │
│  gpt-4       │ OpenAI  │ chat  │ 启用                  │
│  claude-3    │ Anthropic│ chat │ 启用                  │
│  ...         │ ...     │ ...   │ ...                   │
└────────────────────────────────────────────────────────┘
```

**功能**：
- 查看可用模型列表
- 按渠道筛选
- 搜索模型名称

### 5. 我的 API Key `/user/api-keys`

**布局**：单表格

```
┌────────────────────────────────────────────────────────┐
│  [+ 新增 API Key]                                       │
├────────────────────────────────────────────────────────┤
│  名称     │ Key 值(脱敏) │ 状态 │ 创建时间   │ 操作     │
│  生产Key  │ sk-xxx...    │ 启用 │ 2024-01-01 │ 复制 删除│
│  测试Key  │ sk-yyy...    │ 禁用 │ 2024-01-02 │ 启用 删除│
└────────────────────────────────────────────────────────┘
```

**功能**：
- 新增 API Key（创建时显示完整 Key）
- 复制 Key
- 启用/禁用 Key
- 删除 Key

### 6. 个人设置 `/admin/settings` 和 `/user/settings`

**功能**：
- 修改密码（当前密码 + 新密码 + 确认密码）
- 显示当前用户信息（用户名、邮箱、角色）

## 前端目录结构

```
frontend/
├── src/
│   ├── main.tsx                  # 入口文件
│   ├── App.tsx                   # 根组件
│   ├── pages/
│   │   ├── Login/
│   │   │   ├── index.tsx
│   │   │   └── style.module.css
│   │   ├── admin/
│   │   │   ├── Models/
│   │   │   │   ├── index.tsx
│   │   │   │   ├── ChannelList.tsx
│   │   │   │   └── ModelList.tsx
│   │   │   ├── Users/
│   │   │   │   ├── index.tsx
│   │   │   │   ├── UserList.tsx
│   │   │   │   └── ApiKeyList.tsx
│   │   │   └── Settings/
│   │   │       └── index.tsx
│   │   └── user/
│   │       ├── Models/
│   │       │   └── index.tsx
│   │       ├── ApiKeys/
│   │       │   └── index.tsx
│   │       └── Settings/
│   │           └── index.tsx
│   ├── components/
│   │   ├── layout/
│   │   │   ├── AdminLayout.tsx
│   │   │   ├── UserLayout.tsx
│   │   │   └── Header.tsx
│   │   └── shared/
│   │       ├── ModelTable.tsx
│   │       └── ApiKeyTable.tsx
│   ├── stores/
│   │   ├── authStore.ts
│   │   └── themeStore.ts
│   ├── services/
│   │   ├── api/
│   │   │   ├── client.ts
│   │   │   ├── auth.ts
│   │   │   ├── provider.ts
│   │   │   ├── model.ts
│   │   │   ├── apiKey.ts
│   │   │   └── user.ts
│   │   └── query/
│   │       ├── useProviders.ts
│   │       ├── useModels.ts
│   │       ├── useUsers.ts
│   │       └── useApiKeys.ts
│   ├── router/
│   │   ├── index.tsx
│   │   └── guards.tsx
│   ├── types/
│   │   ├── user.ts
│   │   ├── provider.ts
│   │   ├── model.ts
│   │   └── apiKey.ts
│   ├── locales/
│   │   ├── zh-CN/
│   │   │   ├── common.json
│   │   │   ├── login.json
│   │   │   ├── models.json
│   │   │   ├── users.json
│   │   │   └── apiKeys.json
│   │   └── en-US/
│   │       ├── common.json
│   │       ├── login.json
│   │       ├── models.json
│   │       ├── users.json
│   │       └── apiKeys.json
│   ├── i18n.ts
│   └── styles/
│       ├── global.css
│       └── variables.css
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

## 后端 API 对接

已实现的 API 端点：

| 模块 | 端点 | 方法 | 说明 |
|------|------|------|------|
| 认证 | `/api/v1/auth/login` | POST | 登录 |
| 认证 | `/api/v1/auth/logout` | POST | 登出 |
| 认证 | `/api/v1/auth/me` | GET | 当前用户信息 |
| 渠道 | `/api/v1/providers` | GET/POST | 列表/创建 |
| 渠道 | `/api/v1/providers/{id}` | GET/PUT/DELETE | 详情/更新/删除 |
| 模型 | `/api/v1/models` | GET/POST | 列表/创建 |
| 模型 | `/api/v1/models/{id}` | GET/PUT/DELETE | 详情/更新/删除 |
| 用户 | `/api/v1/users` | GET/POST | 列表/创建 |
| 用户 | `/api/v1/users/{id}` | GET/PUT/DELETE | 详情/更新/删除 |
| API Key | `/api/v1/api-keys` | GET/POST | 列表/创建 |
| API Key | `/api/v1/api-keys/{id}` | GET/PUT/DELETE | 详情/更新/删除 |

## 国际化方案

使用 react-i18next 实现多语言支持。

### 支持语言

| 语言 | 代码 | 说明 |
|------|------|------|
| 简体中文 | zh-CN | 默认语言 |
| English | en-US | 英文 |

### 目录结构

```
frontend/src/
├── locales/
│   ├── zh-CN/
│   │   ├── common.json      # 通用文本
│   │   ├── login.json       # 登录页
│   │   ├── models.json      # 模型中心
│   │   ├── users.json       # 用户管理
│   │   └── apiKeys.json     # API Key 管理
│   └── en-US/
│       ├── common.json
│       ├── login.json
│       ├── models.json
│       ├── users.json
│       └── apiKeys.json
└── i18n.ts                  # i18n 配置
```

### 配置示例

```typescript
// src/i18n.ts
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import zhCNCommon from './locales/zh-CN/common.json';
import zhCNLogin from './locales/zh-CN/login.json';
import enUSCommon from './locales/en-US/common.json';
import enUSLogin from './locales/en-US/login.json';

i18n.use(initReactI18next).init({
  resources: {
    'zh-CN': { common: zhCNCommon, login: zhCNLogin },
    'en-US': { common: enUSCommon, login: enUSLogin },
  },
  lng: 'zh-CN',
  fallbackLng: 'zh-CN',
  defaultNS: 'common',
  interpolation: { escapeValue: false },
});

export default i18n;
```

### 翻译文件示例

```json
// locales/zh-CN/common.json
{
  "actions": {
    "add": "新增",
    "edit": "编辑",
    "delete": "删除",
    "save": "保存",
    "cancel": "取消",
    "search": "搜索",
    "reset": "重置"
  },
  "status": {
    "enabled": "启用",
    "disabled": "禁用"
  },
  "confirm": {
    "delete": "确定要删除吗？"
  }
}
```

```json
// locales/en-US/common.json
{
  "actions": {
    "add": "Add",
    "edit": "Edit",
    "delete": "Delete",
    "save": "Save",
    "cancel": "Cancel",
    "search": "Search",
    "reset": "Reset"
  },
  "status": {
    "enabled": "Enabled",
    "disabled": "Disabled"
  },
  "confirm": {
    "delete": "Are you sure you want to delete?"
  }
}
```

### 使用方式

```tsx
import { useTranslation } from 'react-i18next';

function ModelList() {
  const { t } = useTranslation('models');
  
  return (
    <Button>{t('actions.add')}</Button>
  );
}
```

### 语言切换

```tsx
import { useTranslation } from 'react-i18next';

function LanguageSwitcher() {
  const { i18n } = useTranslation();
  
  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng);
    localStorage.setItem('lang', lng);
  };
  
  return (
    <Select
      value={i18n.language}
      onChange={changeLanguage}
      options={[
        { value: 'zh-CN', label: '简体中文' },
        { value: 'en-US', label: 'English' },
      ]}
    />
  );
}
```

### Ant Design 国际化

```tsx
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import enUS from 'antd/locale/en_US';

function App() {
  const { i18n } = useTranslation();
  
  return (
    <ConfigProvider locale={i18n.language === 'zh-CN' ? zhCN : enUS}>
      {/* ... */}
    </ConfigProvider>
  );
}
```

## 认证方案

后端使用 Sa-Token 提供认证服务，前端通过以下方式对接：

1. **Session-Cookie 模式**：浏览器自动携带 Cookie，前端无需额外处理
2. **Token 模式**：登录后获取 Token，存储于 localStorage，请求时通过 Header 携带

前端默认使用 Session-Cookie 模式，企业版可切换为 Token 模式。

### 登录流程

```
用户输入账号密码
    │
    ▼
POST /api/v1/auth/login
    │
    ├── 成功 → 存储用户信息到 Zustand → 重定向到对应路由
    │
    └── 失败 → 显示错误提示
```

### Axios 拦截器配置

```typescript
// 请求拦截器：Token 模式下自动携带 Token
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器：处理 401 未授权
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 跳转登录页
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

## 开发环境配置

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

## 构建部署

```bash
# 开发
cd frontend && pnpm dev

# 构建
pnpm build

# 复制到 Spring Boot static 目录
cp -r dist/* ../src/main/resources/static/
```

## P1 规划（本次不实现）

- OAuth 登录（GitHub、企业微信）
- 操作审计日志
- 数据看板（调用量统计）
- 批量操作
- 高级搜索
