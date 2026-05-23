# 协议 DTO 重构设计：入站出站共用 + domain 层下沉

## 1. 问题陈述

当前 LLM 调用流程存在三层重复的请求/响应模型，职责模糊：

| 层 | 类 | 问题 |
|---|---|---|
| application | `LLMRequest`/`LLMResponse` + `OpenAIChatRequest` 等 + `chat/ChatRequest` 等 | 三套请求模型并存，转换逻辑散落 |
| domain | `LLMRequestVO`/`LLMResponseVO` + `ChatResponseVO` | VO 与 DTO 1:1 映射，纯冗余 |
| infrastructure | `buildRequestBody()` 手拼 Map / `parseResponse()` 手拆 Map | 第三次重新表达同一份协议知识 |

核心矛盾：

- **domain 层反向依赖 application 层**：`ProtocolGateway` import `LLMRequest`（application DTO），违反分层原则
- **10 个协议相关类散落三层**，存在大量冗余映射（`LLMDtoConverter` 做 DTO↔VO 转换）
- **协议知识被分散表达三次**：application DTO → domain VO → infrastructure Map 手拼/手拆
- **协议校验散落在 Controller**：OpenAI/Anthropic 的约束规则（如 Anthropic 的 max_tokens 必填）硬编码在 adapter 层

## 2. 核心原则

**协议是网关的一等公民，不是需要"统一消除"的差异。**

llm-gateway 的核心卖点 = 兼容 OpenAI 协议 + 兼容 Anthropic 协议 + 协议转换。协议格式是网关的业务语言，应该在 domain 层可见。

**入站出站共用同一套协议 DTO。**

入站和出站对同一协议的请求/响应结构几乎一致，差异仅是少数字段覆盖（model 替换、stream 标记）。共用 DTO 消灭中间模型，Jackson `@JsonProperty` 双向复用。

**协议校验是业务规则，属于 domain 层。**

OpenAI 的 model/messages 必填、Anthropic 的 max_tokens 必填且 > 0 —— 这些是协议规范约束，不是"用户输入校验"。

## 3. 设计方案

### 3.1 协议 DTO 沉入 domain 层

**位置**：`domain/proxy/protocol/`

```
domain/proxy/protocol/
├─ OpenAIChatRequest          implements ProtocolRequest
├─ OpenAIChatResponse         implements ProtocolResponse
├─ AnthropicMessagesRequest   implements ProtocolRequest
├─ AnthropicMessagesResponse  implements ProtocolResponse
├─ ProtocolRequest (接口)
└─ ProtocolResponse (接口)
```

从 `application/proxy/dto/` 移到 `domain/proxy/protocol/`，带上 `@JsonProperty` 注解。不带校验注解，纯数据结构 + Getter/Setter。

**ProtocolRequest 接口**：

```java
public interface ProtocolRequest {
    String getModel();
    void setModel(String model);
    String getProtocol();     // "openai" / "anthropic"
    boolean isStream();
    void setStream(boolean stream);
}
```

**ProtocolResponse 接口**：

```java
public interface ProtocolResponse {
    String getModel();
    String getFinishReason();
}
```

各协议 Response 自行定义内部 Usage 类（OpenAI 有 prompt_tokens/completion_tokens/total_tokens，Anthropic 有 input_tokens/output_tokens），不通过接口暴露。

### 3.2 协议校验 — Validator + 专用异常

**位置**：`domain/proxy/protocol/`

```
domain/proxy/protocol/
├─ ... (DTO)
├─ OpenAIProtocolValidator
├─ AnthropicProtocolValidator
└─ (异常) domain/proxy/exception/ProtocolValidationException
```

**校验规则**：

| 协议 | 规则 |
|------|------|
| OpenAI | model 非空、messages 非空、messages[].role 合法值 |
| Anthropic | model 非空、messages 非空、max_tokens 必填且 > 0 |

**Validator 接口**：

```java
public interface ProtocolValidator<T extends ProtocolRequest> {
    String getProtocol();
    void validate(T request);
}
```

**只做入站校验，出站不校验**。出站请求的正确性由 ProtocolConverter 保证。

**ProtocolValidationException**：

```java
public class ProtocolValidationException extends GatewayException {
    private final String protocol;
    private final String field;
    private final String violation;
}
```

由 adapter 层 catch 后转换为对应协议格式的错误响应。

### 3.3 协议转换 — ProtocolConverter

**位置**：`domain/proxy/protocol/`

```java
public class ProtocolConverter {

    // 非流式请求转换
    AnthropicMessagesRequest toAnthropic(OpenAIChatRequest request);
    OpenAIChatRequest toOpenAI(AnthropicMessagesRequest request);

    // 非流式响应转换
    AnthropicMessagesResponse toAnthropic(OpenAIChatResponse response);
    OpenAIChatResponse toOpenAI(AnthropicMessagesResponse response);

    // 流式 chunk 转换
    String convertStreamChunk(String rawChunk, String fromProtocol, String toProtocol);

    // 流式结束标记转换
    String convertStreamDone(String fromProtocol, String toProtocol);
}
```

**请求转换规则**：

| 方向 | 规则 |
|------|------|
| OpenAI→Anthropic | system 角色消息提取到顶层 `system` 字段；max_tokens 缺省补 1024；tool_calls → tool_use content block |
| Anthropic→OpenAI | 顶层 system 字段合并为 system 角色消息；content blocks 拼接为 string；tool_use → tool_calls |

**响应转换规则**：

| 方向 | 规则 |
|------|------|
| OpenAI→Anthropic | choices[0] → content blocks；finish_reason → stop_reason；usage 字段名映射 |
| Anthropic→OpenAI | content blocks → choices[0].message；stop_reason → finish_reason；补 total_tokens |

**流式 chunk 转换规则**：

| 方向 | 规则 |
|------|------|
| OpenAI→Anthropic chunk | delta.content → content_block_delta.delta.text；delta.tool_calls → content_block_delta.delta.partial_json；包装为 Anthropic 事件类型 |
| Anthropic→OpenAI chunk | content_block_delta → choices[0].delta；message_delta.stop_reason → choices[0].finish_reason；多事件类型映射为统一 chunk 格式 |
| 结束标记 | OpenAI [DONE] ↔ Anthropic 无标记（最后一个 message_delta 带 stop_reason），需互相适配 |

### 3.4 Gateway 接口瘦身 + 实例化注入

**ProtocolGatewayFactory**（domain 层接口）：

```java
public interface ProtocolGatewayFactory {
    ProtocolGateway create(String protocol, String baseUrl, String apiKey, int timeoutSeconds);
    List<String> getSupportedProtocols();
}
```

每个 Provider 对应一个 ProtocolGateway 实例（Prototype scope），创建时注入 baseUrl/apiKey/timeout。调用方从 Factory 获取实例后直接调用。

**ProtocolGateway**（domain 层接口）：

```java
public interface ProtocolGateway {
    ProtocolResponse chat(ProtocolRequest request);
    void chatStream(ProtocolRequest request, StreamCallback callback);
    ConnectivityTestResult testConnectivity();
}
```

实例已绑定 Provider 配置，方法签名不再需要 baseUrl/apiKey/timeout 参数。`testConnectivity()` 无参数，测试已绑定 Provider 的连通性。

**ProtocolMetadata 不需要**——协议元数据（name、label、defaultBaseUrl、apiKeyPrefix）要么是常量，要么是前端 UI 逻辑。`getSupportedProtocols()` 返回协议名列表即可，前端按需渲染。ProtocolType 枚举也不需要，避免影响协议扩展。

**ProtocolGatewayRegistry 删除**——由 ProtocolGatewayFactory 替代。

### 3.5 Infrastructure 层适配

**ProtocolGatewayFactoryImpl**：

```java
@Component
public class ProtocolGatewayFactoryImpl implements ProtocolGatewayFactory {

    private final OkHttpClient httpClient;

    @Override
    public ProtocolGateway create(String protocol, String baseUrl, String apiKey, int timeoutSeconds) {
        return switch (protocol) {
            case "openai" -> new OpenAIProtocolGateway(httpClient, baseUrl, apiKey, timeoutSeconds);
            case "anthropic" -> new AnthropicProtocolGateway(httpClient, baseUrl, apiKey, timeoutSeconds);
            default -> throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        };
    }

    @Override
    public List<String> getSupportedProtocols() {
        return List.of("openai", "anthropic");
    }
}
```

**OpenAIProtocolGateway / AnthropicProtocolGateway**：

- 构造函数接收 baseUrl、apiKey、timeoutSeconds（绑定 Provider 配置）
- `chat()`：内部强转 ProtocolRequest 为对应协议 DTO → Jackson 直接序列化 → HTTP 发送 → Jackson 反序列化为对应协议 Response DTO
- 不再有 Map<String, Object> 手拼/手拆
- `buildRequestBody()` 和 `parseResponse()` 用 Jackson 替代手动 Map 操作

### 3.6 调用流程

**非流式 — OpenAI 入站 → Anthropic 出站（跨协议）**：

```
OpenAIController
  ├─ @RequestBody OpenAIChatRequest
  ├─ OpenAIProtocolValidator.validate(request)     ← 入站校验
  ├─ ProxyApplicationService.proxy(request, authResult)
  │   ├─ 认证 → 鍒權 → 限流
  │   ├─ 路由决策 → RoutingContext(protocol=anthropic, baseUrl, apiKey, timeout)
  │   ├─ ProtocolConverter.toAnthropic(request)
  │   ├─ ProtocolGatewayFactory.create("anthropic", baseUrl, apiKey, timeout)
  │   ├─ gateway.chat(convertedRequest) → AnthropicMessagesResponse
  │   ├─ ProtocolConverter.toOpenAI(response) → OpenAIChatResponse
  │   ├─ 计费/审计（从 ProtocolResponse 按协议类型提取 usage）
  └─ ResponseEntity.ok(openAIChatResponse)
```

**流式 — Anthropic 入站 → OpenAI 出站（跨协议）**：

```
AnthropicController
  ├─ @RequestBody AnthropicMessagesRequest
  ├─ AnthropicProtocolValidator.validate(request)
  ├─ ProxyApplicationService.proxyStream(request, authResult, callback)
  │   ├─ 路由决策 → RoutingContext(protocol=openai, baseUrl, apiKey, timeout)
  │   ├─ ProtocolConverter.toOpenAI(request)
  │   ├─ ProtocolGatewayFactory.create("openai", baseUrl, apiKey, timeout)
  │   ├─ 包装 StreamCallback:
  │   │   onChunk(data) → ProtocolConverter.convertStreamChunk(data, "openai", "anthropic")
  │   │                    → 转换后的 chunk → 原始 callback.onChunk()
  │   ├─ gateway.chatStream(convertedRequest, wrappedCallback)
  └─ SSE Emitter
```

**同协议透传（最常见场景）**：

```
OpenAIController → validate → route(protocol=openai)
  → 不转换 → factory.create → gateway.chat(request) → OpenAIChatResponse
  → 计费 → 直接返回
```

零转换开销，请求直接透传。

### 3.7 Experience 流程适配

`ModelExperienceService` 删除 `LLMRequest` 手动构建，改为构建对应的协议 DTO（始终同协议透传，不转换）：

```
ExperienceController → ModelExperienceService
  → 构建协议 DTO（OpenAIChatRequest 或 AnthropicMessagesRequest）
  → ProtocolValidator.validate(request)
  → ProtocolGatewayFactory.create(protocol, baseUrl, apiKey, 60)
  → gateway.chatStream(request, wrappedCallback)
```

## 4. 文件变更清单

### 新增（7）

| 文件 | 位置 | 职责 |
|------|------|------|
| `ProtocolRequest` | `domain/proxy/protocol/` | 协议请求接口 |
| `ProtocolResponse` | `domain/proxy/protocol/` | 协议响应接口 |
| `OpenAIProtocolValidator` | `domain/proxy/protocol/` | OpenAI 入站校验 |
| `AnthropicProtocolValidator` | `domain/proxy/protocol/` | Anthropic 入站校验 |
| `ProtocolValidationException` | `domain/proxy/exception/` | 协议校验异常 |
| `ProtocolConverter` | `domain/proxy/protocol/` | 跨协议转换（请求/响应/流式chunk） |
| `ProtocolGatewayFactory` | `domain/proxy/gateway/` | Gateway 工厂接口 |

### 移动（4）

| 文件 | 从 → 到 |
|------|---------|
| `OpenAIChatRequest` | `application/proxy/dto/` → `domain/proxy/protocol/`，实现 ProtocolRequest |
| `OpenAIChatResponse` | `application/proxy/dto/` → `domain/proxy/protocol/`，实现 ProtocolResponse |
| `AnthropicMessagesRequest` | `application/proxy/dto/` → `domain/proxy/protocol/`，实现 ProtocolRequest |
| `AnthropicMessagesResponse` | `application/proxy/dto/` → `domain/proxy/protocol/`，实现 ProtocolResponse |

### 修改（10）

| 文件 | 变更 |
|------|------|
| `ProtocolGateway` | 签名改为 `chat(ProtocolRequest)` / `chatStream(ProtocolRequest, StreamCallback)` / `testConnectivity()` |
| `OpenAIProtocolGateway` | 实现新签名；Jackson 序列化/反序列化替代 Map；构造函数接收 baseUrl/apiKey/timeout |
| `AnthropicProtocolGateway` | 同上 |
| `ProtocolGatewayRegistryImpl` | 替换为 `ProtocolGatewayFactoryImpl` |
| `ProxyServiceImpl` | 删除 LLMRequest/LLMResponse 构建逻辑；改为 ProtocolConverter + ProtocolGatewayFactory |
| `OpenAIController` | import 路径改为 domain.proxy.protocol；增加入站校验调用 |
| `AnthropicController` | 同上 |
| `ModelExperienceService` | 删除 LLMRequest 手动构建，改为协议 DTO + ProtocolGatewayFactory |
| `RoutingContext` | 配合 Factory 使用 |
| `ProtocolController` | 从注入 ProtocolGatewayRegistry 改为注入 ProtocolGatewayFactory |

### 删除（9）

| 文件 | 原因 |
|------|------|
| `LLMRequest` | 被 ProtocolRequest + 协议 DTO 替代 |
| `LLMResponse` | 被 ProtocolResponse + 协议 DTO 替代 |
| `LLMRequestVO` | 协议 DTO 本身在 domain 层，不需要 VO |
| `LLMResponseVO` | 同上 |
| `ChatResponseVO` | 同上 |
| `ChatRequest` (chat 包) | 同上 |
| `ChatResponse` (chat 包) | 同上 |
| `LLMDtoConverter` | DTO↔VO 转换不再需要 |
| `ProtocolGatewayRegistry` (接口) | 被 ProtocolGatewayFactory 替代 |

**统计**：新增 7，移动 4，修改 10，删除 9。净减少 2 个文件，但消除 8 个冗余类。

## 5. 不在本次范围

- ProtocolType 枚举（影响协议扩展性）
- ProtocolMetadata 接口（过度抽象，元数据是常量/前端 UI 逻辑）
- 出站校验（由 ProtocolConverter 保证转换结果的正确性）

## 6. 成功标准

1. **分层合规**：domain 层不反向依赖 application 层（ProtocolGateway 只依赖 domain 层接口）
2. **冗余消除**：8 个冗余类全部删除，协议知识只存在一处
3. **四种路径全覆盖**：同协议透传 + 跨协议转换（非流式 + 流式）
4. **入站校验 domain 化**：协议校验规则在 domain 层可见
5. **零拷贝透传**：同协议场景无中间转换，Jackson 直接序列化协议 DTO
6. **编译通过 + 测试通过**：所有修改后的测试正常运行