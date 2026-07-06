# Comet Design Handoff

- Change: key-application-binding
- Phase: design
- Mode: compact
- Context hash: 128d2c571665f826665b75f0ee69ade4ddee462bdd87b5707ddf505d9bc7cd9f

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/key-application-binding/proposal.md

- Source: openspec/changes/key-application-binding/proposal.md
- Lines: 1-34
- SHA256: e4e53ea1d52cc17b174665d8202e592d5de070d419d0c18c05a1b87aabe7f92e

```md
## Why

`application` spec 已规定「UserApiKey 增加 `applicationId` 字段作为权限锚点」，`application-access-control` spec 已规定「`applicationId` 为 null 时 `PermissionRouter` 返回空集」。但管理 API 从未暴露 `applicationId`：`UserApiKeyCreateRequest` 无该字段、`UserApiKeyServiceImpl.create()` 不调用 `setApplicationId`、5 个响应 DTO 均不含该字段。后果——通过管理 API 创建的 UserApiKey `applicationId` 永远为 null，调用网关时 `PermissionRouter` 返回空渠道集，Key 能创建但不可路由。这是 spec 与实现的断层，非新设计。

同时前端停留在已归档的「团队继承」模型（`inherited-permission-model`），`UserApiKeyModal` 用 Alert 声明「Key 权限由用户所属团队决定」，与现行 Application 锚点模型直接冲突；`types/userApiKey.ts` 注释残留「挂载到应用」但字段缺失，代码自相矛盾。另：管理员重置密码前端有按钮调用 `POST /users/{id}/reset-password`，后端无此端点，点击即 404。

## What Changes

- **BREAKING** `POST /api/v1/user-api-keys` 请求体新增 `applicationId`（`@NotNull` 必填），`UserApiKeyServiceImpl.create()` 落库 `setApplicationId`
- `PUT /api/v1/user-api-keys/{id}` 支持变更 `applicationId`（补绑/转移应用，存量 null Key 经此修复）
- UserApiKey 响应 DTO（`UserApiKeyResponse`/`UserApiKeyDetailResponse`/`UserApiKeyCreateResponse`）暴露 `applicationId`
- 新增 `GET /api/v1/applications/{id}/api-keys` 查询应用下所有 UserApiKey
- 新增 `POST /api/v1/users/{id}/reset-password` 管理员重置密码端点（修复前端 404）
- **BREAKING** `DELETE /api/v1/applications/{id}` 前置校验：若有 UserApiKey 引用该应用则拒绝（避免 `applicationId` 悬空）
- 前端：创建 Key 表单加 Application 选择（必填）、列表加「所属应用」列、加按应用筛选、`UserApiKeyModal` 删除「团队继承」Alert 并加 Application 选择、Applications 行加「查看 Key」入口、移除 `rotate` 死代码封装、`types/userApiKey.ts` 注释与字段对齐

## Capabilities

### New Capabilities

- `user-apikey-management`: UserApiKey 管理 API 契约——创建（`applicationId` 必填）、更新（补绑/转移 `applicationId`）、响应字段暴露、按应用查询；不覆盖轮换/启用禁用/有效期（留后续）
- `user-password-management`: 用户密码管理——管理员重置他人密码端点

### Modified Capabilities

- `application`: 删除 Application 前置校验有 UserApiKey 引用则拒绝；新增按应用查询 UserApiKey 端点 `GET /api/v1/applications/{id}/api-keys`

## Impact

- **后端**：`UserApiKeyCreateRequest`/`UserApiKeyUpdateRequest`/`UserApiKeyResponse`/`UserApiKeyDetailResponse`/`UserApiKeyCreateResponse` 5 个 DTO；`UserApiKeyServiceImpl`（create/update/toResponse/toDetailResponse）；`UserApiKeyGateway`+`Impl`（新增 `findByApplicationId`）；`ApplicationController`+`ApplicationServiceImpl`（删除校验 + 按应用查 Key 端点）；`UserController`+`UserService(Impl)`（重置密码端点）
- **前端**：`types/userApiKey.ts`、`services/api/userApiKey.ts`、`pages/ApiKeys/DownstreamKeysTable.tsx`、`pages/Users/UserApiKeyModal.tsx`、`pages/Applications/index.tsx`、`services/api/user.ts`
- **路由层**：不改（`PermissionRouter` 行为已符合 `application-access-control` spec），补集成测试验证带 `applicationId` 的 Key 路由返回非空渠道集（回归本次核心问题）
- **数据库**：无 schema 变更（`user_api_keys.application_id` 字段已存在）
- **兼容性**：CreateRequest 新增必填字段为破坏性变更，前端是唯一调用方，已同步改造；DELETE 新增前置校验为破坏性变更，管理员需先转移/删除 Key 才能删应用
```

## openspec/changes/key-application-binding/design.md

- Source: openspec/changes/key-application-binding/design.md
- Lines: 1-81
- SHA256: 7f25c88153d158691585833b0b7a969858b484cc7409b21055d592ffec7bdf04

[TRUNCATED]

```md
## Context

`application` spec 与 `application-access-control` spec 已定义「Application 为权限锚点」「UserApiKey.applicationId 作为权限锚点」「applicationId 为 null 时 PermissionRouter 返回空集」——spec 层模型正确。问题在实现层断层：管理 API 从未暴露 `applicationId`，`UserApiKeyServiceImpl.create()` 不落库该字段，5 个 DTO 均不含它。结果是通过管理 API 创建的 Key `applicationId` 永远为 null，路由时返回空渠道集，Key 能创建但不可用。

前端则停留在已归档的 `inherited-permission-model`（团队继承）模型，`UserApiKeyModal` 的 Alert 与后端现行模型冲突。管理员重置密码前端调用 `POST /users/{id}/reset-password` 但后端无此端点。

本次变更不引入新架构，而是补齐 spec 已规定但实现缺失的管理面 API，并消除前后端模型分歧。

## Goals / Non-Goals

**Goals:**
- 管理 API 完整暴露与操作 `applicationId`：创建必填、更新补绑/转移、响应字段、按应用查询
- 修复管理员重置密码 404
- 前端对齐 Application 锚点模型，删除「团队继承」Alert
- 删除 Application 时防止 `applicationId` 悬空

**Non-Goals:**
- 不实现 UserApiKey rotate 端点（仅清理前端死代码封装）
- 不做 Key 启用/禁用状态、有效期
- 不做用户搜索/筛选/分页后端化、Application 分页/状态切换、批量操作、独立详情页
- 不自动迁移存量 null Key（仅提供补绑入口）
- 不改路由层（PermissionRouter 行为已符合 spec）

## Decisions

### D1: applicationId 创建时 @NotNull 必填
- **选择**：`UserApiKeyCreateRequest.applicationId` 加 `@NotNull`，create 落库 `setApplicationId`
- **理由**：路由层 `PermissionRouter` 对 null 返回空集（spec 规定），null 的 Key 不可路由。必填保证新建 Key 即刻可用
- **否决**：「可选 + 默认应用」引入默认应用概念且无路由放行语义；「可选 + null 放行全部」违反零信任与 spec

### D2: 补绑/转移复用 PUT /user-api-keys/{id}
- **选择**：`UserApiKeyUpdateRequest` 加可选 `applicationId` 字段，update 时若传入则变更
- **理由**：复用现有 update 端点，最小改动；补绑与改名同属 Key 编辑
- **否决**：专用 `/transfer` 端点过度设计

### D3: 按应用查询放 ApplicationController
- **选择**：`GET /api/v1/applications/{id}/api-keys` 作为 Application 子资源
- **理由**：子资源语义清晰（应用的 Key 集合），与现有 `/applications/{id}/channels` 一致
- **否决**：`UserApiKeyController` 加 `applicationId` 查询参数不够 RESTful

### D4: 重置密码后端随机生成 + 一次性返回明文
- **选择**：`POST /users/{id}/reset-password` 生成随机密码，更新哈希，一次性返回明文
- **理由**：与 `UserApiKeyCreate` 明文一次性返回模式一致；管理员不应知晓用户密码
- **否决**：管理员设定密码（安全差）；保留旧密码（不构成"重置"）

### D5: 删除 Application 前置校验有 Key 引用则拒绝
- **选择**：`ApplicationServiceImpl.delete` 删除前查 `applicationId = id` 的非删除 Key，有则抛 Conflict
- **理由**：避免 `applicationId` 悬空导致 Key 不可路由；强制管理员显式处理 Key
- **否决**：级联置 null（引入悬空坏数据）；级联删除 Key（危险，丢失凭证）

### D6: 存量 null Key 不自动迁移
- **选择**：提供补绑入口（D2），存量由管理员手动补绑
- **理由**：自动绑默认应用有赋错权限风险；开发期存量少
- **否决**：自动迁移到默认应用

### D7: 前端删除「团队继承」Alert，创建表单加 Application 必选
- **选择**：`UserApiKeyModal` 删 Alert、加 Application Select；`DownstreamKeysTable` 创建表单同步；列表加「所属应用」列
- **理由**：对齐后端 Application 锚点模型，消除代码自相矛盾

### D8: 移除前端 rotate 死代码封装
- **选择**：`services/api/userApiKey.ts` 移除 `rotate` 方法
- **理由**：后端无该端点，封装误导后续调用

## Risks / Trade-offs

- [CreateRequest 加必填字段是破坏性变更] → 前端是唯一调用方，同步改造；无 CLI/外部调用方依赖旧契约
- [DELETE 加前置校验是破坏性变更] → 管理员需先转移/删除 Key；前端删除按钮加冲突提示
- [存量 null Key 仍不可路由] → 补绑入口上线后由管理员手动处理，release notes 说明
- [重置密码返回明文] → 一次性返回不持久化，HTTPS 传输，与 UserApiKeyCreate 一致

## Migration Plan

1. 后端先行：DTO 加字段 → Service 落库/补绑 → Gateway 加 `findByApplicationId` → Controller 加端点 → 单测 + 集成测试（含「带 applicationId 的 Key 路由返回非空渠道集」回归）
2. 前端跟进：类型加字段 → 表单加 Application 选择 → 列表加列 → 删 Alert → 移除 rotate → Applications 加「查看 Key」入口
3. 存量 null Key：管理员通过补绑入口手动处理
4. 回滚：整体 revert change 分支，无 schema 变更，回滚无副作用

## Open Questions

- 重置密码随机密码长度/字符集：倾向 16 位字母数字混合，实现时定
```

Full source: openspec/changes/key-application-binding/design.md

## openspec/changes/key-application-binding/tasks.md

- Source: openspec/changes/key-application-binding/tasks.md
- Lines: 1-47
- SHA256: 7422ee18609b006b1d512cfd9ae3a45fb6feae237544ca8968bd90999ce00e91

```md
## 1. 后端 DTO 与 Service（UserApiKey 关联打通）

- [ ] 1.1 `UserApiKeyCreateRequest` 新增 `@NotNull Long applicationId`
- [ ] 1.2 `UserApiKeyUpdateRequest` 新增可选 `Long applicationId`
- [ ] 1.3 `UserApiKeyResponse`/`UserApiKeyDetailResponse`/`UserApiKeyCreateResponse` 新增 `applicationId` 字段
- [ ] 1.4 `UserApiKeyServiceImpl.create()` 落库 `setApplicationId` 并校验 Application 存在
- [ ] 1.5 `UserApiKeyServiceImpl.update()` 支持 `applicationId` 变更（补绑/转移）并校验 Application 存在
- [ ] 1.6 `UserApiKeyServiceImpl.toResponse/toDetailResponse` 映射 `applicationId`
- [ ] 1.7 单测 `UserApiKeyServiceImplTest`：创建必填落库、补绑转移、响应含 applicationId、引用不存在 Application 被拒

## 2. 后端 Gateway（按应用查询）

- [ ] 2.1 `UserApiKeyGateway` 接口新增 `findByApplicationId(Long)`
- [ ] 2.2 `UserApiKeyGatewayImpl` 实现 `findByApplicationId`，确认 `toEntity/toDataObject` 读写 `applicationId`
- [ ] 2.3 单测覆盖 `findByApplicationId`

## 3. 后端 Controller 与端点

- [ ] 3.1 `ApplicationController` 新增 `GET /api/v1/applications/{id}/api-keys`
- [ ] 3.2 `ApplicationServiceImpl.delete()` 新增 UserApiKey 引用前置校验，有则抛 Conflict
- [ ] 3.3 `UserController` 新增 `POST /api/v1/users/{id}/reset-password` + `UserService.resetPassword()`（随机密码 + 一次性返回明文 + 禁止内建用户）
- [ ] 3.4 集成测试：带 `applicationId` 的 Key 调用 `ChatDispatchService` 路由返回非空渠道集（回归核心问题）
- [ ] 3.5 集成测试：删除有 Key 引用的 Application 返回 4xx Conflict
- [ ] 3.6 集成测试：重置密码端点成功 + 内建用户拒绝

## 4. 前端类型与 API 层

- [ ] 4.1 `types/userApiKey.ts`：`UserApiKey`/`CreateUserApiKeyRequest`/`UpdateUserApiKeyRequest` 加 `applicationId`，修正注释与字段一致
- [ ] 4.2 `services/api/userApiKey.ts`：移除 `rotate` 死代码封装，新增 `listByApplication(applicationId)`
- [ ] 4.3 `services/api/user.ts`：确认 `resetPassword` 封装（后端已补端点）

## 5. 前端 UserApiKey 管理页

- [ ] 5.1 `DownstreamKeysTable` 创建表单加 Application Select（必填），列表加「所属应用」列，加按应用筛选 Select
- [ ] 5.2 `UserApiKeyModal` 删除「团队继承」Alert，创建表单加 Application Select，支持编辑补绑 applicationId
- [ ] 5.3 组件测试：创建表单 Application 必填校验、列表 Application 列渲染、补绑交互

## 6. 前端 Application 管理页

- [ ] 6.1 `Applications/index.tsx` 行操作加「查看 Key」入口（跳转 `/keys?applicationId=<id>` 触发筛选）
- [ ] 6.2 删除 Application 冲突提示（有 Key 引用时显示后端 Conflict 信息）

## 7. 验证与收尾

- [ ] 7.1 后端全量测试通过（`./mvnw test`）
- [ ] 7.2 前端构建与测试通过（`npm run build` + `npm test`）
- [ ] 7.3 端到端手验：创建 Key 必选 App → 用该 Key 调网关 → 路由成功
```

## openspec/changes/key-application-binding/specs/application/spec.md

- Source: openspec/changes/key-application-binding/specs/application/spec.md
- Lines: 1-39
- SHA256: 01c33f5ce0b4dee4662ec525e42eb49ef7a03cba5d0629b4dcdbd12849f01a65

```md
## ADDED Requirements

### Requirement: 删除 Application 前置 UserApiKey 引用校验

系统 SHALL 在删除 Application 前校验是否存在引用该应用的 UserApiKey，存在则拒绝删除，避免 `applicationId` 悬空导致 Key 不可路由。

**规则**:
- `DELETE /api/v1/applications/{id}` 执行前 MUST 查询 `applicationId = id` 的非删除 UserApiKey
- 存在引用时返回 4xx（Conflict），提示先转移或删除关联 Key
- 无引用时正常删除并级联清理 `ApplicationChannel` 关联

#### Scenario: 有 Key 引用时拒绝删除

- **WHEN** 管理员删除某 Application，且存在 `applicationId` 指向该 Application 的非删除 UserApiKey
- **THEN** 系统 SHALL 拒绝删除（4xx Conflict）
- **THEN** 系统 SHALL 提示先转移或删除关联 Key

#### Scenario: 无 Key 引用时正常删除

- **WHEN** 管理员删除某 Application，且无 UserApiKey 引用
- **THEN** 系统 SHALL 删除 Application 并级联清理 `ApplicationChannel`
- **THEN** 系统 SHALL 返回 HTTP 204

### Requirement: 按应用查询 UserApiKey

系统 SHALL 提供按 Application 查询其下 UserApiKey 的端点，支持应用级 Key 治理。

**API**: `GET /api/v1/applications/{id}/api-keys`
- Response: `List<UserApiKeyResponse>`（每项含 `applicationId`）

#### Scenario: 查询应用下所有 Key

- **WHEN** 管理员调用 `GET /api/v1/applications/{id}/api-keys`
- **THEN** 系统 SHALL 返回 `applicationId = id` 的所有非删除 UserApiKey

#### Scenario: 空应用返回空列表

- **WHEN** 应用下无 UserApiKey
- **THEN** 系统 SHALL 返回空列表
```

## openspec/changes/key-application-binding/specs/user-apikey-management/spec.md

- Source: openspec/changes/key-application-binding/specs/user-apikey-management/spec.md
- Lines: 1-72
- SHA256: 87e04e56e0bb68dc7a339f5a8784e9806cb5f6f89a7654c16a399997c5828a01

```md
## ADDED Requirements

### Requirement: 创建 UserApiKey 必填 applicationId

系统 SHALL 在创建 UserApiKey 时要求 `applicationId` 必填，并将其作为权限锚点落库，使新建 Key 即刻具备可路由的渠道可见性。

**API**: `POST /api/v1/user-api-keys`
- Request Body: `{ userId: Long (NotNull), applicationId: Long (NotNull), name: String (NotBlank) }`
- Response: `UserApiKeyCreateResponse { id, keyPrefix, apiKeyPlain }`（HTTP 201）

**规则**:
- `applicationId` MUST 引用已存在的 Application，否则拒绝
- `UserApiKeyServiceImpl.create()` MUST 调用 `setApplicationId(request.applicationId())` 落库
- 明文 Key 仅在创建响应中一次性返回

#### Scenario: 创建 Key 必填 applicationId 落库可路由

- **WHEN** 管理员调用 `POST /api/v1/user-api-keys` 传入 `{userId, applicationId, name}`
- **THEN** 系统 SHALL 创建 UserApiKey，`applicationId` 落库为传入值
- **THEN** 系统 SHALL 返回 HTTP 201 与 `{id, keyPrefix, apiKeyPlain}`
- **THEN** 该 Key 调用网关时 `PermissionRouter` SHALL 返回该 Application 授权的非空渠道集

#### Scenario: 创建 Key 未传 applicationId 被拒绝

- **WHEN** 管理员调用 `POST /api/v1/user-api-keys` 未传 `applicationId`
- **THEN** 系统 SHALL 返回校验错误（4xx），不创建 Key

#### Scenario: 创建 Key 引用不存在的 Application 被拒绝

- **WHEN** 管理员传入的 `applicationId` 不引用任何已存在 Application
- **THEN** 系统 SHALL 返回校验错误（4xx），不创建 Key

### Requirement: 更新 UserApiKey 支持补绑/转移 applicationId

系统 SHALL 支持通过更新 API 变更 UserApiKey 的 `applicationId`，用于补绑存量 `null` Key 或转移应用归属。

**API**: `PUT /api/v1/user-api-keys/{id}`
- Request Body: `{ applicationId: Long (可选), name: String (可选) }`
- Response: `UserApiKeyResponse`

**规则**:
- `applicationId` 可选；传入时 MUST 引用已存在的 Application
- 不传 `applicationId` 时保持原值不变

#### Scenario: 补绑存量 null Key 的 applicationId

- **WHEN** 管理员调用 `PUT /api/v1/user-api-keys/{id}` 传入 `applicationId`（原值为 null）
- **THEN** 系统 SHALL 更新该 Key 的 `applicationId` 为新值
- **THEN** 系统 SHALL 返回含 `applicationId` 的 `UserApiKeyResponse`
- **THEN** 该 Key 此后调用网关 SHALL 可路由

#### Scenario: 转移 Key 到另一应用

- **WHEN** 管理员调用 `PUT /api/v1/user-api-keys/{id}` 传入新 `applicationId`
- **THEN** 系统 SHALL 更新该 Key 的 `applicationId`
- **THEN** 该 Key 的权限边界 SHALL 由新 Application 的 `ApplicationChannel` 决定

### Requirement: UserApiKey 响应暴露 applicationId

系统 SHALL 在所有 UserApiKey 响应 DTO 中暴露 `applicationId` 字段，使前端能展示 Key 的应用归属。

**DTO**: `UserApiKeyResponse`、`UserApiKeyDetailResponse` 均含 `applicationId: Long`

#### Scenario: 查询 Key 列表返回 applicationId

- **WHEN** 管理员调用 `GET /api/v1/user-api-keys`
- **THEN** 每条响应 SHALL 含 `applicationId` 字段

#### Scenario: 查询 Key 详情返回 applicationId

- **WHEN** 管理员调用 `GET /api/v1/user-api-keys/{id}/detail`
- **THEN** 响应 SHALL 含 `applicationId` 字段
```

## openspec/changes/key-application-binding/specs/user-password-management/spec.md

- Source: openspec/changes/key-application-binding/specs/user-password-management/spec.md
- Lines: 1-31
- SHA256: b885ca1afc6c587b8ccfb2d35733bfafc4ad1de5e8041c57595d7a799e7a939d

```md
## ADDED Requirements

### Requirement: 管理员重置用户密码

系统 SHALL 提供管理员重置他人密码端点，生成新随机密码并一次性返回明文，修复前端 `POST /api/v1/users/{id}/reset-password` 404 bug。

**API**: `POST /api/v1/users/{id}/reset-password`
- Response: `{ newPassword: String }`（HTTP 200）

**规则**:
- 调用方 MUST 具备管理员权限
- 系统 SHALL 生成随机密码，更新目标用户的密码哈希
- 系统 SHALL 一次性返回新密码明文，不持久化明文
- 禁止重置内建用户（`builtin=true`）密码，与现有内建用户保护策略一致

#### Scenario: 管理员重置密码成功

- **WHEN** 管理员调用 `POST /api/v1/users/{id}/reset-password`
- **THEN** 系统 SHALL 生成新随机密码并更新用户密码哈希
- **THEN** 系统 SHALL 一次性返回新密码明文（HTTP 200）
- **THEN** 用户使用新密码 SHALL 能登录成功

#### Scenario: 重置内建用户密码被拒绝

- **WHEN** 管理员重置内建用户（`builtin=true`）密码
- **THEN** 系统 SHALL 拒绝（4xx），不修改密码

#### Scenario: 重置不存在用户被拒绝

- **WHEN** 管理员重置不存在的用户 ID
- **THEN** 系统 SHALL 返回 404
```

