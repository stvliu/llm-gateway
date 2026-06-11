# Comet Design Handoff

- Change: load-balance-and-invoker-refactor
- Phase: design
- Mode: compact
- Context hash: 6c8e205617b19a51d503a0c2efe17619d563a610b50f853cec6dc12aa980610e

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/load-balance-and-invoker-refactor/proposal.md

- Source: openspec/changes/load-balance-and-invoker-refactor/proposal.md
- Lines: 1-30
- SHA256: 4b243cd162b65120c5b60db9b4ba6766a6d4df0f7037a05a74c6d368b2c36f97

```md
## Why

当前 InstanceSelector 仅按 priority 取第一个 ModelInstance，weight 字段已定义但未使用；ChatDispatchServiceImpl 中 Key 故障转移和降级编排散布在主流程代码中，流式调用缺少 Key 故障转移能力。参照 Dubbo 的 LoadBalance 和 Cluster 分层设计，引入负载均衡和层次化 Invoker 编排，提升网关的流量调度能力和代码可维护性。

## What Changes

- **LoadBalance 接口 + 实现**：参照 Dubbo 的 LoadBalance 设计，新增接口和加权随机/轮询实现，InstanceSelector 按 priority 分组后组内走负载均衡
- **EndpointMetrics 统计基础设施**：为每个端点维护 active/total/duration/failed 统计，为 LeastActive 等策略提供数据基础
- **健康感知路由**：InstanceSelector 跳过 CircuitBreaker OPEN 状态的端点
- **Invoker 分层重构**：从 ChatDispatchServiceImpl 提取 KeyFailoverInvoker（Key 级故障转移）和 DegradationInvoker（模型降级），形成层次化调用链
- **流式 Key 故障转移**：补齐流式调用的 Key 遍历能力，与非流式对齐

## Capabilities

### New Capabilities
- `load-balance`: 负载均衡策略（加权随机、加权轮询、最少活跃优先），通过 LoadBalance 接口可扩展
- `endpoint-metrics`: 端点级调用统计（活跃数、耗时、成功率），供负载均衡和可观测性使用
- `invoker-chain`: 层次化 Invoker 调用链（DegradationInvoker → KeyFailoverInvoker → ResilientUpstreamClient）

### Modified Capabilities
- `<empty>`: 无 spec 级需求变更

## Impact

- `application/proxy/routing/InstanceSelector.java`：重构 select 方法，引入 LoadBalance
- `application/proxy/routing/RoutingResolver.java`：传递 strategy 参数
- `application/proxy/ChatDispatchServiceImpl.java`：提取 KeyFailover 和 Degradation 到独立 Invoker
- `infrastructure/resilience/`：新增 EndpointMetrics 和 EndpointMetricsRegistry
- `infrastructure/resilience/ResilientUpstreamClient.java`：增加 EndpointMetrics 埋点
- `application/proxy/invoker/`：新增包，放置 KeyFailoverInvoker + DegradationInvoker
```

## openspec/changes/load-balance-and-invoker-refactor/design.md

- Source: openspec/changes/load-balance-and-invoker-refactor/design.md
- Lines: 1-170
- SHA256: da3fb8afc09d98bb44764d374d936c7377ad928493be7924ebdc78f4aa1b2ee8

[TRUNCATED]

```md
## 高层架构决策

### 方案选型

| 决策 | 选型 | 理由 |
|------|------|------|
| 扩展机制 | **Spring 策略注入**（非 SPI） | Spring Boot 的 `@Component` + `Map<String, 接口>` 注入已足够 |
| 负载均衡 | **自建 LoadBalance 接口** | 参照 Dubbo，但接口更精简，只依赖 `weight` 和 `EndpointMetrics` |
| 容错编排 | **Invoker 链**（非 Cluster 抽象） | 不引入 Cluster/Invoker/Directory 等概念，改用清晰的层次化 Invoker |
| 注册中心 | **不需要** | 上游是外部 LLM API，无注册概念 |
| 配置中心 | **不需要** | Caffeine + DB TTL（1 分钟）对管理后台配置已足够 |
| Router 链 | **暂不引入** | 当前 RoutingResolver 编排已足够，待条件路由需求出现时再抽象 |

### 架构变化

```
当前：
ChatDispatchServiceImpl
  ├── RoutingResolver.resolve()          → 取第一个实例
  ├── callWithKeyFailover()              → 手写 Key 遍历
  └── catch { degradationService.degrade() } → 降级

优化后：
ChatDispatchServiceImpl
  └── DegradationInvoker                 → 模型降级
       └── KeyFailoverInvoker            → Key 故障转移
            └── LoadBalance              → 选实例
                 └── InstanceSelector    → 权限过滤 + 优先级分组
                      └── EndpointMetrics + CircuitBreaker → 健康感知
```

## 数据流

```
请求 → ChatDispatchService
  → DegradationInvoker.invoke()
    → try { KeyFailoverInvoker.invoke() }
      → for each Key:
          → LoadBalance.select(instances)  ← 跳过熔断中的
            → InstanceSelector.select()
              → 按 priority 分组
              → 组内 LoadBalance 策略选择
          → ResilientUpstreamClient.chat()
            → CircuitBreaker.allowRequest()
            → RetryExecutor.execute()
            → delegate.chat()              ← 实际 HTTP 调用
            → EndpointMetrics.endCall()    ← 统计埋点
    → catch ProviderException → 切下一个 Key
  → catch ProviderException → degradationService.degrade()
```

## 核心组件设计

### 1. LoadBalance 接口 + 实现

参照 Dubbo 的极简接口设计：

```java
@FunctionalInterface
public interface LoadBalance {
    ModelInstance select(List<ModelInstance> instances, LoadBalanceContext context);
}
```

实现（第一阶段）：
- `WeightedRandomLoadBalance` — 直接照搬 Dubbo RandomLoadBalance 算法，利用 `ModelInstance.weight`
- `RoundRobinLoadBalance` — 参照 Dubbo 平滑加权轮询

第二阶段补充：
- `LeastActiveLoadBalance` — 依赖 EndpointMetrics，选活跃数最少的

### 2. EndpointMetrics

参照 Dubbo `RpcStatus` 的设计：

```java
public class EndpointMetrics {
    private final AtomicInteger active;          // 进行中请求数
    private final AtomicLong totalCalls;         // 总调用次数
    private final AtomicLong totalDuration;      // 总耗时
```

Full source: openspec/changes/load-balance-and-invoker-refactor/design.md

## openspec/changes/load-balance-and-invoker-refactor/tasks.md

- Source: openspec/changes/load-balance-and-invoker-refactor/tasks.md
- Lines: 1-93
- SHA256: 4690b35b40d586e0f4823150995f0d93352704959ae8e302f370aae55762a03b

[TRUNCATED]

```md
# 任务清单

## RouterChain

- [ ] **T1: 创建 Router 接口 + RouterChain 编排**
  - `Router` 接口：`filter(List<ModelInstance>, RoutingRequest) → List<ModelInstance>`
  - `RouterChain`：Spring 注入 `List<Router>`，按 `@Order` 排序，责任链执行
  - 非强制 Router 过滤结果为空时跳过，强制 Router 为空时直接返回空
- [ ] **T2: 创建 PermissionRouter**
  - 从 `InstanceSelector` 提取团队权限过滤逻辑
  - `@Order(100)`，`isForce() = true`
  - ADMIN 角色跳过团队过滤
- [ ] **T3: 创建 PriorityRouter**
  - 从 `InstanceSelector` 提取 priority 分组逻辑
  - `@Order(200)`，`isForce() = true`
  - 按 priority 分组，只保留 priority 最小的组
- [ ] **T4: 创建 HealthRouter**
  - `@Order(300)`，`isForce() = true`
  - 过滤 `ChannelEndpointCircuitBreakerManager.isAvailable()` 为 false 的端点
- [ ] **T5: 创建 LoadBalanceRouter**
  - `@Order(9999)`，`isForce() = true`，链终结者
  - 内部调用 `LoadBalance.select()`，将返回的单个实例包装为单元素列表

## 负载均衡

- [ ] **T6: 创建 LoadBalance 接口 + 抽象基类**
  - `LoadBalance` 接口：`select(List<ModelInstance>) → ModelInstance`
  - `AbstractLoadBalance` 抽象基类：空检查 + 单元素短路
- [ ] **T7: 实现 WeightedRandomLoadBalance**
  - 参照 Dubbo RandomLoadBalance 算法
  - 利用 `ModelInstance.weight` 字段
  - 所有权重相同时直接 `nextInt(n)`
- [ ] **T14: 实现 RoundRobinLoadBalance**
  - 参照 Dubbo 平滑加权轮询算法
  - 每个实例维护 current 值，选 current 最大的，选中后减去 totalWeight
- [ ] **T15: 实现 LeastActiveLoadBalance**
  - 依赖 EndpointMetrics.getActive()
  - 选活跃数最少的实例，同活跃度内加权随机

## 统计基础设施

- [ ] **T8: 创建 EndpointMetrics + EndpointMetricsRegistry**
  - `EndpointMetrics`：active / totalCalls / totalDuration / failedCalls
  - `EndpointMetricsRegistry`：按 endpointId 索引
  - `beginCall()` / `endCall(duration, success)` 方法
- [ ] **T9: ResilientUpstreamClient 增加 EndpointMetrics 埋点**
  - chat() 入口 `beginCall()`，出口 `endCall()`
  - chatStream() 入口 `beginCall()`，onComplete/onError `endCall()`

## InstanceSelector 简化

- [ ] **T10: InstanceSelector 简化 — 委托给 RouterChain**
  - 当前内部硬编码的权限过滤 + priority 排序 + 取第一个 → 委托给 `RouterChain.filter()`
  - 方法签名增加 `strategy` 参数，传递给 LoadBalanceRouter

## Invoker 链

- [ ] **T11: 创建 KeyFailoverInvoker**
  - 从 `ChatDispatchServiceImpl.callWithKeyFailover()` 提取
  - 遍历 `credentialResolver.resolveAll()`，跳过熔断中的端点
  - 失败切下一个 Key，全部失败抛 ProviderException
  - 新增 `invokeStream()` 方法补齐流式 Key 故障转移
- [ ] **T12: 创建 DegradationInvoker**
  - 从 `ChatDispatchServiceImpl.dispatch()` 的 catch 块提取
  - 包装 KeyFailoverInvoker，捕获 ProviderException 后走降级
  - 降级后重新路由 + 递归调用

## ChatDispatchServiceImpl 简化

- [ ] **T13: ChatDispatchServiceImpl 集成 Invoker 链**
  - dispatch() 中 Key 遍历和降级替换为 DegradationInvoker
  - dispatchStream() 中流式调用替换为 KeyFailoverInvoker.invokeStream()
  - 清理不再需要的直接依赖（credentialResolver、circuitBreakerManager 等）

## 测试

- [ ] **T16: 测试 RouterChain**
  - Router 排序验证、非强制路由跳过、强制路由返回空
  - PermissionRouter 权限过滤
  - PriorityRouter priority 分组
```

Full source: openspec/changes/load-balance-and-invoker-refactor/tasks.md

