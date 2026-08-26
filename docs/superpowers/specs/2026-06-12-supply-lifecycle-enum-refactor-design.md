---
comet_change: supply-lifecycle-enum-refactor
role: technical-design
canonical_spec: openspec
archived-with: 2026-06-12-supply-lifecycle-enum-refactor
status: final
---

# 供应域状态枚举重构 — 生命周期管理设计

## 背景

当前供应域有 6 个状态枚举（ProviderState、ChannelState、ModelState、ChannelModelState、CredentialState、CatalogState），值几乎完全重复（ACTIVE/INACTIVE），但多数实体不需要独立的状态管理。需要统一生命周期模型，消除重复。

## 方案

### 枚举设计

只有 **Channel** 和 **ModelInstance** 持有独立的状态枚举（`Channel.Phase`、`ModelInstance.Phase`），值相同但类型独立，编译器保证互赋值安全。

```java
public enum Phase {
    PENDING,
    ACTIVE,
    SUSPENDED,
    DEPRECATED,
    RETIRED;

    public boolean isRoutable() {
        return this == ACTIVE || this == DEPRECATED;
    }

    public boolean isTerminal() {
        return this == RETIRED;
    }

    public boolean canTransitionTo(Phase target) {
        return switch (this) {
            case PENDING    -> target == ACTIVE;
            case ACTIVE     -> target == SUSPENDED || target == DEPRECATED;
            case SUSPENDED  -> target == ACTIVE    || target == DEPRECATED;
            case DEPRECATED -> target == RETIRED;
            case RETIRED    -> false;
        };
    }
}
```

### 状态转换

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

### 各实体状态字段处理

| 实体 | 原字段 | 改为 |
|------|--------|------|
| Channel | `ChannelState state` | `Channel.Phase phase`，默认 PENDING |
| ModelInstance | `ChannelModelState state` | `ModelInstance.Phase phase`，默认 PENDING |
| Model | `ModelState state` | 去掉 state，新增 `deprecatedAt`、`scheduledRetiredAt`、`deprecationMessage` |
| Provider | `ProviderState state` | 去掉 state |
| ChannelCredential | `CredentialState state` | 去掉 state |
| ChannelEndpoint | （若有 state） | 去掉 state |

### 路由适配

`InstanceSelector` 中：
- 过滤条件从 `state == ACTIVE` 改为 `phase.isRoutable()`
- 排序时 ACTIVE 优先于 DEPRECATED

## 文件变更

- 删除 6 个旧枚举文件
- 修改 6 个实体文件
- 修改 4 个管理服务文件
- 修改 InstanceSelector
- 新增数据库迁移脚本

## 风险

| 风险 | 缓解 |
|------|------|
| 全项目遗漏旧枚举引用 | grep 全项目搜索 |
| 存量数据迁移 | 一次性迁移脚本 |
| Jackson 序列化兼容 | 确认枚举名变更不影响已序列化数据 |
