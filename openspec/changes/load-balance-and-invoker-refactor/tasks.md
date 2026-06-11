# 任务清单

## RouterChain

- [x] **T1: 创建 Router 接口 + RouterChain 编排**
  - `Router` 接口：`filter(List<ModelInstance>, RoutingRequest) → List<ModelInstance>`
  - `RouterChain`：Spring 注入 `List<Router>`，按 `@Order` 排序，责任链执行
  - 非强制 Router 过滤结果为空时跳过，强制 Router 为空时直接返回空
- [x] **T2: 创建 PermissionRouter**
  - 从 `InstanceSelector` 提取团队权限过滤逻辑
  - `@Order(100)`，`isForce() = true`
  - ADMIN 角色跳过团队过滤
- [x] **T3: 创建 PriorityRouter**
  - 从 `InstanceSelector` 提取 priority 分组逻辑
  - `@Order(200)`，`isForce() = true`
  - 按 priority 分组，只保留 priority 最小的组
- [x] **T4: 创建 HealthRouter**
  - `@Order(300)`，`isForce() = true`
  - 过滤 `ChannelEndpointCircuitBreakerManager.isAvailable()` 为 false 的端点
- [x] **T5: 创建 LoadBalanceRouter**
  - `@Order(9999)`，`isForce() = true`，链终结者
  - 内部调用 `LoadBalance.select()`，将返回的单个实例包装为单元素列表

## 负载均衡

- [x] **T6: 创建 LoadBalance 接口 + 抽象基类**
  - `LoadBalance` 接口：`select(List<ModelInstance>) → ModelInstance`
  - `AbstractLoadBalance` 抽象基类：空检查 + 单元素短路
- [x] **T7: 实现 WeightedRandomLoadBalance**
  - 参照 Dubbo RandomLoadBalance 算法
  - 利用 `ModelInstance.weight` 字段
  - 所有权重相同时直接 `nextInt(n)`
- [x] **T14: 实现 RoundRobinLoadBalance**
  - 参照 Dubbo 平滑加权轮询算法
  - 每个实例维护 current 值，选 current 最大的，选中后减去 totalWeight
- [x] **T15: 实现 LeastActiveLoadBalance**
  - 依赖 EndpointMetrics.getActive()
  - 选活跃数最少的实例，同活跃度内加权随机

## 统计基础设施

- [x] **T8: 创建 EndpointMetrics + EndpointMetricsRegistry**
  - `EndpointMetrics`：active / totalCalls / totalDuration / failedCalls
  - `EndpointMetricsRegistry`：按 endpointId 索引
  - `beginCall()` / `endCall(duration, success)` 方法
- [x] **T9: ResilientUpstreamClient 增加 EndpointMetrics 埋点**
  - chat() 入口 `beginCall()`，出口 `endCall()`
  - chatStream() 入口 `beginCall()`，onComplete/onError `endCall()`

## InstanceSelector 简化

- [x] **T10: InstanceSelector 简化 — 委托给 RouterChain**
  - 当前内部硬编码的权限过滤 + priority 排序 + 取第一个 → 委托给 `RouterChain.filter()`
  - 方法签名增加 `strategy` 参数，传递给 LoadBalanceRouter

## Invoker 链

- [x] **T11: 创建 KeyFailoverInvoker**
  - 从 `ChatDispatchServiceImpl.callWithKeyFailover()` 提取
  - 遍历 `credentialResolver.resolveAll()`，跳过熔断中的端点
  - 失败切下一个 Key，全部失败抛 ProviderException
  - 新增 `invokeStream()` 方法补齐流式 Key 故障转移
- [x] **T12: 创建 DegradationInvoker**
  - 从 `ChatDispatchServiceImpl.dispatch()` 的 catch 块提取
  - 包装 KeyFailoverInvoker，捕获 ProviderException 后走降级
  - 降级后重新路由 + 递归调用

## ChatDispatchServiceImpl 简化

- [x] **T13: ChatDispatchServiceImpl 集成 Invoker 链**
  - dispatch() 中 Key 遍历和降级替换为 DegradationInvoker
  - dispatchStream() 中流式调用替换为 KeyFailoverInvoker.invokeStream()
  - 清理不再需要的直接依赖（credentialResolver、circuitBreakerManager 等）

## 测试

- [x] **T16: 测试 RouterChain**
  - Router 排序验证、非强制路由跳过、强制路由返回空
  - PermissionRouter 权限过滤
  - PriorityRouter priority 分组
  - HealthRouter 熔断端点跳过
  - LoadBalanceRouter 负载均衡集成
- [x] **T17: 测试 LoadBalance 实现**
  - WeightedRandomLoadBalance 单元测试（权重分布验证）
  - RoundRobinLoadBalance 单元测试（平滑性验证）
  - LeastActiveLoadBalance 单元测试
- [x] **T18: 测试 Invoker 链**
  - KeyFailoverInvoker 单元测试（Key 遍历、熔断跳过、全部失败）
  - DegradationInvoker 单元测试（降级触发、降级恢复、降级耗尽）
  - 流式 Key 故障转移测试
- [x] **T19: 回归测试**
  - ChatDispatchServiceImpl 集成测试
  - 现有 Resilience 测试通过
