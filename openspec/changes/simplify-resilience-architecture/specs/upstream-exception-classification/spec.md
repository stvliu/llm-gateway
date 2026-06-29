# Upstream Exception Classification Delta Spec

## MODIFIED Requirements

### Requirement: 错误分类接入错误分流表驱动转移决策

上游异常分类结果（`ProviderErrorType`）SHALL 接入错误分流表（`ErrorClassifier`），驱动 L1/NONE 转移决策。

**变更要点**（删除 L2，UNKNOWN 改 NONE）:
- 原错误分类经 `ErrorClassifier.classify(errorType)` 映射到 `FailoverDecision`（L1/L2/NONE）
- 现 `FailoverDecision` 收敛为 L1/NONE（删除 L2）
- `UNKNOWN_ERROR` 由 L2 改为 NONE（未分类错误不转移直接抛，降级决策还给应用）

**分流规则**（错误分流表）:
- `INVALID_REQUEST` → `NONE`（请求级错误，换哪都无效，直接抛出）
- 共因故障（`AUTHENTICATION_ERROR`/`RATE_LIMIT_ERROR`/`QUOTA_EXCEEDED`/`TIMEOUT_ERROR`/`UPSTREAM_ERROR`/`SERVICE_UNAVAILABLE`/`NETWORK_ERROR`）→ `L1`（换渠道）
- `UNKNOWN_ERROR` → `NONE`（未分类错误，不转移直接抛）
- `null` 输入 → `NONE`（编程错误或未分类，直接抛出不转移）
- 未在表中显式映射的新增枚举值 → `NONE`（兜底不转移，直接抛）

**ProviderErrorType 分类来源**（HTTP 状态码映射不变）:

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
| 其他 | UNKNOWN_ERROR | NONE |

#### Scenario: 错误分类驱动 L1 换渠道

- **WHEN** 上游返回 HTTP 401，`ProviderErrorType = AUTHENTICATION_ERROR`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L1`
- **THEN** `ChannelFailoverInvoker` SHALL 换下一候选渠道

#### Scenario: 错误分类驱动 NONE 不转移

- **WHEN** 上游返回 HTTP 400，`ProviderErrorType = INVALID_REQUEST`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `NONE`
- **THEN** `ChannelFailoverInvoker` SHALL 直接抛出原异常，不转移

#### Scenario: 未分类错误不转移

- **WHEN** 上游返回未分类状态码，`ProviderErrorType = UNKNOWN_ERROR`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `NONE`
- **THEN** `ChannelFailoverInvoker` SHALL 直接抛出原异常，不转移（降级决策还给应用）
