# user-apikey-management Specification

## Purpose
TBD - created by archiving change key-application-binding. Update Purpose after archive.
## Requirements
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

