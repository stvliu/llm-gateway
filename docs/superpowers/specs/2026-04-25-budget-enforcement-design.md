# Budget Enforcement 设计文档

**日期:** 2026-04-25
**项目:** LLM-Gateway
**状态:** 设计中

---

## 1. 概述

Budget Enforcement（预算执行）服务负责在每次 LLM API 调用前检查用户预算、在调用后扣减额度，确保用户消耗不超过其配额。

### 设计目标

| 指标 | 目标 |
|------|------|
| 预算维度 | 用户 + 模型（作为整体） |
| 检查时序 | 调用前检查 |
| 软/硬限制 | 80% 警告 / 100% 拒绝 |
| 降级策略 | 不支持降级，超过即拒绝 |
| QPS 支持 | 10,000+ |

### 技术方案

- **默认：** Spring Cache（内存缓存，如 Caffeine）
- **可选：** Redis 缓存，通过 `@Cacheable` 切换
- **一致性：** 调用前同步检查，调用后异步扣减 DB

---

## 2. 预算维度

每条 `token_limits` 记录代表一个组合：

```
(user_id, model_id) → 一个预算桶
```

**周期类型：** DAILY / WEEKLY / MONTHLY / TOTAL

**超限动作：** REJECT（仅支持，拒绝请求）

---

## 3. 检查流程

### 3.1 调用前（同步）

```
请求进入
    │
    ▼
┌──────────────────────────────┐
│ BudgetEnforcementService    │
│   .checkAndReserve()        │
└──────────────────────────────┘
    │
    ▼
┌──────────────────────────────┐
│ BudgetCacheService           │  ← Spring Cache 抽象
│   - 本地：Caffeine            │
│   - 远程：Redis               │
└──────────────────────────────┘
    │
    ▼
┌──────────────────────────────┐
│ Redis Lua 脚本 / 本地原子操作 │
│ 原子检查 + 预扣              │
└──────────────────────────────┘
    │
    ├── 超过 100% → BudgetExceededException（拒绝）
    │
    ├── 超过 80% → 返回 WARN（软限警告）
    │
    └── 未超限 → 放行，执行业务
```

### 3.2 调用后（异步）

```
LLM 响应返回
    │
    ▼
┌──────────────────────────────┐
│ TokenUsageHandler            │
│   .recordUsage()             │  ← 异步，不阻塞响应
└──────────────────────────────┘
    │
    ├── 更新 Redis 缓存（INCRBY）
    │
    └── 异步写 DB（最终一致）
```

---

## 4. 缓存设计

### 4.1 Spring Cache 抽象

```java
@Cacheable(
    value = "budget",
    key = "#userId + ':' + #modelId",
    cacheManager = "budgetCacheManager"  // 可切换 Caffeine / Redis
)
public BudgetSnapshot getBudget(Long userId, Long modelId) { ... }
```

### 4.2 缓存键

```
budget:{userId}:{modelId}
```

### 4.3 本地缓存配置（Caffeine）

```yaml
spring:
  cache:
    type: caffeine
  cache:
    caffeine:
      spec: maximumSize=10000, expireAfterWrite=5m
```

### 4.4 Redis 缓存配置

```yaml
spring:
  cache:
    type: redis
  cache:
    redis:
      time-to-live: 300s
```

### 4.5 两级缓存（推荐）

```
请求 → Caffeine（本地） → Redis（共享） → DB（最终）
```

- **本地 Caffeine：** 热点数据，QPS 支撑
- **Redis：** 多实例共享，周期同步
- **DB：** 持久化，Redis 不可用时的降级

---

## 5. 组件设计

### 5.1 核心组件

| 组件 | 职责 |
|------|------|
| `BudgetEnforcementService` | 预算检查入口，编排逻辑 |
| `BudgetCacheService` | 缓存抽象，支持本地/Redis 切换 |
| `TokenLimitRepository` | DB 访问 |
| `TokenUsageHandler` | 异步扣减处理器 |
| `BudgetCheckResult` | 检查结果不可变对象 |
| `BudgetExceededException` | 预算超限异常 |

### 5.2 核心接口

```java
public interface BudgetEnforcer {
    /**
     * 检查预算并预扣
     * @return 检查结果（通过/警告/拒绝）
     */
    BudgetCheckResult checkAndReserve(Long userId, Long modelId, Integer promptTokens);

    /**
     * 记录实际使用量（异步）
     */
    void recordUsage(Long userId, Long modelId, TokenUsage usage);
}
```

### 5.3 不可变对象

```java
public record BudgetCheckResult(
    boolean allowed,
    boolean warning,
    BigDecimal usedTokens,
    BigDecimal maxTokens,
    BigDecimal remaining
) {}

public record TokenUsage(
    Integer promptTokens,
    Integer completionTokens,
    Integer totalTokens
) {}
```

---

## 6. 数据模型

### 6.1 Redis Key（可选）

```
budget:{userId}:{modelId}:used    → 已用 Token
budget:{userId}:{modelId}:max    → 限额
budget:{userId}:{modelId}:period  → 当前周期标识
```

### 6.2 Lua 脚本（Redis 模式）

```lua
-- 检查并预扣原子操作
local used = tonumber(redis.call('GET', KEYS[1]) or '0')
local max = tonumber(ARGV[1])
local estimated = tonumber(ARGV[2])
local softLimit = max * 0.8

local newUsed = used + estimated
if newUsed > max then
    return 2  -- REJECT
elseif newUsed > softLimit then
    return 1  -- WARN
else
    redis.call('SET', KEYS[1], newUsed)
    return 0  -- OK
end
```

---

## 7. 异常设计

| 异常 | HTTP 状态码 | 说明 |
|------|------------|------|
| `BudgetExceededException` | 429 | 预算耗尽，拒绝请求 |
| `BudgetNotFoundException` | 404 | 未找到对应预算记录 |
| `BudgetCacheException` | 503 | 缓存服务不可用，降级到 DB |

### 响应格式

```json
{
  "error": {
    "code": "BUDGET_EXCEEDED",
    "message": "Budget exceeded for model claude-sonnet-4",
    "details": {
      "userId": 123,
      "modelId": 456,
      "usedTokens": 950000,
      "maxTokens": 1000000,
      "resetAt": "2026-04-26T00:00:00Z"
    }
  }
}
```

---

## 8. 软限警告头

响应中包含警告信息：

```
X-Budget-Warning: soft_limit
X-Budget-Used: 850000
X-Budget-Max: 1000000
X-Budget-Remaining: 150000
```

---

## 9. 周期重置

| 周期类型 | 重置时间点 |
|----------|-----------|
| DAILY | 每日 UTC 00:00 |
| WEEKLY | 每周一 UTC 00:00 |
| MONTHLY | 每月 1 日 UTC 00:00 |
| TOTAL | 不重置 |

**重置机制：** Redis Key 添加日期后缀，DB 通过定时任务清理历史记录。

---

## 10. 测试策略

| 测试类型 | 覆盖率目标 |
|----------|-----------|
| 单元测试 | BudgetEnforcementService ≥ 90% |
| 集成测试 | BudgetCacheService + Redis/Caffeine |
| 性能测试 | 10,000 QPS 下预算检查延迟 |

---

## 11. 实施计划

1. **Phase 1:** `BudgetEnforcementService` + 本地缓存（Caffeine）
2. **Phase 2:** Redis 两级缓存支持
3. **Phase 3:** 异步扣减 + 周期重置
4. **Phase 4:** 监控指标 + 告警

---

## 12. 参考文件

- `gateway-core/src/main/java/com/codingas/gateway/core/domain/entity/TokenLimit.java`
- `gateway-core/src/main/java/com/codingas/gateway/core/security/ratelimit/RateLimitService.java`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/dto/LLMResponse.java`
