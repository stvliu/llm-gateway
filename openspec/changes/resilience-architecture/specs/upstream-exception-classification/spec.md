# Upstream Exception Classification Delta Spec

## MODIFIED Requirements

### Requirement: 错误分类接入错误分流表驱动转移决策

上游异常分类结果（`ProviderErrorType`）SHALL 接入错误分流表（`ErrorClassifier`），驱动 L1/L2/NONE 转移决策。

**变更要点**:
- 原错误分类（`ProviderErrorType`）仅用于差异化重试策略与异常上下文，不直接驱动转移层级
- 现错误分类结果经 `ErrorClassifier.classify(errorType)` 映射到 `FailoverDecision`（L1/L2/NONE），驱动 `ChannelFailoverInvoker` 转移决策

**分流规则**（错误分流表）:
- `INVALID_REQUEST` → `NONE`（请求级错误，换哪都无效，直接抛出）
- 共因故障（`AUTHENTICATION_ERROR`/`RATE_LIMIT_ERROR`/`QUOTA_EXCEEDED`/`TIMEOUT_ERROR`/`UPSTREAM_ERROR`/`SERVICE_UNAVAILABLE`/`NETWORK_ERROR`）→ `L1`（换渠道）
- `UNKNOWN_ERROR` → `L2`（模型能力问题，换模型降级）
- `null` 输入 → `NONE`（编程错误或未分类，直接抛出不转移）
- 未在表中显式映射的新增枚举值 → `L2`（兜底防御性降级）

**ProviderErrorType 分类来源**（保留既有 HTTP 状态码映射）:

| HTTP 状态码 | ProviderErrorType | 分流决策 |
|------------|------------------|---------|
| 401 | AUTHENTICATION_ERROR | L1 |
| 429（不含 quota） | RATE_LIMIT_ERROR | L1 |
| 429（含 quota/insufficient_quota） | QUOTA_EXCEEDED | L1 |
| 400 | INVALID_REQUEST | NONE |
| 408 / ReadTimeout | TIMEOUT_ERROR | L1 |
| 500 / 502 | UPSTREAM_ERROR | L1 |
| 503 | SERVICE_UNAVAILABLE | L1 |
| 504 | TIMEOUT_ERROR | L1 |
| 529（Anthropic 过载） | UPSTREAM_ERROR | L1 |
| IOException | NETWORK_ERROR | L1 |
| 其他 | UNKNOWN_ERROR | L2 |

#### Scenario: 错误分类驱动 L1 换渠道

- **WHEN** 上游返回 HTTP 401，`ProviderErrorType = AUTHENTICATION_ERROR`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L1`
- **THEN** `ChannelFailoverInvoker` SHALL 换下一候选渠道

#### Scenario: 错误分类驱动 NONE 不转移

- **WHEN** 上游返回 HTTP 400，`ProviderErrorType = INVALID_REQUEST`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `NONE`
- **THEN** `ChannelFailoverInvoker` SHALL 直接抛出原异常，不转移

#### Scenario: 错误分类驱动 L2 换模型

- **WHEN** 上游返回未分类状态码，`ProviderErrorType = UNKNOWN_ERROR`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L2`
- **THEN** L1 候选耗尽后 SHALL 进入 L2 模型降级

### Requirement: 错误分类器按供应商区分

错误分类 SHALL 由按供应商区分的策略实现（`ErrorClassificationStrategy`），各供应商（OpenAI/Anthropic）有独立分类器，映射 HTTP 状态码到 `ProviderErrorType`。

**规则**:
- `OpenAIErrorClassifier`（`supportedProvider = "openai"`）：500/502 → `UPSTREAM_ERROR`，不处理 529
- `AnthropicErrorClassifier`（`supportedProvider = "anthropic"`）：500/502/529 → `UPSTREAM_ERROR`（529 为 Anthropic 过载）

#### Scenario: OpenAI 502 映射为 UPSTREAM_ERROR 走 L1

- **WHEN** OpenAI 上游返回 HTTP 502
- **THEN** `OpenAIErrorClassifier.classify` SHALL 返回 `UPSTREAM_ERROR`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L1`

#### Scenario: Anthropic 529 映射为 UPSTREAM_ERROR 走 L1

- **WHEN** Anthropic 上游返回 HTTP 529（过载）
- **THEN** `AnthropicErrorClassifier.classify` SHALL 返回 `UPSTREAM_ERROR`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L1`
