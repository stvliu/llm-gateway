# 数据面授权简化（认证即授权）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 数据面授权简化为"认证即授权"：`PermissionRouter`/`LoadBalanceRouter` 移除、实例查询带渠道配置条件（DB 层过滤）、`RouterChain` 收敛为纯路由（Priority+Health）、`RoutingRequest` 清理 `userId`/`role` 透传、体验中心授权补强（渠道归属校验 + 临时配置限 ADMIN）。

**Architecture:** 授权蕴含在认证（`Identity.applicationId` 即 Key 绑定的应用资源），渠道/实例是应用配置非授权。`InstanceSelector` 前移配置读取（应用→活跃渠道→实例查询带条件），`RouterChain` 只做路由（优先级/健康）。体验中心补强：`ModelExperienceService` 校验体验渠道 ∈ 用户任一应用的应用渠道配置，临时配置（apiKey/baseUrl 直连）仅 ADMIN。

**Tech Stack:** Java 21 + Spring Boot 3.5 + JPA 派生查询 + JUnit 5 + Mockito + AssertJ

## Global Constraints

- 行为等价：候选实例集合与现状一致（授权渠道过滤 + 活跃渠道/实例过滤语义不变）；空候选 → `ResourceNotFoundException`
- `InstanceSelector.select` 签名不变（modelId, applicationId, userId, role, strategy, protocol）——`ChatDispatchServiceImpl` 零改动（userId/role 参数保留但不再透传至 RoutingRequest）
- 分层：iam 不依赖 provider；活跃渠道过滤（`isRoutable`）在 proxy 层（`InstanceSelector`）
- 删除：`PermissionRouter`（+测试，场景迁移 InstanceSelectorTest）、`LoadBalanceRouter`、`AuthorizationService.permittedChannelIds`（+测试）
- 体验中心：渠道归属 = `user_api_keys.user_id` → 应用集合 → `application_channel` 渠道集合并集，channelId 命中任一放行；临时配置（`useSavedConfig=false`）仅 ADMIN；校验在 `chatStream` 入口同步执行（executor 提交前），抛 `IllegalArgumentException` 由 `ExperienceController` 捕获转 403
- 模块坐标：`gateway-provider/provider`、`gateway-provider/provider-data`、`gateway-proxy/proxy`、`gateway-web`；测试命令 `mvn -q -pl <module> -am test`（`-Dtest=` 筛选时追加 `-Dsurefire.failIfNoSpecifiedTests=false`）
- 代码注释、Javadoc、commit message 一律中文（项目语言规范）
- Windows Git Bash：mvn 报 `NoDefaultCurrentDirectoryInExePath` 时用 `env -u NoDefaultCurrentDirectoryInExePath mvn ...` 重试

---

### Task 1: ModelInstanceRepository 渠道集合查询

**Files:**
- Modify: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelInstanceRepository.java`
- Modify: `gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/model/JpaModelInstanceRepository.java`
- Test: `gateway-provider/provider-data/src/test/java/com/codingas/gateway/providerdata/model/JpaModelInstanceRepositoryTest.java`

**Interfaces:**
- Consumes: `ModelInstance`（State 字段）、`JpaModelInstanceRepository` 的 `findByModelIdAndStateInOrderByPriorityAsc`（现有派生方法）、`ROUTABLE_STATES` 常量
- Produces: `List<ModelInstance> findActiveByModelIdAndChannelIds(Long modelId, Collection<Long> channelIds)`——Task 2 的 `InstanceSelector` 消费

- [ ] **Step 1: 写失败测试**（追加到 `JpaModelInstanceRepositoryTest`）

```java
    @Test
    @DisplayName("findActiveByModelIdAndChannelIds：按渠道集合过滤活跃实例，按优先级升序")
    void findActiveByModelIdAndChannelIds_filtersByChannels() {
        // 现有测试基类已提供 save/清理机制——实现者按现有测试文件的实际 setup 形态
        // （如 @BeforeEach 清表 + ModelInstance builder）构造 3 条数据：
        //   mi1: modelId=10, channelId=100, state=ACTIVE, priority=1
        //   mi2: modelId=10, channelId=200, state=ACTIVE, priority=2
        //   mi3: modelId=10, channelId=300, state=INACTIVE（或不可路由状态）, priority=3
        // 断言：findActiveByModelIdAndChannelIds(10L, List.of(100L, 200L, 300L))
        //   → 仅含 mi1（渠道过滤：200/300 不在集合 → 不含；活跃过滤：mi3 不可路由 → 不含）
    }

    @Test
    @DisplayName("findActiveByModelIdAndChannelIds：空渠道集合返回空列表")
    void findActiveByModelIdAndChannelIds_emptyChannels_returnsEmpty() {
        assertThat(repository.findActiveByModelIdAndChannelIds(10L, List.of())).isEmpty();
    }
```

注：实现者先读取 `JpaModelInstanceRepositoryTest` 现有 setup 形态（save 辅助、清表机制、`ROUTABLE_STATES` 中可路由状态枚举），按同形态写断言。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-provider/provider-data -am test -Dtest=JpaModelInstanceRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败 `cannot find symbol: method findActiveByModelIdAndChannelIds`

- [ ] **Step 3: 最小实现**

`ModelInstanceRepository` 接口新增：

```java
    /**
     * 按模型与渠道集合查询活跃实例（按 priority 升序）
     *
     * <p>数据面路由前查询：渠道集合来自应用渠道配置（认证即授权下的配置读取），
     * DB 层过滤替代全量拉取 + 内存过滤。</p>
     *
     * @param modelId    模型 ID
     * @param channelIds 渠道 ID 集合（应用配置的可见渠道）
     * @return 活跃实例列表（priority 升序）
     */
    List<ModelInstance> findActiveByModelIdAndChannelIds(Long modelId, Collection<Long> channelIds);
```

`JpaModelInstanceRepository` 实现（仿 `findActiveByModelIdOrderByPriority` 模式）：

```java
    @Override
    public List<ModelInstance> findActiveByModelIdAndChannelIds(Long modelId, Collection<Long> channelIds) {
        return modelInstanceRepository
                .findByModelIdAndChannelIdInAndStateInOrderByPriorityAsc(modelId, channelIds, ROUTABLE_STATES)
                .stream().map(this::toEntity).toList();
    }
```

注：`findByModelIdAndChannelIdInAndStateInOrderByPriorityAsc` 为 Spring Data 派生方法（需在 JPA 实体仓储接口存在——若不存在需在实体仓储接口声明；实现者按现有 `findByModelIdAndStateInOrderByPriorityAsc` 的声明位置同模式处理）。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-provider/provider-data -am test -Dtest=JpaModelInstanceRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（2 个新用例 + 现有用例全绿）

- [ ] **Step 5: 提交**

```bash
git add gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelInstanceRepository.java gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/model/JpaModelInstanceRepository.java gateway-provider/provider-data/src/test/java/com/codingas/gateway/providerdata/model/JpaModelInstanceRepositoryTest.java
git commit -m "feat(provider): 新增按模型与渠道集合查询活跃实例方法"
```

---

### Task 2: InstanceSelector 改造 + 删除 PermissionRouter/LoadBalanceRouter

**Files:**
- Modify: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/InstanceSelector.java`
- Delete: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/PermissionRouter.java`
- Delete: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/LoadBalanceRouter.java`
- Delete: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/routing/PermissionRouterTest.java`
- Modify: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/routing/InstanceSelectorTest.java`
- Modify: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/AuthorizationService.java`（删除 `permittedChannelIds` 方法、`ApplicationChannelRepository` 构造器依赖与 import）
- Modify: `gateway-iam/iam/src/test/java/com/codingas/gateway/iam/auth/AuthorizationServiceTest.java`（删除 `permittedChannelIds` 相关 2 个用例与 `@Mock ApplicationChannelRepository` stub；`setUp` 构造器同步）

**Interfaces:**
- Consumes: Task 1 的 `findActiveByModelIdAndChannelIds`；现有 `ApplicationChannelRepository.findChannelIdsByApplicationId(Long)`、`ChannelRepository.findByIds(List<Long>)`、`RouterChain`
- Produces: `InstanceSelector.select` 签名不变，内部改为「配置读取 → 活跃渠道过滤 → 实例查询带条件 → RouterChain 纯路由」

- [ ] **Step 1: 改写失败测试**（InstanceSelectorTest 场景迁移 + 新增）

关键点：现测试 mock `ModelInstanceRepository` + `RouterChain` + `ApplicationChannelRepository`；改造后 `InstanceSelector` 构造器新增 `ChannelRepository`。测试场景：

```java
    @Test
    @DisplayName("select：按应用渠道配置过滤实例（认证即授权，配置读取前移）")
    void select_filtersByApplicationChannels() {
        // mock：applicationChannelRepository.findChannelIdsByApplicationId(100L) → Set.of(1L)
        // mock：channelRepository.findByIds(List.of(1L)) → 活跃渠道（state.isRoutable true）
        // mock：modelInstanceRepository.findActiveByModelIdAndChannelIds(10L, Set.of(1L)) → List.of(mi1)
        // mock：routerChain.filter(...) → List.of(mi1)
        // 断言 select(10L, 100L, 1L, "USER", WEIGHTED, protocol) 返回 [mi1]
    }

    @Test
    @DisplayName("select：应用无渠道配置（空集合）→ ResourceNotFoundException")
    void select_emptyChannelConfig_throws() {
        // mock：findChannelIdsByApplicationId → Set.of()
        // 断言 assertThatThrownBy(...).isInstanceOf(ResourceNotFoundException.class)
    }

    @Test
    @DisplayName("select：无活跃实例（查询空）→ ResourceNotFoundException")
    void select_noActiveInstances_throws() {
        // 渠道配置 Set.of(1L)，活跃渠道，findActiveByModelIdAndChannelIds → List.of()
        // 断言 ResourceNotFoundException
    }
```

实现者先读取 `InstanceSelectorTest` 现有全部场景（候选列表/无活跃实例/链后无候选/applicationId+protocol 透传），迁移授权相关断言、补充上述场景，删除依赖 `PermissionRouter` 的用例。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-proxy/proxy -am test -Dtest=InstanceSelectorTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（构造器参数不匹配 / 方法不存在）

- [ ] **Step 3: 修改实现**

`InstanceSelector` 改造：

```java
@Component
@RequiredArgsConstructor
public class InstanceSelector {

    private static final Logger log = LoggerFactory.getLogger(InstanceSelector.class);

    private final ModelInstanceRepository modelInstanceRepository;
    private final RouterChain routerChain;
    /** 应用-渠道配置仓储（认证即授权：配置读取，非授权判定） */
    private final ApplicationChannelRepository applicationChannelRepository;
    /** 渠道仓储（活跃渠道过滤——ABAC 属性，proxy 层执行） */
    private final ChannelRepository channelRepository;

    /**
     * 根据 modelId 和用户身份选择模型实例候选列表
     *
     * <p>数据面授权已蕴含在认证（{@code Identity.applicationId} 即 Key 绑定的应用资源），
     * 本方法读取应用的渠道配置并过滤活跃渠道，带条件查询实例，再经 {@link RouterChain}
     * 纯路由（优先级/健康）返回候选列表。</p>
     *
     * @param modelId       模型 ID
     * @param applicationId 应用 ID（数据面权限锚点，认证产物）
     * @param userId        用户 ID（保留签名兼容，不再透传路由）
     * @param role          用户角色（保留签名兼容，不再透传路由）
     * @param strategy      路由策略
     * @param protocol      入站协议（透传至 RoutingRequest 供 HealthRouter 派生 endpointId）
     * @return 按优先级升序的候选实例列表
     * @throws ResourceNotFoundException 无可用实例
     */
    public List<ModelInstance> select(Long modelId, Long applicationId, Long userId, String role,
                                      RoutingStrategy strategy, Protocol protocol) {
        // 1. 应用渠道配置读取（配置，非授权）
        Set<Long> configuredChannelIds = getConfiguredChannelIds(applicationId);
        if (configuredChannelIds.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 2. 活跃渠道过滤（ABAC 属性：state.isRoutable）
        Set<Long> activeChannelIds = filterRoutableChannels(configuredChannelIds);
        if (activeChannelIds.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 3. 实例查询（DB 层过滤：模型 + 活跃渠道 + 活跃实例）
        List<ModelInstance> candidates = modelInstanceRepository
                .findActiveByModelIdAndChannelIds(modelId, activeChannelIds);
        if (candidates.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 4. 纯路由链（优先级/健康）
        Map<Long, Integer> channelPriorityMap = buildChannelPriorityMap(applicationId);
        RoutingRequest request = new RoutingRequest(modelId, applicationId, strategy, protocol, channelPriorityMap);
        List<ModelInstance> result = routerChain.filter(candidates, request);

        if (result.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }
        return result;
    }

    /** 应用渠道配置读取（applicationId 为 null → 空集） */
    private Set<Long> getConfiguredChannelIds(Long applicationId) {
        if (applicationId == null) {
            return Set.of();
        }
        return applicationChannelRepository.findChannelIdsByApplicationId(applicationId);
    }

    /** 活跃渠道过滤（state.isRoutable()） */
    private Set<Long> filterRoutableChannels(Set<Long> channelIds) {
        return channelRepository.findByIds(List.copyOf(channelIds)).stream()
                .filter(ch -> ch.getState() != null && ch.getState().isRoutable())
                .map(Channel::getId)
                .collect(Collectors.toSet());
    }

    /** 构建应用级渠道优先级映射（PriorityRouter 消费） */
    private Map<Long, Integer> buildChannelPriorityMap(Long applicationId) {
        if (applicationId == null) {
            return Map.of();
        }
        List<ApplicationChannel> channels = applicationChannelRepository.findByApplicationId(applicationId);
        Map<Long, Integer> map = new LinkedHashMap<>();
        for (ApplicationChannel channel : channels) {
            if (channel.getPriority() != null) {
                map.put(channel.getChannelId(), channel.getPriority());
            }
        }
        return map;
    }
}
```

新增 import：`java.util.Set`、`java.util.stream.Collectors`、`com.codingas.gateway.provider.channel.Channel`、`com.codingas.gateway.provider.channel.ChannelRepository`（现有 import 保留：ApplicationChannel/ApplicationChannelRepository/ModelInstance/ModelInstanceRepository/ResourceNotFoundException/Protocol/RoutingStrategy/RoutingRequest/RouterChain）。

- [ ] **Step 4: 删除 PermissionRouter/LoadBalanceRouter、permittedChannelIds 及其测试**

```bash
git rm gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/PermissionRouter.java
git rm gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/LoadBalanceRouter.java
git rm gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/routing/PermissionRouterTest.java
```

删除前 `grep -rl "PermissionRouter\|LoadBalanceRouter" gateway-proxy/proxy/src --include="*.java"` 确认无其他引用（RouterChain 自动装配按 order 收敛，无需改）。

`AuthorizationService` 删除（编辑方式）：删除 `permittedChannelIds` 方法、`ApplicationChannelRepository` 字段与构造器参数、相关 import（`java.util.Set` 若仅该方法使用）、方法 Javadoc；`AuthorizationServiceTest` 同步删除 `permittedChannelIds` 两个用例（`permittedChannelIds_delegates`/`permittedChannelIds_nullApplicationId_returnsEmpty`）、`@Mock ApplicationChannelRepository` 字段与 `setUp` 构造器参数。

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn -q -pl gateway-proxy/proxy,gateway-iam/iam -am test`
Expected: PASS（InstanceSelectorTest 全绿 + AuthorizationServiceTest 收敛后全绿 + 两模块无回归）

- [ ] **Step 6: 提交**

```bash
git add gateway-proxy/proxy/src gateway-iam/iam/src
git commit -m "refactor(proxy,iam): 数据面认证即授权——配置读取前移、路由链收敛纯路由、删除 permittedChannelIds"
```

---

### Task 3: RoutingRequest 字段清理（移除 userId/role 透传）

**Files:**
- Modify: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/RoutingRequest.java`
- Modify: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/routing/HealthRouterTest.java`
- Modify: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/routing/PriorityRouterTest.java`
- Modify: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/routing/RouterChainTest.java`

**Interfaces:**
- Consumes: Task 2 的 `InstanceSelector`（构造 `RoutingRequest` 的调用点）；现有 Router 消费（`getProtocol`/`getChannelPriorityMap`/`getApplicationId`/`getModelId`）
- Produces: `RoutingRequest(Long modelId, Long applicationId, RoutingStrategy strategy, Protocol protocol, Map<Long,Integer> channelPriorityMap)`（移除 userId/role 字段与旧构造器）

- [ ] **Step 1: 改写失败测试**

`HealthRouterTest`/`PriorityRouterTest`/`RouterChainTest` 中 `new RoutingRequest(...)` 调用改为新 5 参构造器。先读取三个测试文件，将形如 `new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED)`（5 参）或 `new RoutingRequest(1L, 1L, 1L, "USER", strategy, protocol, map)`（7 参）的调用改为：

```java
new RoutingRequest(1L, 1L, RoutingStrategy.WEIGHTED, protocol, channelPriorityMap)
```

（protocol/channelPriorityMap 按各测试实际需要传 `null` 或具体值——以测试场景为准；`RoutingRequest` 新构造器参数顺序为 modelId, applicationId, strategy, protocol, channelPriorityMap。）

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-proxy/proxy -am test -Dtest=HealthRouterTest,PriorityRouterTest,RouterChainTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（构造器参数不匹配）

- [ ] **Step 3: 修改实现**

`RoutingRequest` 清理（移除 `userId`/`role` 字段、getter 与全部旧构造器，保留新 5 参构造器）：

```java
public class RoutingRequest {

    private final Long modelId;
    private final Long applicationId;
    private final RoutingStrategy strategy;
    private final Protocol protocol;
    private final Map<Long, Integer> channelPriorityMap;

    /**
     * 路由请求上下文
     *
     * <p>数据面认证即授权后，{@code userId}/{@code role} 不再透传（全链无消费者）；
     * 保留 {@code applicationId}（应用配置锚点）、{@code protocol}（HealthRouter 派生
     * endpointId）、{@code channelPriorityMap}（PriorityRouter 应用级优先级）。</p>
     *
     * @param modelId           模型 ID
     * @param applicationId     应用 ID（配置锚点）
     * @param strategy          路由策略
     * @param protocol          入站协议（可 null）
     * @param channelPriorityMap 应用级渠道优先级映射（可空映射）
     */
    public RoutingRequest(Long modelId, Long applicationId, RoutingStrategy strategy,
                          Protocol protocol, Map<Long, Integer> channelPriorityMap) {
        this.modelId = modelId;
        this.applicationId = applicationId;
        this.strategy = strategy;
        this.protocol = protocol;
        this.channelPriorityMap = channelPriorityMap;
    }

    // 保留 getter：getModelId / getApplicationId / getStrategy / getProtocol / getChannelPriorityMap
    // 删除：getUserId / getRole
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-proxy/proxy -am test`
Expected: PASS（三个测试 + InstanceSelector 全绿，无回归）

- [ ] **Step 5: 提交**

```bash
git add gateway-proxy/proxy/src
git commit -m "refactor(proxy): RoutingRequest 移除 userId/role 透传——数据面认证即授权后无消费者"
```

---

### Task 4: 体验中心授权补强

**Files:**
- Modify: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/experience/ModelExperienceService.java`
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/api/ExperienceController.java`
- Modify: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/experience/ModelExperienceServiceTest.java`

**Interfaces:**
- Consumes: `ExperienceChatParams`（含 `savedConfig`/`channelId`/`apiKey`/`baseUrl` 字段）；`Identity`（controller 经 `@RequestAttribute("identity")`）；`UserApiKeyRepository.findByUserId(Long)`（或等价方法——实现者按现有仓储方法名）、`ApplicationChannelRepository.findChannelIdsByApplicationId(Long)`
- Produces: `ModelExperienceService.chatStream(ExperienceChatParams, Long userId, String role)` 重载（授权校验）；`validateExperienceAccess` 内部校验逻辑（渠道归属 + 临时配置限 ADMIN）

- [ ] **Step 1: 改写失败测试**（追加到 `ModelExperienceServiceTest`）

```java
    @Test
    @DisplayName("使用已保存配置：体验渠道在用户应用渠道配置内 → 放行")
    void chatStream_savedConfig_channelInUserApps_passes() {
        // mock：userApiKeyRepository.findByUserId(1L) → [key1(applicationId=100)]
        // mock：applicationChannelRepository.findChannelIdsByApplicationId(100L) → Set.of(1L)
        // request：savedConfig=true, channelId=1L, credentialId=1L
        // mock 渠道/凭证/上游客户端（现有测试形态）
        // 断言：不抛异常（校验通过，进入正常流程）
    }

    @Test
    @DisplayName("使用已保存配置：体验渠道不在用户应用配置内 → 抛 IllegalArgumentException")
    void chatStream_savedConfig_channelNotInUserApps_rejected() {
        // mock：userApiKeyRepository.findByUserId(1L) → [key1(applicationId=100)]
        // mock：applicationChannelRepository.findChannelIdsByApplicationId(100L) → Set.of(2L)
        // request：savedConfig=true, channelId=1L（不在 {2L} 内）
        // 断言：assertThatThrownBy(...).isInstanceOf(IllegalArgumentException.class)
        //         .hasMessageContaining("渠道")
    }

    @Test
    @DisplayName("临时配置（useSavedConfig=false）：USER 拒绝，ADMIN 放行")
    void chatStream_temporaryConfig_roleGated() {
        // request：savedConfig=false, protocolName=openai, apiKey=sk-test, baseUrl=...
        // USER 角色 → assertThatThrownBy(...).isInstanceOf(IllegalArgumentException.class)
        //   .hasMessageContaining("管理员")
        // ADMIN 角色 → 不抛异常（进入正常流程，mock 上游客户端）
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-proxy/proxy -am test -Dtest=ModelExperienceServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（chatStream 新签名 / 方法不存在）

- [ ] **Step 3: 修改实现**

`ModelExperienceService` 注入授权相关仓储（构造器追加）：

```java
    private final UserApiKeyRepository userApiKeyRepository;
    private final ApplicationChannelRepository applicationChannelRepository;
```

`chatStream` 改造（新增重载，保留校验）：

```java
    /**
     * 流式聊天体验
     *
     * @param request 体验请求
     * @param userId  会话用户 ID（控制面统一身份，渠道归属校验）
     * @param role    会话用户角色（临时配置限 ADMIN）
     * @return SSE Emitter
     */
    public SseEmitter chatStream(ExperienceChatParams request, Long userId, String role) {
        // 授权校验（同步执行，executor 提交前；失败抛 IllegalArgumentException → 控制器转 403）
        validateExperienceAccess(request, userId, role);

        // 原有 isValid 校验 + SSE 流程（保持不变）
        ...
    }

    /**
     * 体验中心授权校验
     *
     * <p>最小补强：使用已保存配置时，体验渠道必须属于该用户任一应用的应用渠道配置；
     * 临时配置（apiKey/baseUrl 直连上游）仅管理员可用（体验沙箱定位）。</p>
     */
    private void validateExperienceAccess(ExperienceChatParams request, Long userId, String role) {
        if (!Boolean.TRUE.equals(request.savedConfig())) {
            if (!RolePermissions.ROLE_ADMIN.equals(role)) {
                throw new IllegalArgumentException("临时配置体验仅管理员可用");
            }
            return;
        }
        if (request.channelId() == null) {
            return; // isValid 已校验，此处防御
        }
        Set<Long> userAppChannelIds = userApiKeyRepository.findByUserId(userId).stream()
                .map(UserApiKey::getApplicationId)
                .filter(Objects::nonNull)
                .flatMap(appId -> applicationChannelRepository.findChannelIdsByApplicationId(appId).stream())
                .collect(Collectors.toSet());
        if (!userAppChannelIds.contains(request.channelId())) {
            throw new IllegalArgumentException("体验渠道不在你的应用授权范围内");
        }
    }
```

注：`UserApiKeyRepository.findByUserId` 方法名以现有仓储为准（实现者读取后调整）；`RolePermissions` 在 gateway-iam（proxy 已有依赖）。

`ExperienceController` 改造（`@RequestAttribute("identity")` 传入）：

```java
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ExperienceChatRequest request,
                                 @RequestAttribute("identity") Identity identity) {
        log.info("Experience chat request: channelId={}, model={}",
            request.getChannelId(), request.getModel());
        try {
            return modelExperienceService.chatStream(request.toCommand(), identity.userId(), identity.role());
        } catch (IllegalArgumentException e) {
            // 授权校验失败 → 403（SSE 端点前置校验，同步失败）
            throw new ForbiddenException(e.getMessage());
        }
    }
```

注：`ForbiddenException` 以项目现有异常类为准（gateway-common 或 iam.exception——实现者确认现有 403 异常类型）；若无合适异常，改用 `ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage())`。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-proxy/proxy,gateway-web -am test`
Expected: PASS（ModelExperienceServiceTest 新用例 + 两模块无回归）

- [ ] **Step 5: 提交**

```bash
git add gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/experience/ModelExperienceService.java gateway-web/src/main/java/com/codingas/gateway/web/api/ExperienceController.java gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/experience/ModelExperienceServiceTest.java
git commit -m "feat(proxy,web): 体验中心授权补强——渠道归属校验 + 临时配置限管理员"
```

---

### Task 5: 全量回归与文档对齐

**Files:**
- Modify: `docs/api-spec.md`（认证即授权语义 + 体验中心授权）
- Modify: `docs/应用架构.md` / `docs/技术架构.md`（RouterChain 成员与数据面授权描述，如相关）

- [ ] **Step 1: 全量回归测试**

Run: `mvn -q test`（Windows 若报 `NoDefaultCurrentDirectoryInExePath`，用 `env -u NoDefaultCurrentDirectoryInExePath mvn -q test`）
Expected: 全模块 PASS

- [ ] **Step 2: 更新 api-spec.md**

在「统一认证架构」「统一授权模型」小节附近补充（或修订既有数据面授权描述）：

```markdown
### 数据面授权（认证即授权，v1.x）

数据面调用链：API Key 认证（Key 绑定应用）→ 应用渠道配置读取 → 实例查询（DB 层按渠道过滤）→ 路由（优先级/健康）。

- 授权蕴含在认证：`Identity.applicationId` 即 Key 绑定的应用资源（invoke 权限），请求参数无法覆盖
- `application_channel` 为应用渠道配置（非授权表）；渠道/实例状态为 ABAC 属性（RouterChain 过滤）
- 路由链仅含 `PriorityRouter`（应用级优先级）与 `HealthRouter`（入站协议派生 endpointId + 实例健康）
- 体验中心（`/api/v1/experience/chat`）：体验渠道须属于用户任一应用的应用渠道配置；临时配置（apiKey/baseUrl 直连）仅管理员
```

- [ ] **Step 3: 提交**

```bash
git add docs/api-spec.md docs/应用架构.md docs/技术架构.md
git commit -m "docs: 数据面认证即授权语义 + 体验中心授权说明"
```

---

## Self-Review 记录

**1. Spec 覆盖**（对照设计文档 2026-10-02-dataplane-auth-simplification-design.md）：
- `PermissionRouter`/`LoadBalanceRouter` 删除 ✔ Task 2
- `findActiveByModelIdAndChannelIds` 新查询（DB 层过滤）✔ Task 1
- `AuthorizationService.permittedChannelIds` 删除 —— ⚠️ **设计文档验收项 3**：未分配任务！补：在 Task 2 中一并删除（`AuthorizationService.java` + `AuthorizationServiceTest` 中 `permittedChannelIds` 方法与相关用例）——Task 2 Step 4 删除命令追加
- `RoutingRequest` userId/role 清理 ✔ Task 3
- 体验中心补强（渠道归属 + 临时配置限 ADMIN）✔ Task 4
- 行为等价 + 全量回归 ✔ Task 5
- 文档 ✔ Task 5

**2. 占位符扫描**：Task 1/2/4 测试中「实现者按现有测试文件形态」「以现有仓储方法名为准」为对现有代码的适配说明（实现者需先读现有文件），核心断言/签名已完整给出；无 TBD/TODO。

**3. 类型一致性**：
- `findActiveByModelIdAndChannelIds(Long, Collection<Long>)` → Task 1 定义、Task 2 消费一致 ✔
- `RoutingRequest(Long, Long, RoutingStrategy, Protocol, Map<Long,Integer>)` → Task 2 构造、Task 3 定义一致 ✔
- `chatStream(ExperienceChatParams, Long, String)` → Task 4 定义与 controller 调用一致 ✔
- `InstanceSelector.select` 签名不变 → ChatDispatchServiceImpl 零改动 ✔
- `permittedChannelIds` 删除 → Task 2 Files/Step 4 已整合（设计文档验收项 3 覆盖）✔
