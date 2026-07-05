# application Specification

## Purpose
TBD - created by archiving change resilience-architecture. Update Purpose after archive.
## Requirements
### Requirement: Application 聚合根实体

系统 SHALL 提供 `Application` 聚合根实体作为「权限 + 行为」双聚合根，承载 N 把 Key 的应用归属、渠道可见性、应用级超时，并预留配额/看板字段。

**实体字段**（移除 `resilienceProfileId`，新增 `timeout` 与 `failureStrategy`）:
- `code` — 应用编码，全局唯一
- `name` — 应用名称
- `description` — 应用描述
- `state` — 应用生命周期状态（`ApplicationState`，控制是否可路由）
- `timeout` — 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout，ResilienceProfile 实体退场）
- `failureStrategy` — 应用级失败处理策略（`FailureStrategy` 枚举：`FAIL_FAST`/`FAIL_RETRY`/`FAIL_OVER` 三选一，默认 `FAIL_RETRY`；承接原 ResilienceProfile 的失败处理策略语义，控制 `ChannelFailoverInvoker` 的 L0/L1 行为）
- `quotaBudgetId` — 配额预算 ID（预留，留 quota 域填充）
- `dashboardId` — 看板 ID（预留，留 audit 域填充）
- 审计字段 `createdBy/createdAt/updatedBy/updatedAt` 继承自 `BaseEntity`

**移除字段**:
- `resilienceProfileId` — ResilienceProfile 实体退场，不再关联画像

**API**:
- `POST /api/v1/applications` — 创建应用（请求体 `code/name/description/timeout/failureStrategy`，返回 `ApplicationResponse`，HTTP 201）
- `PUT /api/v1/applications/{id}` — 更新应用
- `GET /api/v1/applications/{id}` — 查询应用详情
- `GET /api/v1/applications` — 查询全部应用列表
- `DELETE /api/v1/applications/{id}` — 删除应用（级联清理渠道授权关联，HTTP 204）

#### Scenario: 创建应用

- **WHEN** 管理员调用 `POST /api/v1/applications` 传入合法 `code/name/description/timeout/failureStrategy`
- **THEN** 系统 SHALL 创建 `Application` 记录，`code` 全局唯一
- **THEN** 系统 SHALL 返回 HTTP 201 与创建后的 `ApplicationResponse`

#### Scenario: 删除应用级联清理渠道授权

- **WHEN** 管理员调用 `DELETE /api/v1/applications/{id}` 删除应用
- **THEN** 系统 SHALL 级联清理该应用的 `ApplicationChannel` 关联
- **THEN** 系统 SHALL 返回 HTTP 204

### Requirement: 应用级失败处理策略 failureStrategy

`Application.failureStrategy` SHALL 为 `FailureStrategy` 枚举（`FAIL_FAST`/`FAIL_RETRY`/`FAIL_OVER` 三选一互斥），控制 `ChannelFailoverInvoker` 的 L0（同渠道换 Key）/L1（换渠道）行为。轻量单字段挂 `Application`，不演变为已删除的 `ResilienceProfile` 独立实体。

**策略与 L0/L1 行为**（递进关系：`FAIL_FAST` ⊂ `FAIL_RETRY` ⊂ `FAIL_OVER`）:

| 策略 | L0 同渠道换 Key | L1 换渠道 | 行为 |
|------|----------------|----------|------|
| `FAIL_FAST`（快速失败） | 否 | 否 | 首个 Key 失败立即抛错（只试首个 Key，不换 Key 不换渠道） |
| `FAIL_RETRY`（失败重试，默认） | 是 | 否 | 同渠道内换 Key，不换渠道；同渠道 Key 耗尽抛错 |
| `FAIL_OVER`（失败转移） | 是 | 是 | 换 Key + 按 `ApplicationChannel.priority` 换渠道，全耗尽抛错 |

**默认值**:
- 新建应用未指定时 SHALL 默认 `FAIL_RETRY`（契合「同供应商多 Key」主场景，K1 限流换 K2）
- 数据迁移：现有应用在 Flyway 迁移时 SHALL 设为 `FAIL_OVER`（保持原 L0+L1 全跑行为不变）

**与熔断器正交**:
- `failureStrategy` 控制候选间转移决策；`ChannelEndpointCircuitBreakerManager`（端点级熔断器）控制端点级跳过
- 端点连续失败 → OPEN → 后续请求跳过该端点；管理员可手动 forceOpen/forceClose 应急
- 策略与熔断器互不依赖，可组合使用

**与错误分流正交**:
- `ErrorClassifier.classify(errorType)` 返回 `NONE`（请求级错误，如 `INVALID_REQUEST`）时，无论策略如何 SHALL 直接抛出不转移
- 返回 `L1` 时按上述策略表控制 L0/L1 行为

#### Scenario: FAIL_FAST 首个 Key 失败立即抛错

- **WHEN** 应用 `failureStrategy = FAIL_FAST`，候选首个 Key 调用抛出可转移异常（如 `AUTHENTICATION_ERROR`）
- **THEN** `ChannelFailoverInvoker` SHALL NOT 换 Key（不跑 L0）
- **THEN** `ChannelFailoverInvoker` SHALL NOT 换渠道（不跑 L1）
- **THEN** 系统 SHALL 立即抛出原异常

#### Scenario: FAIL_RETRY 同渠道换 Key 不换渠道

- **WHEN** 应用 `failureStrategy = FAIL_RETRY`，候选渠道内某 Key 调用抛出可转移异常
- **THEN** `ChannelFailoverInvoker` SHALL 在同渠道内换下一个 Key 重试（跑 L0）
- **THEN** 同渠道所有 Key 耗尽时 SHALL 抛出最后捕获的异常
- **THEN** `ChannelFailoverInvoker` SHALL NOT 换下一候选渠道（不跑 L1）
- **THEN** 系统 SHALL NOT 发布转移事件（未发生跨候选转移）

#### Scenario: FAIL_OVER 换渠道全耗尽抛错

- **WHEN** 应用 `failureStrategy = FAIL_OVER`，候选渠道所有 Key 耗尽仍失败
- **THEN** `ChannelFailoverInvoker` SHALL 按 `ApplicationChannel.priority` 顺序换下一候选渠道（跑 L1）
- **THEN** 换候选前 SHALL 发布 `FailoverOccurredEvent` 转移事件
- **THEN** 所有候选耗尽时 SHALL 抛出最后捕获的异常

#### Scenario: 请求级错误不转移无视策略

- **WHEN** 候选调用抛出 `errorType = INVALID_REQUEST`（`ErrorClassifier` 返回 `NONE`）
- **THEN** 无论应用 `failureStrategy` 为何值，`ChannelFailoverInvoker` SHALL 直接抛出原异常
- **THEN** 系统 SHALL NOT 换 Key、SHALL NOT 换渠道

#### Scenario: 未指定策略回退默认 FAIL_RETRY

- **WHEN** 应用创建/更新时未传 `failureStrategy` 字段（或为 null）
- **THEN** 系统 SHALL 回退默认值 `FAIL_RETRY`
- **THEN** `ChannelFailoverInvoker` 运行时读取 null 策略时 SHALL 回退 `FAIL_RETRY`

#### Scenario: 现有应用迁移保持 FAIL_OVER 行为

- **WHEN** Flyway 迁移脚本执行（V68）
- **THEN** 现有应用 `failureStrategy` SHALL 设为 `FAIL_OVER`（保持原 L0+L1 全跑行为）
- **THEN** 迁移后新建应用 SHALL 默认 `FAIL_RETRY`

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

