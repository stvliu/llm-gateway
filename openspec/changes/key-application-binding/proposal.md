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
