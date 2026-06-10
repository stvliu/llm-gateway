# DataInitializer 完善方案

## 背景

当前 `DataInitializer` 存在三个问题：

1. **守卫条件错误**：`run()` 使用 `providerGateway.count() > 0` 判断是否已初始化。但 `BuiltinDataLoader`（`@Order(1)`）在 DataInitializer 之前执行，已将供应商数据从 JSON 加载到 `providers` 表，导致该条件永远为真，DataInitializer 实际从未执行过。
2. **职责混杂**：admin 内置用户属于基础设施数据（生产环境也需要），与 test1~test10 等演示数据混在一起。
3. **无环境隔离**：类上无 `@Profile` 注解，理论上可在生产环境执行。

## 设计目标

- 修复守卫条件，使 DataInitializer 正确执行
- 将 admin 内置用户与演示数据分离
- 通过配置属性控制演示数据的启用/禁用
- 保留 provider/model 创建作为 BuiltinDataLoader 的后备

## 设计方案

### 2.1 配置属性

利用现有的 `GatewayProperties` 模式新增 `InitProperties`。默认 `false`，开发环境 profile 显式开启。

```yaml
# GatewayProperties.InitProperties
gateway:
  init:
    demo-data-enabled: false    # 安全默认
```

```yaml
# application-local.yml / application-dev.yml
gateway:
  init:
    demo-data-enabled: true
```

### 2.2 DataInitializer.run() 三阶段执行

```
Phase 1 — 基础设施（无守卫）
  ensureAdminUser()
    → userGateway.findByUsername("admin").isPresent() ? 跳过 : 创建

Phase 2 — 演示开关
  demoDataEnabled == false → 日志记录，return

Phase 3 — 幂等守卫
  userGateway.findByUsername("test1").isPresent() → 已初始化，跳过

Phase 4 — 初始化执行
  ├─ 后备：providerGateway.count() == 0 ? 创建供应商 + 模型
  ├─ initializeChannels()          → 12 个渠道 + 端点 + 凭证
  ├─ initializeTeams()             → 4 个团队
  ├─ initializeTeamChannelAssignments()
  ├─ initializeDemoUsers()         → test1 ~ test10
  ├─ initializeUserTeamAssignments()
  └─ initializeApiKeys()
```

### 2.3 方法拆分

| 原方法 | 改造后 | 变化 |
|--------|--------|------|
| `initializeUsers()` | → `ensureAdminUser()` + `initializeDemoUsers()` | admin 拆分出，test 用户单独 |
| 其余方法 | 保留不变 | — |

### 2.4 事务边界

整个 `run()` 保持 `@Transactional`，三阶段在同一事务中执行。ensureAdminUser 成功后如果阶段 2-4 失败，事务回滚，不产生脏数据。

### 2.5 Demo 数据幂等守卫

选择 `userGateway.findByUsername("test1").isPresent()` 作为守卫条件：

- test1 纯属演示数据，与基础设施无关
- 相比查 team/channel，用户维度更直接反映 DataInitializer 的执行状态

## 涉及文件

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `GatewayProperties.java` | 修改 | 新增 `InitProperties` 内部类 |
| `application.yml` | 修改 | 添加 `gateway.init.demo-data-enabled: false` 默认值 |
| `application-local.yml` | 修改 | 添加 `gateway.init.demo-data-enabled: true` |
| `application-dev.yml` | 修改 | 添加 `gateway.init.demo-data-enabled: true` |
| `DataInitializer.java` | 修改 | 重构 run() 方法，拆分 ensureAdminUser + initializeDemoUsers |

## 执行矩阵

| 场景 | 配置 | Admin 是否创建 | Demo 数据是否创建 |
|------|------|---------------|------------------|
| 首次启动 local | `demo-data-enabled=true` | 是 | 是 |
| 重启 local（已有数据） | `demo-data-enabled=true` | 已存在，跳过 | test1 存在，跳过 |
| 首次启动 dev | `demo-data-enabled=true` | 是 | 是 |
| 首次启动 prod+postgresql | `demo-data-enabled=false`(默认) | 是 | 否 |
| prod 首次部署空库 | `demo-data-enabled=false` | 是 | 否 |