# Quickstart: OpenAI and Anthropic Dual Adapter

**Feature**: 实现OpenAI和Anthropic双适配器
**Date**: 2026-04-23

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 14+ (running)
- API Keys for OpenAI and/or Anthropic

## Project Structure

```
gateway-adapter/
├── src/main/java/com/codingas/gateway/adapter/
│   ├── LLMProviderAdapter.java          # Adapter interface
│   ├── openai/
│   │   └── OpenAIAdapter.java           # OpenAI implementation
│   ├── anthropic/
│   │   └── AnthropicAdapter.java        # Anthropic implementation
│   ├── common/
│   │   └── ...                         # ProviderCapabilities, Exception, etc.
│   └── dto/
│       ├── LLMRequest.java              # Request DTO
│       └── LLMResponse.java             # Response DTO

gateway-dispatch/ (new)
├── src/main/java/com/codingas/gateway/dispatch/
│   ├── ProtocolTranslator.java           # Protocol conversion
│   └── ErrorResponseAdapter.java        # Error format adaptation
```

## Adding Dependencies

### pom.xml (gateway-adapter)

```xml
<!-- Spring Web (MVC) - includes RestClient -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
</dependency>

<!-- OkHttp for streaming -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>mockwebserver</artifactId>
    <version>4.12.0</version>
    <scope>test</scope>
</dependency>
```

## Configuration

```yaml
gateway:
  adapter:
    openai:
      base-url: https://api.openai.com
      timeout-seconds: 30
    anthropic:
      base-url: https://api.anthropic.com
      version: 2023-06-01
      timeout-seconds: 30
```

## Basic Usage

### Creating an OpenAI Adapter

```java
OpenAIAdapter adapter = new OpenAIAdapter(
    "https://api.openai.com",
    apiKey,  // from encrypted storage
    30
);

// Check availability
if (adapter.isAvailable()) {
    System.out.println("OpenAI adapter ready");
}
```

### Sending a Chat Request (Non-Streaming)

```java
LLMRequest request = LLMRequest.builder()
    .model("gpt-4o")
    .messages(List.of(
        LLMRequest.Message.builder()
            .role("user")
            .content("Hello!")
            .build()
    ))
    .temperature(0.7)
    .maxTokens(1024)
    .build();

LLMResponse response = adapter.chat(request);
System.out.println("Response: " + response.getContent().getText());
```

### Sending a Streaming Request

```java
StreamingResponseBody streamingBody = adapter.chatStream(request, 
    new StreamCallback() {
        @Override
        public void onChunk(String data) {
            // data is SSE format: "data: {...}"
            System.out.println("Received: " + data);
        }
        
        @Override
        public void onComplete() {
            System.out.println("Stream complete");
        }
        
        @Override
        public void onError(Throwable t) {
            t.printStackTrace();
        }
    });
```

### Controller Integration (Spring MVC)

```java
@RestController
public class LLMController {

    private final OpenAIAdapter openAIAdapter;
    private final AnthropicAdapter anthropicAdapter;

    @PostMapping("/v1/chat/completions")
    public ResponseEntity<LLMResponse> chat(@RequestBody LLMRequest request) {
        LLMResponse response = openAIAdapter.chat(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/v1/chat/completions/stream")
    public StreamingResponseBody chatStream(@RequestBody LLMRequest request,
                                           HttpServletResponse response) {
        openAIAdapter.chatStream(request, new StreamCallback() {
            @Override
            public void onChunk(String data) {
                try {
                    response.getOutputStream().write(("data: " + data + "\n\n").getBytes());
                    response.getOutputStream().flush();
                } catch (IOException e) {
                    // handle error
                }
            }

            @Override
            public void onComplete() {
                try {
                    response.getOutputStream().write("data: [DONE]\n\n".getBytes());
                    response.getOutputStream().flush();
                    response.getOutputStream().close();
                } catch (IOException e) {
                    // handle error
                }
            }

            @Override
            public void onError(Throwable t) {
                try {
                    response.getOutputStream().write(("data: error\n\n").getBytes());
                    response.getOutputStream().close();
                } catch (IOException e) {
                    // handle error
                }
            }
        });
        return null; // streaming handled via callback
    }

    @PostMapping("/v1/messages")
    public ResponseEntity<LLMResponse> messages(@RequestBody LLMRequest request) {
        LLMResponse response = anthropicAdapter.messages(request);
        return ResponseEntity.ok(response);
    }
}
```

### Using Function Calling

```java
LLMRequest.ToolDefinition tool = LLMRequest.ToolDefinition.builder()
    .type("function")
    .function(LLMRequest.Function.builder()
        .name("get_weather")
        .description("Get weather for a location")
        .parameters("{\"type\":\"object\",\"properties\":{\"location\":{\"type\":\"string\"}}}")
        .build())
    .build();

LLMRequest request = LLMRequest.builder()
    .model("gpt-4o")
    .messages(List.of(
        LLMRequest.Message.builder()
            .role("user")
            .content("What's the weather in Beijing?")
            .build()
    ))
    .tools(List.of(tool))
    .toolChoice("auto")
    .build();
```

## Testing with MockWebServer

```java
@ExtendWith(MockWebServerExtension.class)
class OpenAIAdapterTest {
    
    private OpenAIAdapter adapter;
    private MockWebServer server;
    
    @BeforeEach
    void setUp() {
        server = new MockWebServer();
        adapter = new OpenAIAdapter(server.url("/").toString(), "test-key", 30);
    }
    
    @Test
    void shouldHandleChatRequest() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("{\"id\":\"test\",\"model\":\"gpt-4o\",\"choices\":[{\"message\":{\"content\":\"Hi\"}}]}")
            .addHeader("Content-Type", "application/json"));
        
        LLMResponse response = adapter.chat(testRequest);
        assertEquals("Hi", response.getContent().getText());
    }
    
    @Test
    void shouldHandleStreaming() throws Exception {
        server.enqueue(new MockResponse()
            .setBody("data: {\"choices\":[{\"delta\":{\"content\":\"Hi\"}}]}\n\ndata: [DONE]")
            .addHeader("Content-Type", "text/event-stream"));
        
        List<String> chunks = new ArrayList<>();
        adapter.chatStream(request, new StreamCallback() {
            @Override public void onChunk(String data) { chunks.add(data); }
            @Override public void onComplete() {}
            @Override public void onError(Throwable t) {}
        });
        
        assertEquals(2, chunks.size());
    }
}
```

## Common Issues

### 1. Connection Timeout

- Check network connectivity
- Increase `timeout-seconds` in configuration
- Check firewall/proxy settings

### 2. Authentication Error

- Verify API key is correct
- Check key has sufficient permissions
- Ensure API key is not expired

### 3. SSE Parsing Issues

- OkHttp's `BufferedSource.readUtf8Line()` handles SSE lines correctly
- Filter out empty lines and `data: [DONE]`
