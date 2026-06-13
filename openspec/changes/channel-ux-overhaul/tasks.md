## 1. 后端：健康状态字段与持久化

- [x] 1.1 数据库迁移：channels 表新增 last_health_check_at / last_health_status / last_health_source 三列与索引
- [x] 1.2 Channel JPA 实体新增三字段，Repository / Gateway 层透传
- [x] 1.3 定义健康状态枚举 ChannelHealthStatus（HEALTHY / DEGRADED / FAILED / UNKNOWN）与来源枚举 ChannelHealthSource（CARD / DRAWER / PRECHECK）
- [x] 1.4 编写实体字段迁移与枚举的单元测试

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

## 4. 前端：测试栈引入

- [x] 4.1 安装依赖：vitest / @testing-library/react / @testing-library/user-event / @testing-library/jest-dom / jsdom / @playwright/test
- [x] 4.2 配置 vite.config.ts 的 test 段、vitest.setup.ts、playwright.config.ts
- [x] 4.3 package.json 增加 test / test:watch / test:e2e scripts
- [x] 4.4 编写 1 个 smoke test（如对 ChannelStateTag 的快照测试）验证 RTL 工作
- [x] 4.5 编写 1 个 Playwright smoke test（如打开 /channels 主页面）验证 E2E 工作

## 5. 前端：错误反馈兜底（最低风险）

- [ ] 5.1 审计 pages/Channels/ 全部 mutation catch 块，列出所有空 catch / 仅注释 catch 的位置
- [ ] 5.2 改造 EndpointSection.tsx 错误反馈，至少补齐 62-63 行与 89-90 行两处
- [ ] 5.3 改造 CredentialSection / ModelMappingSection / QuotaSettingsSection 的 catch 块
- [ ] 5.4 单元测试覆盖错误路径

## 6. 前端：状态语义可视化与 SSOT 整合

- [ ] 6.1 用 codegraph 列出 STATE_CONFIG / STATE_TRANSITION_LABELS 全部引用点
- [ ] 6.2 新建 src/domain/channel/lifecycle.ts，定义 CHANNEL_LIFECYCLE 与 selector helpers，新增中英文 i18n key（含 stateXxxDesc / tooltipRoutable / tooltipBilling / tooltipNext / tooltipTerminal）
- [ ] 6.3 替换全部引用为新 SSOT，删除旧 STATE_CONFIG / STATE_TRANSITION_LABELS 导出（无别名）
- [ ] 6.4 ChannelStateTag 增加 Tooltip：内容由 buildStateTooltip 派生
- [ ] 6.5 PENDING 黄色加深至 #d48806 保证 4.5:1 对比度
- [ ] 6.6 RETIRED 卡片视觉重设：移除 opacity 0.5，渠道名 line-through + 灰调 #8c8c8c
- [ ] 6.7 DEPRECATED 卡片增加副标题小字"仍参与流量分配，但已标记为不推荐"
- [ ] 6.8 lifecycle.test.ts 单元测试 + ChannelStateTag.test.tsx 组件测试

## 7. 前端：保存反馈脉冲

- [ ] 7.1 实现 useSavePulse hook（src/components/common/useSavePulse.ts）：state 切换 + 3s 自动清除 success + 错误常驻 + cleanup
- [ ] 7.2 实现 SavePulse.css：keyframes + reduced-motion 适配 + save-tip-ok / save-tip-err 样式
- [ ] 7.3 EndpointSection 接入 useSavePulse + onMutate 乐观更新 + onError 回滚 + 行内反馈渲染
- [ ] 7.4 CredentialSection 接入同上
- [ ] 7.5 ModelMappingSection 接入同上
- [ ] 7.6 QuotaSettingsSection 编辑模式保存成功后对编辑区触发同款脉冲
- [ ] 7.7 useSavePulse.test.tsx 单元测试 + EndpointSection / CredentialSection 组件测试覆盖成功 / 失败两路径

## 8. 前端：危险操作确认升级

- [ ] 8.1 实现 useDangerConfirm hook（含 contextHolder + i18n + danger okType）
- [ ] 8.2 InlineEditableList 删除回调签名扩展：调用方注入 confirm 配置
- [ ] 8.3 暂停操作（→ SUSPENDED）所有入口加 Popconfirm，文案"暂停后该渠道不再分配流量，但保留配置"
- [ ] 8.4 删除 API Key 改用 useDangerConfirm（content 含 keyMasked + "删除后无法恢复，使用此 Key 的请求将立即失败"）
- [ ] 8.5 删除端点改用 useDangerConfirm（content 包含对路由的影响说明）
- [ ] 8.6 删除模型映射改用 useDangerConfirm（content 说明该模型 ID 不再被路由）
- [ ] 8.7 删除整个渠道 / 转 RETIRED 文案与上述对齐
- [ ] 8.8 useDangerConfirm.test.tsx 单元测试

## 9. 前端：测试入口归一与健康指示

- [ ] 9.1 渠道卡片闪电图标行为改造：打开详情抽屉 + 跳到 Credentials Tab + "测试全部" 800ms 高亮
- [ ] 9.2 实现 HealthDot 组件（4 状态颜色 + UNKNOWN 空心 + Popover 显示 lastCheckAt + source）
- [ ] 9.3 ChannelCard 在状态 Tag 右侧 6px 处嵌入 HealthDot
- [ ] 9.4 ProviderGroupHeader 加"N/M 健康"小字聚合
- [ ] 9.5 详情抽屉"连通性测试"重构为唯一执行入口，结果以矩阵 Table 展现（脱敏 Key × 列：认证/可用模型/延迟）
- [ ] 9.6 测试触发 POST /api/channels/{id}/health-check 写入健康状态；前端 AbortController 取消支持
- [ ] 9.7 ConnectivityTestPanel 改名为"预检工具"，UI 文案明确与已建渠道脱耦，发起请求 source=PRECHECK
- [ ] 9.8 useChannels 数据层补充健康字段
- [ ] 9.9 HealthDot.test.tsx 组件测试 + Playwright e2e/health-check-matrix.spec.ts (S5)

## 10. 前端：创建入口合并

- [ ] 10.1 拆分 ProviderCreateModal 为可复用的 ProviderForm 组件（受控组件）
- [ ] 10.2 QuickOnboardMode 状态扁平化：增加 inlineProviderExpanded / inlineProvider 字段，定义不变量
- [ ] 10.3 Step 0 增加"+ 新建供应商"链接，点击展开 Step 0.5 内联 ProviderForm；切换分支时 clear 对方
- [ ] 10.4 Step 0 校验逻辑：`selectedProviderCode != null` 或 (`inlineProviderExpanded && inlineProvider valid`)
- [ ] 10.5 最终提交 payload 含 inlineProvider 字段（仅当走内联路径）
- [ ] 10.6 主页面顶部移除独立的"+ 新增供应商"按钮，相关引用清理
- [ ] 10.7 QuickOnboardMode.test.tsx 状态机测试 + Playwright e2e/onboard-inline-provider.spec.ts (S1)

## 11. 国际化与文案统一

- [ ] 11.1 整理本期所有新增 / 修改的 i18n key（中英文）
- [ ] 11.2 文案审校：危险操作 description / 状态 Tooltip / 保存反馈 / 错误反馈
- [ ] 11.3 codegraph 验证文案 key 全部被引用，无孤立 key

## 12. 联调与回归

- [ ] 12.1 前后端联调：覆盖 9 条端到端验收场景（S1-S7 + 内联创建 + 错误反馈）
- [ ] 12.2 视觉走查：状态色对比度、脉冲动画、矩阵 Table 在 1280×800 下的呈现；决定 ProviderHeader 聚合是否保留
- [ ] 12.3 Playwright e2e/delete-key-confirm.spec.ts (S6)
- [ ] 12.4 回归测试：旧路径（批量导入、模板创建、批量导出）不受影响
- [ ] 12.5 准备验证报告草稿，列出每条 Requirement 的覆盖测试
