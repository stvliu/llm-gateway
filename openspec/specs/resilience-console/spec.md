# resilience-console Specification

## Purpose
TBD - created by archiving change resilience-architecture. Update Purpose after archive.
## Requirements
### Requirement: 容灾总览页

控制台 SHALL 提供容灾总览页（只读），展示故障域拓扑、实时转移事件流与耗尽告警。

**API**:
- `GET /api/v1/resilience/events` — 转移事件流查询（分页 + `since`/`applicationId`/`clusterId` 过滤，按 `occurredAt` 倒序）
  - 参数：`since`（ISO-8601 Instant，可选）、`applicationId`（可选）、`clusterId`（可选）、`limit`（默认 100，上限 500）
- `GET /api/v1/resilience/events/exhausted` — 耗尽告警查询（`exhausted=true` 近期事件，按 `occurredAt` 倒序）
  - 参数：`since`（可选，不传时由 Service 层补默认窗口最近 1 小时）、`limit`（默认 50，上限 500）

**规则**:
- 前端总览页 10s 轮询渲染转移事件流与耗尽告警
- `clusterId` 过滤基于冗余 `fromClusterId`/`toClusterId` 字段匹配（任一命中即返回）
- `FailoverEventResponse` 字段：`id`/`traceId`/`applicationId`/`fromChannelId`/`fromEndpointId`/`toChannelId`/`toEndpointId`/`fromClusterId`/`toClusterId`/`errorType`/`decision`/`exhausted`/`occurredAt`
- `errorType`/`decision` 以字符串返回（枚举名），前端按字符串展示

#### Scenario: 查询转移事件流

- **WHEN** 前端调用 `GET /api/v1/resilience/events?since=...&applicationId=...&clusterId=...&limit=100`
- **THEN** 系统 SHALL 返回按 `occurredAt` 倒序的转移事件列表
- **THEN** 系统 SHALL 按 `since`/`applicationId`/`clusterId` 可选参数过滤

#### Scenario: 查询耗尽告警

- **WHEN** 前端调用 `GET /api/v1/resilience/events/exhausted?limit=50`
- **THEN** 系统 SHALL 返回 `exhausted=true` 的转移事件列表
- **THEN** 系统 SHALL 按 `occurredAt` 倒序返回

#### Scenario: limit 超限截断

- **WHEN** 请求 `limit` 超过上限 500
- **THEN** 系统 SHALL 截断为 500
- **WHEN** 请求 `limit` 为 null 或 <= 0
- **THEN** 系统 SHALL 使用默认值（事件流 100，耗尽告警 50）

### Requirement: 画像模板页

控制台 SHALL 提供画像模板管理页，支持模板 CRUD，专家字段折叠在「高级」，管理员克隆微调而非从零填。

**API**: 见 `resilience-profile` capability（`POST/PUT/GET /api/v1/resilience/profiles`）。

**规则**:
- 管理员面向两字段：容灾模式档位（`STANDARD`/`STRICT`/`AGGRESSIVE`）+ 降级兜底开关
- 其余专家字段（`enableSessionAffinity`/`sessionAffinityTtlMinutes`/`enablePinnedModel`/`pinnedModelId`/`timeout`/`degradationMaxDepth`）由档位自动推导，折叠在「高级」
- 不提供 delete

#### Scenario: 克隆画像模板微调

- **WHEN** 管理员在画像模板页选择一个预设档位克隆
- **THEN** 系统 SHALL 基于该档位推导专家字段生成新画像
- **THEN** 管理员 SHALL 能在「高级」中微调专家字段

### Requirement: 应用管理页容灾模式选择

控制台 SHALL 在应用管理页提供容灾模式档位选择与降级兜底开关。

**API**:
- `PUT /api/v1/applications/{id}/resilience` — 绑定/解绑容灾画像（见 `application` capability）
- 应用渠道授权 `PUT /api/v1/applications/{id}/channels`（见 `application-access-control` capability）

**规则**:
- 管理员选容灾模式档位时，系统按档位推导画像专家字段
- `resilienceProfileId` 为 null 时解绑，解析链回退全局 default 画像

#### Scenario: 为应用选择容灾模式

- **WHEN** 管理员在应用管理页为应用选择容灾模式档位
- **THEN** 系统 SHALL 绑定对应档位的容灾画像到应用
- **THEN** 应用请求 SHALL 按该画像的容灾策略执行

### Requirement: Channels 一键应急操作

控制台 SHALL 在 Channels 页提供一键手动熔断/恢复/紧切域应急操作，复用既有熔断器，无新状态机。

**API**:
- `POST /api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/force-open` — 一键熔断端点（强制熔断器 OPEN，立即切断流量）
- `POST /api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/force-close` — 一键恢复端点（强制熔断器 CLOSED 并重置窗口，立即恢复流量）
- `GET /api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/state` — 查询端点熔断器状态（返回 `CLOSED`/`OPEN`/`HALF_OPEN`）
- `PUT /api/v1/channels/{id}/cluster` — 紧切域（将 `channel.clusterId` 改为目标故障域 ID，用于跨域转移）

**规则**:
- 一键熔断/恢复委托 `ChannelEndpointCircuitBreakerManager`，复用既有熔断器实例
- 紧切域校验目标故障域存在；不校验目标域健康（运维决策）
- 应急操作返回 `CircuitBreakerStateResponse`（含状态名）

#### Scenario: 一键熔断端点

- **WHEN** 运维调用 `POST /api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/force-open`
- **THEN** 系统 SHALL 强制该端点熔断器进入 `OPEN`
- **THEN** 系统 SHALL 立即切断该端点流量
- **THEN** 系统 SHALL 返回状态名 `OPEN`

#### Scenario: 一键恢复端点

- **WHEN** 运维调用 `POST /api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/force-close`
- **THEN** 系统 SHALL 强制该端点熔断器回到 `CLOSED` 并重置窗口
- **THEN** 系统 SHALL 立即恢复该端点流量
- **THEN** 系统 SHALL 返回状态名 `CLOSED`

#### Scenario: 查询端点熔断器状态

- **WHEN** 运维调用 `GET /api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/state`
- **THEN** 系统 SHALL 返回当前熔断器状态名（`CLOSED`/`OPEN`/`HALF_OPEN`）

#### Scenario: 紧切域

- **WHEN** 运维调用 `PUT /api/v1/channels/{id}/cluster` 传入目标 `clusterId`
- **THEN** 系统 SHALL 校验目标故障域存在
- **THEN** 系统 SHALL 将 `channel.clusterId` 更新为目标故障域 ID
- **THEN** 系统 SHALL NOT 校验目标域健康（运维决策）
- **THEN** 系统 SHALL 返回 HTTP 204

