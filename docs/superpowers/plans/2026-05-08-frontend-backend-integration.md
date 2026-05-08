# 前端集成后端实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成前端 gateway-console 与后端 gateway-boot 的完整集成，确保所有 API 接口、类型定义、React Query Hooks 对齐。

**Architecture:** 采用标准化重构策略，前端新增缺失的类型定义和 API 服务，补充现有 API 服务的缺失方法，适配响应格式差异。

**Tech Stack:** React 18, TypeScript, React Query (TanStack Query), Axios, Vite

---

## 文件结构

### 新增文件
```
gateway-console/src/
├── types/
│   ├── providerApiKey.ts    # Provider API Key 类型定义
│   └── tokenLimit.ts        # Token 限额类型定义
├── services/
│   ├── api/
│   │   ├── providerApiKey.ts  # Provider API Key API 服务
│   │   └── tokenLimit.ts      # Token 限额 API 服务
│   └── query/
│       ├── useProviderApiKeys.ts  # Provider API Key Hooks
│       └── useTokenLimits.ts      # Token 限额 Hooks
```

### 修改文件
```
gateway-console/src/
├── types/
│   ├── user.ts              # 补充 UserStatusUpdateRequest, UserRoleAssignRequest
│   └── template.ts          # 补充 SpringPage 类型
├── services/
│   ├── api/
│   │   ├── provider.ts      # 补充 setEnabled 方法
│   │   ├── model.ts         # 补充 setEnabled 方法
│   │   ├── user.ts          # 补充 updateStatus, assignRoles 方法
│   │   ├── apiKey.ts        # 补充 setEnabled 方法
│   │   ├── template.ts      # 适配 SpringPage 响应格式
│   │   └── index.ts        # 统一导出
│   └── query/
│       ├── useProviders.ts  # 补充 useSetEnabledProvider
│       ├── useModels.ts     # 补充 useSetEnabledModel
│       ├── useUsers.ts      # 补充 useUpdateUserStatus, useAssignUserRoles
│       ├── useApiKeys.ts    # 补充 useSetEnabledApiKey
│       └── index.ts        # 统一导出
```

---

## Task 1: 新增 Provider API Key 类型定义

**Files:**
- Create: `gateway-console/src/types/providerApiKey.ts`

- [ ] **Step 1: 创建类型定义文件**

```typescript
import type { Status } from './api';

/** Provider API Key 信息 */
export interface ProviderApiKey {
  id: number;
  providerId: number;
  providerName: string;
  name: string;
  apiKeyPreview: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建 Provider API Key 请求 */
export interface CreateProviderApiKeyRequest {
  providerId: number;
  name: string;
  apiKey: string;
}

/** 创建 Provider API Key 响应 */
export interface CreateProviderApiKeyResponse {
  id: number;
  providerId: number;
  name: string;
  apiKey: string;
}

/** 更新 Provider API Key 请求 */
export interface UpdateProviderApiKeyRequest {
  name?: string;
  status?: Status;
}
```

- [ ] **Step 2: 验证文件创建**

Run: `cat gateway-console/src/types/providerApiKey.ts`
Expected: 文件内容与上述代码一致

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/types/providerApiKey.ts
git commit -m "feat(types): 添加 Provider API Key 类型定义"
```

---

## Task 2: 新增 Token Limit 类型定义

**Files:**
- Create: `gateway-console/src/types/tokenLimit.ts`

- [ ] **Step 1: 创建类型定义文件**

```typescript
import type { Status } from './api';

/** Token 限额类型 */
export type TokenLimitScope = 'USER' | 'API_KEY';

/** Token 限额信息 */
export interface TokenLimit {
  id: number;
  scope: TokenLimitScope;
  targetId: number;
  targetName: string;
  inputLimit: number;
  outputLimit: number;
  inputUsed: number;
  outputUsed: number;
  periodStart: string;
  periodEnd: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建 Token 限额请求 */
export interface CreateTokenLimitRequest {
  scope: TokenLimitScope;
  targetId: number;
  inputLimit: number;
  outputLimit: number;
}

/** 更新 Token 限额请求 */
export interface UpdateTokenLimitRequest {
  inputLimit?: number;
  outputLimit?: number;
  status?: Status;
}
```

- [ ] **Step 2: 验证文件创建**

Run: `cat gateway-console/src/types/tokenLimit.ts`
Expected: 文件内容与上述代码一致

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/types/tokenLimit.ts
git commit -m "feat(types): 添加 Token Limit 类型定义"
```

---

## Task 3: 更新 user.ts 补充缺失类型

**Files:**
- Modify: `gateway-console/src/types/user.ts`

- [ ] **Step 1: 在文件末尾添加缺失类型**

在 `gateway-console/src/types/user.ts` 文件末尾添加：

```typescript
/** 用户状态更新请求 */
export interface UserStatusUpdateRequest {
  status: Status;
}

/** 用户角色分配请求 */
export interface UserRoleAssignRequest {
  roles: UserRole[];
}
```

- [ ] **Step 2: 验证修改**

Run: `cat gateway-console/src/types/user.ts`
Expected: 文件末尾包含新增的两个接口定义

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/types/user.ts
git commit -m "feat(types): 补充用户状态更新和角色分配请求类型"
```

---

## Task 4: 更新 template.ts 补充 SpringPage 类型

**Files:**
- Modify: `gateway-console/src/types/template.ts`

- [ ] **Step 1: 在文件末尾添加 SpringPage 类型**

在 `gateway-console/src/types/template.ts` 文件末尾添加：

```typescript
/** Spring Data Page 响应格式 */
export interface SpringPage<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
```

- [ ] **Step 2: 验证修改**

Run: `cat gateway-console/src/types/template.ts`
Expected: 文件末尾包含 SpringPage 接口定义

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/types/template.ts
git commit -m "feat(types): 添加 SpringPage 类型用于适配模板接口响应"
```

---

## Task 5: 更新 provider.ts 补充 setEnabled 方法

**Files:**
- Modify: `gateway-console/src/services/api/provider.ts`

- [ ] **Step 1: 添加 setEnabled 方法**

在 `gateway-console/src/services/api/provider.ts` 的 `providerApi` 对象中，在 `delete` 方法后添加：

```typescript
  /** 启用/禁用渠道 */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<Provider>(`/providers/${id}/enabled`, null, { params: { enabled } }),
```

完整的 `providerApi` 对象应为：

```typescript
export const providerApi = {
  /** 获取渠道列表 */
  list: (params?: PageParams) =>
    api.get<PageResponse<Provider>>('/providers', { params }),

  /** 获取渠道详情 */
  get: (id: number) =>
    api.get<Provider>(`/providers/${id}`),

  /** 创建渠道 */
  create: (data: CreateProviderRequest) =>
    api.post<Provider>('/providers', data),

  /** 更新渠道 */
  update: (id: number, data: UpdateProviderRequest) =>
    api.put<Provider>(`/providers/${id}`, data),

  /** 删除渠道 */
  delete: (id: number) =>
    api.delete<void>(`/providers/${id}`),

  /** 启用/禁用渠道 */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<Provider>(`/providers/${id}/enabled`, null, { params: { enabled } }),
};
```

- [ ] **Step 2: 验证修改**

Run: `cat gateway-console/src/services/api/provider.ts`
Expected: `providerApi` 对象包含 `setEnabled` 方法

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/api/provider.ts
git commit -m "feat(api): 补充 Provider setEnabled 方法"
```

---

## Task 6: 更新 model.ts 补充 setEnabled 方法

**Files:**
- Modify: `gateway-console/src/services/api/model.ts`

- [ ] **Step 1: 添加 setEnabled 方法**

在 `gateway-console/src/services/api/model.ts` 的 `modelApi` 对象中，在 `delete` 方法后添加：

```typescript
  /** 启用/禁用模型 */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<Model>(`/models/${id}/enabled`, null, { params: { enabled } }),
```

- [ ] **Step 2: 验证修改**

Run: `cat gateway-console/src/services/api/model.ts`
Expected: `modelApi` 对象包含 `setEnabled` 方法

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/api/model.ts
git commit -m "feat(api): 补充 Model setEnabled 方法"
```

---

## Task 7: 更新 user.ts 补充 updateStatus 和 assignRoles 方法

**Files:**
- Modify: `gateway-console/src/services/api/user.ts`

- [ ] **Step 1: 更新导入语句**

将导入语句修改为：

```typescript
import { api } from './client';
import type {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  UserStatusUpdateRequest,
  UserRoleAssignRequest,
} from '@/types/user';
import type { PageResponse, PageParams } from '@/types/api';
```

- [ ] **Step 2: 添加 updateStatus 和 assignRoles 方法**

在 `userApi` 对象中，在 `delete` 方法后、`resetPassword` 方法前添加：

```typescript
  /** 更新用户状态 */
  updateStatus: (id: number, data: UserStatusUpdateRequest) =>
    api.patch<User>(`/users/${id}/status`, data),

  /** 分配用户角色 */
  assignRoles: (id: number, data: UserRoleAssignRequest) =>
    api.put<User>(`/users/${id}/roles`, data),
```

- [ ] **Step 3: 验证修改**

Run: `cat gateway-console/src/services/api/user.ts`
Expected: `userApi` 对象包含 `updateStatus` 和 `assignRoles` 方法

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/services/api/user.ts
git commit -m "feat(api): 补充 User updateStatus 和 assignRoles 方法"
```

---

## Task 8: 更新 apiKey.ts 补充 setEnabled 方法

**Files:**
- Modify: `gateway-console/src/services/api/apiKey.ts`

- [ ] **Step 1: 添加 setEnabled 方法**

在 `gateway-console/src/services/api/apiKey.ts` 的 `apiKeyApi` 对象中，在 `delete` 方法后添加：

```typescript
  /** 启用/禁用 API Key */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<ApiKey>(`/api-keys/${id}/enabled`, null, { params: { enabled } }),
```

- [ ] **Step 2: 验证修改**

Run: `cat gateway-console/src/services/api/apiKey.ts`
Expected: `apiKeyApi` 对象包含 `setEnabled` 方法

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/api/apiKey.ts
git commit -m "feat(api): 补充 ApiKey setEnabled 方法"
```

---

## Task 9: 更新 template.ts 适配 SpringPage 响应格式

**Files:**
- Modify: `gateway-console/src/services/api/template.ts`

- [ ] **Step 1: 更新导入语句**

将导入语句修改为：

```typescript
import { api } from './client';
import type {
  ProviderTemplate,
  CreateTemplateRequest,
  UpdateTemplateRequest,
  ApplyTemplateRequest,
  ApplyTemplateResult,
  TemplateListParams,
  SpringPage,
} from '@/types/template';
import type { PageResponse, PageParams } from '@/types/api';
```

- [ ] **Step 2: 添加 adaptPage 辅助函数**

在导入语句后、`templateApi` 对象前添加：

```typescript
/** 转换 Spring Data Page 为 PageResponse */
function adaptPage<T>(page: SpringPage<T>): PageResponse<T> {
  return {
    items: page.content,
    pagination: {
      page: page.pageable.pageNumber,
      limit: page.pageable.pageSize,
      total: page.totalElements,
      totalPages: page.totalPages,
    },
  };
}
```

- [ ] **Step 3: 修改 list 方法**

将 `list` 方法修改为：

```typescript
  /** 获取模板列表 */
  list: async (params?: TemplateListParams & PageParams): Promise<PageResponse<ProviderTemplate>> => {
    const page = await api.get<SpringPage<ProviderTemplate>>('/templates', { params });
    return adaptPage(page);
  },
```

- [ ] **Step 4: 验证修改**

Run: `cat gateway-console/src/services/api/template.ts`
Expected: 文件包含 `adaptPage` 函数，`list` 方法使用适配器

- [ ] **Step 5: 提交**

```bash
git add gateway-console/src/services/api/template.ts
git commit -m "feat(api): 适配 Template 接口 SpringPage 响应格式"
```

---

## Task 10: 新增 Provider API Key API 服务

**Files:**
- Create: `gateway-console/src/services/api/providerApiKey.ts`

- [ ] **Step 1: 创建 API 服务文件**

```typescript
import { api } from './client';
import type {
  ProviderApiKey,
  CreateProviderApiKeyRequest,
  CreateProviderApiKeyResponse,
  UpdateProviderApiKeyRequest,
} from '@/types/providerApiKey';
import type { PageResponse, PageParams } from '@/types/api';

export const providerApiKeyApi = {
  /** 获取 Provider API Key 列表 */
  list: (params?: PageParams & { providerId?: number }) =>
    api.get<PageResponse<ProviderApiKey>>('/provider-api-keys', { params }),

  /** 获取 Provider API Key 详情 */
  get: (id: number) =>
    api.get<ProviderApiKey>(`/provider-api-keys/${id}`),

  /** 创建 Provider API Key */
  create: (data: CreateProviderApiKeyRequest) =>
    api.post<CreateProviderApiKeyResponse>('/provider-api-keys', data),

  /** 更新 Provider API Key */
  update: (id: number, data: UpdateProviderApiKeyRequest) =>
    api.put<ProviderApiKey>(`/provider-api-keys/${id}`, data),

  /** 删除 Provider API Key */
  delete: (id: number) =>
    api.delete<void>(`/provider-api-keys/${id}`),

  /** 启用/禁用 Provider API Key */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<ProviderApiKey>(`/provider-api-keys/${id}/enabled`, null, { params: { enabled } }),
};
```

- [ ] **Step 2: 验证文件创建**

Run: `cat gateway-console/src/services/api/providerApiKey.ts`
Expected: 文件内容与上述代码一致

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/api/providerApiKey.ts
git commit -m "feat(api): 添加 Provider API Key API 服务"
```

---

## Task 11: 新增 Token Limit API 服务

**Files:**
- Create: `gateway-console/src/services/api/tokenLimit.ts`

- [ ] **Step 1: 创建 API 服务文件**

```typescript
import { api } from './client';
import type {
  TokenLimit,
  CreateTokenLimitRequest,
  UpdateTokenLimitRequest,
} from '@/types/tokenLimit';
import type { PageResponse, PageParams } from '@/types/api';

export const tokenLimitApi = {
  /** 获取 Token 限额列表 */
  list: (params?: PageParams & { scope?: 'USER' | 'API_KEY'; targetId?: number }) =>
    api.get<PageResponse<TokenLimit>>('/token-limits', { params }),

  /** 获取 Token 限额详情 */
  get: (id: number) =>
    api.get<TokenLimit>(`/token-limits/${id}`),

  /** 创建 Token 限额 */
  create: (data: CreateTokenLimitRequest) =>
    api.post<TokenLimit>('/token-limits', data),

  /** 更新 Token 限额 */
  update: (id: number, data: UpdateTokenLimitRequest) =>
    api.put<TokenLimit>(`/token-limits/${id}`, data),

  /** 删除 Token 限额 */
  delete: (id: number) =>
    api.delete<void>(`/token-limits/${id}`),

  /** 重置已使用量 */
  resetUsage: (id: number) =>
    api.patch<TokenLimit>(`/token-limits/${id}/reset-usage`),
};
```

- [ ] **Step 2: 验证文件创建**

Run: `cat gateway-console/src/services/api/tokenLimit.ts`
Expected: 文件内容与上述代码一致

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/api/tokenLimit.ts
git commit -m "feat(api): 添加 Token Limit API 服务"
```

---

## Task 12: 更新 API 服务统一导出

**Files:**
- Modify: `gateway-console/src/services/api/index.ts`

- [ ] **Step 1: 更新导出文件**

将 `gateway-console/src/services/api/index.ts` 内容修改为：

```typescript
export * from './client';
export * from './auth';
export * from './provider';
export * from './model';
export * from './user';
export * from './apiKey';
export * from './providerApiKey';
export * from './tokenLimit';
export * from './template';
```

- [ ] **Step 2: 验证修改**

Run: `cat gateway-console/src/services/api/index.ts`
Expected: 文件包含所有 API 服务的导出

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/api/index.ts
git commit -m "feat(api): 统一导出所有 API 服务"
```

---

## Task 13: 更新 useProviders.ts 补充 useSetEnabledProvider

**Files:**
- Modify: `gateway-console/src/services/query/useProviders.ts`

- [ ] **Step 1: 添加 useSetEnabledProvider Hook**

在 `gateway-console/src/services/query/useProviders.ts` 文件末尾添加：

```typescript
export function useSetEnabledProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      providerApi.setEnabled(id, enabled),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: providerKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: providerKeys.lists() });
    },
  });
}
```

- [ ] **Step 2: 验证修改**

Run: `cat gateway-console/src/services/query/useProviders.ts`
Expected: 文件末尾包含 `useSetEnabledProvider` 函数

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/query/useProviders.ts
git commit -m "feat(query): 补充 useSetEnabledProvider Hook"
```

---

## Task 14: 更新 useModels.ts 补充 useSetEnabledModel

**Files:**
- Modify: `gateway-console/src/services/query/useModels.ts`

- [ ] **Step 1: 添加 useSetEnabledModel Hook**

在 `gateway-console/src/services/query/useModels.ts` 文件末尾添加：

```typescript
export function useSetEnabledModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      modelApi.setEnabled(id, enabled),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: modelKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}
```

- [ ] **Step 2: 验证修改**

Run: `cat gateway-console/src/services/query/useModels.ts`
Expected: 文件末尾包含 `useSetEnabledModel` 函数

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/query/useModels.ts
git commit -m "feat(query): 补充 useSetEnabledModel Hook"
```

---

## Task 15: 更新 useUsers.ts 补充 useUpdateUserStatus 和 useAssignUserRoles

**Files:**
- Modify: `gateway-console/src/services/query/useUsers.ts`

- [ ] **Step 1: 更新导入语句**

将导入语句修改为：

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '@/services/api/user';
import type {
  CreateUserRequest,
  UpdateUserRequest,
  UserStatusUpdateRequest,
  UserRoleAssignRequest,
} from '@/types/user';
```

- [ ] **Step 2: 添加 useUpdateUserStatus Hook**

在 `useResetPassword` 函数后添加：

```typescript
export function useUpdateUserStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UserStatusUpdateRequest }) =>
      userApi.updateStatus(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: userKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}
```

- [ ] **Step 3: 添加 useAssignUserRoles Hook**

在 `useUpdateUserStatus` 函数后添加：

```typescript
export function useAssignUserRoles() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UserRoleAssignRequest }) =>
      userApi.assignRoles(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: userKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}
```

- [ ] **Step 4: 验证修改**

Run: `cat gateway-console/src/services/query/useUsers.ts`
Expected: 文件包含 `useUpdateUserStatus` 和 `useAssignUserRoles` 函数

- [ ] **Step 5: 提交**

```bash
git add gateway-console/src/services/query/useUsers.ts
git commit -m "feat(query): 补充 useUpdateUserStatus 和 useAssignUserRoles Hook"
```

---

## Task 16: 更新 useApiKeys.ts 补充 useSetEnabledApiKey

**Files:**
- Modify: `gateway-console/src/services/query/useApiKeys.ts`

- [ ] **Step 1: 添加 useSetEnabledApiKey Hook**

在 `gateway-console/src/services/query/useApiKeys.ts` 文件末尾添加：

```typescript
export function useSetEnabledApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      apiKeyApi.setEnabled(id, enabled),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.lists() });
    },
  });
}
```

- [ ] **Step 2: 验证修改**

Run: `cat gateway-console/src/services/query/useApiKeys.ts`
Expected: 文件末尾包含 `useSetEnabledApiKey` 函数

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/query/useApiKeys.ts
git commit -m "feat(query): 补充 useSetEnabledApiKey Hook"
```

---

## Task 17: 新增 useProviderApiKeys.ts

**Files:**
- Create: `gateway-console/src/services/query/useProviderApiKeys.ts`

- [ ] **Step 1: 创建 Hooks 文件**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { providerApiKeyApi } from '@/services/api/providerApiKey';
import type { PageParams } from '@/types/api';
import type {
  CreateProviderApiKeyRequest,
  UpdateProviderApiKeyRequest,
} from '@/types/providerApiKey';

export const providerApiKeyKeys = {
  all: ['providerApiKeys'] as const,
  lists: () => [...providerApiKeyKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...providerApiKeyKeys.lists(), params] as const,
  details: () => [...providerApiKeyKeys.all, 'detail'] as const,
  detail: (id: number) => [...providerApiKeyKeys.details(), id] as const,
};

export function useProviderApiKeys(params?: PageParams & { providerId?: number }) {
  return useQuery({
    queryKey: providerApiKeyKeys.list(params),
    queryFn: () => providerApiKeyApi.list(params),
  });
}

export function useProviderApiKey(id: number) {
  return useQuery({
    queryKey: providerApiKeyKeys.detail(id),
    queryFn: () => providerApiKeyApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateProviderApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateProviderApiKeyRequest) => providerApiKeyApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.lists() });
    },
  });
}

export function useUpdateProviderApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateProviderApiKeyRequest }) =>
      providerApiKeyApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.lists() });
    },
  });
}

export function useDeleteProviderApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => providerApiKeyApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.lists() });
    },
  });
}

export function useSetEnabledProviderApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      providerApiKeyApi.setEnabled(id, enabled),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: providerApiKeyKeys.lists() });
    },
  });
}
```

- [ ] **Step 2: 验证文件创建**

Run: `cat gateway-console/src/services/query/useProviderApiKeys.ts`
Expected: 文件内容与上述代码一致

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/query/useProviderApiKeys.ts
git commit -m "feat(query): 添加 Provider API Key Hooks"
```

---

## Task 18: 新增 useTokenLimits.ts

**Files:**
- Create: `gateway-console/src/services/query/useTokenLimits.ts`

- [ ] **Step 1: 创建 Hooks 文件**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { tokenLimitApi } from '@/services/api/tokenLimit';
import type { PageParams } from '@/types/api';
import type {
  CreateTokenLimitRequest,
  UpdateTokenLimitRequest,
} from '@/types/tokenLimit';

export const tokenLimitKeys = {
  all: ['tokenLimits'] as const,
  lists: () => [...tokenLimitKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...tokenLimitKeys.lists(), params] as const,
  details: () => [...tokenLimitKeys.all, 'detail'] as const,
  detail: (id: number) => [...tokenLimitKeys.details(), id] as const,
};

export function useTokenLimits(params?: PageParams & { scope?: 'USER' | 'API_KEY'; targetId?: number }) {
  return useQuery({
    queryKey: tokenLimitKeys.list(params),
    queryFn: () => tokenLimitApi.list(params),
  });
}

export function useTokenLimit(id: number) {
  return useQuery({
    queryKey: tokenLimitKeys.detail(id),
    queryFn: () => tokenLimitApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateTokenLimit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTokenLimitRequest) => tokenLimitApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}

export function useUpdateTokenLimit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateTokenLimitRequest }) =>
      tokenLimitApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}

export function useDeleteTokenLimit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => tokenLimitApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}

export function useResetTokenLimitUsage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => tokenLimitApi.resetUsage(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: tokenLimitKeys.lists() });
    },
  });
}
```

- [ ] **Step 2: 验证文件创建**

Run: `cat gateway-console/src/services/query/useTokenLimits.ts`
Expected: 文件内容与上述代码一致

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/query/useTokenLimits.ts
git commit -m "feat(query): 添加 Token Limit Hooks"
```

---

## Task 19: 更新 Query Hooks 统一导出

**Files:**
- Modify: `gateway-console/src/services/query/index.ts`

- [ ] **Step 1: 更新导出文件**

将 `gateway-console/src/services/query/index.ts` 内容修改为：

```typescript
export * from './useProviders';
export * from './useModels';
export * from './useUsers';
export * from './useApiKeys';
export * from './useTemplates';
export * from './useProviderApiKeys';
export * from './useTokenLimits';
```

- [ ] **Step 2: 验证修改**

Run: `cat gateway-console/src/services/query/index.ts`
Expected: 文件包含所有 Query Hooks 的导出

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/services/query/index.ts
git commit -m "feat(query): 统一导出所有 Query Hooks"
```

---

## Task 20: 验证 TypeScript 编译

**Files:**
- None (验证步骤)

- [ ] **Step 1: 进入前端目录并运行 TypeScript 编译检查**

Run: `cd gateway-console && npm run build`
Expected: 编译成功，无类型错误

- [ ] **Step 2: 如果有编译错误，修复后重新验证**

常见问题：
- 导入路径错误：检查 `@/` 别名是否正确
- 类型不匹配：检查接口定义是否与使用一致

---

## Task 21: 集成测试验证

**Files:**
- None (验证步骤)

- [ ] **Step 1: 启动后端服务**

Run: `cd gateway-boot && ../mvnw spring-boot:run -Dspring-boot.run.profiles=local`
Expected: 后端服务在 8080 端口启动成功

- [ ] **Step 2: 启动前端开发服务器**

Run: `cd gateway-console && npm run dev`
Expected: 前端服务在 5173 端口启动成功

- [ ] **Step 3: 测试登录功能**

1. 访问 `http://localhost:5173/login`
2. 输入用户名密码登录
3. 验证登录成功后跳转到管理页面

- [ ] **Step 4: 测试 Provider 管理功能**

1. 访问 Provider 列表页面
2. 测试创建、编辑、删除 Provider
3. 测试启用/禁用 Provider

- [ ] **Step 5: 测试 Model 管理功能**

1. 访问 Model 列表页面
2. 测试创建、编辑、删除 Model
3. 测试启用/禁用 Model

- [ ] **Step 6: 测试 User 管理功能**

1. 访问 User 列表页面
2. 测试创建、编辑、删除 User
3. 测试更新用户状态
4. 测试分配用户角色

---

## 验收清单

- [ ] 所有新增类型定义文件创建完成
- [ ] 所有新增 API 服务文件创建完成
- [ ] 所有新增 Query Hooks 文件创建完成
- [ ] 现有文件修改完成，补充缺失方法
- [ ] TypeScript 编译无错误
- [ ] 前端开发服务器启动成功
- [ ] 登录功能正常
- [ ] Provider CRUD 和状态切换功能正常
- [ ] Model CRUD 和状态切换功能正常
- [ ] User CRUD、状态更新、角色分配功能正常
- [ ] Template 列表加载正常（响应格式适配成功）
