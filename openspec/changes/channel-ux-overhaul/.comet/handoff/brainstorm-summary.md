# Brainstorm Summary

- Change: channel-ux-overhaul
- Date: 2026-06-13

## 确认的技术方案

### 1. 状态 SSOT 整合（方案 C）
- 新建 `gateway-console/src/domain/channel/lifecycle.ts` 作为唯一来源，承载 `CHANNEL_LIFECYCLE: Record<ChannelState, LifecycleMeta>`，含 8 个事实字段：`label`、`descriptionKey`、`color`、`tagColor`、`isRoutable`、`isBilling`、`nextStates`、`visualStyle`
- **完全替换无别名**：旧 `ChannelStateTag.STATE_CONFIG` 与 `stateTransitions.STATE_TRANSITION_LABELS` 全量删除，所有引用位置改为从 lifecycle.ts import；本期 PR 改动面较大但代码最干净
- 提供纯函数 selector helpers：`isRoutable(s)`、`allowedTransitions(s)`、`buildStateTooltip(s, t)`，方便 unit test

### 2. 反馈 hook（方案 C）
- 抽 `useSavePulse` 仅管视觉反馈：`{ ref, state, errorMsg, className, triggerSuccess, triggerError }`
- 组件正常用 React Query `useMutation`，在 onSuccess/onError 调用 `pulse.triggerSuccess()` / `pulse.triggerError(msg)`
- 错误回滚到上一保存值由调用方在 `onMutate` optimistic update + `onError` 回滚 cache 实现，hook 不强加状态管理风格

### 3. 脉冲动画（方案 A 纯 CSS keyframes）
- `.save-pulse-success`：`background-color` 从 0 → 0.20 → 0 在 800ms 内，ease-out
- `.save-pulse-error`：1px inset shadow 红边 1.5 秒
- 「✓ 已保存」3 秒淡出；「✗ <原因>」常驻直到下次 trigger
- `@media (prefers-reduced-motion: reduce)` 用静态透明度替代脉冲

### 4. 测试矩阵 API + Schema
- 端点：`POST /api/channels/{id}/health-check`，body `{ source: "CARD" | "DRAWER" | "PRECHECK" }`
- 响应：`{ channelId, aggregateStatus, startedAt, finishedAt, matrix: [{ credentialId, keyMasked, auth, authError?, availableModels: string[], latencyMs }] }`
- **后端 CompletableFuture 并行**测试所有 Key，单 Key 5s 超时，总 30s `orTimeout`，前端 fetch timeout 35s
- **availableModels 返回完整字符串数组**（非计数），由前端在 Tooltip / 折叠面板展示
- **后端负责脱敏 keyMasked**，前端不接触明文；同时返回 credentialId 便于针对单 Key 后续操作
- 前端用 `AbortController` 实现取消（关闭 Drawer 时 abort）

### 5. 健康指示点位置（方案 a）
- 8px 圆点放在状态 Tag 右侧 6px 间距处
- HEALTHY=#52c41a / DEGRADED=#faad14 / FAILED=#ff4d4f / UNKNOWN=#bfbfbf 空心圆
- hover 弹 Popover：「最后一次测试：<时间>（来自 <来源>） · X/Y Key 通过」
- Provider Header 旁附加「N/M 健康」小字聚合（补充信息）

### 6. 创建入口合并的状态机（方案 A 延迟提交）
- 扁平 state：`{ selectedProviderCode, inlineProviderExpanded, inlineProvider, ... }`
- `selectedProviderCode` 与 `inlineProviderExpanded` 互斥
- Step 0.5 仅做前端校验，不调 API
- 最终提交时一次性 POST `provisionFromPlan({ ..., inlineProvider })`
- 后端事务保证零孤儿；中途取消纯前端 state 丢弃即可

### 7. 后端事务性 Provision
- 现状 `provisionFromPlan` 已 `@Transactional` 覆盖整个流程；`CatalogException → GatewayException → RuntimeException` 默认触发回滚
- `ProvisionRequest` 增加 `InlineProvider inlineProvider` 字段（含 code/name/description/websiteUrl/apiDocUrl）
- 改造 `ensureProvider(code, inline)`：providerCode 已存在时返回现有 Provider 并忽略 inline；不存在时根据 inline 填充字段（无 inline 时 fallback 到 `name=code`）
- 集成测试用强制抛异常验证回滚

### 8. 后端聚合规则位置（方案 A）
- 新建 `application/supply/ChannelHealthService`
- 聚合规则作为私有静态方法 `aggregate(List<KeyTestResult>) → ChannelHealthStatus`
- 矩阵结果 DTO 与持久化字段映射：`aggregateStatus → last_health_status`、`finishedAt → last_health_check_at`、`request.source → last_health_source`

### 9. 健康字段并发处理（方案 A last-write-wins）
- 不加 `@Version` 乐观锁，不加条件更新
- 直接 UPDATE，timestamp 自然胜出
- 每次写入记 INFO 日志便于事后审计

### 10. Modal.confirm 复用（方案 A）
- 抽 `useDangerConfirm()` hook：接受 `{ titleKey, descriptionKey, descriptionParams, onOk }`
- 内部 `Modal.confirm({ okType: 'danger', okText, cancelText })` 含 i18n
- 5 处删除 + 1 处暂停（注：暂停用 Popconfirm 而非 Modal.confirm，所以不复用此 hook）调用一致

### 11. 测试策略
- **后端**：Unit (聚合 + ensureProvider) / Repository (新字段) / Integration (`/health-check` E2E + `provisionFromPlan` 内联与回滚)，已有 JUnit + Mockito + H2 栈
- **前端**：本期一次性引入 **Vitest + @testing-library/react + jsdom + Playwright**
  - Unit：CHANNEL_LIFECYCLE / useSavePulse / useDangerConfirm
  - Component：EndpointSection / CredentialSection mutation 双路径、ChannelStateTag Tooltip、健康指示点 hover、QuickOnboardMode 状态机
  - **E2E (Playwright) 3 条**：
    - S1（创建路径合并 + 内联 Provider）
    - S5（测试矩阵 + 持久化）
    - S6（删除 Key Modal.confirm）

### 12. Spec Patch（采纳）
追加到 `specs/channel-console-ux/spec.md` "连通性测试入口归一" Requirement 下：

```markdown
#### Scenario: 预检工具的测试结果不持久化
- **WHEN** 用户从供应商分组菜单的"预检工具"完成连通性测试
- **THEN** 系统不写入任何已建渠道的 last_health_check_at / last_health_status / last_health_source 字段
```

## 关键取舍与风险

| 风险 | 应对 |
|---|---|
| SSOT 完全替换无别名 → PR 改动面大，可能影响其他页面引用 | 改造前用 codegraph 全量定位 STATE_CONFIG/STATE_TRANSITION_LABELS 引用点，一次性替换；视为 D3 改造批的开篇任务，独立 commit |
| 后端 CompletableFuture 并行可能撞上某些上游限流 | 单 Key 5s 超时已是软保护；如未来发现需要节流，可加 `Semaphore(N)` 限并发，本期不做 |
| Vitest + RTL + Playwright 一次性引入会拉长 Phase 1 工期 3-5 天 | 用户已知情接受；测试栈引入作为独立 task group（第 4 组），完成后再开始功能开发 |
| `useSavePulse` 的 ref 注入与 antd `Table` rowClassName 协调可能要适配 | Component 测试覆盖 EndpointSection / CredentialSection 在 Tag 行的脉冲渲染，发现问题就地适配 |
| 内联 Provider 的 code 字段可能与已删 Provider 的 code 冲突（unique 约束） | DB 约束冲突会触发回滚（Spec scenario 已覆盖"任意步骤失败"），前端展示 message.error |
| 健康指示点 + Provider 聚合两层显示可能冗余 | 视觉走查阶段确认；如冗余则保留卡片级，去掉 Provider 级 |

## 测试策略（汇总）

见 §11。要点：**前端栈本期建立，后端栈复用现有，E2E 仅覆盖 3 条最关键场景**。

## Spec Patch

补充至 `specs/channel-console-ux/spec.md`：预检工具的测试结果不持久化（详见 §12）。
