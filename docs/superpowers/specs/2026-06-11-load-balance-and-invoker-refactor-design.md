---
comet_change: load-balance-and-invoker-refactor
role: technical-design
canonical_spec: openspec
---

# 负载均衡与 Invoker 链重构 — 技术设计

## 动机

当前 InstanceSelector 仅按 priority 取第一个 ModelInstance，weight 字段已定义但未使用；ChatDispatchServiceImpl 中 Key 故障转移和降级编排散布在主流程代码中，流式调用缺少 Key 故障转移能力。参照 Dubbo 的 LoadBalance 和 Cluster 分层思想，引入负载均衡和层次化 Invoker 编排。

## 架构变化

```
当前：
ChatDispatchServiceImpl
  ├── RoutingResolver.resolve()          → 取第一个实例
  ├── callWithKeyFailover()              → 手写 Key 遍历
  └── catch { degradationService.degrade() } → 降级

优化后：
ChatDispatchServiceImpl                  ← 仅编排，不含具体容错逻辑
  └── DegradationInvoker                 ← 第 3 层：模型降级
       └── KeyFailoverInvoker            ← 第 2 层：Key 故障转移
            └── LoadBalanceRouter        ← 第 1 步：实例路由 + 选择
                 └── RouterChain
                      ├── PermissionRouter    (团队权限)
                      ├── PriorityRouter      (priority 分组)
                      ├── HealthRouter        (熔断过滤)
                      ├── TagRouter           (预留：灰度)
                      ├── ConditionRouter     (预留：条件路由)
                      └── LoadBalanceRouter   (链终结者：选一个)
                           └── LoadBalance (策略注入)
                                └── EndpointMetrics + CircuitBreaker → 健康感知
```

## 数据流

```
请求 → ChatDispatchService
  → DegradationInvoker.invoke()
    → try { KeyFailoverInvoker.invoke() }
      → for each Key:
          → 跳过熔断中的端点（CircuitBreaker.allowRequest()）
          → RouterChain.filter(allInstances, request)
            → PermissionRouter.filter()    ← 团队权限
            → PriorityRouter.filter()      ← 取最小 priority 组
            → HealthRouter.filter()        ← 跳过熔断中的端点
            → LoadBalanceRouter.filter()   ← LoadBalance.select() 选一个
          → ResilientUpstreamClient.chat()
            → CircuitBreaker.allowRequest()
            → RetryExecutor.execute()
            → delegate.chat()
            → EndpointMetrics.endCall()
      → catch ProviderException → 切下一个 Key
    → catch ProviderException → degradationService.degrade() → 重新路由 → 重试
```

## 设计决策

| 决策 | 结论 | 理由 |
|------|------|------|
| LoadBalance 参数 | `select(List<ModelInstance>)` | 外部依赖由 Spring 注入具体实现，不需参数传递 |
| Priority 组全熔断 | **抛异常** | 熔断就是熔断，不自动降级优先级组 |
| 流式 Key 故障转移 | **补齐 Key 遍历 + 熔断跳过** | 传输开始后不切换 Key |
| 扩展机制 | Spring 策略注入 | `@Component` + `Map<String, LoadBalance>` 注入 |
| Router 链 | Spring `@Order` + `List<Router>` 责任链 | InstanceSelector 内部 4 步过滤提取为可插拔 Router |

## 分层归属

```
application/proxy/
├── ChatDispatchServiceImpl.java     ← 主编排
├── routing/
│   ├── Router.java                  ← 接口（新增）
│   ├── RouterChain.java             ← 责任链编排（新增）
│   ├── PermissionRouter.java        ← 团队权限过滤（新增）
│   ├── PriorityRouter.java          ← priority 分组（新增）
│   ├── HealthRouter.java            ← 熔断过滤（新增）
│   ├── LoadBalanceRouter.java       ← 负载均衡终结者（新增）
│   ├── LoadBalance.java             ← 接口（新增）
│   ├── WeightedRandomLoadBalance.java ← 加权随机（新增）
│   ├── RoundRobinLoadBalance.java   ← 加权轮询（新增）
│   ├── LeastActiveLoadBalance.java  ← 最少活跃（新增）
│   ├── InstanceSelector.java        ← 简化：委托给 RouterChain
│   └── RoutingResolver.java         ← 不变
└── invoker/                         ← 新增包
    ├── KeyFailoverInvoker.java      ← Key 级故障转移
    └── DegradationInvoker.java      ← 模型降级包装

infrastructure/resilience/
├── EndpointMetrics.java             ← 新增：端点级调用统计
├── EndpointMetricsRegistry.java     ← 新增：按 endpointId 索引
├── CircuitBreaker.java              ← 不变
├── RetryExecutor.java               ← 不变
└── ResilientUpstreamClient.java     ← 增加 EndpointMetrics 埋点
```

## 组件设计

### 1. LoadBalance 接口

```java
@FunctionalInterface
public interface LoadBalance {
    /** 从候选实例列表中选一个 */
    ModelInstance select(List<ModelInstance> instances);
}
```

### 2. WeightedRandomLoadBalance

参照 Dubbo RandomLoadBalance：
- 所有权重相同 → `ThreadLocalRandom.current().nextInt(n)`
- 权重不同 → 前缀和数组 + `nextInt(totalWeight)` 二分查找
- 仅依赖 `ModelInstance.weight`

### 3. RoundRobinLoadBalance

参照 Dubbo 平滑加权轮询：
- 每个实例维护 `current` 值，初始化 0
- 选择：`current += weight`，选 `current` 最大的
- 选中：`current -= totalWeight`
- 60 秒未更新的条目自动回收

### 4. LeastActiveLoadBalance

参照 Dubbo LeastActiveLoadBalance：
- 通过 `EndpointMetricsRegistry.get(endpointId).getActive()` 获取活跃数
- 选活跃数最少的实例
- 同活跃度内按 `weight` 加权随机

### 5. EndpointMetrics

参照 Dubbo RpcStatus：

```java
public class EndpointMetrics {
    private final AtomicInteger active = new AtomicInteger();
    private final AtomicLong totalCalls = new AtomicLong();
    private final AtomicLong totalDuration = new AtomicLong();
    private final AtomicLong failedCalls = new AtomicLong();
}
```

在 `ResilientUpstreamClient` 中埋点：`beginCall()` / `endCall(duration, success)`。

### 6. Router 接口 + RouterChain

Router 接口：

```java
@FunctionalInterface
public interface Router {
    /** 过滤候选实例列表，返回符合条件的子集 */
    List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request);

    /** 路由结果为空时是否强制执行（不降级到全量列表） */
    default boolean isForce() { return false; }
}
```

RouterChain 编排：

```java
@Component
public class RouterChain {
    private final List<Router> routers;

    public RouterChain(List<Router> routers) {
        this.routers = routers.stream()
                .sorted(Comparator.comparingInt(r -> r.getClass().getAnnotation(Order.class)?.value() ?? 0))
                .toList();
    }

    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        List<ModelInstance> candidates = instances;
        for (Router router : routers) {
            List<ModelInstance> filtered = router.filter(candidates, request);
            if (filtered.isEmpty()) {
                if (router.isForce()) return List.of();
                continue;  // 非强制路由，忽略该步
            }
            candidates = filtered;
        }
        return candidates;
    }
}
```

Router 实现一览：

| Router | @Order | isForce | 本次实现 | 说明 |
|--------|--------|---------|---------|------|
| `PermissionRouter` | 100 | true | ✅ | 按团队权限过滤 |
| `PriorityRouter` | 200 | true | ✅ | 按 priority 分组，只保留最小 priority 组 |
| `HealthRouter` | 300 | true | ✅ | 过滤熔断中的端点 |
| `TagRouter` | 400 | false | ❌ 预留 | 灰度/金丝雀标签路由 |
| `ConditionRouter` | 500 | false | ❌ 预留 | 按请求参数条件路由 |
| `ScriptRouter` | 600 | false | ❌ 预留 | 脚本路由（安全风险高） |
| `LoadBalanceRouter` | 9999 | true | ✅ | 链终结者，内部调用 LoadBalance.select() |

LoadBalanceRouter 返回单元素列表（或空列表），作为责任链的终结者。

### 7. InstanceSelector 重构

```
当前：select(modelId, userId, role) → getFirst()
      内部硬编码：权限过滤 → priority 取第一个

优化：select(modelId, userId, role, strategy) →
      委托给 RouterChain.filter(allInstances, request)
      → PermissionRouter    (团队权限)
      → PriorityRouter      (priority 分组)
      → HealthRouter        (熔断过滤)
      → LoadBalanceRouter   (负载均衡选一个)
```

### 8. KeyFailoverInvoker

从 `ChatDispatchServiceImpl.callWithKeyFailover()` 提取：
- 遍历 `credentialResolver.resolveAll(channelId)`
- 跳过 `circuitBreakerManager.isAvailable()` 为 false 的端点
- 失败切下一个 Key
- 全部失败 → `throw ProviderException`
- 流式：补齐 `invokeStream()`，调用前遍历 Key 检查熔断，传输开始后不切换

### 9. DegradationInvoker

从 `ChatDispatchServiceImpl.dispatch()` 的 catch 块提取：
- 包装 KeyFailoverInvoker
- 捕获 ProviderException → `degradationService.degrade()`
- 降级后重新路由 → 递归调用自身

## 实施计划

| 任务 | 改动量 | 依赖 |
|------|--------|------|
| T1: Router 接口 + RouterChain | 小 | 无 |
| T2: PermissionRouter | 小 | T1 |
| T3: PriorityRouter | 小 | T1 |
| T4: HealthRouter | 小 | T1 |
| T5: LoadBalanceRouter | 小 | T1, T10 |
| T6: LoadBalance 接口 + 抽象基类 | 小 | 无 |
| T7: WeightedRandomLoadBalance | 小 | T6 |
| T8: EndpointMetrics + Registry | 中 | 无 |
| T9: ResilientUpstreamClient 埋点 | 小 | T8 |
| T10: InstanceSelector 简化 | 小 | T1 |
| T11: KeyFailoverInvoker | 中 | T10 |
| T12: DegradationInvoker | 小 | T11 |
| T13: ChatDispatchServiceImpl 简化 | 小 | T11, T12 |
| T14: RoundRobinLoadBalance | 小 | T6 |
| T15: LeastActiveLoadBalance | 小 | T6, T8 |
| T16-T20: 测试 | 中 | T1-T15 |

## 测试策略

- **加权随机**：固定种子 + 大量样本验证权重分布接近配置比例
- **加权轮询**：验证 N 次选择后各实例被选次数比例接近权重比
- **最少活跃**：模拟不同活跃数，验证选活跃最少的
- **InstanceSelector**：多 priority 分组、熔断端点跳过、负载均衡集成
- **KeyFailoverInvoker**：Key 遍历、熔断跳过、全部失败、流式
- **DegradationInvoker**：降级触发、降级恢复、降级耗尽
- **ChatDispatchServiceImpl**：回归测试，确保七阶段调度不变
