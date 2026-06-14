# 验证报告：channel-ux-overhaul

| # | Requirement | Scenario | 自动化测试 / 验证方式 | 结果 |
|---|---|---|---|---|
| S1 | 内联创建供应商 → 接续渠道 | 在向导第一步点击"+ 新建供应商"链接，填写并提交，自动选中新建供应商进入端点配置 | e2e/onboard-inline-provider.spec.ts + 单元测试: QuickOnboardMode.test.tsx, ProviderForm.test.tsx, ChannelsIndex.providerButton.test.tsx | ✅ |
| S2 | 状态语义可视化：Tooltip / RETIRED line-through / DEPRECATED 副标题 | Hover 状态 Tag 显示 Tooltip（含状态描述/可路由/可计费/可转换状态）；RETIRED 渠道名 line-through + 灰调；DEPRECATED 显示副标题"仍参与流量分配，但已标记为不推荐" | 单元测试: lifecycle.test.ts, ChannelStateTag.test.tsx, ChannelStateTag.smoke.test.tsx, ChannelCard.test.tsx | ✅ |
| S3 | 保存反馈脉冲 | 即时保存成功显示绿色脉冲 + "✓ 已保存" 3s 淡出；失败显示红色边框 + "✗ 保存失败" + 全局 message.error + 字段回滚 | 单元测试: useSavePulse.test.tsx, EndpointSection.pulse.test.tsx, CredentialSection.pulse.test.tsx, ModelMappingSection.pulse.test.tsx, QuotaSettingsSection.pulse.test.tsx | ✅ |
| S4 | 危险操作确认升级 | 暂停操作 Popconfirm；删除 API Key / 端点 / 模型映射 / 渠道使用 Modal.confirm（danger okType + 影响说明） | 单元测试: useDangerConfirm.test.tsx, CredentialSection.delete.test.tsx, EndpointSection.delete.test.tsx, ModelMappingSection.delete.test.tsx, ChannelCard.delete.test.tsx, ChannelCard.suspend.test.tsx | ✅ |
| S5 | 连通性测试入口归一 | 卡片闪电图标 → 跳转详情抽屉 Credentials Tab → "测试全部" 800ms 高亮 → 矩阵 Table → 关闭后卡片显示健康指示点 | e2e/health-check-matrix.spec.ts + 单元测试: HealthDot.test.tsx, ChannelCard.healthDot.test.tsx, ChannelCard.testIcon.test.tsx, ProviderGroupHeader.healthSummary.test.tsx, ChannelDetailDrawer.healthMatrix.test.tsx, ConnectivityTestPanel.precheck.test.tsx | ✅ |
| S6 | 删除 API Key 确认对话框 | 删除 API Key 弹出 Modal.confirm 含"删除后无法恢复"和"请求将立即失败"，确认/取消按钮交互 | e2e/delete-key-confirm.spec.ts + 单元测试: CredentialSection.delete.test.tsx | ✅ |
| S7 | 错误反馈不变量 | 所有 mutation 失败必有用户可见反馈（行内错误标记 + message.error 全局提示至少其一）；校验失败字段显示校验错误信息 | 单元测试: error-feedback.test.tsx, EndpointSection.pulse.test.tsx（失败路径）, CredentialSection.pulse.test.tsx（失败路径） | ✅ |
| S8 | 渠道创建入口单一闭合 | 主页面无独立"+ 新增供应商"按钮，仅保留"+ 新增渠道"/"批量导入"/"批量导出" | 单元测试: ChannelsIndex.providerButton.test.tsx, QuickOnboardMode.test.tsx | ✅ |
| S9 | 国际化与文案统一 | 所有新增/修改 i18n key 中英文定义完整；危险操作 description / 状态 Tooltip / 保存反馈 / 错误反馈文案审校；无孤立 key | codegraph 验证文案 key 全部被引用 + 视觉走查 | ✅ |

## 后端集成测试

| # | 测试文件 | 覆盖内容 |
|---|---|---|
| B1 | gateway-boot/.../supply/entity/ChannelHealthFieldsTest.java | 健康状态枚举 + 数据库字段持久化 |
| B2 | gateway-boot/.../supply/ChannelHealthServiceTest.java | 连通性测试聚合规则（HEALTHY/DEGRADED/FAILED/UNKNOWN） |
| B3 | gateway-boot/.../catalog/ChannelProvisionServiceInlineProviderTest.java | 事务性 Provision：内联创建 Provider + Channel、回滚、已存在忽略 |

## 回归测试

| # | 范围 | 验证方式 |
|---|---|---|
| R1 | 批量导入渠道 | 手动回归（旧路径不受影响） |
| R2 | 模板创建渠道 | 手动回归（旧路径不受影响） |
| R3 | 批量导出渠道 | 手动回归（旧路径不受影响） |
