# Comet Design Handoff

- Change: channel-ux-overhaul
- Phase: design
- Mode: compact
- Context hash: 34ad811c705303460ec9d4e0d4c290b85f57ce0e981310f1a61af178c9696781

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/channel-ux-overhaul/proposal.md

- Source: openspec/changes/channel-ux-overhaul/proposal.md
- Lines: 1-46
- SHA256: d217e7e59deca9e5b1bbae2f86c5fa416d9edf17bb3069e3c5cff4be57317ad9

```md
## Why

LLM-Gateway 控制台的「渠道管理」页面经历快速迭代后，积累了多处交互摩擦：创建路径割裂为「先建供应商，再建渠道」两跳；端点/凭证/模型/配额 4 个 Section 的保存策略不一致，用户无法确定改动是否生效；渠道生命周期五状态（PENDING/ACTIVE/SUSPENDED/DEPRECATED/RETIRED）的实际语义对用户不透明，DEPRECATED 仍参与路由这一关键事实在 UI 上完全没有提示；连通性测试存在三个不一致的入口，结果不持久化导致刷新即丢失；暂停操作与多类删除操作的二次确认强度参差，部分错误路径甚至被静默吞掉。

本期不追求功能扩展，而是把这些"用户每天都会遇到的小摩擦"集中清理一轮，使页面从可用走向顺手。

## What Changes

- **创建入口合并**：`Channel Create Wizard` 第一步内嵌「+ 新建供应商」子表单（Step 0.5）；移除主页面顶部独立的「+ 新增供应商」按钮，将供应商创建作为渠道创建流程的派生分支
- **保存反馈脉冲**：保留现有 Section 的即时保存策略，但每次 mutation 成功后行尾短暂出现绿色脉冲 + 「✓ 已保存」3 秒消失；失败时行内出现红色边框 + 「✗ 保存失败：<原因>」
- **状态语义可视化**：5 个状态 Tag 全部加 `Tooltip`，明确告知「是否参与流量分配 / 是否计费 / 后续可转换状态」；DEPRECATED 卡片增加副标题小字「仍参与流量分配，但已标记为不推荐」；RETIRED 卡片去掉 `opacity: 0.5` 改为 `text-decoration: line-through` + 灰调，保证 WCAG AA 对比度；`STATE_CONFIG` 与 `STATE_TRANSITION_LABELS` 整合为单一 SSOT
- **测试入口统一**：渠道卡片闪电图标改为「打开详情抽屉并跳到 Credentials Tab + 高亮『测试全部 Key』」；详情抽屉的「连通性测试」成为唯一执行入口，结果以矩阵展示（每行一个 Key × 列：认证 / 模型可用性 / 延迟），并持久化到 Channel 实体；Provider 菜单中的 `ConnectivityTestPanel` 改名为「预检工具」，定位为创建渠道前的探针，与已建渠道脱耦
- **危险操作确认补齐**：暂停（→ SUSPENDED）从无确认升级为 `Popconfirm`；所有删除类操作（删除 Key / 删除端点 / 删除模型映射）统一升级到 `Modal.confirm`（含 description + danger okType）
- **错误反馈兜底**：审计 Channels 目录下所有 mutation 的 catch 块，把空 catch 或仅注释的分支统一补上 `message.error`，建立"错误必反馈"的不变量
- **新增**：Channel 实体新增 `last_health_check_at` / `last_health_status` / `last_health_source` 三个字段以支撑测试结果持久化
- **新增**：`Provision API` 扩展为支持「事务性创建 Provider+首个 Channel」，创建失败时回滚不留孤儿 Provider

## Capabilities

### New Capabilities

- `channel-console-ux`：控制台渠道管理页面的交互行为契约，覆盖列表/详情/创建/测试/状态展示/反馈这一组前端可观察行为。该 spec 描述前端组件对外可观察的交互结果（如保存成功后的反馈、状态 Tooltip 内容、危险操作的确认形态），不规定具体实现技术
- `channel-health-tracking`：渠道连通性测试结果的持久化与读取。定义 Channel 实体新增的健康状态字段、测试结果写入触发点、聚合规则、读取接口

### Modified Capabilities

- `channel-provision`：扩展现有 `provisionFromPlan` 的能力，支持「内联创建 Provider」模式。新增 Scenario 覆盖事务性回滚要求

## Impact

**前端（gateway-console）**
- `src/pages/Channels/`：`index.tsx`、`ChannelCreateWizard.tsx`、`QuickOnboardMode.tsx`、`ChannelDetailDrawer.tsx`、`ChannelOverviewTab.tsx`、`ChannelCard.tsx`、`EndpointSection.tsx`、`CredentialSection.tsx`、`ModelMappingSection.tsx`、`QuotaSettingsSection.tsx`、`ConnectivityTestPanel.tsx`、`ProviderCreateModal.tsx`、`InlineEditableList.tsx`
- `src/components/common/ChannelStateTag.tsx`、`src/utils/stateTransitions.ts`、`src/services/query/useChannels.ts`
- 国际化资源（中英文）相关 key

**后端（gateway-boot）**
- `domain/supply/`：`Channel` 实体新增 3 个字段 + 对应 Repository/Gateway
- `application/supply/ChannelProvisionService`：新增 `provisionWithInlineProvider` 方法或扩展现有方法的入参
- `application/supply/ChannelHealthService`（新建）：测试完成时写入健康状态的服务
- `adapter/api/`：相关 Controller 增加字段返回 + 新端点（如 `POST /api/channels/{id}/health-check`）
- 数据库迁移：`channels` 表新增 3 列

**不涉及**
- 路由层、协议适配层、调度层、规则引擎
- 监控/账单/审计基础设施
- 现有 5 个 spec 中的 `entity-lifecycle`、`team-channel-management`、`intelligent-degradation`、`upstream-exception-classification`、`catalog-cascade-materialize`、`model-instance`（仅在 design 阶段做依赖说明，不修改其 Requirements）
```

## openspec/changes/channel-ux-overhaul/design.md

- Source: openspec/changes/channel-ux-overhaul/design.md
- Lines: 1-157
- SHA256: ca3346412105be14df7b0178df23a25f361d9f29467be20e6a3cbc6e8ff2c9ae

[TRUNCATED]

```md
## Context

控制台「渠道管理」页面是 LLM-Gateway 管理员日常使用频率最高的页面之一，承担渠道接入、配置维护、连通性诊断、生命周期治理四大职责。现状代码在 `gateway-console/src/pages/Channels/` 下共 21 个组件、约 3900 行，经多轮迭代后形成以下用户可观察的不一致：

- **创建路径**：用户必须先在主页面顶部点「+ 新增供应商」(`ProviderCreateModal`)，回到主页再点「+ 新增渠道」(`ChannelCreateWizard` + `QuickOnboardMode`)，两个动作之间无任何衔接。
- **保存策略**：`EndpointSection`、`ModelMappingSection`、`CredentialSection` 即时保存（每次输入触发 mutation），`QuotaSettingsSection` 进入编辑模式后批量保存。三种保存策略下用户无法从视觉上确认"我刚才改的字段到底有没有落库"。
- **状态展示**：5 个生命周期状态（PENDING/ACTIVE/SUSPENDED/DEPRECATED/RETIRED）通过 `ChannelStateTag` 用色块呈现，但 DEPRECATED 仍可路由（`stateTransitions.ts:38-40` 的 `isRoutable` 返回 true）这一关键事实在 UI 上没有任何提示；RETIRED 卡片采用 `opacity: 0.5` 导致文字对比度低于 WCAG AA。
- **测试入口**：渠道卡片闪电图标测第一个 Key、详情抽屉 header 测全部 Key、Provider 菜单的 `ConnectivityTestPanel` 是独立表单。三套语义、三种结果展现、三种成功反馈方式。测试结果不持久化，刷新即丢。
- **危险操作**：暂停（→ SUSPENDED）无任何二次确认；删除 Key/端点用 `Popconfirm`，删除渠道用 `Modal.confirm`，视觉风格不一致。
- **错误反馈**：`EndpointSection.tsx:62-63` 等多处 catch 块只写 `// 校验失败` 注释，错误被静默吞掉。

技术约束：
- 前端基于 React + Ant Design + React Query，已建立 `useChannels` 数据层
- 后端基于 Spring Boot 3.5 + JPA，`Channel` 实体已遵循 COLA Light 5.0 分层
- 状态机由 `entity-lifecycle` spec 定义，本期不修改状态机本身，仅增强 UI 可见性
- 现有 `provisionFromPlan` API（`channel-provision` spec）支持从 Plan 创建渠道，但要求 Provider 已存在

## Goals / Non-Goals

**Goals**

1. 把"从零接入第一个渠道"的操作步骤从 7+ 次跨页面跳转压缩到 1 个 Drawer 内闭合
2. 让每一次保存动作都可视、可感知，错误路径必须有明确反馈
3. 让用户 hover 任何状态 Tag 都能在 1 秒内理解该状态的业务后果（流量分配 / 计费 / 后续可达状态）
4. 把"渠道是否健康"这一信息从一次性的 toast 变成 Channel 实体的可观测属性，列表卡片直接呈现
5. 把高影响操作（暂停 / 任何形式的删除）的视觉强度统一到与其后果相称的级别

**Non-Goals**

- 不引入模型选择器、不接入真实的 Token / 成本指标、不做表格分页与批量操作（Phase 2）
- 不实现 API Key 明文显示与一键复制（涉及独立的安全策略评估，Phase 2）
- 不做响应式与可访问性专项（Phase 3）
- 不引入 Cmd+K 命令面板等高级用户能力（Phase 3）
- 不修改 `entity-lifecycle` 状态机定义、不修改路由 / 调度 / 协议适配
- 不实现删除操作的"撤销"能力

## Decisions

### D1：创建入口形态 — Wizard 内嵌 Provider 子表单

**选择**：在 `QuickOnboardMode` 的第一步（"选择套餐"）增加一个分支入口「+ 新建供应商」，点击后在同一 Drawer 内展开 Provider 表单作为 Step 0.5；表单提交成功后自动选中新建的供应商，进入 Step 1 端点配置。主页面顶部移除独立的「+ 新增供应商」按钮，将 `ProviderCreateModal` 重构为可被 Wizard 复用的 Provider 表单组件。

**替代方案**：
- (b) 保留双入口 + toast 引导跳转 → 路径仍长、状态在两个组件间传递麻烦
- (c) 模板为主、自由创建为辅 → 改动面过大，本期不做

**理由**：a 方案路径最短，且保留了"先创建 Provider 后才能用"的旧能力（通过批量导入或后续手工创建），不破坏现有用户的肌肉记忆。

### D2：保存策略 — 即时保存 + 视觉脉冲反馈

**选择**：保留 `EndpointSection`、`ModelMappingSection`、`CredentialSection` 的即时保存策略；在 mutation 成功 callback 中触发行级视觉反馈（绿色背景脉冲 ~800ms + 行尾「✓ 已保存」3 秒淡出）；mutation 失败时改为红色边框 + 行尾「✗ 保存失败：<原因>」+ `message.error`。`QuotaSettingsSection` 因字段较少且互相关联，保留批量保存模式，但保存按钮成功后同样触发脉冲。

**替代方案**：
- (A) 全部改为批量保存 + dirty 状态管理 → 实现成本高，且与现有用户习惯背离
- (C) 用户可切换 → 增加配置复杂度，违背"无形交互"原则

**理由**：现状即时保存对快速调参的高级用户更友好；缺的只是反馈层。脉冲方案改动小，风险低。

实现要点：
- 抽取通用 hook `useSavePulse(elementRef)` 封装脉冲动画
- 用纯 CSS keyframes（不引入 framer-motion）保持包体大小

### D3：状态语义可视化 — Tooltip + SSOT 整合 + RETIRED 视觉重设

**选择**：
- 把 `ChannelStateTag.tsx` 的 `STATE_CONFIG` 与 `stateTransitions.ts` 的 `STATE_TRANSITION_LABELS` 合并为单一 SSOT，新增 3 个字段：`isRoutable`、`isBilling`、`description`、`nextStates`
- 5 个状态 Tag 全部加 Tooltip，内容由 SSOT 派生：「当前状态：{label}」+「{description}」+「流量：{是否参与}」+「计费：{是否计费}」+「可转换至：{nextStates 列表}」
- DEPRECATED 卡片增加副标题小字："仍参与流量分配，但已标记为不推荐"
- RETIRED 卡片：移除 `opacity: 0.5`；改为渠道名应用 `text-decoration: line-through`，配色用 `#8c8c8c`（确保 4.5:1 对比度）；保留状态 Tag 的红色作为视觉锚点
- PENDING Tag 的黄色从 `#faad14` 加深到 `#d48806`，配合白色文字保证 4.5:1

**替代方案**：转换后果预览（"转 SUSPENDED 后将不再接收流量"）是更强的方案，但增加 Modal 内的内容复杂度，本期不做。

### D4：测试入口归一 + 健康状态持久化

**选择**：
- 渠道卡片闪电图标改为「打开详情抽屉 + 跳到 Credentials Tab + 高亮『测试全部 Key』按钮 800ms」，不再就地弹 toast
- 详情抽屉的「连通性测试」是唯一执行入口，结果以矩阵 Table 展现：行=Key（脱敏），列=认证状态/可用模型数/延迟/时间戳
- 测试完成后，前端调用新增的 `POST /api/channels/{id}/health-check` 端点，后端持久化 3 个字段：
  - `last_health_check_at` (timestamp)
```

Full source: openspec/changes/channel-ux-overhaul/design.md

## openspec/changes/channel-ux-overhaul/tasks.md

- Source: openspec/changes/channel-ux-overhaul/tasks.md
- Lines: 1-88
- SHA256: 4d0d00f4994f2bf23026605d5f7d87bd70f872c896bbbd0be3ea763c417c4442

[TRUNCATED]

```md
## 1. 后端：健康状态字段与持久化

- [ ] 1.1 数据库迁移：channels 表新增 last_health_check_at / last_health_status / last_health_source 三列与索引
- [ ] 1.2 Channel JPA 实体新增三字段，Repository / Gateway 层透传
- [ ] 1.3 定义健康状态枚举 ChannelHealthStatus（HEALTHY / DEGRADED / FAILED / UNKNOWN）与来源枚举 ChannelHealthSource（CARD / DRAWER / PRECHECK）
- [ ] 1.4 编写实体字段迁移与枚举的单元测试

## 2. 后端：连通性测试 API 与聚合

- [ ] 2.1 新建 ChannelHealthService（application 层），实现"测试矩阵 + 聚合规则 + last-write-wins 写入"
- [ ] 2.2 ChannelHealthService 单元测试：覆盖 HEALTHY / DEGRADED / FAILED / UNKNOWN 四种聚合分支与持久化失败兜底
- [ ] 2.3 新建 POST /api/channels/{id}/health-check 端点（adapter/api/），请求体含 source 字段，返回矩阵详情 + 聚合状态
- [ ] 2.4 GET /api/channels 与 GET /api/channels/{id} 的响应 DTO 增加三个健康字段（向后兼容）
- [ ] 2.5 端点集成测试：覆盖三种 source、零 Key、并发触发场景

## 3. 后端：事务性 Provision 扩展

- [ ] 3.1 ChannelProvisionService.provisionFromPlan 入参新增可选 inlineProvider 字段
- [ ] 3.2 实现"providerCode 不存在 + inlineProvider 非空 → 单事务创建 Provider + Channel"逻辑
- [ ] 3.3 验证创建过程任意步骤失败均整体回滚（含级联 Provider / 级联 Model 失败用例）
- [ ] 3.4 provision API 集成测试：覆盖正常路径、回滚路径、providerCode 已存在时忽略 inlineProvider

## 4. 前端：错误反馈兜底（最低风险）

- [ ] 4.1 审计 pages/Channels/ 全部 mutation catch 块，列出所有空 catch / 仅注释 catch 的位置
- [ ] 4.2 抽取 useMutationWithFeedback hook，统一 message.success + 脉冲 / message.error + 行内红色行为
- [ ] 4.3 改造 EndpointSection.tsx 错误反馈，至少补齐 62-63 行与 89-90 行两处
- [ ] 4.4 改造 CredentialSection / ModelMappingSection / QuotaSettingsSection 的 catch 块
- [ ] 4.5 单元测试覆盖错误路径

## 5. 前端：状态语义可视化与 SSOT 整合

- [ ] 5.1 重构状态配置：将 ChannelStateTag.STATE_CONFIG 与 stateTransitions.STATE_TRANSITION_LABELS 合并为单一 SSOT，新增 isRoutable / isBilling / description / nextStates 字段
- [ ] 5.2 ChannelStateTag 增加 Tooltip：内容由 SSOT 派生（描述 + 是否参与流量分配 + 是否计费 + 可转换至）
- [ ] 5.3 PENDING 黄色加深至 #d48806 保证 4.5:1 对比度
- [ ] 5.4 RETIRED 卡片视觉重设：移除 opacity 0.5，渠道名 line-through + 灰调 #8c8c8c
- [ ] 5.5 DEPRECATED 卡片增加副标题小字"仍参与流量分配，但已标记为不推荐"
- [ ] 5.6 SSOT 单元测试与组件快照测试

## 6. 前端：保存反馈脉冲

- [ ] 6.1 实现通用 useSavePulse hook（CSS keyframes，绿色背景 ~800ms + "✓ 已保存" 3 秒淡出）
- [ ] 6.2 实现失败态：行内红色边框 + "✗ 保存失败：<原因>" 内联展示，字段值回滚到上一保存值
- [ ] 6.3 在 EndpointSection / CredentialSection / ModelMappingSection 的保存成功 / 失败 callback 中接入脉冲
- [ ] 6.4 QuotaSettingsSection 保存成功后对编辑区触发同款脉冲
- [ ] 6.5 视觉走查：确认动画与 antd 主题协调
- [ ] 6.6 组件测试覆盖成功 / 失败两条路径

## 7. 前端：危险操作确认升级

- [ ] 7.1 InlineEditableList 删除回调签名扩展：调用方可注入 Modal.confirm 配置
- [ ] 7.2 暂停操作（→ SUSPENDED）所有入口加 Popconfirm，文案"暂停后该渠道不再分配流量，但保留配置"
- [ ] 7.3 删除 API Key 升级到 Modal.confirm（danger okType + description）
- [ ] 7.4 删除端点升级到 Modal.confirm（danger okType + description）
- [ ] 7.5 删除模型映射升级到 Modal.confirm（danger okType + description）
- [ ] 7.6 删除整个渠道 / 转 RETIRED 文案与上述对齐
- [ ] 7.7 端到端测试覆盖每条确认路径

## 8. 前端：测试入口归一与健康指示

- [ ] 8.1 渠道卡片闪电图标行为改造：打开详情抽屉 + 跳到 Credentials Tab + "测试全部" 800ms 高亮
- [ ] 8.2 详情抽屉"连通性测试"重构为唯一执行入口，结果以矩阵 Table 展现（脱敏 Key × 列）
- [ ] 8.3 测试完成后调用 POST /api/channels/{id}/health-check 写入健康状态
- [ ] 8.4 ConnectivityTestPanel 改名为"预检工具"，UI 文案明确与已建渠道脱耦
- [ ] 8.5 列表卡片在状态 Tag 旁渲染健康指示点，hover 显示最后一次测试时间与来源
- [ ] 8.6 useChannels 数据层补充健康字段，列表 / 详情查询返回包含三字段
- [ ] 8.7 端到端测试：从卡片点击 → 跳转 → 测试 → 持久化 → 列表反映

## 9. 前端：创建入口合并

- [ ] 9.1 拆分 ProviderCreateModal 为可复用的 ProviderForm 组件
- [ ] 9.2 QuickOnboardMode Step 0 增加"+ 新建供应商"链接，点击展开 Step 0.5 内联 Provider 表单
- [ ] 9.3 内联表单提交调用 provision API（携带 inlineProvider 参数），成功后自动选中并进入 Step 1
- [ ] 9.4 主页面顶部移除独立的"+ 新增供应商"按钮，相关引用一并清理
- [ ] 9.5 端到端测试：覆盖"选已有供应商"+"内联创建供应商"+"内联创建中途取消不留孤儿"三条路径

## 10. 国际化与文案统一

- [ ] 10.1 整理本期所有新增 / 修改的 i18n key（中英文），避免与现有 key 冲突
- [ ] 10.2 文案审校：危险操作 description / 状态 Tooltip / 保存反馈 / 错误反馈
```

Full source: openspec/changes/channel-ux-overhaul/tasks.md

## openspec/changes/channel-ux-overhaul/specs/channel-console-ux/spec.md

- Source: openspec/changes/channel-ux-overhaul/specs/channel-console-ux/spec.md
- Lines: 1-117
- SHA256: c62a2dacfd6e19c5159a20c9de8208fb08949e60dda3a81a74ddc0548b10a898

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: 渠道创建入口单一闭合

控制台主页面 SHALL 仅暴露一个渠道接入入口（"+ 新增渠道"按钮），渠道创建向导第一步 SHALL 同时支持选择已有供应商和在同一 Drawer 内联创建新供应商。

#### Scenario: 选择已有供应商进入创建
- **WHEN** 用户点击主页面"+ 新增渠道"按钮，并在第一步选择一个已有供应商
- **THEN** 向导直接进入端点配置步骤，无需跳出 Drawer

#### Scenario: 内联创建供应商后接续创建渠道
- **WHEN** 用户在向导第一步点击"+ 新建供应商"链接，填写并提交供应商表单
- **THEN** 向导在同一 Drawer 内完成供应商创建，并自动选中新建供应商进入端点配置步骤

#### Scenario: 内联创建中途取消
- **WHEN** 用户在内联创建供应商成功后取消向导
- **THEN** 系统通过事务性 API 确保不留下孤儿供应商（已通过 channel-provision 能力保障）

#### Scenario: 主页面不再暴露独立的供应商创建入口
- **WHEN** 用户访问 /channels 主页面
- **THEN** 顶部工具栏不存在独立的"+ 新增供应商"按钮，仅保留"+ 新增渠道"、"批量导入"、"批量导出"

### Requirement: 字段保存反馈可视化

渠道详情抽屉内所有支持即时保存的字段（端点、凭证、模型映射），SHALL 在保存成功后显示视觉反馈，SHALL 在保存失败时显示明确的错误反馈。

#### Scenario: 即时保存成功后的视觉脉冲
- **WHEN** 用户在端点/凭证/模型映射 Section 行内编辑某个字段并触发保存，且后端返回成功
- **THEN** 该行短暂显示绿色背景脉冲（约 800ms），行尾出现"✓ 已保存"标记并在 3 秒内淡出

#### Scenario: 保存失败时的内联错误反馈
- **WHEN** 用户编辑某字段触发保存，且后端返回错误或请求失败
- **THEN** 该行显示红色边框，行尾显示"✗ 保存失败：<原因>"，同时全局 message.error 提示原因，且字段值回滚到上一个已保存值

#### Scenario: 配额批量保存的反馈一致
- **WHEN** 用户在配额 Section 进入编辑模式并点击"保存"
- **THEN** 保存成功后 Section 切回展示模式，并对编辑区触发与即时保存相同的脉冲反馈

### Requirement: 渠道生命周期状态语义可见

渠道生命周期五个状态（PENDING / ACTIVE / SUSPENDED / DEPRECATED / RETIRED）的 UI 呈现，SHALL 让用户在不离开当前页面的情况下理解每个状态的业务后果。

#### Scenario: 状态 Tag 提供 Tooltip 说明
- **WHEN** 用户 hover 任意状态 Tag
- **THEN** Tooltip 显示状态名称、状态描述、是否参与流量分配、是否计费、可转换至的下一状态列表

#### Scenario: DEPRECATED 状态的特殊提示
- **WHEN** 渠道卡片或详情显示状态为 DEPRECATED
- **THEN** 该卡片/标题区域包含小字说明"仍参与流量分配，但已标记为不推荐使用"

#### Scenario: RETIRED 状态的视觉处理
- **WHEN** 渠道卡片显示状态为 RETIRED
- **THEN** 卡片不再使用 opacity 0.5 整体降透，而是渠道名称加 line-through 样式、文字颜色保证 WCAG AA 对比度（≥ 4.5:1）

#### Scenario: 状态配置的单一来源
- **WHEN** 任何组件需要读取状态的颜色、文案、是否可路由、是否计费、可转换状态信息
- **THEN** 这些信息从单一的状态配置 SSOT 派生，不允许在多个文件中重复定义状态属性

### Requirement: 连通性测试入口归一

渠道连通性测试的执行入口 SHALL 集中到详情抽屉，列表卡片的测试图标 SHALL 仅作为快捷跳转引导，供应商级别的"预检工具"SHALL 与已建渠道明确解耦。

#### Scenario: 卡片闪电图标作为跳转引导
- **WHEN** 用户在渠道列表卡片点击闪电图标
- **THEN** 系统打开该渠道的详情抽屉，自动切换到 Credentials Tab，并对"测试全部"按钮做 800ms 高亮提示

#### Scenario: 详情抽屉的测试矩阵展现
- **WHEN** 用户在详情抽屉点击"连通性测试"
- **THEN** 测试结果以矩阵 Table 展现：每行一个 Key（脱敏显示），列包括认证状态、可用模型数、延迟、测试时间戳

#### Scenario: 预检工具与已建渠道脱耦
- **WHEN** 用户从供应商分组菜单打开"预检工具"
- **THEN** UI 文案明确告知"用于在创建渠道前测试 baseUrl + Key 的可用性"，且测试结果不写入任何已建渠道的健康状态字段

#### Scenario: 列表卡片显示最近一次健康状态
- **WHEN** 渠道有至少一次连通性测试记录
- **THEN** 列表卡片在状态 Tag 旁渲染健康指示点，hover 显示"最后一次测试：<时间> 来源：<卡片/详情/预检>"

#### Scenario: 预检工具的测试结果不持久化
- **WHEN** 用户从供应商分组菜单的"预检工具"完成连通性测试
```

Full source: openspec/changes/channel-ux-overhaul/specs/channel-console-ux/spec.md

## openspec/changes/channel-ux-overhaul/specs/channel-health-tracking/spec.md

- Source: openspec/changes/channel-ux-overhaul/specs/channel-health-tracking/spec.md
- Lines: 1-65
- SHA256: cf2713ac57c6cea21ccd98c2aedaeb29c88b5c9fdfb76c925ebf863801d5b07d

```md
## ADDED Requirements

### Requirement: Channel 实体扩展健康状态字段

Channel 实体 SHALL 持久化最近一次连通性测试的结果摘要，以便列表与卡片视图能够直接呈现渠道健康状态而无需每次重新测试。

#### Scenario: 实体新增三个健康字段
- **WHEN** 系统启动并完成数据库迁移
- **THEN** channels 表存在 last_health_check_at（TIMESTAMP NULL）、last_health_status（VARCHAR NULL）、last_health_source（VARCHAR NULL）三列

#### Scenario: 健康状态枚举有限值
- **WHEN** last_health_status 字段被写入
- **THEN** 其值必须属于以下枚举：HEALTHY、DEGRADED、FAILED、UNKNOWN

#### Scenario: 健康来源枚举有限值
- **WHEN** last_health_source 字段被写入
- **THEN** 其值必须属于以下枚举：CARD、DRAWER、PRECHECK

#### Scenario: 未测试的渠道字段为 null
- **WHEN** 渠道从未执行过连通性测试
- **THEN** 三个健康字段保持 null，前端将渠道健康状态视为 UNKNOWN

### Requirement: 连通性测试结果聚合与持久化

系统 SHALL 提供专用的连通性测试 API，并按既定聚合规则将多 Key 测试结果聚合为单一健康状态写入 Channel 实体。

#### Scenario: 提供专用健康检查端点
- **WHEN** 客户端发起 POST /api/channels/{id}/health-check 请求，请求体包含 source 字段（CARD/DRAWER/PRECHECK）
- **THEN** 系统对该渠道下所有 Key 执行连通性测试，返回矩阵详情（每个 Key 的认证状态、可用模型数、延迟）+ 聚合状态

#### Scenario: 全部 Key 通过聚合为 HEALTHY
- **WHEN** 测试矩阵中所有 Key 的认证均成功且返回了至少一个可用模型
- **THEN** 聚合状态为 HEALTHY，写入 last_health_status

#### Scenario: 部分 Key 失败聚合为 DEGRADED
- **WHEN** 测试矩阵中至少一个 Key 通过且至少一个 Key 失败
- **THEN** 聚合状态为 DEGRADED，写入 last_health_status

#### Scenario: 全部 Key 失败聚合为 FAILED
- **WHEN** 测试矩阵中所有 Key 均认证失败或无任何可用模型
- **THEN** 聚合状态为 FAILED，写入 last_health_status

#### Scenario: 无 Key 时聚合为 UNKNOWN
- **WHEN** 渠道没有任何 Key
- **THEN** 聚合状态为 UNKNOWN，写入 last_health_status；不报错

#### Scenario: 持久化失败不阻断主流程
- **WHEN** 健康字段写入数据库失败
- **THEN** API 仍然返回测试矩阵详情，仅记录错误日志，不影响响应状态码

#### Scenario: 并发测试采用 last-write-wins
- **WHEN** 同一渠道在短时间内被多次触发测试
- **THEN** 最终持久化的 last_health_check_at 为最晚完成的那次，其他字段同步覆盖

### Requirement: Channel 列表响应包含健康状态

GET /api/channels 响应 DTO SHALL 在不破坏现有契约的前提下，附加返回三个健康字段，以便前端列表卡片直接渲染健康指示点。

#### Scenario: 列表 DTO 向后兼容地新增字段
- **WHEN** 客户端请求 GET /api/channels
- **THEN** 响应中每个渠道对象包含 lastHealthCheckAt、lastHealthStatus、lastHealthSource 三个字段（可为 null），原有字段保持不变

#### Scenario: 详情 DTO 同样包含字段
- **WHEN** 客户端请求 GET /api/channels/{id}
- **THEN** 响应包含三个健康字段
```

## openspec/changes/channel-ux-overhaul/specs/channel-provision/spec.md

- Source: openspec/changes/channel-ux-overhaul/specs/channel-provision/spec.md
- Lines: 1-46
- SHA256: 38558c0ca238c243ce5e9d243b06acc3c72b0dd523b97f289ba955323e50943a

```md
## ADDED Requirements

### Requirement: 内联创建供应商的事务性渠道供给

系统 SHALL 在 `provisionFromPlan` 流程中支持"内联创建供应商 + 渠道"的事务原子性，避免控制台向导中途取消时残留孤儿 Provider。

#### Scenario: 控制台向导内联创建路径
- **WHEN** 客户端调用 provision API 时携带 inlineProvider 参数（包含 code/name/description 等字段），且 providerCode 在数据库不存在
- **THEN** 系统在单个事务内先创建 Provider，再创建 Channel + ChannelEndpoint + ModelInstance + ChannelCredential

#### Scenario: 内联创建过程中失败回滚
- **WHEN** 内联创建供应商或后续渠道创建过程中任意步骤抛出异常
- **THEN** 整个事务回滚，数据库不会出现孤儿 Provider 或部分创建的渠道

#### Scenario: providerCode 已存在时忽略 inlineProvider
- **WHEN** 客户端传入 inlineProvider 但 providerCode 对应的 Provider 已存在
- **THEN** 系统忽略 inlineProvider 字段，按现有"级联创建 Provider"逻辑使用已有 Provider

## MODIFIED Requirements

### Requirement: 从套餐创建渠道
系统 SHALL 提供 ChannelProvisionService，支持从 PlanCatalog 创建 Channel + ChannelEndpoint + ModelInstance。创建过程中自动级联创建缺失的 Provider 和 Model（使用最小信息）。整个创建过程 SHALL 在单一数据库事务内完成，任何步骤失败均整体回滚。

#### Scenario: 正常创建渠道
- **WHEN** 管理员调用 provisionFromPlan(planCode, request)
- **THEN** 系统创建 Channel（name=planCode，关联 Provider）、ChannelEndpoint（从 endpoints JSON 解析）、ModelInstance（从 pricing JSON 解析，级联创建缺失的 Model）

#### Scenario: 级联创建 Provider
- **WHEN** PlanCatalog.providerCode 对应的 Provider 不存在
- **THEN** 系统自动创建 Provider（code=providerCode，name=providerCode，priority=100，state=ACTIVE）

#### Scenario: 级联创建 Model
- **WHEN** pricing JSON 中某个 modelName 对应的 Model 不存在
- **THEN** 系统自动创建 Model（modelName=modelName，displayName=modelName，state=ACTIVE）

#### Scenario: 渠道已存在
- **WHEN** Channel 已存在（providerId + name 匹配）
- **THEN** 系统返回 skipped 状态，不重复创建

#### Scenario: 创建渠道时传入 API Key
- **WHEN** request 包含 apiKeys 列表
- **THEN** 系统为创建的 Channel 批量创建 ChannelCredential

#### Scenario: 创建过程任意步骤失败整体回滚
- **WHEN** 创建 Channel / ChannelEndpoint / ModelInstance / ChannelCredential / 级联 Provider / 级联 Model 任意步骤抛出异常
- **THEN** 数据库事务回滚，不会出现部分创建的实体或孤儿 Provider
```

