# 应用渠道配置缓存设计文档

> 状态：已确认（Caffeine 本地缓存，数据面配置读取优化）。

## 1. 背景与目标

数据面"认证即授权"改造后，热点查询集中在 `InstanceSelector` 的两次应用渠道配置读取（每请求各 1 次 DB）：

- `ApplicationChannelRepository.findChannelIdsByApplicationId(applicationId)` —— 渠道集合（配置锚点）
- `ApplicationChannelRepository.findByApplicationId(applicationId)` —— 渠道优先级映射（`channelPriorityMap`，PriorityRouter 消费）

设计规模单实例 10,000 QPS：配置不变时这两次 DB 查询是纯浪费（配置量极小：~100 应用）。

**目标**：配置读取本地缓存化，Caffeine 实现——显式失效为主（配置写操作即时生效）、TTL 兜底防遗漏、命中率可观测。

## 2. 设计

### 2.1 缓存层（iam 域，配置提供侧）

```
现状：InstanceSelector → ApplicationChannelRepository（每次 DB）
改造：InstanceSelector → ApplicationChannelConfigProvider（新增，缓存装饰）→ ApplicationChannelRepository
```

新增 `iam.application.ApplicationChannelConfigProvider`（@Service，单例）：

```java
@Service
public class ApplicationChannelConfigProvider {

    /** 应用 → 渠道集合（配置锚点） */
    private final Cache<Long, Set<Long>> channelIdsCache;
    /** 应用 → 渠道优先级映射（PriorityRouter 消费） */
    private final Cache<Long, Map<Long, Integer>> priorityMapCache;

    public Set<Long> findChannelIdsByApplicationId(Long applicationId) {
        // Caffeine get(applicationId, k -> repository.findChannelIdsByApplicationId(k))
        // applicationId null → 空集（不缓存 null）
    }

    public Map<Long, Integer> findPriorityMapByApplicationId(Long applicationId) {
        // Caffeine get(applicationId, k -> buildPriorityMap(repository.findByApplicationId(k)))
        // applicationId null → 空映射（不缓存 null）
    }

    /** 配置变更时显式失效（ApplicationChannel 写操作后调用） */
    public void evict(Long applicationId) {
        channelIdsCache.invalidate(applicationId);
        priorityMapCache.invalidate(applicationId);
    }
}
```

### 2.2 Caffeine 配置

```java
private static Cache<Long, Set<Long>> newChannelIdsCache() {
    return Caffeine.newBuilder()
            .maximumSize(500)            // 应用上限 ~100，留余量
            .expireAfterWrite(60, TimeUnit.SECONDS)   // TTL 兜底（显式失效为主）
            .recordStats()               // 命中率可观测（可接入监控）
            .build();
}
```

### 2.3 失效钩子（配置写操作点）

配置变更必须即时生效（不能等 TTL），在以下写操作后调用 `evict(applicationId)`：

| 写操作 | 位置 |
|--------|------|
| 应用-渠道关联批量写入 | `ChannelProvisionService`（开通/变更） |
| 应用-渠道关联删除 | `ApplicationServiceImpl`（`deleteByApplicationId` 调用处，约 123/152 行） |
| 应用-渠道关联新增/更新 | `ApplicationChannelRepository.saveAll` 调用方（Provision/Application 服务） |

**失效时机**：写事务提交后 evict（避免事务回滚时缓存已清——实际 Caffeine 清空是安全的，仅多一次回查；但为一致性在服务层写操作返回前调用即可）。

### 2.4 分层与依赖

- `ApplicationChannelConfigProvider` 在 iam 域（`iam.application`），依赖现有 `ApplicationChannelRepository`（同域）
- `gateway-proxy` 的 `InstanceSelector` 改为注入 `ApplicationChannelConfigProvider`（替代直查 Repository）——proxy → iam 依赖不变
- 新增 Caffeine 依赖：`gateway-iam/iam` pom（`com.github.ben-manes.caffeine:caffeine`，Spring Boot 管理的版本）

### 2.5 明确不做

- 控制面缓存：`checkControl` 是代码化常量（无 DB 查询），表化暂缓前无缓存需求
- 分布式缓存（Redis）：配置量小 + 单实例部署，本地缓存足够；未来多实例部署时再评估 Redis
- 缓存监控接入（Micrometer）：`recordStats()` 预留，接入可观测平台为独立事项

## 3. 风险与验证

| 风险 | 缓解 |
|------|------|
| 配置变更后缓存陈旧（渠道可见性延迟） | 显式失效钩子（写操作即时 evict）+ TTL 兜底（60s） |
| 事务回滚后缓存被清（额外回查） | 无害：仅多一次 DB 查询，无正确性影响 |
| null applicationId 语义 | 不缓存 null 键值，直接返回空集/空映射（现状语义） |
| 性能 | 配置不变时 DB 查询降为 0；Caffeine recordStats 可观测命中率 |

**验证**：单元测试（缓存命中/失效/空值语义）+ `InstanceSelector` 测试适配（注入 Provider 替换 Repository）+ 全量回归。

## 4. 验收标准

1. `ApplicationChannelConfigProvider` 提供 findChannelIdsByApplicationId/findPriorityMapByApplicationId/evict，null 语义与现状一致
2. `InstanceSelector` 改为消费 Provider（不再直查 Repository），行为等价（候选集合与现状一致）
3. 配置写操作（Provision/Application 服务）调用 evict，配置变更即时生效
4. Caffeine 依赖引入（iam pom），命中率统计可用
5. 全量测试通过（1449+ 基线，无回归）
