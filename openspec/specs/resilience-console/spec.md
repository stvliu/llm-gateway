# resilience-console Specification

## Purpose
TBD - created by archiving change resilience-architecture. Update Purpose after archive.
## Requirements
### Requirement: 容灾总览页

容灾总览页 SHALL 展示转移事件流、耗尽告警与端点熔断状态大盘，移除 Cluster 故障域拓扑、降级/会话亲和/PinnedModel/共因跳过相关展示。

**变更要点**:
- 删除：Cluster 故障域拓扑卡片（`Cluster` 实体与 `clusterId` 字段已删除）、`grouping.ts` 分组逻辑
- 删除：转移事件流的 `clusterId` 字段与「是否共因跳过」列（`commonCauseSkip` 已删除）
- 保留：转移事件流（`from/to` 渠道端点、`errorType`、`decision`、`exhausted`、`occurredAt`，10s 轮询）
- 保留：耗尽告警（候选全耗尽事件高亮）
- 新增：端点熔断状态大盘区块（各端点熔断器 CLOSED/OPEN/HALF_OPEN 状态 + 应急操作入口）

#### Scenario: 总览页不再展示 Cluster 拓扑

- **WHEN** 管理员访问容灾总览页
- **THEN** 页面 SHALL NOT 展示 Cluster 故障域拓扑卡片
- **THEN** 页面 SHALL NOT 引用 `grouping.ts` 分组逻辑
- **THEN** 页面 SHALL NOT 展示已删除的 Cluster region/priority/healthStatus 字段

#### Scenario: 转移事件流删除 clusterId 与共因跳过列

- **WHEN** 转移事件流渲染事件
- **THEN** 事件 SHALL 展示 from→to 渠道/端点、errorType、decision（L1/NONE）、exhausted、occurredAt
- **THEN** 事件 SHALL NOT 展示 `clusterId` / `fromClusterId` / `toClusterId` 字段
- **THEN** 事件 SHALL NOT 展示「是否共因跳过」列（`commonCauseSkip` 已删除）
- **THEN** 页面 SHALL 高亮耗尽事件

#### Scenario: 端点熔断状态大盘

- **WHEN** 管理员访问容灾总览页
- **THEN** 页面 SHALL 展示端点熔断状态大盘区块
- **THEN** 大盘 SHALL 列出各端点熔断器状态（CLOSED/OPEN/HALF_OPEN）
- **THEN** 大盘 SHALL 提供应急操作入口（force-open / force-close）

### Requirement: 应用管理页失败处理策略与超时配置

应用管理页 SHALL 移除容灾画像绑定，改为应用级 `failureStrategy` 策略选择、`timeout` 配置与渠道 `priority` 排序。

**变更要点**:
- 移除：容灾画像模板选择（ResilienceProfile 退场）、容灾模式档位选择
- 新增：应用 `failureStrategy` 配置（`FAIL_FAST`/`FAIL_RETRY`/`FAIL_OVER` 三选一下拉，默认 `FAIL_RETRY`）
- 新增：应用 `timeout` 配置（直接在应用编辑表单）
- 新增：渠道授权页支持 `priority` 配置（拖拽或数值排序，定义 L1 转移先后次序）

#### Scenario: 应用编辑配置 failureStrategy

- **WHEN** 管理员在应用编辑页选择失败处理策略
- **THEN** 系统 SHALL 保存到 `Application.failureStrategy`
- **THEN** 页面 SHALL 提供三选一下拉（`FAIL_FAST`/`FAIL_RETRY`/`FAIL_OVER`）
- **THEN** 未指定时 SHALL 默认 `FAIL_RETRY`
- **THEN** 页面 SHALL NOT 展示容灾画像绑定入口

#### Scenario: 应用编辑配置 timeout

- **WHEN** 管理员在应用编辑页配置 timeout
- **THEN** 系统 SHALL 保存到 `Application.timeout`
- **THEN** 页面 SHALL NOT 展示容灾画像绑定入口

#### Scenario: 渠道授权配置 priority

- **WHEN** 管理员在应用渠道授权页配置各渠道 priority
- **THEN** 系统 SHALL 保存到 `ApplicationChannel.priority`
- **THEN** 页面 SHALL 展示渠道按 priority 排序的先后次序

### Requirement: 端点熔断应急操作 UI

Channels 页与容灾总览页 SHALL 提供端点级熔断器应急操作 UI（force-open / force-close / state 查询），复用既有 `ChannelEndpointCircuitBreakerManager`，无新状态机。

**API**（已有，前端补 UI）:
- `POST /api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/force-open` — 一键熔断（强制 OPEN，立即切断端点流量）
- `POST /api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/force-close` — 一键恢复（强制 CLOSED 并重置窗口，立即恢复流量）
- `GET /api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/state` — 状态查询（返回 CLOSED/OPEN/HALF_OPEN）

**规则**:
- 应急操作前校验端点归属（`endpoint.channelId == 传入 channelId`），避免误操作其他渠道端点
- `forceOpen` 无视滑动窗口直接置 OPEN，`forceClose` 置 CLOSED 并重置窗口
- 应急操作返回 `CircuitBreakerStateResponse`（含状态名）
- 与应用 `failureStrategy` 正交：策略控制候选间转移，熔断器控制端点级跳过

#### Scenario: 一键熔断端点

- **WHEN** 管理员在 Channels 页或总览页大盘对某端点点击「一键熔断」
- **THEN** 系统 SHALL 调用 `force-open` 端点
- **THEN** 熔断器 SHALL 立即转 OPEN，后续请求跳过该端点
- **THEN** `HealthRouter` SHALL 在后续路由中过滤该端点

#### Scenario: 一键恢复端点

- **WHEN** 管理员对已熔断端点点击「一键恢复」
- **THEN** 系统 SHALL 调用 `force-close` 端点
- **THEN** 熔断器 SHALL 立即转 CLOSED 并重置窗口，恢复流量

#### Scenario: 查询端点熔断状态

- **WHEN** 管理员查看某端点熔断状态
- **THEN** 系统 SHALL 调用 `state` 端点
- **THEN** 系统 SHALL 返回 CLOSED/OPEN/HALF_OPEN 之一

#### Scenario: 紧切域功能已移除

- **WHEN** 管理员访问 Channels 页
- **THEN** 页面 SHALL NOT 提供「紧切域」操作
- **THEN** 系统 SHALL NOT 暴露 `PUT /api/v1/channels/{id}/cluster` 端点（Cluster 实体与 clusterId 字段已删除）
