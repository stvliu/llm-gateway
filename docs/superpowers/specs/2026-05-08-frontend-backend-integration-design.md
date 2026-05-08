# 前端集成后端设计文档

## 概述

本文档定义 LLM-Gateway 前端（gateway-console）与后端（gateway-boot）的完整集成方案，采用标准化重构策略，确保前后端数据结构、API 接口、类型定义完全对齐。

## 当前状态分析

### 已有对接模块

| 模块 | 前端 API | 后端 Controller | 状态 |
|------|----------|-----------------|------|
| 认证 | `auth.ts` | `AuthController` | ✅ 基本匹配 |
| 提供商 | `provider.ts` | `ProviderController` | ⚠️ 缺少 `setEnabled` |
| 模型 | `model.ts` | `ModelController` | ⚠️ 缺少 `setEnabled` |
| 用户 | `user.ts` | `UserController` | ⚠️ 缺少 `updateStatus`、`assignRoles` |
| 模板 | `template.ts` | `ProviderTemplateController` | ⚠️ 响应格式不匹配 |
| API Key | `apiKey.ts` | `ApiKeyController` | ⚠️ 缺少 `setEnabled` |

### 缺失对接模块

| 模块 | 后端 Controller | 前端状态 |
|------|-----------------|----------|
| Provider API Key | `ProviderApiKeyController` | ❌ 无前端 API |
| Token 限额 | `TokenLimitController` | ❌ 无前端 API |

### 响应格式问题

- `ProviderTemplateController.listTemplates()` 返回 `ResponseEntity<Page<T>>`
- 其他 Controller 返回 `PageResponse<T>`
- 需要统一为 `PageResponse<T>` 格式

## 设计原则

### 类型定义原则

1. **命名规范**：前端使用 `camelCase`，与后端 DTO 保持一致
2. **日期格式**：使用 `string` 类型表示 ISO 8601 格式日期
3. **枚举定义**：使用字符串字面量联合类型
4. **可选字段**：使用 TypeScript `?` 标记可选属性

### API 调用原则

1. **统一前缀**：所有 API 路径基于 `/api/v1`
2. **认证方式**：JWT Token，请求头 `Authorization: Bearer <token>`
3. **响应解包**：自动解包 `ApiResponse.data` 字段
4. **错误处理**：统一在 `client.ts` 拦截器处理

### 响应格式标准

```typescript
// 单条数据响应
interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: {
    code: string;
    message: string;
    details?: unknown;
  };
  traceId: string;
  timestamp: string;
}

// 分页数据响应
interface PageResponse<T> {
  items: T[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
}
```

## 实现方案

### 一、类型定义标准化

#### 1.1 新增类型文件

**`src/types/providerApiKey.ts`**

```typescript
import type { Status } from './api';

/** Provider API Key 信息 */
export interface ProviderApiKey {
  id: number;
  providerId: number;
  providerName: string;
  name: string;
  apiKeyPreview: string;  // 脱敏后的 API Key 预览
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建 Provider API Key 请求 */
export interface CreateProviderApiKeyRequest {
  providerId: number;
  name: string;
  apiKey: string;  // 原始 API Key，仅创建时传递
}

/** 创建 Provider API Key 响应 */
export interface CreateProviderApiKeyResponse {
  id: number;
  providerId: number;
  name: string;
  apiKey: string;  // 完整 API Key，仅创建时返回一次
}

/** 更新 Provider API Key 请求 */
export interface UpdateProviderApiKeyRequest {
  name?: string;
  status?: Status;
}
```

**`src/types/tokenLimit.ts`**

```typescript
import type { Status } from './api';

/** Token 限额类型 */
export type TokenLimitScope = 'USER' | 'API_KEY';

/** Token 限额信息 */
export interface TokenLimit {
  id: number;
  scope: TokenLimitScope;
  targetId: number;  // 用户 ID 或 API Key ID
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

#### 1.2 现有类型调整

**`src/types/provider.ts`** - 无需调整，已对齐

**`src/types/model.ts`** - 无需调整，已对齐

**`src/types/user.ts`** - 补充缺失类型

```typescript
// 新增：用户状态更新请求
export interface UserStatusUpdateRequest {
  status: Status;
}

// 新增：用户角色分配请求
export interface UserRoleAssignRequest {
  roles: UserRole[];
}
```

**`src/types/apiKey.ts`** - 检查并确认字段对齐

**`src/types/template.ts`** - 适配 `Page<T>` 响应

```typescript
// 新增：Spring Data Page 响应格式（用于模板接口）
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

### 二、API 服务层标准化

#### 2.1 现有 API 服务调整

**`src/services/api/provider.ts`**

```typescript
import { api } from './client';
import type { Provider, CreateProviderRequest, UpdateProviderRequest } from '@/types/provider';
import type { PageResponse, PageParams } from '@/types/api';

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

**`src/services/api/model.ts`**

```typescript
import { api } from './client';
import type { Model, CreateModelRequest, UpdateModelRequest } from '@/types/model';
import type { PageResponse, PageParams } from '@/types/api';

export const modelApi = {
  /** 获取模型列表 */
  list: (params?: PageParams & { providerId?: number }) =>
    api.get<PageResponse<Model>>('/models', { params }),

  /** 获取模型详情 */
  get: (id: number) =>
    api.get<Model>(`/models/${id}`),

  /** 创建模型 */
  create: (data: CreateModelRequest) =>
    api.post<Model>('/models', data),

  /** 更新模型 */
  update: (id: number, data: UpdateModelRequest) =>
    api.put<Model>(`/models/${id}`, data),

  /** 删除模型 */
  delete: (id: number) =>
    api.delete<void>(`/models/${id}`),

  /** 启用/禁用模型 */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<Model>(`/models/${id}/enabled`, null, { params: { enabled } }),
};
```

**`src/services/api/user.ts`**

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

export const userApi = {
  /** 获取用户列表 */
  list: (params?: PageParams) =>
    api.get<PageResponse<User>>('/users', { params }),

  /** 获取用户详情 */
  get: (id: number) =>
    api.get<User>(`/users/${id}`),

  /** 创建用户 */
  create: (data: CreateUserRequest) =>
    api.post<User>('/users', data),

  /** 更新用户 */
  update: (id: number, data: UpdateUserRequest) =>
    api.put<User>(`/users/${id}`, data),

  /** 删除用户 */
  delete: (id: number) =>
    api.delete<void>(`/users/${id}`),

  /** 更新用户状态 */
  updateStatus: (id: number, data: UserStatusUpdateRequest) =>
    api.patch<User>(`/users/${id}/status`, data),

  /** 分配用户角色 */
  assignRoles: (id: number, data: UserRoleAssignRequest) =>
    api.put<User>(`/users/${id}/roles`, data),

  /** 重置密码 */
  resetPassword: (id: number) =>
    api.post<void>(`/users/${id}/reset-password`),
};
```

**`src/services/api/apiKey.ts`**

```typescript
import { api } from './client';
import type { ApiKey, CreateApiKeyRequest, CreateApiKeyResponse, UpdateApiKeyRequest } from '@/types/apiKey';
import type { PageResponse, PageParams } from '@/types/api';

export const apiKeyApi = {
  /** 获取 API Key 列表 */
  list: (params?: PageParams & { userId?: number }) =>
    api.get<PageResponse<ApiKey>>('/api-keys', { params }),

  /** 获取 API Key 详情 */
  get: (id: number) =>
    api.get<ApiKey>(`/api-keys/${id}`),

  /** 创建 API Key */
  create: (data: CreateApiKeyRequest) =>
    api.post<CreateApiKeyResponse>('/api-keys', data),

  /** 更新 API Key */
  update: (id: number, data: UpdateApiKeyRequest) =>
    api.put<ApiKey>(`/api-keys/${id}`, data),

  /** 删除 API Key */
  delete: (id: number) =>
    api.delete<void>(`/api-keys/${id}`),

  /** 启用/禁用 API Key */
  setEnabled: (id: number, enabled: boolean) =>
    api.patch<ApiKey>(`/api-keys/${id}/enabled`, null, { params: { enabled } }),
};
```

**`src/services/api/template.ts`** - 适配响应格式

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

// 转换 Spring Data Page 为 PageResponse
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

export const templateApi = {
  /** 获取模板列表 */
  list: async (params?: TemplateListParams & PageParams): Promise<PageResponse<ProviderTemplate>> => {
    const page = await api.get<SpringPage<ProviderTemplate>>('/templates', { params });
    return adaptPage(page);
  },

  /** 获取模板详情 */
  get: (id: number) =>
    api.get<ProviderTemplate>(`/templates/${id}`),

  /** 创建模板 */
  create: (data: CreateTemplateRequest) =>
    api.post<ProviderTemplate>('/templates', data),

  /** 更新模板 */
  update: (id: number, data: UpdateTemplateRequest) =>
    api.put<ProviderTemplate>(`/templates/${id}`, data),

  /** 删除模板 */
  delete: (id: number) =>
    api.delete<void>(`/templates/${id}`),

  /** 发布模板到公共市场 */
  publish: (id: number) =>
    api.post<void>(`/templates/${id}/publish`),

  /** 应用模板创建 Provider */
  apply: (id: number, data: ApplyTemplateRequest) =>
    api.post<ApplyTemplateResult>(`/templates/${id}/apply`, data),

  /** 导出单个模板 */
  exportTemplate: (id: number) =>
    api.get<void>(`/templates/${id}/export`),

  /** 批量导出模板 */
  exportBatch: (ids: number[]) =>
    api.get<void>('/templates/export/batch', { params: { ids } }),

  /** 导入模板 */
  import: (formData: FormData) =>
    api.post<ProviderTemplate[]>('/templates/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
};
```

#### 2.2 新增 API 服务

**`src/services/api/providerApiKey.ts`**

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

**`src/services/api/tokenLimit.ts`**

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

#### 2.3 统一导出

**`src/services/api/index.ts`**

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

### 三、React Query Hooks 标准化

#### 3.1 新增 Hooks

**`src/services/query/useProviderApiKeys.ts`**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { providerApiKeyApi } from '@/services/api/providerApiKey';
import type { PageParams } from '@/types/api';
import type { CreateProviderApiKeyRequest, UpdateProviderApiKeyRequest } from '@/types/providerApiKey';

export function useProviderApiKeys(params?: PageParams & { providerId?: number }) {
  return useQuery({
    queryKey: ['providerApiKeys', params],
    queryFn: () => providerApiKeyApi.list(params),
  });
}

export function useProviderApiKey(id: number) {
  return useQuery({
    queryKey: ['providerApiKey', id],
    queryFn: () => providerApiKeyApi.get(id),
    enabled: !!id,
  });
}

export function useProviderApiKeyMutations() {
  const queryClient = useQueryClient();

  return {
    create: useMutation({
      mutationFn: (data: CreateProviderApiKeyRequest) => providerApiKeyApi.create(data),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: ['providerApiKeys'] }),
    }),
    update: useMutation({
      mutationFn: ({ id, data }: { id: number; data: UpdateProviderApiKeyRequest }) =>
        providerApiKeyApi.update(id, data),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: ['providerApiKeys'] }),
    }),
    delete: useMutation({
      mutationFn: (id: number) => providerApiKeyApi.delete(id),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: ['providerApiKeys'] }),
    }),
    setEnabled: useMutation({
      mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
        providerApiKeyApi.setEnabled(id, enabled),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: ['providerApiKeys'] }),
    }),
  };
}
```

**`src/services/query/useTokenLimits.ts`**

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { tokenLimitApi } from '@/services/api/tokenLimit';
import type { PageParams } from '@/types/api';
import type { CreateTokenLimitRequest, UpdateTokenLimitRequest } from '@/types/tokenLimit';

export function useTokenLimits(params?: PageParams & { scope?: 'USER' | 'API_KEY'; targetId?: number }) {
  return useQuery({
    queryKey: ['tokenLimits', params],
    queryFn: () => tokenLimitApi.list(params),
  });
}

export function useTokenLimit(id: number) {
  return useQuery({
    queryKey: ['tokenLimit', id],
    queryFn: () => tokenLimitApi.get(id),
    enabled: !!id,
  });
}

export function useTokenLimitMutations() {
  const queryClient = useQueryClient();

  return {
    create: useMutation({
      mutationFn: (data: CreateTokenLimitRequest) => tokenLimitApi.create(data),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tokenLimits'] }),
    }),
    update: useMutation({
      mutationFn: ({ id, data }: { id: number; data: UpdateTokenLimitRequest }) =>
        tokenLimitApi.update(id, data),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tokenLimits'] }),
    }),
    delete: useMutation({
      mutationFn: (id: number) => tokenLimitApi.delete(id),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tokenLimits'] }),
    }),
    resetUsage: useMutation({
      mutationFn: (id: number) => tokenLimitApi.resetUsage(id),
      onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tokenLimits'] }),
    }),
  };
}
```

#### 3.2 更新现有 Hooks

需要为现有的 `useProviders`、`useModels`、`useUsers`、`useApiKeys` 补充缺失的 mutation 方法（`setEnabled`、`updateStatus`、`assignRoles` 等）。

### 四、后端调整

#### 4.1 响应格式统一

**问题**：`ProviderTemplateController.listTemplates()` 返回 `ResponseEntity<Page<T>>`，与其他接口不一致。

**方案**：保持现状，前端适配。

**理由**：
- Spring Data 的 `Page<T>` 是标准分页格式
- 前端添加适配函数 `adaptPage()` 转换格式
- 避免后端改动影响现有功能

### 五、错误处理标准化

#### 5.1 前端错误处理

**`src/services/api/client.ts`** 已实现：
- 自动解包 `ApiResponse.data`
- 处理 `success: false` 业务错误
- 401 状态自动跳转登录

**需要补充**：
- 网络错误重试机制（可选）
- 错误消息国际化

#### 5.2 后端错误处理

确保所有异常通过 `GlobalExceptionHandler` 处理，返回标准 `ApiResponse` 格式。

## 实施步骤

### 阶段一：类型定义（预计 1 小时）

1. 新增 `providerApiKey.ts` 类型文件
2. 新增 `tokenLimit.ts` 类型文件
3. 更新 `user.ts` 补充缺失类型
4. 更新 `template.ts` 补充 `SpringPage` 类型

### 阶段二：API 服务层（预计 2 小时）

1. 更新现有 API 服务，补充缺失方法
2. 新增 `providerApiKey.ts` API 服务
3. 新增 `tokenLimit.ts` API 服务
4. 更新 `template.ts` 适配响应格式
5. 更新 `index.ts` 统一导出

### 阶段三：React Query Hooks（预计 1 小时）

1. 新增 `useProviderApiKeys.ts`
2. 新增 `useTokenLimits.ts`
3. 更新现有 Hooks 补充缺失方法

### 阶段四：集成测试（预计 1 小时）

1. 启动后端服务
2. 启动前端开发服务器
3. 测试各模块 CRUD 操作
4. 验证错误处理

## 验收标准

1. **类型完整性**：所有后端 DTO 在前端有对应类型定义
2. **API 完整性**：所有后端 Controller 方法在前端有对应 API 调用
3. **响应格式一致**：所有 API 响应正确解析
4. **错误处理正确**：业务错误和网络错误正确处理和显示
5. **功能可用**：前端页面能正常调用后端 API 完成业务操作

## 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 类型不匹配 | 运行时错误 | 严格对照后端 DTO 定义类型 |
| API 路径错误 | 请求失败 | 使用统一的路径前缀 `/api/v1` |
| 响应格式变化 | 数据解析失败 | 添加适配函数处理差异 |
| 认证失败 | 无法访问受保护资源 | 确保 Token 正确存储和传递 |

## 附录

### A. 后端 Controller 与前端 API 对照表

| 后端 Controller | 前端 API 服务 | 状态 |
|-----------------|---------------|------|
| `AuthController` | `auth.ts` | ✅ 已对接 |
| `ProviderController` | `provider.ts` | ⚠️ 需补充 `setEnabled` |
| `ModelController` | `model.ts` | ⚠️ 需补充 `setEnabled` |
| `UserController` | `user.ts` | ⚠️ 需补充 `updateStatus`、`assignRoles` |
| `ApiKeyController` | `apiKey.ts` | ⚠️ 需补充 `setEnabled` |
| `ProviderApiKeyController` | `providerApiKey.ts` | ❌ 需新建 |
| `TokenLimitController` | `tokenLimit.ts` | ❌ 需新建 |
| `ProviderTemplateController` | `template.ts` | ⚠️ 需适配响应格式 |

### B. 前端文件变更清单

**新增文件**：
- `src/types/providerApiKey.ts`
- `src/types/tokenLimit.ts`
- `src/services/api/providerApiKey.ts`
- `src/services/api/tokenLimit.ts`
- `src/services/query/useProviderApiKeys.ts`
- `src/services/query/useTokenLimits.ts`

**修改文件**：
- `src/types/user.ts`
- `src/types/template.ts`
- `src/services/api/provider.ts`
- `src/services/api/model.ts`
- `src/services/api/user.ts`
- `src/services/api/apiKey.ts`
- `src/services/api/template.ts`
- `src/services/api/index.ts`
- `src/services/query/useProviders.ts`
- `src/services/query/useModels.ts`
- `src/services/query/useUsers.ts`
- `src/services/query/useApiKeys.ts`
- `src/services/query/index.ts`
