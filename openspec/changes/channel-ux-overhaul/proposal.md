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
