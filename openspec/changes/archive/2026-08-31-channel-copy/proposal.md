# 通道复制 Proposal

## Why

管理员经常需要基于已有渠道快速创建配置相似的渠道（同一供应商、相同计费/配额/超时策略）。当前只能逐字段手动重建，且端点、模型实例映射、凭证需重新配置，效率低且易出错。参照已落地的模型复制功能（`POST /api/v1/models/{id}/copy`），为通道提供同等的一键复制能力。

## What Changes

- 后端 `ChannelService.copy(sourceId, override, copyCredentials)`：复制源通道本体配置 + 端点 + 模型实例；凭证（API Key）复制由参数控制（安全最小权限，默认不复制）
- web 层新增 `POST /api/v1/channels/{id}/copy` 端点 + `ChannelCopyRequest` DTO（`name` 必填 + `copyCredentials` 复选框）
- 业务错误 HTTP 映射对齐模型复制：源不存在 → 404（`ResourceNotFoundException`）、同供应商重名 → 409（`DuplicateResourceException`）
- 前端新增复制入口（通道表格操作列 + 卡片操作区）+ `CopyChannelModal`（预填源配置、name 必填、凭证复制复选框默认不勾选）+ `useCopyChannel` hook
- 复制语义：继承源通道计费/配额/超时/重试配置；重置通道 state=ACTIVE、健康字段清空；模型实例重置为 ACTIVE（可用状态）

## Capabilities

### New Capabilities
- `channel-copy`: 通道一键复制能力——后端 copy 接口（本体+端点+模型实例+可选凭证）+ 前端行内复制按钮与复制弹窗

### Modified Capabilities
<!-- 无：通道供给（channel-provision）、通道 UX 等既有 spec 的验收场景不受影响，复制是独立新增能力 -->

## Impact

- `gateway-provider/provider`：`ChannelService`/`ChannelServiceImpl` 新增 `copy` 方法（复用 `ChannelRepository`/`ChannelEndpointRepository`/`ModelInstanceRepository`/`ChannelCredentialService`）+ 单测
- `gateway-web`：`ChannelController` 新增 copy 端点 + `ChannelCopyRequest` DTO + 契约测试
- `gateway-console`：`channel.ts`（copy API）、`useChannels.ts`（`useCopyChannel`）、`types/channel.ts`（`CopyChannelRequest`）、新建 `CopyChannelModal`、`ChannelTableView`/`ChannelCard` 接入复制按钮 + 组件测试
- 无数据库 schema 变更、无新依赖
