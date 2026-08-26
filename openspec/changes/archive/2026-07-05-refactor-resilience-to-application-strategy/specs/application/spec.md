# Application Delta Spec

## MODIFIED Requirements

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
