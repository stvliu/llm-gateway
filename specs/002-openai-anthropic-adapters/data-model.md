# Data Model: OpenAI and Anthropic Dual Adapter

**Feature**: 实现OpenAI和Anthropic双适配器
**Date**: 2026-04-23

## Entity Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    gateway-adapter 模块                       │
├─────────────────────────────────────────────────────────────┤
│  LLMProviderAdapter (interface)                             │
│  ├── OpenAIAdapter (implements)                             │
│  └── AnthropicAdapter (implements)                         │
│                                                              │
│  DTOs:                                                       │
│  ├── LLMRequest                                              │
│  ├── LLMResponse                                             │
│  ├── ProviderCapabilities                                     │
│  └── ProviderException                                       │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    dispatch 层 (新增)                        │
├─────────────────────────────────────────────────────────────┤
│  ProtocolTranslator                                          │
│  ├── toAnthropicFormat(LLMRequest) → Anthropic-specific     │
│  └── toOpenAIFormat(LLMRequest) → OpenAI-specific          │
│                                                              │
│  ErrorResponseAdapter                                        │
│  ├── toOpenAIError(ProviderException) → OpenAI error format│
│  └── toAnthropicError(ProviderException) → Anthropic error  │
└─────────────────────────────────────────────────────────────┘
```

## Key Entities

### LLMProviderAdapter (Interface)

```java
public interface LLMProviderAdapter {
    String getProviderCode();
    ProviderType getProviderType();
    Mono<LLMResponse> chat(LLMRequest request);
    Flux<LLMResponse> chatStream(LLMRequest request);
    Mono<LLMResponse> messages(LLMRequest request);
    boolean isAvailable();
    boolean isHealthy();
    ProviderCapabilities getCapabilities();
    default int getDefaultTimeout() { return 30; }
}
```

### OpenAIAdapter (Existing, to be Enhanced)

| Field | Type | Description |
|-------|------|-------------|
| webClient | WebClient | HTTP client |
| baseUrl | String | API base URL |
| apiKey | String | API key (from credentials loader) |
| timeoutSeconds | int | Request timeout |

**Methods to enhance**:
- `buildRequestBody()` - Add Function Calling support
- `parseResponse()` - Handle function_call in response
- `parseStreamChunk()` - Handle streaming function calls

### AnthropicAdapter (Existing, to be Enhanced)

| Field | Type | Description |
|-------|------|-------------|
| webClient | WebClient | HTTP client |
| baseUrl | String | API base URL |
| apiKey | String | API key |
| version | String | API version |
| timeoutSeconds | int | Request timeout |

**Methods to enhance**:
- `buildMessagesRequestBody()` - Add Tool Use support
- `parseResponse()` - Handle tool_use in response
- `parseStreamChunk()` - Handle streaming tool use

### ProtocolTranslator (New)

```java
@Component
public class ProtocolTranslator {
    
    // OpenAI → Anthropic conversion
    public Map<String, Object> toAnthropicFormat(LLMRequest request);
    
    // Anthropic → OpenAI conversion  
    public LLMRequest toOpenAIFormat(Map<String, Object> anthropicRequest);
    
    // Response conversion
    public LLMResponse fromAnthropicResponse(Map<String, Object> anthropicResponse);
    public LLMResponse fromOpenAIResponse(Map<String, Object> openaiResponse);
}
```

### ErrorResponseAdapter (New)

```java
@Component
public class ErrorResponseAdapter {
    
    public Map<String, Object> toOpenAIError(ProviderException ex, String requestId);
    public Map<String, Object> toAnthropicError(ProviderException ex, String requestId);
}
```

## Existing DTOs

### LLMRequest (Existing)

| Field | Type | Description |
|-------|------|-------------|
| model | String | Model identifier |
| messages | List<Message> | Conversation messages |
| temperature | Double | Sampling temperature |
| maxTokens | Integer | Max response tokens |
| stream | boolean | Streaming mode |
| systemPrompt | String | System prompt (Anthropic) |
| tools | List<ToolDefinition> | Function definitions |
| toolChoice | String | Tool selection |

### LLMResponse (Existing)

| Field | Type | Description |
|-------|------|-------------|
| providerCode | String | Provider identifier |
| model | String | Model used |
| id | String | Response ID |
| content | Content | Message content |
| usage | Usage | Token usage |
| finishReason | String | Stop reason |
| stream | boolean | Streaming response |
| error | Error | Error details |
| extraData | Map | Additional data |

## Validation Rules

1. **LLMRequest**:
   - `model` must not be null or empty
   - `messages` must not be null or empty
   - `maxTokens` must be > 0 when specified

2. **ProviderCredentials**:
   - API key must be encrypted with AES-256
   - Base URL must be valid HTTPS endpoint

## State Transitions

### Adapter States

```
isAvailable() → true when apiKey is set
isHealthy() → true when isAvailable() + canConnect()
```

### Request Flow

```
Request → Validation → Protocol Translation → Adapter → Response Format Adaptation → Client
```
