---
change: simulator-verification-enhancement
design-doc: docs/superpowers/specs/2026-06-17-simulator-verification-enhancement-design.md
base-ref: ae59252454e90c73efc831c116db43d16b911b03
---

# Simulator 验证增强 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**目标:** 增强 gateway-simulator 模拟能力（新增 6 种错误模式 + 行为序列 + 延迟/流控制/API Key 覆盖），并基于增强后的 Simulator 编写 Gateway 全链路集成测试。

**架构:** SimulatorModeService 升级为多策略模式分发中心，BehaviorSequence/DelayConfig/StreamConfig/ApiKeyOverrideConfig 作为独立配置类，SimulatorController 根据优先级（行为序列 > API Key 覆盖 > 全局模式）分发响应。集成测试同时启动 Gateway 和 Simulator 两个 Spring Context。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JUnit 5, AssertJ, Awaitility

---

## 文件结构

### 新建文件
- `gateway-simulator/src/main/java/com/codingas/simulator/service/BehaviorSequence.java` — 行为序列数据模型
- `gateway-simulator/src/main/java/com/codingas/simulator/service/DelayConfig.java` — 延迟配置
- `gateway-simulator/src/main/java/com/codingas/simulator/service/StreamConfig.java` — 流控制配置
- `gateway-simulator/src/main/java/com/codingas/simulator/service/ApiKeyOverrideConfig.java` — API Key 覆盖配置
- `gateway-boot/src/test/java/com/codingas/gateway/integration/SimulatorGatewayIntegrationTest.java` — 全链路集成测试

### 修改文件
- `gateway-simulator/src/main/java/com/codingas/simulator/service/SimulatorModeService.java` — 扩展枚举 + 新增方法
- `gateway-simulator/src/main/java/com/codingas/simulator/template/SimulatorResponseTemplates.java` — 新增错误模板
- `gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorController.java` — 新模式分发 + 延迟 + 流控制
- `gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorAdminController.java` — 新增管理端点
- `gateway-simulator/src/test/java/.../service/SimulatorModeServiceTest.java` — 新增模式测试
- `gateway-simulator/src/test/java/.../controller/SimulatorAdminControllerTest.java` — 新增端点测试
- `gateway-simulator/src/test/java/.../controller/SimulatorControllerTest.java` — 新模式分发测试
- `gateway-simulator/src/test/java/.../template/SimulatorResponseTemplatesTest.java` — 新增模板测试
- `gateway-simulator/src/test/java/.../SimulatorEndToEndTest.java` — 新增模式 E2E 测试
- `gateway-boot/pom.xml` — 添加 gateway-simulator test dependency

---

### Task 1: 扩展 SimulatorMode 枚举 + 新增响应模板

**Files:**
- Modify: `gateway-simulator/src/main/java/com/codingas/simulator/service/SimulatorModeService.java`
- Modify: `gateway-simulator/src/main/java/com/codingas/simulator/template/SimulatorResponseTemplates.java`

- [x] **Step 1: 扩展 SimulatorMode 枚举**

在 `SimulatorModeService.java` 中将 `SimulatorMode` 枚举从 3 个值扩展到 9 个值：

```java
public enum SimulatorMode {
    NORMAL,
    AUTH_ERROR,
    RATE_LIMITED,
    QUOTA_EXCEEDED,
    INVALID_REQUEST,
    UPSTREAM_ERROR,
    SERVICE_DOWN,
    TIMEOUT,
    INTERMITTENT
}
```

更新 `parseMode()` 方法支持新枚举值：
```java
private SimulatorMode parseMode(String modeConfig) {
    return switch (modeConfig.toLowerCase()) {
        case "rate_limited" -> SimulatorMode.RATE_LIMITED;
        case "fault", "upstream_error" -> SimulatorMode.UPSTREAM_ERROR;
        case "auth_error" -> SimulatorMode.AUTH_ERROR;
        case "quota_exceeded" -> SimulatorMode.QUOTA_EXCEEDED;
        case "invalid_request" -> SimulatorMode.INVALID_REQUEST;
        case "service_down" -> SimulatorMode.SERVICE_DOWN;
        case "timeout" -> SimulatorMode.TIMEOUT;
        case "intermittent" -> SimulatorMode.INTERMITTENT;
        default -> SimulatorMode.NORMAL;
    };
}
```

- [x] **Step 2: 新增 OpenAI 错误响应模板**

在 `SimulatorResponseTemplates.java` 中新增模板方法：

```java
// OpenAI 401
public static String openaiAuthError() {
    return """
            {
              "error": {
                "type": "authentication_error",
                "message": "Simulated authentication error"
              }
            }""";
}

// OpenAI 429 + quota
public static String openaiQuotaExceeded() {
    return """
            {
              "error": {
                "type": "insufficient_quota",
                "message": "Simulated quota exceeded error"
              }
            }""";
}

// OpenAI 400
public static String openaiInvalidRequest() {
    return """
            {
              "error": {
                "type": "invalid_request_error",
                "message": "Simulated invalid request error"
              }
            }""";
}

// OpenAI 503
public static String openaiServiceDown() {
    return """
            {
              "error": {
                "type": "service_unavailable",
                "message": "Simulated service unavailable error"
              }
            }""";
}

// OpenAI 408
public static String openaiTimeoutError() {
    return """
            {
              "error": {
                "type": "timeout",
                "message": "Simulated timeout error"
              }
            }""";
}
```

- [x] **Step 3: 新增 Anthropic 错误响应模板**

```java
// Anthropic 401
public static String anthropicAuthError() {
    return """
            {
              "error": {
                "type": "authentication_error",
                "message": "Simulated authentication error"
              }
            }""";
}

// Anthropic 429 + quota
public static String anthropicQuotaExceeded() {
    return """
            {
              "error": {
                "type": "insufficient_quota",
                "message": "Simulated quota exceeded error"
              }
            }""";
}

// Anthropic 400
public static String anthropicInvalidRequest() {
    return """
            {
              "error": {
                "type": "invalid_request_error",
                "message": "Simulated invalid request error"
              }
            }""";
}

// Anthropic 503
public static String anthropicServiceDown() {
    return """
            {
              "error": {
                "type": "service_unavailable",
                "message": "Simulated service unavailable error"
              }
            }""";
}

// Anthropic 408
public static String anthropicTimeoutError() {
    return """
            {
              "error": {
                "type": "timeout",
                "message": "Simulated timeout error"
              }
            }""";
}
```

- [x] **Step 4: 提交**

```bash
git add gateway-simulator/src/main/java/com/codingas/simulator/service/SimulatorModeService.java gateway-simulator/src/main/java/com/codingas/simulator/template/SimulatorResponseTemplates.java
git commit -m "feat(simulator): 扩展 SimulatorMode 枚举至 9 种并新增错误响应模板

- 新增 AUTH_ERROR / QUOTA_EXCEEDED / INVALID_REQUEST / UPSTREAM_ERROR / SERVICE_DOWN / TIMEOUT / INTERMITTENT
- 新增 OpenAI 和 Anthropic 各 5 种错误响应模板方法
- parseMode 支持所有新枚举值"
```

---

### Task 2: 实现 BehaviorSequence 机制

**Files:**
- Create: `gateway-simulator/src/main/java/com/codingas/simulator/service/BehaviorSequence.java`
- Modify: `gateway-simulator/src/main/java/com/codingas/simulator/service/SimulatorModeService.java`

- [x] **Step 1: 创建 BehaviorSequence 类**

```java
package com.codingas.simulator.service;

import java.util.List;
import java.util.Optional;

/**
 * 行为序列，按预定义 HTTP 状态码序列返回响应。
 * <p>
 * 支持一次性（消费完恢复全局模式）和循环（loop=true）两种模式。
 * 用于熔断器生命周期验证等场景。
 */
public class BehaviorSequence {

    private final List<Integer> steps;
    private final boolean loop;
    private int currentIndex;
    private boolean active;

    public BehaviorSequence(List<Integer> steps, boolean loop) {
        this.steps = List.copyOf(steps);
        this.loop = loop;
        this.currentIndex = 0;
        this.active = true;
    }

    /**
     * 消费当前步进，返回对应的 SimulatorMode。
     * 序列耗尽时：loop=true 重置索引，loop=false 标记 inactive 返回 empty。
     */
    public synchronized Optional<SimulatorMode> consume() {
        if (!active || steps.isEmpty()) {
            return Optional.empty();
        }
        int statusCode = steps.get(currentIndex);
        currentIndex++;
        if (currentIndex >= steps.size()) {
            if (loop) {
                currentIndex = 0;
            } else {
                active = false;
            }
        }
        return Optional.of(httpStatusToMode(statusCode));
    }

    /** 获取当前步进索引（用于状态查询） */
    public synchronized int getCurrentIndex() { return currentIndex; }

    /** 获取序列总步数 */
    public int size() { return steps.size(); }

    /** 是否活跃 */
    public synchronized boolean isActive() { return active; }

    /** 是否循环模式 */
    public boolean isLoop() { return loop; }

    /** 获取步骤副本 */
    public List<Integer> getSteps() { return List.copyOf(steps); }

    /** 重置序列 */
    public synchronized void reset() {
        this.currentIndex = 0;
        this.active = true;
    }

    /** HTTP 状态码 → SimulatorMode 映射 */
    private static SimulatorModeService.SimulatorMode httpStatusToMode(int statusCode) {
        return switch (statusCode) {
            case 200 -> SimulatorModeService.SimulatorMode.NORMAL;
            case 401 -> SimulatorModeService.SimulatorMode.AUTH_ERROR;
            case 429 -> SimulatorModeService.SimulatorMode.RATE_LIMITED;
            case 400 -> SimulatorModeService.SimulatorMode.INVALID_REQUEST;
            case 500 -> SimulatorModeService.SimulatorMode.UPSTREAM_ERROR;
            case 503 -> SimulatorModeService.SimulatorMode.SERVICE_DOWN;
            case 408 -> SimulatorModeService.SimulatorMode.TIMEOUT;
            default -> throw new IllegalArgumentException("不支持的状态码: " + statusCode);
        };
    }
}
```

- [x] **Step 2: 在 SimulatorModeService 中集成 BehaviorSequence**

新增字段和方法：

```java
// 新增字段
private BehaviorSequence behaviorSequence;

// 设置行为序列
public synchronized void setBehaviorSequence(List<Integer> steps, boolean loop) {
    this.behaviorSequence = new BehaviorSequence(steps, loop);
}

// 获取当前行为序列
public synchronized BehaviorSequence getBehaviorSequence() {
    return behaviorSequence;
}

// 清除行为序列
public synchronized void clearBehaviorSequence() {
    this.behaviorSequence = null;
}
```

- [x] **Step 3: 提交**

```bash
git add gateway-simulator/src/main/java/com/codingas/simulator/service/BehaviorSequence.java gateway-simulator/src/main/java/com/codingas/simulator/service/SimulatorModeService.java
git commit -m "feat(simulator): 实现 BehaviorSequence 行为序列机制

- 新增 BehaviorSequence 类，支持一次性/循环两种模式
- HTTP 状态码自动映射为 SimulatorMode
- SimulatorModeService 集成行为序列的 set/get/clear"
```

---

### Task 3: 实现延迟配置 + 流控制 + API Key 覆盖配置类

**Files:**
- Create: `gateway-simulator/src/main/java/com/codingas/simulator/service/DelayConfig.java`
- Create: `gateway-simulator/src/main/java/com/codingas/simulator/service/StreamConfig.java`
- Create: `gateway-simulator/src/main/java/com/codingas/simulator/service/ApiKeyOverrideConfig.java`
- Modify: `gateway-simulator/src/main/java/com/codingas/simulator/service/SimulatorModeService.java`

- [x] **Step 1: 创建 DelayConfig**

```java
package com.codingas.simulator.service;

/**
 * 延迟配置，控制模拟器响应延迟。
 * <p>
 * 独立于模式的正交配置，可与任何模式组合使用。
 */
public class DelayConfig {

    private long fixedDelayMs;
    private boolean active;

    public DelayConfig() {
        this.fixedDelayMs = 0;
        this.active = false;
    }

    public synchronized void setDelay(long delayMs) {
        this.fixedDelayMs = delayMs;
        this.active = delayMs > 0;
    }

    public synchronized void clearDelay() {
        this.fixedDelayMs = 0;
        this.active = false;
    }

    public synchronized long getFixedDelayMs() { return fixedDelayMs; }
    public synchronized boolean isActive() { return active; }

    /** 应用延迟，当前线程 sleep */
    public synchronized void applyDelay() {
        if (active && fixedDelayMs > 0) {
            try {
                Thread.sleep(fixedDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

- [x] **Step 2: 创建 StreamConfig**

```java
package com.codingas.simulator.service;

/**
 * 流控制配置，控制 SSE 流式响应的行为。
 */
public class StreamConfig {

    private String action = "normal";    // normal / interrupt_after / invalid_data / duplicate_done / empty_chunk / incomplete
    private int chunkCount = 3;          // chunk 数量（默认 3）
    private int chunkIntervalMs = 50;    // chunk 间隔毫秒（默认 50）
    private int interruptAfter = 0;      // 中断前发送的 chunk 数
    private String invalidChunk = "";    // 非法数据内容

    public void configure(String action, int chunkCount, int chunkIntervalMs,
                          int interruptAfter, String invalidChunk) {
        this.action = action != null ? action : "normal";
        this.chunkCount = chunkCount > 0 ? chunkCount : 3;
        this.chunkIntervalMs = chunkIntervalMs > 0 ? chunkIntervalMs : 50;
        this.interruptAfter = interruptAfter;
        this.invalidChunk = invalidChunk != null ? invalidChunk : "";
    }

    public void reset() {
        this.action = "normal";
        this.chunkCount = 3;
        this.chunkIntervalMs = 50;
        this.interruptAfter = 0;
        this.invalidChunk = "";
    }

    // getters
    public String getAction() { return action; }
    public int getChunkCount() { return chunkCount; }
    public int getChunkIntervalMs() { return chunkIntervalMs; }
    public int getInterruptAfter() { return interruptAfter; }
    public String getInvalidChunk() { return invalidChunk; }
}
```

- [x] **Step 3: 创建 ApiKeyOverrideConfig**

```java
package com.codingas.simulator.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API Key 覆盖配置，根据请求的 Authorization Header 返回不同响应。
 * <p>
 * 匹配规则：检查 API Key 是否以 keyPrefix 开头。
 * 用于 Key 故障转移验证场景。
 */
public class ApiKeyOverrideConfig {

    private final Map<String, SimulatorModeService.SimulatorMode> overrides = new ConcurrentHashMap<>();

    public void setOverride(String keyPrefix, SimulatorModeService.SimulatorMode mode) {
        overrides.put(keyPrefix, mode);
    }

    public void removeOverride(String keyPrefix) {
        overrides.remove(keyPrefix);
    }

    public void clearAll() {
        overrides.clear();
    }

    public Map<String, SimulatorModeService.SimulatorMode> getOverrides() {
        return Map.copyOf(overrides);
    }

    /**
     * 匹配给定 API Key 的覆盖模式。
     * 遍历所有前缀规则，返回第一个匹配的覆盖模式。
     */
    public Optional<SimulatorModeService.SimulatorMode> matchOverride(String apiKey) {
        if (apiKey == null) return Optional.empty();
        for (Map.Entry<String, SimulatorModeService.SimulatorMode> entry : overrides.entrySet()) {
            if (apiKey.startsWith(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
```

- [x] **Step 4: 在 SimulatorModeService 中集成新增配置类**

```java
// 新增字段
private final DelayConfig delayConfig = new DelayConfig();
private final StreamConfig streamConfig = new StreamConfig();
private final ApiKeyOverrideConfig apiKeyOverrideConfig = new ApiKeyOverrideConfig();

// getters
public DelayConfig getDelayConfig() { return delayConfig; }
public StreamConfig getStreamConfig() { return streamConfig; }
public ApiKeyOverrideConfig getApiKeyOverrideConfig() { return apiKeyOverrideConfig; }
```

- [x] **Step 5: 提交**

```bash
git add gateway-simulator/src/main/java/com/codingas/simulator/service/DelayConfig.java gateway-simulator/src/main/java/com/codingas/simulator/service/StreamConfig.java gateway-simulator/src/main/java/com/codingas/simulator/service/ApiKeyOverrideConfig.java gateway-simulator/src/main/java/com/codingas/simulator/service/SimulatorModeService.java
git commit -m "feat(simulator): 实现延迟配置、流控制和 API Key 覆盖

- DelayConfig: 独立于模式的正交延迟配置
- StreamConfig: 可配置 chunk 数/间隔/中断/非法数据等流行为
- ApiKeyOverrideConfig: 前缀匹配的 API Key 覆盖规则"
```

---

### Task 4: 实现 SimulatorController 新模式分发 + 延迟 + 流控制

**Files:**
- Modify: `gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorController.java`

- [x] **Step 1: 重构 SimulatorController 响应逻辑**

核心变更：
1. 新增 `resolveMode()` 方法实现行为序列 > API Key 覆盖 > 全局模式的优先级
2. 在错误模式分支应用延迟
3. 流式响应支持 streamConfig 控制

新增 `resolveMode` 方法：

```java
private SimulatorMode resolveMode(String authHeader) {
    // 1. 行为序列优先
    BehaviorSequence seq = modeService.getBehaviorSequence();
    if (seq != null && seq.isActive()) {
        Optional<SimulatorMode> seqMode = seq.consume();
        if (seqMode.isPresent()) {
            return seqMode.get();
        }
    }
    // 2. API Key 覆盖
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String apiKey = authHeader.substring(7);
        Optional<SimulatorMode> overrideMode = modeService.getApiKeyOverrideConfig().matchOverride(apiKey);
        if (overrideMode.isPresent()) {
            return overrideMode.get();
        }
    }
    // 3. 全局模式
    SimulatorMode globalMode = modeService.getMode();
    if (globalMode == SimulatorMode.INTERMITTENT) {
        return SimulatorMode.NORMAL; // 无活跃序列时回退
    }
    return globalMode;
}
```

重构 `openaiChatCompletions` 方法：

```java
@PostMapping("/v1/chat/completions")
public ResponseEntity<?> openaiChatCompletions(@RequestBody String body,
                                                @RequestHeader("Authorization") String authHeader) {
    modeService.recordRequest("POST", "/v1/chat/completions");
    SimulatorMode mode = resolveMode(authHeader);
    modeService.getDelayConfig().applyDelay();

    return switch (mode) {
        case RATE_LIMITED -> ResponseEntity.status(429)
                .contentType(MediaType.APPLICATION_JSON)
                .body(SimulatorResponseTemplates.openaiRateLimitError());
        case UPSTREAM_ERROR -> ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(SimulatorResponseTemplates.openaiServerError());
        case AUTH_ERROR -> ResponseEntity.status(401)
                .contentType(MediaType.APPLICATION_JSON)
                .body(SimulatorResponseTemplates.openaiAuthError());
        case QUOTA_EXCEEDED -> ResponseEntity.status(429)
                .contentType(MediaType.APPLICATION_JSON)
                .body(SimulatorResponseTemplates.openaiQuotaExceeded());
        case INVALID_REQUEST -> ResponseEntity.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(SimulatorResponseTemplates.openaiInvalidRequest());
        case SERVICE_DOWN -> ResponseEntity.status(503)
                .contentType(MediaType.APPLICATION_JSON)
                .body(SimulatorResponseTemplates.openaiServiceDown());
        case TIMEOUT -> ResponseEntity.status(408)
                .contentType(MediaType.APPLICATION_JSON)
                .body(SimulatorResponseTemplates.openaiTimeoutError());
        case NORMAL -> handleOpenAINormal(body);
        default -> handleOpenAINormal(body);
    };
}
```

同理重构 `anthropicMessages` 方法。

- [x] **Step 2: 重写流式发送方法支持 StreamConfig**

重构 `sendOpenAIStream` 方法：

```java
private void sendOpenAIStream(SseEmitter emitter) {
    StreamConfig config = modeService.getStreamConfig();
    try {
        switch (config.getAction()) {
            case "interrupt_after" -> {
                for (int i = 0; i < config.getInterruptAfter(); i++) {
                    emitter.send(SseEmitter.event()
                            .data(SimulatorResponseTemplates.openaiStreamChunk("chunk " + i)));
                    Thread.sleep(config.getChunkIntervalMs());
                }
                // 不 complete，模拟中断
            }
            case "invalid_data" -> {
                emitter.send(SseEmitter.event().data(config.getInvalidChunk()));
                emitter.complete();
            }
            case "duplicate_done" -> {
                String[] contents = {"Hello", " from", " simulator"};
                for (String content : contents) {
                    emitter.send(SseEmitter.event()
                            .data(SimulatorResponseTemplates.openaiStreamChunk(content)));
                    Thread.sleep(config.getChunkIntervalMs());
                }
                emitter.send(SseEmitter.event().data(SimulatorResponseTemplates.openaiStreamDone()));
                emitter.send(SseEmitter.event().data(SimulatorResponseTemplates.openaiStreamDone()));
                emitter.complete();
            }
            case "empty_chunk" -> {
                emitter.send(SseEmitter.event().data("data: \n\n"));
                emitter.send(SseEmitter.event().data(SimulatorResponseTemplates.openaiStreamDone()));
                emitter.complete();
            }
            case "incomplete" -> {
                String[] contents = {"Hello", " from", " simulator"};
                for (String content : contents) {
                    emitter.send(SseEmitter.event()
                            .data(SimulatorResponseTemplates.openaiStreamChunk(content)));
                    Thread.sleep(config.getChunkIntervalMs());
                }
                // 不发送 [DONE]，直接 complete
                emitter.complete();
            }
            default -> { // normal
                for (int i = 0; i < config.getChunkCount(); i++) {
                    emitter.send(SseEmitter.event()
                            .data(SimulatorResponseTemplates.openaiStreamChunk("chunk " + i)));
                    Thread.sleep(config.getChunkIntervalMs());
                }
                emitter.send(SseEmitter.event()
                        .data(SimulatorResponseTemplates.openaiStreamDone()));
                emitter.complete();
            }
        }
    } catch (IOException | InterruptedException e) {
        emitter.completeWithError(e);
    }
}
```

同理重构 `sendAnthropicStream` 方法。

- [x] **Step 3: 提交**

```bash
git add gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorController.java
git commit -m "feat(simulator): 重构控制器支持新模式分发和流控制

- resolveMode 实现行为序列 > API Key 覆盖 > 全局模式优先级
- 新增 AUTH_ERROR/QUOTA_EXCEEDED/INVALID_REQUEST/SERVICE_DOWN/TIMEOUT 模式
- 流式响应支持 StreamConfig（中断/非法数据/重复DONE等）"
```

---

### Task 5: 实现管理 API 端点

**Files:**
- Modify: `gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorAdminController.java`

- [x] **Step 1: 新增行为序列管理端点**

```java
@PostMapping("/behavior")
public ResponseEntity<Map<String, Object>> setBehavior(@RequestBody Map<String, Object> body) {
    @SuppressWarnings("unchecked")
    List<Integer> sequence = ((List<Integer>) body.get("sequence"));
    boolean loop = body.containsKey("loop") && (boolean) body.get("loop");
    if (sequence == null || sequence.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "sequence 不能为空"));
    }
    modeService.setBehaviorSequence(sequence, loop);
    return ResponseEntity.ok(Map.of(
            "active", true,
            "steps", sequence,
            "loop", loop,
            "currentIndex", 0
    ));
}

@GetMapping("/behavior")
public ResponseEntity<Map<String, Object>> getBehavior() {
    BehaviorSequence seq = modeService.getBehaviorSequence();
    if (seq == null || !seq.isActive()) {
        return ResponseEntity.ok(Map.of("active", false));
    }
    return ResponseEntity.ok(Map.of(
            "active", true,
            "steps", seq.getSteps(),
            "loop", seq.isLoop(),
            "currentIndex", seq.getCurrentIndex()
    ));
}

@DeleteMapping("/behavior")
public ResponseEntity<Map<String, String>> clearBehavior() {
    modeService.clearBehaviorSequence();
    return ResponseEntity.ok(Map.of("status", "cleared"));
}
```

- [x] **Step 2: 新增延迟管理端点**

```java
@PostMapping("/delay")
public ResponseEntity<Map<String, Object>> setDelay(@RequestBody Map<String, Object> body) {
    Object delayObj = body.get("delayMs");
    if (delayObj == null) {
        return ResponseEntity.badRequest().body(Map.of("error", "缺少 delayMs 参数"));
    }
    long delayMs = delayObj instanceof Number ? ((Number) delayObj).longValue() : Long.parseLong(delayObj.toString());
    modeService.getDelayConfig().setDelay(delayMs);
    return ResponseEntity.ok(Map.of("delayMs", delayMs, "active", true));
}

@DeleteMapping("/delay")
public ResponseEntity<Map<String, String>> clearDelay() {
    modeService.getDelayConfig().clearDelay();
    return ResponseEntity.ok(Map.of("status", "cleared"));
}

@GetMapping("/delay")
public ResponseEntity<Map<String, Object>> getDelay() {
    return ResponseEntity.ok(Map.of(
            "delayMs", modeService.getDelayConfig().getFixedDelayMs(),
            "active", modeService.getDelayConfig().isActive()
    ));
}
```

- [x] **Step 3: 新增流控制端点**

```java
@PostMapping("/stream")
public ResponseEntity<Map<String, Object>> setStreamConfig(@RequestBody Map<String, Object> body) {
    String action = (String) body.getOrDefault("action", "normal");
    int chunkCount = body.containsKey("chunkCount") ? ((Number) body.get("chunkCount")).intValue() : 3;
    int chunkIntervalMs = body.containsKey("chunkIntervalMs") ? ((Number) body.get("chunkIntervalMs")).intValue() : 50;
    int interruptAfter = body.containsKey("chunks") ? ((Number) body.get("chunks")).intValue() : 0;
    String invalidChunk = (String) body.getOrDefault("invalidChunk", "");
    modeService.getStreamConfig().configure(action, chunkCount, chunkIntervalMs, interruptAfter, invalidChunk);
    return ResponseEntity.ok(Map.of("action", action, "configured", true));
}
```

- [x] **Step 4: 新增 API Key 覆盖端点**

```java
@PostMapping("/apikey-override")
public ResponseEntity<Map<String, Object>> setApiKeyOverride(@RequestBody Map<String, Object> body) {
    String keyPrefix = (String) body.get("keyPrefix");
    String modeStr = (String) body.get("mode");
    if (keyPrefix == null || modeStr == null) {
        return ResponseEntity.badRequest().body(Map.of("error", "缺少 keyPrefix 或 mode 参数"));
    }
    SimulatorModeService.SimulatorMode mode = parseMode(modeStr);
    modeService.getApiKeyOverrideConfig().setOverride(keyPrefix, mode);
    return ResponseEntity.ok(Map.of("keyPrefix", keyPrefix, "mode", mode.name()));
}

@DeleteMapping("/apikey-override/{keyPrefix}")
public ResponseEntity<Map<String, String>> removeApiKeyOverride(@PathVariable String keyPrefix) {
    modeService.getApiKeyOverrideConfig().removeOverride(keyPrefix);
    return ResponseEntity.ok(Map.of("status", "removed"));
}

@DeleteMapping("/apikey-override")
public ResponseEntity<Map<String, String>> clearApiKeyOverrides() {
    modeService.getApiKeyOverrideConfig().clearAll();
    return ResponseEntity.ok(Map.of("status", "cleared"));
}

@GetMapping("/apikey-override")
public ResponseEntity<Map<String, String>> getApiKeyOverrides() {
    Map<String, String> result = new java.util.HashMap<>();
    modeService.getApiKeyOverrideConfig().getOverrides()
            .forEach((key, val) -> result.put(key, val.name()));
    return ResponseEntity.ok(result);
}
```

- [x] **Step 5: 更新 parseMode 支持新枚举值**

```java
private SimulatorModeService.SimulatorMode parseMode(String modeStr) {
    return switch (modeStr.toLowerCase()) {
        case "normal" -> SimulatorModeService.SimulatorMode.NORMAL;
        case "rate_limited" -> SimulatorModeService.SimulatorMode.RATE_LIMITED;
        case "fault", "upstream_error" -> SimulatorModeService.SimulatorMode.UPSTREAM_ERROR;
        case "auth_error" -> SimulatorModeService.SimulatorMode.AUTH_ERROR;
        case "quota_exceeded" -> SimulatorModeService.SimulatorMode.QUOTA_EXCEEDED;
        case "invalid_request" -> SimulatorModeService.SimulatorMode.INVALID_REQUEST;
        case "service_down" -> SimulatorModeService.SimulatorMode.SERVICE_DOWN;
        case "timeout" -> SimulatorModeService.SimulatorMode.TIMEOUT;
        case "intermittent" -> SimulatorModeService.SimulatorMode.INTERMITTENT;
        default -> throw new IllegalArgumentException("不支持的模式: " + modeStr);
    };
}
```

- [x] **Step 6: 提交**

```bash
git add gateway-simulator/src/main/java/com/codingas/simulator/controller/SimulatorAdminController.java
git commit -m "feat(simulator): 实现管理 API 端点（behavior/delay/stream/apikey-override）

- 新增 POST/GET/DELETE /simulator/behavior 行为序列管理
- 新增 POST/DELETE/GET /simulator/delay 延迟管理
- 新增 POST /simulator/stream 流控制配置
- 新增 POST/DELETE/GET /simulator/apikey-override API Key 覆盖管理
- parseMode 支持所有新枚举值"
```

---

### Task 6: 编写 Simulator 单元测试

**Files:**
- Modify: `gateway-simulator/src/test/java/com/codingas/simulator/service/SimulatorModeServiceTest.java`
- Modify: `gateway-simulator/src/test/java/com/codingas/simulator/template/SimulatorResponseTemplatesTest.java`
- Modify: `gateway-simulator/src/test/java/com/codingas/simulator/controller/SimulatorAdminControllerTest.java`
- Modify: `gateway-simulator/src/test/java/com/codingas/simulator/controller/SimulatorControllerTest.java`
- Modify: `gateway-simulator/src/test/java/com/codingas/simulator/SimulatorEndToEndTest.java`

- [x] **Step 1: 更新 SimulatorModeServiceTest**

新增测试：新模式枚举值测试、parseMode 测试（包括新枚举值）、BehaviorSequence 集成测试、DelayConfig 测试、ApiKeyOverrideConfig 测试。

关键测试用例：
- `parseMode_parsesAuthError()`
- `parseMode_parsesQuotaExceeded()`
- `parseMode_parsesInvalidRequest()`
- `parseMode_parsesServiceDown()`
- `parseMode_parsesTimeout()`
- `parseMode_parsesIntermittent()`
- `setBehaviorSequence_consumesSteps()`
- `behaviorSequence_loop_resetsOnEnd()`
- `behaviorSequence_nonLoop_deactivatesOnEnd()`
- `delayConfig_applyDelay_sleeps()`
- `apiKeyOverride_matchOverride_returnsCorrectMode()`
- `apiKeyOverride_noMatch_returnsEmpty()`

- [x] **Step 2: 更新 SimulatorResponseTemplatesTest**

新增测试：所有新增模板方法的响应体验证。

关键测试用例：
- `openaiAuthError_containsAuthError()`
- `openaiQuotaExceeded_containsInsufficientQuota()`
- `openaiInvalidRequest_containsInvalidRequest()`
- `openaiServiceDown_containsServiceUnavailable()`
- `openaiTimeoutError_containsTimeout()`
- 同上对应 Anthropic 的 5 个测试

- [x] **Step 3: 更新 SimulatorAdminControllerTest**

新增测试：behavior/delay/stream/apikey-override 管理端点的正确性验证。

- [x] **Step 4: 更新 SimulatorControllerTest**

新增测试：AUTH_ERROR/QUOTA_EXCEEDED/INVALID_REQUEST/UPSTREAM_ERROR/SERVICE_DOWN/TIMEOUT 模式的状态码和响应体验证。

- [x] **Step 5: 更新 SimulatorEndToEndTest**

新增端到端测试：新模式 E2E、行为序列 E2E、延迟 E2E、API Key 覆盖 E2E。

- [x] **Step 6: 提交**

```bash
git add gateway-simulator/src/test/
git commit -m "test(simulator): 新增 Simulator 增强的单元测试和 E2E 测试

- 覆盖所有 6 种新增错误模式
- 覆盖 BehaviorSequence 一次性/循环模式
- 覆盖 DelayConfig 延迟应用
- 覆盖 ApiKeyOverrideConfig 匹配逻辑
- 覆盖 Admin API 新端点"
```

---

### Task 7: Gateway 集成测试 — 依赖配置 + 基础框架

**Files:**
- Modify: `gateway-boot/pom.xml`
- Create: `gateway-boot/src/test/java/com/codingas/gateway/integration/SimulatorGatewayIntegrationTest.java`

- [x] **Step 1: gateway-boot pom.xml 添加 gateway-simulator 依赖**

```xml
<dependency>
    <groupId>com.codingas.gateway</groupId>
    <artifactId>gateway-simulator</artifactId>
    <version>${revision}</version>
    <scope>test</scope>
</dependency>
```

- [x] **Step 2: 创建集成测试基类**

```java
package com.codingas.gateway.integration;

import com.codingas.gateway.GatewayApplication;
import com.codingas.simulator.LLMProviderSimulatorApplication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/**
 * Gateway 全链路集成测试基类。
 * <p>
 * 同时启动 Gateway 和 Simulator 两个 Spring Context，
 * Gateway 的 Provider endpoint 指向 Simulator。
 * 通过 TestRestTemplate 调用 Gateway API，Gateway 转发请求到 Simulator。
 */
@SpringBootTest(
    classes = {GatewayApplication.class, LLMProviderSimulatorApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "simulator.mode=normal",
        "server.port=0",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "gateway.http-client.connect-timeout=2s",
        "gateway.http-client.read-timeout=2s",
        "logging.level.com.codingas.gateway=DEBUG"
    }
)
@TestPropertySource(locations = "classpath:application-test.yml")
public abstract class SimulatorGatewayIntegrationTestBase {

    @Autowired
    protected TestRestTemplate restTemplate;

    @LocalServerPort
    protected int gatewayPort;

    protected String gatewayUrl;

    @BeforeEach
    void setUp() {
        gatewayUrl = "http://localhost:" + gatewayPort;
    }

    protected HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }
}
```

- [x] **Step 3: 提交**

```bash
git add gateway-boot/pom.xml gateway-boot/src/test/java/com/codingas/gateway/integration/SimulatorGatewayIntegrationTest.java
git commit -m "feat(test): 添加 gateway-simulator 依赖和集成测试基类

- gateway-boot pom.xml 添加 gateway-simulator test scope 依赖
- 创建 SimulatorGatewayIntegrationTestBase 基类
- 配置 Gateway 使用内存数据库，禁用 Flyway"
```

---

### Task 8: 实现 Gateway 集成测试 — 正常路径 + 异常场景

**Files:**
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/integration/SimulatorGatewayIntegrationTest.java`

- [x] **Step 1: 实现正常路径和异常场景测试**

```java
package com.codingas.gateway.integration;

import com.codingas.gateway.adapter.protocol.openai.dto.OpenAiChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatorGatewayIntegrationTest extends SimulatorGatewayIntegrationTestBase {

    private static final String CHAT_ENDPOINT = "/v1/chat/completions";
    private static final String SIMULATOR_ADMIN = "http://localhost:{simPort}";

    // ===== 正常路径 =====

    @Test
    void testNormalChat() {
        HttpEntity<String> request = new HttpEntity<>(
            "{\"model\":\"gpt-4\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}",
            createHeaders("sk-test-key")
        );
        ResponseEntity<OpenAiChatResponse> response = restTemplate.exchange(
            CHAT_ENDPOINT, HttpMethod.POST, request, OpenAiChatResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getChoices()).isNotEmpty();
    }

    // ... 更多测试方法
}
```

注意：由于 Simulator 端口是随机的，需要在测试中获取 Simulator 的端口并配置 Gateway 的 Provider endpoint。可以在 `@BeforeEach` 中通过 `TestRestTemplate` 调用 Simulator 的 admin 端点获取端口。

- [x] **Step 2: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/integration/
git commit -m "test(gateway): 实现集成测试 — 正常路径和异常场景

- testNormalChat/testNormalStream 正常路径
- testRateLimit_retried/testAuthError_notRetried/testUpstreamError_retried 异常场景"
```

---

### Task 9: 实现 Gateway 集成测试 — 熔断器 + Key 故障转移 + 降级

**Files:**
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/integration/SimulatorGatewayIntegrationTest.java`

- [x] **Step 1: 实现熔断器生命周期测试**

```java
@Test
void testCircuitBreaker_open() {
    // 配置 Simulator 行为序列：10 次 500
    setSimulatorBehavior(List.of(500, 500, 500, 500, 500, 500, 500, 500, 500, 500), false);

    HttpEntity<String> request = new HttpEntity<>(
        "{\"model\":\"gpt-4\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}",
        createHeaders("sk-test-key")
    );

    // 前 10 次请求应该返回 500（或重试后抛出异常）
    for (int i = 0; i < 10; i++) {
        restTemplate.exchange(CHAT_ENDPOINT, HttpMethod.POST, request, String.class);
    }

    // 第 11 次请求应该被熔断器拒绝
    ResponseEntity<String> response = restTemplate.exchange(
        CHAT_ENDPOINT, HttpMethod.POST, request, String.class);
    // 预期：Gateway 返回 503 或 500（CircuitOpenException 被上层处理）
    assertThat(response.getStatusCode().is5xxServerError()).isTrue();
}
```

- [x] **Step 2: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/integration/
git commit -m "test(gateway): 实现集成测试 — 熔断器/Key故障转移/降级

- testCircuitBreaker_open/testCircuitBreaker_halfOpen 熔断器生命周期
- testKeyFailover 多 Key 自动切换
- testDegradation 模型降级"
```

---

### Task 10: 实现 Gateway 集成测试 — 跨协议 + 超时 + 流中断

**Files:**
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/integration/SimulatorGatewayIntegrationTest.java`

- [x] **Step 1: 实现跨协议转换、超时和流中断测试

```java
@Test
void testTimeout() {
    // 配置 Simulator 延迟 5s
    setSimulatorDelay(5000);

    HttpEntity<String> request = new HttpEntity<>(
        "{\"model\":\"gpt-4\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}",
        createHeaders("sk-test-key")
    );

    // Gateway 超时设为 2s，Simulator 延迟 5s → 超时
    ResponseEntity<String> response = restTemplate.exchange(
        CHAT_ENDPOINT, HttpMethod.POST, request, String.class);

    assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    // 响应体应包含超时相关错误信息
    assertThat(response.getBody()).containsIgnoringCase("timeout");
}

@Test
void testStreamInterrupted() {
    // 配置 Simulator 流中断（发送 2 个 chunk 后关闭）
    setSimulatorStream("interrupt_after", 2, 50);

    HttpHeaders headers = createHeaders("sk-test-key");
    headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM));
    HttpEntity<String> request = new HttpEntity<>(
        "{\"model\":\"gpt-4\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"stream\":true}",
        headers
    );

    ResponseEntity<String> response = restTemplate.exchange(
        CHAT_ENDPOINT, HttpMethod.POST, request, String.class);

    // 流中断后 Gateway 应返回错误
    assertThat(response.getStatusCode().is5xxServerError()).isTrue();
}
```

- [x] **Step 2: 提交**

```bash
git add gateway-boot/src/test/java/com/codingas/gateway/integration/
git commit -m "test(gateway): 实现集成测试 — 跨协议/超时/流中断

- testProtocolConversion 跨协议转换
- testTimeout 超时场景
- testStreamInterrupted 流中断场景"
```

---

### Task 11: 验证全部测试通过

**Files:**
- Modify: `openspec/changes/simulator-verification-enhancement/tasks.md`

- [x] **Step 1: 运行 Simulator 全部测试**

```bash
cd gateway-simulator && ../mvnw test
```

预期：全部测试通过（原有 28 个测试 + 新增约 20 个测试 ≈ 48 个测试全部通过）

- [x] **Step 2: 运行 Gateway-boot 测试**

```bash
cd gateway-boot && ../mvnw test -DskipTests=false
```

预期：全部测试通过

- [x] **Step 3: 运行全量构建**

```bash
./mvnw clean install -DskipTests=false
```

预期：构建成功，全部测试通过

- [x] **Step 4: 更新 tasks.md 勾选完成的任务**

将 `openspec/changes/simulator-verification-enhancement/tasks.md` 中所有 `- [x]` 改为 `- [x]`

- [x] **Step 5: 提交**

```bash
git add .
git commit -m "chore: 验证全部测试通过并更新 tasks.md"
```
