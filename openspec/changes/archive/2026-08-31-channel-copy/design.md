# 通道复制 Design

## 后端复制语义（对齐模型复制）

`ChannelService.copy(Long sourceId, Channel override, boolean copyCredentials)` 在单一事务内：

1. **查源渠道**：`channelRepository.findById(sourceId)`，不存在抛 `ResourceNotFoundException("Channel", sourceId)`（→ 404）
2. **name 唯一校验**：复制必须产生不同的 name，与源同名同样拒绝。通道唯一键是 `(providerId, name)`，用 `existsByProviderIdAndName(source.getProviderId(), override.getName())`，命中抛 `DuplicateResourceException("Channel", "name")`（→ 409）。与模型复制对齐使用 `DuplicateResourceException`（非渠道 create 的 `GatewayRequestException` 400），以获得正确的 409 语义
3. **复制本体**：`providerId`（继承源）/`name`（必填覆盖）/`billingMode`/`quotaLimit`/`timeout`/`maxRetries`（继承源）；**重置** `state=ACTIVE`（与 create 默认一致）、健康字段 `lastHealthCheckAt/lastHealthStatus/lastHealthSource` 清空；不复制 id/审计字段
4. **复制端点**：`channelEndpointRepository.findByChannelId(sourceId)` → 逐条复制 `protocol/endpointUrl`，`channelId` 指向新渠道（同渠道同协议唯一约束天然满足）
5. **复制模型实例**：`modelInstanceRepository.findByChannelId(sourceId)` → 逐条复制 `modelId/upstreamModelName/capabilitiesOverride/contextWindowOverride/priority/weight/quotaLimit`，`channelId` 指向新渠道；**状态重置 ACTIVE**（对齐模型复制"新实体为可用状态"；若保留 PENDING 则不可路由且无 PENDING→ACTIVE 级联触发）
6. **复制凭证（仅 `copyCredentials=true`）**：`channelCredentialService.listByChannelId(sourceId)` 取凭证（含解密明文 `apiKeyPlain`）→ 逐条调 `channelCredentialService.create()`（`channelId` 指向新渠道，明文 Key 按既有加密策略重新加密）

`@Transactional` 保证整体原子性；任一子实体复制失败整体回滚。

## web 层端点

`ChannelController` 新增 `POST /api/v1/channels/{id}/copy`：

- `ChannelCopyRequest` DTO：`name(@NotBlank)` + `copyCredentials(boolean, 默认 false)` + `toEntity()` 组装覆盖字段 `Channel`
- 返回 `ChannelFacade.toResponse` 同款组装（providerName + endpoints），Controller 内直接复用 `ChannelService.copy` + 私有组装（copy 端点不改变 Facade 既有契约）

## 前端

- `channelApi.copy(id, data)` + `useCopyChannel()`（mutation，成功后 invalidate `channelKeys.lists()/allChannels()`，参照 `useAddChannel`）
- `CopyChannelModal`：props `{ open, source: ChannelCard, onClose }`；表单预填 `name`（必填）+ `copyCredentials` Checkbox（默认不勾选，文案提示复制 API Key）；提交调 `useCopyChannel`，错误用 `extractErrorMessage` 透出后端消息（对齐 `CopyModelModal`）
- 入口：`ChannelTableView` 操作列 + `ChannelCard` 操作区各新增复制按钮（`CopyOutlined`），点击 `setCopySource(record)` 打开弹窗；成功后关闭弹窗（列表由 invalidate 自动刷新）
- i18n：沿用保守原则，`t(key, { defaultValue })` 兜底，不新增语言文件

## 测试策略（TDD）

| 层 | 用例 |
|---|---|
| `ChannelServiceTest` | 复制继承本体+重置 ACTIVE/健康字段；复制端点与模型实例；copyCredentials=false 不复制凭证 / true 复制；源不存在 404；同供应商重名 409；失败回滚（凭证复制抛异常 → 渠道不落库） |
| `ChannelControllerTest` | POST copy 返回新渠道；name 缺失 400 |
| 前端组件测试 | CopyChannelModal 预填/必填/复选框提交；表格与卡片复制按钮打开弹窗 |
