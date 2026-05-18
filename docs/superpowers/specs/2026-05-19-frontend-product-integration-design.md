# 前端集成产品层重构设计文档

**日期**: 2026-05-19
**分支**: refactor/template-to-metadata

## 概述

将后端产品层重构的代码集成到前端，包括产品管理、团队管理、产品 API Key 管理三个模块。

## 设计决策

| 决策项 | 选择 | 原因 |
|--------|------|------|
| 产品管理入口 | 供应商详情抽屉新增"产品"Tab | 符合现有代码风格，改动最小 |
| 产品 API Key 管理 | 产品编辑 Modal 中内嵌管理 | 操作流畅，避免多层嵌套 |
| 团队管理入口 | 独立页面，菜单在"用户管理"下 | 与用户管理平级，逻辑清晰 |
| 权限控制 | 复用现有权限 | 产品复用 provider 权限，团队复用 user 权限 |

## 文件结构

```
gateway-console/src/
├── types/
│   ├── product.ts              # 产品和产品 Key 类型定义
│   └── team.ts                 # 团队类型定义
├── services/api/
│   ├── product.ts              # 产品 API 服务
│   └── team.ts                 # 团队 API 服务
├── services/query/
│   ├── useProducts.ts          # 产品 React Query hooks
│   └── useTeams.ts             # 团队 React Query hooks
├── pages/Providers/
│   ├── ProviderProductsTab.tsx     # 产品列表 Tab
│   ├── ProductFormModal.tsx        # 产品编辑 Modal（含 Key 管理）
│   └── ProviderManagementDrawer.tsx # 修改：新增产品 Tab
├── pages/Teams/
│   └── index.tsx               # 团队管理页面
├── constants/
│   ├── menuConfig.tsx          # 修改：新增团队菜单
│   └── permissions.ts          # 无需修改（复用现有权限）
├── router/
│   └── index.tsx               # 修改：新增团队路由
└── locales/
    ├── zh-CN/
    │   ├── product.json        # 产品国际化
    │   └── team.json           # 团队国际化
    └── en-US/
        ├── product.json
        └── team.json
```

## 产品管理设计

### 入口

供应商详情抽屉新增"产品"Tab，位于"体验"Tab 之后。

### ProviderProductsTab 组件

**职责**：展示供应商下的产品列表，提供 CRUD 入口

**UI 结构**：
```
┌─────────────────────────────────────────┐
│ 产品                      [+ 新增产品]   │
├─────────────────────────────────────────┤
│ ┌─────────┐ ┌─────────┐ ┌─────────┐    │
│ │ 按量计费 │ │ 订阅套餐 │ │ ...     │    │
│ │ 模型: 5 │ │ 额度: 1M│ │         │    │
│ │ [编辑]  │ │ [编辑]  │ │ [编辑]  │    │
│ └─────────┘ └─────────┘ └─────────┘    │
└─────────────────────────────────────────┘
```

**功能**：
- 卡片展示产品列表
- 产品类型标签（PAY_PER_USE / SUBSCRIPTION）
- 新增、编辑、删除产品

### ProductFormModal 组件

**职责**：产品创建/编辑，包含产品 API Key 管理

**UI 结构**：
```
┌─────────────────────────────────────────┐
│ 新增产品 / 编辑产品                      │
├─────────────────────────────────────────┤
│ 产品名称: [________________]            │
│ 产品类型: [按量计费 ▼]                  │
│                                          │
│ 模型列表:                                │
│ ┌─────────────────────────────────────┐ │
│ │ gpt-4o, gpt-4o-mini, claude-3-5...  │ │
│ └─────────────────────────────────────┘ │
│                                          │
│ 端点配置:                                │
│ ┌─────────────────────────────────────┐ │
│ │ openai: https://api.openai.com      │ │
│ │ anthropic: https://api.anthropic.com│ │
│ └─────────────────────────────────────┘ │
│                                          │
│ 额度限制: [________] (订阅产品专用)     │
│                                          │
│ ─────────────────────────────────────── │
│ API Keys:                    [+ 新增]   │
│ ┌─────────────────────────────────────┐ │
│ │ 名称        状态      创建时间  操作 │ │
│ │ default-key ● 启用   2024-01-01 [删]│ │
│ │ backup-key  ○ 禁用   2024-01-02 [删]│ │
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│              [取消] [保存]              │
└─────────────────────────────────────────┘
```

**功能**：
- 产品基本信息表单
- 模型列表（标签选择器，从供应商模型中选取）
- 端点配置（Key-Value 编辑器）
- 额度限制（仅订阅产品显示）
- 产品 API Key 内嵌表格管理

## 团队管理设计

### 入口

左侧菜单"用户管理"下新增"团队管理"子菜单，路由 `/teams`。

### Teams 页面

**职责**：团队 CRUD + 成员管理

**UI 结构**：
```
┌─────────────────────────────────────────┐
│ 团队管理                    [+ 新增团队] │
├─────────────────────────────────────────┤
│ 名称        描述        成员数  操作     │
│ ─────────────────────────────────────── │
│ 研发团队    核心研发    5      [成员][编辑][删除] │
│ 产品团队    产品经理    3      [成员][编辑][删除] │
└─────────────────────────────────────────┘
```

### 成员管理 Modal

**职责**：团队成员管理

**UI 结构**：
```
┌─────────────────────────────────────────┐
│ 研发团队 - 成员管理          [+ 添加成员]│
├─────────────────────────────────────────┤
│ 用户名      角色        加入时间  操作   │
│ ─────────────────────────────────────── │
│ zhangsan   管理员      2024-01-01 [移除]│
│ lisi       成员        2024-01-02 [移除]│
└─────────────────────────────────────────┘
├─────────────────────────────────────────┤
│              [关闭]                     │
└─────────────────────────────────────────┘
```

**添加成员**：下拉搜索用户 + 选择角色（管理员/成员）

## API 接口

### 产品 API

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/products?providerId={id}` | 获取供应商下的产品列表 |
| GET | `/api/v1/products/{id}` | 获取产品详情 |
| POST | `/api/v1/products` | 创建产品 |
| PUT | `/api/v1/products/{id}` | 更新产品 |
| DELETE | `/api/v1/products/{id}` | 删除产品 |

### 团队 API

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/teams` | 获取团队列表 |
| GET | `/api/v1/teams/{id}` | 获取团队详情 |
| POST | `/api/v1/teams` | 创建团队 |
| PUT | `/api/v1/teams/{id}` | 更新团队 |
| DELETE | `/api/v1/teams/{id}` | 删除团队 |
| POST | `/api/v1/teams/{teamId}/members` | 添加成员 |
| DELETE | `/api/v1/teams/{teamId}/members/{userId}` | 移除成员 |
| PUT | `/api/v1/teams/{teamId}/members/{userId}/role` | 修改成员角色 |

## 权限映射

| 功能 | 权限码 | 来源 |
|------|--------|------|
| 查看产品 | `provider:read` | 复用 |
| 管理产品 | `provider:write` | 复用 |
| 查看团队 | `user:read` | 复用 |
| 管理团队 | `user:write` | 复用 |

## 类型定义

### product.ts

```typescript
/** 产品类型 */
export type ProductType = 'PAY_PER_USE' | 'SUBSCRIPTION';

/** 产品状态 */
export type ProductState = 'ACTIVE' | 'DISABLED' | 'DELETED';

/** 产品 API Key 状态 */
export type ProductApiKeyState = 'ACTIVE' | 'DISABLED' | 'DELETED';

/** 产品 */
export interface Product {
  id: number;
  providerId: number;
  providerName: string;
  name: string;
  productType: ProductType;
  models: string[];
  endpoints: Record<string, string>;
  quotaLimit?: number;
  state: ProductState;
  createdAt: string;
  updatedAt: string;
}

/** 产品 API Key */
export interface ProductApiKey {
  id: number;
  productId: number;
  keyName: string;
  state: ProductApiKeyState;
  createdAt: string;
  updatedAt: string;
}

/** 创建产品请求 */
export interface CreateProductRequest {
  providerId: number;
  name: string;
  productType: ProductType;
  models: string[];
  endpoints: Record<string, string>;
  quotaLimit?: number;
}

/** 更新产品请求 */
export interface UpdateProductRequest {
  name?: string;
  productType?: ProductType;
  models?: string[];
  endpoints?: Record<string, string>;
  quotaLimit?: number;
  state?: ProductState;
}
```

### team.ts

```typescript
/** 团队角色 */
export type TeamRole = 'admin' | 'member';

/** 团队状态 */
export type TeamState = 'ACTIVE' | 'DISABLED' | 'DELETED';

/** 团队 */
export interface Team {
  id: number;
  name: string;
  description?: string;
  state: TeamState;
  createdAt: string;
  updatedAt: string;
}

/** 团队成员 */
export interface TeamMember {
  userId: number;
  username: string;
  email: string;
  role: TeamRole;
  joinedAt: string;
}

/** 创建团队请求 */
export interface CreateTeamRequest {
  name: string;
  description?: string;
}

/** 更新团队请求 */
export interface UpdateTeamRequest {
  name?: string;
  description?: string;
  state?: TeamState;
}
```

## 实现顺序

1. **类型定义** — product.ts, team.ts
2. **API 服务** — product.ts, team.ts
3. **React Query hooks** — useProducts.ts, useTeams.ts
4. **国际化** — product.json, team.json
5. **产品管理** — ProviderProductsTab, ProductFormModal, 修改 Drawer
6. **团队管理** — Teams 页面, 路由, 菜单

## 测试要点

- 产品 CRUD 操作正常
- 产品 API Key 管理正常
- 团队 CRUD 操作正常
- 成员管理（添加/移除/改角色）正常
- 权限控制正确（无权限时按钮隐藏）
- 国际化文本正确显示
