# Interface Contract: LLMProviderAdapter

## Overview

`LLMProviderAdapter` 是适配器层的核心接口，定义了与模型提供商交互的标准方法。所有 Provider 适配器（如 OpenAIAdapter、AnthropicAdapter）必须实现此接口。

---

## Interface Definition

```java
package com.codingas.gateway.adapter;

/**
 * LLM Provider 适配器接口
 * 所有模型提供商适配器必须实现此接口
 *
 * <p>设计原则：对扩展开放、对修改关闭
 * 新增 Provider 只需实现此接口，无需修改框架代码
 */
public interface LLMProviderAdapter {

    /**
     * 聊天补全（OpenAI 格式）
     *
     * @param request ChatCompletionRequest
     * @return ChatCompletionResult
     * @throws ProviderException 当调用失败时
     */
    ChatCompletionResult chatCompletion(ChatCompletionRequest request);

    /**
     * 消息 API（Anthropic 格式）
     *
     * @param request MessagesRequest
     * @return MessagesResult
     * @throws ProviderException 当调用失败时
     */
    MessagesResult messages(MessagesRequest request);

    /**
     * 向量嵌入
     *
     * @param request EmbeddingRequest
     * @return EmbeddingResult
     * @throws ProviderException 当调用失败时
     */
    EmbeddingResult embeddings(EmbeddingRequest request);

    /**
     * 获取 Provider 能力描述
     *
     * @return ProviderCapabilities
     */
    ProviderCapabilities getCapabilities();

    /**
     * 健康检查
     *
     * @return true 表示 Provider 可用
     */
    boolean isHealthy();

    /**
     * 获取适配器对应的 Provider 类型
     *
     * @return ProviderType
     */
    ProviderType getProviderType();
}
```

---

## Request/Response Objects

### ChatCompletionRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| model | String | 是 | 模型 ID（如 gpt-4o） |
| messages | List<Message> | 是 | 消息列表 |
| temperature | Double | 否 | 采样温度（0-2） |
| maxTokens | Integer | 否 | 最大生成 token 数 |
| topP | Double | 否 | 核采样概率 |
| stream | Boolean | 否 | 是否流式响应（默认 false） |
| stop | List<String> | 否 | 停止词列表 |
| extraParams | Map | 否 | 额外参数 |

### ChatCompletionResult

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 请求 ID |
| object | String | 对象类型（如 chat.completion） |
| created | long | 创建时间戳 |
| model | String | 实际使用的模型 |
| choices | List<Choice> | 选项列表 |
| usage | Usage | Token 使用量统计 |

### MessagesRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| model | String | 是 | 模型 ID（如 claude-sonnet-4-20250514） |
| messages | List<Message> | 是 | 消息列表 |
| maxTokens | Integer | 是 | 最大生成 token 数 |
| temperature | Double | 否 | 采样温度 |
| systemPrompt | String | 否 | 系统提示 |

### MessagesResult

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 请求 ID |
| type | String | 响应类型 |
| role | String | 角色 |
| content | ContentBlock[] | 内容块 |
| stopReason | String | 停止原因 |
| usage | Usage | Token 使用量统计 |

### EmbeddingRequest

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| model | String | 是 | Embedding 模型 ID |
| input | String 或 List<String> | 是 | 待嵌入文本 |
| encodingFormat | String | 否 | 编码格式（如 float） |

### EmbeddingResult

| 字段 | 类型 | 说明 |
|------|------|------|
| object | String | 对象类型 |
| data | List<Embedding> | 嵌入向量列表 |
| usage | Usage | Token 使用量统计 |

---

## ProviderCapabilities

```java
public record ProviderCapabilities(
    String providerType,           // OPENAI / ANTHROPIC / GEMINI / OTHER
    boolean supportsChatCompletion, // OpenAI 格式
    boolean supportsMessages,       // Anthropic 格式
    boolean supportsEmbeddings,     // 向量嵌入
    boolean supportsStreaming,      // 流式响应
    boolean supportsFunctionCalling,// 函数调用
    Set<String> supportedModels,    // 支持的模型列表
    Map<String, String> extraInfo   // 额外能力信息
) {}
```

---

## Error Handling

### ProviderException

```java
public class ProviderException extends GatewayException {
    private final String providerCode;    // 出错的 Provider
    private final String modelId;         // 出错的模型
    private final ProviderErrorType errorType;  // 错误类型
    private final boolean retryable;      // 是否可重试
}

public enum ProviderErrorType {
    AUTHENTICATION_ERROR,   // 认证失败
    RATE_LIMIT_ERROR,       // 限流
    QUOTA_EXCEEDED,         // 配额用尽
    TIMEOUT_ERROR,          // 超时
    INVALID_REQUEST,        // 请求格式错误
    UPSTREAM_ERROR,         // 上游 Provider 错误
    NETWORK_ERROR,          // 网络错误
    UNKNOWN_ERROR           // 未知错误
}
```

**错误响应规范**:
- 所有 ProviderException 必须包含 `traceId` 用于追踪
- 错误消息不得包含敏感信息（API Key、token 等）
- `retryable = true` 时路由层可以尝试其他 Channel/Key

---

## SPI Registration

适配器通过 Java SPI 自动发现。实现类在以下文件注册：

**文件**: `META-INF/services/com.codingas.gateway.adapter.LLMProviderAdapter`

**内容示例**:
```
com.codingas.gateway.adapter.OpenAIAdapter
com.codingas.gateway.adapter.AnthropicAdapter
# 可按需添加更多适配器
```

**加载机制**:
```java
ServiceLoader<LLMProviderAdapter> loader = ServiceLoader.load(LLMProviderAdapter.class);
for (LLMProviderAdapter adapter : loader) {
    // 注册到 AdapterRegistry
}
```

---

## Implementation Requirements

### 线程安全
- 适配器实现必须是**无状态**的，支持并发调用
- 共享状态（如 Key 轮换计数器）必须使用线程安全数据结构

### 超时处理
- 所有 Provider 调用必须设置超时（可配置，默认 30s）
- 超时时应抛出 `ProviderException(errorType = TIMEOUT_ERROR, retryable = true)`

### 重试策略
- 对于 `retryable = true` 的错误，框架层自动重试
- 不建议在适配器内部实现重试，交给路由层统一处理

### 资源清理
- HTTP 连接必须正确释放（使用 try-with-resources 或 finally）
- 不得泄露连接资源

---

## Compliance Check

- [ ] 接口方法覆盖 OpenAI 和 Anthropic 两种协议
- [ ] 所有方法都有明确的错误类型和 retryable 标志
- [ ] SPI 机制正确配置
- [ ] 超时和资源清理策略明确