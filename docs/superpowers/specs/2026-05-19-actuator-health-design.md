# Spring Boot Actuator 健康检测设计

## 概述

用 Spring Boot Actuator 体系替换自定义 HealthController，实现分层健康检测，支持 K8s liveness/readiness probe 和 Provider 可达性混合探测策略。

## 背景

### 当前问题

1. **自定义 HealthController 无真实检测能力** — `/api/v1/health` 返回硬编码 `UP`
2. **无自定义 HealthIndicator** — 对网关最关键的 Provider 可达性无健康检测
3. **K8s probe 路径未标准化** — Helm chart 配置了 liveness/readiness，但未指向 Actuator
4. **安全拦截器可能阻断 Actuator** — 缺少对 `/actuator/health` 的公开访问控制

### 已有基础

- `spring-boot-actuator` 和 `micrometer-registry-prometheus` 已引入
- `application.yml` 已配置 Actuator 端点
- `LLMAdapter` 已有 `isHealthy()` 和 `checkConnection()` 方法
- `AdapterRegistry` 已有 `getAllAdapters()` 方法
- Prometheus 已配置抓取 `/actuator/prometheus`

## 设计决策

### 方案选择：纯 Actuator 模式

删除自定义 HealthController，完全依赖 Actuator 体系。实现自定义 HealthIndicator 注册到 Actuator，利用内置 health group 机制区分 liveness/readiness。

选择理由：
- 项目已引入 Actuator 和 Micrometer，无需额外依赖
- Spring Boot 生态标准做法，运维和 K8s 集成最简单
- 自定义 HealthIndicator 机制足够灵活

### 健康检测分层

| 层级 | 端点 | 检测内容 | K8s 用途 |
|------|------|----------|----------|
| liveness | `/actuator/health/liveness` | 仅进程存活（ping） | 判断是否重启 Pod |
| readiness | `/actuator/health/readiness` | DB + 至少一个 Provider 可达 | 判断是否路由流量 |
| 全量详情 | `/actuator/health` | DB + Provider + diskSpace + ... | 运维/监控面板 |

**核心原则：** liveness 只检测进程本身，readiness 检测服务是否可接收流量。DB 或 Provider 暂时不可达不应触发 Pod 重启（重启解决不了外部依赖问题），只应暂停路由流量。

### Provider 可达性检测 — 混合策略

```
启动时: 主动调用 checkConnection() → 缓存结果
运行中: 基于最近 N 次实际请求的成功/失败推断
超时后: 超过 T 秒无请求 → 重新主动探测
```

- 至少一个 Provider UP → readiness UP
- 全部 Provider DOWN → readiness DOWN

### Health 端点公开访问控制

新增配置项 `management.endpoint.health.public-access`（默认 `true`）：
- `true`：`/actuator/health/**` 跳过认证拦截，K8s probe 和外部监控可直接访问
- `false`：需要认证才能访问，适用于安全要求更高的生产环境

## 组件设计

### ProviderRegistryHealthIndicator

位置：`infrastructure/actuator/ProviderRegistryHealthIndicator.java`

```java
@Component
public class ProviderRegistryHealthIndicator extends AbstractHealthIndicator {

    private final AdapterRegistry adapterRegistry;
    private final ProviderHealthTracker healthTracker;

    // health() 逻辑：
    // 1. 遍历 adapterRegistry.getAllAdapters()
    // 2. 对每个 adapter 查询 healthTracker.getStatus(providerCode)
    // 3. 至少一个 Provider UP → 整体 UP
    //    全部 DOWN → 整体 DOWN
    // 4. details 中列出每个 Provider 的状态、上次检测时间、连续失败次数
}
```

### ProviderHealthTracker（混合策略核心）

位置：`infrastructure/actuator/ProviderHealthTracker.java`

```java
@Component
public class ProviderHealthTracker {

    // 内部状态：每个 Provider 的 ProviderHealthState
    // - status: UP / DOWN / UNKNOWN
    // - lastCheckTime: 上次主动探测时间
    // - lastRequestTime: 上次实际请求时间
    // - consecutiveFailures: 连续失败次数
    // - lastError: 最近错误信息

    // 核心方法：
    // getStatus(providerCode) → ProviderHealthState
    //   - 如果距上次主动探测 > staleThreshold → 异步触发主动探测
    //   - 返回缓存状态（不阻塞）

    // recordRequestResult(providerCode, success, error) → void
    //   - 实际请求后调用，更新被动推断状态
    //   - 连续失败 >= failureThreshold → 标记 DOWN
    //   - 连续成功 >= successThreshold → 标记 UP
}
```

### ProviderHealthState

位置：`infrastructure/actuator/ProviderHealthState.java`

```java
public record ProviderHealthState(
    String providerCode,
    org.springframework.boot.actuate.health.Status status,  // UP / DOWN / UNKNOWN
    Instant lastCheckTime,
    Instant lastRequestTime,
    int consecutiveFailures,
    String lastError
) {}
```

### ActuatorSecurityConfig

位置：`infrastructure/config/ActuatorSecurityConfig.java`

```java
@Configuration
public class ActuatorSecurityConfig {

    @Value("${management.endpoint.health.public-access:true}")
    private boolean healthPublicAccess;

    // 注册 WebMvcConfigurer，将 /actuator/health/**
    // 从 SecurityInterceptorChain 的拦截范围中排除（当 public-access=true）
}
```

## 配置项

### application.yml 变更

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,traces
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
      public-access: true  # 新增：控制 health 端点是否公开
      group:
        liveness:
          include: ping  # 仅进程存活
        readiness:
          include: db, providerRegistry  # DB + Provider 就绪

gateway:
  health:
    provider:
      stale-threshold: 300s    # 超过此时间无请求则重新主动探测
      failure-threshold: 3     # 连续失败 N 次标记 DOWN
      success-threshold: 2     # 连续成功 N 次恢复 UP
      probe-timeout: 10s       # 主动探测超时
```

### Helm values.yaml 变更

```yaml
livenessProbe:
  path: /actuator/health/liveness  # 原 /api/v1/health
readinessProbe:
  path: /actuator/health/readiness  # 原 /api/v1/health
```

## 数据流

```
K8s → /actuator/health/readiness
         ↓
    Actuator HealthAggregator
         ↓
    ┌─────────────┬──────────────────────┐
    │ db (内置)    │ providerRegistry      │
    │ HikariCP    │ ProviderRegistryHI    │
    │ 连接池检查   │       ↓               │
    │             │ ProviderHealthTracker  │
    │             │   ├─ OpenAI: UP       │
    │             │   ├─ Anthropic: UP    │
    │             │   └─ Volcengine: DOWN │
    │             │   → 至少1个UP = UP     │
    └─────────────┴──────────────────────┘
         ↓
    聚合结果: UP / DOWN
```

## 错误处理

| 场景 | 处理 |
|------|------|
| DB 连接池耗尽 | readiness DOWN，K8s 暂停路由流量，不重启 Pod |
| 全部 Provider DOWN | readiness DOWN，Pod 不接收新请求 |
| 主动探测超时 | 标记该 Provider 为 DOWN，不影响其他 Provider |
| 主动探测异常（网络错误） | 标记 DOWN，记录错误信息到 details |
| HealthTracker 状态过期 | 异步触发重新探测，返回上次缓存状态（不阻塞 health 请求） |
| `/actuator/health` 被认证拦截 | `public-access=true` 则排除拦截；否则返回 401 |

## 文件变更清单

| 操作 | 文件 |
|------|------|
| 新增 | `infrastructure/actuator/ProviderRegistryHealthIndicator.java` |
| 新增 | `infrastructure/actuator/ProviderHealthTracker.java` |
| 新增 | `infrastructure/actuator/ProviderHealthState.java` |
| 新增 | `infrastructure/config/ActuatorSecurityConfig.java` |
| 修改 | `application.yml` — 添加 health group 和配置项 |
| 修改 | `WebConfig.java` — 拦截器注册中排除 `/actuator/health/**`（当 public-access=true） |
| 修改 | `helm/values.yaml` — K8s probe 路径 |
| 删除 | `adapter/api/HealthController.java` |
| 删除 | `HealthControllerTest.java` |

## 测试策略

| 测试类型 | 测试内容 | 覆盖目标 |
|----------|----------|----------|
| 单元测试 | `ProviderHealthTracker` 状态转换逻辑（UP→DOWN→UP） | ≥90% |
| 单元测试 | `ProviderRegistryHealthIndicator` 聚合逻辑 | ≥90% |
| 单元测试 | `ActuatorSecurityConfig` 配置项生效验证 | ≥80% |
| 集成测试 | `/actuator/health` 端点返回正确结构 | ≥80% |
| 集成测试 | `/actuator/health/liveness` 只含 ping | ≥80% |
| 集成测试 | `/actuator/health/readiness` 含 db + providerRegistry | ≥80% |
| 集成测试 | `public-access=false` 时 health 端点需认证 | ≥80% |

### 测试要点

1. **ProviderHealthTracker** 是最核心的测试对象：
   - 连续失败 N 次后状态变为 DOWN
   - 连续成功 M 次后状态恢复 UP
   - staleThreshold 过期后触发主动探测
   - 异步探测不阻塞 getStatus 调用

2. **集成测试** 使用 `@SpringBootTest` + `MockMvc`：
   - Mock `AdapterRegistry` 和 `LLMAdapter`
   - 验证 health 端点 JSON 结构符合 Actuator 规范
   - 验证 liveness/readiness 分组只包含预期组件
