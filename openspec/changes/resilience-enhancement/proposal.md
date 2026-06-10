## Why

LLM-Gateway 作为企业级 AI 网关，上游大模型（Provider）异常处理直接影响业务连续性 SLA。当前代码存在以下问题：

1. **所有异常一律抛 `"UPSTREAM_ERROR"`** — `ProviderErrorType` 8 种枚举定义了但未使用，无法区分 429 限流、503 不可用、504 超时等不同场景
2. **SSE 流式错误裸奔** — 文档要求结构化 SSE 错误事件，实际直接 `callback.onError(t)` 抛原始 IOException
3. **熔断器异常未纳入异常体系** — `CircuitOpenException` 独立于 `RuntimeException`，全局异常处理器无法统一捕获
4. **差异化重试策略缺失** — 文档要求 429/503/504 不同退避策略，代码统一指数退避
5. **无渠道级故障转移** — 重试耗尽后不会切换 Channel/Key，请求直接失败
6. **异常上下文缺失** — Trace ID、Model、Provider 等诊断信息未注入异常
7. **无 Metrics 埋点** — 熔断器/重试器关键事件无监控指标
8. **智能降级未实现** — 模型级智能降级（P0 核心竞争力）代码空白

这些问题共同导致：上游异常不可观测、不可区分、不可差异化处理，严重影响运维排障能力和业务连续性承诺。

## What Changes

1. **ProviderErrorType 落地** — `OpenAIUpstreamClient` / `AnthropicUpstreamClient` 按 HTTP 状态码和错误体内容，映射到正确的 `ProviderErrorType`，抛出带类型的 `ProviderException`
2. **SSE 流式结构化错误** — 流式错误按文档要求格式化为 `event: error\ndata: {"error":"<type>","retry_after":N}` 格式
3. **CircuitOpenException 继承改造** — 改为继承 `ProviderException`，纳入统一异常体系
4. **差异化重试策略** — `RetryExecutor` 按状态码选择退避策略（429 长退避、504 快速重试、503 适中等待）
5. **渠道级故障转移** — 重试耗尽后，`ChatDispatchService` 尝试下一可用 Channel/Key
6. **异常上下文注入** — `ProviderException` 新增 `traceId`、`model`、`provider`、`channelEndpointId` 字段
7. **Metrics 埋点** — `CircuitBreaker` 和 `RetryExecutor` 关键事件上报 Micrometer 指标
8. **智能降级骨架** — 实现 `DegradationService`，支持降级链配置和自动切换

## Capabilities

### New Capabilities
- `upstream-exception-classification`: 上游异常分类与结构化错误响应
- `intelligent-degradation`: 模型级智能降级（降级链配置 + 自动切换 + 回切）

### Modified Capabilities
- （无现有 spec 变更 — 这是基础设施层增强，不改变已有 capability 的外部行为）

## Impact

| 影响范围 | 说明 |
|---------|------|
| `infrastructure/resilience/` | CircuitBreaker、RetryExecutor、ResilientUpstreamClient 增强 |
| `infrastructure/upstream/` | OpenAIUpstreamClient、AnthropicUpstreamClient 错误分类 |
| `domain/supply/exception/` | ProviderException 增加上下文字段 |
| `application/proxy/` | ChatDispatchServiceImpl 故障转移逻辑 |
| `application/degradation/` | 新建 DegradationService 实现 |
| `adapter/advice/` | GlobalExceptionHandler 适配新异常类型 |
| 配置 | 新增差异化重试配置、降级链配置 |
