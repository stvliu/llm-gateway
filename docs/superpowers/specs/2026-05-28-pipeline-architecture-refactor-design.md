# 可配置管道架构重构与缺陷修复设计

## 概述

对 LLM-Gateway 的七阶段调度链进行架构重构，将 ChatDispatchServiceImpl 中的硬编码调用链升级为可配置的 StagePipeline 模式，同步修复已识别的 10+ 缺陷。

## 目标

1. **可配置管道**：调度阶段可编排、可插拔、可观测
2. **行为等价**：重构前后外部行为一致，430+ 测试全绿
3. **职责归位**：协议控制器统一化、上游客户端模板化、跨领域关注点分离
4. **缺陷归零**：已识别的 10+ 缺陷全部修复并有测试覆盖

## 实施策略

分 5 个 Phase 交付，每 Phase 可独立编译且不破坏主分支：

| Phase | 名称 | 文件数 | 可独立编译 |
|-------|------|--------|-----------|
| 1 | 契约层加固 + RoutingContext 完善 | ~8 | 是 |
| 2 | 基础设施层抽象 + 上游客户端标准化 | ~6 | 是 |
| 3 | Pipeline + Controller 层重构 | ~15 | 是（Phase 1/2 前置） |
| 4 | 横切关注点（缓存/Trace/注解） | ~5 | 是（Phase 3 前置） |
| 5 | 缺陷修复 + 测试覆盖 | ~8 | 是（Phase 3 前置） |

---

## Phase 1：契约层加固

### 变更清单

#### 1.1 ProtocolRequest 不可变改造

- **文件**: `domain/protocol/contract/ProtocolRequest.java`
- 变更: `setModel(String)` → `withModel(String)` 返回新实例；`setStream(Boolean)` → `withStream(Boolean)` 返回新实例
- 影响: 所有实现类（OpenAIChatRequest、AnthropicMessagesRequest）同步更新
- **连带影响**: `OutboundTuner.tune(ProtocolRequest, RoutingContext)` 当前可能原地修改 request，需改为返回 `ProtocolRequest` 新实例。此变更在 Phase 3.3 OutboundTuneStage 中落地

#### 1.2 ProtocolResponse 增加 getUsage()

- **文件**: `domain/protocol/contract/ProtocolResponse.java`
- 变更: 增加默认方法 `default Optional<TokenUsage> getUsage() { return Optional.empty(); }`
- 新增 `TokenUsage` record: `record TokenUsage(int promptTokens, int completionTokens)`
- 影响: OpenAIChatResponse、AnthropicMessagesResponse 覆写 getUsage()

#### 1.3 RoutingContext 完善

- **文件**: `domain/supply/valueobject/RoutingContext.java`
- 变更: 增加 `String providerCode` 字段；`toString()` 对 `providerApiKey` 做脱敏（`sk-****`）
- 影响: RoutingResolver 在构造 RoutingContext 时从 `Channel.providerCode` 回填。
- **连带变更**: `domain/supply/entity/Channel.java` 新增 `providerCode` 字段（String，标识提供商代号如 `"openai"`、`"anthropic"`）；`DataInitializer` 预填该字段

#### 1.4 新增 TokenUsage

- **文件**: `domain/protocol/contract/TokenUsage.java`（新增）
- 内容: `record TokenUsage(int promptTokens, int completionTokens)`，作为跨 DTO 的共用不可变对象

### 验收标准

| # | 标准 | 验证方式 |
|---|------|---------|
| AC1.1 | 编译通过，所有原有测试全绿 | `mvn clean install` 430 测试通过 |
| AC1.2 | `OpenAIChatRequest request.withModel("gpt-4o")` 返回新对象，原对象不变 | 单元测试验证不可变性 |
| AC1.3 | `RoutingContext.toString()` 输出中 API Key 被脱敏为 `sk-***` | 日志断言测试 |
| AC1.4 | `OpenAIChatResponse.getUsage()` 返回 `Optional<TokenUsage>`，含 promptTokens 和 completionTokens | 单元测试 + 结构断言 |

---

## Phase 2：基础设施层抽象

### 变更清单

#### 2.1 AbstractUpstreamClient 模板方法

- **文件**: `infrastructure/gateway/upstream/AbstractUpstreamClient.java`（新增）
- **设计约束**: 模板方法必须兼容 OpenAI（`[DONE]` 标记）和 Anthropic（`event:` + `message_stop` 配对）两种 SSE 风格的流结束检测。不应使用 `shouldSkipLine()` 抽象方法，因为 Anthropic 的 `event:` 行包含流结束判断所需状态。
- 抽象方法:
  - `String getEndpointPath()` → 返回 `/v1/chat/completions` 或 `/v1/messages`
  - `Map<String, String> getHeaders(String apiKey)` → 返回认证头 + 内容类型头
  - `Class<? extends ProtocolResponse> getResponseType()` → 返回响应类型
  - `boolean isStreamComplete(String line)` → 判断行是否为流结束标记（OpenAI: `"[DONE]"`；Anthropic: `"event: message_stop"` 后的 data 行）
  - `void onStreamLine(String line)` → **子类钩子**，每行回调，用于 Anthropic 追踪 `event:` 行状态（OpenAI 为空实现）
- 具体方法（final）:
  - `ProtocolResponse chat(ProtocolRequest request)` → 构造 HTTP 请求（路径来自 `getEndpointPath()`，头来自 `getHeaders()`，类型来自 `getResponseType()`），发送，解析响应
  - `void chatStream(ProtocolRequest request, StreamCallback callback)` → SSE 事件循环：逐行读取 → `onStreamLine()` → `isStreamComplete()` → 决定是否结束

#### 2.2 OpenAIUpstreamClient 精简

- **文件**: `infrastructure/gateway/upstream/OpenAIUpstreamClient.java`
- 变更: 继承 AbstractUpstreamClient，实现 ~50 行
- `getEndpointPath()` → `/v1/chat/completions`
- `getHeaders(apiKey)` → `{"Authorization": "Bearer {apiKey}", "Content-Type": "application/json"}`
- `getResponseType()` → `OpenAIChatResponse.class`
- `isStreamComplete(line)` → `"[DONE]".equals(line.trim())`
- `onStreamLine(line)` → 空实现（OpenAI 不需要 per-line 状态追踪）

#### 2.3 AnthropicUpstreamClient 精简

- **文件**: `infrastructure/gateway/upstream/AnthropicUpstreamClient.java`
- 变更: 继承 AbstractUpstreamClient，实现 ~60 行
- `getEndpointPath()` → `/v1/messages`
- `getHeaders(apiKey)` → `{"x-api-key": "{apiKey}", "anthropic-version": "2023-06-01", "Content-Type": "application/json"}`
- `getResponseType()` → `AnthropicMessagesResponse.class`
- `isStreamComplete(line)` → `currentEvent == "message_stop"`（状态由 `onStreamLine` 维护）
- `onStreamLine(line)` → 追踪 `event:` 行：若 `line.startsWith("event:")` 则 `currentEvent = line.substring(6).trim()`；data 行不动
- 保留: 类级别字段 `String currentEvent` 用于追踪最近一个 event 类型

#### 2.4 HttpClientConfig 连接池

- **文件**: `infrastructure/config/HttpClientConfig.java`（新增）
- 内容: `@ConfigurationProperties("app.http.client")` 绑定可配置参数；`@Bean OkHttpClient` 带共享 `ConnectionPool(maxIdleConnections, keepAliveMinutes)`
- 默认值: `maxIdleConnections=50`, `keepAliveMinutes=5`（通过 ConfigurationProperties 外部化，不在代码中硬编码）
- 影响: AbstractUpstreamClient 通过构造器注入共用 OkHttpClient 实例

#### 2.5 ProviderException 增强

- **文件**: `infrastructure/exception/ProviderException.java`
- 变更: 增加 `int statusCode` 字段 + `isServerError()` = `statusCode >= 500`

#### 2.6 ResilientUpstreamClient 修复

- **文件**: `infrastructure/resilience/ResilientUpstreamClient.java`
- 变更:
  - 4xx 错误不触发熔断器 `recordFailure()`
  - 流式场景：首次连接失败可重试，连接成功后不重试
  - `@Log` → `@Slf4j`

### 验收标准

| # | 标准 | 验证方式 |
|---|------|---------|
| AC2.1 | OpenAIUpstreamClient、AnthropicUpstreamClient 均继承 AbstractUpstreamClient，实现 4 个抽象方法 + 1 个钩子，原有行为等价 | 全量测试通过 + 行为等价测试 |
| AC2.2 | 不支持的 Provider 尝试创建客户端时抛出明确异常 | 单元测试 |
| AC2.3 | HttpClientConfig 创建的 OkHttpClient 参数可配置（application.yml），连接池默认值=50/5min | 配置断言测试 |
| AC2.4 | ProviderException 含 statusCode，isServerError() 对 500+ 返回 true | 单元测试 |
| AC2.5 | 4xx 错误不写入熔断器状态 | 熔断器调用计数验证 |

---

## Phase 3：Pipeline + 控制器层

### 变更清单

#### 3.1 Stage 接口体系

- **文件**: `application/proxy/pipeline/Stage.java`（新增）
- **设计原则**: 所有状态通过 StageContext 传递，Stage 不直接返回 ProtocolResponse，避免中游阶段（RoutingStage）被迫返回无意义值
- 接口:
```java
public interface Stage {
    int order();
    String name();
    void execute(StageContext context);
}
```

- **文件**: `application/proxy/pipeline/StreamStage.java`（新增）
```java
public interface StreamStage extends Stage {
    void executeStream(StageContext context, StreamCallback callback);
}
```

- **文件**: `application/proxy/pipeline/StageContext.java`（新增）
- 属性:
  - `String traceId` → 全链路追踪 ID
  - `Identity identity` → 调用方身份
  - `RoutingStrategy routingStrategy` → 路由策略
  - `RoutingContext routingContext` → 路由解析结果
  - `ProtocolRequest currentRequest` → 当前阶段处理的请求（各阶段可替换）
  - `ProtocolResponse currentResponse` → 上游调用结果（仅 UpstreamCallStage 后有效）
  - `Protocol inboundProtocol` → 入站协议
  - `Map<String, Object> attributes` → 扩展属性表
  - `CallLog callLog` → 审计日志（Pipeline 创建，AuditStage 完成）
  - `Throwable executionError` → 执行异常（任一阶段抛异常时由 Pipeline 设置，AuditStage 由此判断成功/失败）

#### 3.2 StagePipeline 编排器

- **文件**: `application/proxy/pipeline/StagePipeline.java`（新增）
- 构造器: `StagePipeline(List<Stage> stages, AuditStage auditStage)` — 注入阶段列表，按 order 升序排序，AuditStage 在 finally 中执行
- 方法:
  - `ProtocolResponse execute(ProtocolRequest req, Identity identity, RoutingStrategy strategy)` → 创建 StageContext，按 order 升序执行
  - `void executeStream(...)` → 流式执行
- **错误处理策略**: Pipeline 用 try-catch 包裹全阶段执行，确保**不管哪个阶段抛异常，AuditStage 始终执行**（AuditStage 不参与主阶段链，由 Pipeline 在 finally 中调用）

#### 3.3 七阶段实现

| 阶段 | 类名 | order | 职责 |
|------|------|-------|------|
| 协议识别 | ProtocolIdentifyStage | 100 | getInboundProtocol() + 校验 |
| 路由解析 | RoutingStage | 200 | routingResolver.resolve() → 写入 context |
| 请求转换 | RequestConversionStage | 300 | 跨协议时 convertRequest() |
| 出站调谐 | OutboundTuneStage | 400 | outboundTuner.tune() 返回新实例，写回 context.currentRequest |
| 上游调用 | UpstreamCallStage | 500 | getResilientClient() + client.chat() |
| 响应转换 | ResponseConversionStage | 600 | 跨协议时 convertResponse() |
| 审计 | AuditStage | 700 | CallLog + TokenUsedEvent |

- 每个阶段一个独立文件: `application/proxy/pipeline/stage/*.java`

#### 3.4 ChatDispatchServiceImpl 简化

- **文件**: `application/proxy/ChatDispatchServiceImpl.java`
- 变更: 内部持有 `StagePipeline`，`dispatch()` 和 `dispatchStream()` 委托给管道
- 保留: `ChatDispatchService` 接口签名不变（API 兼容）
- **文件**: `application/proxy/pipeline/config/PipelineConfig.java`（新增）→ `@Bean` 注册所有 Stage 实例到 StagePipeline

#### 3.5 AbstractProtocolController 模板方法

- **文件**: `adapter/api/AbstractProtocolController.java`（新增）
- 流程: `validate(request)` → `resolveIdentity()` → `stream?` → `dispatchStream/dispatch` → `wrapResponse/wrapError`
- 子类只需实现`validate()` + 请求类型泛型参数

#### 3.6 控制器迁移

- **文件**:
  - `adapter/api/OpenAIController.java` → 继承 AbstractProtocolController<OpenAIChatRequest>
  - `adapter/api/AnthropicController.java` → 继承 AbstractProtocolController<AnthropicMessagesRequest>

### 验收标准

| # | 标准 | 验证方式 |
|---|------|---------|
| AC3.1 | ChatDispatchService 接口签名不变，所有消费者不改代码 | 编译通过 |
| AC3.2 | 7 个阶段按 order 顺序执行，可通过日志验证 | 集成测试 + 日志断言 |
| AC3.3 | 流式/非流式两种模式端到端回归 | 完整的模拟 HTTP 请求测试 |
| AC3.4 | 任何阶段抛异常时 Pipeline 仍执行审计日志，异常传递到调用方 | Mock 验证审计日志始终写入 |
| AC3.6 | OpenAIController 和 AnthropicController 均继承 AbstractProtocolController | 编译 + 类型断言 |

---

## Phase 4：横切关注点

### 变更清单

#### 4.1 CachedRoutingResolver

- **文件**: `application/proxy/routing/CachedRoutingResolver.java`（新增）
- 装饰器模式，Caffeine 缓存，maxSize=10_000，expireAfterWrite=60s
- **文件**: `infrastructure/config/CacheConfig.java`（新增）
- 条件: `@ConditionalOnProperty("app.routing.cache.enabled")`

#### 4.2 RoutingCacheInvalidator

- **文件**: `infrastructure/event/RoutingCacheInvalidator.java`（新增）
- `@EventListener` 监听 `ConfigChangedEvent`（覆盖 PROVIDER / MODEL / PROVIDER_API_KEY 变更）→ 清空缓存

#### 4.3 MDC TraceId 传播

- **文件**: `application/proxy/pipeline/StagePipeline.java`
- 变更: `execute()` 入口 MDC.put("traceId", context.getTraceId())，finally MDC.clear()
- 依赖: `logback-spring.xml` 追加 `[%X{traceId}]` 到 pattern

#### 4.4 JSR-305 注解

- 所有 Gateway 接口参数/返回值增加 `@Nonnull` / `@Nullable`

### 验收标准

| # | 标准 | 验证方式 |
|---|------|---------|
| AC4.1 | Caffeine 缓存命中率可通过 Actuator 或日志观测 | 手动验证 |
| AC4.2 | 渠道状态变更事件触发后，缓存命中率归零 | 集成测试 |
| AC4.3 | 所有请求日志中包含 traceId MDC 字段 | 日志格式断言 |
| AC4.4 | `app.routing.cache.enabled=false` 时可关闭缓存 | 配置覆盖测试 |
| AC4.5 | 所有 Gateway 接口已添加 @Nonnull/@Nullable 注解 | 代码审查 |

---

## Phase 5：缺陷修复

### 变更清单

#### 5.1 SSE 客户端断开取消上游

- **文件**: `infrastructure/util/SseStreamHelper.java`
- 修复: 客户端 disconnect 时调用 upstream call 的 `cancel()`

#### 5.2 Token 事件零值兜底

- **文件**: `AuditStage`（Phase 3 已创建）→ 补充零值兜底逻辑
- 修复: 即使 `response.getUsage()` 为 `null`，也要发布 `TokenUsedEvent`（用量为 0）
- **清理**: 删除 `ChatDispatchServiceImpl.publishTokenUsedEvent()` 方法（逻辑已迁移到 AuditStage）

#### 5.3 ProtocolConverter JSON 解析容错

- **文件**: `domain/protocol/conversion/ProtocolConverter.java`
- 修复: JSON 解析失败时 log warn + 返回原始数据透传（而不是 null 导致 NPE）

#### 5.4 ProtocolConverter tool_choice 对象格式

- **文件**: `domain/protocol/conversion/ProtocolConverter.java`
- 修复: tool_choice 支持 `{type: "function", function: {name: "..."}}` 对象格式

#### 5.5 测试用例补充

- 每个 bug fix 对应一个测试用例覆盖
- 新增集成测试：流式客户端断开场景

### 验收标准

| # | 标准 | 验证方式 |
|---|------|---------|
| AC5.1 | 每个缺陷修复有对应的测试用例 | 测试覆盖率审查 |
| AC5.2 | 客户端断开时上游 call.cancel() 被调用 | Mock 验证 |
| AC5.3 | Usage 为 null 时仍然发布 TokenUsedEvent（tokens=0） | 事件发布断言 |
| AC5.4 | ProtocolConverter JSON 解析异常时 log warn + 透传，不抛 NPE | 异常场景测试 |
| AC5.5 | tool_choice 支持对象格式 | 序列化/反序列化测试 |

---

## 整体验收标准汇总

### 功能等价

| # | 标准 | 关联 Phase |
|---|------|-----------|
| G-1 | 全量 430+ 测试通过 | 全部 |
| G-2 | 双协议（OpenAI/Anthropic）流式+非流式端到端回归 | P3、P5 |
| G-3 | 路由解析结果与重构前一致（等价测试） | P1、P4 |

### 行为合规

| # | 标准 | 关联 Phase |
|---|------|-----------|
| G-4 | 所有日志中 API Key 已脱敏 | P1 |
| G-5 | TraceId 全链路 MDC 传播 | P4 |
| G-6 | 4xx 错误不触发熔断 | P2 |
| G-7 | 异常类型符合分层规范（ProviderException vs IllegalArgumentException） | P2 |

### 性能门禁

| # | 标准 | 关联 Phase |
|---|------|-----------|
| G-8 | 重构后 P99 延迟偏差 ≤5%（与当前 main 分支对比） | P3、P4 |
| G-9 | 吞吐量不降（同压测条件下 QPS 偏差 ≤5%） | P3 |
| G-10 | Caffeine 缓存命中后路由耗时 ≤1ms | P4 |

> **性能基线方法**: 在 main 分支上用相同压测脚本（`wrk -t4 -c100 -d30s`）跑 3 轮取均值作为基线。重构分支用相同参数跑 3 轮，计算偏差百分比。压测端点选 `/v1/chat/completions`（OpenAI 协议 + gpt-4o 模型）。

---

## 回滚策略

| 场景 | 动作 |
|------|------|
| Phase 1 编译失败 | 回退该 Phase 变更，检查契约接口兼容性 |
| Phase 3 集成测试失败 | 保留 Phase 1/2，回退 Phase 3；ChatDispatchServiceImpl 回退到直接调用 |
| 性能退化 >5% | 禁用 Caffeine 缓存（配置开关），Profile 瓶颈阶段 |

---

## 附录：当前文件结构映射

```
重构前                                             重构后
ChatDispatchServiceImpl (7阶段硬编码)           → ChatDispatchServiceImpl (委托给 StagePipeline)
                                                   → pipeline/Stage.java
                                                   → pipeline/StreamStage.java
                                                   → pipeline/StageContext.java
                                                   → pipeline/StagePipeline.java
                                                   → pipeline/stage/ProtocolIdentifyStage.java
                                                   → pipeline/stage/RoutingStage.java
                                                   → pipeline/stage/RequestConversionStage.java
                                                   → pipeline/stage/OutboundTuneStage.java
                                                   → pipeline/stage/UpstreamCallStage.java
                                                   → pipeline/stage/ResponseConversionStage.java
                                                   → pipeline/stage/AuditStage.java

OpenAIUpstreamClient (全部逻辑)                 → AbstractUpstreamClient (模板)
                                                   → OpenAIUpstreamClient (~50行)
                                                   → AnthropicUpstreamClient (~60行)

OpenAIController / AnthropicController (重复)   → AbstractProtocolController (模板)
                                                   → OpenAIController (精简)
                                                   → AnthropicController (精简)

RoutingResolver (直接调用)                      → RoutingResolver (保持)
                                                   → CachedRoutingResolver (装饰器)
                                                   → RoutingCacheInvalidator (事件监听)
```

### AbstractUpstreamClient 模板流程

```
chatStream(request, callback) {
    httpCall = httpClient.newCall(buildRequest(request));
    response = httpCall.execute();
    // SSE 事件循环
    response.body().lines().forEach(line -> {
        onStreamLine(line);             // 子类钩子：Anthropic 追踪 event: 状态
        if (isStreamComplete(line)) {   // 子类策略：OpenAI=[DONE], Anthropic=message_stop
            callback.onComplete();
            return;
        }
        callback.onChunk(line);         // 透传 data: 行
    });
}
```

> `onStreamLine` 在 `isStreamComplete` 之前调用，确保 Anthropic 的 `event: message_stop` 在判断 data 行前已被记录。