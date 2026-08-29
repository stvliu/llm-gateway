# 同家族模型复制 — 技术设计

> 日期：2026-08-29
> 状态：已确认（复制范围/交互入口/源选择/后端接口方案）

## 1. 背景与目标

### 1.1 背景

- 模型新增目前两条通道：models.dev 目录同步自动新增（已有）+ 管理台人工新增（`ModelCreateModal`，仅 8 个规格字段）。
- 新模型发布时（如 GPT 系列新变体），管理员需要手动重复填写能力/限额/描述/基准等大量字段，效率低。
- 同家族模型（同一 `modelFamily`）规格高度相似（能力/上下文/模态接近），复制价值最大。

### 1.2 目标

提供**同家族模型复制**能力：基于已有模型规格一键生成新模型，管理员只改 `modelName`（必填）与可选的 `displayName`/`modelFamily` 即完成上架。

## 2. 需求决策（已与用户确认）

| 决策点 | 结论 |
|--------|------|
| 复制范围 | **仅复制规格**（不复制 ModelInstance 挂载） |
| 交互入口 | **两者结合**：模型列表行内"复制"按钮 + 新增模型弹窗"从同家族模型复制"选择器，共用同一复制逻辑 |
| 源选择 | 按 `modelFamily` 分组展示 + 名称搜索；行内按钮以当前行为源 |
| 实现方式 | **后端 copy 接口**（`ModelCreateRequest` 仅 8 字段，纯前端预填无法继承 description/benchmarks 等全量规格） |

## 3. 架构设计

### 3.1 后端接口

```
POST /api/v1/models/{id}/copy
Body: ModelCopyRequest { modelName(必填), displayName?, modelFamily? }
→ ModelResponse（新模型）
```

**ModelService.copy(Long sourceId, ModelCopyRequest request)**

| 行为 | 字段 |
|------|------|
| **继承**（源模型全量复制） | displayName、modelFamily、description、contextWindow、maxInputTokens、maxOutputTokens、knowledgeCutoff、license、openWeights、benchmarks、weights、capabilities、modalities |
| **覆盖**（请求可改） | modelName（必填，唯一校验）、displayName、modelFamily |
| **重置** | `source=MANUAL`、`externalId=null`（避免 models.dev 同步按 externalId 幂等接管新模型）、`lockedFields` 清空（新模型无人工编辑历史）、`deprecatedAt/scheduledRetiredAt/deprecationMessage` 置 null（新模型为可用状态） |
| **不复制** | id、审计字段、ModelInstance 挂载 |

**校验**：
- 源模型不存在 → `ResourceNotFoundException`
- `modelName` 已存在 → 重复资源异常（复用现有创建校验模式）

### 3.2 前端

**① Models 列表行内"复制"按钮**
- 每行操作区新增复制图标（`CopyOutlined`）→ 打开复制对话框（源 = 当前行，modelName 必填 + displayName/modelFamily 可改）

**② 新增模型弹窗"从同家族模型复制"选择器**
- `ModelCreateModal` 顶部新增选择器：下拉按 `modelFamily` 分组（OptionGroup）+ 名称搜索
- 选择源模型 → 调 copy 接口 → 返回新 Model 后表单预填（modelName 置空待填，displayName/modelFamily 预填源值）
- 与行内按钮共用 `CopyModelModal` 组件（或复制逻辑抽为可复用 hook）

**③ 复制后行为**
- 复制成功 → 提示 + 刷新模型列表；可选跳转编辑抽屉微调

## 4. 文件落点

| 层 | 文件 |
|----|------|
| provider 域核心 | `ModelService.copy` + `ModelServiceImpl` 实现 |
| web 层 | `ModelCopyRequest`（DTO）、`ModelController` 增加 `POST /{id}/copy`、`ModelFacade`/组装 |
| console | `Models/index.tsx` 行内复制按钮、`ModelCreateModal` 复制选择器、复制对话框组件、前端 API（modelApi.copy）与 hook |

## 5. 测试策略（TDD）

- **ModelServiceTest.copy**：
  - 继承全量规格字段（源 → 新模型逐字段断言）
  - 重置字段（source=MANUAL/externalId=null/lockedFields 空/生命周期字段 null）
  - 源不存在抛 `ResourceNotFoundException`
  - modelName 重复抛重复资源异常
  - displayName/modelFamily 覆盖生效
- **web ControllerTest**：`POST /api/v1/models/{id}/copy` 契约（成功/400 校验）
- **前端组件测试**：复制按钮触发弹窗、选择器分组渲染、选择源后预填、复制成功刷新

## 6. 后续可扩展（本期不做）

- 复制挂载（ModelInstance）——已决策本期仅复制规格
- 复制时可选"连同渠道挂载模板"——见后续需求
