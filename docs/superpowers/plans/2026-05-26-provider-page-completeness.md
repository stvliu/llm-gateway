# 供应商页面功能完善实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将供应商页面从基础信息展示补全为完整的供给管理中心，支持管理员 CRUD 和普通用户只读两种视角。

**Architecture:** 渐进式扩展现有抽屉架构，在 ProviderManagementDrawer 中增加"渠道"和"模型规格" tab，渠道 tab 内嵌套端点+凭证管理。后端补全 ModelSpec CRUD API。

**Tech Stack:** Java 21 + Spring Boot 3.5.x + JPA（后端），React + TypeScript + Ant Design + React Query + i18next（前端）

---

## 文件结构

### 后端新增/修改

| 文件 | 操作 | 职责 |
|------|------|------|
| `adapter/api/ModelSpecController.java` | 新增 | 模型规格 REST 端点 |
| `application/modelspec/ModelSpecService.java` | 新增 | 模型规格管理服务接口 |
| `application/modelspec/ModelSpecServiceImpl.java` | 新增 | 模型规格管理服务实现 |
| `application/modelspec/dto/ModelSpecCreateRequest.java` | 新增 | 创建模型规格请求 DTO |
| `application/modelspec/dto/ModelSpecUpdateRequest.java` | 新增 | 更新模型规格请求 DTO |
| `application/modelspec/dto/ModelSpecResponse.java` | 新增 | 模型规格响应 DTO |
| `application/modelspec/dto/ModelSpecQueryRequest.java` | 新增 | 查询模型规格请求 DTO |

### 前端新增/修改

| 文件 | 操作 | 职责 |
|------|------|------|
| `pages/Providers/ProviderChannelTab.tsx` | 新增 | 渠道 tab（含端点+凭证展开行） |
| `pages/Providers/ProviderModelSpecTab.tsx` | 新增 | 模型规格 tab |
| `pages/Providers/ProvidersTableView.tsx` | 重写 | 表格视图实现 |
| `pages/Providers/ChannelFormModal.tsx` | 新增 | 渠道创建/编辑弹窗 |
| `pages/Providers/ChannelEndpointFormModal.tsx` | 新增 | 端点创建弹窗 |
| `pages/Providers/CredentialFormModal.tsx` | 新增 | 凭证创建弹窗 |
| `pages/Providers/ModelSpecFormModal.tsx` | 新增 | 模型规格创建/编辑弹窗 |
| `pages/Providers/ConnectivityTestPanel.tsx` | 新增 | 连通性测试面板 |
| `services/api/modelSpec.ts` | 新增 | 模型规格 API 调用 |
| `services/query/useModelSpecs.ts` | 新增 | 模型规格 React Query hooks |
| `types/modelSpec.ts` | 新增 | 模型规格类型定义 |
| `pages/Providers/ProviderManagementDrawer.tsx` | 修改 | 扩展为 3-tab 体系 |
| `pages/Providers/ProviderBasicInfoTab.tsx` | 修改 | 补全 description + 连通性测试 |
| `pages/Providers/ProviderCreateModal.tsx` | 修改 | 补全 description 字段 |
| `pages/Providers/ProviderCardView.tsx` | 修改 | 增加渠道数/模型数统计 |
| `pages/Providers/ProviderCard.tsx` | 修改 | 增加渠道数/模型数标签 |
| `pages/Providers/index.tsx` | 修改 | 增加状态筛选 + 视图切换器 |
| `services/api/provider.ts` | 修改 | 增加连通性测试 API |
| `services/query/useProviders.ts` | 修改 | 增加连通性测试 hook |
| `services/query/index.ts` | 修改 | 导出 useModelSpecs |
| `services/api/index.ts` | 修改 | 导出 modelSpecApi |
| `types/provider.ts` | 修改 | 补全 CreateProviderRequest.description |
| `locales/zh-CN/providers.json` | 修改 | 补全新增文案 |
| `locales/en-US/providers.json` | 修改 | 补全新增文案 |

---

## Task 1: 后端 — ModelSpec CRUD API

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/modelspec/ModelSpecService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/modelspec/ModelSpecServiceImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/modelspec/dto/ModelSpecCreateRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/modelspec/dto/ModelSpecUpdateRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/modelspec/dto/ModelSpecResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/modelspec/dto/ModelSpecQueryRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ModelSpecController.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ModelSpecControllerTest.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/modelspec/ModelSpecServiceImplTest.java`

- [ ] **Step 1: 创建 ModelSpec DTO 类**

创建 `ModelSpecCreateRequest.java`:

```java
package com.codingas.gateway.application.modelspec.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ModelSpecCreateRequest {
    @NotNull(message = "供应商 ID 不能为空")
    private Long providerId;

    @NotBlank(message = "供应商模型 ID 不能为空")
    @Size(max = 128)
    private String providerModelId;

    @Size(max = 128)
    private String displayName;

    @Size(max = 64)
    private String modelFamily;

    private Integer contextWindow;

    private Integer maxInputTokens;

    private Integer maxOutputTokens;

    private Map<String, Boolean> capabilities;

    private List<String> modalities;

    private Integer priority;

    private Integer weight;
}
```

创建 `ModelSpecUpdateRequest.java`:

```java
package com.codingas.gateway.application.modelspec.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ModelSpecUpdateRequest {
    @Size(max = 128)
    private String providerModelId;

    @Size(max = 128)
    private String displayName;

    @Size(max = 64)
    private String modelFamily;

    private Integer contextWindow;

    private Integer maxInputTokens;

    private Integer maxOutputTokens;

    private Map<String, Boolean> capabilities;

    private List<String> modalities;

    private Integer priority;

    private Integer weight;

    private String state;
}
```

创建 `ModelSpecResponse.java`:

```java
package com.codingas.gateway.application.modelspec.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class ModelSpecResponse {
    private Long id;
    private Long providerId;
    private String providerModelId;
    private String displayName;
    private String modelFamily;
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private Map<String, Boolean> capabilities;
    private List<String> modalities;
    private String state;
    private Integer priority;
    private Integer weight;
    private Instant createdAt;
    private Instant updatedAt;
}
```

创建 `ModelSpecQueryRequest.java`:

```java
package com.codingas.gateway.application.modelspec.dto;

import lombok.Data;

@Data
public class ModelSpecQueryRequest {
    private Long providerId;
    private String keyword;
    private String state;
}
```

- [ ] **Step 2: 创建 ModelSpecService 接口和实现**

创建 `ModelSpecService.java`:

```java
package com.codingas.gateway.application.modelspec;

import com.codingas.gateway.application.modelspec.dto.ModelSpecCreateRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecQueryRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecResponse;
import com.codingas.gateway.application.modelspec.dto.ModelSpecUpdateRequest;

import java.util.List;

public interface ModelSpecService {
    ModelSpecResponse create(ModelSpecCreateRequest request);
    ModelSpecResponse getById(Long id);
    List<ModelSpecResponse> query(ModelSpecQueryRequest request);
    ModelSpecResponse update(Long id, ModelSpecUpdateRequest request);
    void delete(Long id);
    ModelSpecResponse setEnabled(Long id, boolean enabled);
}
```

创建 `ModelSpecServiceImpl.java`:

```java
package com.codingas.gateway.application.modelspec;

import com.codingas.gateway.application.modelspec.dto.ModelSpecCreateRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecQueryRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecResponse;
import com.codingas.gateway.application.modelspec.dto.ModelSpecUpdateRequest;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelSpecServiceImpl implements ModelSpecService {

    private final ModelSpecGateway modelSpecGateway;

    @Override
    @Transactional
    public ModelSpecResponse create(ModelSpecCreateRequest request) {
        ModelSpec spec = new ModelSpec();
        spec.setProviderId(request.getProviderId());
        spec.setProviderModelId(request.getProviderModelId());
        spec.setDisplayName(request.getDisplayName());
        spec.setModelFamily(request.getModelFamily());
        spec.setContextWindow(request.getContextWindow());
        spec.setMaxInputTokens(request.getMaxInputTokens());
        spec.setMaxOutputTokens(request.getMaxOutputTokens());
        spec.setCapabilities(request.getCapabilities());
        spec.setModalities(request.getModalities());
        spec.setPriority(request.getPriority());
        spec.setWeight(request.getWeight());
        spec.setState(ModelSpecState.ACTIVE);

        ModelSpec saved = modelSpecGateway.save(spec);
        log.info("Created model spec: id={}, providerModelId={}", saved.getId(), saved.getProviderModelId());
        return toResponse(saved);
    }

    @Override
    public ModelSpecResponse getById(Long id) {
        ModelSpec spec = modelSpecGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));
        return toResponse(spec);
    }

    @Override
    public List<ModelSpecResponse> query(ModelSpecQueryRequest request) {
        List<ModelSpec> specs;
        if (request.getProviderId() != null) {
            specs = modelSpecGateway.findByProviderId(request.getProviderId());
        } else {
            specs = modelSpecGateway.findAll();
        }

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            specs = specs.stream()
                .filter(s -> (s.getProviderModelId() != null && s.getProviderModelId().toLowerCase().contains(keyword))
                    || (s.getDisplayName() != null && s.getDisplayName().toLowerCase().contains(keyword)))
                .toList();
        }

        if (request.getState() != null && !request.getState().isBlank()) {
            ModelSpecState state = ModelSpecState.valueOf(request.getState());
            specs = specs.stream().filter(s -> s.getState().equals(state)).toList();
        }

        return specs.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ModelSpecResponse update(Long id, ModelSpecUpdateRequest request) {
        ModelSpec spec = modelSpecGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));

        if (request.getProviderModelId() != null) spec.setProviderModelId(request.getProviderModelId());
        if (request.getDisplayName() != null) spec.setDisplayName(request.getDisplayName());
        if (request.getModelFamily() != null) spec.setModelFamily(request.getModelFamily());
        if (request.getContextWindow() != null) spec.setContextWindow(request.getContextWindow());
        if (request.getMaxInputTokens() != null) spec.setMaxInputTokens(request.getMaxInputTokens());
        if (request.getMaxOutputTokens() != null) spec.setMaxOutputTokens(request.getMaxOutputTokens());
        if (request.getCapabilities() != null) spec.setCapabilities(request.getCapabilities());
        if (request.getModalities() != null) spec.setModalities(request.getModalities());
        if (request.getPriority() != null) spec.setPriority(request.getPriority());
        if (request.getWeight() != null) spec.setWeight(request.getWeight());
        if (request.getState() != null) spec.setState(ModelSpecState.valueOf(request.getState()));

        return toResponse(modelSpecGateway.save(spec));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ModelSpec spec = modelSpecGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));
        modelSpecGateway.delete(spec);
        log.info("Deleted model spec: id={}", id);
    }

    @Override
    @Transactional
    public ModelSpecResponse setEnabled(Long id, boolean enabled) {
        ModelSpec spec = modelSpecGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", id));
        spec.setState(enabled ? ModelSpecState.ACTIVE : ModelSpecState.INACTIVE);
        return toResponse(modelSpecGateway.save(spec));
    }

    private ModelSpecResponse toResponse(ModelSpec spec) {
        ModelSpecResponse resp = new ModelSpecResponse();
        resp.setId(spec.getId());
        resp.setProviderId(spec.getProviderId());
        resp.setProviderModelId(spec.getProviderModelId());
        resp.setDisplayName(spec.getDisplayName());
        resp.setModelFamily(spec.getModelFamily());
        resp.setContextWindow(spec.getContextWindow());
        resp.setMaxInputTokens(spec.getMaxInputTokens());
        resp.setMaxOutputTokens(spec.getMaxOutputTokens());
        resp.setCapabilities(spec.getCapabilities());
        resp.setModalities(spec.getModalities());
        resp.setState(spec.getState() != null ? spec.getState().name() : null);
        resp.setPriority(spec.getPriority());
        resp.setWeight(spec.getWeight());
        resp.setCreatedAt(spec.getCreatedAt());
        resp.setUpdatedAt(spec.getUpdatedAt());
        return resp;
    }
}
```

- [ ] **Step 3: 创建 ModelSpecController**

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.modelspec.ModelSpecService;
import com.codingas.gateway.application.modelspec.dto.ModelSpecCreateRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecQueryRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecResponse;
import com.codingas.gateway.application.modelspec.dto.ModelSpecUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/model-specs")
@RequiredArgsConstructor
public class ModelSpecController {

    private final ModelSpecService modelSpecService;

    @PostMapping
    public ResponseEntity<ModelSpecResponse> create(@Valid @RequestBody ModelSpecCreateRequest request) {
        return ResponseEntity.ok(modelSpecService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModelSpecResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(modelSpecService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ModelSpecResponse>> query(ModelSpecQueryRequest request) {
        return ResponseEntity.ok(modelSpecService.query(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModelSpecResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody ModelSpecUpdateRequest request) {
        return ResponseEntity.ok(modelSpecService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        modelSpecService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/state")
    public ResponseEntity<ModelSpecResponse> setEnabled(@PathVariable Long id,
                                                         @RequestParam boolean enabled) {
        return ResponseEntity.ok(modelSpecService.setEnabled(id, enabled));
    }
}
```

- [ ] **Step 4: 编写后端单元测试**

创建 `ModelSpecControllerTest.java` 和 `ModelSpecServiceImplTest.java`，参照 `ChannelCredentialControllerTest.java` 的模式，使用 Mockito + AssertJ。

- [ ] **Step 5: 运行测试确认通过**

Run: `./mvnw test -pl gateway-boot -Dtest="ModelSpec*Test" -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ModelSpecController.java \
  gateway-boot/src/main/java/com/codingas/gateway/application/modelspec/ \
  gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ModelSpecControllerTest.java \
  gateway-boot/src/test/java/com/codingas/gateway/application/modelspec/ModelSpecServiceImplTest.java
git commit -m "feat: 新增 ModelSpec CRUD API（Controller + Service + DTO）"
```

---

## Task 2: 前端 — 模型规格 API 层和类型定义

**Files:**
- Create: `gateway-console/src/types/modelSpec.ts`
- Create: `gateway-console/src/services/api/modelSpec.ts`
- Create: `gateway-console/src/services/query/useModelSpecs.ts`
- Modify: `gateway-console/src/services/api/index.ts`
- Modify: `gateway-console/src/services/query/index.ts`

- [ ] **Step 1: 创建模型规格类型定义**

创建 `types/modelSpec.ts`:

```typescript
export type ModelSpecState = 'ACTIVE' | 'INACTIVE';

export interface ModelSpec {
  id: number;
  providerId: number;
  providerModelId: string;
  displayName?: string;
  modelFamily?: string;
  contextWindow?: number;
  maxInputTokens?: number;
  maxOutputTokens?: number;
  capabilities?: Record<string, boolean>;
  modalities?: string[];
  state: ModelSpecState;
  priority?: number;
  weight?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateModelSpecRequest {
  providerId: number;
  providerModelId: string;
  displayName?: string;
  modelFamily?: string;
  contextWindow?: number;
  maxInputTokens?: number;
  maxOutputTokens?: number;
  capabilities?: Record<string, boolean>;
  modalities?: string[];
  priority?: number;
  weight?: number;
}

export interface UpdateModelSpecRequest {
  providerModelId?: string;
  displayName?: string;
  modelFamily?: string;
  contextWindow?: number;
  maxInputTokens?: number;
  maxOutputTokens?: number;
  capabilities?: Record<string, boolean>;
  modalities?: string[];
  priority?: number;
  weight?: number;
  state?: ModelSpecState;
}
```

- [ ] **Step 2: 创建模型规格 API 调用**

创建 `services/api/modelSpec.ts`:

```typescript
import { api } from './client';
import type { ModelSpec, CreateModelSpecRequest, UpdateModelSpecRequest } from '@/types/modelSpec';

export const modelSpecApi = {
  list: (params?: Record<string, unknown>) =>
    api.get<ModelSpec[]>('/model-specs', { params }),

  get: (id: number) =>
    api.get<ModelSpec>(`/model-specs/${id}`),

  create: (data: CreateModelSpecRequest) =>
    api.post<ModelSpec>('/model-specs', data),

  update: (id: number, data: UpdateModelSpecRequest) =>
    api.put<ModelSpec>(`/model-specs/${id}`, data),

  delete: (id: number) =>
    api.delete<void>(`/model-specs/${id}`),

  setEnabled: (id: number, enabled: boolean) =>
    api.patch<ModelSpec>(`/model-specs/${id}/state`, null, { params: { enabled } }),
};
```

- [ ] **Step 3: 创建模型规格 React Query hooks**

创建 `services/query/useModelSpecs.ts`:

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { modelSpecApi } from '@/services/api/modelSpec';
import type { CreateModelSpecRequest, UpdateModelSpecRequest } from '@/types/modelSpec';

export const modelSpecKeys = {
  all: ['modelSpecs'] as const,
  lists: () => [...modelSpecKeys.all, 'list'] as const,
  list: (params?: Record<string, unknown>) => [...modelSpecKeys.lists(), params] as const,
  details: () => [...modelSpecKeys.all, 'detail'] as const,
  detail: (id: number) => [...modelSpecKeys.details(), id] as const,
};

export function useModelSpecs(providerId?: number) {
  return useQuery({
    queryKey: modelSpecKeys.list({ providerId }),
    queryFn: () => modelSpecApi.list({ providerId }),
  });
}

export function useModelSpec(id: number) {
  return useQuery({
    queryKey: modelSpecKeys.detail(id),
    queryFn: () => modelSpecApi.get(id),
    enabled: id > 0,
  });
}

export function useCreateModelSpec() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateModelSpecRequest) => modelSpecApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.lists() });
    },
  });
}

export function useUpdateModelSpec() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateModelSpecRequest }) =>
      modelSpecApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.lists() });
    },
  });
}

export function useDeleteModelSpec() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => modelSpecApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.lists() });
    },
  });
}

export function useSetEnabledModelSpec() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      modelSpecApi.setEnabled(id, enabled),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: modelSpecKeys.lists() });
    },
  });
}
```

- [ ] **Step 4: 更新 index 导出**

在 `services/api/index.ts` 末尾添加:
```typescript
export { modelSpecApi } from './modelSpec';
```

在 `services/query/index.ts` 末尾添加:
```typescript
export * from './useModelSpecs';
```

- [ ] **Step 5: 提交**

```bash
git add gateway-console/src/types/modelSpec.ts \
  gateway-console/src/services/api/modelSpec.ts \
  gateway-console/src/services/query/useModelSpecs.ts \
  gateway-console/src/services/api/index.ts \
  gateway-console/src/services/query/index.ts
git commit -m "feat(console): 新增 ModelSpec API 层和 React Query hooks"
```

---

## Task 3: 前端 — Provider API 补全连通性测试

**Files:**
- Modify: `gateway-console/src/services/api/provider.ts`
- Modify: `gateway-console/src/services/query/useProviders.ts`
- Modify: `gateway-console/src/types/provider.ts`

- [ ] **Step 1: 在 types/provider.ts 中补全类型**

在 `Provider` 接口中添加 `description` 字段（如缺失），在 `CreateProviderRequest` 中添加 `description?: string`。

添加连通性测试相关类型:

```typescript
export interface ConnectivityTestRequest {
  protocolName: string;
  baseUrl?: string;
  apiKey: string;
  model?: string;
}

export interface ConnectivityTestLevelResult {
  success: boolean;
  message: string;
  latencyMs: number | null;
  errorType: string | null;
  models: string[] | null;
}

export interface ConnectivityTestResult {
  success: boolean;
  message: string;
  models: string[] | null;
  level1: ConnectivityTestLevelResult | null;
  level2: ConnectivityTestLevelResult | null;
  totalLatencyMs: number;
}
```

- [ ] **Step 2: 在 services/api/provider.ts 中添加连通性测试 API**

```typescript
import type { ConnectivityTestRequest, ConnectivityTestResult } from '@/types/provider';

// 在 providerApi 对象中添加:
testConnectivity: (data: ConnectivityTestRequest) =>
  api.post<ConnectivityTestResult>('/providers/test-connectivity', data),
```

- [ ] **Step 3: 在 services/query/useProviders.ts 中添加连通性测试 hook**

```typescript
import type { ConnectivityTestRequest } from '@/types/provider';

export function useTestConnectivity() {
  return useMutation({
    mutationFn: (data: ConnectivityTestRequest) => providerApi.testConnectivity(data),
  });
}
```

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/types/provider.ts \
  gateway-console/src/services/api/provider.ts \
  gateway-console/src/services/query/useProviders.ts
git commit -m "feat(console): 补全 Provider 连通性测试 API 和类型定义"
```

---

## Task 4: 前端 — 供应商列表页增强（状态筛选 + 视图切换 + 表格视图）

**Files:**
- Modify: `gateway-console/src/pages/Providers/index.tsx`
- Rewrite: `gateway-console/src/pages/Providers/ProvidersTableView.tsx`

- [ ] **Step 1: 修改 index.tsx — 增加状态筛选和视图切换**

在 `Providers` 组件中:
1. 新增 `viewMode` state（`'card' | 'table'`）
2. 新增 `stateFilter` state（`string | undefined`）
3. 搜索栏右侧增加 `Select` 状态筛选下拉和 `Segmented` 视图切换
4. 根据 `viewMode` 条件渲染 `ProviderCardView` 或 `ProvidersTableView`
5. 状态筛选逻辑：`filtered` 先按 keyword 过滤，再按 state 过滤

- [ ] **Step 2: 实现 ProvidersTableView.tsx**

使用 Ant Design `Table` 组件:
- 列定义：品牌标识（ProviderIcon + code）、供应商名称、状态（Tag）、优先级、创建时间、操作
- 操作列：查看、编辑、删除（权限控制）
- 行点击打开详情抽屉
- 空状态提示

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Providers/index.tsx \
  gateway-console/src/pages/Providers/ProvidersTableView.tsx
git commit -m "feat(console): 供应商列表增加状态筛选和表格视图"
```

---

## Task 5: 前端 — ProviderManagementDrawer 扩展为 3-tab 体系

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx`
- Create: `gateway-console/src/pages/Providers/ProviderChannelTab.tsx`
- Create: `gateway-console/src/pages/Providers/ProviderModelSpecTab.tsx`

- [ ] **Step 1: 修改 ProviderManagementDrawer.tsx**

关键变更:
1. 宽度从 560 改为 720
2. `defaultTab` 类型扩展为 `'basic' | 'channels' | 'modelSpecs'`
3. tabs 数组增加 `channels` 和 `modelSpecs` 项
4. 引入 `ProviderChannelTab` 和 `ProviderModelSpecTab` 组件
5. 根据 `activeTab` 渲染对应 tab 内容
6. 编辑模式下只显示基本信息 tab（保持现有行为）
7. 普通用户（无 `PROVIDER_WRITE` 权限）隐藏编辑/删除按钮

tabs 配置:

```typescript
const tabs = [
  { key: 'basic', label: t('detail.basicInfo'), icon: <SettingOutlined /> },
  { key: 'channels', label: t('detail.channels', { defaultValue: '渠道' }), icon: <ApiOutlined /> },
  { key: 'modelSpecs', label: t('detail.modelSpecs', { defaultValue: '模型规格' }), icon: <RobotOutlined /> },
];
```

- [ ] **Step 2: 创建 ProviderChannelTab.tsx 骨架**

渠道 tab 组件，接收 `providerId` 和 `editing` props:
- 使用 `useChannels(providerId)` 获取渠道列表
- 渲染 `Table` 组件，列：名称、状态、端点数、凭证数、优先级、操作
- 渠道行可展开，展开后显示端点列表 + 凭证列表（Divider 分隔）
- 管理员可见创建/编辑/删除操作，普通用户只读

- [ ] **Step 3: 创建 ProviderModelSpecTab.tsx 骨架**

模型规格 tab 组件，接收 `providerId` 和 `editing` props:
- 使用 `useModelSpecs(providerId)` 获取模型规格列表
- 渲染 `Table` 组件，列：供应商模型 ID、显示名、模型族、上下文窗口、能力标签、状态、操作
- 能力标签用 `Tag` 组渲染
- 管理员可见创建/编辑/删除操作，普通用户只读

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx \
  gateway-console/src/pages/Providers/ProviderChannelTab.tsx \
  gateway-console/src/pages/Providers/ProviderModelSpecTab.tsx
git commit -m "feat(console): 供应商详情抽屉扩展为 3-tab 体系（基本信息+渠道+模型规格）"
```

---

## Task 6: 前端 — ProviderBasicInfoTab 补全 description + 连通性测试

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderBasicInfoTab.tsx`
- Create: `gateway-console/src/pages/Providers/ConnectivityTestPanel.tsx`
- Modify: `gateway-console/src/pages/Providers/ProviderCreateModal.tsx`

- [ ] **Step 1: 修改 ProviderBasicInfoTab.tsx**

变更:
1. 查看模式增加 `description` 字段显示
2. 编辑模式增加 `description` 文本域（`Input.TextArea`）
3. 新增"连通性测试"按钮（仅管理员可见），点击展开 `ConnectivityTestPanel`
4. 连通性测试需要输入 API Key（弹窗输入或内联表单）

- [ ] **Step 2: 创建 ConnectivityTestPanel.tsx**

连通性测试面板组件:
- 接收 `providerId`（品牌标识，用于确定协议）和可选的 `baseUrl`
- 表单：API Key 输入、协议选择（openai/anthropic）、Base URL（可选）、测试模型（可选）
- 提交调用 `useTestConnectivity` hook
- 结果展示：使用 `Collapse` 组件分两层
  - Panel 1：认证结果（成功/失败 + 延迟 + 错误信息）
  - Panel 2：模型可用性列表（每个模型一行：名称 + 状态 Tag）

- [ ] **Step 3: 修改 ProviderCreateModal.tsx — 补全 description 字段**

在自定义创建表单（`renderCustomForm`）中，在 `apiDocUrl` 之后增加:

```tsx
<Form.Item
  name="description"
  label={t('fields.description', { defaultValue: '描述' })}
>
  <Input.TextArea rows={3} placeholder={t('fields.descriptionPlaceholder', { defaultValue: '供应商描述信息' })} />
</Form.Item>
```

- [ ] **Step 4: 提交**

```bash
git add gateway-console/src/pages/Providers/ProviderBasicInfoTab.tsx \
  gateway-console/src/pages/Providers/ConnectivityTestPanel.tsx \
  gateway-console/src/pages/Providers/ProviderCreateModal.tsx
git commit -m "feat(console): 基本信息 tab 补全 description 和连通性测试"
```

---

## Task 7: 前端 — 渠道 Tab 完整实现（含端点 + 凭证展开行）

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderChannelTab.tsx`
- Create: `gateway-console/src/pages/Providers/ChannelFormModal.tsx`
- Create: `gateway-console/src/pages/Providers/ChannelEndpointFormModal.tsx`
- Create: `gateway-console/src/pages/Providers/CredentialFormModal.tsx`

- [ ] **Step 1: 实现 ChannelFormModal.tsx**

渠道创建/编辑弹窗，参照 `TeamFormModal.tsx` 模式:
- 表单字段：渠道名称（必填）、计费模式（Select: pay_per_call/subscription）、优先级、权重、超时、最大重试
- 编辑模式预填现有值
- 提交调用 `useCreateChannel` 或 `useUpdateChannel`

- [ ] **Step 2: 实现 ChannelEndpointFormModal.tsx**

端点创建弹窗:
- 表单字段：协议类型（Select: openai/anthropic）、Base URL（必填）
- 提交调用 `channelApi.addEndpoint`（需在 `services/api/channel.ts` 中补充此方法）

- [ ] **Step 3: 实现 CredentialFormModal.tsx**

凭证创建弹窗:
- 表单字段：API Key（Input.Password，必填）、名称、优先级、权重
- 提交调用 `useCreateChannelCredential`
- 创建成功后显示 API Key 前缀提示（仅此一次可见）

- [ ] **Step 4: 完善 ProviderChannelTab.tsx**

完整实现渠道 tab:
1. 渠道列表 Table（列：名称、状态 Tag、端点数、凭证数、优先级、操作）
2. 操作列：编辑（ChannelFormModal）、启用/禁用（确认弹窗 → `useSetEnabledChannel`）、删除（确认弹窗 → `useDeleteChannel`）
3. 创建按钮（ChannelFormModal）
4. 展开行（expandable）:
   - 上半部分：端点列表 Table（列：Base URL、协议 Tag、状态 Tag、操作）
   - 端点操作：启用/禁用、删除
   - 添加端点按钮（ChannelEndpointFormModal）
   - Divider 分隔
   - 下半部分：凭证列表 Table（列：API Key 脱敏、状态 Tag、最后验证时间、操作）
   - 凭证操作：测试（调用 `useTestChannelCredential`，显示结果）、启用/禁用、删除
   - 添加凭证按钮（CredentialFormModal）
5. 权限控制：无 `PROVIDER_WRITE` 时隐藏所有写操作按钮

- [ ] **Step 5: 补全 channel API 中缺失的方法**

在 `services/api/channel.ts` 中添加，同时在 `types/channel.ts` 中补全 `ChannelEndpointResponse` 类型:

```typescript
// types/channel.ts 新增:
export interface ChannelEndpointResponse {
  id: number;
  channelId: number;
  protocol: string;
  endpointUrl: string;
  state: string;
  createdAt: string;
  updatedAt: string;
}
```

```typescript
// services/api/channel.ts 新增:
addEndpoint: (channelId: number, data: { protocol: string; endpointUrl: string }) =>
  api.post<ChannelEndpointResponse>(`/channels/${channelId}/endpoints`, data),

removeEndpoint: (channelId: number, endpointId: number) =>
  api.delete<void>(`/channels/${channelId}/endpoints/${endpointId}`),

enableEndpoint: (channelId: number, endpointId: number) =>
  api.put<ChannelEndpointResponse>(`/channels/${channelId}/endpoints/${endpointId}/enable`),

disableEndpoint: (channelId: number, endpointId: number) =>
  api.put<ChannelEndpointResponse>(`/channels/${channelId}/endpoints/${endpointId}/disable`),
```

- [ ] **Step 6: 提交**

```bash
git add gateway-console/src/pages/Providers/ProviderChannelTab.tsx \
  gateway-console/src/pages/Providers/ChannelFormModal.tsx \
  gateway-console/src/pages/Providers/ChannelEndpointFormModal.tsx \
  gateway-console/src/pages/Providers/CredentialFormModal.tsx \
  gateway-console/src/services/api/channel.ts
git commit -m "feat(console): 渠道 tab 完整实现（渠道+端点+凭证管理）"
```

---

## Task 8: 前端 — 模型规格 Tab 完整实现

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderModelSpecTab.tsx`
- Create: `gateway-console/src/pages/Providers/ModelSpecFormModal.tsx`

- [ ] **Step 1: 实现 ModelSpecFormModal.tsx**

模型规格创建/编辑弹窗:
- 表单字段：供应商模型 ID（必填）、显示名、模型族、上下文窗口、最大输入/输出 Token、优先级、权重
- 编辑模式预填现有值
- 提交调用 `useCreateModelSpec` 或 `useUpdateModelSpec`

- [ ] **Step 2: 完善 ProviderModelSpecTab.tsx**

完整实现模型规格 tab:
1. 模型规格列表 Table（列：供应商模型 ID、显示名、模型族、上下文窗口、能力标签、状态、操作）
2. 能力标签渲染：遍历 `capabilities` 对象，值为 true 的 key 渲染为 `Tag`
3. 操作列：编辑（ModelSpecFormModal）、启用/禁用（确认弹窗 → `useSetEnabledModelSpec`）、删除（确认弹窗 → `useDeleteModelSpec`）
4. 创建按钮（ModelSpecFormModal）
5. 权限控制：无 `PROVIDER_WRITE` 时隐藏所有写操作按钮

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Providers/ProviderModelSpecTab.tsx \
  gateway-console/src/pages/Providers/ModelSpecFormModal.tsx
git commit -m "feat(console): 模型规格 tab 完整实现"
```

---

## Task 9: 前端 — ProviderCard 增强（渠道数/模型数统计）

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderCard.tsx`
- Modify: `gateway-console/src/pages/Providers/ProviderCardView.tsx`

- [ ] **Step 1: 修改 ProviderCard.tsx**

在卡片底部增加模型数统计:
- 使用 `useModelSpecs(provider.id)` 获取模型列表
- 在 Channels section 下方增加 Models section，显示模型数和前 3 个模型名 Tag

- [ ] **Step 2: 提交**

```bash
git add gateway-console/src/pages/Providers/ProviderCard.tsx
git commit -m "feat(console): 供应商卡片增加模型数统计"
```

---

## Task 10: 前端 — i18n 文案补全

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/providers.json`
- Modify: `gateway-console/src/locales/en-US/providers.json`

- [ ] **Step 1: 补全 zh-CN/providers.json**

新增文案:

```json
{
  "detail.channels": "渠道",
  "detail.modelSpecs": "模型规格",
  "detail.connectivityTest": "连通性测试",
  "detail.runTest": "运行测试",
  "detail.testResult": "测试结果",
  "detail.level1Auth": "认证检测",
  "detail.level2Model": "模型可用性",
  "detail.latency": "延迟",
  "detail.success": "成功",
  "detail.failed": "失败",
  "fields.description": "描述",
  "fields.descriptionPlaceholder": "供应商描述信息",
  "card.models": "模型",
  "card.noModels": "暂无模型",
  "channel.name": "渠道名称",
  "channel.billingMode": "计费模式",
  "channel.priority": "优先级",
  "channel.weight": "权重",
  "channel.timeout": "超时(ms)",
  "channel.maxRetries": "最大重试",
  "channel.create": "创建渠道",
  "channel.edit": "编辑渠道",
  "channel.endpoints": "端点",
  "channel.noEndpoints": "暂无端点",
  "channel.addEndpoint": "添加端点",
  "channel.endpointUrl": "Base URL",
  "channel.protocol": "协议",
  "credential.name": "凭证名称",
  "credential.apiKey": "API Key",
  "credential.add": "添加凭证",
  "credential.test": "测试",
  "credential.testSuccess": "测试成功",
  "credential.testFailed": "测试失败",
  "credential.prefix": "前缀",
  "credential.lastUsedAt": "最后使用",
  "credential.createdSuccess": "凭证创建成功",
  "credential.createdHint": "请立即保存此密钥，关闭后将无法再次查看",
  "modelSpec.providerModelId": "供应商模型 ID",
  "modelSpec.displayName": "显示名",
  "modelSpec.modelFamily": "模型族",
  "modelSpec.contextWindow": "上下文窗口",
  "modelSpec.capabilities": "能力",
  "modelSpec.create": "创建模型规格",
  "modelSpec.edit": "编辑模型规格",
  "stateFilter.all": "全部",
  "stateFilter.active": "启用",
  "stateFilter.inactive": "停用",
  "viewMode.card": "卡片视图",
  "viewMode.table": "表格视图"
}
```

- [ ] **Step 2: 补全 en-US/providers.json**

对应英文翻译。

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/locales/zh-CN/providers.json \
  gateway-console/src/locales/en-US/providers.json
git commit -m "feat(console): 补全供应商页面 i18n 文案"
```

---

## Task 11: 集成验证

- [ ] **Step 1: 启动后端，确认 ModelSpec API 可用**

Run: `./mvnw spring-boot:run -pl gateway-boot`

验证端点:
- `GET /api/v1/model-specs` 返回 200
- `POST /api/v1/model-specs` 创建成功
- `GET /api/v1/model-specs?providerId=1` 按供应商筛选

- [ ] **Step 2: 启动前端，验证页面功能**

Run: `cd gateway-console && pnpm dev`

验证:
1. 供应商列表页：状态筛选、视图切换正常
2. 表格视图：列显示正确、操作可用
3. 供应商详情抽屉：3 个 tab 切换正常
4. 渠道 tab：渠道列表、展开行（端点+凭证）、CRUD 操作
5. 模型规格 tab：列表、CRUD 操作
6. 基本信息 tab：description 字段、连通性测试
7. 普通用户视角：只读模式，无写操作按钮

- [ ] **Step 3: 修复发现的问题**

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "feat: 供应商页面功能完善完成（全功能补全）"
```
