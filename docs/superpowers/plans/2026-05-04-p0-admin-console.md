# P0 管理控制台实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 LLM-Gateway P0 阶段管理控制台前端，支持管理员和普通用户两种角色。

**Architecture:** React 19 + TypeScript + Vite 单页应用，使用 Ant Design 5.x 作为 UI 组件库，Zustand 管理全局状态，TanStack Query 管理服务端状态，react-i18next 实现国际化。构建产物打包进 Spring Boot JAR 的 static 目录。

**Tech Stack:** React 19, TypeScript 5.x, Vite 6.x, Ant Design 5.x, Zustand 5.x, TanStack Query 5.x, React Router 7.x, Axios 1.x, react-i18next 14.x

---

## 文件结构

```
frontend/
├── index.html                    # 入口 HTML
├── package.json                  # 依赖配置
├── tsconfig.json                 # TypeScript 配置
├── tsconfig.node.json            # Node TypeScript 配置
├── vite.config.ts                # Vite 配置
├── .eslintrc.cjs                 # ESLint 配置
├── .prettierrc                   # Prettier 配置
├── src/
│   ├── main.tsx                  # 应用入口
│   ├── App.tsx                   # 根组件
│   ├── vite-env.d.ts             # Vite 类型声明
│   │
│   ├── types/                    # TypeScript 类型定义
│   │   ├── user.ts
│   │   ├── provider.ts
│   │   ├── model.ts
│   │   ├── apiKey.ts
│   │   └── api.ts
│   │
│   ├── stores/                   # Zustand 状态管理
│   │   ├── authStore.ts
│   │   └── themeStore.ts
│   │
│   ├── services/                 # API 服务层
│   │   ├── api/
│   │   │   ├── client.ts         # Axios 实例
│   │   │   ├── auth.ts           # 认证 API
│   │   │   ├── provider.ts       # 渠道 API
│   │   │   ├── model.ts          # 模型 API
│   │   │   ├── user.ts           # 用户 API
│   │   │   └── apiKey.ts         # API Key API
│   │   └── query/
│   │       ├── index.ts          # TanStack Query 配置
│   │       ├── useProviders.ts
│   │       ├── useModels.ts
│   │       ├── useUsers.ts
│   │       └── useApiKeys.ts
│   │
│   ├── locales/                  # 国际化资源
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
│   │
│   ├── i18n.ts                   # i18n 配置
│   │
│   ├── components/               # 组件
│   │   ├── layout/
│   │   │   ├── AdminLayout.tsx   # 管理员布局
│   │   │   ├── UserLayout.tsx    # 用户布局
│   │   │   ├── Header.tsx        # 顶部栏
│   │   │   ├── Sidebar.tsx       # 侧边栏
│   │   │   └── TabBar.tsx        # 标签页栏
│   │   └── shared/
│   │       ├── ModelTable.tsx    # 模型表格
│   │       └── ApiKeyTable.tsx   # API Key 表格
│   │
│   ├── pages/                    # 页面
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
│   │
│   ├── router/                   # 路由
│   │   ├── index.tsx             # 路由配置
│   │   └── guards.tsx            # 路由守卫
│   │
│   └── styles/                   # 样式
│       ├── global.css            # 全局样式
│       └── variables.css         # CSS 变量
│
└── dist/                         # 构建输出
```

---

## Task 1: 项目初始化

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/index.html`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/App.tsx`
- Create: `frontend/src/vite-env.d.ts`

- [ ] **Step 1: 创建 frontend 目录和 package.json**

```bash
mkdir -p frontend/src
cd frontend
```

创建 `frontend/package.json`:

```json
{
  "name": "llm-gateway-admin",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "lint": "eslint .",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "react-router-dom": "^7.5.0",
    "antd": "^5.24.0",
    "@ant-design/icons": "^5.6.0",
    "zustand": "^5.0.0",
    "@tanstack/react-query": "^5.70.0",
    "axios": "^1.8.0",
    "i18next": "^24.0.0",
    "react-i18next": "^15.0.0",
    "i18next-browser-languagedetector": "^8.0.0"
  },
  "devDependencies": {
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "@vitejs/plugin-react": "^4.3.0",
    "typescript": "~5.8.0",
    "vite": "^6.2.0",
    "eslint": "^9.22.0",
    "@eslint/js": "^9.22.0",
    "typescript-eslint": "^8.26.0",
    "eslint-plugin-react-hooks": "^5.2.0",
    "eslint-plugin-react-refresh": "^0.4.19",
    "globals": "^16.0.0",
    "prettier": "^3.5.0"
  }
}
```

- [ ] **Step 2: 创建 TypeScript 配置**

创建 `frontend/tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "useDefineForClassFields": true,
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedSideEffectImports": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src"]
}
```

创建 `frontend/tsconfig.node.json`:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2023"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedSideEffectImports": true
  },
  "include": ["vite.config.ts"]
}
```

- [ ] **Step 3: 创建 Vite 配置**

创建 `frontend/vite.config.ts`:

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
});
```

- [ ] **Step 4: 创建入口 HTML**

创建 `frontend/index.html`:

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>LLM Gateway - Admin Console</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 5: 创建应用入口**

创建 `frontend/src/vite-env.d.ts`:

```typescript
/// <reference types="vite/client" />
```

创建 `frontend/src/main.tsx`:

```typescript
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';
import './styles/global.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhCN}>
        <App />
      </ConfigProvider>
    </QueryClientProvider>
  </StrictMode>
);
```

创建 `frontend/src/App.tsx`:

```typescript
function App() {
  return (
    <div style={{ padding: 24 }}>
      <h1>LLM Gateway Admin Console</h1>
      <p>Initializing...</p>
    </div>
  );
}

export default App;
```

- [ ] **Step 6: 创建全局样式**

创建 `frontend/src/styles/global.css`:

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#root {
  height: 100%;
  width: 100%;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
```

- [ ] **Step 7: 安装依赖并验证**

```bash
cd frontend
pnpm install
pnpm dev
```

访问 http://localhost:5173，确认页面显示 "LLM Gateway Admin Console"。

- [ ] **Step 8: 提交**

```bash
git add frontend/
git commit -m "feat: initialize frontend project with Vite + React + TypeScript"
```

---

## Task 2: TypeScript 类型定义

**Files:**
- Create: `frontend/src/types/api.ts`
- Create: `frontend/src/types/user.ts`
- Create: `frontend/src/types/provider.ts`
- Create: `frontend/src/types/model.ts`
- Create: `frontend/src/types/apiKey.ts`

- [ ] **Step 1: 创建 API 响应类型**

创建 `frontend/src/types/api.ts`:

```typescript
/** 分页请求参数 */
export interface PageParams {
  page?: number;
  size?: number;
}

/** 分页响应 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/** API 响应包装 */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

/** 通用状态枚举 */
export type Status = 'ENABLED' | 'DISABLED';
```

- [ ] **Step 2: 创建用户类型**

创建 `frontend/src/types/user.ts`:

```typescript
import type { Status } from './api';

/** 用户角色 */
export type UserRole = 'ADMIN' | 'USER';

/** 用户信息 */
export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建用户请求 */
export interface CreateUserRequest {
  username: string;
  password: string;
  email: string;
  role: UserRole;
}

/** 更新用户请求 */
export interface UpdateUserRequest {
  email?: string;
  role?: UserRole;
  status?: Status;
}

/** 登录请求 */
export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

/** 登录响应 */
export interface LoginResponse {
  user: User;
  token?: string;
}

/** 当前用户信息 */
export interface CurrentUser {
  id: number;
  username: string;
  email: string;
  role: UserRole;
}
```

- [ ] **Step 3: 创建渠道类型**

创建 `frontend/src/types/provider.ts`:

```typescript
import type { Status } from './api';

/** 渠道类型 */
export type ProviderType = 'OPENAI' | 'ANTHROPIC' | 'GOOGLE' | 'AZURE' | 'CUSTOM';

/** 渠道信息 */
export interface Provider {
  id: number;
  name: string;
  code: string;
  type: ProviderType;
  baseUrl: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建渠道请求 */
export interface CreateProviderRequest {
  name: string;
  code: string;
  type: ProviderType;
  baseUrl: string;
}

/** 更新渠道请求 */
export interface UpdateProviderRequest {
  name?: string;
  baseUrl?: string;
  status?: Status;
}
```

- [ ] **Step 4: 创建模型类型**

创建 `frontend/src/types/model.ts`:

```typescript
import type { Status } from './api';

/** 模型类型 */
export type ModelType = 'CHAT' | 'COMPLETION' | 'EMBEDDING' | 'IMAGE' | 'AUDIO';

/** 模型信息 */
export interface Model {
  id: number;
  name: string;
  code: string;
  providerId: number;
  providerName: string;
  type: ModelType;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建模型请求 */
export interface CreateModelRequest {
  name: string;
  code: string;
  providerId: number;
  type: ModelType;
}

/** 更新模型请求 */
export interface UpdateModelRequest {
  name?: string;
  status?: Status;
}
```

- [ ] **Step 5: 创建 API Key 类型**

创建 `frontend/src/types/apiKey.ts`:

```typescript
import type { Status } from './api';

/** API Key 信息 */
export interface ApiKey {
  id: number;
  name: string;
  key: string; // 脱敏后的 Key
  userId: number;
  userName: string;
  status: Status;
  createdAt: string;
  updatedAt: string;
}

/** 创建 API Key 请求 */
export interface CreateApiKeyRequest {
  name: string;
  userId: number;
}

/** 创建 API Key 响应（包含完整 Key） */
export interface CreateApiKeyResponse {
  id: number;
  name: string;
  key: string; // 完整 Key，仅创建时返回一次
  userId: number;
  createdAt: string;
}

/** 更新 API Key 请求 */
export interface UpdateApiKeyRequest {
  name?: string;
  status?: Status;
}
```

- [ ] **Step 6: 提交**

```bash
git add frontend/src/types/
git commit -m "feat: add TypeScript type definitions"
```

---

## Task 3: API 客户端

**Files:**
- Create: `frontend/src/services/api/client.ts`
- Create: `frontend/src/services/api/auth.ts`
- Create: `frontend/src/services/api/provider.ts`
- Create: `frontend/src/services/api/model.ts`
- Create: `frontend/src/services/api/user.ts`
- Create: `frontend/src/services/api/apiKey.ts`

- [ ] **Step 1: 创建 Axios 客户端**

创建 `frontend/src/services/api/client.ts`:

```typescript
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios';

const instance: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const api = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    instance.get<T>(url, config).then((res) => res.data),

  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    instance.post<T>(url, data, config).then((res) => res.data),

  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    instance.put<T>(url, data, config).then((res) => res.data),

  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    instance.delete<T>(url, config).then((res) => res.data),
};

export default instance;
```

- [ ] **Step 2: 创建认证 API**

创建 `frontend/src/services/api/auth.ts`:

```typescript
import { api } from './client';
import type { LoginRequest, LoginResponse, CurrentUser } from '@/types/user';

export const authApi = {
  /** 登录 */
  login: (data: LoginRequest) =>
    api.post<LoginResponse>('/auth/login', data),

  /** 登出 */
  logout: () =>
    api.post<void>('/auth/logout'),

  /** 获取当前用户信息 */
  me: () =>
    api.get<CurrentUser>('/auth/me'),

  /** 修改密码 */
  changePassword: (data: { currentPassword: string; newPassword: string }) =>
    api.post<void>('/auth/change-password', data),
};
```

- [ ] **Step 3: 创建渠道 API**

创建 `frontend/src/services/api/provider.ts`:

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
};
```

- [ ] **Step 4: 创建模型 API**

创建 `frontend/src/services/api/model.ts`:

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
};
```

- [ ] **Step 5: 创建用户 API**

创建 `frontend/src/services/api/user.ts`:

```typescript
import { api } from './client';
import type { User, CreateUserRequest, UpdateUserRequest } from '@/types/user';
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

  /** 重置密码 */
  resetPassword: (id: number) =>
    api.post<void>(`/users/${id}/reset-password`),
};
```

- [ ] **Step 6: 创建 API Key API**

创建 `frontend/src/services/api/apiKey.ts`:

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
};
```

- [ ] **Step 7: 提交**

```bash
git add frontend/src/services/api/
git commit -m "feat: add API client services"
```

---

## Task 4: TanStack Query Hooks

**Files:**
- Create: `frontend/src/services/query/index.ts`
- Create: `frontend/src/services/query/useProviders.ts`
- Create: `frontend/src/services/query/useModels.ts`
- Create: `frontend/src/services/query/useUsers.ts`
- Create: `frontend/src/services/query/useApiKeys.ts`

- [ ] **Step 1: 创建 Query 配置**

创建 `frontend/src/services/query/index.ts`:

```typescript
export * from './useProviders';
export * from './useModels';
export * from './useUsers';
export * from './useApiKeys';
```

- [ ] **Step 2: 创建渠道 Query Hooks**

创建 `frontend/src/services/query/useProviders.ts`:

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { providerApi } from '@/services/api/provider';
import type { CreateProviderRequest, UpdateProviderRequest } from '@/types/provider';

export const providerKeys = {
  all: ['providers'] as const,
  lists: () => [...providerKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...providerKeys.lists(), params] as const,
  details: () => [...providerKeys.all, 'detail'] as const,
  detail: (id: number) => [...providerKeys.details(), id] as const,
};

export function useProviders(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: providerKeys.list(params),
    queryFn: () => providerApi.list(params),
  });
}

export function useProvider(id: number) {
  return useQuery({
    queryKey: providerKeys.detail(id),
    queryFn: () => providerApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateProviderRequest) => providerApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: providerKeys.lists() });
    },
  });
}

export function useUpdateProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateProviderRequest }) =>
      providerApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: providerKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: providerKeys.lists() });
    },
  });
}

export function useDeleteProvider() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => providerApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: providerKeys.lists() });
    },
  });
}
```

- [ ] **Step 3: 创建模型 Query Hooks**

创建 `frontend/src/services/query/useModels.ts`:

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { modelApi } from '@/services/api/model';
import type { CreateModelRequest, UpdateModelRequest } from '@/types/model';

export const modelKeys = {
  all: ['models'] as const,
  lists: () => [...modelKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...modelKeys.lists(), params] as const,
  details: () => [...modelKeys.all, 'detail'] as const,
  detail: (id: number) => [...modelKeys.details(), id] as const,
};

export function useModels(params?: { page?: number; size?: number; providerId?: number }) {
  return useQuery({
    queryKey: modelKeys.list(params),
    queryFn: () => modelApi.list(params),
  });
}

export function useModel(id: number) {
  return useQuery({
    queryKey: modelKeys.detail(id),
    queryFn: () => modelApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateModelRequest) => modelApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}

export function useUpdateModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateModelRequest }) =>
      modelApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: modelKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}

export function useDeleteModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => modelApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}
```

- [ ] **Step 4: 创建用户 Query Hooks**

创建 `frontend/src/services/query/useUsers.ts`:

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '@/services/api/user';
import type { CreateUserRequest, UpdateUserRequest } from '@/types/user';

export const userKeys = {
  all: ['users'] as const,
  lists: () => [...userKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...userKeys.lists(), params] as const,
  details: () => [...userKeys.all, 'detail'] as const,
  detail: (id: number) => [...userKeys.details(), id] as const,
};

export function useUsers(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: userKeys.list(params),
    queryFn: () => userApi.list(params),
  });
}

export function useUser(id: number) {
  return useQuery({
    queryKey: userKeys.detail(id),
    queryFn: () => userApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserRequest) => userApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateUserRequest }) =>
      userApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: userKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}

export function useDeleteUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => userApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}

export function useResetPassword() {
  return useMutation({
    mutationFn: (id: number) => userApi.resetPassword(id),
  });
}
```

- [ ] **Step 5: 创建 API Key Query Hooks**

创建 `frontend/src/services/query/useApiKeys.ts`:

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiKeyApi } from '@/services/api/apiKey';
import type { CreateApiKeyRequest, UpdateApiKeyRequest } from '@/types/apiKey';

export const apiKeyKeys = {
  all: ['apiKeys'] as const,
  lists: () => [...apiKeyKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...apiKeyKeys.lists(), params] as const,
  details: () => [...apiKeyKeys.all, 'detail'] as const,
  detail: (id: number) => [...apiKeyKeys.details(), id] as const,
};

export function useApiKeys(params?: { page?: number; size?: number; userId?: number }) {
  return useQuery({
    queryKey: apiKeyKeys.list(params),
    queryFn: () => apiKeyApi.list(params),
  });
}

export function useApiKey(id: number) {
  return useQuery({
    queryKey: apiKeyKeys.detail(id),
    queryFn: () => apiKeyApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateApiKeyRequest) => apiKeyApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.lists() });
    },
  });
}

export function useUpdateApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateApiKeyRequest }) =>
      apiKeyApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.lists() });
    },
  });
}

export function useDeleteApiKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiKeyApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: apiKeyKeys.lists() });
    },
  });
}
```

- [ ] **Step 6: 提交**

```bash
git add frontend/src/services/query/
git commit -m "feat: add TanStack Query hooks"
```

---

## Task 5: 状态管理

**Files:**
- Create: `frontend/src/stores/authStore.ts`
- Create: `frontend/src/stores/themeStore.ts`

- [ ] **Step 1: 创建认证状态**

创建 `frontend/src/stores/authStore.ts`:

```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { CurrentUser } from '@/types/user';

interface AuthState {
  user: CurrentUser | null;
  token: string | null;
  isAuthenticated: boolean;
  setUser: (user: CurrentUser | null) => void;
  setToken: (token: string | null) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      setUser: (user) =>
        set({
          user,
          isAuthenticated: !!user,
        }),
      setToken: (token) => set({ token }),
      logout: () => {
        localStorage.removeItem('token');
        set({
          user: null,
          token: null,
          isAuthenticated: false,
        });
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
```

- [ ] **Step 2: 创建主题状态**

创建 `frontend/src/stores/themeStore.ts`:

```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type ThemeMode = 'system' | 'light' | 'dark';

interface ThemeState {
  mode: ThemeMode;
  setMode: (mode: ThemeMode) => void;
  getEffectiveTheme: () => 'light' | 'dark';
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      mode: 'system',
      setMode: (mode) => set({ mode }),
      getEffectiveTheme: () => {
        const { mode } = get();
        if (mode === 'system') {
          return window.matchMedia('(prefers-color-scheme: dark)').matches
            ? 'dark'
            : 'light';
        }
        return mode;
      },
    }),
    {
      name: 'theme-storage',
    }
  )
);
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/stores/
git commit -m "feat: add Zustand stores for auth and theme"
```

---

## Task 6: 国际化配置

**Files:**
- Create: `frontend/src/i18n.ts`
- Create: `frontend/src/locales/zh-CN/common.json`
- Create: `frontend/src/locales/zh-CN/login.json`
- Create: `frontend/src/locales/zh-CN/models.json`
- Create: `frontend/src/locales/zh-CN/users.json`
- Create: `frontend/src/locales/zh-CN/apiKeys.json`
- Create: `frontend/src/locales/en-US/common.json`
- Create: `frontend/src/locales/en-US/login.json`
- Create: `frontend/src/locales/en-US/models.json`
- Create: `frontend/src/locales/en-US/users.json`
- Create: `frontend/src/locales/en-US/apiKeys.json`

- [ ] **Step 1: 创建 i18n 配置**

创建 `frontend/src/i18n.ts`:

```typescript
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// 中文
import zhCNCommon from './locales/zh-CN/common.json';
import zhCNLogin from './locales/zh-CN/login.json';
import zhCNModels from './locales/zh-CN/models.json';
import zhCNUsers from './locales/zh-CN/users.json';
import zhCNApiKeys from './locales/zh-CN/apiKeys.json';

// 英文
import enUSCommon from './locales/en-US/common.json';
import enUSLogin from './locales/en-US/login.json';
import enUSModels from './locales/en-US/models.json';
import enUSUsers from './locales/en-US/users.json';
import enUSApiKeys from './locales/en-US/apiKeys.json';

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
        apiKeys: zhCNApiKeys,
      },
      'en-US': {
        common: enUSCommon,
        login: enUSLogin,
        models: enUSModels,
        users: enUSUsers,
        apiKeys: enUSApiKeys,
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

- [ ] **Step 2: 创建中文翻译文件**

创建 `frontend/src/locales/zh-CN/common.json`:

```json
{
  "app": {
    "title": "LLM Gateway",
    "copyright": "© 2024 LLM Gateway"
  },
  "actions": {
    "add": "新增",
    "edit": "编辑",
    "delete": "删除",
    "save": "保存",
    "cancel": "取消",
    "search": "搜索",
    "reset": "重置",
    "submit": "提交",
    "confirm": "确认",
    "copy": "复制",
    "enable": "启用",
    "disable": "禁用",
    "resetPassword": "重置密码"
  },
  "status": {
    "enabled": "启用",
    "disabled": "禁用"
  },
  "confirm": {
    "delete": "确定要删除吗？",
    "enable": "确定要启用吗？",
    "disable": "确定要禁用吗？"
  },
  "message": {
    "success": "操作成功",
    "error": "操作失败",
    "copied": "已复制到剪贴板"
  },
  "theme": {
    "system": "跟随系统",
    "light": "亮色",
    "dark": "暗色"
  },
  "language": {
    "zhCN": "简体中文",
    "enUS": "English"
  },
  "user": {
    "logout": "退出登录"
  }
}
```

创建 `frontend/src/locales/zh-CN/login.json`:

```json
{
  "title": "LLM Gateway",
  "subtitle": "欢迎使用 LLM Gateway",
  "username": "用户名",
  "password": "密码",
  "rememberMe": "记住我",
  "submit": "登录",
  "error": {
    "title": "登录失败",
    "message": "用户名或密码错误"
  },
  "validation": {
    "usernameRequired": "请输入用户名",
    "passwordRequired": "请输入密码"
  }
}
```

创建 `frontend/src/locales/zh-CN/models.json`:

```json
{
  "title": "模型中心",
  "channelList": "渠道列表",
  "modelList": "模型列表",
  "addChannel": "新增渠道",
  "addModel": "新增模型",
  "channel": {
    "name": "渠道名称",
    "code": "渠道代码",
    "type": "渠道类型",
    "baseUrl": "API 地址",
    "status": "状态"
  },
  "model": {
    "name": "模型名称",
    "code": "模型代码",
    "type": "模型类型",
    "provider": "所属渠道",
    "status": "状态"
  },
  "type": {
    "OPENAI": "OpenAI",
    "ANTHROPIC": "Anthropic",
    "GOOGLE": "Google",
    "AZURE": "Azure",
    "CUSTOM": "自定义",
    "CHAT": "对话",
    "COMPLETION": "补全",
    "EMBEDDING": "向量",
    "IMAGE": "图像",
    "AUDIO": "音频"
  }
}
```

创建 `frontend/src/locales/zh-CN/users.json`:

```json
{
  "title": "用户管理",
  "userList": "用户列表",
  "apiKeyList": "API Key 列表",
  "addUser": "新增用户",
  "addApiKey": "新增 API Key",
  "user": {
    "username": "用户名",
    "email": "邮箱",
    "role": "角色",
    "status": "状态",
    "createdAt": "创建时间"
  },
  "apiKey": {
    "name": "名称",
    "key": "Key 值",
    "status": "状态",
    "createdAt": "创建时间",
    "copySuccess": "API Key 已复制",
    "createSuccess": "创建成功，请立即复制保存 Key 值"
  },
  "role": {
    "ADMIN": "管理员",
    "USER": "普通用户"
  }
}
```

创建 `frontend/src/locales/zh-CN/apiKeys.json`:

```json
{
  "title": "我的 API Key",
  "add": "新增 API Key",
  "name": "名称",
  "key": "Key 值",
  "status": "状态",
  "createdAt": "创建时间",
  "copySuccess": "API Key 已复制",
  "createSuccess": "创建成功，请立即复制保存 Key 值",
  "confirmDelete": "确定要删除此 API Key 吗？"
}
```

- [ ] **Step 3: 创建英文翻译文件**

创建 `frontend/src/locales/en-US/common.json`:

```json
{
  "app": {
    "title": "LLM Gateway",
    "copyright": "© 2024 LLM Gateway"
  },
  "actions": {
    "add": "Add",
    "edit": "Edit",
    "delete": "Delete",
    "save": "Save",
    "cancel": "Cancel",
    "search": "Search",
    "reset": "Reset",
    "submit": "Submit",
    "confirm": "Confirm",
    "copy": "Copy",
    "enable": "Enable",
    "disable": "Disable",
    "resetPassword": "Reset Password"
  },
  "status": {
    "enabled": "Enabled",
    "disabled": "Disabled"
  },
  "confirm": {
    "delete": "Are you sure you want to delete?",
    "enable": "Are you sure you want to enable?",
    "disable": "Are you sure you want to disable?"
  },
  "message": {
    "success": "Operation successful",
    "error": "Operation failed",
    "copied": "Copied to clipboard"
  },
  "theme": {
    "system": "System",
    "light": "Light",
    "dark": "Dark"
  },
  "language": {
    "zhCN": "简体中文",
    "enUS": "English"
  },
  "user": {
    "logout": "Logout"
  }
}
```

创建 `frontend/src/locales/en-US/login.json`:

```json
{
  "title": "LLM Gateway",
  "subtitle": "Welcome to LLM Gateway",
  "username": "Username",
  "password": "Password",
  "rememberMe": "Remember me",
  "submit": "Login",
  "error": {
    "title": "Login failed",
    "message": "Invalid username or password"
  },
  "validation": {
    "usernameRequired": "Please enter username",
    "passwordRequired": "Please enter password"
  }
}
```

创建 `frontend/src/locales/en-US/models.json`:

```json
{
  "title": "Model Center",
  "channelList": "Channel List",
  "modelList": "Model List",
  "addChannel": "Add Channel",
  "addModel": "Add Model",
  "channel": {
    "name": "Channel Name",
    "code": "Channel Code",
    "type": "Channel Type",
    "baseUrl": "API URL",
    "status": "Status"
  },
  "model": {
    "name": "Model Name",
    "code": "Model Code",
    "type": "Model Type",
    "provider": "Provider",
    "status": "Status"
  },
  "type": {
    "OPENAI": "OpenAI",
    "ANTHROPIC": "Anthropic",
    "GOOGLE": "Google",
    "AZURE": "Azure",
    "CUSTOM": "Custom",
    "CHAT": "Chat",
    "COMPLETION": "Completion",
    "EMBEDDING": "Embedding",
    "IMAGE": "Image",
    "AUDIO": "Audio"
  }
}
```

创建 `frontend/src/locales/en-US/users.json`:

```json
{
  "title": "User Management",
  "userList": "User List",
  "apiKeyList": "API Key List",
  "addUser": "Add User",
  "addApiKey": "Add API Key",
  "user": {
    "username": "Username",
    "email": "Email",
    "role": "Role",
    "status": "Status",
    "createdAt": "Created At"
  },
  "apiKey": {
    "name": "Name",
    "key": "Key Value",
    "status": "Status",
    "createdAt": "Created At",
    "copySuccess": "API Key copied",
    "createSuccess": "Created successfully, please copy and save the key immediately"
  },
  "role": {
    "ADMIN": "Admin",
    "USER": "User"
  }
}
```

创建 `frontend/src/locales/en-US/apiKeys.json`:

```json
{
  "title": "My API Keys",
  "add": "Add API Key",
  "name": "Name",
  "key": "Key Value",
  "status": "Status",
  "createdAt": "Created At",
  "copySuccess": "API Key copied",
  "createSuccess": "Created successfully, please copy and save the key immediately",
  "confirmDelete": "Are you sure you want to delete this API Key?"
}
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/i18n.ts frontend/src/locales/
git commit -m "feat: add i18n configuration with zh-CN and en-US translations"
```

---

## Task 7: 路由配置

**Files:**
- Create: `frontend/src/router/index.tsx`
- Create: `frontend/src/router/guards.tsx`

- [ ] **Step 1: 创建路由配置**

创建 `frontend/src/router/index.tsx`:

```typescript
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AuthGuard, RoleGuard } from './guards';
import Login from '@/pages/Login';
import AdminModels from '@/pages/admin/Models';
import AdminUsers from '@/pages/admin/Users';
import AdminSettings from '@/pages/admin/Settings';
import UserModels from '@/pages/user/Models';
import UserApiKeys from '@/pages/user/ApiKeys';
import UserSettings from '@/pages/user/Settings';

export const router = createBrowserRouter([
  // 公共路由
  {
    path: '/login',
    element: <Login />,
  },

  // 管理员路由
  {
    path: '/admin',
    element: (
      <AuthGuard>
        <RoleGuard allowedRoles={['ADMIN']}>
          <AdminLayout />
        </RoleGuard>
      </AuthGuard>
    ),
    children: [
      { index: true, element: <Navigate to="/admin/models" replace /> },
      { path: 'models', element: <AdminModels /> },
      { path: 'users', element: <AdminUsers /> },
      { path: 'settings', element: <AdminSettings /> },
    ],
  },

  // 普通用户路由
  {
    path: '/user',
    element: (
      <AuthGuard>
        <UserLayout />
      </AuthGuard>
    ),
    children: [
      { index: true, element: <Navigate to="/user/models" replace /> },
      { path: 'models', element: <UserModels /> },
      { path: 'api-keys', element: <UserApiKeys /> },
      { path: 'settings', element: <UserSettings /> },
    ],
  },

  // 默认重定向
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '*', element: <Navigate to="/login" replace /> },
]);

// 临时占位组件，后续实现
function AdminLayout({ children }: { children?: React.ReactNode }) {
  return <div>Admin Layout (to be implemented)</div>;
}

function UserLayout({ children }: { children?: React.ReactNode }) {
  return <div>User Layout (to be implemented)</div>;
}

export default router;
```

- [ ] **Step 2: 创建路由守卫**

创建 `frontend/src/router/guards.tsx`:

```typescript
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import type { UserRole } from '@/types/user';

interface AuthGuardProps {
  children: React.ReactNode;
}

/** 认证守卫：检查是否已登录 */
export function AuthGuard({ children }: AuthGuardProps) {
  const { isAuthenticated } = useAuthStore();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}

interface RoleGuardProps {
  children: React.ReactNode;
  allowedRoles: UserRole[];
}

/** 角色守卫：检查用户角色 */
export function RoleGuard({ children, allowedRoles }: RoleGuardProps) {
  const { user } = useAuthStore();
  const location = useLocation();

  if (user && !allowedRoles.includes(user.role)) {
    // 非管理员重定向到用户页面
    return <Navigate to="/user/models" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}
```

- [ ] **Step 3: 更新 App.tsx 使用路由**

更新 `frontend/src/App.tsx`:

```typescript
import { RouterProvider } from 'react-router-dom';
import { router } from '@/router';
import '@/i18n';

function App() {
  return <RouterProvider router={router} />;
}

export default App;
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/router/ frontend/src/App.tsx
git commit -m "feat: add router configuration with auth and role guards"
```

---

由于计划篇幅较长，我将在下一个任务中继续布局组件和页面的实现。当前已完成：

1. 项目初始化
2. TypeScript 类型定义
3. API 客户端
4. TanStack Query Hooks
5. 状态管理
6. 国际化配置
7. 路由配置

后续任务将包括：
- Task 8: 布局组件
- Task 9: 登录页
- Task 10: 管理员模型中心页
- Task 11: 管理员用户管理页
- Task 12: 管理员个人设置页
- Task 13: 用户模型查看页
- Task 14: 用户 API Key 管理页
- Task 15: 用户个人设置页
- Task 16: 构建部署配置
