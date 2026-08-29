# 同家族模型复制 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现基于已有模型规格一键复制新模型（后端 copy 接口 + 行内复制按钮 + 新增弹窗复制选择器）。

**Architecture:** 后端 `ModelService.copy(sourceId, Model)` 复制源模型全量规格、按请求覆盖 modelName/displayName/modelFamily、重置 source/externalId/lockedFields/生命周期字段；web 层 `POST /api/v1/models/{id}/copy` 透传。前端复用同一复制对话框（`CopyModelModal`），提供行内复制按钮与新增弹窗"从模型复制"选择器（按 family 分组 + 搜索）。复制源**不限制家族**。

**Tech Stack:** Java 21 + Spring Boot 3.5 + JPA；React 19 + antd + react-i18next + TanStack Query

## Global Constraints

- 分层依赖：web → 核心 Service；`ModelCopyRequest.toEntity()` 在 web 层组装为域实体
- 中文 Javadoc：public 方法必须中文注释
- 复制语义（spec 确认）：继承全量规格；覆盖 modelName(必填)/displayName/modelFamily；重置 source=MANUAL、externalId=null、lockedFields 清空、deprecatedAt/scheduledRetiredAt/deprecationMessage=null；不复制 id/审计字段/挂载
- 复制源不限制家族（分组仅展示组织）
- modelName 唯一校验：已存在抛 `DuplicateResourceException`（`com.codingas.gateway.common.exception.DuplicateResourceException`，确认存在——ChannelCredential 等已用）
- TDD：先写测试再实现；每任务独立提交
- 前端文案用 `t(..., { defaultValue: ... })` 兜底，不新增语言文件（保守原则）

---

### Task 1: ModelService.copy 核心实现（provider 域）

**Files:**
- Modify: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelService.java`
- Modify: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelServiceImpl.java`
- Test: `gateway-provider/provider/src/test/java/com/codingas/gateway/provider/model/ModelServiceTest.java`

**Interfaces:**
- Consumes: 现有 `ModelService`/`ModelRepository`（save/findById/findByModelName）
- Produces: `Model copy(Long sourceId, Model override)` — 返回新 Model（id 为 null 由 save 生成）；`override` 承载覆盖字段（modelName 必填非 null，displayName/modelFamily 可为 null）

- [ ] **Step 1: 写失败测试**

在 `ModelServiceTest` 新增嵌套类（放在 UpdateTests 之后）：

```java
    // ==================== copy 测试 ====================

    @Nested
    @DisplayName("copy 复制模型")
    class CopyTests {

        @Test
        @DisplayName("复制继承源模型全量规格并重置生命周期字段")
        void copy_inheritsFullSpecAndResetsLifecycle() {
            // given：源模型（含描述/限额/能力/模态/元信息/废弃字段）
            Model source = createTestModel(1L, "gpt-4", testProvider, "GPT-4", true);
            source.setDescription("desc");
            source.setMaxInputTokens(128000);
            source.setMaxOutputTokens(4096);
            source.setKnowledgeCutoff("2023-10");
            source.setLicense("MIT");
            source.setOpenWeights(false);
            source.setBenchmarks(List.of(Map.of("name", "MMLU", "score", 86)));
            source.setWeights(List.of(Map.of("label", "card", "url", "http://x")));
            source.setSource("MODELS_DEV");
            source.setExternalId("openai/gpt-4");
            source.setLockedFields(List.of("displayName"));
            source.setDeprecatedAt(Instant.parse("2026-01-01T00:00:00Z"));
            source.setScheduledRetiredAt(Instant.parse("2026-06-01T00:00:00Z"));
            source.setDeprecationMessage("old");
            when(modelRepository.findById(1L)).thenReturn(Optional.of(source));
            when(modelRepository.save(any(Model.class))).thenAnswer(inv -> {
                Model m = inv.getArgument(0);
                m.setId(2L);
                return m;
            });

            // when：覆盖 modelName
            Model override = new Model();
            override.setModelName("gpt-4o");
            Model result = modelService.copy(1L, override);

            // then：继承全量 + 覆盖 + 重置
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getModelName()).isEqualTo("gpt-4o");
            assertThat(result.getDisplayName()).isEqualTo("GPT-4");
            assertThat(result.getDescription()).isEqualTo("desc");
            assertThat(result.getMaxInputTokens()).isEqualTo(128000);
            assertThat(result.getMaxOutputTokens()).isEqualTo(4096);
            assertThat(result.getKnowledgeCutoff()).isEqualTo("2023-10");
            assertThat(result.getLicense()).isEqualTo("MIT");
            assertThat(result.getOpenWeights()).isFalse();
            assertThat(result.getBenchmarks()).hasSize(1);
            assertThat(result.getWeights()).hasSize(1);
            assertThat(result.getSource()).isEqualTo("MANUAL");
            assertThat(result.getExternalId()).isNull();
            assertThat(result.getLockedFields()).isEmpty();
            assertThat(result.getDeprecatedAt()).isNull();
            assertThat(result.getScheduledRetiredAt()).isNull();
            assertThat(result.getDeprecationMessage()).isNull();
            assertThat(result.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("复制覆盖 displayName 与 modelFamily")
        void copy_overridesDisplayNameAndFamily() {
            // given
            Model source = createTestModel(1L, "gpt-4", testProvider, "GPT-4", true);
            when(modelRepository.findById(1L)).thenReturn(Optional.of(source));
            when(modelRepository.save(any(Model.class))).thenAnswer(inv -> inv.getArgument(0));

            Model override = new Model();
            override.setModelName("gpt-4o");
            override.setDisplayName("GPT-4o");
            override.setModelFamily("gpt-4o-family");

            // when
            Model result = modelService.copy(1L, override);

            // then
            assertThat(result.getDisplayName()).isEqualTo("GPT-4o");
            assertThat(result.getModelFamily()).isEqualTo("gpt-4o-family");
        }

        @Test
        @DisplayName("源模型不存在抛 ResourceNotFoundException")
        void copy_sourceNotFound_throws() {
            when(modelRepository.findById(99L)).thenReturn(Optional.empty());
            Model override = new Model();
            override.setModelName("x");

            assertThatThrownBy(() -> modelService.copy(99L, override))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("modelName 已存在抛 DuplicateResourceException")
        void copy_duplicateModelName_throws() {
            Model source = createTestModel(1L, "gpt-4", testProvider, "GPT-4", true);
            when(modelRepository.findById(1L)).thenReturn(Optional.of(source));
            when(modelRepository.findByModelName("gpt-5")).thenReturn(Optional.of(new Model()));

            Model override = new Model();
            override.setModelName("gpt-5");

            assertThatThrownBy(() -> modelService.copy(1L, override))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(modelRepository, never()).save(any(Model.class));
        }
    }
```

（补 import：`com.codingas.gateway.common.exception.DuplicateResourceException`、`java.util.Map`——若测试文件已有则跳过。）

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-provider/provider -am test -Dtest=ModelServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（`copy` 不存在）

- [ ] **Step 3: 实现**

`ModelService.java` 接口（`create` 后新增）：

```java
    /**
     * 复制模型规格生成新模型
     *
     * <p>基于源模型全量规格复制，按 {@code override} 覆盖 modelName（必填）与
     * displayName/modelFamily；新模型重置为人工来源（MANUAL）、externalId 清空、
     * 锁定字段清空、生命周期字段清空（可用状态）。不复制挂载。</p>
     *
     * @param sourceId 源模型 ID
     * @param override 覆盖字段（modelName 必填非 null）
     * @return 新模型
     */
    Model copy(Long sourceId, Model override);
```

`ModelServiceImpl` 实现（`create` 方法后新增；复制逻辑用中文注释分节）：

```java
    /**
     * 复制模型规格生成新模型（继承全量 + 覆盖 + 重置，见接口 Javadoc）
     */
    @Override
    @Transactional
    public Model copy(Long sourceId, Model override) {
        Model source = modelRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Model", sourceId));

        // modelName 唯一校验（覆盖字段必填）
        String newName = override.getModelName();
        modelRepository.findByModelName(newName)
                .filter(existing -> !existing.getId().equals(sourceId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Model", "modelName");
                });

        // 复制源模型全量规格
        Model target = new Model();
        target.setModelName(newName);
        target.setDisplayName(override.getDisplayName() != null
                ? override.getDisplayName() : source.getDisplayName());
        target.setModelFamily(override.getModelFamily() != null
                ? override.getModelFamily() : source.getModelFamily());
        target.setDescription(source.getDescription());
        target.setContextWindow(source.getContextWindow());
        target.setMaxInputTokens(source.getMaxInputTokens());
        target.setMaxOutputTokens(source.getMaxOutputTokens());
        target.setKnowledgeCutoff(source.getKnowledgeCutoff());
        target.setReleaseDate(source.getReleaseDate());
        target.setLastUpdated(source.getLastUpdated());
        target.setLicense(source.getLicense());
        target.setOpenWeights(source.getOpenWeights());
        target.setBenchmarks(source.getBenchmarks());
        target.setWeights(source.getWeights());
        target.setCapabilities(source.getCapabilities());
        target.setModalities(source.getModalities());

        // 重置：人工来源、清 externalId/锁定/生命周期字段（新模型为可用状态）
        target.setSource("MANUAL");
        target.setExternalId(null);
        target.setLockedFields(List.of());
        target.setDeprecatedAt(null);
        target.setScheduledRetiredAt(null);
        target.setDeprecationMessage(null);

        Model saved = modelRepository.save(target);
        log.info("模型复制成功, sourceId={}, newId={}, modelName={}",
                sourceId, saved.getId(), saved.getModelName());
        return saved;
    }
```

（补 import：`com.codingas.gateway.common.exception.DuplicateResourceException`。）

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS（4 用例）

- [ ] **Step 5: Commit**

```bash
git add gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelService.java \
        gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelServiceImpl.java \
        gateway-provider/provider/src/test/java/com/codingas/gateway/provider/model/ModelServiceTest.java
git commit -m "feat(model-copy): ModelService.copy 复制规格生成新模型（继承+覆盖+重置）"
```

---

### Task 2: web 层 copy 端点

**Files:**
- Create: `gateway-web/src/main/java/com/codingas/gateway/web/api/dto/ModelCopyRequest.java`
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/api/ModelController.java`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/api/ModelControllerTest.java`

**Interfaces:**
- Consumes: `ModelService.copy(Long, Model)`（Task 1）
- Produces: `POST /api/v1/models/{id}/copy`，body `ModelCopyRequest{ modelName(@NotBlank), displayName?, modelFamily? }` → `ModelResponse`；`ModelCopyRequest.toEntity()` 返回覆盖字段 Model

- [ ] **Step 1: 写失败测试**

在 `ModelControllerTest` 新增：

```java
    @Nested
    @DisplayName("copy 复制模型")
    class CopyTests {

        @Test
        @DisplayName("POST /api/v1/models/{id}/copy 返回新模型")
        void copy_returnsNewModel() throws Exception {
            ModelResponse resp = new ModelResponse();
            resp.setId(2L);
            resp.setModelName("gpt-4o");
            when(modelService.copy(eq(1L), any(Model.class))).thenReturn(resp);

            mockMvc.perform(post("/api/v1/models/{id}/copy", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"modelName\":\"gpt-4o\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2L))
                    .andExpect(jsonPath("$.modelName").value("gpt-4o"));
        }

        @Test
        @DisplayName("modelName 缺失返回 400")
        void copy_missingModelName_badRequest() throws Exception {
            mockMvc.perform(post("/api/v1/models/{id}/copy", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
```

（按 `ModelControllerTest` 既有 mockMvc/mock 装配方式适配；`ModelResponse` 字段 setter 需确认存在——id/modelName 必有。）

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-web -am test -Dtest=ModelControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（`ModelCopyRequest`/copy 端点不存在）

- [ ] **Step 3: 实现**

`ModelCopyRequest.java`（参照 `ModelCreateRequest` 模式）：

```java
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.provider.model.Model;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模型复制请求 DTO（HTTP 契约）
 *
 * <p>仅承载可覆盖字段；未覆盖的规格字段从源模型继承。</p>
 */
@Data
public class ModelCopyRequest {

    /** 新模型名（必填，路由匹配唯一键） */
    @NotBlank(message = "modelName 不能为空")
    private String modelName;

    /** 显示名称（可选覆盖，null 继承源） */
    private String displayName;

    /** 模型族（可选覆盖，null 继承源） */
    private String modelFamily;

    /**
     * 转换为覆盖字段实体（其余规格由服务层从源模型复制）
     *
     * @return 仅含覆盖字段的 Model
     */
    public Model toEntity() {
        Model model = new Model();
        model.setModelName(modelName);
        model.setDisplayName(displayName);
        model.setModelFamily(modelFamily);
        return model;
    }
}
```

`ModelController.java`（`create` 端点后新增）：

```java
    /**
     * 复制模型规格生成新模型
     */
    @PostMapping("/{id}/copy")
    public ModelResponse copy(@PathVariable Long id,
                              @Valid @RequestBody ModelCopyRequest request) {
        return ModelResponse.from(modelService.copy(id, request.toEntity()));
    }
```

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS（2 用例）

- [ ] **Step 5: Commit**

```bash
git add gateway-web/src/main/java/com/codingas/gateway/web/api/dto/ModelCopyRequest.java \
        gateway-web/src/main/java/com/codingas/gateway/web/api/ModelController.java \
        gateway-web/src/test/java/com/codingas/gateway/web/api/ModelControllerTest.java
git commit -m "feat(model-copy): web 层模型复制端点（POST /{id}/copy + ModelCopyRequest）"
```

---

### Task 3: 前端 API 与复制 hook

**Files:**
- Modify: `gateway-console/src/services/api/model.ts`
- Modify: `gateway-console/src/services/query/useModels.ts`
- Modify: `gateway-console/src/types/model.ts`
- Test: `gateway-console/src/services/api/__tests__/` 下新建或扩展（若现有 API 测试模式存在，参照）

**Interfaces:**
- Consumes: 现有 `modelApi`/`modelKeys` 模式
- Produces: `modelApi.copy(id: number, data: CopyModelRequest): Promise<Model>`、`useCopyModel()`（mutation + invalidate lists）、`CopyModelRequest` 类型 `{ modelName: string; displayName?: string; modelFamily?: string }`

- [ ] **Step 1: 写失败测试**

若 `model.ts` 有对应 API 测试则按模式扩展；否则此步改为类型检查验证（tsc）。在 `useModels.ts` 的复制 hook 测试（若有 hook 测试文件）或跳过——本任务以 tsc + 手工验证为主，测试重点放 Task 4 组件测试。

- [ ] **Step 2: 实现（TS 无独立测试时以 tsc 验证）**

`types/model.ts` 新增：

```ts
/** 模型复制请求（仅覆盖字段，其余规格从源继承） */
export interface CopyModelRequest {
  modelName: string;
  displayName?: string;
  modelFamily?: string;
}
```

`services/api/model.ts`（`unlock` 后新增）：

```ts
  copy: (id: number, data: CopyModelRequest) =>
    api.post<Model>(`/models/${id}/copy`, data),
```

（补 import `CopyModelRequest`。）

`services/query/useModels.ts` 新增：

```ts
/** 复制模型（继承源规格生成新模型） */
export function useCopyModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: CopyModelRequest }) =>
      modelApi.copy(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: modelKeys.lists() });
    },
  });
}
```

- [ ] **Step 3: 验证类型与 lint**

Run: `cd E:/workspace/llm-gateway/gateway-console && npx tsc -b --noEmit && npx eslint src/services/api/model.ts src/services/query/useModels.ts src/types/model.ts`
Expected: 无错误

- [ ] **Step 4: Commit**

```bash
git add gateway-console/src/services/api/model.ts \
        gateway-console/src/services/query/useModels.ts \
        gateway-console/src/types/model.ts
git commit -m "feat(console): 模型复制 API 与 useCopyModel hook"
```

---

### Task 4: 前端复制 UI（行内按钮 + 新增弹窗选择器 + 复制对话框）

**Files:**
- Create: `gateway-console/src/pages/Models/CopyModelModal.tsx`
- Modify: `gateway-console/src/pages/Models/index.tsx`
- Modify: `gateway-console/src/pages/Models/ModelCreateModal.tsx`
- Test: `gateway-console/src/pages/Models/__tests__/` 下新建（参照现有 Models 相关测试模式）

**Interfaces:**
- Consumes: `useCopyModel()`（Task 3）、`useModels` 列表数据（含 modelFamily）、`useTranslation('models')`
- Produces: `CopyModelModal{ open, source: Model | null, onClose, onCopied }` — 表单含 modelName（必填，预填源.modelName + 后缀建议）/displayName/modelFamily（预填源值）；提交调 `useCopyModel` → 成功 onCopied(新模型) + 刷新列表

- [ ] **Step 1: 写失败测试（前端组件）**

新建 `gateway-console/src/pages/Models/__tests__/CopyModelModal.test.tsx`（参照仓库现有 antd 组件测试模式，mock `useCopyModel`）：

```tsx
describe('CopyModelModal', () => {
  it('预填源模型字段并提交复制', async () => {
    // render(<CopyModelModal open source={{ modelName: 'gpt-4', displayName: 'GPT-4', modelFamily: 'gpt-4' }} ... />)
    // 断言 modelName/displayName/modelFamily 输入框预填
    // 改 modelName → 点击确定 → mock useCopyModel 被调 { id, data: { modelName, displayName, modelFamily } }
    // onCopied 被调
  });

  it('modelName 为空时禁止提交', () => {
    // 清空 modelName → 确定按钮 disabled 或校验失败
  });
});
```

（具体 mock 方式按仓库现有测试基建——`vi.mock('@/services/query/useModels')` 等。）

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway/gateway-console && npx vitest run src/pages/Models/__tests__/CopyModelModal.test.tsx`
Expected: 失败（组件不存在）

- [ ] **Step 3: 实现 CopyModelModal 组件**

`CopyModelModal.tsx`：

```tsx
import { Modal, Form, Input, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCopyModel } from '@/services/query/useModels';
import type { Model } from '@/types/model';

interface Props {
  open: boolean;
  /** 源模型（null 时不渲染表单） */
  source: Model | null;
  onClose: () => void;
  /** 复制成功回调（携带新模型） */
  onCopied: (created: Model) => void;
}

export default function CopyModelModal({ open, source, onClose, onCopied }: Props) {
  const { t } = useTranslation('models');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const copyMutation = useCopyModel();

  const handleOk = async () => {
    if (!source) return;
    try {
      const values = await form.validateFields();
      const created = await copyMutation.mutateAsync({
        id: source.id,
        data: {
          modelName: values.modelName,
          displayName: values.displayName,
          modelFamily: values.modelFamily,
        },
      });
      message.success(t('copied', { defaultValue: '模型复制成功' }));
      form.resetFields();
      onClose();
      onCopied(created);
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'errorFields' in e) return;
      message.error(t('copyFailed', { defaultValue: '复制失败' }));
    }
  };

  return (
    <Modal
      title={t('copyModel', { defaultValue: '复制模型' })}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={copyMutation.isPending}
      destroyOnHidden
      width={480}
    >
      {source && (
        <Form form={form} layout="vertical" initialValues={{
          modelName: source.modelName,
          displayName: source.displayName,
          modelFamily: source.modelFamily,
        }}>
          <Form.Item
            name="modelName"
            label={t('modelName', { defaultValue: '模型标识' })}
            rules={[{ required: true, message: t('modelNameRequired', { defaultValue: '请输入模型标识' }) }]}
          >
            <Input placeholder="gpt-4o" />
          </Form.Item>
          <Form.Item name="displayName" label={t('displayName', { defaultValue: '显示名称' })}>
            <Input />
          </Form.Item>
          <Form.Item name="modelFamily" label={t('modelFamily', { defaultValue: '模型族' })}>
            <Input />
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
}
```

- [ ] **Step 4: 接入 Models 列表行内复制按钮**

`Models/index.tsx`：
1. import `CopyOutlined` 图标、`CopyModelModal`、`useState` 已有。
2. 新增状态 `copySource`（Model | null）。
3. 操作列（编辑/删除按钮旁）新增复制按钮：

```tsx
          <Tooltip title={t('copy', { defaultValue: '复制' })}>
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => setCopySource(record)}
            />
          </Tooltip>
```

4. 组件区新增：

```tsx
      <CopyModelModal
        open={!!copySource}
        source={copySource}
        onClose={() => setCopySource(null)}
        onCopied={() => refetch()}
      />
```

（`refetch` 已存在；复制成功后刷新列表。）

- [ ] **Step 5: 接入新增模型弹窗"从模型复制"选择器**

`ModelCreateModal.tsx`：
1. import `Select` 已有；新增 `CopyModelModal`、`useState`。
2. 在表单顶部（providerId 之前）新增选择器：

```tsx
        <Form.Item label={t('copyFrom', { defaultValue: '从已有模型复制' })}>
          <Select
            allowClear
            placeholder={t('copyFromPlaceholder', { defaultValue: '选择源模型（可选）' })}
            showSearch
            optionFilterProp="label"
            onChange={(sourceId: number) => {
              const source = models?.items.find((m) => m.id === sourceId);
              if (source) setCopySource(source);
            }}
            options={groupByFamily(models?.items ?? [])}
          />
        </Form.Item>
```

3. 新增分组工具（`optionFilterProp="label"` 与 `Select` 分组 `options` 需兼容搜索——antd 分组用法：`options: [{ label: family, options: [{value,label}] }]`，`optionFilterProp="label"` 对分组内 label 生效需设 `optionFilterProp="label"` + `filterOption` 自定义或 `showSearch` 默认）：

```tsx
/** 按模型族分组 + 名称搜索的源模型选择项 */
function groupByFamily(models: Model[]): { label: string; options: { value: number; label: string }[] }[] {
  const groups = new Map<string, { value: number; label: string }[]>();
  for (const m of models) {
    const family = m.modelFamily || t('unknownFamily', { defaultValue: '未分组' });
    const list = groups.get(family) ?? [];
    list.push({ value: m.id, label: `${m.modelName}${m.displayName ? ` (${m.displayName})` : ''}` });
    groups.set(family, list);
  }
  return [...groups.entries()].map(([family, options]) => ({
    label: family,
    options: options.sort((a, b) => a.label.localeCompare(b.label)),
  }));
}
```

（若 `ModelCreateModal` 无 `models` 数据，需 `useModels` 或从 `useProviders/useChannels` 派生——确认后补。也可在复制选择器用独立的 `useModels({ limit: 1000 })`。）

4. 新增 `CopyModelModal`（复用上述组件）挂载：

```tsx
      <CopyModelModal
        open={!!copySource}
        source={copySource}
        onClose={() => setCopySource(null)}
        onCopied={() => message.success(t('copied', { defaultValue: '模型复制成功' }))}
      />
```

- [ ] **Step 6: 运行测试确认通过**

Run: `cd E:/workspace/llm-gateway/gateway-console && npx vitest run src/pages/Models/__tests__/CopyModelModal.test.tsx` 及既有 Models 测试（`npx vitest run src/pages/Models`）
Expected: 全 PASS；`npx tsc -b --noEmit` 无错误

- [ ] **Step 7: Commit**

```bash
git add gateway-console/src/pages/Models/CopyModelModal.tsx \
        gateway-console/src/pages/Models/index.tsx \
        gateway-console/src/pages/Models/ModelCreateModal.tsx
git add gateway-console/src/pages/Models/__tests__/
git commit -m "feat(console): 同家族模型复制 UI（行内复制按钮 + 新增弹窗复制选择器）"
```

---

## Self-Review 结果

- **Spec 覆盖**：§3.1 后端 copy 接口（Task 1/2）、§3.2 前端双入口（Task 3/4）、§4 文件落点（各任务）、§5 测试策略（各任务）——全覆盖。
- **占位符**：Task 4 的组件测试标注"参照仓库现有模式"——已在 Step 1 给出测试结构与断言要点；模型分组 `t('unknownFamily')` 的 `t` 作用域问题在实现时以组件内 `t` 调整。无 TBD。
- **类型一致性**：`ModelService.copy(Long, Model)` Task 1 定义，Task 2 引用一致；`modelApi.copy(id, CopyModelRequest)` Task 3 定义，Task 4 引用一致；`CopyModelModal` props Task 4 定义并自引用。
