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
    private final AtomicLong failedCalls;        // 失败次数
}
```

在 `ResilientUpstreamClient` 中埋点（请求开始 `beginCall()`，请求结束 `endCall()`）。

`EndpointMetricsRegistry` 管理所有端点的统计实例，按 `endpointId` 索引。

### 3. InstanceSelector 重构

```
当前：select(Long modelId, Long userId, String role) → ModelInstance
      逻辑：权限过滤 → 按 priority 取第一个

优化后：select(Long modelId, Long userId, String role, String strategy) → ModelInstance
        逻辑：权限过滤 → 按 priority 分组
              → 取 priority 最小的组
              → 过滤熔断中的端点
              → LoadBalance.select(candidates)
```

### 4. KeyFailoverInvoker

从 `ChatDispatchServiceImpl.callWithKeyFailover()` 提取为独立组件：

```java
@Component
public class KeyFailoverInvoker {
    public ProtocolResponse invoke(RoutingContext ctx, ProtocolRequest request) {
        // 遍历 credentialResolver.resolveAll(channelId)
        // 跳过熔断中的端点
        // 失败切下一个 Key
        // 全部失败 → throw ProviderException
    }
}
```

流式也复用：`invokeStream(ctx, request, callback)`。

### 5. DegradationInvoker

```java
@Component
public class DegradationInvoker {
    public ProtocolResponse invoke(RoutingContext ctx, ProtocolRequest request) {
        try {
            return keyFailoverInvoker.invoke(ctx, request);
        } catch (ProviderException e) {
            String fallback = degradationService.degrade(request.getModel(), e.getErrorType());
            if (fallback != null) {
                request.setModel(fallback);
                // 重新路由 + 重试
                RoutingContext newCtx = routingResolver.resolve(...);
                return invoke(newCtx, request);
            }
            throw e;
        }
    }
}
```

### 6. ChatDispatchServiceImpl 简化

```java
@Override
public ProtocolResponse dispatch(ProtocolRequest request, Identity identity, RoutingStrategy strategy) {
    // 阶段 2：路由
    RoutingContext ctx = routingResolver.resolve(...);
    // 阶段 3：协议转换
    // 阶段 4：出站调谐
    // 阶段 5：Invoker 链调用（熔断 + 重试 + Key 故障转移 + 降级）
    ProtocolResponse response = degradationInvoker.invoke(ctx, outboundReq);
    // 阶段 6：响应转换
    // 阶段 7：后置处理
}
```

## 实施优先级

| 顺序 | 任务 | 依赖 | 改动量 |
|------|------|------|--------|
| 1 | 创建 LoadBalance 接口 + WeightedRandomLoadBalance | 无 | 小 |
| 2 | 重构 InstanceSelector（按 priority 分组 + 健康感知 + 负载均衡） | 任务 1 | 中 |
| 3 | 创建 EndpointMetrics + EndpointMetricsRegistry + 埋点 | 无 | 中 |
| 4 | 创建 KeyFailoverInvoker（提取自 callWithKeyFailover） | 任务 2 | 中 |
| 5 | 创建 DegradationInvoker（提取自 dispatch catch 块） | 任务 4 | 小 |
| 6 | ChatDispatchServiceImpl 简化 | 任务 5 | 小 |
| 7 | 流式 Key 故障转移对齐 | 任务 4 | 小 |
| 8 | RoundRobinLoadBalance 补充 | 任务 1 | 小 |
| 9 | LeastActiveLoadBalance 补充 | 任务 3 | 小 |
