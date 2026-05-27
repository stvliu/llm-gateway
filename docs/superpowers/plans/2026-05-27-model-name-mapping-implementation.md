# 模型名映射实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现模型名映射的完整链路——路由时携带上游模型名、出站时替换模型名、Catalog 物化时预填映射值、提供用户面模型发现 API。

**Architecture:** RoutingContext 增加两个 String 字段（保持值对象轻量），RoutingResolver 组装时从 Model/ChannelModel 提取填入，OutboundTuner 直接读取替换 request.model。CatalogMaterializeService 增加内置映射表，物化 Plan 时预填 upstreamModelName。新建 ModelDiscoveryController 提供兼容 OpenAI 的 /v1/models 端点。

**Tech Stack:** Java 21 + Spring Boot 3.5.x, JPA, Jackson, Spring MVC

---

### Task 1: RoutingContext 增加模型名字段 + OutboundTuner 模型名替换

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/valueobject/RoutingContext.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RoutingResolver.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/OutboundTuner.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/OutboundTunerTest.java`

- [ ] **Step 1: RoutingContext record 增加 modelName/upstreamModelName 字段**

```java
// 修改后
public record RoutingContext(
        Long channelId,
        Long channelEndpointId,
        String endpointUrl,
        Protocol upstreamProtocol,
        String providerApiKey,
        Integer timeout,
        boolean needsProtocolAdaptation,
        String modelName,          // 用户传入的模型名，对应 Model.modelName
        String upstreamModelName   // 上游模型名，null 表示与 modelName 相同
) {}
```

文件: `domain/supply/valueobject/RoutingContext.java`，在 `needsProtocolAdaptation` 之后追加两个字段。

- [ ] **Step 2: 修改 RoutingResolver.resolve() 传入模型名字段**

修改 `RoutingResolver.java` 中组装 RoutingContext 的部分（第 55-63 行），在 `needsAdaptation` 之后追加 modelName 和 upstreamModelName：

```java
return new RoutingContext(
        channel.getId(),
        endpoint.getId(),
        endpoint.getEndpointUrl(),
        endpoint.getProtocol(),
        apiKey,
        channel.getTimeout(),
        needsAdaptation,
        model.getModelName(),           // 新增
        channelModel.getUpstreamModelName()  // 新增
);
```

- [ ] **Step 3: 修改 OutboundTuner.tune() 实现模型名替换**

替换 OutboundTuner.java 第 53-54 行的 TODO 注释为实际逻辑：

```java
// 第二层：通道级调谐 — 模型名替换
String upstreamModelName = context.upstreamModelName();
if (upstreamModelName != null && !upstreamModelName.isBlank()) {
    request.setModel(upstreamModelName);
    log.debug("模型名替换: {} -> {}", context.modelName(), upstreamModelName);
}
```

- [ ] **Step 4: 编写 OutboundTuner 测试**

创建 `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/OutboundTunerTest.java`:

```java
package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboundTunerTest {

    private OutboundTuner tuner;

    @BeforeEach
    void setUp() {
        // 无协议调谐器，只测试模型名替换
        tuner = new OutboundTuner(List.of());
    }

    @Test
    void shouldReplaceModelNameWhenUpstreamModelNameIsNotNull() {
        RoutingContext ctx = new RoutingContext(
                1L, 1L, "https://api.example.com", Protocol.OPENAI,
                "sk-test", 30, false,
                "deepseek-v4-flash", "deepseek-v4-flash-260425");

        ProtocolRequest request = new OpenAIChatRequest();
        request.setModel("deepseek-v4-flash");

        ProtocolRequest result = tuner.tune(request, ctx);

        assertEquals("deepseek-v4-flash-260425", result.getModel());
    }

    @Test
    void shouldKeepModelNameWhenUpstreamModelNameIsNull() {
        RoutingContext ctx = new RoutingContext(
                1L, 1L, "https://api.example.com", Protocol.OPENAI,
                "sk-test", 30, false,
                "deepseek-v4-flash", null);

        ProtocolRequest request = new OpenAIChatRequest();
        request.setModel("deepseek-v4-flash");

        ProtocolRequest result = tuner.tune(request, ctx);

        assertEquals("deepseek-v4-flash", result.getModel());
    }

    @Test
    void shouldKeepModelNameWhenUpstreamModelNameIsBlank() {
        RoutingContext ctx = new RoutingContext(
                1L, 1L, "https://api.example.com", Protocol.OPENAI,
                "sk-test", 30, false,
                "deepseek-v4-flash", "  ");

        ProtocolRequest request = new OpenAIChatRequest();
        request.setModel("deepseek-v4-flash");

        ProtocolRequest result = tuner.tune(request, ctx);

        assertEquals("deepseek-v4-flash", result.getModel());
    }
}
```

**注**：`OpenAIChatRequest` 需要能通过无参构造器创建。如果构造函数需要参数，使用 builder 模式或反射设置字段。

- [ ] **Step 5: 运行测试验证**

Run: `./mvnw test -pl gateway-boot -Dtest=OutboundTunerTest -q`
Expected: 3 tests PASS

- [ ] **Step 6: 更新 ChatDispatchServiceTest 中 RoutingContext 构造**

搜索 `ChatDispatchServiceTest.java` 中所有 `new RoutingContext(` 调用处，追加两个参数（`"test-model", null`），保持编译通过。确认测试中不验证模型名替换行为（已在 OutboundTunerTest 中覆盖）。

Run: `./mvnw test -pl gateway-boot -Dtest=ChatDispatchServiceTest -q`
Expected: All tests PASS

- [ ] **Step 7: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/supply/valueobject/RoutingContext.java \
       gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RoutingResolver.java \
       gateway-boot/src/main/java/com/codingas/gateway/application/proxy/OutboundTuner.java \
       gateway-boot/src/test/java/com/codingas/gateway/application/proxy/OutboundTunerTest.java
git commit -m "feat: OutboundTuner 模型名替换 + RoutingContext 增加模型名字段"
```

---

### Task 2: CatalogMaterializeService 预填 upstreamModelName

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/catalog/CatalogMaterializeService.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/catalog/CatalogMaterializeServiceTest.java`

- [ ] **Step 1: 在 CatalogMaterializeService 中添加 UPSTREAM_MODEL_NAME_RULES 映射表**

在 CatalogMaterializeService 的字段声明区添加：

```java
/** 内置上游模型名映射规则表 */
private static final Map<String, Map<String, String>> UPSTREAM_MODEL_NAME_RULES = Map.of(
        "aws-bedrock", Map.ofEntries(
                Map.entry("claude-opus-4-7", "anthropic.claude-opus-4-7"),
                Map.entry("claude-sonnet-4-6", "anthropic.claude-sonnet-4-6"),
                Map.entry("claude-haiku-4-5", "anthropic.claude-haiku-4-5-20251001-v1:0"),
                Map.entry("claude-3-opus-20240229", "anthropic.claude-3-opus-20240229-v1:0"),
                Map.entry("claude-3-sonnet-20240229", "anthropic.claude-3-sonnet-20240229-v1:0"),
                Map.entry("claude-3-haiku-20240307", "anthropic.claude-3-haiku-20240307-v1:0")
        ),
        "azure-openai", Map.of(
                "chat-latest", "gpt-chat-latest"
        )
);
```

- [ ] **Step 2: 添加 resolveUpstreamModelName 方法**

在 CatalogMaterializeService 中添加解析方法（放在辅助方法区，例如 `findOrCreateModel` 之后）：

```java
/**
 * 解析上游模型名
 *
 * <p>根据供应商编码和模型名，在内置映射表中查找对应的上游模型名。
 * 未命中则返回 null（走默认值 = Model.modelName）。</p>
 *
 * @param providerCode 供应商编码
 * @param modelName    用户面模型名
 * @return 上游模型名，null 表示与 modelName 相同
 */
private String resolveUpstreamModelName(String providerCode, String modelName) {
    Map<String, String> rules = UPSTREAM_MODEL_NAME_RULES.get(providerCode);
    if (rules == null) {
        return null;
    }
    return rules.get(modelName);
}
```

- [ ] **Step 3: 在 materializePlan() 中预填 upstreamModelName**

修改 `CatalogMaterializeService.materializePlan()` 中创建 ChannelModel 的循环（第 153-164 行），在 `setModelId` 之后、`setInputPrice` 之前增加 upstreamModelName 预填：

```java
ChannelModel channelModel = new ChannelModel();
channelModel.setChannelId(savedChannel.getId());
channelModel.setModelId(model.getId());
// 预填上游模型名
String resolved = resolveUpstreamModelName(provider.getCode(), modelName);
channelModel.setUpstreamModelName(resolved);
channelModel.setInputPrice(toBigDecimal(p.get("inputPrice")));
```

- [ ] **Step 4: 编写测试**

在 `CatalogMaterializeServiceTest.java` 中补充两个测试方法（放在 `materializePlan` 测试组中）：

```java
@Test
void shouldPrefillUpstreamModelNameWhenRulesMatch() {
    // 准备：azure-openai 供应商 + chat-latest 模型
    when(providerGateway.findByCode("azure-openai"))
            .thenReturn(Optional.of(createProvider("azure-openai")));
    when(planCatalogGateway.findByPlanCode("azure-openai-standard"))
            .thenReturn(Optional.of(createAzurePlan()));
    when(channelGateway.existsByProviderIdAndName(anyLong(), eq("azure-openai-standard")))
            .thenReturn(false);
    when(channelGateway.save(any())).thenReturn(createChannel(1L));
    when(modelGateway.findByModelName("chat-latest"))
            .thenReturn(Optional.of(createModel(1L, "chat-latest")));

    MaterializeResult result = service.materializePlan("azure-openai-standard");

    assertThat(result.getStatus()).isEqualTo("CREATED");
    ArgumentCaptor<ChannelModel> captor = ArgumentCaptor.forClass(ChannelModel.class);
    verify(channelModelGateway, times(1)).save(captor.capture());
    assertThat(captor.getValue().getUpstreamModelName()).isEqualTo("gpt-chat-latest");
}

@Test
void shouldSetNullUpstreamModelNameWhenNoRuleMatches() {
    // 准备：非受管供应商（如 deepseek）
    when(providerGateway.findByCode("deepseek"))
            .thenReturn(Optional.of(createProvider("deepseek")));
    when(planCatalogGateway.findByPlanCode("deepseek-standard"))
            .thenReturn(Optional.of(createDeepseekPlan()));
    when(channelGateway.existsByProviderIdAndName(anyLong(), eq("deepseek-standard")))
            .thenReturn(false);
    when(channelGateway.save(any())).thenReturn(createChannel(2L));
    when(modelGateway.findByModelName("deepseek-v4-flash"))
            .thenReturn(Optional.of(createModel(2L, "deepseek-v4-flash")));

    MaterializeResult result = service.materializePlan("deepseek-standard");

    assertThat(result.getStatus()).isEqualTo("CREATED");
    ArgumentCaptor<ChannelModel> captor = ArgumentCaptor.forClass(ChannelModel.class);
    verify(channelModelGateway, times(1)).save(captor.capture());
    assertThat(captor.getValue().getUpstreamModelName()).isNull();
}
```

需要补充的辅助方法（或在已有的 fixture 基础上追加）：

```java
private Provider createProvider(String code) {
    Provider p = new Provider();
    p.setId(1L);
    p.setCode(code);
    p.setName(code);
    p.setState(ProviderState.ACTIVE);
    return p;
}

private Model createModel(Long id, String modelName) {
    Model m = new Model();
    m.setId(id);
    m.setModelName(modelName);
    m.setState(ModelState.ACTIVE);
    return m;
}

private PlanCatalog createAzurePlan() {
    PlanCatalog p = new PlanCatalog();
    p.setPlanCode("azure-openai-standard");
    p.setProviderCode("azure-openai");
    p.setBillingMode(BillingMode.PAY_AS_YOU_GO);
    p.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://azure.openai.com\"}]");
    p.setPricing("[{\"modelName\":\"chat-latest\",\"inputPrice\":2.5,\"outputPrice\":10.0}]");
    return p;
}

private PlanCatalog createDeepseekPlan() {
    PlanCatalog p = new PlanCatalog();
    p.setPlanCode("deepseek-standard");
    p.setProviderCode("deepseek");
    p.setBillingMode(BillingMode.PAY_AS_YOU_GO);
    p.setEndpoints("[{\"protocol\":\"OPENAI\",\"url\":\"https://api.deepseek.com\"}]");
    p.setPricing("[{\"modelName\":\"deepseek-v4-flash\",\"inputPrice\":1.0,\"outputPrice\":4.0}]");
    return p;
}

private Channel createChannel(Long id) {
    Channel c = new Channel();
    c.setId(id);
    c.setState(ChannelState.ACTIVE);
    return c;
}
```

- [ ] **Step 5: 运行测试**

Run: `./mvnw test -pl gateway-boot -Dtest=CatalogMaterializeServiceTest -q`
Expected: All tests PASS

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/catalog/CatalogMaterializeService.java
git commit -m "feat: Catalog 物化时预填 upstreamModelName 映射规则"
```

---

### Task 3: 用户面 /v1/models 接口

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ModelDiscoveryController.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/model/ModelDiscoveryService.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/application/model/dto/ModelDiscoveryResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/interceptor/ApiKeyAuthInterceptor.java`（如需要注册 /v1/models 路径）
- Test: `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ModelDiscoveryControllerTest.java`

- [ ] **Step 1: 创建 ModelDiscoveryResponse DTO**

新建 `gateway-boot/src/main/java/com/codingas/gateway/application/model/dto/ModelDiscoveryResponse.java`：

```java
package com.codingas.gateway.application.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 模型发现响应（兼容 OpenAI /v1/models 格式）
 */
@Data
@AllArgsConstructor
public class ModelDiscoveryResponse {
    private String object;
    private List<ModelItem> data;

    @Data
    @AllArgsConstructor
    public static class ModelItem {
        private String id;
        private String object;
        private long created;
        private String ownedBy;
    }
}
```

- [ ] **Step 2: 创建 ModelDiscoveryService**

新建 `gateway-boot/src/main/java/com/codingas/gateway/application/model/ModelDiscoveryService.java`：

```java
package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型发现服务
 *
 * <p>根据 API Key 可见的渠道，返回可用的模型列表。</p>
 */
@Service
@RequiredArgsConstructor
public class ModelDiscoveryService {

    private final UserApiKeyGateway userApiKeyGateway;
    private final ChannelModelGateway channelModelGateway;
    private final ModelGateway modelGateway;

    /**
     * 获取 API Key 可见的模型列表
     *
     * @param apiKeyId API Key ID（已认证的身份）
     * @return 兼容 OpenAI 格式的模型列表
     */
    public ModelDiscoveryResponse getVisibleModels(Long apiKeyId) {
        UserApiKey apiKey = userApiKeyGateway.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在"));

        List<Long> channelIds = apiKey.getChannelIds();
        if (channelIds == null || channelIds.isEmpty()) {
            return new ModelDiscoveryResponse("list", List.of());
        }

        // 通过渠道关联查询活跃的 ChannelModel → 关联的活跃 Model（去重）
        List<Model> visibleModels = channelIds.stream()
                .flatMap(channelId -> channelModelGateway.findActiveByChannelId(channelId).stream())
                .filter(cm -> ChannelModelState.ACTIVE.equals(cm.getState()))
                .map(cm -> modelGateway.findById(cm.getModelId()).orElse(null))
                .filter(m -> m != null && ModelState.ACTIVE.equals(m.getState()))
                .distinct()
                .toList();

        List<ModelDiscoveryResponse.ModelItem> items = visibleModels.stream()
                .map(m -> new ModelDiscoveryResponse.ModelItem(
                        m.getModelName(),
                        "model",
                        m.getCreatedAt() != null ? m.getCreatedAt().getEpochSecond() : 0L,
                        "system"
                ))
                .toList();

        return new ModelDiscoveryResponse("list", items);
    }
}
```

**注**：依赖的 Gateway 方法需确保存在：
- `UserApiKeyGateway.findById(Long id)` — 应存在
- `ChannelModelGateway.findActiveByChannelId(Long channelId)` — 需确认此方法存在，不存在则需在 `ChannelModelRepository` 中添加
- `ModelGateway.findById(Long id)` — 应存在
- `Model.getCreatedAt()` — 继承自 `BaseEntity`，应为 `Instant` 类型

- [ ] **Step 3: 确认 ChannelModelGateway 有 findActiveByChannelId 方法**

搜索 `ChannelModelGateway.java` 接口文件，确认已有 `findActiveByChannelId(Long channelId)` 方法。如果没有，添加：

```java
// domain/supply/gateway/ChannelModelGateway.java
List<ChannelModel> findActiveByChannelId(Long channelId);
```

并在 `ChannelModelRepository` 中实现（若也缺失）：

```java
// infrastructure/supply/gateway/database/repository/ChannelModelRepository.java
@Query("SELECT c FROM ChannelModelDo c WHERE c.channelId = :channelId AND c.state = 'ACTIVE'")
List<ChannelModelDo> findActiveByChannelId(@Param("channelId") Long channelId);
```

并在 `ChannelModelGatewayImpl` 中转换。

- [ ] **Step 4: 创建 ModelDiscoveryController**

新建 `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ModelDiscoveryController.java`：

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.model.ModelDiscoveryService;
import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户面模型发现控制器
 *
 * <p>兼容 OpenAI /v1/models 格式，供 API Key 持有者查询可用模型。</p>
 */
@RestController
@RequestMapping("/v1/models")
@RequiredArgsConstructor
public class ModelDiscoveryController {

    private final ModelDiscoveryService modelDiscoveryService;

    /**
     * 获取可见模型列表
     */
    @GetMapping
    public ModelDiscoveryResponse listModels(HttpServletRequest request) {
        Identity identity = (Identity) request.getAttribute("identity");
        if (identity == null || identity.credentialId() == null) {
            throw new IllegalArgumentException("缺少认证信息");
        }
        return modelDiscoveryService.getVisibleModels(identity.credentialId());
    }
}
```

- [ ] **Step 5: 确认鉴权路径 —— 验证 RoutingResolverTest 编译**

搜索所有 `new RoutingContext(` 调用处，确认以下测试/源码文件已更新：

- `RoutingResolverTest.java` 中的 mock 构造
- `ChatDispatchServiceTest.java` 中的 mock 构造
- `ChatDispatchServiceImpl.java` 中的调用（已在第 55-63 行附近，由 Step 2 修改）

每个 `new RoutingContext(` 调用都需要追加两个参数：`modelName, upstreamModelName`。对于测试中不关注模型名的场景，填入 `"test-model", null`。

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: 编写测试**

新建 `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ModelDiscoveryControllerTest.java`：

```java
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.model.ModelDiscoveryService;
import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModelDiscoveryController.class)
class ModelDiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModelDiscoveryService modelDiscoveryService;

    @Test
    void shouldReturnModelList() throws Exception {
        var items = List.of(
                new ModelDiscoveryResponse.ModelItem("deepseek-v4-flash", "model", 1700000000L, "system")
        );
        var response = new ModelDiscoveryResponse("list", items);

        // 需要根据实际鉴权配置来调整——这里假设无需鉴权或 mock 了鉴权
        when(modelDiscoveryService.getVisibleModels(1L)).thenReturn(response);

        mockMvc.perform(get("/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object").value("list"))
                .andExpect(jsonPath("$.data[0].id").value("deepseek-v4-flash"));
    }
}
```

**注意**：测试需模拟 API Key 鉴权。当前鉴权体系通过 `ApiKeyAuthInterceptor` 拦截 `/v1/**` 路径注入 `Identity` 到 request attribute。单元测试中可以直接调用 Controller 并 mock request attribute，或通过 `MockMvc` 加自定义 filter 注入 identity。根据项目现有测试风格（参考 `ChatDispatchServiceTest.java` 中如何 mock 认证）调整。

- [ ] **Step 7: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ModelDiscoveryController.java \
       gateway-boot/src/main/java/com/codingas/gateway/application/model/ModelDiscoveryService.java \
       gateway-boot/src/main/java/com/codingas/gateway/application/model/dto/ModelDiscoveryResponse.java \
       gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ModelDiscoveryControllerTest.java
git commit -m "feat: 用户面 /v1/models 模型发现接口"
```

---

### Task 4: 编译验证 + 集成测试

- [ ] **Step 1: 全量编译**

Run: `./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行全部测试**

Run: `./mvnw test -pl gateway-boot -q`
Expected: All tests PASS

- [ ] **Step 3: 提交最终累积修改**

```bash
git add -A
git commit -m "feat: 模型名映射完整实现 — RoutingContext + OutboundTuner + Catalog 预填 + /v1/models"
```