# Research: OpenAI and Anthropic Dual Adapter Implementation

**Feature**: 实现OpenAI和Anthropic双适配器
**Date**: 2026-04-23

## Technology Stack

**Decision**: Spring MVC + RestClient (non-streaming) + OkHttp (streaming)

**Rationale**:
- User explicitly requires Spring MVC (not WebFlux)
- RestClient is Spring 6.1's modern blocking HTTP client
- OkHttp provides superior streaming support via `BufferedSource`

## HTTP Client Architecture

### Non-Streaming: RestClient

```java
RestClient restClient = RestClient.create();
restClient.post()
    .uri("/v1/chat/completions")
    .contentType(MediaType.APPLICATION_JSON)
    .body(requestBody)
    .retrieve()
    .body(ChatCompletionResponse.class);
```

### Streaming: OkHttp

```java
OkHttpClient client = new OkHttpClient();
Request request = new Request.Builder()
    .url("https://api.openai.com/v1/chat/completions")
    .post(RequestBody.create(json, JSON_MEDIA_TYPE))
    .build();

client.newCall(request).enqueue(new Callback() {
    public void onResponse(Call call, Response response) {
        BufferedSource source = response.body().source();
        while (!source.isExhausted()) {
            String line = source.readUtf8Line();
            if (line != null && line.startsWith("data: ")) {
                callback.onChunk(line);
            }
        }
        callback.onComplete();
    }
});
```

## Key Implementation Notes

1. RestClient handles non-streaming requests (chat, messages)
2. OkHttp handles streaming SSE with `BufferedSource` line reading
3. StreamingResponseBody wraps OkHttp callback for MVC integration
4. Protocol translation in dispatch layer, adapters stay single-protocol
