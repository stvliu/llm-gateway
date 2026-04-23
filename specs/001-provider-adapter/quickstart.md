# Quickstart: Provider Adapter Framework

## 概述

本指南帮助开发者快速理解 Provider 适配器框架，并在 2 小时内实现一个新的 Provider 适配器。

---

## 前置条件

- JDK 21+
- Maven 3.8+
- 对 Java SPI 机制有基本了解

---

## 核心概念

### 1. LLMProviderAdapter 接口

所有适配器必须实现 `LLMProviderAdapter` 接口：

```java
public interface LLMProviderAdapter {
    Mono<LLMResponse> chat(LLMRequest request);
    Flux<LLMResponse> chatStream(LLMRequest request);
    Mono<LLMResponse> messages(LLMRequest request);
    ProviderCapabilities getCapabilities();
    ProviderType getProviderType();
}
```

### 2. SPI 自动发现

适配器通过 Java SPI 自动被发现和加载。只需：

1. 实现 `LLMProviderAdapter` 接口
2. 在 `META-INF/services/com.codingas.gateway.adapter.LLMProviderAdapter` 注册
3. 将 JAR 放入 classpath

### 3. 适配器注册中心

框架提供 `AdapterRegistry` 统一管理所有适配器：

```java
@Service
public class AdapterRegistry {
    public void register(LLMProviderAdapter adapter);
    public LLMProviderAdapter getAdapter(ProviderType type);
    public List<LLMProviderAdapter> getAllAdapters();
}
```

---

## 创建新适配器（Step by Step）

### Step 1: 创建适配器类

```java
package com.codingas.gateway.adapter;

public class MyProviderAdapter implements LLMProviderAdapter {

    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        // 调用 MyProvider API
        // 转换为标准 LLMResponse 返回
    }

    @Override
    public Mono<LLMResponse> messages(LLMRequest request) {
        throw new UnsupportedOperationException("MyProvider 不支持 Anthropic 格式");
    }

    @Override
    public ProviderCapabilities getCapabilities() {
        return new ProviderCapabilities(
            ProviderType.OTHER,
            true,   // supportsChatCompletion
            false,  // supportsMessages
            true,   // supportsEmbeddings
            false,  // supportsStreaming
            Set.of("my-model-v1", "my-model-v2")
        );
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.OTHER;
    }
}
```

### Step 2: 注册 SPI

创建文件：
```
gateway-adapter/src/main/resources/META-INF/services/com.codingas.gateway.adapter.LLMProviderAdapter
```

内容：
```
com.codingas.gateway.adapter.openai.OpenAIAdapter
com.codingas.gateway.adapter.anthropic.AnthropicAdapter
com.codingas.gateway.adapter.MyProviderAdapter
```

### Step 3: 配置 Provider

通过 Admin API 或数据库创建 Provider 记录：

```sql
INSERT INTO providers (provider_code, provider_name, provider_type, base_url, priority, status)
VALUES ('my_provider', 'My Provider', 'OTHER', 'https://api.myprovider.com', 100, 'ACTIVE');
```

### Step 4: 验证

启动应用，查看日志：

```
Adapter Loader: Found adapter com.codingas.gateway.adapter.MyProviderAdapter
Adapter Registry: Registered MyProviderAdapter for type OTHER
```

---

## 实体关系

```
Provider (全局)
  └── Model[] (全局)
  └── ProviderApiKey[] (系统维度，管理员配置)

User (用户)
  └── GatewayApiKey[] (用户维度，网关访问凭证)
  GatewayApiKey ──── Provider (关联)
```

---

## 测试适配器

### 单元测试

```java
@ExtendWith(MockitoExtension.class)
class MyProviderAdapterTest {

    @Mock
    private WebClient webClient;

    @InjectMocks
    private MyProviderAdapter adapter;

    @Test
    void shouldReturnCapabilities() {
        var caps = adapter.getCapabilities();
        assertTrue(caps.supportsChatCompletion());
        assertEquals(Set.of("my-model-v1"), caps.supportedModels());
    }

    @Test
    void shouldThrowProviderExceptionOnError() {
        when(webClient.post(any(), any())).thenThrow(new IOException("Network error"));

        assertThrows(ProviderException.class, () ->
            adapter.chat(new LLMRequest(...)).block()
        );
    }
}
```

### 集成测试

```java
@SpringBootTest
class AdapterLoaderIntegrationTest {

    @Autowired
    private AdapterRegistry registry;

    @Test
    void shouldLoadAllRegisteredAdapters() {
        var adapters = registry.getAllAdapters();
        assertTrue(adapters.size() >= 2);  // OpenAI + Anthropic
    }

    @Test
    void shouldGetAdapterByType() {
        var openai = registry.getAdapter(ProviderType.OPENAI);
        assertNotNull(openai);
        assertTrue(openai instanceof OpenAIAdapter);
    }
}
```

---

## 常见问题

### Q: 如何处理 Provider 返回的非标准格式？

A: 在适配器内部做格式转换，转换为标准 `LLMResponse`。如果 Provider 返回完全不同的结构，抛出 `ProviderException` 并记录日志。

### Q: Provider API Key 如何安全存储？

A: `ProviderApiKey.encrypted_api_key` 使用 AES-256 加密存储。加密/解密由 `EncryptionService` 提供。

### Q: 如何实现 Key 自动轮换？

A: 通过 ProviderApiKey 的 priority 字段实现按优先级选择 Key（数值越大越优先）。

---

## 下一步

- 查看 `data-model.md` 了解实体详情
- 查看 `contracts/adapter-interface.md` 了解接口契约
- 查看 `tasks.md` 了解完整实现进度
