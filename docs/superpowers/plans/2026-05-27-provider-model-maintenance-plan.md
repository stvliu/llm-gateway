# 供应商页面模型维护重构 - 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标**：将 ModelSpec 从按 Provider 挂载改为全局注册表，供应商详情移模型规格 tab，在渠道展开行中增加 Models 关联面板

**架构**：后端去掉 ModelSpec.providerId，新增 ChannelModel 的 REST API；前端去掉独立 ModelSpec tab，在渠道展开行中新增 Models tab

**Tech Stack**：Java 21 + Spring Boot 3.5.x (后端), React + Ant Design + React Query (前端), PostgreSQL (DB)

---

### Task 1: DB 迁移 — 去掉 model_specs.provider_id

**Files:**
- Create: `gateway-boot/src/main/resources/db/migration/V40__drop_model_specs_provider_id.sql`

- [ ] **Step 1: 编写 V40 迁移 SQL**

```sql
-- V40: Drop provider_id from model_specs, make it a global registry
-- 1. 合并相同 provider_model_id 的重复记录（保留 id 最小的那行）
WITH duplicates AS (
    SELECT provider_model_id, MIN(id) AS keep_id
    FROM model_specs
    GROUP BY provider_model_id
    HAVING COUNT(*) > 1
)
UPDATE channel_models cm
SET model_spec_id = d.keep_id
FROM duplicates d
WHERE cm.model_spec_id IN (
    SELECT ms.id FROM model_specs ms
    WHERE ms.provider_model_id = d.provider_model_id
    AND ms.id != d.keep_id
);

-- 2. 删除重复的 model_specs 记录
DELETE FROM model_specs ms
USING (
    SELECT provider_model_id, MIN(id) AS keep_id
    FROM model_specs
    GROUP BY provider_model_id
    HAVING COUNT(*) > 1
) d
WHERE ms.provider_model_id = d.provider_model_id
  AND ms.id != d.keep_id;

-- 3. 先改为 nullable
ALTER TABLE model_specs ALTER COLUMN provider_id DROP NOT NULL;

-- 4. 设置所有 provider_id 为 NULL
UPDATE model_specs SET provider_id = NULL;

-- 5. 删除 provider_id 列
ALTER TABLE model_specs DROP COLUMN provider_id;

-- 6. 添加唯一约束
ALTER TABLE model_specs ADD CONSTRAINT uq_model_specs_provider_model_id UNIQUE (provider_model_id);
```

- [ ] **Step 2: 检查 migration 文件命名正确**

Run: `ls gateway-boot/src/main/resources/db/migration/ | grep V40`
Expected: 能看到 `V40__drop_model_specs_provider_id.sql`

- [ ] **Step 3: Commit**

```bash
git add gateway-boot/src/main/resources/db/migration/V40__drop_model_specs_provider_id.sql
git commit -m "feat(db): V40 迁移 — model_specs 去 provider_id，全局注册表"
```

---

### Task 2: 后端 — ModelSpec 领域实体去掉 providerId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ModelSpec.java`

- [ ] **Step 1: ModelSpec.java 删除 providerId 字段及相关 import**

```java
// 删除第 N 行的字段：
// private Long providerId;
```

- [ ] **Step 2: ModelSpecDo.java 删除 providerId 字段**

读取文件：`gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/doobj/ModelSpecDo.java`
删除 `private Long providerId;` 以及对应数据库列映射

- [ ] **Step 3: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ModelSpec.java
git add <path to ModelSpecDo.java>
git commit -m "feat(model-spec): 实体去掉 providerId 字段"
```

---

### Task 3: 后端 — ModelSpecGateway/Repository 去掉 findByProviderId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/gateway/ModelSpecGateway.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/ModelSpecGatewayImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/supply/gateway/database/repository/ModelSpecRepository.java`

- [ ] **Step 1: ModelSpecGateway 接口删除 `findByProviderId`**

```java
// 删除方法声明：
// List<ModelSpec> findByProviderId(Long providerId);
```

- [ ] **Step 2: ModelSpecRepository 删除 `findByProviderId`**

```java
// 删除：
// List<ModelSpecDo> findByProviderId(Long providerId);
```

- [ ] **Step 3: ModelSpecGatewayImpl 删除 findByProviderId 实现**

删除 `findByProviderId` 方法的完整实现（含 `toEntity` 映射）

- [ ] **Step 4: 验证编译**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(model-spec): Gateway/Repository 去掉 findByProviderId"
```

---

### Task 4: 后端 — ModelSpecService 调整

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/modelspec/ModelSpecServiceImpl.java`

- [ ] **Step 1: query() 方法去掉 providerId 过滤**

当前逻辑（伪）：
```java
if (request.getProviderId() != null) {
    return modelSpecGateway.findByProviderId(request.getProviderId())
            .stream()...;
}
```
改为始终走 `findAll()`：
```java
List<ModelSpec> all = modelSpecGateway.findAll();
Stream<ModelSpec> stream = all.stream();
```

同时检查 `ModelSpecQueryRequest` 中是否有 `providerId` 字段 — 删除该字段或忽略它。

- [ ] **Step 2: create/update 方法去掉 providerId 相关代码**

检查 `create()` 中是否从 `CreateModelSpecRequest` 读取了 `providerId`，如果有则删除相关行。

- [ ] **Step 3: 验证编译**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(model-spec): Service 层去掉 providerId 逻辑"
```

---

### Task 5: 后端 — DataInitializer 去掉 providerId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/init/DataInitializer.java`

- [ ] **Step 1: 修改 createModelSpec 方法**

```java
// 当前签名：
private void createModelSpec(Long providerId, String providerModelId, String displayName, int contextWindow) {
    ModelSpec modelSpec = new ModelSpec();
    modelSpec.setProviderId(providerId);  // 删除这行
    ...
}

// 改为：
private void createModelSpec(String providerModelId, String displayName, int contextWindow) {
    ModelSpec modelSpec = new ModelSpec();
    ...
}
```

- [ ] **Step 2: 更新所有调用处**

将所有 `createModelSpec(providerId, "xxx", "xxx", n)` 改为 `createModelSpec("xxx", "xxx", n)`

- [ ] **Step 3: 验证编译**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(model-spec): DataInitializer 去掉 providerId 参数"
```

---

### Task 6: 后端 — CatalogMaterializeService 去掉 providerId

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/catalog/CatalogMaterializeService.java`

- [ ] **Step 1: materializeModelSpec 方法去掉 providerId 相关代码**

当前 `materializeModelSpec` 已有 TODO "providerId 已从 ModelSpec 移除"，去掉所有 `setProviderId` 调用

- [ ] **Step 2: findOrCreateModelSpec 方法去掉 providerId 参数**

```java
// 当前：
private ModelSpec findOrCreateModelSpec(String providerModelId, Long providerId) {
    ...
    spec.setProviderId(providerId);  // 删除这行
}

// 改为：
private ModelSpec findOrCreateModelSpec(String providerModelId) {
```

- [ ] **Step 3: 更新所有调用 findOrCreateModelSpec 的地方**

去掉第二个参数 `providerId`

- [ ] **Step 4: 验证编译**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(model-spec): CatalogMaterializeService 去掉 providerId"
```

---

### Task 7: 后端 — ConfigCacheService/getModelsByProviderId 调整

**Files:**
- Find and modify: `gateway-boot/src/main/java/.../ConfigCacheService.java`

- [ ] **Step 1: 查找 getModelsByProviderId 方法**

Grepping: `grep -rn "getModelsByProviderId" gateway-boot/src/main/java/`
根据协议重构文档，该方法应调整为通过 ChannelModel 间接获取

- [ ] **Step 2: 调整实现**

改为：查该 provider 的所有 channels → 查 channel_models → 关联 model_specs → 去重返回

- [ ] **Step 3: 验证编译**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(model-spec): ConfigCacheService 改为通过 ChannelModel 查询模型"
```

---

### Task 8: 后端 — 新增 ChannelModel 应用层 Service + DTO

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/channel/ChannelModelService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/channel/dto/ChannelModelResponse.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/channel/dto/ChannelModelCreateRequest.java`

- [ ] **Step 1: 创建 ChannelModelCreateRequest DTO**

```java
package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotNull;

public class ChannelModelCreateRequest {
    @NotNull(message = "模型 ID 不能为空")
    private Long modelSpecId;
    // getters/setters
}
```

- [ ] **Step 2: 创建 ChannelModelResponse DTO**

```java
package com.codingas.gateway.application.channel.dto;

public class ChannelModelResponse {
    private Long id;
    private Long channelId;
    private Long modelSpecId;
    private String providerModelId;
    private String displayName;
    private String modelFamily;
    private String state;
    // getters/setters
}
```

- [ ] **Step 3: 创建 ChannelModelService**

```java
package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.*;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.entity.ChannelModelState;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelModelService {

    private final ChannelModelGateway channelModelGateway;
    private final ModelSpecGateway modelSpecGateway;

    public List<ChannelModelResponse> getModelsByChannelId(Long channelId) {
        List<ChannelModel> channelModels = channelModelGateway.findByChannelId(channelId);
        return channelModels.stream()
                .map(this::toResponse)
                .toList();
    }

    public ChannelModelResponse create(Long channelId, ChannelModelCreateRequest request) {
        ChannelModel cm = new ChannelModel();
        cm.setChannelId(channelId);
        cm.setModelSpecId(request.getModelSpecId());
        cm.setState(ChannelModelState.ACTIVE);
        cm = channelModelGateway.save(cm);
        return toResponse(cm);
    }

    public void delete(Long channelId, Long id) {
        channelModelGateway.deleteById(id);
    }

    public void setEnabled(Long channelId, Long id, boolean enabled) {
        ChannelModel cm = channelModelGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChannelModel not found"));
        cm.setState(enabled ? ChannelModelState.ACTIVE : ChannelModelState.INACTIVE);
        channelModelGateway.save(cm);
    }

    private ChannelModelResponse toResponse(ChannelModel cm) {
        ChannelModelResponse resp = new ChannelModelResponse();
        resp.setId(cm.getId());
        resp.setChannelId(cm.getChannelId());
        resp.setModelSpecId(cm.getModelSpecId());
        resp.setState(cm.getState().name());

        modelSpecGateway.findById(cm.getModelSpecId()).ifPresent(spec -> {
            resp.setProviderModelId(spec.getProviderModelId());
            resp.setDisplayName(spec.getDisplayName());
            resp.setModelFamily(spec.getModelFamily());
        });

        return resp;
    }
}
```

- [ ] **Step 4: 验证编译**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(channel-model): 新增 ChannelModelService 和 DTO"
```

---

### Task 9: 后端 — 新增 ChannelModelController

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ChannelModelController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channel.ChannelModelService;
import com.codingas.gateway.application.channel.dto.ChannelModelCreateRequest;
import com.codingas.gateway.application.channel.dto.ChannelModelResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channels/{channelId}/models")
@RequiredArgsConstructor
public class ChannelModelController {

    private final ChannelModelService channelModelService;

    @GetMapping
    public ResponseEntity<List<ChannelModelResponse>> list(@PathVariable Long channelId) {
        return ResponseEntity.ok(channelModelService.getModelsByChannelId(channelId));
    }

    @PostMapping
    public ResponseEntity<ChannelModelResponse> create(
            @PathVariable Long channelId,
            @Valid @RequestBody ChannelModelCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(channelModelService.create(channelId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long channelId, @PathVariable Long id) {
        channelModelService.delete(channelId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/state")
    public ResponseEntity<Void> setEnabled(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        channelModelService.setEnabled(channelId, id, enabled);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 验证启动（可选）**

Run: `./mvnw spring-boot:run -pl gateway-boot` → Ctrl+C after startup
Expected: 应用启动成功，无异常

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(channel-model): 新增 ChannelModelController REST API"
```

---

### Task 10: 后端 — ModelSpec 全局查询 API 调整

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ModelSpecController.java`（可选调整）
- 后端删除 ProviderController 中 /model-specs 相关端点（如果存在）

- [ ] **Step 1: 确认 ModelSpecController 无 providerId 依赖**

检查 `ModelSpecController.query()` — 确保查询参数 `providerId` 被移除或忽略

- [ ] **Step 2: 验证编译**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(model-spec): 全局查询 API 不再支持 providerId 过滤"
```

---

### Task 11: 前端 — 类型定义更新

**Files:**
- Modify: `gateway-console/src/types/modelSpec.ts`

- [ ] **Step 1: 去掉 providerId 字段**

```typescript
export interface ModelSpec {
  id: number;
  // 删除: providerId: number;
  providerModelId: string;
  ...
}
```

- [ ] **Step 2: 检查 check ProviderModelSpecTab.tsx 中的 editModelSpec 是否传 providerId**

如果 `ModelSpecFormModal` 不再需要 `providerId` prop，更新类型

- [ ] **Step 3: 编译检查**

前端无构建脚本？不做正式构建，记录为已修改

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(console): ModelSpec 类型去掉 providerId"
```

---

### Task 12: 前端 — 删除 ModelSpec Tab

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx`
- Delete: `gateway-console/src/pages/Providers/ProviderModelSpecTab.tsx`

- [ ] **Step 1: ProviderManagementDrawer 去掉 modelSpecs tab**

当前代码（伪）：
```tsx
const tabItems = [
  { key: 'basic', label: '基本信息', ... },
  { key: 'channels', label: '渠道', ... },
  { key: 'modelSpecs', label: '模型规格', ... },  // 删除此行
];
```

- [ ] **Step 2: 删除 ProviderModelSpecTab.tsx**

使用 git 删除：
```bash
git rm gateway-console/src/pages/Providers/ProviderModelSpecTab.tsx
```

- [ ] **Step 3: 删除 ModelSpecFormModal.tsx（如不再使用）**

检查 `ModelSpecFormModal.tsx` 是否还被其他地方引用。如果只在 ProviderModelSpecTab 中使用，一并删除。

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(console): 移除供应商详情中的模型规格 tab"
```

---

### Task 13: 前端 — 新增 ChannelModelsPanel 组件

**Files:**
- Create: `gateway-console/src/pages/Providers/ChannelModelsPanel.tsx`
- Create: `gateway-console/src/services/api/channelModel.ts`（API 调用封装）
- Create: `gateway-console/src/services/query/useChannelModels.ts`（React Query hooks）

- [ ] **Step 1: 创建 API 服务**

```typescript
// gateway-console/src/services/api/channelModel.ts
import api from './api';

export const channelModelApi = {
  list: (channelId: number) => api.get(`/channels/${channelId}/models`),
  create: (channelId: number, data: { modelSpecId: number }) =>
    api.post(`/channels/${channelId}/models`, data),
  delete: (channelId: number, id: number) =>
    api.delete(`/channels/${channelId}/models/${id}`),
  setEnabled: (channelId: number, id: number, enabled: boolean) =>
    api.patch(`/channels/${channelId}/models/${id}/state`, null, { params: { enabled } }),
};
```

- [ ] **Step 2: 创建 React Query hooks**

```typescript
// gateway-console/src/services/query/useChannelModels.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { channelModelApi } from '../api/channelModel';

export const useChannelModels = (channelId: number) =>
  useQuery({
    queryKey: ['channel-models', channelId],
    queryFn: () => channelModelApi.list(channelId).then(r => r.data),
    enabled: !!channelId,
  });

export const useCreateChannelModel = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, data }: { channelId: number; data: { modelSpecId: number } }) =>
      channelModelApi.create(channelId, data),
    onSuccess: (_, { channelId }) => qc.invalidateQueries({ queryKey: ['channel-models', channelId] }),
  });
};

export const useDeleteChannelModel = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, id }: { channelId: number; id: number }) =>
      channelModelApi.delete(channelId, id),
    onSuccess: (_, { channelId }) => qc.invalidateQueries({ queryKey: ['channel-models', channelId] }),
  });
};

export const useSetEnabledChannelModel = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ channelId, id, enabled }: { channelId: number; id: number; enabled: boolean }) =>
      channelModelApi.setEnabled(channelId, id, enabled),
    onSuccess: (_, { channelId }) => qc.invalidateQueries({ queryKey: ['channel-models', channelId] }),
  });
};
```

- [ ] **Step 3: 创建 ChannelModelsPanel 组件**

```tsx
// gateway-console/src/pages/Providers/ChannelModelsPanel.tsx
import React, { useState } from 'react';
import { Table, Button, Tag, Space, Popconfirm, Switch, Modal, Select } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useChannelModels, useCreateChannelModel, useDeleteChannelModel, useSetEnabledChannelModel } from '../../services/query/useChannelModels';
import { useModelSpecs } from '../../services/query/useModelSpecs';

interface ChannelModelsPanelProps {
  channelId: number;
  canWrite?: boolean;
}

const ChannelModelsPanel: React.FC<ChannelModelsPanelProps> = ({ channelId, canWrite }) => {
  const { data: models, isLoading } = useChannelModels(channelId);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedModelSpecId, setSelectedModelSpecId] = useState<number | null>(null);

  const createMutation = useCreateChannelModel();
  const deleteMutation = useDeleteChannelModel();
  const setEnabledMutation = useSetEnabledChannelModel();

  // 全局模型列表
  const { data: allModelSpecs } = useModelSpecs();

  const columns = [
    { title: '模型标识', dataIndex: 'providerModelId', key: 'providerModelId' },
    { title: '显示名', dataIndex: 'displayName', key: 'displayName' },
    { title: '模型系列', dataIndex: 'modelFamily', key: 'modelFamily' },
    {
      title: '状态',
      dataIndex: 'state',
      key: 'state',
      render: (state: string) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'default'}>{state}</Tag>
      ),
    },
    ...(canWrite
      ? [
          {
            title: '操作',
            key: 'actions',
            render: (_: unknown, record: any) => (
              <Space>
                <Switch
                  checked={record.state === 'ACTIVE'}
                  onChange={(checked) =>
                    setEnabledMutation.mutate({ channelId, id: record.id, enabled: checked })
                  }
                />
                <Popconfirm title="确认解绑该模型？" onConfirm={() => deleteMutation.mutate({ channelId, id: record.id })}>
                  <Button type="link" danger>解绑</Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]
      : []),
  ];

  const handleAdd = () => {
    if (!selectedModelSpecId) return;
    createMutation.mutate(
      { channelId, data: { modelSpecId: selectedModelSpecId } },
      { onSuccess: () => { setModalOpen(false); setSelectedModelSpecId(null); } },
    );
  };

  // 过滤已关联的模型（避免重复关联）
  const availableModelSpecs = allModelSpecs?.filter(
    (spec: any) => !models?.some((m: any) => m.modelSpecId === spec.id),
  );

  return (
    <>
      <div style={{ marginBottom: 16 }}>
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
            关联模型
          </Button>
        )}
      </div>
      <Table
        dataSource={models}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={false}
        size="small"
      />
      <Modal
        title="关联全局模型"
        open={modalOpen}
        onOk={handleAdd}
        onCancel={() => { setModalOpen(false); setSelectedModelSpecId(null); }}
        okText="关联"
        cancelText="取消"
      >
        <Select
          showSearch
          style={{ width: '100%' }}
          placeholder="搜索并选择模型"
          value={selectedModelSpecId}
          onChange={setSelectedModelSpecId}
          filterOption={(input, option) =>
            (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
          }
          options={availableModelSpecs?.map((spec: any) => ({
            value: spec.id,
            label: `${spec.providerModelId} (${spec.displayName || spec.modelFamily || ''})`,
          }))}
        />
      </Modal>
    </>
  );
};

export default ChannelModelsPanel;
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(console): 新增 ChannelModelsPanel 组件和 API 封装"
```

---

### Task 14: 前端 — 渠道展开行增加 Models tab

**Files:**
- Modify: `gateway-console/src/pages/Providers/ProviderChannelTab.tsx`（或 `ChannelExpandedRow` 组件）

- [ ] **Step 1: 找到 expandedRowRender 实现**

读取 ProviderChannelTab.tsx 中 expandedRowRender 代码，定位 Endpoints 和 Credentials 的 tab 切换逻辑（使用 Segmented 或 Tabs）

- [ ] **Step 2: 在展开行中增加第三个 tab**

在 tab 列表中加入 `Models` tab，渲染 `ChannelModelsPanel` 组件：

```tsx
// 在 Endpoints / Credentials 两个 tab 旁边增加
{
  key: 'models',
  label: '模型',
  children: <ChannelModelsPanel channelId={record.id} canWrite={canWrite} />,
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(console): 渠道展开行增加 Models tab"
```

---

### Task 15: 验证集成

- [ ] **Step 1: 后端编译**

Run: `./mvnw clean compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 后端测试（如果有相关测试）**

Run: `./mvnw test -pl gateway-boot -q`
Expected: BUILD SUCCESS（或排除与变更无关的失败）

- [ ] **Step 3: 最终完整性检查**

确认所有改动文件已加入版本管理，无遗漏