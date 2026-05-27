# 供应商页面功能完善设计

> 日期: 2026-05-26
> 状态: 待审核
> 分支: refactor/template-to-metadata

## 1. 背景与目标

### 现状

供应商页面（`/providers`）已有基础框架，但功能严重不完整：

- 详情抽屉只有"基本信息" tab，渠道/凭证/模型规格管理缺失
- 后端 Channel/Credential/ModelSpec API 大部分已就绪，但前端未集成
- 连通性测试功能前端未暴露
- i18n 已定义 `viewMode.card/table` 但表格视图未实现
- 创建/编辑表单缺少 `description` 字段
- 无状态筛选功能

### 目标

以管理员 + 普通用户双视角，将供应商页面补全为完整的供给管理中心，采用渐进式扩展（方案 A）—— 在现有抽屉架构上增量补全，不改变整体 UI 结构。

## 2. 设计方案

### 2.1 供应商列表页增强

**搜索与筛选区：**

| 组件 | 变更 |
|------|------|
| 搜索框 | 保留，无变更 |
| 状态筛选 | 新增下拉（全部 / 启用 / 停用），筛选 `providerState` |
| 视图切换 | 新增切换器（卡片 ↔ 表格），放在搜索框右侧 |

**表格视图（ProvidersTableView）：**

- 列：品牌标识（Logo）、供应商名称、状态（Tag）、渠道数、优先级、创建时间、操作
- 操作列：查看详情、编辑、删除（权限控制）
- 渠道数从 Channel API 按 providerId 聚合
- 空状态引导创建

**卡片视图优化：**

- 卡片底部增加"渠道数"和"模型数"统计标签
- 状态筛选联动卡片显示

**权限差异化：**

| 权限 | 可见操作 |
|------|---------|
| `PROVIDER_READ` | 列表、详情、渠道/模型只读 |
| `PROVIDER_WRITE` | 创建、编辑、删除、启用禁用 |

### 2.2 供应商详情抽屉 Tab 体系

抽屉宽度从 560px 扩展到 **720px**。

**Tab 结构：**

| Tab | 管理员 | 普通用户 | 内容 |
|-----|--------|---------|------|
| 基本信息 | 读写 | 只读 | 品牌、名称、描述、官网、API文档、优先级、状态、连通性测试、审计信息 |
| 渠道 | 读写 | 只读 | 渠道列表 + 端点管理 + 凭证管理 |
| 模型规格 | 读写 | 只读 | 模型规格列表 |

**基本信息 Tab 增强：**

- 补全 `description` 字段（创建和编辑表单均增加）
- 新增"连通性测试"按钮
- 创建表单增加 `description` 文本域

### 2.3 渠道 Tab

**渠道列表（Table）：**

| 列 | 说明 |
|----|------|
| 渠道名称 | Channel.name |
| 状态 | Tag 渲染 ChannelState |
| 端点数 | 关联 ChannelEndpoint 数量 |
| 凭证数 | 关联 ChannelCredential 数量 |
| 优先级 | Channel.priority |
| 操作 | 编辑、启用/禁用、删除 |

**渠道操作：**

- 创建：Modal 弹窗表单（名称、优先级、状态）
- 编辑：Modal 弹窗表单（名称、优先级）
- 启用/禁用：确认弹窗 → 调用 `PATCH /channels/{id}/state`
- 删除：确认弹窗（提示关联端点/凭证数）

**渠道展开行 — 端点与凭证：**

展开行内端点和凭证在同一区域，端点列表在上、凭证列表在下，用 Divider 分隔。

- 端点列表（Table 嵌套）
- 端点列：Base URL、协议类型（Tag: OpenAI/Anthropic）、状态
- "添加端点"按钮 → Modal 表单（Base URL、协议类型）
- 端点启用/禁用/删除

**渠道展开行 — 凭证管理（端点列表下方，Divider 分隔）：**

- 凭证列表（Table 嵌套）
- 凭证列：API Key（脱敏 `sk-...3xYz`）、状态、最后验证时间
- "添加凭证"按钮 → Modal 表单（API Key 输入框 + 明文提示）
- "测试凭证"按钮 → 调用 `POST /channels/{channelId}/credentials/test` → 显示结果
- 凭证启用/禁用/删除

### 2.4 模型规格 Tab

**模型规格列表（Table）：**

| 列 | 说明 |
|----|------|
| 供应商模型 ID | ModelSpec.providerModelId（如 `gpt-4o`） |
| 显示名 | ModelSpec.displayName |
| 模型族 | ModelSpec.modelFamily |
| 上下文窗口 | ModelSpec.contextWindow |
| 能力标签 | Tag 组（`chat`、`vision`、`streaming` 等） |
| 状态 | Tag 渲染 ModelSpecState |
| 操作 | 编辑、启用/禁用、删除 |

**模型规格操作：**

- 创建：Modal 弹窗表单
- 编辑：Modal 弹窗表单
- 启用/禁用：确认弹窗
- 删除：确认弹窗

**后端 API 对齐：**

ModelSpec 目前只有 Domain 层（`ModelSpec` 实体、`ModelSpecGateway`、`ModelSpecDomainService`），需补充：

- `ModelSpecController` — REST 端点
- `ModelSpecService` / `ModelSpecServiceImpl` — 应用层服务
- DTO — `ModelSpecCreateRequest`、`ModelSpecUpdateRequest`、`ModelSpecResponse`、`ModelSpecQueryRequest`

### 2.5 连通性测试

**交互流程：**

1. 基本信息Tab 中"测试连通性"按钮
2. 调用 `POST /providers/test-connectivity`
3. 结果分两层展示（Collapse 组件）：
   - **认证层**：API Key 有效性（成功/失败 + 错误信息）
   - **模型层**：模型可用性列表（每个模型：可用/不可用 + 错误信息）
4. 测试中按钮 loading 状态，防止重复点击

**其他交互增强：**

- 供应商卡片/表格行 hover 显示快捷操作（编辑、删除、测试连通性）
- 创建供应商模板选择后自动填充 `description`
- 删除确认弹窗增加"关联渠道数"提示

## 3. 后端 API 清单

| API | 方法 | 路径 | 状态 |
|-----|------|------|------|
| 供应商 CRUD | 已实现 | `/providers/**` | ✅ |
| 连通性测试 | 已实现 | `POST /providers/test-connectivity` | ✅ |
| 渠道 CRUD | 已实现 | `/channels/**` | ✅ |
| 凭证 CRUD | 已实现 | `/channels/{id}/credentials/**` | ✅ |
| 凭证测试 | 已实现 | `POST /channels/{id}/credentials/test` | ✅ |
| 模型规格 CRUD | **待实现** | `/model-specs/**` | ❌ |
| 端点 CRUD | 已实现 | `/channels/{id}/endpoints/**` | ✅ |

## 4. 前端文件变更清单

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `ProviderManagementDrawer.tsx` | 修改 | 扩展为 3-tab 体系，宽度 720px |
| `ProviderBasicInfoTab.tsx` | 修改 | 补全 description、连通性测试 |
| `ProviderChannelTab.tsx` | **重写** | 渠道列表 + 展开行(端点+凭证) |
| `ProviderCredentialTab.tsx` | 删除 | 合并入 ProviderChannelTab |
| `ProviderCreateModal.tsx` | 修改 | 补全 description 字段 |
| `ProvidersTableView.tsx` | **重写** | 完整表格视图实现 |
| `ProviderCardView.tsx` | 修改 | 增加渠道数/模型数统计 |
| `ProviderCard.tsx` | 修改 | 增加渠道数/模型数标签 |
| `index.tsx` | 修改 | 增加状态筛选、视图切换器 |
| `services/api/modelSpec.ts` | **新增** | 模型规格 API 调用 |
| `services/query/useModelSpecs.ts` | **新增** | 模型规格 React Query hooks |
| `types/modelSpec.ts` | **新增** | 模型规格类型定义 |
| `locales/zh-CN/providers.json` | 修改 | 补全新增文案 |

## 5. 权限矩阵

| 操作 | `PROVIDER_READ` | `PROVIDER_WRITE` |
|------|:---:|:---:|
| 查看供应商列表 | ✅ | ✅ |
| 查看供应商详情 | ✅ | ✅ |
| 查看渠道/凭证/模型 | ✅ | ✅ |
| 创建供应商 | - | ✅ |
| 编辑供应商 | - | ✅ |
| 删除供应商 | - | ✅ |
| 启用/禁用供应商 | - | ✅ |
| 创建渠道/凭证/模型 | - | ✅ |
| 编辑渠道/凭证/模型 | - | ✅ |
| 删除渠道/凭证/模型 | - | ✅ |
| 连通性测试 | - | ✅ |
| 凭证测试 | - | ✅ |
