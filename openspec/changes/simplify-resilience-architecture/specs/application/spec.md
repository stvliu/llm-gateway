# Application Delta Spec

## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Application 承载容灾画像绑定
**Reason**: ResilienceProfile 实体退场（删 L2/PinnedModel/会话亲和后只剩 timeout，不配独立实体）。`Application.resilienceProfileId` 关联移除，timeout 直接挂 Application 字段。
**Migration**: `PUT /api/v1/applications/{id}/resilience` 端点移除。timeout 通过应用 CRUD 端点（创建/更新）直接配置。容灾画像解析链（Application→Global）退场，timeout 由 `Application.timeout` 直接提供。
