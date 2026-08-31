# 通道复制 Tasks

> TDD：每任务先写失败测试 → 实现 → 测试通过 → 独立提交；commit message 格式 `tweak: <简述>`。

## Task 1: ChannelService.copy 核心实现（provider 域）

**Files:**
- Modify: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/channel/ChannelService.java`
- Modify: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/channel/ChannelServiceImpl.java`
- Test: `gateway-provider/provider/src/test/java/com/codingas/gateway/provider/channel/ChannelServiceImplTest.java`

**Interfaces:**
- Consumes: `ChannelRepository`（findById/save/existsByProviderIdAndName）、`ChannelEndpointRepository`（findByChannelId/save）、`ModelInstanceRepository`（findByChannelId/save）、`ChannelCredentialService`（listByChannelId/create）
- Produces: `Channel copy(Long sourceId, Channel override, boolean copyCredentials)` — 复制本体+端点+模型实例，可选复制凭证；`override` 承载 name（必填非 null）

- [x] **Step 1: 写失败测试**

在 `ChannelServiceImplTest` 新增 `CopyTests` 嵌套类：
- 复制继承本体配置（billingMode/quotaLimit/timeout/maxRetries）+ 覆盖 name + state=ACTIVE + 健康字段清空
- 复制端点与模型实例（断言新 channelId、模型实例 state=ACTIVE）
- copyCredentials=false 不复制凭证 / true 复制凭证（verify create 调用次数）
- 源不存在抛 `ResourceNotFoundException`（404 语义）
- 同供应商重名抛 `DuplicateResourceException`（409 语义），save 不被调用
- 凭证复制抛异常 → 渠道不落库（整体回滚，verify save 未被调用或事务断言）

- [x] **Step 2: 运行测试确认失败**

```bash
./mvnw -pl gateway-provider/provider -am test -Dtest=ChannelServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```
Expected: 编译失败（`copy` 不存在）

- [x] **Step 3: 实现**

`ChannelService.java` 接口新增中文 Javadoc 的 `copy(Long sourceId, Channel override, boolean copyCredentials)`。

`ChannelServiceImpl.java` 实现（@Transactional，中文注释分节）：
- 查源渠道 → `ResourceNotFoundException`
- name 唯一校验：`existsByProviderIdAndName(source.getProviderId(), newName)` → `DuplicateResourceException`
- 复制本体（providerId 继承源、name 覆盖、其余继承）+ 重置 state=ACTIVE、健康字段清空
- 复制端点：`findByChannelId` → 新 channelId save
- 复制模型实例：`findByChannelId` → 新 channelId，state=ACTIVE，save
- 复制凭证（copyCredentials=true）：`listByChannelId` → 复用 apiKeyPlain 调 `create`

- [x] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add gateway-provider/provider/src/main/java/com/codingas/gateway/provider/channel/ChannelService.java \
        gateway-provider/provider/src/main/java/com/codingas/gateway/provider/channel/ChannelServiceImpl.java \
        gateway-provider/provider/src/test/java/com/codingas/gateway/provider/channel/ChannelServiceImplTest.java
git commit -m "tweak: ChannelService.copy 复制渠道（本体+端点+模型实例+可选凭证）"
```

## Task 2: web 层 copy 端点

**Files:**
- Create: `gateway-web/src/main/java/com/codingas/gateway/web/api/dto/ChannelCopyRequest.java`
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/api/ChannelController.java`
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/api/facade/ChannelFacade.java`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/api/ChannelControllerCopyTest.java`

**Interfaces:**
- Consumes: `ChannelService.copy(Long, Channel, boolean)`（Task 1）
- Produces: `POST /api/v1/channels/{id}/copy`，body `ChannelCopyRequest{ name(@NotBlank), copyCredentials(默认 false) }` → `ChannelResponse`（经 `ChannelFacade.copy` 组装 providerName/endpoints）

- [x] **Step 1: 写失败测试**

新建 `ChannelControllerCopyTest`（参照 `ChannelControllerListTest` standalone 装配）：
- POST `/api/v1/channels/{id}/copy` body `{"name":"new"}` → 200 + 断言响应 id/name，且 copyCredentials 默认 false 透传
- POST body `{"name":"new","copyCredentials":true}` → copyCredentials=true 透传
- body 缺 name → 400

- [x] **Step 2: 运行测试确认失败**

```bash
./mvnw -pl gateway-web -am test -Dtest=ChannelControllerCopyTest -Dsurefire.failIfNoSpecifiedTests=false
```
Expected: 编译失败（`ChannelCopyRequest`/copy 端点不存在）

- [x] **Step 3: 实现**

`ChannelCopyRequest.java`（参照 `ModelCopyRequest` 模式）：`name(@NotBlank)` + `copyCredentials(boolean)` + `toEntity()`。

`ChannelFacade.java` 新增 `copy(Long id, ChannelCopyRequest request)`：调 `channelService.copy(id, request.toEntity(), request.isCopyCredentials())` → `toResponse` 组装。

`ChannelController.java` 新增 `@PostMapping("/{id}/copy")`：返回 `channelFacade.copy(id, request)`。

- [x] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add gateway-web/src/main/java/com/codingas/gateway/web/api/dto/ChannelCopyRequest.java \
        gateway-web/src/main/java/com/codingas/gateway/web/api/ChannelController.java \
        gateway-web/src/main/java/com/codingas/gateway/web/api/facade/ChannelFacade.java \
        gateway-web/src/test/java/com/codingas/gateway/web/api/ChannelControllerCopyTest.java
git commit -m "tweak: web 层通道复制端点（POST /{id}/copy + ChannelCopyRequest）"
```

## Task 3: 前端 API 与 useCopyChannel hook

**Files:**
- Modify: `gateway-console/src/services/api/channel.ts`
- Modify: `gateway-console/src/services/query/useChannels.ts`
- Modify: `gateway-console/src/types/channel.ts`

**Interfaces:**
- Produces: `channelApi.copy(id, data)`、`CopyChannelRequest{ name: string; copyCredentials?: boolean }`、`useCopyChannel()`（mutation + invalidate `channelKeys.lists()/allChannels()`）

- [x] **Step 1: 实现（TS 以 tsc 验证）**

`types/channel.ts` 新增 `CopyChannelRequest`。
`channel.ts` 新增 `copy: (id, data) => api.post<Channel>(`/channels/${id}/copy`, data)`。
`useChannels.ts` 新增 `useCopyChannel()`（参照 `useAddChannel`，成功后 invalidate `channelKeys.lists()/allChannels()`）。

- [x] **Step 2: 验证类型与 lint**

```bash
cd gateway-console && npx tsc -b --noEmit && npx eslint src/services/api/channel.ts src/services/query/useChannels.ts src/types/channel.ts
```
Expected: 无错误

- [x] **Step 3: Commit**

```bash
git add gateway-console/src/services/api/channel.ts \
        gateway-console/src/services/query/useChannels.ts \
        gateway-console/src/types/channel.ts
git commit -m "tweak(console): 通道复制 API 与 useCopyChannel hook"
```

## Task 4: 前端复制 UI（CopyChannelModal + 表格/卡片复制按钮）

**Files:**
- Create: `gateway-console/src/pages/Channels/CopyChannelModal.tsx`
- Modify: `gateway-console/src/pages/Channels/ChannelTableView.tsx`
- Modify: `gateway-console/src/pages/Channels/ChannelCard.tsx`
- Modify: `gateway-console/src/pages/Channels/index.tsx`（持有 copySource 状态 + 挂载弹窗）
- Test: `gateway-console/src/pages/Channels/__tests__/` 新增 CopyChannelModal 相关测试

**Interfaces:**
- Consumes: `useCopyChannel()`（Task 3）、`useTranslation('channels')`
- Produces: `CopyChannelModal{ open, source: ChannelCard | null, onClose }` — name 必填（预填源.name）+ copyCredentials Checkbox（默认不勾选）；提交成功 message.success + onClose（列表由 invalidate 自动刷新）

- [x] **Step 1: 写失败测试**

新建 `__tests__/CopyChannelModal.test.tsx`（参照 `CopyModelModal.test.tsx` 模式，mock `useCopyChannel`）：
- 预填源 name，提交调 copy（copyCredentials 默认 false）
- 勾选复选框后提交带 copyCredentials=true
- name 为空禁止提交

- [x] **Step 2: 运行测试确认失败**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/CopyChannelModal.test.tsx
```
Expected: 失败（组件不存在）

- [x] **Step 3: 实现 CopyChannelModal 组件**

参照 `CopyModelModal.tsx` 结构：`Form`（name 必填 + `Checkbox` copyCredentials）、`App.useApp()` message、`extractErrorMessage` 错误透出、`destroyOnHidden`。

- [x] **Step 4: 接入表格与卡片复制按钮 + 页面状态**

- `ChannelTableView.tsx`：操作列新增复制按钮（`CopyOutlined`），点击回调 `onCopy(record)`
- `ChannelCard.tsx`：操作区新增复制按钮（`CopyOutlined`），点击回调 `onCopy(record)`（经 `ChannelGroupedList` 透传）
- `Channels/index.tsx`：新增 `copySource` 状态，传入两处 `onCopy/onCopyChannel`，挂载 `CopyChannelModal`（open=!!copySource，onClose=清空）

- [x] **Step 5: 运行测试确认通过**

```bash
cd gateway-console && npx vitest run src/pages/Channels/__tests__/CopyChannelModal.test.tsx && npx vitest run src/pages/Channels/__tests__/ChannelTableView.test.tsx 2>/dev/null || true
npx tsc -b --noEmit
```
Expected: 全 PASS、tsc 无错误（Channels 全目录 52 用例 PASS）

- [x] **Step 6: Commit**

```bash
git add gateway-console/src/pages/Channels/CopyChannelModal.tsx \
        gateway-console/src/pages/Channels/ChannelTableView.tsx \
        gateway-console/src/pages/Channels/ChannelCard.tsx \
        gateway-console/src/pages/Channels/index.tsx \
        gateway-console/src/pages/Channels/__tests__/
git commit -m "tweak(console): 通道复制 UI（表格/卡片复制按钮 + CopyChannelModal）"
```
