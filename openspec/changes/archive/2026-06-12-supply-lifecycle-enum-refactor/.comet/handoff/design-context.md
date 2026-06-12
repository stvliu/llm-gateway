# Comet Design Handoff

- Change: supply-lifecycle-enum-refactor
- Phase: design
- Mode: compact
- Context hash: 913736ce4ad15abbe5325dae7e9fe0ad47d583be30c7517b8ea35d040f3e9c46

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/supply-lifecycle-enum-refactor/proposal.md

- Source: openspec/changes/supply-lifecycle-enum-refactor/proposal.md
- Lines: 1-35
- SHA256: 3a76500b98d8e2dd5e048d15d95c3cedd5680c876d79788a5aa9b9d9f3280666

```md
## Why

当前供应域（supply）存在 6 个状态枚举（ProviderState、ChannelState、ModelState、ChannelModelState、CredentialState、CatalogState），值几乎完全重复（ACTIVE/INACTIVE），但多数实体实际上不需要独立的状态管理。同时 Catalog 与 Runtime 的实体结构镜像但存在不必要的数据搬运。需要统一生命周期模型，消除重复，明确每个实体真正需要的状态管理。

## What Changes

- **新增** Channel.Phase 和 ModelInstance.Phase 两个独立枚举，各含 PENDING / ACTIVE / SUSPENDED / DEPRECATED / RETIRED 五个状态，附带 isRoutable()、isTerminal()、canTransitionTo() 方法
- **删除** 6 个旧枚举文件：ProviderState、ChannelState、ModelState、ChannelModelState、CredentialState、CatalogState
- **修改** Channel 实体：用 Channel.Phase 替换 ChannelState
- **修改** ModelInstance 实体：用 ModelInstance.Phase 替换 ChannelModelState
- **修改** Model 实体：去掉 ModelState，改为 deprecatedAt / scheduledRetiredAt / deprecationMessage 字段
- **修改** Provider 实体：去掉 ProviderState
- **修改** ChannelCredential 实体：去掉 CredentialState
- **修改** ChannelEndpoint 实体：去掉 ChannelEndpointState（若存在）
- **修改** 所有 Catalog 实体：去掉 CatalogState（Catalog 层合并后不再需要）
- **修改** 路由调度层（InstanceSelector 等）：适配新的 phase 判断逻辑
- **修改** 数据库迁移脚本：处理旧状态到新模型的映射

## Capabilities

### New Capabilities
- `entity-lifecycle`: 供应域实体生命周期管理，定义统一的状态转换规则和路由可见性

### Modified Capabilities
- （无 spec 级别的需求变更，本次仅为实现层重构）

## Impact

- **gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/**：删除 6 个枚举文件
- **gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/**：修改 Channel、ModelInstance、Model、Provider、ChannelCredential、ChannelEndpoint
- **gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/entity/**：修改所有 Catalog 实体（去掉 CatalogState）
- **gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/enums/**：删除 CatalogState
- **gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/**：修改 InstanceSelector 等调用方
- **数据库迁移脚本**：新增 migration 脚本处理状态映射
- 涉及约 15-20 个文件
```

## openspec/changes/supply-lifecycle-enum-refactor/design.md

- Source: openspec/changes/supply-lifecycle-enum-refactor/design.md
- Lines: 1-109
- SHA256: b13b3fb88683011d82f9273ee140d842fe9d017c294bdc2fc835e1fdfe23c2e5

[TRUNCATED]

```md
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
```

Full source: openspec/changes/supply-lifecycle-enum-refactor/design.md

## openspec/changes/supply-lifecycle-enum-refactor/tasks.md

- Source: openspec/changes/supply-lifecycle-enum-refactor/tasks.md
- Lines: 1-42
- SHA256: cfe5e2f0cd4e118897441c97cdd873b99f968729732ba0d1243291d70899a0c5

```md
## 1. 枚举定义

- [ ] 1.1 在 Channel 实体中新增 Phase 枚举（PENDING / ACTIVE / SUSPENDED / DEPRECATED / RETIRED），包含 isRoutable()、isTerminal()、canTransitionTo() 方法
- [ ] 1.2 在 ModelInstance 实体中新增 Phase 枚举（同上）

## 2. 实体修改

- [ ] 2.1 修改 Channel 实体：用 Channel.Phase phase 替换 ChannelState state
- [ ] 2.2 修改 ModelInstance 实体：用 ModelInstance.Phase phase 替换 ChannelModelState state
- [ ] 2.3 修改 Model 实体：去掉 ModelState，新增 deprecatedAt / scheduledRetiredAt / deprecationMessage 字段
- [ ] 2.4 修改 Provider 实体：去掉 ProviderState 字段
- [ ] 2.5 修改 ChannelCredential 实体：去掉 CredentialState 字段
- [ ] 2.6 修改 ChannelEndpoint 实体：去掉 ChannelEndpointState（若存在）

## 3. 旧枚举删除

- [ ] 3.1 删除 ProviderState.java
- [ ] 3.2 删除 ChannelState.java
- [ ] 3.3 删除 ModelState.java
- [ ] 3.4 删除 ChannelModelState.java
- [ ] 3.5 删除 CredentialState.java
- [ ] 3.6 删除 CatalogState.java
- [ ] 3.7 删除 Catalog 实体中的 CatalogState 引用

## 4. 调用方适配

- [ ] 4.1 修改 InstanceSelector：phase.isRoutable() 替代 state == ACTIVE 判断，ACTIVE 优先于 DEPRECATED
- [ ] 4.2 修改 ChannelDomainService：适配 Channel.Phase
- [ ] 4.3 修改 ModelDomainService：适配 Model 无状态字段
- [ ] 4.4 修改 ProviderDomainService：适配 Provider 无状态字段
- [ ] 4.5 修改 ChannelCredentialDomainService：适配无状态字段
- [ ] 4.6 搜索全项目所有引用旧枚举的地方并适配

## 5. 测试

- [ ] 5.1 为 Channel.Phase 编写单元测试（状态转换合法性、isRoutable、isTerminal）
- [ ] 5.2 为 ModelInstance.Phase 编写单元测试
- [ ] 5.3 更新 InstanceSelector 测试

## 6. 数据库迁移

- [ ] 6.1 创建数据库迁移脚本：旧状态值到新 LifecyclePhase 的映射
```

## openspec/changes/supply-lifecycle-enum-refactor/specs/entity-lifecycle/spec.md

- Source: openspec/changes/supply-lifecycle-enum-refactor/specs/entity-lifecycle/spec.md
- Lines: 1-83
- SHA256: 651b46cdc2ae3f1da6302b41a9771bf3ebc887001cefe93c143992c6a5fe730d

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: 实体生命周期阶段定义

供应域实体（Channel、ModelInstance）必须支持五阶段生命周期：PENDING、ACTIVE、SUSPENDED、DEPRECATED、RETIRED。

- PENDING：实体已创建但配置不完整，不得参与路由
- ACTIVE：实体配置完整，正常运行，参与路由
- SUSPENDED：管理员暂停，可恢复，不参与路由
- DEPRECATED：上游标记即将下线，仍参与路由但优先级低于 ACTIVE
- RETIRED：已废弃，不可逆，不参与路由

#### Scenario: 新创建的实体默认为 PENDING

- **WHEN** 一个新的 Channel 或 ModelInstance 被创建
- **THEN** 其 phase 必须默认为 PENDING

#### Scenario: PENDING 实体不参与路由

- **WHEN** InstanceSelector 查询可用模型实例
- **THEN** phase 为 PENDING 的 ModelInstance 不得出现在结果中

#### Scenario: DEPRECATED 实体可路由但优先级低于 ACTIVE

- **WHEN** 存在 phase=ACTIVE 和 phase=DEPRECATED 的 ModelInstance 同时可用
- **THEN** InstanceSelector 必须优先选择 ACTIVE 的实例

### Requirement: 状态转换规则

所有状态转换必须通过 canTransitionTo() 校验，非法转换必须拒绝。

- PENDING → ACTIVE（管理员激活）
- ACTIVE → SUSPENDED（管理员暂停）
- ACTIVE → DEPRECATED（上游标记下线）
- SUSPENDED → ACTIVE（管理员恢复）
- SUSPENDED → DEPRECATED（上游标记下线）
- DEPRECATED → RETIRED（废弃）

#### Scenario: 合法转换通过校验

- **WHEN** 调用 canTransitionTo() 检查合法转换（如 PENDING → ACTIVE）
- **THEN** 返回 true

#### Scenario: 非法转换被拒绝

- **WHEN** 调用 canTransitionTo() 检查非法转换（如 PENDING → RETIRED）
- **THEN** 返回 false

#### Scenario: RETIRED 是终端状态

- **WHEN** 实体 phase 为 RETIRED
- **THEN** isTerminal() 返回 true，canTransitionTo(任何值) 返回 false

### Requirement: 路由可见性判断

实体的路由可见性由 isRoutable() 决定。

- ACTIVE：可路由
- DEPRECATED：可路由（优先级低于 ACTIVE）
- PENDING、SUSPENDED、RETIRED：不可路由

#### Scenario: 路由过滤

- **WHEN** InstanceSelector 或 ChannelGateway 查询可用实体
- **THEN** 只返回 phase.isRoutable() 为 true 的实体

### Requirement: Model 废弃信息

Model 实体必须支持记录上游废弃信息，不依赖状态枚举。

- deprecatedAt：上游标记废弃的时间，null 表示正常
- scheduledRetiredAt：计划下线日期
- deprecationMessage：下线原因或建议迁移目标

#### Scenario: Model 废弃信息不影响路由

- **WHEN** Model 的 deprecatedAt 不为 null
- **THEN** 其参与路由的能力不受影响（由 ModelInstance.phase 决定）

#### Scenario: 管理界面展示废弃标记
```

Full source: openspec/changes/supply-lifecycle-enum-refactor/specs/entity-lifecycle/spec.md

