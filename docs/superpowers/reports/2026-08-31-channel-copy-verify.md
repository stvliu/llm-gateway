# channel-copy 验证报告

- **日期**: 2026-08-31
- **Workflow**: Comet tweak（verify_mode: full）
- **验证轮次**: 第 2 轮（第 1 轮发现 2 项 WARNING 场景覆盖缺口，verify-fail 回 build 修复后复验通过）
- **报告语言**: zh-CN（comet 配置产物语言）

## 检查结论总表

| # | 检查项 | 结果 | 证据 |
|---|--------|------|------|
| 1 | tasks.md 全部任务已完成 `[x]` | PASS | 19/19 步骤勾选；build 守卫 `tasks.md all tasks checked` PASS |
| 2 | OpenSpec schema 校验 | PASS | `comet classic openspec -- validate --changes channel-copy` → `✓ change/channel-copy`（1 passed, 0 failed） |
| 3 | 实现符合 design.md 高层设计决策 | PASS | 逐项对照见下文「设计决策对照」 |
| 4 | Design Doc（docs/superpowers/specs/） | 不适用 | tweak 预设流程无 Design Doc（用户确认继续 tweak） |
| 5 | 能力规格场景全部通过 | PASS | delta spec 11 场景，10 个有自动化测试覆盖，1 个 SUGGESTION（见下文） |
| 6 | proposal.md 目标已满足 | PASS | 后端 copy 接口 + web 端点 + 404/409 错误映射 + 前端双入口/弹窗/hook 全部落地 |
| 7 | delta spec 与 design 无矛盾 | PASS | design.md 与 delta spec 语义一致 |
| 8 | docs/superpowers/specs/ 关联文档 | 不适用 | tweak 预设流程无 Design Doc |
| 9 | 编译/构建 | PASS | `./mvnw -pl gateway-provider/provider,gateway-web -am test` → BUILD SUCCESS |
| 10 | 后端测试 | PASS | gateway-provider 323 用例 + gateway-web 122 用例，0 Failures 0 Errors |
| 11 | 前端测试 | PASS | `npx vitest run src/pages/Channels src/pages/Models` → 24 文件 57 用例全过；`npx tsc -b --noEmit` 无错误 |
| 12 | 无明显安全问题 | PASS | 无硬编码密钥；凭证复制复用后端解密明文走既有加密通道（`ChannelCredentialService.create`），默认不复制（最小权限）；无新增 unsafe 操作 |

## 设计决策对照（design.md → 实现）

| design.md 决策 | 实现 | 状态 |
|---|---|---|
| `ChannelService.copy(Long sourceId, Channel override, boolean copyCredentials)` 单事务 | `ChannelServiceImpl.copy`（@Transactional） | 一致 |
| 源不存在抛 `ResourceNotFoundException`（404） | ChannelServiceImpl:80 | 一致 |
| name 唯一校验 `existsByProviderIdAndName` → `DuplicateResourceException`（409） | ChannelServiceImpl:87 | 一致 |
| 复制本体 + 重置 state=ACTIVE、健康字段清空 | ChannelServiceImpl:92-104 | 一致 |
| 端点复制（新 channelId）、模型实例复制重置 ACTIVE | ChannelServiceImpl:107-126 | 一致 |
| 凭证复制（copyCredentials=true）复用明文走 create 重新加密 | ChannelServiceImpl:129-141 | 一致 |
| web 层 `POST /{id}/copy` + `ChannelCopyRequest{name@NotBlank, copyCredentials}` | ChannelController:64-69 + ChannelCopyRequest.java | 一致 |
| 经 `ChannelFacade.copy` 组装 providerName/endpoints | ChannelFacade:62-67 | 一致 |
| 前端 `channelApi.copy` + `useCopyChannel`（invalidate lists/allChannels） | channel.ts / useChannels.ts | 一致 |
| `CopyChannelModal` 预填 name + 凭证复选框默认不勾选 + extractErrorMessage | CopyChannelModal.tsx | 一致 |
| 双入口：ChannelTableView 操作列 + ChannelCard 操作区（CopyOutlined） | ChannelTableView.tsx / ChannelCard.tsx / index.tsx | 一致 |

## delta spec 场景覆盖（11/11 核对）

**Requirement: 复制渠道配置（5 场景）**

| 场景 | 覆盖 |
|---|---|
| 正常复制渠道（继承+重置） | `ChannelServiceImplTest.copy_inheritsConfigAndResetsState` |
| 复制凭证 | `copy_withCredentials_copiesCredentials` |
| 源渠道不存在→404 | 服务层 `copy_sourceNotFound_throws` + API 契约 `ChannelControllerCopyTest.copy_sourceNotFound_notFound`（404 + $.error.code=NOT_FOUND） |
| 同供应商重名→409 | 服务层 `copy_duplicateName_throws` + API 契约 `copy_duplicateName_conflict`（409 + $.error.code=CONFLICT） |
| 复制失败整体回滚 | `copy_credentialFailure_propagates`（异常传播）+ `@Transactional` 保证 |

**Requirement: 渠道复制 API（3 场景）**：默认不复制凭证 / 携带凭证 / name 缺失 400 —— `ChannelControllerCopyTest` 3 用例（响应断言对齐 `ApiResponseWrapperAdvice` 的 `$.data.*` 契约）。

**Requirement: 复制渠道控制台入口（3 场景）**

| 场景 | 覆盖 |
|---|---|
| 表格行内复制 | `ChannelsIndex.copyButton.test.tsx`（点击 CopyOutlined → CopyChannelModal open + source 对应行） |
| 卡片操作区复制 | SUGGESTION（见下文）：按钮实现与表格对称（同一 copySource→CopyChannelModal 链路），链路已被 Modal 组件测试与表格按钮测试覆盖 |
| 复制凭证复选框 | `CopyChannelModal.test.tsx`（默认 false / 勾选 true / name 空拦截） |

## 第 1 轮发现与修复记录

| 发现 | 级别 | 处理 |
|---|---|---|
| 404/409 场景缺 API 层契约测试（模型复制有对齐先例） | WARNING | `comet state transition verify-fail` 回 build，补 2 个契约用例（commit `dc45d9fc`） |
| 表格行内复制场景无自动化测试 | WARNING | 补 `ChannelsIndex.copyButton.test.tsx`（同 commit） |
| （修复中发现）契约测试响应断言未对齐 `ApiResponseWrapperAdvice` 的 `$.data.*` 契约、standalone 装配缺 advice | 随修复 | setUp 装配 `ApiResponseWrapperAdvice` + `GlobalExceptionHandler`，断言改 `$.data.*`（与 `ModelControllerTest` 及 commit b02b5ab7 先例对齐） |

## SUGGESTION（不阻塞归档）

- **卡片操作区复制按钮无独立自动化测试**：`ChannelCard` 的复制按钮与表格按钮共用同一 `copySource` 状态与 `CopyChannelModal` 链路，实现完全对称；Modal 链路已有 3 个组件用例覆盖。风险低，可在后续维护中按需补充。
- **ESLint 既有告警**：`ChannelCard.tsx`/`ChannelTableView.tsx` 的 `buildMenuItems` 存在既有 `any[]` 告警（非本次改动引入，保持最小改动原则未动）。

## 最终评估

无 CRITICAL / WARNING 问题。全部检查项通过（2 项因 tweak 流程不适用）。**验证通过，可进入归档。**
