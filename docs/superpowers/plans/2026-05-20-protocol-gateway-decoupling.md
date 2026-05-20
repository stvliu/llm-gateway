# 端点协议与供应商解耦重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将供应商和协议职责完全解耦，端点只跟产品关联，协议类型由 ProtocolGateway 实现类自声明

**Architecture:** 新增 ProtocolGateway 接口及 OpenAI/Anthropic 实现，移除 ProviderType 枚举和 Provider.baseUrl，Product.endpoints key 由 ProtocolGateway.getProtocolName() 定义，前端从后端动态获取协议列表

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, React + Ant Design, i18next

---

## File Structure

### 新建
| 文件 | 职责 |
|------|------|
| `domain/proxy/gateway/ProtocolGateway.java` | 协议网关接口（认证、请求构建、响应解析） |
| `domain/proxy/gateway/ProtocolGatewayRegistry.java` | 协议网关注册表接口 |
| `infrastructure/proxy/gateway/protocol/OpenAIProtocolGateway.java` | OpenAI 协议实现 |
| `infrastructure/proxy/gateway/protocol/AnthropicProtocolGateway.java` | Anthropic 协议实现 |
| `infrastructure/proxy/gateway/protocol/ProtocolGatewayRegistryImpl.java` | 注册表实现 |
| `adapter/api/ProtocolController.java` | 协议列表 REST API |
| `db/migration/V17__protocol_gateway_and_provider_simplify.sql` | 数据库迁移 |

### 修改
| 文件 | 变更 |
|------|------|
| `domain/model/entity/Provider.java` | 移除 baseUrl，type 从 ProviderType 改为 String |
| `domain/product/entity/Product.java` | getDefaultEndpoint 优化 |
| `domain/proxy/entity/RoutingContext.java` | providerType 从 ProviderType 改为 String |
| `domain/proxy/gateway/LLMGateway.java` | getProviderType 返回 String |
| `domain/proxy/gateway/LLMGatewayRegistry.java` | getGateway 参数改为 String |
| `domain/model/entity/ProviderCapabilities.java` | providerType 改为 String |
| `application/proxy/ProductRoutingService.java` | 使用 ProtocolGateway 路由 |
| `application/proxy/ChannelRoutingService.java` | 移除 provider.getBaseUrl() |
| `application/proxy/ProxyServiceImpl.java` | 使用 ProtocolGateway |
| `application/provider/ProviderServiceImpl.java` | providerType→String，移除 baseUrl |
| `application/provider/dto/*` | providerType→String，移除 baseUrl |
| `application/experience/*` | providerType→String |
| `application/metadata/*` | providerType→String |
| `adapter/api/ProviderController.java` | getProviderTypes 改为返回供应商名称 |
| `infrastructure/proxy/gateway/rpc/*Adapter.java` | getProviderType 返回 String |
| `infrastructure/proxy/gateway/rpc/AdapterBuilderFactory.java` | 基于 ProtocolGateway |
| `infrastructure/model/gateway/ProviderGatewayImpl.java` | 移除 baseUrl，type→String |
| `infrastructure/model/gateway/database/dataobject/ProviderDo.java` | 移除 baseUrl，type→String |
| `infrastructure/init/DataInitializer.java` | 端点写入产品 |
| `gateway-console/src/types/product.ts` | 端点 key 动态获取 |
| `gateway-console/src/pages/Providers/ProductFormModal.tsx` | 端点 key 改为动态 Select |

### 删除
| 文件 | 原因 |
|------|------|
| `domain/model/enums/ProviderType.java` | 品牌标识改用 Provider.name |

---

## Task 1: 新增 ProtocolGateway 接口和注册表

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/gateway/ProtocolGateway.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/gateway/ProtocolGatewayRegistry.java`

- [ ] **Step 1: 创建 ProtocolGateway 接口**

```java
package com.codingas.gateway.domain.proxy.gateway;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;

/**
 * 协议网关接口
 *
 * <p>按协议（而非供应商）定义请求/响应处理能力。</p>
 * <p>每个协议类型对应一个实现类，实现类通过 getProtocolName() 自声明唯一标识。</p>
 */
public interface ProtocolGateway {

    /**
     * 协议唯一标识（如 "openai", "anthropic"）
     */
    String getProtocolName();

    /**
     * 协议显示名称
     */
    String getProtocolLabel();

    /**
     * 验证 API Key 格式是否合法
     */
    boolean validateApiKeyFormat(String apiKey);

    /**
     * 非流式聊天请求
     */
    LLMResponse chat(LLMRequest request, String baseUrl, String apiKey, int timeoutSeconds);

    /**
     * 流式聊天请求
     */
    void chatStream(LLMRequest request, String baseUrl, String apiKey, int timeoutSeconds, StreamCallback callback);

    /**
     * 获取默认 Base URL
     */
    String getDefaultBaseUrl();

    /**
     * 获取默认测试模型
     */
    String getDefaultTestModel();

    /**
     * 连通性测试
     */
    ConnectivityTestResult testConnectivity(String apiKey, String baseUrl, String model);
}
```

- [ ] **Step 2: 创建 ProtocolGatewayRegistry 接口**

```java
package com.codingas.gateway.domain.proxy.gateway;

import java.util.List;
import java.util.Optional;

/**
 * 协议网关注册表
 *
 * <p>按协议名称查找 ProtocolGateway 实现。</p>
 */
public interface ProtocolGatewayRegistry {

    /**
     * 根据协议名称获取网关
     */
    Optional<ProtocolGateway> getGateway(String protocolName);

    /**
     * 获取所有已注册的协议网关
     */
    List<ProtocolGateway> getAllGateways();
}
```

- [ ] **Step 3: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/gateway/ProtocolGateway.java gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/gateway/ProtocolGatewayRegistry.java
git commit -m "feat: 新增 ProtocolGateway 接口和注册表"
```

---

## Task 2: 实现 OpenAIProtocolGateway

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/proxy/gateway/protocol/OpenAIProtocolGateway.java`

- [ ] **Step 1: 创建 OpenAIProtocolGateway**

从 `OpenAIAdapter` 中提取协议相关逻辑（请求构建、响应解析、认证方式、连通性测试），
封装为 ProtocolGateway 实现。核心方法：

- `getProtocolName()` → `"openai"`
- `getProtocolLabel()` → `"OpenAI Chat Completions 协议"`
- `validateApiKeyFormat()` → `apiKey.startsWith("sk-")`
- `chat()` → 构建 OpenAI 格式请求，`Authorization: Bearer` 认证
- `chatStream()` → 流式版本
- `getDefaultBaseUrl()` → `"https://api.openai.com"`
- `getDefaultTestModel()` → `"gpt-4o-mini"`
- `testConnectivity()` → 从 OpenAIAdapter.testConnectivity 迁移

复用 OpenAIAdapter 中已有的 `buildRequestBody`、`parseResponse`、`testLevel1ModelsApi`、`testLevel2ChatCompletion` 等方法。
使用共享的 OkHttpClient（通过构造注入）。

- [ ] **Step 2: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/proxy/gateway/protocol/OpenAIProtocolGateway.java
git commit -m "feat: 实现 OpenAIProtocolGateway"
```

---

## Task 3: 实现 AnthropicProtocolGateway

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/proxy/gateway/protocol/AnthropicProtocolGateway.java`

- [ ] **Step 1: 创建 AnthropicProtocolGateway**

从 `AnthropicAdapter` 中提取协议相关逻辑，封装为 ProtocolGateway 实现。核心方法：

- `getProtocolName()` → `"anthropic"`
- `getProtocolLabel()` → `"Anthropic Messages 协议"`
- `validateApiKeyFormat()` → `apiKey.startsWith("sk-ant-")`
- `chat()` → 构建 Anthropic 格式请求，`x-api-key` + `anthropic-version` 认证
- `chatStream()` → 流式版本
- `getDefaultBaseUrl()` → `"https://api.anthropic.com"`
- `getDefaultTestModel()` → `"claude-haiku-3-5-20250514"`
- `testConnectivity()` → 从 AnthropicAdapter.testConnectivity 迁移

- [ ] **Step 2: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/proxy/gateway/protocol/AnthropicProtocolGateway.java
git commit -m "feat: 实现 AnthropicProtocolGateway"
```

---

## Task 4: 实现 ProtocolGatewayRegistryImpl + ProtocolController

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/proxy/gateway/protocol/ProtocolGatewayRegistryImpl.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProtocolController.java`

- [ ] **Step 1: 创建 ProtocolGatewayRegistryImpl**

```java
@Component
public class ProtocolGatewayRegistryImpl implements ProtocolGatewayRegistry {
    private final Map<String, ProtocolGateway> gateways;

    public ProtocolGatewayRegistryImpl(List<ProtocolGateway> gatewayList) {
        this.gateways = gatewayList.stream()
            .collect(Collectors.toMap(ProtocolGateway::getProtocolName, Function.identity()));
        // 启动时校验无重复
    }

    @Override
    public Optional<ProtocolGateway> getGateway(String protocolName) {
        return Optional.ofNullable(gateways.get(protocolName));
    }

    @Override
    public List<ProtocolGateway> getAllGateways() {
        return List.copyOf(gateways.values());
    }
}
```

- [ ] **Step 2: 创建 ProtocolController**

```java
@RestController
@RequestMapping("/api/protocols")
@RequiredArgsConstructor
public class ProtocolController {
    private final ProtocolGatewayRegistry registry;

    @GetMapping
    public List<Map<String, String>> listProtocols() {
        return registry.getAllGateways().stream()
            .map(gw -> Map.of("name", gw.getProtocolName(), "label", gw.getProtocolLabel()))
            .toList();
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/proxy/gateway/protocol/ProtocolGatewayRegistryImpl.java gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProtocolController.java
git commit -m "feat: 实现 ProtocolGatewayRegistry 和协议列表 API"
```

---

## Task 5: Provider.type 从 ProviderType 改为 String + 移除 baseUrl

**Files:**
- Modify: `domain/model/entity/Provider.java`
- Modify: `domain/model/gateway/database/dataobject/ProviderDo.java`
- Modify: `infrastructure/model/gateway/ProviderGatewayImpl.java`
- Delete: `domain/model/enums/ProviderType.java`

- [ ] **Step 1: 修改 Provider 实体**

移除 `import ProviderType`，`type` 字段从 `ProviderType` 改为 `String`，移除 `baseUrl` 字段。

- [ ] **Step 2: 修改 ProviderDo**

移除 `baseUrl` 字段，`type` 字段从 `@Enumerated(EnumType.STRING) ProviderType` 改为 `String`。

- [ ] **Step 3: 修改 ProviderGatewayImpl**

toEntity/toDo 中移除 baseUrl 转换，type 直接按 String 传递。

- [ ] **Step 4: 删除 ProviderType.java**

- [ ] **Step 5: 编译验证（预期失败，因为其他文件还引用 ProviderType）**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q 2>&1 | head -30`
Expected: 编译错误，列出所有还引用 ProviderType 的文件

- [ ] **Step 6: 提交**

```bash
git add -A && git commit -m "refactor: Provider.type 改为 String，移除 baseUrl，删除 ProviderType 枚举"
```

---

## Task 6: 修复所有 ProviderType 编译错误

**Files:**
- Modify: `domain/proxy/entity/RoutingContext.java`
- Modify: `domain/proxy/gateway/LLMGateway.java`
- Modify: `domain/proxy/gateway/LLMGatewayRegistry.java`
- Modify: `domain/model/entity/ProviderCapabilities.java`
- Modify: `infrastructure/proxy/gateway/rpc/LLMAdapter.java`
- Modify: `infrastructure/proxy/gateway/rpc/OpenAIAdapter.java`
- Modify: `infrastructure/proxy/gateway/rpc/AnthropicAdapter.java`
- Modify: `infrastructure/proxy/gateway/rpc/AdapterBuilderFactory.java`
- Modify: `infrastructure/proxy/gateway/rpc/VolcengineAdapter.java`（如存在）
- Modify: `application/proxy/ProxyServiceImpl.java`
- Modify: `application/provider/ProviderServiceImpl.java`
- Modify: `application/provider/dto/ProviderCreateRequest.java`
- Modify: `application/provider/dto/ProviderUpdateRequest.java`
- Modify: `application/provider/dto/ProviderResponse.java`
- Modify: `application/provider/dto/ProviderQueryRequest.java`
- Modify: `application/provider/dto/ConnectivityTestRequest.java`
- Modify: `adapter/api/ProviderController.java`
- Modify: `application/experience/ModelExperienceService.java`
- Modify: `application/experience/dto/ExperienceChatRequest.java`
- Modify: `application/metadata/ProviderMetadataService.java`
- Modify: `infrastructure/config/LLMAdapterConfig.java`
- Modify: `infrastructure/model/gateway/ConnectivityTesterImpl.java`
- Modify: `infrastructure/actuator/ProviderHealthTracker.java`

- [ ] **Step 1: 逐个修复编译错误**

每个文件中：
- `ProviderType` → `String`
- `getProviderType()` 返回 `String`
- `getGateway(ProviderType)` → `getGateway(String)`
- ProviderController.getProviderTypes() 改为返回所有供应商名称列表（从 ProviderGateway.findAll() 获取）
- AdapterBuilderFactory.createAdapter 签名改为 `(String providerName, String baseUrl, String apiKey, int timeoutSeconds)`
- ExperienceChatRequest 中 `ProviderType providerType` → `String providerType`
- ConnectivityTestRequest 中 `ProviderType providerType` → `String providerType`

- [ ] **Step 2: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "refactor: 修复所有 ProviderType 引用，改为 String"
```

---

## Task 7: 修改 Provider DTO 移除 baseUrl

**Files:**
- Modify: `application/provider/dto/ProviderCreateRequest.java`
- Modify: `application/provider/dto/ProviderUpdateRequest.java`
- Modify: `application/provider/dto/ProviderResponse.java`
- Modify: `application/provider/ProviderServiceImpl.java`

- [ ] **Step 1: 移除 DTO 中的 baseUrl 字段**

ProviderCreateRequest、ProviderUpdateRequest、ProviderResponse 中移除 `baseUrl` 字段。
ProviderServiceImpl 中移除所有 `setBaseUrl`/`getBaseUrl` 调用。

- [ ] **Step 2: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "refactor: Provider DTO 移除 baseUrl"
```

---

## Task 8: 修改路由层使用 ProtocolGateway

**Files:**
- Modify: `application/proxy/ProductRoutingService.java`
- Modify: `application/proxy/ChannelRoutingService.java`
- Modify: `application/proxy/ProxyServiceImpl.java`
- Modify: `infrastructure/proxy/gateway/rpc/AdapterBuilderFactory.java`

- [ ] **Step 1: 修改 ProductRoutingService**

- 注入 ProtocolGatewayRegistry
- `resolveEndpoint` 改为：按 protocolName 从 Product.endpoints 获取 URL，再通过 ProtocolGatewayRegistry 获取 ProtocolGateway
- 不再需要查询 Provider 获取 providerType（路由不依赖供应商类型）
- RoutingContext 构建时 `providerType` 使用 `provider.getName()`（品牌名称）

- [ ] **Step 2: 修改 ChannelRoutingService（旧架构兼容）**

- `buildLegacyContext` 和 `resolveLegacyFallback` 中 `provider.getBaseUrl()` 改为从产品的 endpoints 获取
- 旧架构降级：如果产品无端点配置，尝试从 ProviderDo 的 baseUrl 字段读取（过渡期兼容）

- [ ] **Step 3: 修改 ProxyServiceImpl**

- 注入 ProtocolGatewayRegistry
- `doProxy` 和 `doProxyStream` 中使用 ProtocolGateway 替代 AdapterBuilderFactory
- 从 RoutingContext 获取 protocolName，查找 ProtocolGateway，调用 chat/chatStream

- [ ] **Step 4: 修改 AdapterBuilderFactory**

- `createAdapter` 改为接受 `String protocolName` 参数
- 内部通过 ProtocolGatewayRegistry 查找 ProtocolGateway
- 保留原有签名作为过渡兼容（内部委托给新逻辑）

- [ ] **Step 5: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add -A && git commit -m "refactor: 路由层使用 ProtocolGateway 替代 ProviderType"
```

---

## Task 9: 修改 DataInitializer 和数据库迁移

**Files:**
- Modify: `infrastructure/init/DataInitializer.java`
- Create: `db/migration/V17__protocol_gateway_and_provider_simplify.sql`

- [ ] **Step 1: 修改 DataInitializer**

- 移除所有 `provider.setBaseUrl()` 调用
- 改为在创建默认产品时，将 baseUrl 写入产品的 endpoints：
  ```java
  product.setEndpoints(Map.of("openai", "https://api.openai.com/v1"));
  ```

- [ ] **Step 2: 创建数据库迁移 V17**

```sql
-- V17: 协议网关解耦 + 供应商简化

-- 1. 迁移 providers.base_url 到 products.endpoints
-- 为每个有 base_url 的供应商，更新其默认产品的 endpoints
UPDATE products p
SET endpoints = (
    SELECT jsonb_build_object('openai', prv.base_url)
    FROM providers prv
    WHERE prv.id = p.provider_id
      AND prv.base_url IS NOT NULL
      AND prv.base_url != ''
)
WHERE EXISTS (
    SELECT 1 FROM providers prv
    WHERE prv.id = p.provider_id
      AND prv.base_url IS NOT NULL
      AND prv.base_url != ''
);

-- 2. providers 表移除 base_url 列
ALTER TABLE providers DROP COLUMN IF EXISTS base_url;

-- 3. providers.type 列已经是 VARCHAR，无需 DDL 变更
-- 数据迁移：将枚举名称保持不变（OPENAI, ANTHROPIC 等作为品牌标识字符串）
```

- [ ] **Step 3: 编译验证**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "refactor: DataInitializer 和数据库迁移 V17"
```

---

## Task 10: 前端适配 — 协议列表动态获取

**Files:**
- Modify: `gateway-console/src/types/product.ts`
- Modify: `gateway-console/src/services/api/index.ts`
- Modify: `gateway-console/src/services/api/provider.ts`（或新建 protocol.ts）
- Modify: `gateway-console/src/pages/Providers/ProductFormModal.tsx`

- [ ] **Step 1: 新增协议类型和 API**

在 `types/product.ts` 中新增：
```typescript
/** 协议选项（从后端动态获取） */
export interface ProtocolOption {
  name: string;   // "openai", "anthropic"
  label: string;  // "OpenAI Chat Completions 协议"
}
```

在 `services/api/` 中新增获取协议列表的 API：
```typescript
export const fetchProtocols = () =>
  client.get<ProtocolOption[]>('/api/protocols');
```

- [ ] **Step 2: 修改 ProductFormModal**

端点 key 从自由 Input 改为 Select 下拉：
- 组件挂载时调用 `fetchProtocols()` 获取可选协议列表
- 每个端点行的 key 字段改为 Select，选项为协议列表
- 用户可添加多个端点（不同协议）

- [ ] **Step 3: 前端编译验证**

Run: `cd /mnt/e/workspace/llm-gateway/gateway-console && pnpm build`
Expected: 构建成功

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "feat(console): 端点协议类型动态下拉"
```

---

## Task 11: 前端适配 — Provider 表单移除 baseUrl

**Files:**
- Modify: `gateway-console/src/types/provider.ts`
- Modify: `gateway-console/src/pages/Providers/ProviderCreateModal.tsx`（或 BasicInfoStep.tsx）
- Modify: `gateway-console/src/pages/Providers/ProviderManagementDrawer.tsx`

- [ ] **Step 1: 移除 Provider 类型中的 baseUrl**

- [ ] **Step 2: 移除 Provider 表单中的 baseUrl 输入框**

- [ ] **Step 3: ProviderType 下拉改为自由文本输入（保留预定义列表作为建议）**

- [ ] **Step 4: 前端编译验证**

Run: `cd /mnt/e/workspace/llm-gateway/gateway-console && pnpm build`
Expected: 构建成功

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "refactor(console): Provider 表单移除 baseUrl，类型改为自由文本"
```

---

## Task 12: 测试更新和清理

**Files:**
- Modify: 所有 `gateway-boot/src/test/` 下引用 ProviderType 的测试
- Modify: 所有引用 Provider.getBaseUrl() 的测试
- 新增: `OpenAIProtocolGatewayTest.java`
- 新增: `AnthropicProtocolGatewayTest.java`
- 新增: `ProtocolGatewayRegistryImplTest.java`

- [ ] **Step 1: 修复所有测试中的 ProviderType 引用**

- [ ] **Step 2: 修复所有测试中的 Provider.getBaseUrl() 引用**

- [ ] **Step 3: 新增 ProtocolGateway 测试**

- [ ] **Step 4: 运行全部测试**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw test -pl gateway-boot`
Expected: 全部通过

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "test: 更新测试适配协议网关重构"
```

---

## Task 13: 最终验证和清理

- [ ] **Step 1: 后端完整编译**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw clean install -pl gateway-boot -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: 后端测试**

Run: `cd /mnt/e/workspace/llm-gateway && ./mvnw test -pl gateway-boot`
Expected: 全部通过

- [ ] **Step 3: 前端完整构建**

Run: `cd /mnt/e/workspace/llm-gateway/gateway-console && pnpm build`
Expected: 构建成功

- [ ] **Step 4: 检查无残留 ProviderType 引用**

Run: `grep -r "ProviderType" --include="*.java" gateway-boot/src/main/java/`
Expected: 无输出

- [ ] **Step 5: 检查无残留 baseUrl 引用**

Run: `grep -r "getBaseUrl\|setBaseUrl" --include="*.java" gateway-boot/src/main/java/`
Expected: 无输出（或仅在过渡兼容代码中）

- [ ] **Step 6: 最终提交**

```bash
git add -A && git commit -m "refactor: 端点协议与供应商解耦重构完成"
```
