# application Specification

## Purpose
TBD - created by archiving change resilience-architecture. Update Purpose after archive.
## Requirements
### Requirement: Application 聚合根实体

系统 SHALL 提供 `Application` 聚合根实体作为「权限 + 行为」双聚合根，承载 N 把 Key 的应用归属、渠道可见性、应用级超时，并预留配额/看板字段。

**实体字段**（移除 `resilienceProfileId`，新增 `timeout`）:
- `code` — 应用编码，全局唯一
- `name` — 应用名称
- `description` — 应用描述
- `state` — 应用生命周期状态（`ApplicationState`，控制是否可路由）
- `timeout` — 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout，ResilienceProfile 实体退场）
- `quotaBudgetId` — 配额预算 ID（预留，留 quota 域填充）
- `dashboardId` — 看板 ID（预留，留 audit 域填充）
- 审计字段 `createdBy/createdAt/updatedBy/updatedAt` 继承自 `BaseEntity`

**移除字段**:
- `resilienceProfileId` — ResilienceProfile 实体退场，不再关联画像

**API**:
- `POST /api/v1/applications` — 创建应用（请求体 `code/name/description/timeout`，返回 `ApplicationResponse`，HTTP 201）
- `PUT /api/v1/applications/{id}` — 更新应用
- `GET /api/v1/applications/{id}` — 查询应用详情
- `GET /api/v1/applications` — 查询全部应用列表
- `DELETE /api/v1/applications/{id}` — 删除应用（级联清理渠道授权关联，HTTP 204）

#### Scenario: 创建应用

- **WHEN** 管理员调用 `POST /api/v1/applications` 传入合法 `code/name/description/timeout`
- **THEN** 系统 SHALL 创建 `Application` 记录，`code` 全局唯一
- **THEN** 系统 SHALL 返回 HTTP 201 与创建后的 `ApplicationResponse`

#### Scenario: 删除应用级联清理渠道授权

- **WHEN** 管理员调用 `DELETE /api/v1/applications/{id}` 删除应用
- **THEN** 系统 SHALL 级联清理该应用的 `ApplicationChannel` 关联
- **THEN** 系统 SHALL 返回 HTTP 204

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

