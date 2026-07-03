# Application Delta Spec

## MODIFIED Requirements

### Requirement: Application 聊合根实体

系统 SHALL 提供 `Application` 聚合根实体作为「权限 + 行为」双聚合根，承载 N 把 Key 的应用归属、渠道可见性、应用级超时，并挂载应用级容灾策略，预留配额/看板字段。

**实体字段**（保留 timeout，挂载容灾策略）:
- `code` — 应用编码，全局唯一
- `name` — 应用名称
- `description` — 应用描述
- `state` — 应用生命周期状态
- `timeout` — 请求超时秒数（0 表示用渠道默认）
- 容灾策略 — 应用级场景化容灾配置（挂载方式由 design 定，轻量不独立实体，详见 application-resilience-strategy capability）
- `quotaBudgetId` — 配额预算 ID（预留）
- `dashboardId` — 看板 ID（预留）
- 审计字段继承自 `BaseEntity`

**API**:
- `POST /api/v1/applications` — 创建应用（含策略配置）
- `PUT /api/v1/applications/{id}` — 更新应用（含策略配置）
- `GET /api/v1/applications/{id}` — 查询应用详情（含策略）
- `GET /api/v1/applications` — 查询全部应用列表
- `DELETE /api/v1/applications/{id}` — 删除应用（HTTP 204）
