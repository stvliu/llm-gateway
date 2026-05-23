# 前端集成产品层重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将后端产品层重构（Product、Team、ProductApiKey）集成到前端，包含产品管理、团队管理、产品 API Key 管理三个模块。

**Architecture:** 产品管理嵌入供应商详情抽屉的"产品"Tab，使用 Modal 编辑；团队管理为独立页面；产品 API Key 在产品编辑 Modal 中内嵌管理。复用现有 React Query + Ant Design 模式。

**Tech Stack:** React 18 + TypeScript + Ant Design 5 + React Query v5 + React Router v6 + i18next

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| Create | `src/types/product.ts` | 产品和产品 Key 类型定义 |
| Create | `src/types/team.ts` | 团队类型定义 |
| Create | `src/services/api/product.ts` | 产品 API 服务 |
| Create | `src/services/api/team.ts` | 团队 API 服务 |
| Create | `src/services/query/useProducts.ts` | 产品 React Query hooks |
| Create | `src/services/query/useTeams.ts` | 团队 React Query hooks |
| Create | `src/locales/zh-CN/products.json` | 产品中文国际化 |
| Create | `src/locales/en-US/products.json` | 产品英文国际化 |
| Create | `src/locales/zh-CN/teams.json` | 团队中文国际化 |
| Create | `src/locales/en-US/teams.json` | 团队英文国际化 |
| Create | `src/pages/Providers/ProviderProductsTab.tsx` | 产品列表 Tab |
| Create | `src/pages/Providers/ProductFormModal.tsx` | 产品编辑 Modal（含 Key 管理） |
| Create | `src/pages/Teams/index.tsx` | 团队管理页面 |
| Create | `src/pages/Teams/TeamFormModal.tsx` | 团队编辑 Modal |
| Create | `src/pages/Teams/TeamMemberModal.tsx` | 成员管理 Modal |
| Modify | `src/i18n.ts` | 注册产品和团队国际化资源 |
| Modify | `src/constants/menuConfig.tsx` | 新增团队菜单 |
| Modify | `src/router/index.tsx` | 新增团队路由 |
| Modify | `src/pages/Providers/ProviderManagementDrawer.tsx` | 新增产品 Tab |

---

## 后端 API 接口（已实现）

### 产品

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/products?providerId={id}` | 获取供应商下的产品列表 |
| GET | `/api/v1/products/{id}` | 获取产品详情 |
| POST | `/api/v1/products` | 创建产品 |
| PUT | `/api/v1/products/{id}` | 更新产品 |
| DELETE | `/api/v1/products/{id}` | 删除产品 |

### 团队

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/teams` | 获取团队列表 |
| POST | `/api/v1/teams` | 创建团队 |
| PUT | `/api/v1/teams/{id}` | 更新团队 |
| DELETE | `/api/v1/teams/{id}` | 删除团队 |
| POST | `/api/v1/teams/{teamId}/members` | 添加成员 |
| DELETE | `/api/v1/teams/{teamId}/members/{userId}` | 移除成员 |
| PUT | `/api/v1/teams/{teamId}/members/{userId}/role` | 修改成员角色 |

**注意**：后端 ProductApiKey 尚无独立 Controller，产品 API Key 管理暂在前端预留 UI，待后端接口就绪后对接。

---

### Task 1: 产品类型定义

**Files:**
- Create: `gateway-console/src/types/product.ts`

- [ ] **Step 1: 创建产品类型文件**

```typescript
import type { PageResult } from './api';

/** 产品类型 */
export type ProductType = 'pay_as_you_go' | 'subscription';

/** 产品状态 */
export type ProductState = 'active' | 'inactive' | 'deleted';

/** 产品 API Key 状态 */
export type ProductApiKeyState = 'active' | 'inactive' | 'deleted';

/** 产品 */
export interface Product {
  id: number;
  providerId: number;
  name: string;
  productType: ProductType;
  models: string[];
  endpoints: Record<string, string>;
  quotaLimit: number | null;
  state: ProductState;
  createdAt: string;
  updatedAt: string;
}

/** 产品 API Key */
export interface ProductApiKey {
  id: number;
  productId: number;
  name: string;
  apiKeyPrefix: string;
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

/** 产品分页结果 */
export type ProductPageResult = PageResult<Product>;
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/types/product.ts
git commit -m "feat(console): 添加产品类型定义"
```

---

### Task 2: 团队类型定义

**Files:**
- Create: `gateway-console/src/types/team.ts`

- [ ] **Step 1: 创建团队类型文件**

根据后端 `TeamResponse` 和 `TeamMemberResponse` 的实际字段：

```typescript
import type { PageResult } from './api';

/** 团队角色 */
export type TeamRole = 'owner' | 'admin' | 'member';

/** 团队状态 */
export type TeamState = 'active' | 'inactive' | 'deleted';

/** 团队成员（后端 TeamMemberResponse） */
export interface TeamMember {
  userId: number;
  role: TeamRole;
}

/** 团队（后端 TeamResponse） */
export interface Team {
  id: number;
  name: string;
  description: string;
  state: TeamState;
  members: TeamMember[];
  createdAt: string;
  updatedAt: string;
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

/** 添加成员请求 */
export interface AddTeamMemberRequest {
  userId: number;
  role: TeamRole;
}

/** 修改成员角色请求 */
export interface UpdateMemberRoleRequest {
  role: TeamRole;
}

/** 团队分页结果 */
export type TeamPageResult = PageResult<Team>;
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/types/team.ts
git commit -m "feat(console): 添加团队类型定义"
```

---

### Task 3: 产品 API 服务

**Files:**
- Create: `gateway-console/src/services/api/product.ts`

- [ ] **Step 1: 创建产品 API 服务**

遵循现有 `services/api/provider.ts` 的模式，使用 `client` 实例：

```typescript
import client from './client';
import type {
  Product,
  CreateProductRequest,
  UpdateProductRequest,
  ProductPageResult,
} from '@/types/product';

const BASE_URL = '/api/v1/products';

/** 获取供应商下的产品列表 */
export async function getProducts(providerId: number): Promise<Product[]> {
  const { data } = await client.get<Product[]>(BASE_URL, {
    params: { providerId },
  });
  return data;
}

/** 获取产品详情 */
export async function getProduct(id: number): Promise<Product> {
  const { data } = await client.get<Product>(`${BASE_URL}/${id}`);
  return data;
}

/** 创建产品 */
export async function createProduct(req: CreateProductRequest): Promise<Product> {
  const { data } = await client.post<Product>(BASE_URL, req);
  return data;
}

/** 更新产品 */
export async function updateProduct(id: number, req: UpdateProductRequest): Promise<Product> {
  const { data } = await client.put<Product>(`${BASE_URL}/${id}`, req);
  return data;
}

/** 删除产品 */
export async function deleteProduct(id: number): Promise<void> {
  await client.delete(`${BASE_URL}/${id}`);
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/services/api/product.ts
git commit -m "feat(console): 添加产品 API 服务"
```

---

### Task 4: 团队 API 服务

**Files:**
- Create: `gateway-console/src/services/api/team.ts`

- [ ] **Step 1: 创建团队 API 服务**

```typescript
import client from './client';
import type {
  Team,
  CreateTeamRequest,
  UpdateTeamRequest,
  AddTeamMemberRequest,
  UpdateMemberRoleRequest,
} from '@/types/team';

const BASE_URL = '/api/v1/teams';

/** 获取团队列表 */
export async function getTeams(): Promise<Team[]> {
  const { data } = await client.get<Team[]>(BASE_URL);
  return data;
}

/** 创建团队 */
export async function createTeam(req: CreateTeamRequest): Promise<Team> {
  const { data } = await client.post<Team>(BASE_URL, req);
  return data;
}

/** 更新团队 */
export async function updateTeam(id: number, req: UpdateTeamRequest): Promise<Team> {
  const { data } = await client.put<Team>(`${BASE_URL}/${id}`, req);
  return data;
}

/** 删除团队 */
export async function deleteTeam(id: number): Promise<void> {
  await client.delete(`${BASE_URL}/${id}`);
}

/** 添加成员 */
export async function addTeamMember(teamId: number, req: AddTeamMemberRequest): Promise<void> {
  await client.post(`${BASE_URL}/${teamId}/members`, req);
}

/** 移除成员 */
export async function removeTeamMember(teamId: number, userId: number): Promise<void> {
  await client.delete(`${BASE_URL}/${teamId}/members/${userId}`);
}

/** 修改成员角色 */
export async function updateMemberRole(
  teamId: number,
  userId: number,
  req: UpdateMemberRoleRequest,
): Promise<void> {
  await client.put(`${BASE_URL}/${teamId}/members/${userId}/role`, req);
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/services/api/team.ts
git commit -m "feat(console): 添加团队 API 服务"
```

---

### Task 5: 产品 React Query Hooks

**Files:**
- Create: `gateway-console/src/services/query/useProducts.ts`

- [ ] **Step 1: 创建产品 React Query hooks**

遵循现有 `useProviders.ts` 的模式：

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as productApi from '@/services/api/product';
import type { CreateProductRequest, UpdateProductRequest } from '@/types/product';

/** 产品列表 */
export function useProducts(providerId: number) {
  return useQuery({
    queryKey: ['products', providerId],
    queryFn: () => productApi.getProducts(providerId),
    enabled: !!providerId,
  });
}

/** 产品详情 */
export function useProduct(id: number) {
  return useQuery({
    queryKey: ['product', id],
    queryFn: () => productApi.getProduct(id),
    enabled: !!id,
  });
}

/** 创建产品 */
export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: CreateProductRequest) => productApi.createProduct(req),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['products', variables.providerId] });
    },
  });
}

/** 更新产品 */
export function useUpdateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, req }: { id: number; req: UpdateProductRequest }) =>
      productApi.updateProduct(id, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
  });
}

/** 删除产品 */
export function useDeleteProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: number; providerId: number }) =>
      productApi.deleteProduct(id),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['products', variables.providerId] });
    },
  });
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/services/query/useProducts.ts
git commit -m "feat(console): 添加产品 React Query hooks"
```

---

### Task 6: 团队 React Query Hooks

**Files:**
- Create: `gateway-console/src/services/query/useTeams.ts`

- [ ] **Step 1: 创建团队 React Query hooks**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as teamApi from '@/services/api/team';
import type { CreateTeamRequest, UpdateTeamRequest, AddTeamMemberRequest, UpdateMemberRoleRequest } from '@/types/team';

/** 团队列表 */
export function useTeams() {
  return useQuery({
    queryKey: ['teams'],
    queryFn: () => teamApi.getTeams(),
  });
}

/** 创建团队 */
export function useCreateTeam() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: CreateTeamRequest) => teamApi.createTeam(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
    },
  });
}

/** 更新团队 */
export function useUpdateTeam() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, req }: { id: number; req: UpdateTeamRequest }) =>
      teamApi.updateTeam(id, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
    },
  });
}

/** 删除团队 */
export function useDeleteTeam() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => teamApi.deleteTeam(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
    },
  });
}

/** 添加成员 */
export function useAddTeamMember() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, req }: { teamId: number; req: AddTeamMemberRequest }) =>
      teamApi.addTeamMember(teamId, req),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      queryClient.invalidateQueries({ queryKey: ['team', variables.teamId] });
    },
  });
}

/** 移除成员 */
export function useRemoveTeamMember() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, userId }: { teamId: number; userId: number }) =>
      teamApi.removeTeamMember(teamId, userId),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      queryClient.invalidateQueries({ queryKey: ['team', variables.teamId] });
    },
  });
}

/** 修改成员角色 */
export function useUpdateMemberRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ teamId, userId, req }: { teamId: number; userId: number; req: UpdateMemberRoleRequest }) =>
      teamApi.updateMemberRole(teamId, userId, req),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['teams'] });
      queryClient.invalidateQueries({ queryKey: ['team', variables.teamId] });
    },
  });
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/services/query/useTeams.ts
git commit -m "feat(console): 添加团队 React Query hooks"
```

---

### Task 7: 产品国际化

**Files:**
- Create: `gateway-console/src/locales/zh-CN/products.json`
- Create: `gateway-console/src/locales/en-US/products.json`
- Modify: `gateway-console/src/i18n.ts`

- [ ] **Step 1: 创建中文国际化文件**

```json
{
  "product": {
    "title": "产品",
    "name": "产品名称",
    "type": "产品类型",
    "typePayAsYouGo": "按量计费",
    "typeSubscription": "订阅套餐",
    "models": "模型列表",
    "endpoints": "端点配置",
    "quotaLimit": "额度限制",
    "state": "状态",
    "stateActive": "启用",
    "stateInactive": "禁用",
    "apiKey": "API Key",
    "apiKeyName": "Key 名称",
    "apiKeyPrefix": "Key 前缀",
    "apiKeyState": "Key 状态",
    "addProduct": "新增产品",
    "editProduct": "编辑产品",
    "deleteProduct": "删除产品",
    "deleteConfirm": "确定要删除产品「{{name}}」吗？",
    "addApiKey": "新增 Key",
    "deleteApiKey": "删除 Key",
    "deleteApiKeyConfirm": "确定要删除 Key「{{name}}」吗？",
    "lastProductWarning": "供应商下至少保留一个产品，无法删除",
    "lastApiKeyWarning": "产品下至少保留一个 API Key，无法删除",
    "endpointRequired": "至少配置一个端点",
    "apiKeyRequired": "至少添加一个 API Key",
    "nameRequired": "请输入产品名称",
    "typeRequired": "请选择产品类型"
  }
}
```

- [ ] **Step 2: 创建英文国际化文件**

```json
{
  "product": {
    "title": "Product",
    "name": "Product Name",
    "type": "Product Type",
    "typePayAsYouGo": "Pay As You Go",
    "typeSubscription": "Subscription",
    "models": "Models",
    "endpoints": "Endpoints",
    "quotaLimit": "Quota Limit",
    "state": "State",
    "stateActive": "Active",
    "stateInactive": "Inactive",
    "apiKey": "API Key",
    "apiKeyName": "Key Name",
    "apiKeyPrefix": "Key Prefix",
    "apiKeyState": "Key State",
    "addProduct": "Add Product",
    "editProduct": "Edit Product",
    "deleteProduct": "Delete Product",
    "deleteConfirm": "Are you sure you want to delete product \"{{name}}\"?",
    "addApiKey": "Add Key",
    "deleteApiKey": "Delete Key",
    "deleteApiKeyConfirm": "Are you sure you want to delete key \"{{name}}\"?",
    "lastProductWarning": "At least one product is required per provider",
    "lastApiKeyWarning": "At least one API Key is required per product",
    "endpointRequired": "At least one endpoint is required",
    "apiKeyRequired": "At least one API Key is required",
    "nameRequired": "Please enter product name",
    "typeRequired": "Please select product type"
  }
}
```

- [ ] **Step 3: 注册国际化资源到 i18n.ts**

在 `i18n.ts` 顶部添加静态 import：

```typescript
import zhCNProducts from './locales/zh-CN/products.json';
import enUSProducts from './locales/en-US/products.json';
```

在 `resources` 对象的 `zh-CN` 中添加：
```typescript
products: zhCNProducts,
```

在 `en-US` 中添加：
```typescript
products: enUSProducts,
```

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/locales/zh-CN/products.json gateway-console/src/locales/en-US/products.json gateway-console/src/i18n.ts
git commit -m "feat(console): 添加产品国际化资源"
```

---

### Task 8: 团队国际化

**Files:**
- Create: `gateway-console/src/locales/zh-CN/teams.json`
- Create: `gateway-console/src/locales/en-US/teams.json`
- Modify: `gateway-console/src/i18n.ts`

- [ ] **Step 1: 创建中文国际化文件**

```json
{
  "team": {
    "title": "团队管理",
    "name": "团队名称",
    "description": "描述",
    "state": "状态",
    "stateActive": "启用",
    "stateInactive": "禁用",
    "members": "成员",
    "memberCount": "成员数",
    "role": "角色",
    "roleAdmin": "管理员",
    "roleMember": "成员",
    "addTeam": "新增团队",
    "editTeam": "编辑团队",
    "deleteTeam": "删除团队",
    "deleteConfirm": "确定要删除团队「{{name}}」吗？",
    "manageMembers": "成员管理",
    "addMember": "添加成员",
    "removeMember": "移除",
    "removeMemberConfirm": "确定要将该成员从团队中移除吗？",
    "selectUser": "选择用户",
    "selectRole": "选择角色",
    "nameRequired": "请输入团队名称"
  }
}
```

- [ ] **Step 2: 创建英文国际化文件**

```json
{
  "team": {
    "title": "Team Management",
    "name": "Team Name",
    "description": "Description",
    "state": "State",
    "stateActive": "Active",
    "stateInactive": "Inactive",
    "members": "Members",
    "memberCount": "Members",
    "role": "Role",
    "roleAdmin": "Admin",
    "roleMember": "Member",
    "addTeam": "Add Team",
    "editTeam": "Edit Team",
    "deleteTeam": "Delete Team",
    "deleteConfirm": "Are you sure you want to delete team \"{{name}}\"?",
    "manageMembers": "Manage Members",
    "addMember": "Add Member",
    "removeMember": "Remove",
    "removeMemberConfirm": "Are you sure you want to remove this member from the team?",
    "selectUser": "Select User",
    "selectRole": "Select Role",
    "nameRequired": "Please enter team name"
  }
}
```

- [ ] **Step 3: 注册国际化资源到 i18n.ts**

在 `i18n.ts` 顶部添加静态 import：

```typescript
import zhCNTeams from './locales/zh-CN/teams.json';
import enUSTeams from './locales/en-US/teams.json';
```

在 `resources` 对象的 `zh-CN` 中添加：
```typescript
teams: zhCNTeams,
```

在 `en-US` 中添加：
```typescript
teams: enUSTeams,
```

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/locales/zh-CN/teams.json gateway-console/src/locales/en-US/teams.json gateway-console/src/i18n.ts
git commit -m "feat(console): 添加团队国际化资源"
```

---

### Task 9: 产品列表 Tab

**Files:**
- Create: `gateway-console/src/pages/Providers/ProviderProductsTab.tsx`

- [ ] **Step 1: 创建产品列表 Tab 组件**

遵循现有 `ProviderApiKeysTab.tsx` 的模式，使用 Ant Design Card + List 展示产品：

```tsx
import { useState } from 'react';
import { Card, Button, Tag, Space, Empty, Spin, App } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProducts, useDeleteProduct } from '@/services/query/useProducts';
import { useHasPermission } from '@/hooks/usePermission';
import type { Product } from '@/types/product';
import ProductFormModal from './ProductFormModal';

interface ProviderProductsTabProps {
  providerId: number;
}

const PRODUCT_TYPE_COLOR: Record<string, string> = {
  pay_as_you_go: 'blue',
  subscription: 'green',
};

export default function ProviderProductsTab({ providerId }: ProviderProductsTabProps) {
  const { t } = useTranslation('products');
  const { message } = App.useApp();
  const { data: products, isLoading } = useProducts(providerId);
  const deleteMutation = useDeleteProduct();
  const canWrite = useHasPermission('provider:write');

  const [formVisible, setFormVisible] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | undefined>();

  const handleAdd = () => {
    setEditingProduct(undefined);
    setFormVisible(true);
  };

  const handleEdit = (product: Product) => {
    setEditingProduct(product);
    setFormVisible(true);
  };

  const handleDelete = (product: Product) => {
    if (products && products.length <= 1) {
      message.warning(t('product.lastProductWarning'));
      return;
    }
    deleteMutation.mutate(
      { id: product.id, providerId },
      {
        onSuccess: () => message.success(t('product.deleteProduct')),
        onError: () => message.error(t('product.deleteProduct')),
      },
    );
  };

  if (isLoading) return <Spin />;

  return (
    <>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('product.addProduct')}
          </Button>
        )}
      </div>

      {!products?.length ? (
        <Empty description={t('product.title')} />
      ) : (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          {products.map((product) => (
            <Card
              key={product.id}
              size="small"
              title={
                <Space>
                  {product.name}
                  <Tag color={PRODUCT_TYPE_COLOR[product.productType]}>
                    {t(`product.type${product.productType === 'pay_as_you_go' ? 'PayAsYouGo' : 'TypeSubscription'}`)}
                  </Tag>
                </Space>
              }
              extra={
                canWrite && (
                  <Space>
                    <Button
                      type="text"
                      size="small"
                      icon={<EditOutlined />}
                      onClick={() => handleEdit(product)}
                    />
                    <Button
                      type="text"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => handleDelete(product)}
                    />
                  </Space>
                )
              }
            >
              <p>模型: {product.models?.join(', ') || '-'}</p>
              <p>
                端点:{' '}
                {Object.entries(product.endpoints || {})
                  .map(([k, v]) => `${k}: ${v}`)
                  .join(', ') || '-'}
              </p>
              {product.quotaLimit != null && <p>额度: {product.quotaLimit}</p>}
            </Card>
          ))}
        </Space>
      )}

      <ProductFormModal
        visible={formVisible}
        providerId={providerId}
        product={editingProduct}
        onClose={() => setFormVisible(false)}
      />
    </>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Providers/ProviderProductsTab.tsx
git commit -m "feat(console): 添加产品列表 Tab 组件"
```

---

### Task 10: 产品编辑 Modal

**Files:**
- Create: `gateway-console/src/pages/Providers/ProductFormModal.tsx`

- [ ] **Step 1: 创建产品编辑 Modal 组件**

包含产品基本信息表单 + API Key 内嵌表格。遵循现有 `ProviderCreateModal.tsx` 的 Modal + Form 模式：

```tsx
import { useEffect } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Button,
  Table,
  Space,
  Tag,
  App,
  InputNumber,
} from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useCreateProduct, useUpdateProduct } from '@/services/query/useProducts';
import type { Product, ProductType, CreateProductRequest } from '@/types/product';

interface ProductFormModalProps {
  visible: boolean;
  providerId: number;
  product?: Product;
  onClose: () => void;
}

interface EndpointEntry {
  key: string;
  protocol: string;
  url: string;
}

interface ApiKeyEntry {
  key: string;
  name: string;
}

export default function ProductFormModal({ visible, providerId, product, onClose }: ProductFormModalProps) {
  const { t } = useTranslation('products');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const createMutation = useCreateProduct();
  const updateMutation = useUpdateProduct();
  const isEdit = !!product;

  useEffect(() => {
    if (visible) {
      if (product) {
        const endpoints: EndpointEntry[] = Object.entries(product.endpoints || {}).map(
          ([protocol, url], idx) => ({ key: String(idx), protocol, url }),
        );
        form.setFieldsValue({
          name: product.name,
          productType: product.productType,
          models: product.models,
          endpoints,
          quotaLimit: product.quotaLimit,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          endpoints: [{ key: '0', protocol: 'openai', url: '' }],
        });
      }
    }
  }, [visible, product, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const endpoints: Record<string, string> = {};
      (values.endpoints || []).forEach((e: EndpointEntry) => {
        if (e.protocol && e.url) endpoints[e.protocol] = e.url;
      });

      if (Object.keys(endpoints).length === 0) {
        message.warning(t('product.endpointRequired'));
        return;
      }

      if (isEdit) {
        await updateMutation.mutateAsync({
          id: product!.id,
          req: {
            name: values.name,
            productType: values.productType,
            models: values.models,
            endpoints,
            quotaLimit: values.quotaLimit,
          },
        });
        message.success(t('product.editProduct'));
      } else {
        await createMutation.mutateAsync({
          providerId,
          name: values.name,
          productType: values.productType,
          models: values.models,
          endpoints,
          quotaLimit: values.quotaLimit,
        });
        message.success(t('product.addProduct'));
      }
      onClose();
    } catch {
      // 表单验证失败
    }
  };

  return (
    <Modal
      title={isEdit ? t('product.editProduct') : t('product.addProduct')}
      open={visible}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={createMutation.isPending || updateMutation.isPending}
      width={640}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label={t('product.name')}
          rules={[{ required: true, message: t('product.nameRequired') }]}
        >
          <Input />
        </Form.Item>

        <Form.Item
          name="productType"
          label={t('product.type')}
          rules={[{ required: true, message: t('product.typeRequired') }]}
        >
          <Select>
            <Select.Option value="pay_as_you_go">{t('product.typePayAsYouGo')}</Select.Option>
            <Select.Option value="subscription">{t('product.typeSubscription')}</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item name="models" label={t('product.models')}>
          <Select mode="tags" placeholder="gpt-4o, claude-3-5-sonnet" />
        </Form.Item>

        <Form.Item label={t('product.endpoints')} required>
          <Form.List name="endpoints">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...restField }) => (
                  <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    <Form.Item
                      {...restField}
                      name={[name, 'protocol']}
                      rules={[{ required: true, message: 'Protocol' }]}
                    >
                      <Input placeholder="openai" style={{ width: 120 }} />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'url']}
                      rules={[{ required: true, message: 'URL' }]}
                    >
                      <Input placeholder="https://api.openai.com" style={{ width: 300 }} />
                    </Form.Item>
                    {fields.length > 1 && (
                      <DeleteOutlined onClick={() => remove(name)} />
                    )}
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  添加端点
                </Button>
              </>
            )}
          </Form.List>
        </Form.Item>

        <Form.Item name="quotaLimit" label={t('product.quotaLimit')}>
          <InputNumber style={{ width: '100%' }} min={0} />
        </Form.Item>
      </Form>

      {/* 产品 API Key 管理 — 编辑模式下展示已有 Key */}
      {isEdit && product && (
        <div style={{ marginTop: 16 }}>
          <h4>{t('product.apiKey')}</h4>
          <p style={{ color: '#999', fontSize: 12 }}>
            API Key 管理功能待后端接口就绪后开放
          </p>
        </div>
      )}
    </Modal>
  );
}
```

**注意**：产品 API Key 的内嵌表格管理暂用占位提示，因为后端 ProductApiKey 尚无独立 Controller。待后端接口就绪后，在此区域添加 Key 的 CRUD 表格。

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Providers/ProductFormModal.tsx
git commit -m "feat(console): 添加产品编辑 Modal 组件"
```

---

### Task 11: 供应商详情抽屉集成产品 Tab

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx`

- [ ] **Step 1: 在 ProviderManagementDrawer 中添加产品 Tab**

找到现有的 Tabs 组件（包含"基础信息"、"模型"、"API Key"、"体验"等 Tab 项），在最后一个 Tab 之后新增"产品"Tab：

1. 在文件顶部添加 import：
```typescript
import ProviderProductsTab from './ProviderProductsTab';
```

2. 在 Tabs 的 `items` 数组中追加：
```typescript
{
  key: 'products',
  label: t('product.title'),
  children: <ProviderProductsTab providerId={provider.id} />,
},
```

3. 确认 `useTranslation` 中已包含 `'products'` 命名空间，如未包含则添加。

- [ ] **Step 2: 验证**

在浏览器中打开供应商详情抽屉，确认"产品"Tab 出现且可切换。

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx
git commit -m "feat(console): 供应商详情抽屉集成产品 Tab"
```

---

### Task 12: 团队管理页面

**Files:**
- Create: `gateway-console/src/pages/Teams/index.tsx`

- [ ] **Step 1: 创建团队管理页面**

遵循现有 `Users/index.tsx` 的页面模式（Table + 搜索 + 操作按钮）：

```tsx
import { useState } from 'react';
import { Table, Button, Space, Tag, App, Popconfirm, Input } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTeams, useDeleteTeam } from '@/services/query/useTeams';
import { useHasPermission } from '@/hooks/usePermission';
import type { Team } from '@/types/team';
import TeamFormModal from './TeamFormModal';
import TeamMemberModal from './TeamMemberModal';

const STATE_COLOR: Record<string, string> = {
  active: 'green',
  inactive: 'default',
};

export default function TeamsPage() {
  const { t } = useTranslation('teams');
  const { message } = App.useApp();
  const { data: teams, isLoading } = useTeams();
  const deleteMutation = useDeleteTeam();
  const canWrite = useHasPermission('user:write');

  const [formVisible, setFormVisible] = useState(false);
  const [editingTeam, setEditingTeam] = useState<Team | undefined>();
  const [memberTeam, setMemberTeam] = useState<Team | undefined>();
  const [searchText, setSearchText] = useState('');

  const handleAdd = () => {
    setEditingTeam(undefined);
    setFormVisible(true);
  };

  const handleEdit = (team: Team) => {
    setEditingTeam(team);
    setFormVisible(true);
  };

  const handleDelete = (id: number) => {
    deleteMutation.mutate(id, {
      onSuccess: () => message.success(t('team.deleteTeam')),
      onError: () => message.error(t('team.deleteTeam')),
    });
  };

  const handleMembers = (team: Team) => {
    setMemberTeam(team);
  };

  const filteredTeams = teams?.filter(
    (t) =>
      t.name.toLowerCase().includes(searchText.toLowerCase()) ||
      t.description?.toLowerCase().includes(searchText.toLowerCase()),
  );

  const columns = [
    {
      title: t('team.name'),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('team.description'),
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: t('team.memberCount'),
      key: 'memberCount',
      render: (_: unknown, record: Team) => record.members?.length ?? 0,
    },
    {
      title: t('team.state'),
      dataIndex: 'state',
      key: 'state',
      render: (state: string) => (
        <Tag color={STATE_COLOR[state]}>{t(`team.state${state.charAt(0).toUpperCase() + state.slice(1)}`)}</Tag>
      ),
    },
    ...(canWrite
      ? [
          {
            title: '操作',
            key: 'action',
            render: (_: unknown, record: Team) => (
              <Space>
                <Button type="link" size="small" onClick={() => handleMembers(record)}>
                  {t('team.manageMembers')}
                </Button>
                <Button type="link" size="small" onClick={() => handleEdit(record)}>
                  {t('team.editTeam')}
                </Button>
                <Popconfirm
                  title={t('team.deleteConfirm', { name: record.name })}
                  onConfirm={() => handleDelete(record.id)}
                >
                  <Button type="link" size="small" danger>
                    {t('team.deleteTeam')}
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Input.Search
          placeholder="搜索团队"
          style={{ width: 300 }}
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          allowClear
        />
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('team.addTeam')}
          </Button>
        )}
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={filteredTeams}
        loading={isLoading}
        pagination={{ pageSize: 20 }}
      />

      <TeamFormModal
        visible={formVisible}
        team={editingTeam}
        onClose={() => setFormVisible(false)}
      />

      <TeamMemberModal
        visible={!!memberTeam}
        team={memberTeam}
        onClose={() => setMemberTeam(undefined)}
      />
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Teams/index.tsx
git commit -m "feat(console): 添加团队管理页面"
```

---

### Task 13: 团队编辑 Modal

**Files:**
- Create: `gateway-console/src/pages/Teams/TeamFormModal.tsx`

- [ ] **Step 1: 创建团队编辑 Modal 组件**

遵循现有 `UserFormModal.tsx` 的模式：

```tsx
import { useEffect } from 'react';
import { Modal, Form, Input, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateTeam, useUpdateTeam } from '@/services/query/useTeams';
import type { Team } from '@/types/team';

interface TeamFormModalProps {
  visible: boolean;
  team?: Team;
  onClose: () => void;
}

export default function TeamFormModal({ visible, team, onClose }: TeamFormModalProps) {
  const { t } = useTranslation('teams');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const createMutation = useCreateTeam();
  const updateMutation = useUpdateTeam();
  const isEdit = !!team;

  useEffect(() => {
    if (visible) {
      if (team) {
        form.setFieldsValue({ name: team.name, description: team.description });
      } else {
        form.resetFields();
      }
    }
  }, [visible, team, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (isEdit) {
        await updateMutation.mutateAsync({ id: team!.id, req: values });
        message.success(t('team.editTeam'));
      } else {
        await createMutation.mutateAsync(values);
        message.success(t('team.addTeam'));
      }
      onClose();
    } catch {
      // 表单验证失败
    }
  };

  return (
    <Modal
      title={isEdit ? t('team.editTeam') : t('team.addTeam')}
      open={visible}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={createMutation.isPending || updateMutation.isPending}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label={t('team.name')}
          rules={[{ required: true, message: t('team.nameRequired') }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="description" label={t('team.description')}>
          <Input.TextArea rows={3} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Teams/TeamFormModal.tsx
git commit -m "feat(console): 添加团队编辑 Modal 组件"
```

---

### Task 14: 成员管理 Modal

**Files:**
- Create: `gateway-console/src/pages/Teams/TeamMemberModal.tsx`

- [ ] **Step 1: 创建成员管理 Modal 组件**

```tsx
import { useState } from 'react';
import { Modal, Table, Button, Select, Space, Tag, App, Popconfirm } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAddTeamMember, useRemoveTeamMember, useUpdateMemberRole } from '@/services/query/useTeams';
import { useUsers } from '@/services/query/useUsers';
import type { Team, TeamRole } from '@/types/team';

const ROLE_COLOR: Record<string, string> = {
  admin: 'red',
  member: 'blue',
};

interface TeamMemberModalProps {
  visible: boolean;
  team?: Team;
  onClose: () => void;
}

export default function TeamMemberModal({ visible, team, onClose }: TeamMemberModalProps) {
  const { t } = useTranslation('teams');
  const { message } = App.useApp();
  const addMemberMutation = useAddTeamMember();
  const removeMemberMutation = useRemoveTeamMember();
  const updateRoleMutation = useUpdateMemberRole();
  const { data: users } = useUsers();

  const [adding, setAdding] = useState(false);
  const [selectedUser, setSelectedUser] = useState<number | undefined>();
  const [selectedRole, setSelectedRole] = useState<TeamRole>('member');

  const handleAddMember = async () => {
    if (!team || !selectedUser) return;
    try {
      await addMemberMutation.mutateAsync({
        teamId: team.id,
        req: { userId: selectedUser, role: selectedRole },
      });
      message.success(t('team.addMember'));
      setAdding(false);
      setSelectedUser(undefined);
      setSelectedRole('member');
    } catch {
      message.error(t('team.addMember'));
    }
  };

  const handleRemove = async (userId: number) => {
    if (!team) return;
    await removeMemberMutation.mutateAsync({ teamId: team.id, userId });
  };

  const handleRoleChange = async (userId: number, role: TeamRole) => {
    if (!team) return;
    await updateRoleMutation.mutateAsync({ teamId: team.id, userId, req: { role } });
  };

  const memberIds = new Set(team?.members?.map((m) => m.userId) ?? []);
  const availableUsers = users?.filter((u) => !memberIds.has(u.id)) ?? [];

  const columns = [
    {
      title: 'ID',
      dataIndex: 'userId',
      key: 'userId',
    },
    {
      title: t('team.role'),
      dataIndex: 'role',
      key: 'role',
      render: (role: TeamRole, record: { userId: number }) => (
        <Select
          value={role}
          size="small"
          style={{ width: 100 }}
          onChange={(val: TeamRole) => handleRoleChange(record.userId, val)}
          options={[
            { value: 'admin', label: t('team.roleAdmin') },
            { value: 'member', label: t('team.roleMember') },
          ]}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: { userId: number }) => (
        <Popconfirm
          title={t('team.removeMemberConfirm')}
          onConfirm={() => handleRemove(record.userId)}
        >
          <Button type="link" size="small" danger>
            {t('team.removeMember')}
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <Modal
      title={`${team?.name ?? ''} - ${t('team.manageMembers')}`}
      open={visible}
      onCancel={onClose}
      footer={null}
      width={600}
      destroyOnClose
    >
      <div style={{ marginBottom: 16 }}>
        {adding ? (
          <Space>
            <Select
              style={{ width: 200 }}
              placeholder={t('team.selectUser')}
              value={selectedUser}
              onChange={setSelectedUser}
              options={availableUsers.map((u) => ({ value: u.id, label: u.username }))}
              showSearch
              optionFilterProp="label"
            />
            <Select
              style={{ width: 120 }}
              value={selectedRole}
              onChange={setSelectedRole}
              options={[
                { value: 'admin', label: t('team.roleAdmin') },
                { value: 'member', label: t('team.roleMember') },
              ]}
            />
            <Button type="primary" onClick={handleAddMember} disabled={!selectedUser}>
              确定
            </Button>
            <Button onClick={() => setAdding(false)}>取消</Button>
          </Space>
        ) : (
          <Button type="dashed" icon={<PlusOutlined />} onClick={() => setAdding(true)}>
            {t('team.addMember')}
          </Button>
        )}
      </div>

      <Table
        rowKey="userId"
        columns={columns}
        dataSource={team?.members ?? []}
        pagination={false}
        size="small"
      />
    </Modal>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Teams/TeamMemberModal.tsx
git commit -m "feat(console): 添加成员管理 Modal 组件"
```

---

### Task 15: 团队路由和菜单

**Files:**
- Modify: `gateway-console/src/router/index.tsx`
- Modify: `gateway-console/src/constants/menuConfig.tsx`

- [ ] **Step 1: 添加团队路由**

在 `router/index.tsx` 中，找到用户管理相关的路由配置，添加团队路由：

```typescript
import TeamsPage from '@/pages/Teams';

// 在用户管理路由同级或子级位置添加：
{
  path: '/teams',
  element: <TeamsPage />,
}
```

具体位置参考现有路由结构，确保与用户管理路由平级。

- [ ] **Step 2: 添加团队菜单**

在 `menuConfig.tsx` 中，找到"用户管理"菜单项，在其下新增"团队管理"子菜单：

```typescript
{
  key: '/teams',
  label: t('team.title'),
  icon: <TeamOutlined />,
  permission: 'user:read',
}
```

确保 `TeamOutlined` 图标已从 `@ant-design/icons` 导入。如未导入，添加：
```typescript
import { TeamOutlined } from '@ant-design/icons';
```

- [ ] **Step 3: 验证**

启动开发服务器，确认左侧菜单出现"团队管理"入口，点击可跳转到团队页面。

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/router/index.tsx gateway-console/src/constants/menuConfig.tsx
git commit -m "feat(console): 添加团队路由和菜单"
```

---

### Task 16: 端到端验证

**Files:** 无新增

- [ ] **Step 1: 启动前端开发服务器**

```bash
cd gateway-console && npm run dev
```

- [ ] **Step 2: 验证产品管理**

1. 打开供应商列表，点击某个供应商进入详情抽屉
2. 切换到"产品"Tab，确认产品列表展示正常
3. 点击"新增产品"，填写表单并提交
4. 点击产品卡片上的"编辑"按钮，修改并保存
5. 尝试删除产品（验证至少保留一个产品的约束）

- [ ] **Step 3: 验证团队管理**

1. 点击左侧菜单"团队管理"
2. 点击"新增团队"，填写名称和描述并提交
3. 点击"编辑"修改团队信息
4. 点击"成员管理"，添加/移除成员、修改角色
5. 删除团队

- [ ] **Step 4: 验证权限控制**

1. 使用无 `provider:write` 权限的账号登录，确认产品管理按钮不显示
2. 使用无 `user:write` 权限的账号登录，确认团队管理按钮不显示

- [ ] **Step 5: 修复发现的问题**

根据验证结果修复任何 bug，提交修复。
