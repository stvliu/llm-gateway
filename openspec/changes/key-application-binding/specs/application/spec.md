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
