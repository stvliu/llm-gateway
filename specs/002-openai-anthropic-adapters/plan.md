# Implementation Plan: OpenAI and Anthropic Dual Adapter

**Branch**: `002-openai-anthropic-adapters` | **Date**: 2026-04-23 | **Spec**: [spec.md](./spec.md)

## Summary

实现 OpenAI 和 Anthropic 双适配器，支持：
1. OpenAI `/v1/chat/completions` 和 Anthropic `/v1/messages` 双端点
2. 完整 Function Calling / Tool Use 支持
3. OpenAI ↔ Anthropic 双向协议转换
4. 根据请求来源适配错误格式
5. 80%+ 测试覆盖率

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.5.x, Spring MVC (spring-boot-starter-web), RestClient, OkHttp 4.12.0, Jackson
**Storage**: PostgreSQL 14+ (provider credentials, encrypted API keys)
**Testing**: JUnit 5, MockMvc + OkHttp MockWebServer, Testcontainers
**Target Platform**: Linux Server
**Project Type**: LLM Gateway Web Service
**Performance Goals**: 10,000 QPS, <200ms P50 latency, <500ms P95 non-streaming, <300ms P95 streaming first byte
**Scale/Scope**: Enterprise gateway supporting 100+ teams, 10,000 concurrent users

## Constitution Check

| Constitution Rule | Status | Notes |
|-----------------|--------|-------|
| 双 API 兼容 (原则一) | ✅ PASS | OpenAI + Anthropic 双端点实现 |
| 安全零信任 (原则二) | ✅ PASS | AES-256 加密存储 API Key |
| 测试驱动开发 (原则三) | ✅ PASS | 80%+ 覆盖率要求 |
| 可观测性内建 (原则四) | ✅ PASS | OpenTelemetry tracing support |
| Token 成本透明 (原则五) | ✅ PASS | Usage tracking in LLMResponse |
| 分层架构 | ✅ PASS | Adapter layer in infrastructure, protocol translation in dispatch |
| 模型纯洁性 | ✅ PASS | Entities only have getters/setters |
| 配置外部化 | ✅ PASS | @ConfigurationProperties |

## HTTP Client Architecture

### 非流式请求
- **HTTP Client**: `RestClient` (Spring 6.1, blocking)
- **用途**: 标准 chat/messages 请求

### 流式请求 (SSE)
- **HTTP Client**: `OkHttp` + `Call` + `Callback`
- **原因**: OkHttp 支持同步流式读取，`Response.body().source()` 可边读边输出
- **SSE 解析**: `okio.BufferedSource` 按行读取 SSE `data:` 事件

## Project Structure

### Documentation

```text
specs/002-openai-anthropic-adapters/
├── plan.md, research.md, data-model.md, quickstart.md
├── contracts/
│   ├── openai-contract.md
│   └── anthropic-contract.md
└── tasks.md (by /speckit.tasks)
```

### Source Code

```text
gateway-adapter/src/main/java/com/codingas/gateway/adapter/
├── LLMProviderAdapter.java           # Interface (update for Spring MVC)
├── openai/OpenAIAdapter.java       # (enhance)
├── anthropic/AnthropicAdapter.java  # (enhance)
├── common/                         # (exists)
└── dto/                           # (exists)

gateway-dispatch/src/main/java/com/codingas/gateway/dispatch/  (NEW)
├── ProtocolTranslator.java
└── ErrorResponseAdapter.java
```

## Adapter Interface

```java
public interface LLMProviderAdapter {
    String getProviderCode();
    ProviderType getProviderType();
    
    // 非流式 - RestClient (blocking)
    LLMResponse chat(LLMRequest request);
    LLMResponse messages(LLMRequest request);
    
    // 流式 - OkHttp
    void chatStream(LLMRequest request, StreamCallback callback);
    void messagesStream(LLMRequest request, StreamCallback callback);
    
    boolean isAvailable();
    boolean isHealthy();
    ProviderCapabilities getCapabilities();
}

public interface StreamCallback {
    void onChunk(String data);  // SSE data 行
    void onComplete();
    void onError(Throwable t);
}
```

## Implementation Phases

### Phase 1: Adapter Enhancement
1. OpenAIAdapter - RestClient + OkHttp streaming + Function Calling
2. AnthropicAdapter - RestClient + OkHttp streaming + Tool Use

### Phase 2: Protocol Translation
3. ProtocolTranslator - OpenAI ↔ Anthropic 双向转换
4. ErrorResponseAdapter - 错误格式适配

### Phase 3: Testing
5. Unit Tests - MockMvc + OkHttp MockWebServer
6. Integration Tests - Testcontainers

## Verification

- [ ] Adapter layer coverage ≥ 80%
- [ ] OpenAI API v1 compatibility 100%
- [ ] Anthropic Messages API compatibility 100%
- [ ] Protocol conversion accuracy ≥ 99.9%
- [ ] Error format adaptation works correctly
