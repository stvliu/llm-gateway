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
  - `last_health_status` (enum: HEALTHY / DEGRADED / FAILED / UNKNOWN)
  - `last_health_source` (enum: CARD / DRAWER / PRECHECK，区分触发来源)
- 列表卡片在 Provider Header 下、状态 Tag 旁渲染一个小型健康指示点，hover 显示最后一次测试时间与来源
- Provider 菜单中的 `ConnectivityTestPanel` 改名为「预检工具」，UI 文案明确"在创建渠道前测试 baseUrl + Key 的可用性"，与已建渠道完全脱耦

**聚合规则**（多 Key 场景）：
- 所有 Key 测试通过 → HEALTHY
- 部分通过、部分失败 → DEGRADED
- 全部失败 → FAILED
- 无 Key 或测试未执行 → UNKNOWN

**替代方案**：保留三入口仅统一文案与持久化 → 用户认知负担仍存在。

### D5：危险操作确认 — 删除类全部升级到 Modal.confirm

**选择**：
- 暂停（→ SUSPENDED）：从无确认升级为 `Popconfirm`（轻量级，因为可恢复），文案"暂停后该渠道不再分配流量，但保留配置"
- 删除 API Key、删除端点、删除模型映射：从 `Popconfirm` 统一升级到 `Modal.confirm`（`okType: danger`，含 description），文案需明确"删除后无法恢复"+"对运行流量的具体影响"
- 删除整个渠道、转 RETIRED：保持现有 `Modal.confirm`，文案微调与上述对齐
- `InlineEditableList` 的删除回调签名扩展，由调用方传入 `Modal.confirm` 配置，组件本身不再硬编码 Popconfirm

**替代方案**：保守方案仅补暂停 / 中等方案仅强化 Popconfirm 文案 → 视觉一致性仍不足；激进方案与「删除是真危险操作」的认知一致。

### D6：错误反馈兜底 — 不变量"错误必反馈"

**选择**：
- 全量审计 `pages/Channels/` 下所有 mutation 的 catch 块，把仅有注释的分支补齐 `message.error`
- 抽取通用 mutation 错误处理 hook `useMutationWithFeedback`，封装"成功 → message.success + 脉冲，失败 → message.error + 行内红色"
- 在前端代码层面建立 lint 规则或 review 清单，未来禁止空 catch

### D7：后端字段与 API 边界

**新增字段**（Channel 表）：
```sql
ALTER TABLE channels ADD COLUMN last_health_check_at TIMESTAMP NULL;
ALTER TABLE channels ADD COLUMN last_health_status VARCHAR(16) NULL;
ALTER TABLE channels ADD COLUMN last_health_source VARCHAR(16) NULL;
CREATE INDEX idx_channels_last_health_status ON channels(last_health_status);
```

**新增端点**：
- `POST /api/channels/{id}/health-check`：执行测试并写入健康状态。请求体声明 source 字段。响应包含矩阵详情 + 聚合状态
- 现有 `GET /api/channels` 的响应 DTO 增加 3 个字段返回（向后兼容）

**事务性 Provision**：
- 扩展 `ChannelProvisionService.provisionFromPlan` 入参，新增可选 `inlineProvider` 字段
- 当 `inlineProvider` 非空且 `providerCode` 在数据库不存在时，先创建 Provider 再创建 Channel，整个过程在单事务内
- 创建失败时事务回滚，不留孤儿 Provider

## Risks / Trade-offs

| 风险 | 应对 |
|---|---|
| 内嵌 Provider 表单导致 Wizard 状态机变复杂 | 用独立的 sub-step 索引 + Provider 表单组件作为受控组件，状态扁平化 |
| 测试持久化的并发问题：用户 A 在测试时用户 B 也触发测试 | 后端用 last-write-wins，时间戳作为决胜字段；前端不做乐观锁 |
| 删除类升级到 Modal.confirm 后误触下降但操作时长上升 | 验收场景包含"高频用户连续删除 5 行"的体验确认；如反馈过重，Phase 2 引入"7 天内不再提示"开关 |
| 状态 SSOT 整合改动影响其他页面引用 | 提供向后兼容的 `STATE_CONFIG` 与 `STATE_TRANSITION_LABELS` 导出别名，后续 Phase 2 统一清理 |
| 健康字段写入失败影响测试主流程 | 字段写入为副作用，主流程返回测试结果即可；写入失败仅记日志，不阻塞 UI |
| 主页面移除「+ 新增供应商」按钮影响习惯路径 | 在 Wizard 第一步的"选择供应商"下拉中显著呈现「+ 新建供应商」入口；批量导入仍可创建无渠道的供应商 |

## Migration Plan

1. **后端先行**：DB 迁移（增 3 列）+ `ChannelProvisionService` 扩展 + 新端点。向后兼容，旧 API 不变。
2. **前端跟进**：按"组件级"逐步替换：
   - 第一批：错误反馈兜底（D6）+ 暂停确认（D5 子集），改动最小、风险最低
   - 第二批：状态 SSOT 整合 + Tooltip + RETIRED 重设（D3）
   - 第三批：保存脉冲（D2）
   - 第四批：测试入口归一 + 健康指示点（D4）
   - 第五批：删除类升级 Modal.confirm（D5 剩余）
   - 第六批：创建入口合并（D1，改动最大）
3. **回滚策略**：每批独立 PR、独立可回滚；后端字段无非空约束，可保留不用

## Open Questions

- 健康指示点在卡片上的位置：状态 Tag 旁 vs Provider Header 旁 vs 单独右上角徽标 → comet-design 阶段做视觉验证
- 测试矩阵 Table 的列宽与移动端折叠规则 → comet-design 阶段决定
- `useMutationWithFeedback` 是否进一步抽到全局 services 层供其他页面复用 → 看本期实现量后决定
