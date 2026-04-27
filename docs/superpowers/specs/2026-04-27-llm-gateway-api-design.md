# LLM Gateway API 双兼容设计

## 背景

LLM Gateway 需要同时兼容 OpenAI 和 Anthropic 两种 API 格式，让开发者可以使用熟悉的 SDK 接入。

## 目标

| SDK | Base URL | Endpoint | 格式 |
|-----|----------|----------|------|
| OpenAI | `https://api.codingas.com/v1` | `/v1/chat/completions` | OpenAI |
| Anthropic | `https://api.codingas.com/anthropic` | `/v1/messages` | Anthropic |

## 客户端配置示例

```bash
# OpenAI SDK
export OPENAI_BASE_URL=https://api.codingas.com/v1
export OPENAI_API_KEY=xxx

# Anthropic SDK
export ANTHROPIC_BASE_URL=https://api.codingas.com/anthropic
export ANTHROPIC_API_KEY=xxx
```

## 架构设计

```
                         ┌─────────────────────────────────────┐
                         │            LLM Gateway               │
                         │                                     │
https://api.codingas.com/│                                     │
                         │  ┌─────────────────┐  ┌───────────┐ │
                         │  │ OpenAIController│  │Anthropic  │ │
                         │  │ @RequestMapping │  │Controller │ │
                         │  │   ("/v1")      │  │("/anthrop │ │
                         │  └────────┬────────┘  │  ic/v1")  │ │
                         │           │          └─────┬─────┘ │
                         │           │                │       │
                         │           └───────┬────────┘       │
                         │                   │                │
                         │          ┌───────▼────────┐      │
                         │          │  LLMChatUseCase │      │
                         │          │   (统一路由)     │      │
                         │          └───────┬────────┘      │
                         │                  │                │
                         └──────────────────┼────────────────┘
                                            │
                              ┌─────────────▼─────────────┐
                              │     Model Router          │
                              │  (Provider 自动选择)      │
                              └─────────────┬─────────────┘
                                            │
                    ┌───────────────────────┼───────────────────────┐
                    │                       │                       │
          ┌─────────▼─────────┐   ┌─────────▼─────────┐   ┌────────▼────────┐
          │  OpenAI Provider  │   │ Anthropic Provider │   │ Other Provider │
          └───────────────────┘   └───────────────────┘   └────────────────┘
```

## 实现改动

### 1. AnthropicController 路径调整

```java
// 修改前
@RequestMapping("/v1")
public class AnthropicController { ... }

// 修改后
@RequestMapping("/anthropic/v1")
public class AnthropicController { ... }
```

### 2. OpenAIController 保持不变

```java
@RequestMapping("/v1")
public class OpenAIController { ... }
```

## 影响范围

| 文件 | 改动 |
|------|------|
| `AnthropicController.java` | `@RequestMapping` 从 `/v1` 改为 `/anthropic/v1` |

## 内部统一

- 两个 Controller 共用同一个 `LLMChatUseCase`
- 路由、限流、鉴权、计费逻辑完全共用
- 只是请求/响应格式转换不同

## 测试验证

1. OpenAI SDK 调用 `POST /v1/chat/completions` 正常
2. Anthropic SDK 调用 `POST /anthropic/v1/messages` 正常
3. 两种方式的流式响应均正常
4. 内部路由、限流、计费行为一致
