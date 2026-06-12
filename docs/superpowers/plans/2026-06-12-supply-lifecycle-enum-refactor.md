---
archived-with: 2026-06-12-supply-lifecycle-enum-refactor
status: final
---
# 供应域状态枚举重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 6 个重复状态枚举合并为 Channel.Phase 和 ModelInstance.Phase 两个独立枚举，其他实体去掉状态字段

**Architecture:** Channel 和 ModelInstance 各自持有内部 Phase 枚举（PENDING/ACTIVE/SUSPENDED/DEPRECATED/RETIRED），包含 isRoutable()/isTerminal()/canTransitionTo() 方法。Provider、Model、ChannelCredential、ChannelEndpoint 去掉状态字段。Model 用 deprecation 时间字段代替状态。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, H2/PostgreSQL

**base-ref:** 1d19409c667e48f42d01add8a58db93162a10d8d

---

## 文件映射

### 新增枚举（嵌入实体）
- `Channel.java` — 内部 `enum Phase { PENDING, ACTIVE, SUSPENDED, DEPRECATED, RETIRED }`
- `ModelInstance.java` — 内部 `enum Phase { ... }`（同上，独立类型）

### 修改的实体
- `entity/Channel.java` — `ChannelState state` → `Channel.Phase phase`
- `entity/ModelInstance.java` — `ChannelModelState state` → `ModelInstance.Phase phase`
- `entity/Model.java` — 去掉 `ModelState state`，加 `deprecatedAt`/`scheduledRetiredAt`/`deprecationMessage`
- `entity/Provider.java` — 去掉 `ProviderState state`
- `entity/ChannelCredential.java` — 去掉 `CredentialState state`
- `entity/ChannelEndpoint.java` — 无 state，不变
- `catalog/entity/PlanCatalog.java` — 去掉 `CatalogState state`
- `catalog/entity/PlanModelCatalog.java` — 去掉 `CatalogState state`

### 修改的 DO
- `dataobject/ChannelDo.java` — `ChannelState state` → `String phase`
- `dataobject/ModelInstanceDo.java` — `String state` → `String phase`（字段重命名）
- `dataobject/ProviderDo.java` — `String state` → 直接去掉
- `dataobject/ModelDo.java` — `String state` → 直接去掉
- `dataobject/ChannelCredentialDo.java` — `String state` → 直接去掉

### 修改的 Repository
- `repository/ChannelRepository.java` — `findByState(ChannelState)` → `findByPhase(String)`
- `repository/ModelInstanceRepository.java` — state 查询方法适配 phase 字段名

### 修改的 Gateway 接口
- `gateway/ChannelGateway.java` — `findAllActive()` 保持，内部实现适配
- `gateway/ModelInstanceGateway.java` — state 方法适配
- `gateway/ProviderGateway.java` — `findAllActive()` 保持

### 修改的 Gateway 实现
- `gateway/ChannelGatewayImpl.java` — toEntity/toDo 中 state → phase
- `gateway/ModelInstanceGatewayImpl.java` — 同上
- `gateway/ProviderGatewayImpl.java` — 去掉 state 相关代码
- `gateway/ModelGatewayImpl.java` — 去掉 state 相关代码
- `gateway/ChannelCredentialGatewayImpl.java` — 去掉 state 相关代码

### 修改的应用层
- `application/proxy/routing/InstanceSelector.java` — 适配 phase.isRoutable()
- `application/proxy/routing/ModelMatcher.java` — 去掉 ModelState 判断
- `application/channel/ChannelServiceImpl.java` — 适配 Channel.Phase
- `application/channel/ModelInstanceServiceImpl.java` — 适配 ModelInstance.Phase
- `application/provider/ProviderServiceImpl.java` — 适配无 state
- `application/model/ModelServiceImpl.java` — 适配无 state
- `application/model/ModelDiscoveryService.java` — 适配无 state
- `application/channelcredential/ChannelCredentialServiceImpl.java` — 适配无 state
- `application/catalog/ChannelProvisionService.java` — 适配 CatalogState
- `application/catalog/PlanCatalogServiceImpl.java` — 适配 CatalogState
- `domain/supply/service/ChannelDomainService.java` — 适配 Channel.Phase
- `domain/supply/service/ModelDomainService.java` — 适配无 state
- `domain/supply/service/ProviderDomainService.java` — 适配无 state
- `domain/supply/service/ChannelCredentialDomainService.java` — 适配无 state

### 删除的枚举文件
- `enums/ProviderState.java`
- `enums/ChannelState.java`
- `enums/ModelState.java`
- `enums/ChannelModelState.java`
- `enums/CredentialState.java`
- `catalog/enums/CatalogState.java`

### 修改的测试
- 所有引用旧枚举的测试文件需要适配

### 数据库迁移
- 新建 migration 脚本

---

### Task 1: 在 Channel 和 ModelInstance 中新增 Phase 枚举

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/Channel.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/entity/ModelInstance.java`
- Test: 后续 Task 中编写

- [ ] **Step 1: 在 Channel.java 中新增 Phase 枚举**

在 `Channel.java` 的 `@Slf4j` 注解之前（或在实体字段之前），插入内部枚举：

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

- [ ] **Step 2: 在 ModelInstance.java 中新增 Phase 枚举**

代码与 Step 1 相同，但枚举名是 `ModelInstance.Phase`（独立类型，值相同）。

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat(supply): 新增 Channel.Phase 和 ModelInstance.Phase 枚举"
```

---

### Task 2: 修改实体字段 — state → phase

**Files:**
- Modify: `entity/Channel.java` — 替换 state 字段类型
- Modify: `entity/ModelInstance.java` — 替换 state 字段类型
- Modify: `entity/Model.java` — 去掉 state，加 deprecation 字段
- Modify: `entity/Provider.java` — 去掉 state
- Modify: `entity/ChannelCredential.java` — 去掉 state
- Modify: `catalog/entity/PlanCatalog.java` — 去掉 state
- Modify: `catalog/entity/PlanModelCatalog.java` — 去掉 state

- [ ] **Step 1: 修改 Channel.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.enums.ChannelState;
// ...
private ChannelState state = ChannelState.ACTIVE;

// 修改后
private Phase phase = Phase.PENDING;
```

同时修改 `isAvailable()` 方法：

```java
// 删除 isAvailable() 方法，或改为：
public boolean isAvailable() {
    return phase.isRoutable();
}
```

- [ ] **Step 2: 修改 ModelInstance.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
// ...
private ChannelModelState state = ChannelModelState.ACTIVE;

// 修改后
private Phase phase = Phase.PENDING;
```

- [ ] **Step 3: 修改 Model.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.enums.ModelState;
private ModelState state = ModelState.ACTIVE;

// 修改后（去掉 state 字段，新增 deprecation 字段）
private Instant deprecatedAt;
private Instant scheduledRetiredAt;
private String deprecationMessage;
```

删除 `isAvailable()` 方法。删除 `ModelState` import。

- [ ] **Step 4: 修改 Provider.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.enums.ProviderState;
private ProviderState state = ProviderState.ACTIVE;

// 修改后：删除 state 字段和 isAvailable() 方法
```

- [ ] **Step 5: 修改 ChannelCredential.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.enums.CredentialState;
private CredentialState state = CredentialState.ACTIVE;

// 修改后：删除 state 字段和 isAvailable() 方法
```

- [ ] **Step 6: 修改 PlanCatalog.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
private CatalogState state = CatalogState.ACTIVE;

// 修改后：删除 state 字段
```

- [ ] **Step 7: 修改 PlanModelCatalog.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
private CatalogState state = CatalogState.ACTIVE;

// 修改后：删除 state 字段
```

- [ ] **Step 8: 提交**

```bash
git add -A && git commit -m "refactor(supply): 实体 state 字段迁移为 phase，Model 新增 deprecation 字段"
```

---

### Task 3: 修改 DO 和 Repository

**Files:**
- Modify: `dataobject/ChannelDo.java`
- Modify: `dataobject/ModelInstanceDo.java`
- Modify: `dataobject/ProviderDo.java`
- Modify: `dataobject/ModelDo.java`
- Modify: `dataobject/ChannelCredentialDo.java`
- Modify: `repository/ChannelRepository.java`

- [ ] **Step 1: 修改 ChannelDo.java**

```java
// 修改前
@Enumerated(EnumType.STRING)
@Column(name = "state", nullable = false, length = 32)
private com.codingas.gateway.domain.supply.enums.ChannelState state;

// 修改后
@Column(name = "phase", nullable = false, length = 32)
private String phase;
```

注意：JPA 列名从 `state` 改为 `phase`，需要数据库 migration。

- [ ] **Step 2: 修改 ModelInstanceDo.java**

```java
// 修改前
@Column(name = "state", nullable = false, length = 32)
private String state;

// 修改后
@Column(name = "phase", nullable = false, length = 32)
private String phase;
```

- [ ] **Step 3: 修改 ProviderDo.java**

```java
// 修改前
@Column(name = "state", nullable = false)
private String state;

// 修改后：删除 state 字段
```

- [ ] **Step 4: 修改 ModelDo.java**

```java
// 修改前
@Column(name = "state", nullable = false)
private String state;

// 修改后：删除 state 字段，新增 deprecation 字段
@Column(name = "deprecated_at")
private Instant deprecatedAt;

@Column(name = "scheduled_retired_at")
private Instant scheduledRetiredAt;

@Column(name = "deprecation_message", length = 512)
private String deprecationMessage;
```

- [ ] **Step 5: 修改 ChannelCredentialDo.java**

```java
// 修改前
@Column(name = "state", nullable = false, length = 32)
private String state;

// 修改后：删除 state 字段
```

- [ ] **Step 6: 修改 ChannelRepository.java**

```java
// 修改前
List<ChannelDo> findByState(ChannelState state);

// 修改后
List<ChannelDo> findByPhase(String phase);
```

- [ ] **Step 7: 提交**

```bash
git add -A && git commit -m "refactor(supply): DO 和 Repository 适配 phase 字段变更"
```

---

### Task 4: 修改 Gateway 实现

**Files:**
- Modify: `gateway/ChannelGatewayImpl.java`
- Modify: `gateway/ModelInstanceGatewayImpl.java`
- Modify: `gateway/ProviderGatewayImpl.java`
- Modify: `gateway/ModelGatewayImpl.java`
- Modify: `gateway/ChannelCredentialGatewayImpl.java`

- [ ] **Step 1: 修改 ChannelGatewayImpl.java**

toEntity 中：
```java
// 修改前
entity.setState(doObj.getState());

// 修改后
entity.setPhase(Channel.Phase.valueOf(doObj.getPhase()));
```

toDo 中：
```java
// 修改前
doObj.setState(entity.getState() != null ? entity.getState() : ChannelState.ACTIVE);

// 修改后
doObj.setPhase(entity.getPhase() != null ? entity.getPhase().name() : Channel.Phase.PENDING.name());
```

findAllActive 中：
```java
// 修改前
return channelRepository.findByState(ChannelState.ACTIVE);

// 修改后
return channelRepository.findByPhase(Channel.Phase.ACTIVE.name());
```

- [ ] **Step 2: 修改 ModelInstanceGatewayImpl.java**

toEntity 中：
```java
// 修改前
entity.setState(ChannelModelState.valueOf(doObj.getState()));

// 修改后
entity.setPhase(ModelInstance.Phase.valueOf(doObj.getPhase()));
```

toDo 中：
```java
// 修改前
doObj.setState(entity.getState() != null ? entity.getState().name() : ChannelModelState.ACTIVE.name());

// 修改后
doObj.setPhase(entity.getPhase() != null ? entity.getPhase().name() : ModelInstance.Phase.PENDING.name());
```

所有 `findActiveBy*` 方法改为：
```java
// 修改前
modelInstanceRepository.findByModelIdAndState(modelId, ChannelModelState.ACTIVE.name())

// 修改后
modelInstanceRepository.findByModelIdAndPhase(modelId, ModelInstance.Phase.ACTIVE.name())
```

注意：需要修改 ModelInstanceRepository 中的方法名（从 `findByModelIdAndState` → `findByModelIdAndPhase` 等）。

- [ ] **Step 3: 修改 ProviderGatewayImpl.java**

去掉 state 相关代码。toEntity 和 toDo 中删除 state 行：

```java
// toEntity 删除：
entity.setState(ProviderState.valueOf(doObj.getState()));

// toDo 删除：
doObj.setState(entity.getState() != null ? entity.getState().name() : ProviderState.ACTIVE.name());
```

findAllActive 需要修改——如果没有 state 字段，改为查询所有或使用其他逻辑：
```java
// 简单方案：返回所有 Provider（Provider 本身不控制启用/禁用）
@Override
public List<Provider> findAllActive() {
    return findAll();
}
```

- [ ] **Step 4: 修改 ModelGatewayImpl.java**

toEntity 中：
```java
// 修改前
entity.setState(ModelState.valueOf(doObj.getState()));

// 修改后：删除 state 行，新增 deprecation 字段映射
entity.setDeprecatedAt(doObj.getDeprecatedAt());
entity.setScheduledRetiredAt(doObj.getScheduledRetiredAt());
entity.setDeprecationMessage(doObj.getDeprecationMessage());
```

toDo 中：
```java
// 修改前
doObj.setState(entity.getState() != null ? entity.getState().name() : ModelState.ACTIVE.name());

// 修改后：删除 state 行，新增 deprecation 字段映射
doObj.setDeprecatedAt(entity.getDeprecatedAt());
doObj.setScheduledRetiredAt(entity.getScheduledRetiredAt());
doObj.setDeprecationMessage(entity.getDeprecationMessage());
```

findAllActive 和 findActiveByModelName —— 不再按 state 过滤，改为返回全部：
```java
@Override
public List<Model> findAllActive() {
    return findAll(); // Model 本身不再有状态，由 ModelInstance 控制
}
```

- [ ] **Step 5: 修改 ChannelCredentialGatewayImpl.java**

toEntity 中：
```java
// 修改前
entity.setState(CredentialState.valueOf(doObj.getState()));

// 修改后：删除 state 行
```

toDo 中：
```java
// 修改前
doObj.setState(entity.getState() != null ? entity.getState().name() : CredentialState.ACTIVE.name());

// 修改后：删除 state 行
```

findActiveByChannelId —— 去掉 state 过滤，返回所有：
```java
@Override
public List<ChannelCredential> findActiveByChannelId(Long channelId) {
    return findByChannelId(channelId); // 不再按 state 过滤
}
```

- [ ] **Step 6: 提交**

```bash
git add -A && git commit -m "refactor(supply): Gateway 实现适配 phase 和无 state 实体"
```

---

### Task 5: 修改 Gateway 接口

**Files:**
- Modify: `gateway/ChannelGateway.java`
- Modify: `gateway/ModelInstanceGateway.java`
- Modify: `gateway/ProviderGateway.java`
- Modify: `gateway/ModelGateway.java`（可能）
- Modify: `gateway/ChannelCredentialGateway.java`

- [ ] **Step 1: 修改 ChannelGateway.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.enums.ChannelState;

// 修改后：删除 ChannelState import
```

`findAllActive()` 方法签名保持，但实现改为按 phase 查询。

- [ ] **Step 2: 修改 ModelInstanceGateway.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
List<ModelInstance> findByChannelIdAndState(Long channelId, ChannelModelState state);

// 修改后：删除 ChannelModelState import
// 方法改为：
List<ModelInstance> findByChannelIdAndPhase(Long channelId, String phase);
```

`findActiveByModelIdOrderByPriority` 方法签名保持，内部实现改为按 phase 查询。

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "refactor(supply): Gateway 接口适配 phase 变更"
```

---

### Task 6: 修改领域服务

**Files:**
- Modify: `domain/supply/service/ChannelDomainService.java`
- Modify: `domain/supply/service/ModelDomainService.java`
- Modify: `domain/supply/service/ProviderDomainService.java`
- Modify: `domain/supply/service/ChannelCredentialDomainService.java`

- [ ] **Step 1: 修改 ChannelDomainService.java**

```java
// 修改前
channel.setState(ChannelState.ACTIVE);
channel.setState(ChannelState.INACTIVE);
// 使用 channel.isAvailable()

// 修改后
channel.setPhase(Channel.Phase.ACTIVE);
channel.setPhase(Channel.Phase.SUSPENDED);
// 使用 channel.getPhase().isRoutable()
```

- [ ] **Step 2: 修改 ModelDomainService.java**

去掉 ModelState 引用。enable/disable 方法改为操作 deprecation 字段或不操作。

- [ ] **Step 3: 修改 ProviderDomainService.java**

去掉 ProviderState 引用。enable/disable 方法如果只是操作 state 字段，可以删除或保留为空操作。

- [ ] **Step 4: 修改 ChannelCredentialDomainService.java**

去掉 CredentialState 引用。enable/disable 方法可以删除或保留为空操作。

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "refactor(supply): 领域服务适配 phase 和无 state 实体"
```

---

### Task 7: 修改应用层服务

**Files:**
- Modify: `application/proxy/routing/InstanceSelector.java`
- Modify: `application/proxy/routing/ModelMatcher.java`
- Modify: `application/proxy/routing/PermissionRouter.java`
- Modify: `application/channel/ChannelServiceImpl.java`
- Modify: `application/channel/ModelInstanceServiceImpl.java`
- Modify: `application/provider/ProviderServiceImpl.java`
- Modify: `application/model/ModelServiceImpl.java`
- Modify: `application/model/ModelDiscoveryService.java`
- Modify: `application/channelcredential/ChannelCredentialServiceImpl.java`
- Modify: `application/catalog/ChannelProvisionService.java`
- Modify: `application/catalog/PlanCatalogServiceImpl.java`
- Modify: `application/init/BuiltinVendorLoader.java`

- [ ] **Step 1: 修改 InstanceSelector.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.enums.ChannelState;
// ...
.filter(ch -> ch.getState() == ChannelState.ACTIVE)

// 修改后
// 删除 ChannelState import
.filter(ch -> ch.getPhase() != null && ch.getPhase().isRoutable())
```

同时修改优先级排序逻辑，让 ACTIVE 优先于 DEPRECATED：

```java
// 在 findActiveByModelIdOrderByPriority 之后，添加排序
List<ModelInstance> sorted = modelInstances.stream()
    .sorted(Comparator
        .comparingInt((ModelInstance mi) ->
            mi.getPhase() == ModelInstance.Phase.ACTIVE ? 0 : 1)
        .thenComparingInt(ModelInstance::getPriority))
    .toList();
```

- [ ] **Step 2: 修改 ModelMatcher.java**

```java
// 修改前
import com.codingas.gateway.domain.supply.enums.ModelState;
// ...
.filter(m -> m.getState() == ModelState.ACTIVE)

// 修改后
// 删除 ModelState import，去掉 state 过滤
// Model 不再有状态，直接返回匹配的 model
public Model match(String modelName) {
    return modelGateway.findByModelName(modelName)
            .orElseThrow(() -> new ResourceNotFoundException("Model", modelName));
}
```

- [ ] **Step 3: 修改 PermissionRouter.java**

查看文件中 ChannelState 的引用并适配。

- [ ] **Step 4: 修改 ChannelServiceImpl.java、ModelInstanceServiceImpl.java**

搜索并替换所有 state → phase 引用。

- [ ] **Step 5: 修改其他应用层文件**

逐个修改 ProviderServiceImpl、ModelServiceImpl、ModelDiscoveryService、ChannelCredentialServiceImpl、ChannelProvisionService、PlanCatalogServiceImpl、BuiltinVendorLoader。

- [ ] **Step 6: 提交**

```bash
git add -A && git commit -m "refactor(supply): 应用层适配 phase 和无 state 实体"
```

---

### Task 8: 删除旧枚举文件

**Files:**
- Delete: `enums/ProviderState.java`
- Delete: `enums/ChannelState.java`
- Delete: `enums/ModelState.java`
- Delete: `enums/ChannelModelState.java`
- Delete: `enums/CredentialState.java`
- Delete: `catalog/enums/CatalogState.java`

- [ ] **Step 1: 确认没有遗漏引用**

```bash
# 搜索是否还有引用这些枚举的地方
grep -r "ProviderState" --include="*.java" gateway-boot/src/
grep -r "ChannelState" --include="*.java" gateway-boot/src/
grep -r "ModelState" --include="*.java" gateway-boot/src/
grep -r "ChannelModelState" --include="*.java" gateway-boot/src/
grep -r "CredentialState" --include="*.java" gateway-boot/src/
grep -r "CatalogState" --include="*.java" gateway-boot/src/
```

- [ ] **Step 2: 删除文件**

```bash
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ProviderState.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ChannelState.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ModelState.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/ChannelModelState.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/enums/CredentialState.java
rm gateway-boot/src/main/java/com/codingas/gateway/domain/supply/catalog/enums/CatalogState.java
```

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "refactor(supply): 删除 6 个旧状态枚举文件"
```

---

### Task 9: 修改测试文件

**Files:**
- Modify: 所有引用旧枚举的测试文件

- [ ] **Step 1: 搜索并修改测试文件**

```bash
grep -r "ProviderState\|ChannelState\|ModelState\|ChannelModelState\|CredentialState\|CatalogState" --include="*.java" gateway-boot/src/test/
```

逐个修改测试文件：
- `PermissionRouterTest.java` — 适配 phase
- `ChannelCredentialServiceImplTest.java` — 去掉 state
- `ChannelCredentialControllerTest.java` — 去掉 state
- `ModelDiscoveryServiceTest.java` — 适配
- `ModelServiceTest.java` — 适配
- `ModelControllerTest.java` — 适配
- `CredentialResolverTest.java` — 适配
- `ModelMatcherTest.java` — 适配
- `PlanCatalogTest.java` — 去掉 CatalogState
- `PlanModelCatalogTest.java` — 去掉 CatalogState
- `CatalogStateTest.java` — 删除

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "test(supply): 测试适配 phase 和无 state 实体"
```

---

### Task 10: 编写单元测试

**Files:**
- Create: 新增 Channel.Phase 和 ModelInstance.Phase 的测试类

- [ ] **Step 1: 为 Channel.Phase 编写单元测试**

在 `domain/supply/entity/` 对应的测试位置：

```java
class ChannelPhaseTest {

    @Test
    void pendingShouldNotBeRoutable() {
        assertFalse(Channel.Phase.PENDING.isRoutable());
    }

    @Test
    void activeShouldBeRoutable() {
        assertTrue(Channel.Phase.ACTIVE.isRoutable());
    }

    @Test
    void suspendedShouldNotBeRoutable() {
        assertFalse(Channel.Phase.SUSPENDED.isRoutable());
    }

    @Test
    void deprecatedShouldBeRoutable() {
        assertTrue(Channel.Phase.DEPRECATED.isRoutable());
    }

    @Test
    void retiredShouldNotBeRoutable() {
        assertFalse(Channel.Phase.RETIRED.isRoutable());
    }

    @Test
    void retiredIsTerminal() {
        assertTrue(Channel.Phase.RETIRED.isTerminal());
    }

    @Test
    void nonRetiredIsNotTerminal() {
        assertFalse(Channel.Phase.ACTIVE.isTerminal());
        assertFalse(Channel.Phase.PENDING.isTerminal());
        assertFalse(Channel.Phase.SUSPENDED.isTerminal());
        assertFalse(Channel.Phase.DEPRECATED.isTerminal());
    }

    @Test
    void pendingCanTransitionToActive() {
        assertTrue(Channel.Phase.PENDING.canTransitionTo(Channel.Phase.ACTIVE));
    }

    @Test
    void pendingCannotTransitionToSuspended() {
        assertFalse(Channel.Phase.PENDING.canTransitionTo(Channel.Phase.SUSPENDED));
    }

    @Test
    void activeCanTransitionToSuspended() {
        assertTrue(Channel.Phase.ACTIVE.canTransitionTo(Channel.Phase.SUSPENDED));
    }

    @Test
    void activeCanTransitionToDeprecated() {
        assertTrue(Channel.Phase.ACTIVE.canTransitionTo(Channel.Phase.DEPRECATED));
    }

    @Test
    void suspendedCanTransitionToActive() {
        assertTrue(Channel.Phase.SUSPENDED.canTransitionTo(Channel.Phase.ACTIVE));
    }

    @Test
    void deprecatedCanTransitionToRetired() {
        assertTrue(Channel.Phase.DEPRECATED.canTransitionTo(Channel.Phase.RETIRED));
    }

    @Test
    void retiredCannotTransitionToAny() {
        assertFalse(Channel.Phase.RETIRED.canTransitionTo(Channel.Phase.ACTIVE));
        assertFalse(Channel.Phase.RETIRED.canTransitionTo(Channel.Phase.PENDING));
        assertFalse(Channel.Phase.RETIRED.canTransitionTo(Channel.Phase.SUSPENDED));
        assertFalse(Channel.Phase.RETIRED.canTransitionTo(Channel.Phase.DEPRECATED));
        assertFalse(Channel.Phase.RETIRED.canTransitionTo(Channel.Phase.RETIRED));
    }
}
```

- [ ] **Step 2: 为 ModelInstance.Phase 编写单元测试**

同上，类型改为 `ModelInstance.Phase`。

- [ ] **Step 3: 运行测试**

```bash
cd gateway-boot && ../mvnw test -pl gateway-boot -Dtest="*Phase*" -DfailIfNoTests=false
```

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "test(supply): 新增 Phase 枚举单元测试"
```

---

### Task 11: 构建验证

- [ ] **Step 1: 编译项目**

```bash
cd gateway-boot && ../mvnw compile -pl gateway-boot
```

- [ ] **Step 2: 运行所有测试**

```bash
cd gateway-boot && ../mvnw test -pl gateway-boot
```

- [ ] **Step 3: 修复编译错误和测试失败**

- [ ] **Step 4: 提交最终修复**

```bash
git add -A && git commit -m "fix(supply): 修复编译错误和测试"
```

---

### Task 12: 数据库迁移脚本

- [ ] **Step 1: 创建迁移脚本**

在 `gateway-boot/src/main/resources/db/migration/` 下创建：

```sql
-- VXX__supply_lifecycle_phase_migration.sql

-- 1. Channel: state → phase，值映射 ACTIVE→ACTIVE, INACTIVE→SUSPENDED
ALTER TABLE channels ADD COLUMN phase VARCHAR(32);
UPDATE channels SET phase = 'ACTIVE' WHERE state = 'ACTIVE';
UPDATE channels SET phase = 'SUSPENDED' WHERE state = 'INACTIVE';
ALTER TABLE channels DROP COLUMN state;
ALTER TABLE channels ALTER COLUMN phase SET NOT NULL;

-- 2. ModelInstance: state → phase，值映射 ACTIVE→ACTIVE, INACTIVE→SUSPENDED
ALTER TABLE model_instances ADD COLUMN phase VARCHAR(32);
UPDATE model_instances SET phase = 'ACTIVE' WHERE state = 'ACTIVE';
UPDATE model_instances SET phase = 'SUSPENDED' WHERE state = 'INACTIVE';
ALTER TABLE model_instances DROP COLUMN state;
ALTER TABLE model_instances ALTER COLUMN phase SET NOT NULL;

-- 3. Provider: 删除 state 列
ALTER TABLE providers DROP COLUMN state;

-- 4. Model: 删除 state 列，新增 deprecation 字段
ALTER TABLE models DROP COLUMN state;
ALTER TABLE models ADD COLUMN deprecated_at TIMESTAMP;
ALTER TABLE models ADD COLUMN scheduled_retired_at TIMESTAMP;
ALTER TABLE models ADD COLUMN deprecation_message VARCHAR(512);

-- 5. ChannelCredential: 删除 state 列
ALTER TABLE channel_credentials DROP COLUMN state;
```
