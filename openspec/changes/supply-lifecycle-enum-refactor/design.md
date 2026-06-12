## Context

当前供应域（`domain/supply/`）有 6 个状态枚举，值几乎相同但分散在不同实体中：

| 枚举 | 值 | 使用者 |
|------|-----|--------|
| ProviderState | ACTIVE, INACTIVE | Provider |
| ChannelState | ACTIVE, INACTIVE | Channel |
| ModelState | ACTIVE, INACTIVE | Model |
| ChannelModelState | ACTIVE, INACTIVE | ModelInstance |
| CredentialState | ACTIVE, INACTIVE | ChannelCredential |
| CatalogState | ACTIVE, DEPRECATED | Catalog 实体 |

经过逐实体评估，发现多数实体并不需要独立的状态管理——它们的状态要么是派生的（Provider），要么可以用更简单的机制表达（Model 用 deprecation 字段、Credential 用 boolean）。

**约束条件**：
- Java 枚举不支持继承，无法通过继承复用状态集
- 两个真正需要状态管理的实体（Channel、ModelInstance）对状态的需求完全一致
- 数据库已有存量数据，需要迁移脚本

## Goals / Non-Goals

**Goals:**
- 只有 Channel 和 ModelInstance 持有状态枚举，其他实体无状态字段
- 每个枚举包含 isRoutable()、isTerminal()、canTransitionTo() 方法
- 状态转换规则明确且受约束（canTransitionTo 校验）
- 删除 6 个旧枚举文件，消除重复
- 路由调度层适配新的 phase 判断

**Non-Goals:**
- 不改变路由算法本身（仅适配 phase 判断方式）
- 不改变 Catalog 层的整体合并方案（本次只处理状态枚举）
- 不引入新的运行时 Metrics（熔断器行为不变）
- 不修改数据库表名或字段名（仅改变字段类型和约束）

## Decisions

### 决策 1：独立枚举而非共享 LifecyclePhase

**方案**：Channel 和 ModelInstance 各自声明内部枚举 `Phase`，值相同但类型不同。

```
public class Channel extends BaseEntity {
    public enum Phase {
        PENDING, ACTIVE, SUSPENDED, DEPRECATED, RETIRED;
    }
    private Phase phase = Phase.PENDING;
}

public class ModelInstance extends BaseEntity {
    public enum Phase {
        PENDING, ACTIVE, SUSPENDED, DEPRECATED, RETIRED;
    }
    private Phase phase = Phase.PENDING;
}
```

**备选方案**：
1. 共享 `LifecyclePhase` 单枚举 → 所有实体可互赋值，类型不安全
2. 接口抽象 `LifecyclePhase` → 增加间接层，值和方法完全一样时抽象收益为零
3. boolean 组合（enabled + deprecated）→ 状态空间组合爆炸，编译器不阻止非法组合

**理由**：值相同但类型不同，编译器保证不会把 `Channel.Phase.ACTIVE` 赋值给 `ModelInstance.phase`。各自可独立演化。`isRoutable()` 方法重复 2 行代码的成本远低于接口抽象带来的认知开销。

### 决策 2：PENDING / ACTIVE / SUSPENDED / DEPRECATED / RETIRED 五状态

```
PENDING ──→ ACTIVE ──→ DEPRECATED ──→ RETIRED
                ↑ │
                │ └── SUSPENDED
                └───────↻
```

| 状态 | 路由 | 触发者 | 含义 |
|------|------|--------|------|
| PENDING | 否 | 系统同步创建 | 待激活，缺 apiKey 等配置 |
| ACTIVE | 是 | 管理员激活 | 正常运行 |
| SUSPENDED | 否 | 管理员 | 暂停，可恢复 |
| DEPRECATED | 是 | 系统同步 | 上游标记即将下线，优先级低于 ACTIVE |
| RETIRED | 否 | 系统同步/管理员 | 已废弃，不可逆 |

**备选方案**：不考虑 ERROR 状态——运行时错误（超时、熔断）由基础设施层处理，不污染持久化生命周期。

### 决策 3：Model 用 deprecation 字段代替状态

```java
public class Model extends BaseEntity {
    private Instant deprecatedAt;        // 上游标记下线时间，null=正常
    private Instant scheduledRetiredAt;  // 预计下线日期
    private String deprecationMessage;   // 下线原因/建议迁移目标
}
```

**理由**：Model 是纯规格数据，不需要"待激活"或"暂停"。上游通知模型即将下线是一个**属性**（含时间、原因），不是一个**状态**。用字段表达比用枚举更丰富。

### 决策 4：其他实体无状态字段

- **Provider**：无 state。通过是否有 ACTIVE 的 Channel 派生可用性
- **ChannelCredential**：无 state。密钥轮换用新建/删除记录表达
- **ChannelEndpoint**：无 state。跟随 Channel 的 phase

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| [R1] 旧数据迁移：存量数据的 6 种枚举值需映射到新模型 | 提供一次性迁移脚本，明确映射规则（INACTIVE → SUSPENDED，CatalogState.DEPRECATED → RETIRED） |
| [R2] Model 失去 ModelState 后，查询"哪些模型可用"变复杂 | 通过 `ModelInstance` 的 phase 查询——有 ACTIVE ModelInstance 的 Model 就是可用的 |
| [R3] ChannelEndpoint 无独立状态，如果未来需要独立控制端点 | 当前无此需求，未来可通过 `ChannelEndpoint` 继承 `Channel` 的 phase 或新增字段 |
| [R4] canTransitionTo 只在 Java 层校验，数据库层无约束 | 可接受——数据库层只存值，业务规则在 domain 层实施 |
