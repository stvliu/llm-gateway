# OpenAI API Contract

**Provider**: OpenAI
**Version**: API v1
**Endpoint**: `/v1/chat/completions`

## Request Contract

### Headers

| Header | Required | Description |
|--------|----------|-------------|
| Authorization | Yes | `Bearer {API_KEY}` |
| Content-Type | Yes | `application/json` |

### Request Body

```json
{
  "model": "gpt-4o",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello!"}
  ],
  "temperature": 0.7,
  "max_tokens": 1024,
  "stream": false,
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "Get weather for a location",
        "parameters": {"type": "object", "properties": {"location": {"type": "string"}}}
      }
    }
  ],
  "tool_choice": "auto"
}
```

### Response Contract (Non-Streaming)

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello! How can I help you?",
        "tool_calls": [
          {
            "id": "call_abc123",
            "type": "function",
            "function": {
              "name": "get_weather",
              "arguments": "{\"location\":\"Beijing\"}"
            }
          }
        ]
      },
      "finish_reason": "tool_calls"
    }
  ],
  "usage": {
    "prompt_tokens": 20,
    "completion_tokens": 50,
    "total_tokens": 70
  }
}
```

### Response Contract (Streaming)

```
data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-4o","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":null}]}

data: [DONE]
```

### Error Contract

```json
{
  "error": {
    "message": "Invalid API key",
    "type": "authentication_error",
    "code": "invalid_api_key",
    "param": null
  }
}
```

## Key Fields Mapping

| LLMResponse Field | OpenAI Response Field |
|-------------------|----------------------|
| providerCode | (set to "openai") |
| id | response.id |
| model | response.model |
| content.text | response.choices[0].message.content |
| content.toolCalls | response.choices[0].message.tool_calls |
| usage.promptTokens | response.usage.prompt_tokens |
| usage.completionTokens | response.usage.completion_tokens |
| usage.totalTokens | response.usage.total_tokens |
| finishReason | response.choices[0].finish_reason |
