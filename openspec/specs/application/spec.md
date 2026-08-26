# application Specification

## Purpose
TBD - created by archiving change resilience-architecture. Update Purpose after archive.
## Requirements
### Requirement: Application 根实体实体

系统 SHALL 提供 `Application` 根实体实体作为「权限 + 行为」双根实体，承载 N 把 Key 的应用归属、渠道可见性、应用级超时、失败处理策略，并预留配额/看板字段。

**实体字段**（保留 timeout，新增 failureStrategy）:
- `code` — 应用编码，全局唯一
- `name` — 应用名称
- `description` — 应用描述
- `state` — 应用生命周期状态（`ApplicationState`）
- `timeout` — 请求超时秒数（0 表示用渠道默认）
- `failureStrategy` — 失败处理策略枚举（`FAIL_FAST`/`FAIL_OVER`/`FAIL_RETRY`，默认 `FAIL_RETRY`，详见 application-failure-strategy capability）
- `quotaBudgetId` — 配额预算 ID（预留）
- `dashboardId` — 看板 ID（预留）
- 审计字段 `createdBy/createdAt/updatedBy/updatedAt` 继承自 `BaseEntity`

**API**:
- `POST /api/v1/applications` — 创建应用（请求体 `code/name/description/timeout/failureStrategy`，返回 `ApplicationResponse`，HTTP 201）
- `PUT /api/v1/applications/{id}` — 更新应用（含 failureStrategy）
- `GET /api/v1/applications/{id}` — 查询应用详情（含 failureStrategy）
- `GET /api/v1/applications` — 查询全部应用列表
- `DELETE /api/v1/applications/{id}` — 删除应用（HTTP 204）

#### Scenario: 创建应用含失败处理策略

- **WHEN** 管理员调用 `POST /api/v1/applications` 传入 `code/name/description/timeout/failureStrategy`
- **THEN** 系统 SHALL 创建 `Application` 记录，`code` 全局唯一
- **THEN** 系统 SHALL 返回 HTTP 201 与创建后的 `ApplicationResponse`

#### Scenario: 未指定策略默认失败重试

- **WHEN** 创建应用未传 `failureStrategy`
- **THEN** 系统 SHALL 将 `failureStrategy` 设为默认值 `FAIL_RETRY`

### Requirement: Application 为权限锚点而非人/团队

Application SHALL 取代 Team 成为权限锚点。权限链 MUST 重写为 `UserApiKey → Application → ApplicationChannel → Channel`，不再经过 `User → Team → TeamChannel`。

**规则**:
- `UserApiKey` 增加 `applicationId` 字段，作为权限锚点
- 多把 Key 共用一个 Application（N Key → 1 Application）
- Application 不保留成员管理概念（谁持 Key 谁能用）

#### Scenario: API Key 归属应用

- **WHEN** 一个 API Key 被创建或迁移并绑定到某 Application
- **THEN** 该 Key 的权限边界由 `Application → ApplicationChannel` 决定
- **THEN** 不再依赖 `User → Team → TeamChannel` 链路

### Requirement: Application 预留配额与看板字段

Application SHALL 预留 `quotaBudgetId` 与 `dashboardId` 字段供后续 `quota`/`audit` 域填充，本 change 不实做其计费与呈现逻辑。

#### Scenario: 预留字段留空

- **WHEN** 本 change 创建或更新 Application
- **THEN** `quotaBudgetId` 与 `dashboardId` SHALL 保持为 null（预留未启用）
- **THEN** 系统 SHALL NOT 对这两个字段执行任何业务逻辑

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

