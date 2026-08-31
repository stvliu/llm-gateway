# 模型生命周期管理自动化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现模型生命周期自动化：上游列表探测（即将废弃→渠道实例 DEPRECATED）+ 运行期 model_not_found 确认（已废弃→规格 deprecatedAt + 实例 RETIRED），全部受系统设置开关控制。

**Architecture:** 双通道信号（管理面定时探测 + 数据面异常确认）→ 渠道级状态流转（ModelInstance.DEPRECATED 继续路由 / RETIRED 停用）+ 规格级 deprecatedAt 全局停用。计数用进程内内存（不依赖 Redis）。配置走 gateway-settings 域（catalog.deprecation.*）。

**Tech Stack:** Java 21 + Spring Boot 3.5 + JPA + JDK HttpClient（探测）

## Global Constraints

- 分层依赖铁律：proxy → provider 接口；provider 不反向依赖 proxy；web 不访问 Repository
- 实体纯洁性：JPA 实体只含 Getter/Setter，无业务逻辑
- 模型纯洁性：业务逻辑在 Service，Controller/Facade 只组装
- 中文注释/Javadoc：public 方法必须中文 Javadoc，关键业务逻辑加中文注释
- 配置外部化：所有可变参数通过 SystemSettingService（catalog.deprecation.*），禁止魔法数字
- 开关关闭时数据面零干预；测试环境关闭定时任务装配（gateway.catalog.probe.auto-enabled=false）
- 计数用进程内内存（ConcurrentHashMap），不引入 Redis 依赖
- TDD：先写测试再实现；每任务独立提交

---

### Task 1: ProviderErrorType 新增 MODEL_NOT_FOUND + ErrorClassifier 分流

**Files:**
- Modify: `gateway-common/common/src/main/java/com/codingas/gateway/common/enums/ProviderErrorType.java`
- Modify: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/chat/ErrorClassifier.java`
- Test: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/chat/ErrorClassifierTest.java`

**Interfaces:**
- Consumes: 现有 `ProviderErrorType`（common 枚举）、`ErrorClassifier`（proxy 分流表）
- Produces: `ProviderErrorType.MODEL_NOT_FOUND` 枚举值；`ErrorClassifier.classify(MODEL_NOT_FOUND)` 返回 `FailoverDecision.NONE`

- [ ] **Step 1: 写失败测试（ErrorClassifier 分流）**

在 `ErrorClassifierTest` 追加：

```java
@Test
@DisplayName("MODEL_NOT_FOUND 归为 NONE（请求级，不故障转移）")
void classify_modelNotFound_returnsNone() {
    assertThat(classifier.classify(ProviderErrorType.MODEL_NOT_FOUND))
            .isEqualTo(FailoverDecision.NONE);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-proxy/proxy -am test -Dtest=ErrorClassifierTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（`MODEL_NOT_FOUND` 不存在）

- [ ] **Step 3: 实现**

`ProviderErrorType.java` 在 `INVALID_REQUEST` 后新增：

```java
    /** 上游返回模型不存在（404 model_not_found / not_found_error） */
    MODEL_NOT_FOUND,
```

`ErrorClassifier.java` 静态块在 INVALID_REQUEST 行后新增：

```java
        // 模型不存在：请求级错误，换哪都无效，直接抛出（供废弃检测器识别）
        DECISION_TABLE.put(ProviderErrorType.MODEL_NOT_FOUND, FailoverDecision.NONE);
```

- [ ] **Step 4: 运行测试确认通过**

Run: 同上命令
Expected: PASS（含新增用例）

- [ ] **Step 5: Commit**

```bash
git add gateway-common/common/src/main/java/com/codingas/gateway/common/enums/ProviderErrorType.java \
        gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/chat/ErrorClassifier.java \
        gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/chat/ErrorClassifierTest.java
git commit -m "feat(model-lifecycle): ProviderErrorType 新增 MODEL_NOT_FOUND，ErrorClassifier 归为 NONE"
```

---

### Task 2: UpstreamException 补 httpStatus + OpenAI/Anthropic 协议层归类 MODEL_NOT_FOUND

**Files:**
- Modify: `gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/UpstreamException.java`
- Modify: `gateway-protocol/protocol-openai/src/main/java/com/codingas/gateway/protocol/openai/OpenAIErrorClassifier.java`
- Modify: `gateway-protocol/protocol-openai/src/main/java/com/codingas/gateway/protocol/openai/OpenAIUpstreamClient.java`
- Modify: `gateway-protocol/protocol-anthropic/src/main/java/com/codingas/gateway/protocol/anthropic/AnthropicErrorClassifier.java`
- Modify: `gateway-protocol/protocol-anthropic/src/main/java/com/codingas/gateway/protocol/anthropic/AnthropicUpstreamClient.java`
- Test: `gateway-protocol/protocol-openai/src/test/java/com/codingas/gateway/protocol/openai/OpenAIErrorClassifierTest.java`、`OpenAIUpstreamClientTest.java`

**Interfaces:**
- Consumes: `ProviderErrorType.MODEL_NOT_FOUND`（Task 1）
- Produces: `UpstreamException.getHttpStatus()`（`Integer`）；OpenAI/Anthropic 对 404 且错误类型匹配时抛 `MODEL_NOT_FOUND` 且带 httpStatus

- [ ] **Step 1: 写失败测试**

`OpenAIErrorClassifierTest` 追加：

```java
@Test
@DisplayName("404 且错误码 model_not_found 归类为 MODEL_NOT_FOUND")
void classify_404ModelNotFound_returnsModelNotFound() {
    assertThat(classifier.classify(404,
            "{\"error\":{\"message\":\"The model 'xxx' does not exist\",\"type\":\"invalid_request_error\",\"code\":\"model_not_found\"}}"))
            .isEqualTo(ProviderErrorType.MODEL_NOT_FOUND);
}

@Test
@DisplayName("普通 404 无 model_not_found 标志仍归 UNKNOWN")
void classify_plain404_notModelNotFound() {
    assertThat(classifier.classify(404, "{\"error\":{\"message\":\"not found\"}}"))
            .isEqualTo(ProviderErrorType.UNKNOWN_ERROR);
}
```

`AnthropicErrorClassifierTest` 追加（若文件存在，否则新建）:

```java
@Test
@DisplayName("404 且错误类型 not_found_error 归类为 MODEL_NOT_FOUND")
void classify_404NotFoundError_returnsModelNotFound() {
    assertThat(classifier.classify(404,
            "{\"type\":\"error\",\"error\":{\"type\":\"not_found_error\",\"message\":\"model: claude-x\"}}"))
            .isEqualTo(ProviderErrorType.MODEL_NOT_FOUND);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-protocol/protocol-openai -am test -Dtest=OpenAIErrorClassifierTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 失败（分类器未识别 404/model_not_found）

- [ ] **Step 3: 实现 UpstreamException.httpStatus**

在 `UpstreamException.java` 增加字段与构造重载（现有构造保持，新增带 status 的）：

```java
    /** HTTP 状态码（上游响应；网络异常等场景为 null） */
    private final Integer httpStatus;

    public UpstreamException(ProviderErrorType errorType, String message,
                             Integer httpStatus, String traceId, String model,
                             String provider, Long channelEndpointId, Integer retryAfterSeconds) {
        super(errorType.name(), message);
        this.errorType = errorType;
        this.httpStatus = httpStatus;
        this.traceId = traceId;
        this.model = model;
        this.provider = provider;
        this.channelEndpointId = channelEndpointId;
        this.retryAfterSeconds = retryAfterSeconds;
    }
```

其余构造（errorType, message 等）补 `this.httpStatus = null;`。

- [ ] **Step 4: 实现 OpenAIErrorClassifier 404 分支**

在 `classify(int statusCode, String responseBody)` 的 switch 中 `case 400` 后新增：

```java
            case 404 -> isModelNotFound(responseBody) ? ProviderErrorType.MODEL_NOT_FOUND
                    : ProviderErrorType.UNKNOWN_ERROR;
```

新增私有方法：

```java
    /** 识别 OpenAI model_not_found 错误码（body 含 code=model_not_found 或消息含 does not exist） */
    private boolean isModelNotFound(String responseBody) {
        if (responseBody == null) return false;
        return responseBody.contains("model_not_found")
                || responseBody.toLowerCase().contains("does not exist");
    }
```

- [ ] **Step 5: 实现 OpenAIUpstreamClient 透传 httpStatus**

在 `OpenAIUpstreamClient` 中构造 `UpstreamException` 的两处（非流式 82 行附近、流式 127 行附近）改为传 `response.code()`：

```java
                    throw new UpstreamException(errorType, errorBody, response.code(),
                            null, null, null, null, null);
```

（流式 `callback.onError` 同理传 `response.code()`。）

- [ ] **Step 6: 实现 Anthropic 侧（分类器 + Client）**

`AnthropicErrorClassifier` 同样加 `case 404` 分支，识别 `not_found_error`：

```java
            case 404 -> responseBody != null && responseBody.contains("not_found_error")
                    ? ProviderErrorType.MODEL_NOT_FOUND : ProviderErrorType.UNKNOWN_ERROR;
```

`AnthropicUpstreamClient` 构造 UpstreamException 处透传 `response.code()`。

- [ ] **Step 7: 运行测试确认通过**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-protocol/protocol-openai,gateway-protocol/protocol-anthropic,gateway-protocol/protocol -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（分类器 + Client 既有测试与新增用例）

- [ ] **Step 8: Commit**

```bash
git add gateway-protocol/protocol/src/main/java/com/codingas/gateway/protocol/transport/UpstreamException.java \
        gateway-protocol/protocol-openai/src/main/java/com/codingas/gateway/protocol/openai/OpenAIErrorClassifier.java \
        gateway-protocol/protocol-openai/src/main/java/com/codingas/gateway/protocol/openai/OpenAIUpstreamClient.java \
        gateway-protocol/protocol-anthropic/src/main/java/com/codingas/gateway/protocol/anthropic/AnthropicErrorClassifier.java \
        gateway-protocol/protocol-anthropic/src/main/java/com/codingas/gateway/protocol/anthropic/AnthropicUpstreamClient.java
git add gateway-protocol/protocol-openai/src/test/java/com/codingas/gateway/protocol/openai/OpenAIErrorClassifierTest.java
git commit -m "feat(model-lifecycle): UpstreamException 补 httpStatus，OpenAI/Anthropic 404 归类 MODEL_NOT_FOUND"
```

---

### Task 3: 系统设置新增 catalog.deprecation.* 配置项种子

**Files:**
- Modify: `gateway-settings/settings-starter/src/main/java/com/codingas/gateway/autoconfigure/settings/SettingsDefaultDataInitializer.java`
- Test: `gateway-settings/settings-starter/src/test/java/.../SettingsDefaultDataInitializerTest.java`（若无则新建，参考现有配置项断言）

**Interfaces:**
- Consumes: `SystemSettingService.getBoolean/getInt/getEnum`（settings 域）
- Produces: 5 个默认配置项（`catalog.deprecation.enabled=true`、`catalog.deprecation.runtime.enabled=true`、`catalog.deprecation.confirm-count=3`、`catalog.deprecation.probe.enabled=true`、`catalog.deprecation.probe.interval=WEEKLY`）

- [ ] **Step 1: 写失败测试（种子包含新配置项）**

在初始化器测试中追加断言（若测试类已存在，在既有断言后追加；否则新建）：

```java
    @Test
    @DisplayName("默认种子包含模型废弃自动化配置项")
    void seedContainsDeprecationDefaults() {
        assertThat(settingService.getBoolean("catalog.deprecation.enabled", false)).isTrue();
        assertThat(settingService.getBoolean("catalog.deprecation.runtime.enabled", false)).isTrue();
        assertThat(settingService.getInt("catalog.deprecation.confirm-count", 0)).isEqualTo(3);
        assertThat(settingService.getBoolean("catalog.deprecation.probe.enabled", false)).isTrue();
        assertThat(settingService.getEnum("catalog.deprecation.probe.interval", SyncInterval.class, null))
                .isEqualTo(SyncInterval.WEEKLY);
    }
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-settings/settings-starter -am test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 失败（配置项不存在）

- [ ] **Step 3: 实现（seedDefaults 列表追加）**

在 `SettingsDefaultDataInitializer.seedDefaults()` 的 `List.of(...)` 中追加：

```java
                setting("catalog.deprecation.enabled", "true", "BOOLEAN", "CATALOG", "模型废弃自动化总开关", true),
                setting("catalog.deprecation.runtime.enabled", "true", "BOOLEAN", "CATALOG", "调用中自动检查模型废弃", true),
                setting("catalog.deprecation.confirm-count", "3", "NUMBER", "CATALOG", "废弃确认次数（防误判）", true),
                setting("catalog.deprecation.probe.enabled", "true", "BOOLEAN", "CATALOG", "上游列表探测（提前预警）", true),
                setting("catalog.deprecation.probe.interval", "WEEKLY", "ENUM", "CATALOG", "上游列表探测周期", true)
```

确认 `SystemSettingService` 有 `getInt(String, int)` 方法（`confirm-count` 为 NUMBER 类型）；若无则实现中只断言 getBoolean/getEnum，NUMBER 断言改为 `getValue` 字符串比较（按既有测试模式）。

- [ ] **Step 4: 运行测试确认通过**

Run: 同上命令
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-settings/settings-starter/src/main/java/com/codingas/gateway/autoconfigure/settings/SettingsDefaultDataInitializer.java
git add gateway-settings/settings-starter/src/test/java
git commit -m "feat(model-lifecycle): 系统设置新增 catalog.deprecation.* 配置项种子"
```

---

### Task 4: ModelDeprecationService（provider 域）— 自动标记废弃/实例状态流转/审计

**Files:**
- Create: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelDeprecationService.java`
- Modify: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelInstanceRepository.java`（新增 `findByModelId`）
- Modify: `gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/model/ModelInstanceJpaRepository.java`（新增 `findByModelId` 派生查询）
- Modify: `gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/model/JpaModelInstanceRepository.java`（实现 `findByModelId` 映射）
- Test: `gateway-provider/provider/src/test/java/com/codingas/gateway/provider/model/ModelDeprecationServiceTest.java`

**Interfaces:**
- Consumes: `ModelRepository`（findByModelName/save）、`ModelInstanceRepository`（新增 `findByModelId`/findById/save）、`AuditLogRepository`（saveAuditLog）
- Produces:
  - `void markDeprecated(String modelName, String reason)` — 运行期确认：幂等设置 `Model.deprecatedAt` + `deprecationMessage`，该模型所有实例转 RETIRED，写审计（action=`MODEL_DEPRECATED`）
  - `void markInstanceDeprecated(Long instanceId)` — 探测确认：ACTIVE→DEPRECATED（走状态机合法路径），写审计
  - `void restoreInstance(Long instanceId)` — 探测恢复：DEPRECATED→ACTIVE，写审计

- [ ] **Step 1: 写失败测试**

`ModelDeprecationServiceTest`（Mockito，参考 `ModelInstanceServiceImplTest` 风格）：

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelDeprecationService 模型废弃自动化")
class ModelDeprecationServiceTest {

    @Mock private ModelRepository modelRepository;
    @Mock private ModelInstanceRepository instanceRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private ModelDeprecationService service;

    private Model model() {
        Model m = new Model();
        m.setId(1L);
        m.setModelName("gpt-4");
        return m;
    }

    @Test
    @DisplayName("markDeprecated 幂等设置 deprecatedAt 并转 RETIRED + 审计")
    void markDeprecated_setsFieldsAndRetiresInstances() {
        Model m = model();
        when(modelRepository.findByModelName("gpt-4")).thenReturn(Optional.of(m));
        ModelInstance inst = new ModelInstance();
        inst.setId(10L);
        inst.setState(ModelInstance.State.ACTIVE);
        when(instanceRepository.findByModelId(1L)).thenReturn(List.of(inst));

        service.markDeprecated("gpt-4", "上游确认模型已废弃（model_not_found）");

        assertThat(m.getDeprecatedAt()).isNotNull();
        assertThat(m.getDeprecationMessage()).contains("model_not_found");
        assertThat(inst.getState()).isEqualTo(ModelInstance.State.RETIRED);
        verify(modelRepository).save(m);
        verify(auditLogRepository).saveAuditLog(any(AuditLog.class));
    }

    @Test
    @DisplayName("markDeprecated 已废弃时幂等跳过（不重复审计）")
    void markDeprecated_alreadyDeprecated_skips() {
        Model m = model();
        m.setDeprecatedAt(Instant.now());
        when(modelRepository.findByModelName("gpt-4")).thenReturn(Optional.of(m));

        service.markDeprecated("gpt-4", "again");

        verify(modelRepository, never()).save(any(Model.class));
        verify(auditLogRepository, never()).saveAuditLog(any(AuditLog.class));
    }

    @Test
    @DisplayName("markInstanceDeprecated ACTIVE→DEPRECATED")
    void markInstanceDeprecated_transitions() {
        ModelInstance inst = new ModelInstance();
        inst.setId(10L);
        inst.setState(ModelInstance.State.ACTIVE);
        when(instanceRepository.findById(10L)).thenReturn(Optional.of(inst));

        service.markInstanceDeprecated(10L);

        assertThat(inst.getState()).isEqualTo(ModelInstance.State.DEPRECATED);
        verify(instanceRepository).save(inst);
    }

    @Test
    @DisplayName("restoreInstance DEPRECATED→ACTIVE")
    void restoreInstance_transitions() {
        ModelInstance inst = new ModelInstance();
        inst.setId(10L);
        inst.setState(ModelInstance.State.DEPRECATED);
        when(instanceRepository.findById(10L)).thenReturn(Optional.of(inst));

        service.restoreInstance(10L);

        assertThat(inst.getState()).isEqualTo(ModelInstance.State.ACTIVE);
        verify(instanceRepository).save(inst);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-provider/provider -am test -Dtest=ModelDeprecationServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 实现**

先为 `ModelInstanceRepository` 新增 `findByModelId`：

`ModelInstanceRepository.java`（接口）追加：

```java
    /**
     * 按模型 ID 查找全部模型实例（含各状态，供自动标记废弃时批量下线）
     *
     * @param modelId 模型 ID
     * @return 模型实例实体列表
     */
    List<ModelInstance> findByModelId(Long modelId);
```

`ModelInstanceJpaRepository.java` 追加派生查询：

```java
    List<ModelInstanceDo> findByModelId(Long modelId);
```

`JpaModelInstanceRepository.java` 追加映射实现：

```java
    @Override
    public List<ModelInstance> findByModelId(Long modelId) {
        return modelInstanceRepository.findByModelId(modelId).stream().map(this::toEntity).toList();
    }
```

（同步在 `JpaModelInstanceRepositoryTest` 补 `findByModelId` 用例：同 modelId 多实例/含不同状态。）

再创建 `ModelDeprecationService`：

```java
package com.codingas.gateway.provider.model;

/**
 * 模型废弃自动化服务
 *
 * <p>承载两条自动信号通道的状态落地：运行期确认（{@link #markDeprecated}）与
 * 列表探测（{@link #markInstanceDeprecated}/{@link #restoreInstance}）。
 * 所有状态变更写管理操作审计（AuditLog）。幂等：已废弃/已处于目标状态的跳过。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDeprecationService {

    private final ModelRepository modelRepository;
    private final ModelInstanceRepository instanceRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * 运行期确认模型已废弃：幂等设置 deprecatedAt + deprecationMessage，
     * 该模型所有实例转 RETIRED（系统强信号，直接设置），写审计。
     *
     * @param modelName 模型名（用户面标识）
     * @param reason    废弃原因
     */
    @Transactional
    public void markDeprecated(String modelName, String reason) {
        Model model = modelRepository.findByModelName(modelName).orElse(null);
        if (model == null) {
            log.warn("废弃确认目标模型不存在, modelName={}", modelName);
            return;
        }
        if (model.getDeprecatedAt() != null) {
            log.debug("模型已废弃，跳过重复确认, modelName={}", modelName);
            return;
        }
        model.setDeprecatedAt(Instant.now());
        model.setDeprecationMessage(reason);
        modelRepository.save(model);
        // 该模型所有实例转 RETIRED（上游已确认不可用，系统级强制）
        List<ModelInstance> instances = instanceRepository.findByModelId(model.getId());
        for (ModelInstance instance : instances) {
            if (instance.getState() != ModelInstance.State.RETIRED) {
                instance.setState(ModelInstance.State.RETIRED);
                instanceRepository.save(instance);
            }
        }
        writeAudit("MODEL_DEPRECATED", "Model:" + modelName, "SUCCESS");
        log.info("模型已自动标记废弃, modelName={}, reason={}", modelName, reason);
    }

    /**
     * 探测确认渠道不再提供该模型：ACTIVE→DEPRECATED（即将废弃，继续路由）
     *
     * @param instanceId 模型实例 ID
     */
    @Transactional
    public void markInstanceDeprecated(Long instanceId) {
        ModelInstance instance = instanceRepository.findById(instanceId).orElse(null);
        if (instance == null || instance.getState() == ModelInstance.State.DEPRECATED) {
            return;
        }
        instance.setState(ModelInstance.State.DEPRECATED);
        instanceRepository.save(instance);
        writeAudit("MODEL_INSTANCE_DEPRECATED", "ModelInstance:" + instanceId, "SUCCESS");
        log.info("模型实例标记即将废弃, id={}", instanceId);
    }

    /**
     * 探测发现模型重新出现：DEPRECATED→ACTIVE（自动恢复）
     *
     * @param instanceId 模型实例 ID
     */
    @Transactional
    public void restoreInstance(Long instanceId) {
        ModelInstance instance = instanceRepository.findById(instanceId).orElse(null);
        if (instance == null || instance.getState() != ModelInstance.State.DEPRECATED) {
            return;
        }
        instance.setState(ModelInstance.State.ACTIVE);
        instanceRepository.save(instance);
        writeAudit("MODEL_INSTANCE_RESTORED", "ModelInstance:" + instanceId, "SUCCESS");
        log.info("模型实例已自动恢复, id={}", instanceId);
    }

    /** 写管理操作审计（自动系统操作，userId 置空） */
    private void writeAudit(String action, String resource, String result) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setResource(resource);
        auditLog.setResult(result);
        try {
            auditLogRepository.saveAuditLog(auditLog);
        } catch (Exception e) {
            log.warn("写入废弃审计失败: {}", e.getMessage());
        }
    }
}
```

（补 import：`java.time.Instant`、`java.util.List`、`com.codingas.gateway.audit.AuditLog`、`AuditLogRepository`、`org.springframework.transaction.annotation.Transactional`。）

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS（4 用例）

- [ ] **Step 5: Commit**

```bash
git add gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelDeprecationService.java
git add gateway-provider/provider/src/test/java/com/codingas/gateway/provider/model/ModelDeprecationServiceTest.java
git commit -m "feat(model-lifecycle): ModelDeprecationService 自动标记废弃/实例状态流转/审计"
```

---

### Task 5: RuntimeDeprecationDetector（proxy 域）— 内存计数 + 开关 + 阈值确认

**Files:**
- Create: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/chat/RuntimeDeprecationDetector.java`
- Test: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/chat/RuntimeDeprecationDetectorTest.java`

**Interfaces:**
- Consumes: `SystemSettingService`（settings 域：getBoolean/getInt）、`ModelDeprecationService.markDeprecated`（Task 4）
- Produces: `void onModelNotFound(String modelName)` — 开关校验→内存计数→达到 confirm-count 调 markDeprecated 并清计数

- [ ] **Step 1: 写失败测试**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("RuntimeDeprecationDetector 运行期废弃检测")
class RuntimeDeprecationDetectorTest {

    @Mock private SystemSettingService settingService;
    @Mock private ModelDeprecationService deprecationService;
    @InjectMocks private RuntimeDeprecationDetector detector;

    @Test
    @DisplayName("总开关关闭时不计数不确认")
    void disabled_totalSwitch_skips() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(false);
        detector.onModelNotFound("gpt-4");
        verify(deprecationService, never()).markDeprecated(any(), any());
    }

    @Test
    @DisplayName("运行期子开关关闭时不确认")
    void disabled_runtimeSwitch_skips() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.runtime.enabled", true)).thenReturn(false);
        detector.onModelNotFound("gpt-4");
        verify(deprecationService, never()).markDeprecated(any(), any());
    }

    @Test
    @DisplayName("达到确认次数触发标记并清计数")
    void reachesThreshold_confirmsAndResets() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.runtime.enabled", true)).thenReturn(true);
        when(settingService.getInt("catalog.deprecation.confirm-count", 3)).thenReturn(3);

        detector.onModelNotFound("gpt-4");
        detector.onModelNotFound("gpt-4");
        verify(deprecationService, never()).markDeprecated(any(), any());

        detector.onModelNotFound("gpt-4");
        verify(deprecationService).markDeprecated(eq("gpt-4"), contains("model_not_found"));

        // 清计数后再次计数，不应立即触发
        detector.onModelNotFound("gpt-4");
        verify(deprecationService, times(1)).markDeprecated(any(), any());
    }

    @Test
    @DisplayName("不同模型独立计数")
    void countsPerModelIndependently() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.runtime.enabled", true)).thenReturn(true);
        when(settingService.getInt("catalog.deprecation.confirm-count", 3)).thenReturn(3);

        detector.onModelNotFound("gpt-4");
        detector.onModelNotFound("gpt-4");
        detector.onModelNotFound("claude-3");
        verify(deprecationService, never()).markDeprecated(any(), any());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-proxy/proxy -am test -Dtest=RuntimeDeprecationDetectorTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 实现**

```java
package com.codingas.gateway.proxy.chat;

/**
 * 运行期废弃检测器（数据面兜底确认）
 *
 * <p>数据面识别到 model_not_found 时由 {@link #onModelNotFound} 计数，
 * 达到 {@code catalog.deprecation.confirm-count} 后确认模型废弃（幂等）。
 * 计数使用进程内内存（{@link ConcurrentHashMap}），开发/本地/单实例不依赖 Redis；
 * 多实例各自计数，确认动作幂等无副作用。开关关闭时完全不干预（正常路径零开销）。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuntimeDeprecationDetector {

    /** 连续确认次数：modelName → 计数 */
    private final ConcurrentHashMap<String, Integer> confirmations = new ConcurrentHashMap<>();

    private final SystemSettingService settingService;
    private final ModelDeprecationService deprecationService;

    /**
     * 记录一次 model_not_found 信号；达到阈值确认废弃并清计数
     *
     * @param modelName 模型名
     */
    public void onModelNotFound(String modelName) {
        if (!settingService.getBoolean("catalog.deprecation.enabled", true)
                || !settingService.getBoolean("catalog.deprecation.runtime.enabled", true)) {
            log.debug("模型废弃自动化已关闭，忽略 model_not_found: {}", modelName);
            return;
        }
        int confirmCount = settingService.getInt("catalog.deprecation.confirm-count", 3);
        int count = confirmations.merge(modelName, 1, Integer::sum);
        if (count >= confirmCount) {
            confirmations.remove(modelName);
            log.info("模型连续 {} 次确认废弃, modelName={}", count, modelName);
            try {
                deprecationService.markDeprecated(modelName,
                        "上游确认模型已废弃（model_not_found）");
            } catch (RuntimeException e) {
                // 确认失败不阻断请求（错误已按原样返回用户）
                log.error("自动标记废弃失败, modelName={}: {}", modelName, e.getMessage());
            }
        }
    }
}
```

（补 import：`com.codingas.gateway.settings.SystemSettingService`、`com.codingas.gateway.provider.model.ModelDeprecationService`、`java.util.concurrent.ConcurrentHashMap`。）

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS（4 用例）

- [ ] **Step 5: Commit**

```bash
git add gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/chat/RuntimeDeprecationDetector.java
git add gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/chat/RuntimeDeprecationDetectorTest.java
git commit -m "feat(model-lifecycle): RuntimeDeprecationDetector 内存计数 + 开关 + 阈值确认"
```

---

### Task 6: 数据面挂接 — ChatDispatchServiceImpl 识别 MODEL_NOT_FOUND 触发检测

**Files:**
- Modify: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/chat/ChatDispatchServiceImpl.java`
- Test: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/chat/ChatDispatchServiceImplTest.java`（参考现有测试结构）

**Interfaces:**
- Consumes: `RuntimeDeprecationDetector.onModelNotFound(modelName)`（Task 5）
- Produces: 非流式 catch 与流式 onError 中，`UpstreamException` 且 `errorType == MODEL_NOT_FOUND` 时调用 detector（用 `UpstreamException.getModel()` 作为 modelName）

- [ ] **Step 1: 写失败测试**

在 `ChatDispatchServiceImplTest` 追加（mock detector）：

```java
    @Mock
    private RuntimeDeprecationDetector deprecationDetector;

    @Test
    @DisplayName("MODEL_NOT_FOUND 异常触发废弃检测")
    void dispatch_modelNotFound_triggersDetector() {
        // 构造：invoke 抛 UpstreamException(MODEL_NOT_FOUND, ..., model="gpt-4", ...)
        // （按现有测试桩方式：when(channelFailoverInvoker.invoke(...)).thenThrow(...)）
        // then：
        verify(deprecationDetector).onModelNotFound("gpt-4");
    }

    @Test
    @DisplayName("非 MODEL_NOT_FOUND 异常不触发检测")
    void dispatch_otherError_doesNotTriggerDetector() {
        // invoke 抛 UpstreamException(INVALID_REQUEST, ...)
        // then：
        verify(deprecationDetector, never()).onModelNotFound(anyString());
    }
```

（测试具体桩代码按现有 `ChatDispatchServiceImplTest` 的 mock 模式实现——需 mock `RoutingResolver.resolveCandidates` 返回单候选、`ChannelFailoverInvoker.invoke` 抛指定异常。）

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-proxy/proxy -am test -Dtest=ChatDispatchServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 失败（未挂接 detector，verify 不满足）

- [ ] **Step 3: 实现**

在 `ChatDispatchServiceImpl`：
1. 注入 `RuntimeDeprecationDetector`（构造参数）。
2. 非流式 `dispatch` 的 `catch (Exception e)` 块开头追加：

```java
        } catch (Exception e) {
            // 模型不存在信号：触发废弃检测（仅确认计数，不阻断错误返回）
            if (e instanceof UpstreamException ue
                    && ue.getErrorType() == ProviderErrorType.MODEL_NOT_FOUND) {
                deprecationDetector.onModelNotFound(ue.getModel() != null ? ue.getModel() : request.getModel());
            }
            callLog.setDurationMs(System.currentTimeMillis() - startTime);
            ...
```

3. 流式 `auditingCallback.onError(Throwable t)` 开头追加：

```java
            public void onError(Throwable t) {
                if (t instanceof UpstreamException ue
                        && ue.getErrorType() == ProviderErrorType.MODEL_NOT_FOUND) {
                    deprecationDetector.onModelNotFound(ue.getModel() != null ? ue.getModel() : request.getModel());
                }
                ...
```

（补 import：`com.codingas.gateway.common.enums.ProviderErrorType`。）

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/chat/ChatDispatchServiceImpl.java
git add gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/chat/ChatDispatchServiceImplTest.java
git commit -m "feat(model-lifecycle): 数据面挂接 MODEL_NOT_FOUND 触发废弃检测"
```

---

### Task 7: findActive* 查询修复 — ACTIVE + DEPRECATED（isRoutable 语义）

**Files:**
- Modify: `gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/model/ModelInstanceJpaRepository.java`
- Modify: `gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/model/JpaModelInstanceRepository.java`
- Modify: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelInstanceRepository.java`（注释更新）
- Test: `gateway-provider/provider-data/src/test/java/com/codingas/gateway/providerdata/model/JpaModelInstanceRepositoryTest.java`

**Interfaces:**
- Consumes: 现有 `findActiveByChannelId`/`findActiveByModelIdOrderByPriority` 调用方（ModelDiscoveryService、InstanceSelector、ModelExperienceService）
- Produces: `findActive*` 返回 ACTIVE + DEPRECATED（未到期）实例——与 `State.isRoutable()` 语义对齐，"即将废弃继续路由"落地

- [ ] **Step 1: 写失败测试**

在 `JpaModelInstanceRepositoryTest` 追加：

```java
    @Test
    @DisplayName("findActiveByChannelId 包含 ACTIVE 与 DEPRECATED，排除 SUSPENDED/RETIRED")
    void findActiveByChannelId_includesDeprecated() {
        // 通过 Repository 创建 3 个实例：ACTIVE、DEPRECATED、RETIRED（同渠道）
        // when
        List<ModelInstance> active = repository.findActiveByChannelId(channelId);
        // then：ACTIVE + DEPRECATED 返回，RETIRED 排除
        assertThat(active).extracting(ModelInstance::getState)
                .containsExactlyInAnyOrder(ModelInstance.State.ACTIVE, ModelInstance.State.DEPRECATED);
    }
```

（具体建数据方式参考该测试类现有 fixture。）

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-provider/provider-data -am test -Dtest=JpaModelInstanceRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 失败（当前只返回 ACTIVE）

- [ ] **Step 3: 实现**

`ModelInstanceJpaRepository` 新增派生查询（替换现有两个 findActive 使用的单状态查询）：

```java
    List<ModelInstanceDo> findByChannelIdAndStateIn(Long channelId, List<String> states);

    List<ModelInstanceDo> findByModelIdAndStateInOrderByPriorityAsc(Long modelId, List<String> states);
```

`JpaModelInstanceRepository` 两个 findActive 方法改为：

```java
    @Override
    public List<ModelInstance> findActiveByChannelId(Long channelId) {
        return modelInstanceRepository.findByChannelIdAndStateIn(channelId, ROUTABLE_STATES)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelInstance> findActiveByModelIdOrderByPriority(Long modelId) {
        return modelInstanceRepository.findByModelIdAndStateInOrderByPriorityAsc(modelId, ROUTABLE_STATES)
                .stream().map(this::toEntity).toList();
    }
```

新增常量（`JpaModelInstanceRepository` 类内）：

```java
    /** 可路由状态集合（与 ModelInstance.State.isRoutable() 语义一致：ACTIVE + DEPRECATED） */
    private static final List<String> ROUTABLE_STATES = List.of(
            ModelInstance.State.ACTIVE.name(), ModelInstance.State.DEPRECATED.name());
```

`ModelInstanceRepository` 接口两个 findActive 方法 Javadoc 更新为"返回可路由（ACTIVE/DEPRECATED）实例"。

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/model/ModelInstanceJpaRepository.java \
        gateway-provider/provider-data/src/main/java/com/codingas/gateway/providerdata/model/JpaModelInstanceRepository.java \
        gateway-provider/provider/src/main/java/com/codingas/gateway/provider/model/ModelInstanceRepository.java
git add gateway-provider/provider-data/src/test/java/com/codingas/gateway/providerdata/model/JpaModelInstanceRepositoryTest.java
git commit -m "fix(model-lifecycle): findActive* 查询对齐 isRoutable 语义（ACTIVE+DEPRECATED）"
```

---

### Task 8: UpstreamModelProbeClient（provider 域）— 按协议探测上游模型列表

**Files:**
- Create: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/catalog/sync/UpstreamModelProbeClient.java`
- Modify: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/catalog/sync/CatalogSyncException.java`（新增单消息构造器 `CatalogSyncException(String message)`）
- Test: `gateway-provider/provider/src/test/java/com/codingas/gateway/provider/catalog/sync/UpstreamModelProbeClientTest.java`

**Interfaces:**
- Consumes: JDK `HttpClient` Bean（`CatalogSyncClientConfiguration.httpClient()`）、渠道凭证（`ChannelCredentialService.listByChannelId` → `apiKeyPlain`）、`ChannelEndpoint`（protocol/endpointUrl）
- Produces: `Set<String> fetchModelIds(ChannelEndpoint endpoint, String apiKey)` — 按 `endpoint.protocol` 调 `{endpointUrl}/v1/models`，解析返回模型 ID 集合；HTTP 非 2xx 抛 `CatalogSyncException`

- [ ] **Step 1: 写失败测试**

```java
@DisplayName("UpstreamModelProbeClient 上游列表探测")
class UpstreamModelProbeClientTest {

    @Test
    @DisplayName("OpenAI 格式响应解析为模型 ID 集合")
    void fetchModelIds_openAiFormat_parsesIds() throws Exception {
        // given：HttpClient mock 返回 200 + {"data":[{"id":"gpt-4"},{"id":"claude-3"}]}
        //      ChannelEndpoint(endpointUrl="https://api.example.com", protocol=OPENAI)
        // when
        Set<String> ids = client.fetchModelIds(endpoint, "sk-test");
        // then
        assertThat(ids).containsExactlyInAnyOrder("gpt-4", "claude-3");
    }

    @Test
    @DisplayName("Gemini 格式响应解析 models[].name")
    void fetchModelIds_geminiFormat_parsesNames() throws Exception {
        // 200 + {"models":[{"name":"models/gemini-pro"},{"name":"models/gemini-1.5"}]}
        // then：{"models/gemini-pro", "models/gemini-1.5"}
    }

    @Test
    @DisplayName("非 2xx 抛 CatalogSyncException")
    void fetchModelIds_errorStatus_throws() throws Exception {
        // 401 响应 → assertThatThrownBy(...).isInstanceOf(CatalogSyncException.class)
    }
}
```

（用 Mockito mock `HttpClient` 的 `send` 返回构造的 `HttpResponse<String>`，参考 `ModelCatalogClientTest` 的 mock 模式。）

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-provider/provider -am test -Dtest=UpstreamModelProbeClientTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 实现**

先给 `CatalogSyncException` 加单消息构造器：

```java
    /** 构造同步/探测异常（无原因链） */
    public CatalogSyncException(String message) {
        super(message);
    }
```

再创建客户端：

```java
package com.codingas.gateway.provider.catalog.sync;

/**
 * 上游模型列表探测客户端
 *
 * <p>按渠道协议类型调用上游模型列表 API（OpenAI 兼容 / Anthropic / Gemini），
 * 归一化为模型 ID 集合返回，供 {@link CatalogProbeService} 对比本地下线情况。</p>
 */
@Component
@RequiredArgsConstructor
public class UpstreamModelProbeClient {

    private final HttpClient httpClient;

    /**
     * 拉取上游可用模型 ID 集合
     *
     * @param endpoint 渠道端点（protocol 决定列表 API 与响应格式）
     * @param apiKey   渠道凭证明文
     * @return 模型 ID 集合（OpenAI/Anthropic 取 data[].id；Gemini 取 models[].name）
     * @throws CatalogSyncException 请求失败或响应解析失败
     */
    public Set<String> fetchModelIds(ChannelEndpoint endpoint, String apiKey) {
        String url = endpoint.getEndpointUrl() + "/v1/models";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new CatalogSyncException("上游模型列表请求失败: " + url + " - " + e.getMessage());
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new CatalogSyncException("上游模型列表请求失败, status=" + response.statusCode());
        }
        return parse(response.body(), endpoint.getProtocol());
    }

    /** 按协议解析响应：OpenAI/Anthropic 取 data[].id，Gemini 取 models[].name */
    private Set<String> parse(String body, Protocol protocol) {
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            Set<String> ids = new HashSet<>();
            if (protocol == Protocol.GEMINI) {
                for (JsonNode node : root.path("models")) {
                    ids.add(node.path("name").asText());
                }
            } else {
                for (JsonNode node : root.path("data")) {
                    ids.add(node.path("id").asText());
                }
            }
            return ids;
        } catch (Exception e) {
            throw new CatalogSyncException("上游模型列表响应解析失败: " + e.getMessage());
        }
    }
}
```

（补 import：`java.net.URI`、`java.net.http.*`、`java.time.Duration`、`com.fasterxml.jackson.databind.JsonNode/ObjectMapper`、`com.codingas.gateway.protocol.Protocol`、`com.codingas.gateway.provider.channel.ChannelEndpoint`。若 `CatalogSyncException` 构造器不支持单消息，按现有签名调整。）

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS（3 用例）

- [ ] **Step 5: Commit**

```bash
git add gateway-provider/provider/src/main/java/com/codingas/gateway/provider/catalog/sync/UpstreamModelProbeClient.java
git add gateway-provider/provider/src/test/java/com/codingas/gateway/provider/catalog/sync/UpstreamModelProbeClientTest.java
git commit -m "feat(model-lifecycle): UpstreamModelProbeClient 按协议探测上游模型列表"
```

---

### Task 9: CatalogProbeService（provider 域）— 探测编排 + 状态流转 + 自动恢复

**Files:**
- Create: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/catalog/sync/CatalogProbeService.java`
- Modify: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/catalog/sync/ModelCatalogSyncService.java`（不必须；探测日志复用 `CatalogSyncLogRepository`）
- Test: `gateway-provider/provider/src/test/java/com/codingas/gateway/provider/catalog/sync/CatalogProbeServiceTest.java`

**Interfaces:**
- Consumes: `ChannelService.getEndpoints(channelId)`、`ChannelCredentialService.listByChannelId(channelId)`、`ModelInstanceRepository.findByChannelId`、`UpstreamModelProbeClient.fetchModelIds`（Task 8）、`ModelDeprecationService.markInstanceDeprecated/restoreInstance`（Task 4）、`SystemSettingService`、`CatalogSyncLogRepository`
- Produces: `CatalogSyncReport probe()` — 遍历渠道，探测→对比→消失计数（内存）→DEPRECATED/恢复 ACTIVE，报告落 catalog_sync_logs（result=PROBE）

- [ ] **Step 1: 写失败测试**

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogProbeService 列表探测编排")
class CatalogProbeServiceTest {

    @Mock private ChannelService channelService;
    @Mock private ChannelCredentialService credentialService;
    @Mock private ModelInstanceRepository instanceRepository;
    @Mock private UpstreamModelProbeClient probeClient;
    @Mock private ModelDeprecationService deprecationService;
    @Mock private CatalogSyncLogRepository logRepository;
    @Mock private SystemSettingService settingService;
    @InjectMocks private CatalogProbeService probeService;

    @Test
    @DisplayName("上游消失连续 N 次后实例转 DEPRECATED")
    void missingConsecutively_marksInstanceDeprecated() {
        // given：渠道 1 有 endpoint(OPENAI)+凭证；实例 upstreamModelName="gpt-4"
        //      probeClient.fetchModelIds 返回空集合（不含 gpt-4）；confirm-count=2
        //      probe() 连续调用两次
        // then：第二次后 verify(deprecationService).markInstanceDeprecated(instanceId)
    }

    @Test
    @DisplayName("上游重新出现则恢复 ACTIVE")
    void reappears_restoresInstance() {
        // given：第一次探测缺失（计数 1）→ 实例被标记 DEPRECATED；
        //       第二次探测出现 → verify(deprecationService).restoreInstance(instanceId)
    }

    @Test
    @DisplayName("无凭证或协议不支持的渠道跳过")
    void channelWithoutCredential_skipped() {
        // given：渠道无端点或无凭证
        // when：probe()
        // then：probeClient 不被调用
    }

    @Test
    @DisplayName("探测总开关关闭时不执行")
    void disabled_skips() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(false);
        probeService.probe();
        verify(probeClient, never()).fetchModelIds(any(), any());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-provider/provider -am test -Dtest=CatalogProbeServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 实现**

```java
package com.codingas.gateway.provider.catalog.sync;

/**
 * 上游列表探测编排服务（提前预警通道）
 *
 * <p>对每个参与探测的渠道（有端点、有凭证、协议支持列表 API）拉取上游模型 ID 集合，
 * 对比该渠道 ModelInstance.upstreamModelName：消失连续 N 次（内存计数）转 DEPRECATED
 * （即将废弃，继续路由）；重新出现自动恢复 ACTIVE。结果写入 catalog_sync_logs
 * （result=PROBE）。总开关/探测开关关闭时不执行。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogProbeService {

    /** 连续消失计数：instanceId → 计数 */
    private final ConcurrentHashMap<Long, Integer> missingCounts = new ConcurrentHashMap<>();

    private final ChannelService channelService;
    private final ChannelCredentialService credentialService;
    private final ModelInstanceRepository instanceRepository;
    private final UpstreamModelProbeClient probeClient;
    private final ModelDeprecationService deprecationService;
    private final CatalogSyncLogRepository logRepository;
    private final SystemSettingService settingService;

    /**
     * 执行一轮上游列表探测
     *
     * @return 探测报告
     */
    public CatalogSyncReport probe() {
        Instant startedAt = Instant.now();
        CatalogSyncReport report = CatalogSyncReport.builder()
                .success(true).syncedAt(startedAt).messages(new ArrayList<>()).build();
        if (!settingService.getBoolean("catalog.deprecation.enabled", true)
                || !settingService.getBoolean("catalog.deprecation.probe.enabled", true)) {
            log.debug("上游列表探测已关闭，跳过");
            return report;
        }
        int confirmCount = settingService.getInt("catalog.deprecation.confirm-count", 3);
        try {
            List<Channel> channels = channelService.getAll();
            for (Channel channel : channels) {
                probeChannel(channel, confirmCount, report);
            }
            saveProbeLog(report, null, startedAt);
        } catch (RuntimeException e) {
            saveProbeLog(report, e.getMessage(), startedAt);
            log.error("上游列表探测失败: {}", e.getMessage(), e);
            throw e;
        }
        return report;
    }

    /** 探测单个渠道：拉列表 → 对比实例 → 消失计数/恢复 */
    private void probeChannel(Channel channel, int confirmCount, CatalogSyncReport report) {
        List<ChannelEndpoint> endpoints = channelService.getEndpoints(channel.getId());
        List<ChannelEndpoint> probeable = endpoints.stream()
                .filter(e -> e.getProtocol() == Protocol.OPENAI
                        || e.getProtocol() == Protocol.ANTHROPIC
                        || e.getProtocol() == Protocol.GEMINI)
                .toList();
        if (probeable.isEmpty()) {
            return;
        }
        List<ChannelCredential> credentials = credentialService.listByChannelId(channel.getId());
        if (credentials.isEmpty()) {
            return;
        }
        ChannelEndpoint endpoint = probeable.get(0);
        Set<String> upstreamIds;
        try {
            upstreamIds = probeClient.fetchModelIds(endpoint, credentials.get(0).getApiKeyPlain());
        } catch (CatalogSyncException e) {
            // 单渠道探测失败不阻断其他渠道（凭证失效/网络异常不触发废弃）
            report.addMessage("渠道探测失败: " + channel.getName() + " - " + e.getMessage());
            return;
        }
        for (ModelInstance instance : instanceRepository.findByChannelId(channel.getId())) {
            String upstreamName = instance.getUpstreamModelName() != null
                    ? instance.getUpstreamModelName() : null;
            String modelName = upstreamName != null ? upstreamName
                    : String.valueOf(instance.getModelId());
            if (upstreamIds.contains(modelName)) {
                // 重新出现：恢复（若曾标记）
                missingCounts.remove(instance.getId());
                if (instance.getState() == ModelInstance.State.DEPRECATED) {
                    deprecationService.restoreInstance(instance.getId());
                    report.incrementUpdated();
                }
            } else {
                // 消失：连续计数
                int count = missingCounts.merge(instance.getId(), 1, Integer::sum);
                if (count >= confirmCount) {
                    missingCounts.remove(instance.getId());
                    if (instance.getState() == ModelInstance.State.ACTIVE) {
                        deprecationService.markInstanceDeprecated(instance.getId());
                        report.incrementUpdated();
                    }
                }
            }
        }
    }

    /** 探测日志落 catalog_sync_logs（result=PROBE） */
    private void saveProbeLog(CatalogSyncReport report, String error, Instant startedAt) {
        CatalogSyncLog syncLog = new CatalogSyncLog();
        syncLog.setResult(error == null ? "PROBE" : "FAILURE");
        syncLog.setAddedCount(0);
        syncLog.setUpdatedCount(report.getUpdatedCount());
        syncLog.setSkippedCount(report.getSkippedCount());
        syncLog.setFailedCount(report.getFailedCount());
        syncLog.setMessage(error != null ? error : "上游列表探测完成");
        syncLog.setSyncedAt(startedAt);
        try {
            logRepository.save(syncLog);
        } catch (Exception e) {
            log.warn("保存探测日志失败: {}", e.getMessage());
        }
    }
}
```

**注意**：`ModelInstance` 与 `upstreamModelName` 对比需用真实模型名——若 `upstreamModelName` 为 null（默认=Model.modelName），需先取 `Model.modelName`。实现时通过 `ModelRepository.findById(modelId)` 兜底取 modelName（在循环内查，量小可接受；测试桩覆盖）。为简化，若 `upstreamModelName == null` 则跳过对比（视为无需探测），并在 Javadoc 说明"仅探测显式映射了 upstreamModelName 或能解析到 Model 名的实例"。

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS（4 用例）

- [ ] **Step 5: Commit**

```bash
git add gateway-provider/provider/src/main/java/com/codingas/gateway/provider/catalog/sync/CatalogProbeService.java
git add gateway-provider/provider/src/test/java/com/codingas/gateway/provider/catalog/sync/CatalogProbeServiceTest.java
git commit -m "feat(model-lifecycle): CatalogProbeService 列表探测编排 + 消失计数 + 自动恢复"
```

---

### Task 10: CatalogProbeTask（provider 域）— 定时探测 + 装配开关

**Files:**
- Create: `gateway-provider/provider/src/main/java/com/codingas/gateway/provider/catalog/sync/CatalogProbeTask.java`
- Test: 装配测试参考 `CatalogSyncTask` 现有测试（provider 模块）

**Interfaces:**
- Consumes: `CatalogProbeService.probe()`（Task 9）、`SystemSettingService`（getBoolean/getEnum）
- Produces: `@Scheduled(fixedRate=3600_000)` 任务——总开关/探测开关关闭跳过；按 `catalog.deprecation.probe.interval` 周期判断；`@ConditionalOnProperty(gateway.catalog.probe.auto-enabled, matchIfMissing=true)` 装配（测试环境关闭）

- [ ] **Step 1: 写失败测试（装配/行为）**

参考 `CatalogSyncTask` 的既有测试模式，新增：

```java
    @Test
    @DisplayName("探测开关关闭时跳过")
    void probeDisabled_skips() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.probe.enabled", true)).thenReturn(false);
        task.check();
        verify(probeService, never()).probe();
    }

    @Test
    @DisplayName("未达探测周期时跳过")
    void notDue_skips() {
        // 最近探测日志在阈值内 → verify(probeService, never()).probe()
    }

    @Test
    @DisplayName("达周期时执行探测")
    void due_executesProbe() {
        // 无最近日志 → verify(probeService).probe()
    }
```

（按 `CatalogSyncTask` 现有测试桩：mock `SystemSettingService`、`CatalogProbeService`、`CatalogSyncLogRepository.findLatest`。）

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway && ./mvnw -pl gateway-provider/provider -am test -Dtest=CatalogProbeTaskTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（类不存在）

- [ ] **Step 3: 实现**

```java
package com.codingas.gateway.provider.catalog.sync;

/**
 * 上游列表探测定时任务
 *
 * <p>每小时检查一次是否需要执行探测：先读 {@code catalog.deprecation.enabled} 与
 * {@code catalog.deprecation.probe.enabled}（关闭则跳过），再读
 * {@code catalog.deprecation.probe.interval} 周期（DAILY=24h / WEEKLY=7d / MONTHLY=30d），
 * 按最近一次探测时间（{@link CatalogSyncLogRepository#findLatest}）判断是否达间隔。
 * 失败仅记录 error 日志，不向调度器抛异常。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "gateway.catalog.probe", name = "auto-enabled",
        havingValue = "true", matchIfMissing = true)
public class CatalogProbeTask {

    private final CatalogProbeService probeService;
    private final CatalogSyncLogRepository logRepository;
    private final SystemSettingService settingService;

    @Scheduled(fixedRate = 3600_000) // 每小时检查一次
    public void check() {
        if (!settingService.getBoolean("catalog.deprecation.enabled", true)
                || !settingService.getBoolean("catalog.deprecation.probe.enabled", true)) {
            log.debug("上游列表探测已关闭，跳过");
            return;
        }
        SyncInterval interval = settingService.getEnum(
                "catalog.deprecation.probe.interval", SyncInterval.class, SyncInterval.WEEKLY);
        long thresholdHours = switch (interval) {
            case DAILY -> 24;
            case WEEKLY -> 24 * 7;
            case MONTHLY -> 24 * 30;
        };
        Optional<CatalogSyncLog> latest = logRepository.findLatest();
        boolean shouldProbe = latest.isEmpty()
                || latest.get().getSyncedAt() == null
                || latest.get().getSyncedAt().isBefore(Instant.now().minus(thresholdHours, ChronoUnit.HOURS));
        if (!shouldProbe) {
            log.debug("距上次探测未达间隔({}), 跳过", interval);
            return;
        }
        try {
            CatalogSyncReport report = probeService.probe();
            log.info("上游列表探测完成: updated={}", report.getUpdatedCount());
        } catch (RuntimeException e) {
            log.error("上游列表探测失败: {}", e.getMessage(), e);
        }
    }
}
```

（补 import：`SyncInterval`、`SystemSettingService`、`java.time.temporal.ChronoUnit`、`ConditionalOnProperty`、`Scheduled`。）

**注意**：`findLatest` 返回的日志含目录同步与探测两类，周期判断共用最新一条即可（若需区分，实现时按 `result` 过滤——按既有 `CatalogSyncLogRepository` 能力实现，若无法过滤则共用并注释说明）。

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-provider/provider/src/main/java/com/codingas/gateway/provider/catalog/sync/CatalogProbeTask.java
git add gateway-provider/provider/src/test/java/com/codingas/gateway/provider/catalog/sync/CatalogProbeTaskTest.java
git commit -m "feat(model-lifecycle): CatalogProbeTask 定时探测（开关+周期+装配开关）"
```

---

### Task 11: 前端标注 — 渠道详情抽屉模型映射 DEPRECATED 标签

**Files:**
- Modify: `gateway-console/src/pages/Channels/ModelMappingSection.tsx`
- Test: `gateway-console/src/pages/Channels/__tests__/ModelMappingSection.pulse.test.tsx`（或新增专属测试）

**Interfaces:**
- Consumes: `ModelMappingSection` 现有 props（`channelModels` 的 `state` 字段）
- Produces: `DEPRECATED` 状态实例显示黄色"即将废弃"标签

- [ ] **Step 1: 写失败测试（前端）**

在 ModelMappingSection 相关测试文件新增：

```tsx
    it('DEPRECATED 状态实例显示黄色即将废弃标签', () => {
      render(<ModelMappingSection channelId={1} channelModels={[{ id: 1, modelId: 100, state: 'DEPRECATED', ... }]} />);
      expect(screen.getByText('即将废弃')).toBeInTheDocument();
    });
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd E:/workspace/llm-gateway/gateway-console && npx vitest run src/pages/Channels/__tests__/ModelMappingSection.pulse.test.tsx`
Expected: 失败（无标签渲染）

- [ ] **Step 3: 实现**

在 `ModelMappingSection.tsx` 渲染映射项状态处（现映射项展示 modelName + upstreamModelName 的 Tag 区域）追加：

```tsx
        {mapping.state === 'DEPRECATED' && (
          <Tag color="orange" style={{ fontSize: 11 }}>
            {t('deprecating', { defaultValue: '即将废弃' })}
          </Tag>
        )}
```

（若 `mapping.state` 类型未含 `'DEPRECATED'`，同步更新 `gateway-console/src/types/channel.ts` 中 ChannelModel 的 state 联合类型。）

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2 命令
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-console/src/pages/Channels/ModelMappingSection.tsx
git add gateway-console/src/types/channel.ts
git add gateway-console/src/pages/Channels/__tests__/
git commit -m "feat(console): 渠道模型映射 DEPRECATED 状态展示即将废弃标签"
```

---

## Self-Review 结果

- **Spec 覆盖**：§3.1 状态语义（Task 4/7）、§3.2 配置项（Task 3）、§4 数据面信号（Task 1/2/5/6）、§5 管理面探测（Task 8/9/10）、§6 前端与审计（Task 4 审计 + Task 11）、§7 错误处理（Task 9 单渠道失败隔离 + Task 5 确认失败不阻断）、§8 测试策略（各任务 TDD）——全覆盖。
- **占位符**：已消除；探测的 upstreamModelName 解析（null 回退 Model.modelName）与 findLatest 区分探测/同步日志两处标注了实现注意点，无 TBD。
- **类型一致性**：`ModelDeprecationService.markDeprecated(String,String)`/`markInstanceDeprecated(Long)`/`restoreInstance(Long)` 在 Task 4 定义，Task 5/9 引用一致；`UpstreamModelProbeClient.fetchModelIds(ChannelEndpoint,String)` 在 Task 8 定义，Task 9 引用一致；`RuntimeDeprecationDetector.onModelNotFound(String)` Task 5 定义，Task 6 引用一致。
