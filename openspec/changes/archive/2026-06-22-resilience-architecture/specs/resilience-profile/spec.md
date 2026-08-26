# Resilience Profile Delta Spec

## ADDED Requirements

### Requirement: ResilienceProfile 容灾画像实体

系统 SHALL 提供 `ResilienceProfile` 根实体实体作为应用级容灾配置的载体，承载四层容灾栈的开关与参数。画像纯数据库管理，全部落库 + CRUD + 控制台管理。

**实体字段**:
- `code` — 画像编码，全局唯一（如 `default`/`strict`/`aggressive`/`batch`）
- `name` — 画像名称
- `mode` — 容灾模式档位（`ResilienceMode`：`STANDARD`/`STRICT`/`AGGRESSIVE`），管理员面向字段
- `enableL2ModelDegradation` — 是否启用 L2 模型级降级兜底
- `degradationMaxDepth` — L2 降级最大深度（0 表示禁用降级）
- `enableSessionAffinity` — 是否启用会话亲和
- `sessionAffinityTtlMinutes` — 会话亲和 TTL（分钟），默认 30
- `enablePinnedModel` — 是否启用模型锁定
- `pinnedModelId` — 锁定模型 ID（可空）
- `timeout` — 请求超时秒数（0 表示用渠道默认）
- 审计字段继承自 `BaseEntity`

**API**:
- `POST /api/v1/resilience/profiles` — 创建画像（HTTP 201）
- `PUT /api/v1/resilience/profiles/{id}` — 更新画像
- `GET /api/v1/resilience/profiles/{id}` — 查询画像详情
- `GET /api/v1/resilience/profiles` — 查询全部画像列表

**规则**:
- 不提供 delete：default 画像为系统兜底禁删；其余画像因 Gateway 无 delete 方法遵循既有模式
- 预设档位（default/strict/aggressive/batch）由初始化数据（`V56__seed_resilience_profiles.sql`）写入

#### Scenario: 创建容灾画像

- **WHEN** 管理员调用 `POST /api/v1/resilience/profiles` 传入合法画像字段
- **THEN** 系统 SHALL 创建 `ResilienceProfile` 记录，`code` 全局唯一
- **THEN** 系统 SHALL 返回 HTTP 201 与创建后的画像响应

#### Scenario: 预设档位由初始化数据写入

- **WHEN** 系统启动并执行数据库迁移
- **THEN** 系统 SHALL 写入 `default`/`strict`/`aggressive`/`batch` 预设档位画像
- **THEN** 运行期 `default` 画像 SHALL 始终存在

### Requirement: 解析链 Application → Global

`ResilienceResolver` SHALL 按解析链 `Application → Global` 解析应用对应的容灾画像（Team 已移除，无中间层）。

**解析规则**:
1. 按 `applicationId` 查 `Application`；不存在则 fail-fast 抛 `GatewayRequestException`（`APPLICATION_NOT_FOUND`）
2. `Application.resilienceProfileId` 非空时，按 ID 查画像；命中则返回应用级画像
3. `Application` 未挂画像、或画像已被删（`findById` 返回 null）时，回退全局 `default` 画像（`findByCode("default")`）
4. `default` 画像不存在时 fail-fast 抛 `GatewayRequestException`（`RESILIENCE_DEFAULT_PROFILE_MISSING`），暴露系统初始化异常

#### Scenario: 命中应用级画像

- **WHEN** `Application.resilienceProfileId` 非空且画像存在
- **THEN** `ResilienceResolver.resolve` SHALL 返回应用级画像

#### Scenario: 应用未挂画像回退 default

- **WHEN** `Application.resilienceProfileId` 为 null
- **THEN** `ResilienceResolver.resolve` SHALL 回退全局 `default` 画像

#### Scenario: 应用画像被删回退 default

- **WHEN** `Application.resilienceProfileId` 非空但对应画像已被删除
- **THEN** `ResilienceResolver.resolve` SHALL 回退全局 `default` 画像（不抛异常，保持解析链鲁棒性）

#### Scenario: default 画像缺失 fail-fast

- **WHEN** 全局 `default` 画像不存在
- **THEN** `ResilienceResolver.resolve` SHALL 抛 `GatewayRequestException`（`RESILIENCE_DEFAULT_PROFILE_MISSING`）

### Requirement: 容灾模式档位推导

`ResilienceProfileApplier` SHALL 按容灾模式档位（`STANDARD`/`STRICT`/`AGGRESSIVE`）自动推导画像专家字段。管理员选档位时，按档位覆盖专家字段，其余字段保留 base 画像原值。

**档位语义**:
- `STANDARD`（标准）— `enableL2=true`, `degradationMaxDepth=2`（浅降级）, `timeout=0`（用渠道默认）。通用默认，平衡可用性与质量。
- `STRICT`（严格）— `enableL2=false`, `degradationMaxDepth=0`（L2 关闭）, `timeout=60`。不可降级场景，宁可报错不可换模型（对应 Claude Code/CodeX）。
- `AGGRESSIVE`（激进）— `enableL2=true`, `degradationMaxDepth=3`（深降级）, `timeout=15`（短超时）。可用性优先，质量次之（对应客服/HelpDesk）。

**规则**:
- `BATCH`（批量）为 `STANDARD` 的 `QUEUED` 转移变体，不单列档位，由「容灾模式=STANDARD + 高级里切 transferMode=QUEUED」实现
- `apply` 不修改 base（不可变），返回推导后的新 `ResilienceProfile`
- 覆盖字段：`mode`/`enableL2ModelDegradation`/`degradationMaxDepth`/`timeout`
- 保留字段：`code`/`name`/`enableSessionAffinity`/`sessionAffinityTtlMinutes`/`enablePinnedModel`/`pinnedModelId`

#### Scenario: STANDARD 档位推导

- **WHEN** 管理员选择 `STANDARD` 档位
- **THEN** `ResilienceProfileApplier.apply` SHALL 设置 `enableL2ModelDegradation=true`、`degradationMaxDepth=2`、`timeout=0`

#### Scenario: STRICT 档位推导

- **WHEN** 管理员选择 `STRICT` 档位
- **THEN** `ResilienceProfileApplier.apply` SHALL 设置 `enableL2ModelDegradation=false`、`degradationMaxDepth=0`、`timeout=60`

#### Scenario: AGGRESSIVE 档位推导

- **WHEN** 管理员选择 `AGGRESSIVE` 档位
- **THEN** `ResilienceProfileApplier.apply` SHALL 设置 `enableL2ModelDegradation=true`、`degradationMaxDepth=3`、`timeout=15`

### Requirement: 会话亲和

系统 SHALL 提供会话亲和（`SessionAffinityStore`），按请求头 `X-Session-Id` 亲和到首次命中渠道。

**接口**: `get(sessionId)` / `put(sessionId, channelId)` / `evict(sessionId)`

**双实现**:
- `RedisSessionAffinityStore`（生产环境，需 `StringRedisTemplate`）
- `InMemorySessionAffinityStore`（开发/测试环境兜底）

**配置**（前缀 `session.affinity`）:
- `enabled` — 是否启用（默认 `true`）
- `ttlMinutes` — TTL 分钟（默认 `30`）

**规则**:
- 标识缺失（`sessionId` 为 null）时 `get` 返回 null（不亲和）、`put` 不存储（安全降级）
- TTL 过期后 `get` 返回 null（不亲和）
- 亲和优先非强制：命中渠道熔断则转移并更新亲和绑定

#### Scenario: 按 X-Session-Id 亲和

- **WHEN** 请求携带 `X-Session-Id` 且该会话已绑定渠道
- **THEN** 系统 SHALL 优先路由到绑定渠道

#### Scenario: 会话标识缺失不亲和

- **WHEN** 请求未携带 `X-Session-Id`（或为 null）
- **THEN** 系统 SHALL NOT 亲和，按正常路由选择

#### Scenario: 亲和渠道熔断则转移

- **WHEN** 会话亲和命中的渠道已熔断
- **THEN** 系统 SHALL 转移到下一候选
- **THEN** 系统 SHALL 更新亲和绑定到新命中渠道

### Requirement: 画像门禁 L2 降级

容灾画像 SHALL 作为 L2 模型降级的门禁。`DegradationService.degrade(reason, profile)` 在 `profile != null` 时受画像字段约束。

**门禁规则**（`profile != null` 时）:
- `enableL2ModelDegradation=false` → 不降级，返回 null
- `degradationMaxDepth <= 0` → 不降级，返回 null
- 按 errorType 分流：仅 `FailoverDecision.L2` 类错误（模型能力问题）触发模型降级；`L1`（共因故障）/`NONE`（请求级错误）不换模型
- 深度上限取 `profile.degradationMaxDepth`（ungated 时取配置 `maxChainDepth`）

#### Scenario: 画像关闭 L2 不降级

- **WHEN** `profile.enableL2ModelDegradation=false`
- **THEN** `degrade` SHALL 返回 null，不触发模型降级

#### Scenario: 画像深度为 0 不降级

- **WHEN** `profile.degradationMaxDepth <= 0`
- **THEN** `degrade` SHALL 返回 null

#### Scenario: 非 L2 错误不降级

- **WHEN** 错误经 `ErrorClassifier.classify` 分流为 `L1` 或 `NONE`
- **THEN** `degrade` SHALL 返回 null，不换模型

### Requirement: 画像解析 fail-open

`InstanceSelector` 解析容灾画像时 SHALL fail-open：`applicationId` 为 null 或画像解析抛异常时降级为 null profile，不阻断路由。

#### Scenario: 画像解析失败不阻断路由

- **WHEN** `ResilienceResolver.resolve` 抛异常
- **THEN** `InstanceSelector.resolveProfileSafely` SHALL 捕获异常并返回 null profile
- **THEN** 路由链 SHALL 继续执行（仅影响画像化决策，不影响基础路由）
