# Anthropic Messages API Contract

**Provider**: Anthropic
**Version**: 2023-06-01
**Endpoint**: `/v1/messages`

## Request Contract

### Headers

| Header | Required | Description |
|--------|----------|-------------|
| Authorization | Yes | `Bearer {API_KEY}` |
| Content-Type | Yes | `application/json` |
| anthropic-version | Yes | `2023-06-01` |

### Request Body

```json
{
  "model": "claude-opus-4-5",
  "messages": [
    {"role": "user", "content": "Hello!"}
  ],
  "max_tokens": 1024,
  "temperature": 0.7,
  "system": "You are a helpful assistant.",
  "stream": false,
  "tools": [
    {
      "name": "get_weather",
      "description": "Get weather for a location",
      "input_schema": {
        "type": "object",
        "properties": {
          "location": {"type": "string"}
        }
      }
    }
  ],
  "tool_choice": {"type": "auto"}
}
```

### Response Contract (Non-Streaming)

```json
{
  "id": "msg_abc123",
  "type": "message",
  "role": "assistant",
  "content": [
    {
      "type": "text",
      "text": "Hello! How can I help you?"
    },
    {
      "type": "tool_use",
      "id": "toolu_abc123",
      "name": "get_weather",
      "input": {"location": "Beijing"}
    }
  ],
  "model": "claude-opus-4-5",
  "stop_reason": "tool_calls",
  "stop_sequence": null,
  "usage": {
    "input_tokens": 20,
    "output_tokens": 50
  }
}
```

### Response Contract (Streaming)

```
event: message_start
data: {"type":"message_start","id":"msg_abc123","model":"claude-opus-4-5","usage":{"input_tokens":20}}

event: content_block_start
data: {"type":"content_block_start","id":"msg_abc123_0","index":0,"type":"text"}

event: ping
data: {"type":"ping"}

event: content_block_delta
data: {"type":"content_block_delta","id":"msg_abc123_0","index":0,"delta":{"type":"text_delta","text":"Hello"}}

event: content_block_delta
data: {"type":"content_block_delta","id":"msg_abc123_0","index":0,"delta":{"type":"text_delta","text":"!"}}

event: content_block_stop
data: {"type":"content_block_stop","id":"msg_abc123_0","index":0}

event: message_stop
data: {"type":"message_stop"}
```

### Error Contract

```json
{
  "type": "error",
  "error": {
    "type": "authentication_error",
    "message": "Invalid API key"
  }
}
```

## Key Fields Mapping

| LLMResponse Field | Anthropic Response Field |
|------------------|------------------------|
| providerCode | (set to "anthropic") |
| id | response.id |
| model | response.model |
| content.text | response.content[0].text (when type=text) |
| content.toolCalls | response.content (when type=tool_use) |
| usage.promptTokens | response.usage.input_tokens |
| usage.completionTokens | response.usage.output_tokens |
| finishReason | response.stop_reason |

## OpenAI ↔ Anthropic Mapping

### Message Role Mapping

| OpenAI Role | Anthropic Role |
|-------------|---------------|
| system | system (separate field) |
| user | user |
| assistant | assistant |
| tool | (embedded in assistant) |

### Function Calling ↔ Tool Use Mapping

| OpenAI | Anthropic |
|--------|-----------|
| `tools[].function.name` | `tools[].name` |
| `tools[].function.description` | `tools[].description` |
| `tools[].function.parameters` | `tools[].input_schema` |
| `tool_calls[].id` | `tool_use.id` |
| `tool_calls[].function.name` | `tool_use.name` |
| `tool_calls[].function.arguments` | `tool_use.input` |
