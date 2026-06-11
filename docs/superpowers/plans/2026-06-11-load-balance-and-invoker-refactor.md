---
change: load-balance-and-invoker-refactor
design-doc: docs/superpowers/specs/2026-06-11-load-balance-and-invoker-refactor-design.md
base-ref: 3ebffc1917a455cf45f0b7999ab847c641c90230
---

# 负载均衡与 Invoker 链重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 InstanceSelector 的硬编码过滤逻辑提取为可插拔的 RouterChain，引入 LoadBalance 接口及三种实现（加权随机、加权轮询、最少活跃），提取 KeyFailoverInvoker 和 DegradationInvoker 简化 ChatDispatchServiceImpl。

**Architecture:** RouterChain 使用 Spring `@Order` + `List<Router>` 责任链模式，按顺序执行 PermissionRouter → PriorityRouter → HealthRouter → LoadBalanceRouter；LoadBalance 接口参照 Dubbo 设计；Invoker 链形成 DegradationInvoker → KeyFailoverInvoker → ResilientUpstreamClient 的层次化调用。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JUnit 5 + Mockito + AssertJ

---

## 文件结构总览

### 新增文件
| 文件 | 所属任务 |
|------|---------|
| `application/proxy/routing/Router.java` | T1 |
| `application/proxy/routing/RouterChain.java` | T1 |
| `application/proxy/routing/RoutingRequest.java` | T1 |
| `application/proxy/routing/PermissionRouter.java` | T2 |
| `application/proxy/routing/PriorityRouter.java` | T3 |
| `application/proxy/routing/HealthRouter.java` | T4 |
| `application/proxy/routing/LoadBalanceRouter.java` | T5 |
| `application/proxy/routing/LoadBalance.java` | T6 |
| `application/proxy/routing/AbstractLoadBalance.java` | T6 |
| `application/proxy/routing/WeightedRandomLoadBalance.java` | T7 |
| `application/proxy/routing/RoundRobinLoadBalance.java` | T14 |
| `application/proxy/routing/LeastActiveLoadBalance.java` | T15 |
| `infrastructure/resilience/EndpointMetrics.java` | T8 |
| `infrastructure/resilience/EndpointMetricsRegistry.java` | T8 |
| `application/proxy/invoker/KeyFailoverInvoker.java` | T11 |
| `application/proxy/invoker/DegradationInvoker.java` | T12 |
| `application/proxy/routing/RoundRobinLoadBalance.java` | T14 |
| 测试文件 11 个 | T16-T19 |

### 修改文件
| 文件 | 改动 |
|------|------|
| `infrastructure/resilience/ResilientUpstreamClient.java` | 增加 EndpointMetrics 埋点（T9） |
| `application/proxy/routing/InstanceSelector.java` | 委托给 RouterChain（T10） |
| `application/proxy/ChatDispatchServiceImpl.java` | 集成 Invoker 链，简化（T13） |
| `application/proxy/routing/RoutingResolver.java` | 传递 strategy 参数（T10） |

---

## Task 1: 创建 Router 接口 + RoutingRequest + RouterChain

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/Router.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RoutingRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RouterChain.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/RouterChainTest.java`

- [ ] **Step 1: 创建 Router 接口**

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;

import java.util.List;

/**
 * 路由器接口 — 对候选实例列表执行过滤，返回符合条件的子集
 */
@FunctionalInterface
public interface Router {

    /**
     * 过滤候选实例列表
     *
     * @param instances 候选实例列表
     * @param request   路由请求上下文
     * @return 符合条件的实例子集
     */
    List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request);

    /**
     * 路由结果为空时是否强制执行
     *
     * <p>强制 Router 返回空列表时，RouterChain 直接返回空；
     * 非强制 Router 返回空列表时，RouterChain 跳过该步骤，使用上一步的候选列表继续。</p>
     */
    default boolean isForce() { return false; }
}
```

- [ ] **Step 2: 创建 RoutingRequest**

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.enums.RoutingStrategy;

/**
 * 路由请求上下文 — 携带 RouterChain 各环节所需的信息
 */
public class RoutingRequest {

    private final Long modelId;
    private final Long userId;
    private final String role;
    private final RoutingStrategy strategy;

    public RoutingRequest(Long modelId, Long userId, String role) {
        this(modelId, userId, role, RoutingStrategy.WEIGHTED);
    }

    public RoutingRequest(Long modelId, Long userId, String role, RoutingStrategy strategy) {
        this.modelId = modelId;
        this.userId = userId;
        this.role = role;
        this.strategy = strategy;
    }

    public Long getModelId() { return modelId; }
    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public RoutingStrategy getStrategy() { return strategy; }
}
```

- [ ] **Step 3: 创建 RouterChain**

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 路由器责任链 — 按 @Order 排序依次执行 Router
 *
 * <p>非强制 Router 过滤结果为空时跳过，强制 Router 为空时直接返回空列表。</p>
 */
@Component
public class RouterChain {

    private static final Logger log = LoggerFactory.getLogger(RouterChain.class);

    private final List<Router> routers;

    public RouterChain(List<Router> routers) {
        this.routers = routers.stream()
                .sorted(Comparator.comparingInt(
                        r -> {
                            Order order = r.getClass().getAnnotation(Order.class);
                            return order != null ? order.value() : Integer.MAX_VALUE;
                        }))
                .toList();
        log.info("RouterChain initialized with {} routers: {}", this.routers.size(),
                this.routers.stream().map(r -> r.getClass().getSimpleName()).toList());
    }

    /**
     * 执行路由链过滤
     *
     * @param instances 原始候选实例列表
     * @param request   路由请求上下文
     * @return 最终过滤后的实例列表
     */
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        List<ModelInstance> candidates = instances;

        for (Router router : routers) {
            List<ModelInstance> filtered = router.filter(candidates, request);
            if (filtered.isEmpty()) {
                if (router.isForce()) {
                    log.debug("Router {} returned empty, chain terminated", router.getClass().getSimpleName());
                    return List.of();
                }
                log.debug("Router {} returned empty, skipping (non-force)", router.getClass().getSimpleName());
                continue;
            }
            log.debug("Router {} filtered {} -> {}", router.getClass().getSimpleName(),
                    candidates.size(), filtered.size());
            candidates = filtered;
        }

        return candidates;
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/Router.java
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RoutingRequest.java
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RouterChain.java
git commit -m "feat(routing): 创建 Router 接口 + RoutingRequest + RouterChain 责任链"
```

---

## Task 2: 创建 PermissionRouter

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/PermissionRouter.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/PermissionRouterTest.java`

- [ ] **Step 1: 创建 PermissionRouter**

从 `InstanceSelector.select()` 提取团队权限过滤逻辑：

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限路由器 — 按用户团队权限过滤模型实例
 *
 * <p>ADMIN 角色跳过团队渠道过滤，可以访问所有活跃渠道。</p>
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class PermissionRouter implements Router {

    private final ChannelGateway channelGateway;
    private final UserTeamGateway userTeamGateway;
    private final TeamChannelGateway teamChannelGateway;

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        // 获取用户有权限的渠道 ID 集合
        Set<Long> permittedChannelIds = getPermittedChannelIds(request);

        if (permittedChannelIds.isEmpty()) {
            return List.of();
        }

        // 过滤：只保留有权限的渠道内的实例
        List<ModelInstance> permitted = instances.stream()
                .filter(mi -> permittedChannelIds.contains(mi.getChannelId()))
                .toList();

        if (permitted.isEmpty()) {
            return List.of();
        }

        // 再过滤活跃 Channel
        List<Long> channelIds = permitted.stream().map(ModelInstance::getChannelId).toList();
        List<Channel> activeChannels = channelGateway.findByIds(channelIds).stream()
                .filter(ch -> ch.getState() == ChannelState.ACTIVE)
                .toList();
        Set<Long> activeChannelIds = activeChannels.stream().map(Channel::getId).collect(Collectors.toSet());

        return permitted.stream()
                .filter(mi -> activeChannelIds.contains(mi.getChannelId()))
                .toList();
    }

    @Override
    public boolean isForce() { return true; }

    private Set<Long> getPermittedChannelIds(RoutingRequest request) {
        if ("ADMIN".equals(request.getRole())) {
            return channelGateway.findAll().stream()
                    .filter(ch -> ch.getState() == ChannelState.ACTIVE)
                    .map(Channel::getId)
                    .collect(Collectors.toSet());
        }

        Long teamId = userTeamGateway.findTeamIdByUserId(request.getUserId());
        if (teamId == null) {
            return Set.of();
        }
        return new HashSet<>(teamChannelGateway.findChannelIdsByTeamId(teamId));
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/PermissionRouter.java
git commit -m "feat(routing): 创建 PermissionRouter — 从 InstanceSelector 提取团队权限过滤"
```

---

## Task 3: 创建 PriorityRouter

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/PriorityRouter.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/PriorityRouterTest.java`

- [ ] **Step 1: 创建 PriorityRouter**

从 `InstanceSelector` 提取 priority 分组逻辑：

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 优先级路由器 — 按 priority 分组，只保留 priority 最小的组
 */
@Component
@Order(200)
public class PriorityRouter implements Router {

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        if (instances.isEmpty()) {
            return List.of();
        }

        // 找最小 priority
        int minPriority = instances.stream()
                .min(Comparator.comparingInt(mi -> mi.getPriority() != null ? mi.getPriority() : 100))
                .map(mi -> mi.getPriority() != null ? mi.getPriority() : 100)
                .orElse(100);

        // 只保留 priority 最小的组
        return instances.stream()
                .filter(mi -> (mi.getPriority() != null ? mi.getPriority() : 100) == minPriority)
                .toList();
    }

    @Override
    public boolean isForce() { return true; }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/PriorityRouter.java
git commit -m "feat(routing): 创建 PriorityRouter — 从 InstanceSelector 提取 priority 分组逻辑"
```

---

## Task 4: 创建 HealthRouter

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/HealthRouter.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/HealthRouterTest.java`

- [ ] **Step 1: 创建 HealthRouter**

过滤熔断中的端点：

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 健康路由器 — 过滤熔断中的端点
 */
@Component
@Order(300)
@RequiredArgsConstructor
public class HealthRouter implements Router {

    private final ChannelEndpointCircuitBreakerManager circuitBreakerManager;

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        // 注意：ModelInstance 不直接持有 channelEndpointId，这里按 channelId 去重后检查
        // 在 RouterChain 中，HealthRouter 在 PriorityRouter 之后执行，
        // 此时 instances 数量已经很小，可直接逐 instance 检查
        return instances.stream()
                .filter(mi -> circuitBreakerManager.isAvailable(mi.getChannelId()))
                .toList();
    }

    @Override
    public boolean isForce() { return true; }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/HealthRouter.java
git commit -m "feat(routing): 创建 HealthRouter — 过滤熔断中的端点"
```

---

## Task 5: 创建 LoadBalanceRouter

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/LoadBalanceRouter.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/LoadBalanceRouterTest.java`

**注意：** LoadBalanceRouter 依赖 LoadBalance 接口（T6），因此此任务应在 T6 之后实现。

- [ ] **Step 1: 创建 LoadBalanceRouter**

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 负载均衡路由器 — 链终结者，内部调用 LoadBalance.select() 选一个实例
 */
@Component
@Order(9999)
@RequiredArgsConstructor
public class LoadBalanceRouter implements Router {

    private final Map<String, LoadBalance> loadBalanceMap;

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        if (instances.isEmpty()) {
            return List.of();
        }

        // 根据策略选择 LoadBalance 实现
        String strategyName = request.getStrategy().name().toLowerCase();
        LoadBalance loadBalance = loadBalanceMap.get(strategyName);
        if (loadBalance == null) {
            loadBalance = loadBalanceMap.get("weighted");
        }

        ModelInstance selected = loadBalance.select(instances);
        return selected != null ? List.of(selected) : List.of();
    }

    @Override
    public boolean isForce() { return true; }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/LoadBalanceRouter.java
git commit -m "feat(routing): 创建 LoadBalanceRouter — 链终结者，委托 LoadBalance.select()"
```

---

## Task 6: 创建 LoadBalance 接口 + AbstractLoadBalance

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/LoadBalance.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/AbstractLoadBalance.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/AbstractLoadBalanceTest.java`

- [ ] **Step 1: 创建 LoadBalance 接口**

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;

import java.util.List;

/**
 * 负载均衡策略接口
 *
 * <p>从候选实例列表中按策略选择一个实例。</p>
 */
@FunctionalInterface
public interface LoadBalance {

    /**
     * 从候选实例列表中选择一个
     *
     * @param instances 候选实例列表（非空）
     * @return 选中的实例
     */
    ModelInstance select(List<ModelInstance> instances);
}
```

- [ ] **Step 2: 创建 AbstractLoadBalance 抽象基类**

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;

import java.util.List;

/**
 * 负载均衡抽象基类
 *
 * <p>提供空检查和单元素短路。</p>
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    @Override
    public ModelInstance select(List<ModelInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        if (instances.size() == 1) {
            return instances.getFirst();
        }
        return doSelect(instances);
    }

    /**
     * 实际选择逻辑（由子类实现）
     */
    protected abstract ModelInstance doSelect(List<ModelInstance> instances);
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/LoadBalance.java
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/AbstractLoadBalance.java
git commit -m "feat(routing): 创建 LoadBalance 接口 + AbstractLoadBalance 抽象基类"
```

---

## Task 7: 实现 WeightedRandomLoadBalance

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/WeightedRandomLoadBalance.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/WeightedRandomLoadBalanceTest.java`

- [ ] **Step 1: 创建 WeightedRandomLoadBalance**

参照 Dubbo RandomLoadBalance 算法：

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 加权随机负载均衡
 *
 * <p>参照 Dubbo RandomLoadBalance 实现：</p>
 * <ul>
 *   <li>所有权重相同 → ThreadLocalRandom.current().nextInt(n)</li>
 *   <li>权重不同 → 前缀和数组 + nextInt(totalWeight) 二分查找</li>
 * </ul>
 */
@Component("weightedRandomLoadBalance")
public class WeightedRandomLoadBalance extends AbstractLoadBalance {

    @Override
    protected ModelInstance doSelect(List<ModelInstance> instances) {
        int totalWeight = 0;
        boolean sameWeight = true;
        int firstWeight = getWeight(instances.getFirst());

        for (int i = 0; i < instances.size(); i++) {
            int weight = getWeight(instances.get(i));
            totalWeight += weight;
            if (sameWeight && i > 0 && weight != firstWeight) {
                sameWeight = false;
            }
        }

        if (totalWeight <= 0) {
            return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
        }

        if (!sameWeight) {
            // 加权随机：前缀和 + 二分查找
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            for (ModelInstance instance : instances) {
                offset -= getWeight(instance);
                if (offset < 0) {
                    return instance;
                }
            }
        }

        // 所有权重相同，均匀随机
        return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }

    private int getWeight(ModelInstance instance) {
        return instance.getWeight() != null ? instance.getWeight() : 100;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/WeightedRandomLoadBalance.java
git commit -m "feat(routing): 实现 WeightedRandomLoadBalance — 参照 Dubbo RandomLoadBalance"
```

---

## Task 8: 创建 EndpointMetrics + EndpointMetricsRegistry

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/EndpointMetrics.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/EndpointMetricsRegistry.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/EndpointMetricsTest.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/EndpointMetricsRegistryTest.java`

- [ ] **Step 1: 创建 EndpointMetrics**

参照 Dubbo RpcStatus，为每个端点维护调用统计：

```java
package com.codingas.gateway.infrastructure.resilience;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 端点级调用统计（参照 Dubbo RpcStatus）
 *
 * <p>为每个 ChannelEndpoint 维护活跃数、总调用次数、总耗时、失败次数。</p>
 * <p>线程安全，使用 Atomic 系列实现无锁统计。</p>
 */
public class EndpointMetrics {

    private final AtomicInteger active = new AtomicInteger(0);
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    private final AtomicLong failedCalls = new AtomicLong(0);

    /**
     * 调用开始，活跃数 +1
     */
    public void beginCall() {
        active.incrementAndGet();
    }

    /**
     * 调用结束，更新统计
     *
     * @param durationMs 耗时（毫秒）
     * @param success    是否成功
     */
    public void endCall(long durationMs, boolean success) {
        active.decrementAndGet();
        totalCalls.incrementAndGet();
        totalDuration.addAndGet(durationMs);
        if (!success) {
            failedCalls.incrementAndGet();
        }
    }

    /** 当前活跃请求数 */
    public int getActive() { return active.get(); }

    /** 总调用次数 */
    public long getTotalCalls() { return totalCalls.get(); }

    /** 总耗时（毫秒） */
    public long getTotalDuration() { return totalDuration.get(); }

    /** 失败次数 */
    public long getFailedCalls() { return failedCalls.get(); }

    /** 平均耗时 */
    public double getAverageDuration() {
        long total = totalCalls.get();
        return total > 0 ? (double) totalDuration.get() / total : 0;
    }

    /** 失败率 */
    public double getFailureRate() {
        long total = totalCalls.get();
        return total > 0 ? (double) failedCalls.get() / total : 0;
    }
}
```

- [ ] **Step 2: 创建 EndpointMetricsRegistry**

```java
package com.codingas.gateway.infrastructure.resilience;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 端点统计注册表 — 按 endpointId 索引管理 EndpointMetrics
 */
@Component
public class EndpointMetricsRegistry {

    private final ConcurrentMap<Long, EndpointMetrics> metricsMap = new ConcurrentHashMap<>();

    /**
     * 获取或创建端点的统计实例
     */
    public EndpointMetrics get(Long endpointId) {
        return metricsMap.computeIfAbsent(endpointId, k -> new EndpointMetrics());
    }

    /**
     * 获取所有统计实例
     */
    public ConcurrentMap<Long, EndpointMetrics> getAll() {
        return metricsMap;
    }

    /**
     * 移除指定端点的统计
     */
    public void remove(Long endpointId) {
        metricsMap.remove(endpointId);
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/EndpointMetrics.java
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/EndpointMetricsRegistry.java
git commit -m "feat(resilience): 创建 EndpointMetrics + EndpointMetricsRegistry — 端点级调用统计"
```

---

## Task 9: ResilientUpstreamClient 增加 EndpointMetrics 埋点

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ResilientUpstreamClient.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/ResilientUpstreamClientMetricsTest.java`

- [ ] **Step 1: 修改 ResilientUpstreamClient**

在 chat() 和 chatStream() 中增加 `beginCall()` / `endCall()` 埋点：

```java
// 在类中增加 EndpointMetricsRegistry 依赖
private final EndpointMetricsRegistry metricsRegistry;

// 构造方法增加 metricsRegistry 参数
public ResilientUpstreamClient(UpstreamClient delegate, CircuitBreaker circuitBreaker,
                                RetryExecutor retryExecutor, MeterRegistry meterRegistry,
                                EndpointMetricsRegistry metricsRegistry,
                                String providerCode, Long endpointId) {
    this.delegate = delegate;
    this.circuitBreaker = circuitBreaker;
    this.retryExecutor = retryExecutor;
    this.meterRegistry = meterRegistry;
    this.metricsRegistry = metricsRegistry;
    this.providerCode = providerCode;
    this.endpointId = endpointId;
}
```

chat() 方法埋点：

```java
@Override
public ProtocolResponse chat(ProtocolRequest request) {
    if (!circuitBreaker.allowRequest()) {
        meterRegistry.counter("gateway.circuitbreaker.blocked",
                "provider", providerCode,
                "endpoint_id", String.valueOf(endpointId)).increment();
        throw new CircuitOpenException("熔断器开启，拒绝请求");
    }

    EndpointMetrics metrics = metricsRegistry.get(endpointId);
    metrics.beginCall();
    long startTime = System.currentTimeMillis();

    try {
        ProtocolResponse response = retryExecutor.execute(() -> delegate.chat(request));
        circuitBreaker.recordSuccess();
        metrics.endCall(System.currentTimeMillis() - startTime, true);
        return response;
    } catch (ProviderException e) {
        circuitBreaker.recordFailure();
        metrics.endCall(System.currentTimeMillis() - startTime, false);
        meterRegistry.counter("gateway.provider.errors",
                "provider", providerCode,
                "error_type", e.getErrorType().name()).increment();
        throw e;
    } catch (Exception e) {
        circuitBreaker.recordFailure();
        metrics.endCall(System.currentTimeMillis() - startTime, false);
        meterRegistry.counter("gateway.provider.errors",
                "provider", providerCode,
                "error_type", "UNKNOWN").increment();
        throw e;
    }
}
```

chatStream() 方法埋点（在回调中统计）：

```java
@Override
public void chatStream(ProtocolRequest request, StreamCallback callback) {
    if (!circuitBreaker.allowRequest()) {
        meterRegistry.counter("gateway.circuitbreaker.blocked",
                "provider", providerCode,
                "endpoint_id", String.valueOf(endpointId)).increment();
        throw new CircuitOpenException("熔断器开启，拒绝流式请求");
    }

    EndpointMetrics metrics = metricsRegistry.get(endpointId);
    metrics.beginCall();
    long startTime = System.currentTimeMillis();

    try {
        delegate.chatStream(request, new StreamCallback() {
            @Override
            public void onChunk(String data) {
                callback.onChunk(data);
            }

            @Override
            public void onComplete() {
                circuitBreaker.recordSuccess();
                metrics.endCall(System.currentTimeMillis() - startTime, true);
                callback.onComplete();
            }

            @Override
            public void onError(Throwable t) {
                circuitBreaker.recordFailure();
                metrics.endCall(System.currentTimeMillis() - startTime, false);
                if (t instanceof ProviderException pe) {
                    meterRegistry.counter("gateway.provider.errors",
                            "provider", providerCode,
                            "error_type", pe.getErrorType().name()).increment();
                } else {
                    meterRegistry.counter("gateway.provider.errors",
                            "provider", providerCode,
                            "error_type", "UNKNOWN").increment();
                }
                callback.onError(t);
            }
        });
    } catch (ProviderException e) {
        circuitBreaker.recordFailure();
        metrics.endCall(System.currentTimeMillis() - startTime, false);
        meterRegistry.counter("gateway.provider.errors",
                "provider", providerCode,
                "error_type", e.getErrorType().name()).increment();
        throw e;
    } catch (Exception e) {
        circuitBreaker.recordFailure();
        metrics.endCall(System.currentTimeMillis() - startTime, false);
        meterRegistry.counter("gateway.provider.errors",
                "provider", providerCode,
                "error_type", "UNKNOWN").increment();
        throw e;
    }
}
```

- [ ] **Step 2: 修改 ResilientClientFactory**

查找 `ResilientClientFactory` 的 wrap 方法，增加 `EndpointMetricsRegistry` 参数传递：

```java
// 在 factory 中注入 EndpointMetricsRegistry
private final EndpointMetricsRegistry metricsRegistry;

public ResilientUpstreamClient wrap(UpstreamClient client, Long endpointId) {
    CircuitBreaker breaker = circuitBreakerManager.getBreaker(endpointId);
    RetryExecutor retryExecutor = new RetryExecutor(maxRetries, retryDelayMs);
    return new ResilientUpstreamClient(client, breaker, retryExecutor, meterRegistry,
            metricsRegistry, providerCode, endpointId);
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/ResilientUpstreamClient.java
git add <ResilientClientFactory.java path>
git commit -m "feat(resilience): ResilientUpstreamClient 增加 EndpointMetrics 埋点"
```

---

## Task 10: InstanceSelector 简化 — 委托给 RouterChain

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/InstanceSelector.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RoutingResolver.java`

- [ ] **Step 1: 重构 InstanceSelector — 委托给 RouterChain**

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型实例选择器 — 委托给 RouterChain 执行权限过滤 + 优先级分组 + 负载均衡
 */
@Component
@RequiredArgsConstructor
public class InstanceSelector {

    private final ModelInstanceGateway modelInstanceGateway;
    private final RouterChain routerChain;

    /**
     * 根据 modelId 和用户身份选择模型实例
     *
     * @param modelId  模型 ID
     * @param userId   用户 ID
     * @param role     用户角色
     * @param strategy 路由策略
     * @return 选中的 ModelInstance
     * @throws ResourceNotFoundException 无可用实例
     */
    public ModelInstance select(Long modelId, Long userId, String role, RoutingStrategy strategy) {
        // 获取所有活跃实例（按 priority 升序）
        List<ModelInstance> allInstances = modelInstanceGateway.findActiveByModelIdOrderByPriority(modelId);
        if (allInstances.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 委托 RouterChain 执行过滤链
        RoutingRequest request = new RoutingRequest(modelId, userId, role, strategy);
        List<ModelInstance> result = routerChain.filter(allInstances, request);

        if (result.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        return result.getFirst();
    }
}
```

- [ ] **Step 2: 更新 RoutingResolver — 传递 strategy 参数**

```java
// 修改方法签名，增加 strategy 参数
public RoutingContext resolve(String modelName, Protocol protocol, Long userId, String role, RoutingStrategy strategy) {
    Model model = modelMatcher.match(modelName);
    ModelInstance modelInstance = instanceSelector.select(model.getId(), userId, role, strategy);
    // ... 其余不变
}
```

更新 `RoutingResolver` 的所有调用方（在 `ChatDispatchServiceImpl` 中），传入 `strategy` 参数。

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/InstanceSelector.java
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RoutingResolver.java
git commit -m "refactor(routing): InstanceSelector 委托给 RouterChain，RoutingResolver 传递 strategy"
```

---

## Task 11: 创建 KeyFailoverInvoker

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/invoker/KeyFailoverInvoker.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/invoker/`（包目录）
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/invoker/KeyFailoverInvokerTest.java`

- [ ] **Step 1: 创建 KeyFailoverInvoker**

从 `ChatDispatchServiceImpl.callWithKeyFailover()` 提取：

```java
package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.application.proxy.routing.CredentialResolver;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.ResilientClientFactory;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Key 级故障转移 Invoker
 *
 * <p>遍历同一 Channel 下的多个 Credential（Key），跳过熔断中的端点，
 * 失败切下一个 Key，全部失败抛 ProviderException。</p>
 */
@Component
public class KeyFailoverInvoker {

    private static final Logger log = LoggerFactory.getLogger(KeyFailoverInvoker.class);

    private final CredentialResolver credentialResolver;
    private final UpstreamClientRegistry clientRegistry;
    private final ResilientClientFactory resilientClientFactory;
    private final ChannelEndpointCircuitBreakerManager circuitBreakerManager;
    private final MeterRegistry meterRegistry;

    public KeyFailoverInvoker(CredentialResolver credentialResolver,
                               UpstreamClientRegistry clientRegistry,
                               ResilientClientFactory resilientClientFactory,
                               ChannelEndpointCircuitBreakerManager circuitBreakerManager,
                               MeterRegistry meterRegistry) {
        this.credentialResolver = credentialResolver;
        this.clientRegistry = clientRegistry;
        this.resilientClientFactory = resilientClientFactory;
        this.circuitBreakerManager = circuitBreakerManager;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 非流式调用 — 带 Key 级故障转移
     */
    public ProtocolResponse invoke(RoutingContext ctx, ProtocolRequest request) {
        List<ChannelCredential> credentials = credentialResolver.resolveAll(ctx.channelId());
        String provider = ctx.upstreamProtocol().name().toLowerCase();
        ProviderException lastException = null;

        for (ChannelCredential cred : credentials) {
            if (!circuitBreakerManager.isAvailable(ctx.channelEndpointId())) {
                log.debug("端点 {} 熔断中，跳过 Key {}", ctx.channelEndpointId(), cred.getId());
                continue;
            }

            UpstreamClient client = buildClient(ctx, cred);

            try {
                return client.chat(request);
            } catch (ProviderException e) {
                lastException = e;
                meterRegistry.counter("gateway.failover.triggered",
                        "provider", provider,
                        "from_key", String.valueOf(cred.getId()),
                        "error_type", e.getErrorType().name()).increment();
                log.warn("Key {} 失败: {} {}, 尝试下一个 Key", cred.getId(), e.getErrorType(), e.getMessage());
            }
        }

        meterRegistry.counter("gateway.failover.exhausted",
                "provider", provider,
                "channel_id", String.valueOf(ctx.channelId())).increment();
        throw new ProviderException(
                lastException != null ? lastException.getErrorType() : ProviderErrorType.UPSTREAM_ERROR,
                "所有 Key 均失败: " + (lastException != null ? lastException.getMessage() : "无可用 Key"),
                null, request.getModel(), provider, ctx.channelEndpointId(), null);
    }

    /**
     * 流式调用 — 调用前遍历 Key 检查熔断，传输开始后不切换
     */
    public void invokeStream(RoutingContext ctx, ProtocolRequest request, StreamCallback callback) {
        List<ChannelCredential> credentials = credentialResolver.resolveAll(ctx.channelId());
        String provider = ctx.upstreamProtocol().name().toLowerCase();

        for (ChannelCredential cred : credentials) {
            if (!circuitBreakerManager.isAvailable(ctx.channelEndpointId())) {
                log.debug("端点 {} 熔断中，跳过 Key {}", ctx.channelEndpointId(), cred.getId());
                continue;
            }

            UpstreamClient client = buildClient(ctx, cred);
            try {
                // 流式传输开始后不切换 Key
                client.chatStream(request, callback);
                return;
            } catch (ProviderException e) {
                log.warn("Key {} 流式启动失败: {} {}, 尝试下一个 Key",
                        cred.getId(), e.getErrorType(), e.getMessage());
                meterRegistry.counter("gateway.failover.triggered",
                        "provider", provider,
                        "from_key", String.valueOf(cred.getId()),
                        "error_type", e.getErrorType().name()).increment();
            }
        }

        // 所有 Key 流式启动失败
        meterRegistry.counter("gateway.failover.exhausted",
                "provider", provider,
                "channel_id", String.valueOf(ctx.channelId())).increment();
        throw new ProviderException(
                ProviderErrorType.UPSTREAM_ERROR,
                "流式调用：所有 Key 均失败",
                null, request.getModel(), provider, ctx.channelEndpointId(), null);
    }

    private UpstreamClient buildClient(RoutingContext ctx, ChannelCredential cred) {
        UpstreamClient rawClient = clientRegistry.getClient(
                ctx.upstreamProtocol().name().toLowerCase(),
                ctx.endpointUrl(),
                cred.getApiKeyPlain(),
                ctx.timeout() != null ? ctx.timeout() : 60);
        return resilientClientFactory.wrap(rawClient, ctx.channelEndpointId());
    }
}
```

- [ ] **Step 2: 提交**

```bash
mkdir -p gateway-boot/src/main/java/com/codingas/gateway/application/proxy/invoker
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/invoker/KeyFailoverInvoker.java
git commit -m "feat(invoker): 创建 KeyFailoverInvoker — 从 ChatDispatchServiceImpl 提取 Key 故障转移"
```

---

## Task 12: 创建 DegradationInvoker

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/invoker/DegradationInvoker.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/invoker/DegradationInvokerTest.java`

- [ ] **Step 1: 创建 DegradationInvoker**

从 `ChatDispatchServiceImpl.dispatch()` 的 catch 块提取：

```java
package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.application.degradation.DegradationService;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 模型降级 Invoker
 *
 * <p>包装 KeyFailoverInvoker，捕获 ProviderException 后走降级。
 * 降级后重新路由 + 递归调用。</p>
 */
@Component
public class DegradationInvoker {

    private static final Logger log = LoggerFactory.getLogger(DegradationInvoker.class);

    private final KeyFailoverInvoker keyFailoverInvoker;
    private final DegradationService degradationService;
    private final RoutingResolver routingResolver;

    public DegradationInvoker(KeyFailoverInvoker keyFailoverInvoker,
                               DegradationService degradationService,
                               RoutingResolver routingResolver) {
        this.keyFailoverInvoker = keyFailoverInvoker;
        this.degradationService = degradationService;
        this.routingResolver = routingResolver;
    }

    /**
     * 非流式调用 — 带降级保护
     */
    public ProtocolResponse invoke(RoutingContext ctx, ProtocolRequest request,
                                    Protocol inboundProtocol, Long userId, String role,
                                    RoutingStrategy strategy) {
        try {
            return keyFailoverInvoker.invoke(ctx, request);
        } catch (ProviderException e) {
            String fallbackModel = degradationService.degrade(request.getModel(), e.getErrorType());
            if (fallbackModel != null) {
                log.info("模型 {} 降级为 {}，重新调度", request.getModel(), fallbackModel);
                request.setModel(fallbackModel);
                RoutingContext newCtx = routingResolver.resolve(
                        fallbackModel, inboundProtocol, userId, role, strategy);
                return invoke(newCtx, request, inboundProtocol, userId, role, strategy);
            }
            throw e;
        }
    }

    /**
     * 流式调用 — 带降级保护
     */
    public void invokeStream(RoutingContext ctx, ProtocolRequest request, StreamCallback callback,
                              Protocol inboundProtocol, Long userId, String role,
                              RoutingStrategy strategy) {
        try {
            keyFailoverInvoker.invokeStream(ctx, request, callback);
        } catch (ProviderException e) {
            String fallbackModel = degradationService.degrade(request.getModel(), e.getErrorType());
            if (fallbackModel != null) {
                log.info("流式调用：模型 {} 降级为 {}，重新调度", request.getModel(), fallbackModel);
                request.setModel(fallbackModel);
                RoutingContext newCtx = routingResolver.resolve(
                        fallbackModel, inboundProtocol, userId, role, strategy);
                invokeStream(newCtx, request, callback, inboundProtocol, userId, role, strategy);
            }
            throw e;
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/invoker/DegradationInvoker.java
git commit -m "feat(invoker): 创建 DegradationInvoker — 从 ChatDispatchServiceImpl 提取降级逻辑"
```

---

## Task 13: ChatDispatchServiceImpl 集成 Invoker 链

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java`

- [ ] **Step 1: 重构 ChatDispatchServiceImpl**

替换 Key 遍历和降级为 DegradationInvoker，替换流式调用为 KeyFailoverInvoker.invokeStream()：

```java
@Service
public class ChatDispatchServiceImpl implements ChatDispatchService {

    private static final Logger log = LoggerFactory.getLogger(ChatDispatchServiceImpl.class);

    private final RoutingResolver routingResolver;
    private final OutboundTuner outboundTuner;
    private final ProtocolConverter protocolConverter;
    private final AuditGateway auditGateway;
    private final DomainEventPublisher eventPublisher;
    private final DegradationInvoker degradationInvoker;

    // 移除了: credentialResolver, circuitBreakerManager, clientRegistry, resilientClientFactory, meterRegistry

    public ChatDispatchServiceImpl(RoutingResolver routingResolver,
                                    OutboundTuner outboundTuner,
                                    ProtocolConverter protocolConverter,
                                    AuditGateway auditGateway,
                                    DomainEventPublisher eventPublisher,
                                    DegradationInvoker degradationInvoker) {
        this.routingResolver = routingResolver;
        this.outboundTuner = outboundTuner;
        this.protocolConverter = protocolConverter;
        this.auditGateway = auditGateway;
        this.eventPublisher = eventPublisher;
        this.degradationInvoker = degradationInvoker;
    }

    @Override
    public ProtocolResponse dispatch(ProtocolRequest request, Identity identity, RoutingStrategy strategy) {
        String traceId = UUID.randomUUID().toString();
        Protocol inboundProtocol = getInboundProtocol(request);
        RoutingContext ctx = routingResolver.resolve(
                request.getModel(), inboundProtocol, identity.userId(), identity.role(), strategy);

        log.info("Dispatch request: model={}, channelId={}, upstreamProtocol={}, endpointUrl={}, traceId={}",
                request.getModel(), ctx.channelId(), ctx.upstreamProtocol(), ctx.endpointUrl(), traceId);

        CallLog callLog = createCallLog(identity, request, ctx, inboundProtocol, traceId);
        long startTime = System.currentTimeMillis();

        try {
            ProtocolRequest outboundReq = request;

            // 阶段 3：请求转换
            if (ctx.needsProtocolAdaptation()) {
                outboundReq = convertRequest(request, ctx);
            }

            // 阶段 4：出站调谐
            outboundReq = outboundTuner.tune(outboundReq, ctx);

            // 阶段 5：Invoker 链调用（熔断 + 重试 + Key 故障转移 + 降级）
            ProtocolResponse response = degradationInvoker.invoke(
                    ctx, outboundReq, inboundProtocol, identity.userId(), identity.role(), strategy);

            // 阶段 6：响应转换
            if (ctx.needsProtocolAdaptation()) {
                response = convertResponse(response, ctx, inboundProtocol);
            }

            // 阶段 7：后置处理
            callLog.setDurationMs(System.currentTimeMillis() - startTime);
            callLog.setSuccess(true);
            publishTokenUsedEvent(response, identity, ctx, traceId);
            auditGateway.saveCallLog(callLog);

            return response;
        } catch (Exception e) {
            callLog.setDurationMs(System.currentTimeMillis() - startTime);
            callLog.setSuccess(false);
            callLog.setErrorMessage(e.getMessage());
            auditGateway.saveCallLog(callLog);
            throw e;
        }
    }

    @Override
    public void dispatchStream(ProtocolRequest request, Identity identity, RoutingStrategy strategy,
                               StreamCallback callback) {
        String traceId = UUID.randomUUID().toString();
        Protocol inboundProtocol = getInboundProtocol(request);
        RoutingContext ctx = routingResolver.resolve(
                request.getModel(), inboundProtocol, identity.userId(), identity.role(), strategy);

        log.info("Stream dispatch: model={}, channelId={}, upstreamProtocol={}, endpointUrl={}, traceId={}",
                request.getModel(), ctx.channelId(), ctx.upstreamProtocol(), ctx.endpointUrl(), traceId);

        CallLog callLog = createCallLog(identity, request, ctx, inboundProtocol, traceId);
        long startTime = System.currentTimeMillis();

        ProtocolRequest outboundReq = request;
        if (ctx.needsProtocolAdaptation()) {
            outboundReq = convertRequest(request, ctx);
        }
        outboundReq = outboundTuner.tune(outboundReq, ctx);

        StreamCallback auditingCallback = new StreamCallback() {
            @Override
            public void onChunk(String data) { callback.onChunk(data); }

            @Override
            public void onComplete() {
                callLog.setDurationMs(System.currentTimeMillis() - startTime);
                callLog.setSuccess(true);
                auditGateway.saveCallLog(callLog);
                callback.onComplete();
            }

            @Override
            public void onError(Throwable t) {
                callLog.setDurationMs(System.currentTimeMillis() - startTime);
                callLog.setSuccess(false);
                callLog.setErrorMessage(t.getMessage());
                auditGateway.saveCallLog(callLog);
                callback.onError(t);
            }
        };

        // 使用 KeyFailoverInvoker 的流式调用
        degradationInvoker.invokeStream(ctx, outboundReq, auditingCallback,
                inboundProtocol, identity.userId(), identity.role(), strategy);
    }
    // ... 其余辅助方法不变（createCallLog, publishTokenUsedEvent, getInboundProtocol, convertRequest, convertResponse）
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ChatDispatchServiceImpl.java
git commit -m "refactor(proxy): ChatDispatchServiceImpl 集成 Invoker 链，移除直接依赖的 credentialResolver/circuitBreakerManager 等"
```

---

## Task 14: 实现 RoundRobinLoadBalance

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RoundRobinLoadBalance.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/RoundRobinLoadBalanceTest.java`

- [ ] **Step 1: 创建 RoundRobinLoadBalance**

参照 Dubbo 平滑加权轮询算法：

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 平滑加权轮询负载均衡
 *
 * <p>参照 Dubbo RoundRobinLoadBalance 的平滑加权轮询算法：</p>
 * <ul>
 *   <li>每个实例维护 current 值，初始化 0</li>
 *   <li>选择：current += weight，选 current 最大的</li>
 *   <li>选中：current -= totalWeight</li>
 * </ul>
 */
@Component("roundRobinLoadBalance")
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    private final ConcurrentMap<Integer, AtomicInteger> currents = new ConcurrentHashMap<>();
    private static final int RECYCLE_PERIOD = 60000; // 60 秒

    @Override
    protected ModelInstance doSelect(List<ModelInstance> instances) {
        int totalWeight = 0;
        boolean sameWeight = true;
        int firstWeight = getWeight(instances.getFirst());

        for (int i = 0; i < instances.size(); i++) {
            int weight = getWeight(instances.get(i));
            totalWeight += weight;
            if (sameWeight && i > 0 && weight != firstWeight) {
                sameWeight = false;
            }
        }

        if (!sameWeight) {
            return doRoundRobin(instances, totalWeight);
        }

        // 所有权重相同，简单轮询
        int idx = getCurrent(0).updateAndGet(c -> (c + 1) % instances.size());
        return instances.get(Math.abs(idx) % instances.size());
    }

    private ModelInstance doRoundRobin(List<ModelInstance> instances, int totalWeight) {
        int maxCurrent = Integer.MIN_VALUE;
        int selectedIndex = 0;

        for (int i = 0; i < instances.size(); i++) {
            int weight = getWeight(instances.get(i));
            int current = getCurrent(i).addAndGet(weight);
            if (current > maxCurrent) {
                maxCurrent = current;
                selectedIndex = i;
            }
        }

        getCurrent(selectedIndex).addAndGet(-totalWeight);
        return instances.get(selectedIndex);
    }

    private AtomicInteger getCurrent(int index) {
        return currents.computeIfAbsent(index, k -> new AtomicInteger(0));
    }

    private int getWeight(ModelInstance instance) {
        return instance.getWeight() != null ? instance.getWeight() : 100;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RoundRobinLoadBalance.java
git commit -m "feat(routing): 实现 RoundRobinLoadBalance — 平滑加权轮询"
```

---

## Task 15: 实现 LeastActiveLoadBalance

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/LeastActiveLoadBalance.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/LeastActiveLoadBalanceTest.java`

- [ ] **Step 1: 创建 LeastActiveLoadBalance**

```java
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.infrastructure.resilience.EndpointMetricsRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 最少活跃负载均衡
 *
 * <p>参照 Dubbo LeastActiveLoadBalance：</p>
 * <ul>
 *   <li>通过 EndpointMetricsRegistry 获取活跃数</li>
 *   <li>选活跃数最少的实例</li>
 *   <li>同活跃度内按 weight 加权随机</li>
 * </ul>
 */
@Component("leastActiveLoadBalance")
@RequiredArgsConstructor
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    private final EndpointMetricsRegistry metricsRegistry;

    @Override
    protected ModelInstance doSelect(List<ModelInstance> instances) {
        int leastActive = Integer.MAX_VALUE;
        int leastCount = 0;
        int[] leastIndexes = new int[instances.size()];
        int totalWeight = 0;
        int firstWeight = 0;
        boolean sameWeight = true;

        for (int i = 0; i < instances.size(); i++) {
            ModelInstance instance = instances.get(i);
            int active = metricsRegistry.get(instance.getChannelId()).getActive();

            if (active < leastActive) {
                // 发现更小的活跃数，重置
                leastActive = active;
                leastCount = 1;
                leastIndexes[0] = i;
                totalWeight = getWeight(instance);
                firstWeight = totalWeight;
                sameWeight = true;
            } else if (active == leastActive) {
                // 同活跃度，累加权重
                leastIndexes[leastCount++] = i;
                totalWeight += getWeight(instance);
                if (sameWeight && totalWeight != firstWeight * leastCount) {
                    sameWeight = false;
                }
            }
        }

        if (leastCount == 1) {
            return instances.get(leastIndexes[0]);
        }

        // 同活跃度内加权随机
        if (!sameWeight && totalWeight > 0) {
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            for (int i = 0; i < leastCount; i++) {
                offset -= getWeight(instances.get(leastIndexes[i]));
                if (offset < 0) {
                    return instances.get(leastIndexes[i]);
                }
            }
        }

        // 同权重，均匀随机
        return instances.get(leastIndexes[ThreadLocalRandom.current().nextInt(leastCount)]);
    }

    private int getWeight(ModelInstance instance) {
        return instance.getWeight() != null ? instance.getWeight() : 100;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/LeastActiveLoadBalance.java
git commit -m "feat(routing): 实现 LeastActiveLoadBalance — 最少活跃优先"
```

---

## Task 16: 测试 RouterChain（含所有 Router 实现）

**Files:**
- Create/Update: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/RouterChainTest.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/PermissionRouterTest.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/PriorityRouterTest.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/HealthRouterTest.java`

- [ ] **Step 1: 编写 RouterChainTest**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("RouterChain 单元测试")
class RouterChainTest {

    @Test
    @DisplayName("Router 按 @Order 排序执行")
    void routers_executedInOrder() { ... }

    @Test
    @DisplayName("非强制 Router 过滤结果为空时跳过")
    void nonForceRouter_empty_skipped() { ... }

    @Test
    @DisplayName("强制 Router 过滤结果为空时直接返回空")
    void forceRouter_empty_returnsEmpty() { ... }
}
```

- [ ] **Step 2: 编写 PermissionRouterTest**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionRouter 单元测试")
class PermissionRouterTest {

    @Test
    @DisplayName("ADMIN 角色跳过团队过滤，返回所有活跃渠道实例")
    void admin_skipsTeamFilter() { ... }

    @Test
    @DisplayName("普通用户只返回团队渠道内的实例")
    void normalUser_filtersByTeam() { ... }

    @Test
    @DisplayName("用户无团队时返回空列表")
    void userWithoutTeam_returnsEmpty() { ... }
}
```

- [ ] **Step 3: 编写 PriorityRouterTest**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("PriorityRouter 单元测试")
class PriorityRouterTest {

    @Test
    @DisplayName("只保留 priority 最小的组")
    void keepsMinPriorityGroup() { ... }

    @Test
    @DisplayName("单 priority 组返回全部")
    void singlePriority_returnsAll() { ... }

    @Test
    @DisplayName("空列表返回空")
    void emptyInput_returnsEmpty() { ... }
}
```

- [ ] **Step 4: 编写 HealthRouterTest**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthRouter 单元测试")
class HealthRouterTest {

    @Mock
    ChannelEndpointCircuitBreakerManager cbManager;

    @Test
    @DisplayName("过滤熔断中的端点")
    void filtersCircuitBreakerOpen() { ... }

    @Test
    @DisplayName("全部熔断时返回空列表")
    void allCircuitBreakerOpen_returnsEmpty() { ... }
}
```

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/RouterChainTest.java
git add gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/PermissionRouterTest.java
git add gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/PriorityRouterTest.java
git add gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/HealthRouterTest.java
git commit -m "test(routing): 添加 RouterChain 及所有 Router 实现的单元测试"
```

---

## Task 17: 测试 LoadBalance 实现

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/WeightedRandomLoadBalanceTest.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/RoundRobinLoadBalanceTest.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/LeastActiveLoadBalanceTest.java`

- [ ] **Step 1: 编写 WeightedRandomLoadBalanceTest**

```java
@DisplayName("WeightedRandomLoadBalance 单元测试")
class WeightedRandomLoadBalanceTest {

    @Test
    @DisplayName("所有权重相同时均匀分布")
    void sameWeight_evenDistribution() {
        // 1000 次选择，每个实例应被选 ~333 次
        // 使用卡方检验验证分布均匀性
    }

    @Test
    @DisplayName("权重不同时分布接近权重比")
    void differentWeight_weightedDistribution() {
        // 权重比 1:2:3 的实例，10000 次选择后验证比例
    }

    @Test
    @DisplayName("单元素列表直接返回")
    void singleInstance_returnsDirectly() { ... }
}
```

- [ ] **Step 2: 编写 RoundRobinLoadBalanceTest**

```java
@DisplayName("RoundRobinLoadBalance 单元测试")
class RoundRobinLoadBalanceTest {

    @Test
    @DisplayName("N 次选择后各实例被选次数比例接近权重比")
    void weightedRoundRobin_distribution() {
        // 权重 1:2:3，6 次轮询：分别被选 1/2/3 次
    }

    @Test
    @DisplayName("平滑性验证：不会连续选择同一高权重实例")
    void smoothness_noConsecutive() { ... }
}
```

- [ ] **Step 3: 编写 LeastActiveLoadBalanceTest**

```java
@DisplayName("LeastActiveLoadBalance 单元测试")
class LeastActiveLoadBalanceTest {

    @Mock
    EndpointMetricsRegistry metricsRegistry;

    @Test
    @DisplayName("选活跃数最少的实例")
    void selectsLeastActive() { ... }

    @Test
    @DisplayName("同活跃度内按权重随机")
    void sameActive_weightedRandom() { ... }
}
```

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/WeightedRandomLoadBalanceTest.java
git add gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/RoundRobinLoadBalanceTest.java
git add gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/LeastActiveLoadBalanceTest.java
git commit -m "test(routing): 添加 LoadBalance 三种实现的单元测试"
```

---

## Task 18: 测试 Invoker 链

**Files:**
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/invoker/KeyFailoverInvokerTest.java`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/invoker/DegradationInvokerTest.java`

- [ ] **Step 1: 编写 KeyFailoverInvokerTest**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("KeyFailoverInvoker 单元测试")
class KeyFailoverInvokerTest {

    @Test
    @DisplayName("第一个 Key 成功时直接返回")
    void firstKeySuccess_returns() { ... }

    @Test
    @DisplayName("第一个 Key 失败时自动切换到下一个")
    void failover_toNextKey() { ... }

    @Test
    @DisplayName("所有 Key 失败时抛 ProviderException")
    void allKeysFailed_throwsProviderException() { ... }

    @Test
    @DisplayName("熔断中的端点被跳过")
    void circuitBreakerOpen_skipped() { ... }
}
```

- [ ] **Step 2: 编写 DegradationInvokerTest**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("DegradationInvoker 单元测试")
class DegradationInvokerTest {

    @Test
    @DisplayName("Key 全部失败时触发降级")
    void allKeysFailed_triggersDegradation() { ... }

    @Test
    @DisplayName("降级后重新路由并成功")
    void degradation_reroute_succeeds() { ... }

    @Test
    @DisplayName("降级耗尽时抛出原异常")
    void degradationExhausted_throwsOriginal() { ... }
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/application/proxy/invoker/KeyFailoverInvokerTest.java
git add gateway-boot/src/test/java/com/codingas/gateway/application/proxy/invoker/DegradationInvokerTest.java
git commit -m "test(invoker): 添加 KeyFailoverInvoker 和 DegradationInvoker 单元测试"
```

---

## Task 19: 回归测试

**Files:**
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/ChatDispatchServiceTest.java`
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/routing/RoutingResolverTest.java`

- [ ] **Step 1: 更新 ChatDispatchServiceTest**

适配新的构造方法（移除 credentialResolver、circuitBreakerManager 等 mock，增加 DegradationInvoker mock）：

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatDispatchService 回归测试")
class ChatDispatchServiceTest {

    @Mock private RoutingResolver routingResolver;
    @Mock private OutboundTuner outboundTuner;
    @Mock private ProtocolConverter protocolConverter;
    @Mock private AuditGateway auditGateway;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private DegradationInvoker degradationInvoker;

    private ChatDispatchServiceImpl dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new ChatDispatchServiceImpl(
                routingResolver, outboundTuner, protocolConverter,
                auditGateway, eventPublisher, degradationInvoker);
    }

    @Test
    @DisplayName("dispatch 七阶段调用链不变")
    void dispatch_sevenStageChain_preserved() { ... }

    @Test
    @DisplayName("dispatchStream 流式调用正常")
    void dispatchStream_succeeds() { ... }
}
```

- [ ] **Step 2: 运行现有测试确保不回归**

```bash
./mvnw test -pl gateway-boot -Dtest="*ChatDispatchService*,*RoutingResolver*,*InstanceSelector*,*ResilientUpstreamClient*"
```

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/application/proxy/ChatDispatchServiceTest.java
git commit -m "test(proxy): 更新 ChatDispatchServiceTest 适配 Invoker 链重构"
```

---

## 任务执行顺序

```
T1 (Router接口+RouterChain)
  ├── T2 (PermissionRouter)
  ├── T3 (PriorityRouter)
  ├── T4 (HealthRouter)
  └── T5 (LoadBalanceRouter) ── 依赖 T6
T6 (LoadBalance接口+AbstractLoadBalance)
  ├── T7 (WeightedRandomLoadBalance)
  ├── T14 (RoundRobinLoadBalance)
  └── T15 (LeastActiveLoadBalance) ── 依赖 T8
T8 (EndpointMetrics+Registry)
  └── T9 (ResilientUpstreamClient埋点)
T10 (InstanceSelector简化 ── 依赖 T1-T5)
T11 (KeyFailoverInvoker ── 依赖 T10)
  └── T12 (DegradationInvoker ── 依赖 T11)
T13 (ChatDispatchServiceImpl简化 ── 依赖 T11,T12)
T16 (测试RouterChain ── 依赖 T1-T5)
T17 (测试LoadBalance ── 依赖 T6-T7,T14-T15)
T18 (测试Invoker链 ── 依赖 T11-T12)
T19 (回归测试 ── 依赖 T13)
```

**建议执行批次：**
1. T1-T4 (Router 基础 + 3 个 Router 实现)
2. T6-T7 (LoadBalance 基础 + WeightedRandom)
3. T8-T9 (统计基础设施 + 埋点)
4. T14-T15 (RoundRobin + LeastActive)
5. T5 (LoadBalanceRouter — 需要 LoadBalance 接口就绪)
6. T10 (InstanceSelector 简化)
7. T11-T12 (Invoker 链)
8. T13 (ChatDispatchServiceImpl 简化)
9. T16-T19 (测试)
