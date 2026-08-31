# API 接口规范 / API Specification

> **文档版本**: v1.3
> **生成日期**: 2026-05-02（2026-08-31 对齐修订）
> **状态**: 草案

---

## 目录

- [一、概述](#一概述)
- [二、OpenAI API](#二openai-api)
- [三、Anthropic API](#三anthropic-api)
- [四、协议转换](#四协议转换)
- [五、错误响应](#五错误响应)
- [六、版本兼容性](#六版本兼容性)
- [七、管理面 API 总览](#七管理面-api-总览)

---

## 一、概述

### 1.1 文档目的

本文档定义 LLM-Gateway 网关对外暴露的 API 接口契约，包括：

- OpenAI 兼容 API 的端点、请求格式、响应格式
- Anthropic 兼容 API 的端点、请求格式、响应格式
- 两种协议之间的转换规则

### 1.2 兼容性声明

| 协议 | 兼容标准 | 说明 |
|------|---------|------|
| **OpenAI API** | OpenAI API Reference (2026-04) | 完全兼容 OpenAI 官方 SDK |
| **Anthropic API** | Anthropic Messages API (2026-04) | 完全兼容 Anthropic 官方 SDK |

### 1.3 基础约定

| 约定 | 说明 |
|------|------|
| **Base URL（OpenAI 兼容）** | `https://{gateway-host}/v1` |
| **Base URL（Anthropic 兼容）** | `https://{gateway-host}/anthropic/v1` |
| **认证方式** | Bearer Token (GatewayApiKey) |
| **内容类型** | `application/json` |
| **字符编码** | UTF-8 |
| **日期时间格式** | ISO 8601 (`2026-04-28T10:30:00Z`) |

---

## 二、OpenAI API

### 2.1 端点总览

| 端点 | 方法 | 说明 | 优先级 |
|------|------|------|--------|
| `/v1/chat/completions` | POST | 聊天补全（核心） | P0 |
| `/v1/completions` | POST | 文本补全（Legacy，规划中） | P1 |
| `/v1/embeddings` | POST | 向量嵌入（规划中） | P0 |
| `/v1/models` | GET | 模型列表 | P1 |
| `/v1/models/{model}` | GET | 模型详情（规划中） | P1 |
| `/v1/images/generations` | POST | 图像生成（规划中） | P1 |
| `/v1/images/edits` | POST | 图像编辑（规划中） | P2 |
| `/v1/images/variations` | POST | 图像变体（规划中） | P2 |
| `/v1/audio/speech` | POST | 文字转语音 (TTS)（规划中） | P1 |
| `/v1/audio/transcriptions` | POST | 语音转文字 (STT)（规划中） | P1 |
| `/v1/audio/translations` | POST | 语音翻译（规划中） | P2 |
| `/v1/moderations` | POST | 内容审核（规划中） | P1 |

---

### 2.2 Chat Completions API

#### 2.2.1 请求格式

**端点**: `POST /v1/chat/completions`

```json
{
  "model": "gpt-4o",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello!"},
    {"role": "assistant", "content": "Hi there!"},
    {"role": "user", "content": "How are you?"}
  ],
  "temperature": 0.7,
  "stream": false,
  "stop": ["STOP"],
  "max_tokens": 1024,
  "presence_penalty": 0.0,
  "frequency_penalty": 0.0,
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "Get current weather",
        "parameters": {
          "type": "object",
          "properties": {
            "location": {"type": "string"}
          },
          "required": ["location"]
        }
      }
    }
  ],
  "tool_choice": "auto",
  "response_format": {"type": "json_object"},
  "seed": 42
}
```

#### 2.2.2 请求参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `model` | string | ✅ | - | 模型 ID，如 `gpt-4o`、`gpt-4-turbo` |
| `messages` | array | ✅ | - | 对话消息列表 |
| `temperature` | number | ❌ | 1.0 | 采样温度 (0-2)，值越低越确定 |
| `stream` | boolean | ❌ | false | 是否流式返回 |
| `stop` | List<String> | ❌ | null | 停止生成的序列 |
| `max_tokens` | integer | ❌ | 模型上限 | 最大生成 Token 数 |
| `presence_penalty` | number | ❌ | 0 | 存在惩罚 (-2.0 到 2.0) |
| `frequency_penalty` | number | ❌ | 0 | 频率惩罚 (-2.0 到 2.0) |
| `tools` | array | ❌ | null | 工具定义列表 |
| `tool_choice` | String | ❌ | "auto" | 工具选择策略 |
| `response_format` | object | ❌ | null | 响应格式约束 |
| `seed` | integer | ❌ | null | 随机种子（确定性输出） |

#### 2.2.3 消息格式

**基础消息**:

```json
{"role": "system|user|assistant", "content": "消息内容"}
```

**多模态消息 (Vision)（规划中）**:

> **现状**: 当前请求 DTO（`OpenAIChatRequest.Message`）的 `content` 为纯字符串，`content` 数组与 `image_url` 图片输入尚未实现。

```json
{
  "role": "user",
  "content": [
    {"type": "text", "text": "这张图片是什么?"},
    {
      "type": "image_url",
      "image_url": {
        "url": "https://example.com/image.png",
        "detail": "auto"
      }
    }
  ]
}
```

**图片输入方式**:

| 方式 | 格式 | 说明 |
|------|------|------|
| URL | `"url": "https://..."` | 图片 URL |
| Base64 | `"url": "data:image/png;base64,..."` | Base64 编码 |

**Assistant 消息带 Tool Calls**:

```json
{
  "role": "assistant",
  "content": null,
  "tool_calls": [
    {
      "id": "call_abc123",
      "type": "function",
      "function": {
        "name": "get_weather",
        "arguments": "{\"location\": \"Beijing\"}"
      }
    }
  ]
}
```

**Tool 响应消息**:

```json
{
  "role": "tool",
  "tool_call_id": "call_abc123",
  "content": "{\"temperature\": 25, \"condition\": \"sunny\"}"
}
```

#### 2.2.4 响应格式

**非流式响应**:

```json
{
  "id": "chatcmpl-abc123",
  "object": "chat.completion",
  "created": 1713833628,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello! How can I help you today?",
        "tool_calls": null
      },
      "logprobs": null,
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 15,
    "completion_tokens": 10,
    "total_tokens": 25
  },
  "system_fingerprint": "fp_abc123"
}
```

**带 Tool Calls 的响应**:

```json
{
  "id": "chatcmpl-abc123",
  "object": "chat.completion",
  "created": 1713833628,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": null,
        "tool_calls": [
          {
            "id": "call_abc123",
            "type": "function",
            "function": {
              "name": "get_weather",
              "arguments": "{\"location\": \"Beijing\"}"
            }
          }
        ]
      },
      "finish_reason": "tool_calls"
    }
  ],
  "usage": {
    "prompt_tokens": 50,
    "completion_tokens": 20,
    "total_tokens": 70
  }
}
```

#### 2.2.5 流式 SSE 事件格式

**请求**: 设置 `"stream": true`

**SSE 事件流**:

```
data: {"id":"chatcmpl-abc123","object":"chat.completion.chunk","created":1713833628,"model":"gpt-4o","choices":[{"index":0,"delta":{"role":"assistant"},"finish_reason":null}]}

data: {"id":"chatcmpl-abc123","object":"chat.completion.chunk","created":1713833628,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

data: {"id":"chatcmpl-abc123","object":"chat.completion.chunk","created":1713833628,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":null}]}

data: {"id":"chatcmpl-abc123","object":"chat.completion.chunk","created":1713833628,"model":"gpt-4o","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

**Delta 字段说明**:

| 字段 | 说明 |
|------|------|
| `role` | 仅首个 chunk 包含 |
| `content` | 增量文本内容 |
| `tool_calls` | 工具调用增量 |

#### 2.2.6 finish_reason 枚举

| 值 | 说明 |
|------|------|
| `stop` | 正常结束 |
| `length` | 达到 max_tokens 限制 |
| `tool_calls` | 模型调用了工具 |
| `content_filter` | 内容被过滤 |
| `function_call` | (Deprecated) 函数调用 |

---

### 2.3 Embeddings API

#### 2.3.1 请求格式

**端点**: `POST /v1/embeddings`

```json
{
  "model": "text-embedding-3-small",
  "input": "The food was delicious and the waiter was friendly.",
  "encoding_format": "float",
  "dimensions": 1536,
  "user": "user-123"
}
```

#### 2.3.2 请求参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `model` | string | ✅ | - | 嵌入模型 ID |
| `input` | string/array | ✅ | - | 输入文本（最多 2048 个） |
| `encoding_format` | string | ❌ | "float" | 编码格式：`float` / `base64` |
| `dimensions` | integer | ❌ | 模型默认 | 输出维度 |
| `user` | string | ❌ | null | 终端用户标识 |

#### 2.3.3 响应格式

```json
{
  "object": "list",
  "data": [
    {
      "object": "embedding",
      "index": 0,
      "embedding": [-0.0069, -0.0044, 0.0152, ...]
    }
  ],
  "model": "text-embedding-3-small",
  "usage": {
    "prompt_tokens": 10,
    "total_tokens": 10
  }
}
```

---

### 2.4 Images API

#### 2.4.1 图像生成

**端点**: `POST /v1/images/generations`

```json
{
  "model": "dall-e-3",
  "prompt": "A cute baby sea otter floating on its back",
  "n": 1,
  "quality": "standard",
  "response_format": "url",
  "size": "1024x1024",
  "style": "vivid",
  "user": "user-123"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `model` | string | ✅ | `dall-e-2` / `dall-e-3` / `gpt-image-1` |
| `prompt` | string | ✅ | 图像描述（dall-e-3 最多 4000 字符） |
| `n` | integer | ❌ | 生成数量（dall-e-3 仅支持 1） |
| `quality` | string | ❌ | `standard` / `hd` |
| `response_format` | string | ❌ | `url` / `b64_json` |
| `size` | string | ❌ | `256x256` / `512x512` / `1024x1024` / `1792x1024` / `1024x1792` |
| `style` | string | ❌ | `vivid` / `natural`（仅 dall-e-3） |

**响应格式**:

```json
{
  "created": 1713833628,
  "data": [
    {
      "url": "https://cdn.example.com/image.png",
      "revised_prompt": "A cute baby sea otter..."
    }
  ]
}
```

#### 2.4.2 图像编辑

**端点**: `POST /v1/images/edits`

**Content-Type**: `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `image` | file | ✅ | 原始图片（PNG，≤4MB，正方形） |
| `mask` | file | ❌ | 透明区域为编辑区域 |
| `prompt` | string | ✅ | 编辑描述 |
| `model` | string | ❌ | 默认 `dall-e-2` |
| `n` | integer | ❌ | 生成数量 |
| `size` | string | ❌ | `256x256` / `512x512` / `1024x1024` |

#### 2.4.3 图像变体

**端点**: `POST /v1/images/variations`

**Content-Type**: `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `image` | file | ✅ | 原始图片（PNG，≤4MB，正方形） |
| `model` | string | ❌ | 默认 `dall-e-2` |
| `n` | integer | ❌ | 生成数量 |
| `size` | string | ❌ | 输出尺寸 |

---

### 2.5 Audio API

#### 2.5.1 文字转语音 (TTS)

**端点**: `POST /v1/audio/speech`

```json
{
  "model": "tts-1",
  "input": "Hello, how are you today?",
  "voice": "alloy",
  "response_format": "mp3",
  "speed": 1.0
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `model` | string | ✅ | `tts-1` / `tts-1-hd` |
| `input` | string | ✅ | 输入文本（最多 4096 字符） |
| `voice` | string | ✅ | `alloy` / `echo` / `fable` / `onyx` / `nova` / `shimmer` |
| `response_format` | string | ❌ | `mp3` / `opus` / `aac` / `flac` / `wav` / `pcm` |
| `speed` | number | ❌ | 语速 (0.25-4.0) |

**响应**: 二进制音频数据

#### 2.5.2 语音转文字 (STT)

**端点**: `POST /v1/audio/transcriptions`

**Content-Type**: `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file | ✅ | 音频文件 |
| `model` | string | ✅ | `whisper-1` |
| `language` | string | ❌ | ISO-639-1 语言代码 |
| `prompt` | string | ❌ | 转录提示 |
| `response_format` | string | ❌ | `json` / `text` / `srt` / `verbose_json` / `vtt` |
| `temperature` | number | ❌ | 采样温度 (0-1) |

**响应**:

```json
{
  "text": "Hello, how are you today?"
}
```

#### 2.5.3 语音翻译

**端点**: `POST /v1/audio/translations`

**Content-Type**: `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file | ✅ | 音频文件 |
| `model` | string | ✅ | `whisper-1` |
| `prompt` | string | ❌ | 翻译提示 |
| `response_format` | string | ❌ | 输出格式 |

**响应**: 翻译为英文的文本

---

### 2.6 Models API

#### 2.6.1 模型列表

**端点**: `GET /v1/models`

**响应**:

```json
{
  "object": "list",
  "data": [
    {
      "id": "gpt-4o",
      "object": "model",
      "created": 1715367049,
      "ownedBy": "system"
    }
  ]
}
```

#### 2.6.2 模型详情

**端点**: `GET /v1/models/{model}`

**响应**:

```json
{
  "id": "gpt-4o",
  "object": "model",
  "created": 1715367049,
  "ownedBy": "system"
}
```

---

### 2.7 Moderations API

**端点**: `POST /v1/moderations`

```json
{
  "model": "omni-moderation-latest",
  "input": "I want to harm someone."
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `model` | string | ❌ | `omni-moderation-latest` / `text-moderation-latest` |
| `input` | string/array | ✅ | 待审核内容 |

**响应**:

```json
{
  "id": "modr-abc123",
  "model": "omni-moderation-latest",
  "results": [
    {
      "flagged": true,
      "categories": {
        "hate": false,
        "harassment": true,
        "self-harm": false,
        "sexual": false,
        "violence": true
      },
      "category_scores": {
        "hate": 0.1,
        "harassment": 0.9,
        "self-harm": 0.0,
        "sexual": 0.0,
        "violence": 0.8
      }
    }
  ]
}
```

---

## 三、Anthropic API

### 3.1 端点总览

| 端点 | 方法 | 说明 | 优先级 |
|------|------|------|--------|
| `/anthropic/v1/messages` | POST | 消息创建（核心） | P0 |
| `/anthropic/v1/messages/batches` | POST | 批量消息请求（规划中） | P2 |
| `/anthropic/v1/messages/batches/{batch_id}` | GET | 查询批量状态（规划中） | P2 |
| `/anthropic/v1/messages/batches/{batch_id}/results` | GET | 获取批量结果（规划中） | P2 |

---

### 3.2 Messages API

#### 3.2.1 请求格式

**端点**: `POST /anthropic/v1/messages`

**请求头**:

```
Content-Type: application/json
x-api-key: {api_key}
anthropic-version: 2023-06-01
```

```json
{
  "model": "claude-sonnet-4-20250514",
  "max_tokens": 1024,
  "system": "You are a helpful assistant.",
  "messages": [
    {"role": "user", "content": "Hello!"},
    {"role": "assistant", "content": "Hi there!"},
    {"role": "user", "content": "How are you?"}
  ],
  "temperature": 0.7,
  "stream": false,
  "stop_sequences": ["STOP"],
  "tools": [
    {
      "name": "get_weather",
      "description": "Get current weather",
      "input_schema": {
        "type": "object",
        "properties": {
          "location": {"type": "string"}
        },
        "required": ["location"]
      }
    }
  ],
  "tool_choice": {"type": "auto"}
}
```

#### 3.2.2 请求参数说明

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `model` | string | ✅ | - | 模型 ID，如 `claude-sonnet-4-20250514` |
| `max_tokens` | integer | ✅ | - | 最大生成 Token 数 |
| `messages` | array | ✅ | - | 对话消息列表 |
| `system` | string | ❌ | null | 系统提示词 |
| `temperature` | number | ❌ | 1.0 | 采样温度 (0-1) |
| `stream` | boolean | ❌ | false | 是否流式返回 |
| `stop_sequences` | array | ❌ | null | 停止序列 |
| `tools` | array | ❌ | null | 工具定义列表 |
| `tool_choice` | object | ❌ | `{"type": "auto"}` | 工具选择策略 |

#### 3.2.3 消息格式

**基础消息**:

```json
{"role": "user|assistant", "content": "消息内容"}
```

**多模态消息 (Vision)**:

```json
{
  "role": "user",
  "content": [
    {"type": "text", "text": "这张图片是什么?"},
    {
      "type": "image",
      "source": {
        "type": "url",
        "url": "https://example.com/image.png"
      }
    }
  ]
}
```

**图片输入方式**:

| 方式 | 格式 |
|------|------|
| URL | `{"type": "url", "url": "https://..."}` |
| Base64 | `{"type": "base64", "media_type": "image/png", "data": "..."}` |

**支持的图片格式**:

| 格式 | media_type |
|------|------------|
| PNG | `image/png` |
| JPEG | `image/jpeg` |
| GIF | `image/gif` |
| WebP | `image/webp` |

**Assistant 消息带 Tool Use**:

```json
{
  "role": "assistant",
  "content": [
    {
      "type": "text",
      "text": "Let me check the weather for you."
    },
    {
      "type": "tool_use",
      "id": "toolu_abc123",
      "name": "get_weather",
      "input": {"location": "Beijing"}
    }
  ]
}
```

**Tool Result 消息**:

```json
{
  "role": "user",
  "content": [
    {
      "type": "tool_result",
      "tool_use_id": "toolu_abc123",
      "content": "{\"temperature\": 25, \"condition\": \"sunny\"}"
    }
  ]
}
```

#### 3.2.4 响应格式

**非流式响应**:

```json
{
  "id": "msg_abc123",
  "type": "message",
  "role": "assistant",
  "model": "claude-sonnet-4-20250514",
  "content": [
    {
      "type": "text",
      "text": "Hello! How can I help you today?"
    }
  ],
  "stop_reason": "end_turn",
  "stop_sequence": null,
  "usage": {
    "input_tokens": 15,
    "output_tokens": 10
  }
}
```

**带 Tool Use 的响应**:

```json
{
  "id": "msg_abc123",
  "type": "message",
  "role": "assistant",
  "model": "claude-sonnet-4-20250514",
  "content": [
    {
      "type": "text",
      "text": "Let me check that for you."
    },
    {
      "type": "tool_use",
      "id": "toolu_abc123",
      "name": "get_weather",
      "input": {"location": "Beijing"}
    }
  ],
  "stop_reason": "tool_use",
  "stop_sequence": null,
  "usage": {
    "input_tokens": 50,
    "output_tokens": 30
  }
}
```

#### 3.2.5 流式 SSE 事件格式

**请求**: 设置 `"stream": true`

**SSE 事件流**:

```
event: message_start
data: {"type":"message_start","message":{"id":"msg_abc123","type":"message","role":"assistant","content":[],"model":"claude-sonnet-4-20250514","stop_reason":null,"usage":{"input_tokens":15,"output_tokens":0}}}

event: content_block_start
data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}

event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"!"}}

event: content_block_stop
data: {"type":"content_block_stop","index":0}

event: message_delta
data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":10}}

event: message_stop
data: {"type":"message_stop"}
```

**事件类型说明**:

| 事件 | 说明 |
|------|------|
| `message_start` | 消息开始，包含初始信息 |
| `content_block_start` | 内容块开始 |
| `content_block_delta` | 内容块增量 |
| `content_block_stop` | 内容块结束 |
| `message_delta` | 消息增量（stop_reason 等） |
| `message_stop` | 消息结束 |
| `ping` | 心跳事件 |

#### 3.2.6 stop_reason 枚举

| 值 | 说明 |
|------|------|
| `end_turn` | 正常结束 |
| `max_tokens` | 达到 max_tokens 限制 |
| `stop_sequence` | 遇到停止序列 |
| `tool_use` | 模型调用了工具 |

---

### 3.3 Messages Batches API

> **状态**: 规划中（未实现）

#### 3.3.1 创建批量请求

**端点**: `POST /anthropic/v1/messages/batches`

```json
{
  "requests": [
    {
      "custom_id": "request-1",
      "params": {
        "model": "claude-sonnet-4-20250514",
        "max_tokens": 100,
        "messages": [{"role": "user", "content": "Hello"}]
      }
    },
    {
      "custom_id": "request-2",
      "params": {
        "model": "claude-sonnet-4-20250514",
        "max_tokens": 100,
        "messages": [{"role": "user", "content": "World"}]
      }
    }
  ]
}
```

**响应**:

```json
{
  "id": "batch_abc123",
  "type": "message_batch",
  "processing_status": "in_progress",
  "request_counts": {
    "processing": 2,
    "succeeded": 0,
    "errored": 0,
    "canceled": 0,
    "expired": 0
  }
}
```

#### 3.3.2 查询批量状态

**端点**: `GET /anthropic/v1/messages/batches/{batch_id}`

#### 3.3.3 获取批量结果

**端点**: `GET /anthropic/v1/messages/batches/{batch_id}/results`

---

## 四、协议转换

### 4.1 转换矩阵

| 源协议 | 目标协议 | 支持状态 | 说明 |
|--------|---------|---------|------|
| OpenAI | Anthropic | ✅ | 客户端用 OpenAI SDK 调用 Anthropic 模型 |
| Anthropic | OpenAI | ✅ | 客户端用 Anthropic SDK 调用 OpenAI 模型 |
| 各厂商 | OpenAI | ✅ | 统一转换为 OpenAI 格式 |
| 各厂商 | Anthropic | ✅ | 统一转换为 Anthropic 格式 |

### 4.2 参数映射表

#### 4.2.1 Chat Completions ↔ Messages 参数映射

| OpenAI 参数 | Anthropic 参数 | 转换规则 |
|-------------|----------------|---------|
| `model` | `model` | 直接映射（模型别名） |
| `messages` | `messages` | 格式转换（见 4.3） |
| `temperature` | `temperature` | 直接映射 |
| `max_tokens` | `max_tokens` | 直接映射 |
| `stop` | `stop_sequences` | 数组格式一致 |
| `stream` | `stream` | 直接映射 |
| `tools` | `tools` | 格式转换（见 4.4） |
| `tool_choice` | `tool_choice` | 格式转换 |
| `presence_penalty` | - | Anthropic 不支持 |
| `frequency_penalty` | - | Anthropic 不支持 |
| `seed` | - | Anthropic 不支持 |
| - | `system` | OpenAI 用 messages 中 role=system |

#### 4.2.2 system 消息处理

**OpenAI 格式**:
```json
{"role": "system", "content": "You are a helpful assistant."}
```

**Anthropic 格式**:
```json
{"system": "You are a helpful assistant."}
```

**转换规则**: OpenAI 的第一条 `role=system` 消息提取为 Anthropic 的 `system` 字段。

### 4.3 消息格式转换

#### 4.3.1 基础消息转换

**OpenAI → Anthropic**:

| OpenAI | Anthropic |
|--------|-----------|
| `{"role": "user", "content": "text"}` | `{"role": "user", "content": "text"}` |
| `{"role": "assistant", "content": "text"}` | `{"role": "assistant", "content": "text"}` |
| `{"role": "system", "content": "text"}` | 提取为 `system` 字段 |

#### 4.3.2 多模态消息转换

**OpenAI Vision → Anthropic Vision**:

```json
// OpenAI 格式
{
  "role": "user",
  "content": [
    {"type": "text", "text": "What is this?"},
    {"type": "image_url", "image_url": {"url": "https://example.com/img.png"}}
  ]
}

// 转换为 Anthropic 格式
{
  "role": "user",
  "content": [
    {"type": "text", "text": "What is this?"},
    {"type": "image", "source": {"type": "url", "url": "https://example.com/img.png"}}
  ]
}
```

**Base64 图片转换**:

```json
// OpenAI 格式
{"type": "image_url", "image_url": {"url": "data:image/png;base64,iVBORw0KGgo..."}}

// 转换为 Anthropic 格式
{
  "type": "image",
  "source": {
    "type": "base64",
    "media_type": "image/png",
    "data": "iVBORw0KGgo..."
  }
}
```

### 4.4 Tool Use 转换

#### 4.4.1 工具定义转换

**OpenAI Function → Anthropic Tool**:

```json
// OpenAI 格式
{
  "type": "function",
  "function": {
    "name": "get_weather",
    "description": "Get weather info",
    "parameters": {
      "type": "object",
      "properties": {"location": {"type": "string"}},
      "required": ["location"]
    }
  }
}

// 转换为 Anthropic 格式
{
  "name": "get_weather",
  "description": "Get weather info",
  "input_schema": {
    "type": "object",
    "properties": {"location": {"type": "string"}},
    "required": ["location"]
  }
}
```

#### 4.4.2 工具调用转换

**OpenAI Tool Call → Anthropic Tool Use**:

```json
// OpenAI 格式
{
  "role": "assistant",
  "content": null,
  "tool_calls": [
    {
      "id": "call_abc123",
      "type": "function",
      "function": {
        "name": "get_weather",
        "arguments": "{\"location\": \"Beijing\"}"
      }
    }
  ]
}

// 转换为 Anthropic 格式
{
  "role": "assistant",
  "content": [
    {
      "type": "tool_use",
      "id": "call_abc123",
      "name": "get_weather",
      "input": {"location": "Beijing"}
    }
  ]
}
```

#### 4.4.3 工具结果转换

**OpenAI Tool Result → Anthropic Tool Result**:

```json
// OpenAI 格式
{
  "role": "tool",
  "tool_call_id": "call_abc123",
  "content": "{\"temp\": 25}"
}

// 转换为 Anthropic 格式
{
  "role": "user",
  "content": [
    {
      "type": "tool_result",
      "tool_use_id": "call_abc123",
      "content": "{\"temp\": 25}"
    }
  ]
}
```

### 4.5 流式事件转换

| OpenAI SSE | Anthropic SSE | 转换规则 |
|------------|---------------|---------|
| `data: {"choices":[{"delta":{"role":"assistant"}}]}` | `event: message_start` | 首个事件 |
| `data: {"choices":[{"delta":{"content":"Hello"}}]}` | `event: content_block_delta` + `text_delta` | 文本增量 |
| `data: {"choices":[{"finish_reason":"stop"}]}` | `event: message_delta` + `stop_reason` | 结束事件 |
| `data: [DONE]` | `event: message_stop` | 结束标记 |

---

## 五、错误响应

### 5.1 OpenAI 错误格式

```json
{
  "error": {
    "message": "Invalid API key provided.",
    "type": "invalid_request_error",
    "param": null,
    "code": "invalid_api_key"
  }
}
```

**错误类型**:

| type | 说明 |
|------|------|
| `invalid_request_error` | 请求格式错误 |
| `authentication_error` | 认证失败 |
| `permission_error` | 权限不足 |
| `not_found_error` | 资源不存在 |
| `rate_limit_error` | 限流 |
| `api_error` | 服务器错误 |

### 5.2 Anthropic 错误格式

```json
{
  "type": "error",
  "error": {
    "type": "invalid_request_error",
    "message": "max_tokens is required"
  }
}
```

**错误类型**:

| type | 说明 |
|------|------|
| `invalid_request_error` | 请求格式错误 |
| `authentication_error` | 认证失败 |
| `permission_error` | 权限不足 |
| `not_found_error` | 资源不存在 |
| `rate_limit_error` | 限流 |
| `api_error` | 服务器错误 |
| `overloaded_error` | 服务过载 |

### 5.3 网关统一错误格式

网关错误响应按端口区分；透传上游错误时保持原始格式，网关自身错误结构如下。

**管理面 API（`/api/v1/**`）**：统一 `ApiResponse` 信封（gateway-common `ApiResponse`），业务响应经 `ApiResponseWrapperAdvice` 自动包装为 `$.data.*`（单对象）：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "QUOTA_EXCEEDED",
    "message": "用户可读的错误信息",
    "details": null
  },
  "traceId": "trace_1713833628",
  "timestamp": "2026-08-31T10:30:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | boolean | 请求是否成功 |
| `data` | T | 业务数据（失败时为 `null`） |
| `error.code` | string | 业务错误码 |
| `error.message` | string | 用户可读的错误信息 |
| `error.details` | object | 附加详情（可选） |
| `traceId` | string | 链路追踪 ID |
| `timestamp` | string | ISO 8601 时间戳 |

**OpenAI 兼容端点**：网关自身校验异常返回 OpenAI 兼容结构（仅 `message/type/code` 三个字段）：

```json
{
  "error": {
    "message": "max_tokens is required",
    "type": "invalid_request_error",
    "code": "max_tokens"
  }
}
```

**Anthropic 兼容端点**：网关自身校验异常返回 Anthropic 兼容结构：

```json
{
  "type": "error",
  "error": {
    "type": "invalid_request_error",
    "message": "max_tokens is required"
  }
}
```

### 5.4 HTTP 状态码映射

| HTTP 状态码 | 说明 | OpenAI code | Anthropic type |
|-------------|------|-------------|----------------|
| 400 | 请求格式错误 | `invalid_request_error` | `invalid_request_error` |
| 401 | 认证失败 | `invalid_api_key` | `authentication_error` |
| 403 | 权限不足 | `permission_denied` | `permission_error` |
| 404 | 资源不存在 | `model_not_found` | `not_found_error` |
| 429 | 限流 | `rate_limit_exceeded` | `rate_limit_error` |
| 500 | 服务器错误 | `api_error` | `api_error` |
| 503 | 服务不可用 | `server_error` | `overloaded_error` |

---

## 六、版本兼容性

### 6.1 OpenAI API 版本

OpenAI API 不使用显式版本号，通过模型 ID 和端点演进：

| 模型系列 | 说明 |
|---------|------|
| `gpt-4o` | 最新旗舰模型 |
| `gpt-4-turbo` | GPT-4 Turbo 系列 |
| `gpt-3.5-turbo` | 高性价比模型 |
| `o1` | 推理增强模型 |
| `text-embedding-3-*` | 最新嵌入模型 |
| `dall-e-3` | 最新图像生成模型 |

### 6.2 Anthropic API 版本

Anthropic API 通过请求头指定版本：

```
anthropic-version: 2023-06-01
```

**当前支持版本**: `2023-06-01`

**模型系列**:

| 模型系列 | 说明 |
|---------|------|
| `claude-opus-4-*` | 最强能力模型 |
| `claude-sonnet-4-*` | 平衡性能与成本 |
| `claude-haiku-*` | 快速轻量模型 |

### 6.3 网关兼容策略

| 策略 | 说明 |
|------|------|
| **向后兼容** | 新增字段不影响旧客户端 |
| **版本升级** | 通过 URL 版本控制（`/v1/`） |
| **废弃通知** | 响应头添加 `Deprecation: true` |

---

## 附录

### A. 参考文档

- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)
- [Anthropic API Reference](https://docs.anthropic.com/en/api)

### B. 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2026-05-02 | 初始版本，完整定义 OpenAI 和 Anthropic API 兼容规范 |
| v1.1 | 2026-05-06 | 新增第七章管理 API - Provider API Key 管理 API |

---

## 七、管理面 API 总览

> **说明**:
>
> - 本章为管理面（Console/CLI 消费）全部 API 端点清单，按 Controller 分组，与 `gateway-web/src/main/java/com/codingas/gateway/web/api/` 下的实现一一对应。
> - 管理面统一响应信封 `ApiResponse`（字段见 5.3），Controller 返回的业务对象经 `ApiResponseWrapperAdvice` 自动包装为 `$.data.*`（单对象；列表端点的 `$.data` 为数组，分页端点为分页对象）。
> - 供应商凭证（ChannelCredential）管理挂在渠道维度（`/api/v1/channels/{channelId}/credentials`），替代早期 Provider API Key 设计。
> - 「请求体/响应」列：`body:` 后为 JSON 请求体 DTO（`*` 为必填字段），`query:` 后为查询参数，`→` 后为 `$.data` 中的业务数据类型；删除/状态类操作无业务数据。

### 7.1 模型管理（ModelController，`/api/v1/models`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/models` | 创建模型 | body: `ModelCreateRequest` → `ModelResponse` |
| POST | `/api/v1/models/{id}/copy` | 复制模型 | body: `{modelName*, displayName?, modelFamily?}` → `ModelResponse` |
| GET | `/api/v1/models/{id}` | 模型详情 | → `ModelResponse` |
| GET | `/api/v1/models` | 分页查询模型 | query: 分页/过滤参数 → `PageResponse<ModelResponse>` |
| PUT | `/api/v1/models/{id}` | 更新模型 | body: `ModelUpdateRequest` → `ModelResponse` |
| POST | `/api/v1/models/{id}/unlock` | 解锁模型字段 | → `ModelResponse` |
| DELETE | `/api/v1/models/{id}` | 删除模型 | 无业务数据 |
| PATCH | `/api/v1/models/{id}/state?enabled=` | 启用/禁用模型 | query: `enabled`（boolean）→ `ModelResponse` |

### 7.2 渠道管理（ChannelController，`/api/v1/channels`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/channels` | 创建渠道 | body: `ChannelRequest` → `ChannelResponse` |
| POST | `/api/v1/channels/{id}/copy` | 复制渠道 | body: `{name*, copyCredentials=false}` → `ChannelResponse` |
| PUT | `/api/v1/channels/{id}` | 更新渠道 | body: `ChannelRequest` → `ChannelResponse` |
| GET | `/api/v1/channels/{id}` | 渠道详情 | → `ChannelResponse` |
| GET | `/api/v1/channels` | 渠道列表 | query: `providerId?`、`billingMode?`、`sortBy?`、`sortOrder?` → `List<ChannelResponse>` |
| PUT | `/api/v1/channels/{id}/state` | 渠道状态流转 | body: `ChannelStateTransitionRequest`，无业务数据 |
| DELETE | `/api/v1/channels/{id}` | 删除渠道 | 无业务数据 |
| POST | `/api/v1/channels/{channelId}/endpoints` | 新增渠道端点 | body: `ChannelEndpointRequest` → `ChannelEndpointResponse` |
| DELETE | `/api/v1/channels/{channelId}/endpoints/{endpointId}` | 删除渠道端点 | 无业务数据 |
| PUT | `/api/v1/channels/{channelId}/endpoints/{endpointId}` | 更新渠道端点 | body: `ChannelEndpointRequest` → `ChannelEndpointResponse` |
| POST | `/api/v1/channels/{id}/health-check` | 触发健康检查 | body: `ChannelHealthCheckRequest` → `ChannelHealthResult` |
| POST | `/api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/force-open` | 强制开启熔断 | → `CircuitBreakerStateResponse` |
| POST | `/api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/force-close` | 强制关闭熔断（恢复） | → `CircuitBreakerStateResponse` |
| GET | `/api/v1/channels/{channelId}/endpoints/{endpointId}/circuit-breaker/state` | 查询熔断状态 | → `CircuitBreakerStateResponse` |

### 7.3 渠道凭证管理（ChannelCredentialController，`/api/v1/channels/{channelId}/credentials`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| GET | `/api/v1/channels/{channelId}/credentials` | 凭证列表 | → `List<ChannelCredentialResponse>` |
| GET | `/api/v1/channels/{channelId}/credentials/{id}` | 凭证详情 | → `ChannelCredentialResponse` |
| POST | `/api/v1/channels/{channelId}/credentials` | 创建凭证 | body: `ChannelCredentialCreateRequest` → `ChannelCredentialCreateResponse` |
| PUT | `/api/v1/channels/{channelId}/credentials/{id}` | 更新凭证 | body: `ChannelCredentialUpdateRequest` → `ChannelCredentialResponse` |
| DELETE | `/api/v1/channels/{channelId}/credentials/{id}` | 删除凭证 | 无业务数据 |
| POST | `/api/v1/channels/{channelId}/credentials/{id}/test` | 凭证连通性测试 | → `ApiKeyTestResponse` |

### 7.4 渠道模型实例（ModelInstanceController，`/api/v1/channels/{channelId}/models`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| GET | `/api/v1/channels/{channelId}/models` | 渠道模型实例列表 | query: `sortBy?`、`sortOrder?` → `List<ModelInstanceResponse>` |
| POST | `/api/v1/channels/{channelId}/models` | 渠道绑定模型 | body: `ModelInstanceCreateRequest` → `ModelInstanceResponse` |
| DELETE | `/api/v1/channels/{channelId}/models/{id}` | 删除模型实例 | 无业务数据 |
| PUT | `/api/v1/channels/{channelId}/models/{id}/state` | 启用/禁用模型实例 | body: `ModelInstanceStateTransitionRequest`，无业务数据 |
| PATCH | `/api/v1/channels/{channelId}/models/{id}/upstream-model-name` | 更新上游模型名 | body: `{"upstreamModelName": "..."}`，无业务数据 |
| PUT | `/api/v1/channels/{channelId}/models/{id}` | 更新模型实例 | body: `ModelInstanceUpdateRequest` → `ModelInstanceResponse` |

### 7.5 渠道开通（ChannelProvisionController，`/api/v1/provision`，均需 ADMIN 角色）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/provision/from-plan/{planCode}` | 按套餐开通渠道 | → `ProvisionResult` |
| POST | `/api/v1/provision/batch/{providerCode}` | 按供应商批量开通渠道 | → `BatchProvisionResult` |
| POST | `/api/v1/provision/model/{modelName}` | 按模型开通渠道 | → `ProvisionResult` |
| POST | `/api/v1/provision/sync/builtin` | 同步内置渠道 | 无业务数据 |

### 7.6 供应商管理（ProviderController，`/api/v1/providers`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/providers` | 创建供应商 | body: `ProviderCreateRequest` → `ProviderResponse` |
| GET | `/api/v1/providers/{id}` | 供应商详情 | → `ProviderResponse` |
| GET | `/api/v1/providers` | 分页查询供应商 | query: 分页/过滤参数（`ProviderQueryRequest`）→ `PageResponse<ProviderResponse>` |
| PUT | `/api/v1/providers/{id}` | 更新供应商 | body: `ProviderUpdateRequest` → `ProviderResponse` |
| DELETE | `/api/v1/providers/{id}` | 删除供应商 | 无业务数据 |
| GET | `/api/v1/providers/names` | 供应商名称列表 | → `List<String>` |
| POST | `/api/v1/providers/test-connectivity` | 连通性测试 | body: `ConnectivityTestRequest` → `ConnectivityTestResult` |

### 7.7 套餐目录（PlanCatalogController，`/api/v1/plan-catalogs`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| GET | `/api/v1/plan-catalogs/providers` | 目录供应商列表 | query: `keyword?` → `List<ProviderCatalogResponse>` |
| GET | `/api/v1/plan-catalogs` | 套餐列表 | query: `providerCode?` → `List<PlanCatalogResponse>` |
| GET | `/api/v1/plan-catalogs/{planCode}` | 套餐详情 | → `PlanDetailResponse` |
| GET | `/api/v1/plan-catalogs/{planCode}/pricing` | 套餐定价 | → `List<PlanDetailResponse.PricingInfo>` |
| GET | `/api/v1/plan-catalogs/models` | 目录模型列表 | query: `keyword?` → `List<ModelResponse>` |

### 7.8 目录同步（CatalogSyncController，`/api/v1/catalog/sync`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/catalog/sync` | 触发目录同步 | → `CatalogSyncReportResponse` |
| GET | `/api/v1/catalog/sync/status` | 最近同步状态 | → `CatalogSyncStatusResponse` |

### 7.9 协议查询（ProtocolController，`/api/v1/protocols`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| GET | `/api/v1/protocols` | 已注册协议列表 | → `List<{name, label}>` |

### 7.10 应用管理（ApplicationController，`/api/v1/applications`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/applications` | 创建应用 | body: `ApplicationRequest` → `ApplicationResponse` |
| PUT | `/api/v1/applications/{id}` | 更新应用 | body: 应用更新请求 → `ApplicationResponse` |
| GET | `/api/v1/applications/{id}` | 应用详情 | → `ApplicationResponse` |
| GET | `/api/v1/applications` | 应用列表 | → `List<ApplicationResponse>` |
| DELETE | `/api/v1/applications/{id}` | 删除应用 | 无业务数据 |
| GET | `/api/v1/applications/{id}/api-keys` | 应用 API Key 列表 | → `List<UserApiKeyResponse>` |
| GET | `/api/v1/applications/{id}/channels` | 应用授权渠道列表 | → `List<ApplicationChannelItem>` |
| PUT | `/api/v1/applications/{id}/channels` | 更新应用授权渠道 | body: `ApplicationChannelRequest`，无业务数据 |

### 7.11 API Key 管理（UserApiKeyController，`/api/v1/user-api-keys`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/user-api-keys` | 创建 API Key | body: `UserApiKeyCreateRequest` → `UserApiKeyCreateResponse` |
| GET | `/api/v1/user-api-keys?userId=` | 按 userId 查询 API Key | query: `userId` → `List<UserApiKeyResponse>` |
| GET | `/api/v1/user-api-keys` | API Key 列表 | → `List<UserApiKeyResponse>` |
| GET | `/api/v1/user-api-keys/{id}` | API Key 详情 | → `UserApiKeyResponse` |
| GET | `/api/v1/user-api-keys/{id}/detail` | API Key 明细查询 | → `UserApiKeyResponse` |
| PUT | `/api/v1/user-api-keys/{id}` | 更新 API Key | body: `UserApiKeyUpdateRequest` → `UserApiKeyResponse` |
| DELETE | `/api/v1/user-api-keys/{id}` | 删除 API Key | 无业务数据 |

### 7.12 用户管理（UserController，`/api/v1/users`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/users` | 创建用户 | body: `UserCreateRequest` → `UserResponse` |
| GET | `/api/v1/users/{id}` | 用户详情 | → `UserResponse` |
| GET | `/api/v1/users` | 分页查询用户 | query: 分页/过滤参数（`UserQueryRequest`）→ `PageResponse<UserResponse>` |
| PUT | `/api/v1/users/{id}` | 更新用户 | body: 用户更新请求 → `UserResponse` |
| DELETE | `/api/v1/users/{id}` | 删除用户 | 无业务数据 |
| PATCH | `/api/v1/users/{id}/state` | 启用/禁用用户 | body: `UserStateUpdateRequest` → `UserResponse` |
| PUT | `/api/v1/users/{id}/roles` | 分配用户角色 | body: `UserRoleAssignRequest` → `UserResponse` |
| POST | `/api/v1/users/{id}/reset-password` | 重置密码 | → `ResetPasswordResponse`（含一次性明文密码） |
| GET | `/api/v1/users/{userId}/api-keys` | 用户 API Key 列表 | → `List<UserApiKeyResponse>` |

### 7.13 认证（AuthController，`/api/v1/auth`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/auth/login` | 登录 | body: `{username*, password*, rememberMe?}` → `LoginResponse` |
| POST | `/api/v1/auth/logout` | 登出 | 无业务数据 |
| GET | `/api/v1/auth/me` | 当前登录用户 | → `UserResponse` |
| PATCH | `/api/v1/auth/me/password` | 修改密码 | body: `ChangePasswordRequest`，无业务数据 |

### 7.14 个人中心（MeController，`/api/v1/me`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| GET | `/api/v1/me/api-keys` | 当前登录用户的 API Key 列表 | → `List<UserApiKeyResponse>` |

### 7.15 Token 限额（TokenLimitController，`/api/v1/token-limits`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/token-limits` | 创建限额规则 | body: `TokenLimitCreateRequest` → `TokenLimitResponse` |
| GET | `/api/v1/token-limits/{id}` | 限额详情 | → `TokenLimitResponse` |
| GET | `/api/v1/token-limits` | 分页查询限额 | query: 分页/过滤参数（`TokenLimitQueryRequest`）→ `PageResponse<TokenLimitResponse>` |
| PUT | `/api/v1/token-limits/{id}` | 更新限额 | body: 限额更新请求 → `TokenLimitResponse` |
| DELETE | `/api/v1/token-limits/{id}` | 删除限额 | 无业务数据 |
| PATCH | `/api/v1/token-limits/{id}/reset-usage` | 重置用量 | → `TokenLimitResponse` |

### 7.16 统计（StatsController，`/api/v1/stats`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| GET | `/api/v1/stats` | 总览统计 | → `StatsResponse` |
| GET | `/api/v1/stats/trend` | 调用趋势 | query: `days`（默认 7）→ `List<StatsTrendResponse>` |
| GET | `/api/v1/stats/model-usage` | 模型用量排行 | query: `limit`（默认 5）→ `List<StatsModelUsageResponse>` |

### 7.17 韧性事件（ResilienceEventController，`/api/v1/resilience/events`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| GET | `/api/v1/resilience/events` | 韧性事件列表 | query: `since?` 等 → `List<FailoverEventResponse>` |
| GET | `/api/v1/resilience/events/exhausted` | 预算耗尽事件 | query: `since?` 等 → `List<FailoverEventResponse>` |

### 7.18 系统设置（SettingsController，`/api/v1/settings`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| GET | `/api/v1/settings` | 系统设置列表 | → `List<SystemSettingResponse>` |
| PUT | `/api/v1/settings/{key}` | 更新设置项 | body: `SettingUpdateRequest` → `SystemSettingResponse` |

### 7.19 审计日志（AuditController，`/api/v1/audit-logs`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| GET | `/api/v1/audit-logs` | 分页查询审计日志 | query: 查询参数（`AuditLogQueryRequest`）→ `PageResponse<AuditLogResponse>` |
| DELETE | `/api/v1/audit-logs` | 清理审计日志 | query: `days?`、`before?` → 删除数量统计 |

### 7.20 体验对话（ExperienceController，`/api/v1/experience`）

| HTTP 动词 | 路径 | 用途 | 请求体/响应 |
|-----------|------|------|------------|
| POST | `/api/v1/experience/chat` | 体验对话（SSE 流式） | body: `ExperienceChatRequest` → SSE（`text/event-stream`） |
| GET | `/api/v1/experience/providers/{providerId}/models` | 供应商可用模型列表 | → `List<ExperienceModelResponse>` |

---

## 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.3 | 2026-08-31 | 与代码实现对齐修订：Anthropic 端点路径修正为 `/anthropic/v1/messages`（batches 端点保留并标注规划中）；Chat/Messages 请求参数表对齐协议 DTO（移除 top_p、n、logit_bias、user / top_p、top_k、metadata）；模型列表字段修正为 `ownedBy`（示例值 system）；5.3 网关统一错误格式重写为 ApiResponse 信封 + 双协议兼容错误；未实现端点标注「规划中」；第七章替换为「管理面 API 总览」（按 Controller 分组的全部管理面端点清单） |
| v1.2 | 2026-05-06 | 补充完整的 CRUD 接口：创建 Key（7.2）、获取详情（7.4）、更新 Key（7.5）、删除 Key（7.6）；新增手动触发恢复检查接口（7.15） |
| v1.1 | 2026-05-06 | 新增第七章管理 API - Provider API Key 管理 API |
| v1.0 | 2026-05-02 | 初始版本 |
