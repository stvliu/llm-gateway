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
| 认证 | Sa-Token | 1.39.x |

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

### 1. 登录页 `/login`

**功能**：
- 用户名/密码登录
- 记住我选项
- 登录失败提示
- 响应式布局

**交互**：
- 表单验证（必填、格式校验）
- 登录成功后根据角色重定向
- 登录失败显示错误提示

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

## 认证方案

Sa-Token 双模式支持：

1. **Session-Cookie 模式**：浏览器访问，自动携带 Cookie
2. **Token 模式**：API 调用，Header 携带 `Authorization: Bearer {token}`

前端默认使用 Session-Cookie 模式。

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
