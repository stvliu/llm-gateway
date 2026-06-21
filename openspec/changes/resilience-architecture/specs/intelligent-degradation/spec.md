# Intelligent Degradation Delta Spec

## MODIFIED Requirements

### Requirement: 降级触发

`DegradationService.degrade(reason)` SHALL 由「不分流的全局主容灾手段」改为「按错误类型分流 + L2 应用可选兜底」。降级定位由全局主容灾降级为 L2 应用可选兜底，受画像门禁约束。本条修订既有「降级触发」Requirement：在保留原触发条件表的基础上，叠加按 `ProviderErrorType` 分流与画像门禁两层约束。

**变更要点**:
- 原 `degrade(originalModel, reason)` 不分流——任一降级触发条件均切换备选模型
- 现 `degrade(originalModel, reason, profile)` 按 `ProviderErrorType` 分流：仅 `FailoverDecision.L2` 类错误（模型能力问题，`UNKNOWN_ERROR`）触发模型降级；`L1`（共因故障）/`NONE`（请求级错误）不换模型
- 降级定位由「全局主容灾」降级为「L2 应用可选兜底」：L1 Channel 级转移（`ChannelFailoverInvoker`）为主路径，L2 模型降级仅在 L1 全耗尽且画像启用时触发
- `enableL2ModelDegradation`/`degradationMaxDepth` 受画像门禁（`profile != null` 时）

**门禁规则**（`profile != null` 时）:
- `enableL2ModelDegradation=false` → 不降级，返回 null
- `degradationMaxDepth <= 0` → 不降级，返回 null
- 按 errorType 分流：仅 `FailoverDecision.L2` 触发降级
- 深度上限取 `profile.degradationMaxDepth`（ungated 时取配置 `maxChainDepth`）

**触发条件**（保留原表，但受分流与门禁约束）:

| 触发条件 | 判定方式 | errorType | 分流决策 |
|---------|---------|-----------|---------|
| 主模型上游 429/5xx | `ProviderException` | `RATE_LIMIT_ERROR`/`UPSTREAM_ERROR`/`SERVICE_UNAVAILABLE` | L1（不降级） |
| 主模型熔断器 OPEN | `CircuitOpenException` | （共因） | L1（不降级） |
| 主模型 Token 限额超限 | `QuotaExceededException` | `QUOTA_EXCEEDED` | L1（不降级） |
| 主模型超时 | `TIMEOUT_ERROR` | `TIMEOUT_ERROR` | L1（不降级） |
| 请求级错误 | `INVALID_REQUEST` | `INVALID_REQUEST` | NONE（不转移不降级） |
| 模型能力问题 | `UNKNOWN_ERROR` | `UNKNOWN_ERROR` | L2（降级） |

#### Scenario: 共因故障不触发模型降级

- **WHEN** 调用主模型抛出 `errorType = AUTHENTICATION_ERROR`（或 `QUOTA_EXCEEDED`/`RATE_LIMIT_ERROR`/`TIMEOUT_ERROR`/`UPSTREAM_ERROR`/`SERVICE_UNAVAILABLE`/`NETWORK_ERROR`）
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L1`
- **THEN** `degrade` SHALL 返回 null（不换模型，由 L1 Channel 级转移处理）

#### Scenario: 模型能力问题触发降级（受画像门禁）

- **WHEN** 调用主模型抛出 `errorType = UNKNOWN_ERROR` 且 `profile.enableL2ModelDegradation=true`、`degradationMaxDepth > 0`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L2`
- **THEN** `degrade` SHALL 返回降级链中第一个可用备选模型名

#### Scenario: 请求级错误不降级

- **WHEN** 调用主模型抛出 `errorType = INVALID_REQUEST`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `NONE`
- **THEN** `degrade` SHALL 返回 null

#### Scenario: 画像关闭 L2 不降级

- **WHEN** `profile.enableL2ModelDegradation=false`（如 STRICT 档位）
- **THEN** `degrade` SHALL 返回 null，即使错误分流为 L2

#### Scenario: 降级链中所有模型均不可用时抛出异常

- **WHEN** 降级链中所有模型均不可用
- **THEN** 抛出 `ProviderException("ALL_MODELS_DEGRADED")`

## ADDED Requirements

### Requirement: L2 降级信号由上层重路由

`ChannelFailoverInvoker` 在 L1 候选全耗尽且 L2 降级成功时，SHALL 抛出 `L2DegradationRequiredException`（携带 `fallbackModel` + 原始失败 cause），由上层 `ChatDispatchService` 捕获并用 fallback 模型重新 `resolveCandidates` + 调用 Invoker。替代原隐式字符串前缀契约。

#### Scenario: L2 降级成功抛信号重路由

- **WHEN** L1 候选全部耗尽，`degrade` 返回有效 `fallbackModel`
- **THEN** `ChannelFailoverInvoker` SHALL 抛出 `L2DegradationRequiredException`（含 `fallbackModel`/`originalModel`/`lastErrorType`/`lastException`）
- **THEN** 上层 SHALL 用 `fallbackModel` 重新解析候选并调用

#### Scenario: L2 降级失败抛最后异常

- **WHEN** L1 候选全部耗尽，`degrade` 返回 null（无可用备选或门禁未通过）
- **THEN** `ChannelFailoverInvoker` SHALL 抛出最后捕获的 `ProviderException`
