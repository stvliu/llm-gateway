## Why

供应商模块是 LLM-Gateway 的核心管理功能，但前端存在多项功能缺口和 UX 问题：无法查看提供商级统计、无批量操作、创建流程断层、部分页面模型数据错误、渠道无分页等。这些问题影响运营效率，需要在现有基础上补齐关键功能并优化交互体验。

## What Changes

### 功能补齐
- 新增提供商级统计面板（请求量/Token 消耗/错误率）
- 提供商列表支持批量启用/停用
- 提供商复制功能（以现有提供商为模板快速创建新提供商）
- 创建向导扩展：支持在创建提供商时一并完成渠道和初始模型关联
- 渠道列表添加服务端分页

### Bug 修复
- 修复 ProviderCard 中 `useModels()` 全量拉取问题，改为按提供商过滤

### 体验优化
- 端点协议选项从 `ProtocolController` 动态加载，替换硬编码
- 补充缺失的 mutation 成功反馈消息
- 优化渠道标签点击跳转到抽屉的渠道标签页

**BREAKING**: 无

## Capabilities

### New Capabilities
- `provider-statistics`: 提供商级统计信息（请求量、Token消耗、错误率），在提供商详情抽屉中新增标签页
- `batch-operations`: 提供商的批量启用/停用功能
- `provider-duplicate`: 提供商复制功能

### Modified Capabilities

无（项目中尚无正式 specs 定义）

## Impact

| 影响范围 | 说明 |
|---------|------|
| gateway-console/src/pages/Providers/ | 主页面、CardView、Drawer、ChannelTab 等 13 个文件，新增 3-4 个组件 |
| gateway-console/src/services/query/ | 新增 `useProviderStats` hook，扩展现有 hooks |
| gateway-console/src/locales/ | 补充新的 i18n 翻译键 |
| gateway-boot ProviderController | 可能需要新增统计相关 API 端点 |