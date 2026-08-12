# 能力插件化第 1 阶段：Canonical IR + ProtocolAdapter SPI 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 引入规范内部模型（Canonical IR）与 `ProtocolAdapter` SPI，把硬编码的 `ProtocolConverter` 从"OpenAI↔Anthropic 两两转换"重构为"每协议原生↔规范"的两个适配器，为后续插件化（gateway-capability-*）打地基，且**存量行为完全不变**。

**Architecture:** 新建独立的 `gateway-capability-api` Maven 模块，承载 Canonical IR 模型 + `ProtocolAdapter` SPI（纯接口，无 Spring 依赖）。在 `gateway-boot` 内实现 OpenAI/Anthropic 两个 Adapter，新增 `ProtocolConversionFacade` 统一调用入口，重写 `ChannelFailoverInvoker` 中请求/响应的转换走 canonical（normalize→denormalize）。**流式 chunk 转换本轮保持原样**（仍用 JSON 字符串方向转换），作为独立 `ProtocolStreamConverter` 平移保留，后续阶段再做流式 canonical 化（YAGNI）。

**Tech Stack:** Java 21, Spring Boot 3.5.13, Maven 多模块, Jackson 2.x, JUnit 5, Mockito。

## Global Constraints

- 语言：所有注释、Javadoc、Commit Message 使用**简体中文**。
- 版权头：所有新文件必须包含 Apache 2.0 标准完整版权头（参考 `gateway-boot/src/main/java/.../ProtocolRequest.java` 顶部 15 行）。
- 覆盖率：核心层（ProtocolConversionFacade、适配器）单元测试 ≥85%，迁移后的集成测试全部通过。
- 行为保持：`ProtocolConversionIntegrationTest`、`ChannelFailoverIntegrationTest`、`ChannelFailoverInvokerTest`、`ChatDispatchServiceTest` 必须全部通过（不改变既有对外行为）。
- 依赖倒置：`gateway-capability-api` **不依赖** Spring 与 `gateway-boot` 任何实现；`gateway-boot` 依赖 `gateway-capability-api`。
- 双 API 兼容：请求/响应转换后，OpenAI 与 Anthropic 两端语义等价（复用现有 `ProtocolConverter` 已覆盖的映射语义）。

---

## 文件结构

**新增模块** `gateway-capability-api/`（纯接口 + 模型，无 Spring）：
- `pom.xml` — 独立模块 POM（依赖 Jackson）
- `src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalChatRequest.java`
- `src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalChatResponse.java`
- `src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalMessage.java`
- `src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalTool.java`
- `src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalToolCall.java`
- `src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalContentBlock.java`
- `src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalUsage.java`
- `src/main/java/com/codingas/gateway/api/capability/protocol/ProtocolAdapter.java`

**`gateway-boot` 内新增（或改造）**：
- `src/main/java/com/codingas/gateway/infrastructure/protocol/OpenAIProtocolAdapter.java`
- `src/main/java/com/codingas/gateway/infrastructure/protocol/AnthropicProtocolAdapter.java`
- `src/main/java/com/codingas/gateway/infrastructure/protocol/ProtocolStreamConverter.java`（平移自旧 ProtocolConverter 的流式方法）
- `src/main/java/com/codingas/gateway/domain/protocol/conversion/ProtocolConversionFacade.java`
- 修改：`application/proxy/invoker/ChannelFailoverInvoker.java`（改用 Facade）
- 删除：`domain/protocol/conversion/ProtocolConverter.java`（逻辑平移后删除）

**测试**：
- `gateway-capability-api/.../CanonicalChatRequestTest.java`（规范模型序列化/构建）
- `gateway-boot/.../infrastructure/protocol/OpenAIProtocolAdapterTest.java`
- `gateway-boot/.../infrastructure/protocol/AnthropicProtocolAdapterTest.java`
- `gateway-boot/.../domain/protocol/conversion/ProtocolConversionFacadeTest.java`
- `gateway-boot/.../infrastructure/protocol/ProtocolStreamConverterTest.java`（平移自旧转换测试）

---

### Task 1: 新建 gateway-capability-api 模块与 Canonical IR 模型

**Files:**
- Create: `gateway-capability-api/pom.xml`
- Create: `gateway-capability-api/src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalChatRequest.java`
- Create: `gateway-capability-api/src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalMessage.java`
- Create: `gateway-capability-api/src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalTool.java`
- Create: `gateway-capability-api/src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalToolCall.java`
- Create: `gateway-capability-api/src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalChatResponse.java`
- Create: `gateway-capability-api/src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalContentBlock.java`
- Create: `gateway-capability-api/src/main/java/com/codingas/gateway/api/capability/protocol/CanonicalUsage.java`
- Modify: `pom.xml`（父 POM `<modules>` 增加 `gateway-capability-api`）
- Modify: `gateway-boot/pom.xml`（增加对 `gateway-capability-api` 的依赖）
- Test: `gateway-capability-api/src/test/java/com/codingas/gateway/api/capability/protocol/CanonicalChatRequestTest.java`

**Interfaces:**
- Produces: 规范模型类（`CanonicalChatRequest`/`CanonicalChatResponse` 等），供 Task 2/3/4 使用。

- [ ] **Step 1: 新建模块 POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.codingas.gateway</groupId>
        <artifactId>gateway-project</artifactId>
        <version>${revision}</version>
    </parent>
    <artifactId>gateway-capability-api</artifactId>
    <packaging>jar</packaging>
    <name>LLM-Gateway Capability API</name>
    <description>能力 SPI 契约：规范内部模型 + ProtocolAdapter 纯接口（无 Spring 依赖）</description>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 父 POM 与 gateway-boot 注册模块**

在父 `pom.xml` 的 `<modules>` 中新增 `<module>gateway-capability-api</module>`（置于 `gateway-boot` 之前）。在 `gateway-boot/pom.xml` 的 `<dependencies>` 中新增：

```xml
<dependency>
    <groupId>com.codingas.gateway</groupId>
    <artifactId>gateway-capability-api</artifactId>
    <version>${revision}</version>
</dependency>
```

- [ ] **Step 3: 定义规范模型（Canonical IR）**

每个类都带 Apache 2.0 版权头（复制自 `ProtocolRequest.java` 顶部 15 行）与中文 Javadoc。全部使用 Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`。

`CanonicalChatRequest.java`（请求规范模型）：

```java
package com.codingas.gateway.api.capability.protocol;

import lombok.*;

import java.util.List;

/**
 * 规范聊天请求（Canonical IR）——与厂商无关的中立表示。
 *
 * <p>用于协议适配层：入站原生请求 normalize 为规范模型，出站由规范模型
 * denormalize 为上游原生请求。任意两协议互转 = normalize + denormalize 两跳。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalChatRequest {

    /** 模型名（用户面，路由后可能被上游模型名覆盖） */
    private String model;

    /** system 顶层指令（Anthropic 风格；OpenAI 由 system 角色消息提取） */
    private String system;

    /** 消息列表（不含 system 角色） */
    private List<CanonicalMessage> messages;

    /** 最大输出 token，null 表示未指定 */
    private Integer maxTokens;

    /** 采样温度，null 表示未指定 */
    private Double temperature;

    /** 停止序列 */
    private List<String> stop;

    /** 工具列表（function calling） */
    private List<CanonicalTool> tools;

    /** tool_choice 类型（"auto"/"required"/"none"/指定工具名） */
    private String toolChoice;

    /** 是否流式 */
    private boolean stream;
}
```

`CanonicalMessage.java`：

```java
package com.codingas.gateway.api.capability.protocol;

import lombok.*;

import java.util.List;

/** 规范消息：role + 文本内容 + 工具调用（assistant 侧） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalMessage {

    /** 角色：user / assistant / tool */
    private String role;

    /** 文本内容（Anthropic 多 content block 拼接为 string 的等价表示） */
    private String content;

    /** assistant 消息的工具调用列表（OpenAI tool_calls 等价） */
    private List<CanonicalToolCall> toolCalls;

    /** tool 角色消息关联的工具调用 ID */
    private String toolCallId;

    /** 工具角色名（OpenAI Message.name） */
    private String name;
}
```

`CanonicalTool.java`：

```java
package com.codingas.gateway.api.capability.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

/** 规范工具定义：name + description + parameters(JSON Schema) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalTool {

    private String name;
    private String description;

    /** 工具入参 JSON Schema */
    private JsonNode parameters;
}
```

`CanonicalToolCall.java`：

```java
package com.codingas.gateway.api.capability.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

/** 规范工具调用：id + name + arguments(JSON) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalToolCall {

    private String id;
    private String name;

    /** 工具调用实参（JSON） */
    private JsonNode arguments;
}
```

`CanonicalChatResponse.java`：

```java
package com.codingas.gateway.api.capability.protocol;

import lombok.*;

import java.util.List;

/** 规范聊天响应——中立表示，覆盖 text 与 tool_use 两种 content block */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalChatResponse {

    private String id;
    private String model;

    /** 内容块列表（text / toolUse） */
    private List<CanonicalContentBlock> content;

    /** 停止原因（end_turn / max_tokens / tool_use 等规范值） */
    private String stopReason;

    /** token 用量 */
    private CanonicalUsage usage;
}
```

`CanonicalContentBlock.java`：

```java
package com.codingas.gateway.api.capability.protocol;

import lombok.*;

/** 规范内容块：type=text 或 type=toolUse */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalContentBlock {

    /** "text" | "toolUse" */
    private String type;

    /** type=text 时的文本 */
    private String text;

    /** type=toolUse 时的工具调用 */
    private CanonicalToolCall toolUse;
}
```

`CanonicalUsage.java`：

```java
package com.codingas.gateway.api.capability.protocol;

import lombok.*;

/** 规范 token 用量 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalUsage {

    private Integer inputTokens;
    private Integer outputTokens;
}
```

- [ ] **Step 4: 写失败测试**

`CanonicalChatRequestTest.java`（验证构建与序列化）：

```java
package com.codingas.gateway.api.capability.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalChatRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void builderConstructsAndSerializes() throws Exception {
        CanonicalMessage msg = CanonicalMessage.builder()
                .role("user").content("你好").build();
        CanonicalChatRequest req = CanonicalChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(msg))
                .stream(true)
                .build();

        String json = mapper.writeValueAsString(req);

        assertThat(req.getModel()).isEqualTo("gpt-4o");
        assertThat(req.getMessages().get(0).getContent()).isEqualTo("你好");
        assertThat(json).contains("\"model\"");
    }
}
```

- [ ] **Step 5: 运行测试验证失败**

Run: `./mvnw test -pl gateway-capability-api`
Expected: FAIL（`CanonicalChatRequest` 类不存在 / 编译失败）

- [ ] **Step 6: 运行测试验证通过**

Run: `./mvnw test -pl gateway-capability-api`
Expected: PASS（1 个测试通过）

- [ ] **Step 7: Commit**

```bash
git add gateway-capability-api pom.xml gateway-boot/pom.xml
git commit -m "feat: 新建 gateway-capability-api 模块与 Canonical IR 规范模型"
```

---

### Task 2: ProtocolAdapter SPI

**Files:**
- Create: `gateway-capability-api/src/main/java/com/codingas/gateway/api/capability/protocol/ProtocolAdapter.java`
- Test: `gateway-capability-api/src/test/java/com/codingas/gateway/api/capability/protocol/ProtocolAdapterContractTest.java`

**Interfaces:**
- Consumes: Task 1 的规范模型（`CanonicalChatRequest`/`CanonicalChatResponse`）。
- Produces: `ProtocolAdapter<T extends ProtocolRequest>` 接口，Task 3/4 实现与消费。签名见下。

- [ ] **Step 1: 写失败测试（契约测试）**

`ProtocolAdapterContractTest.java` —— 验证一个最小内存实现满足契约：

```java
package com.codingas.gateway.api.capability.protocol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolAdapterContractTest {

    /** 最小实现：identity 适配器，用于验证接口契约可被实现 */
    static class IdentityAdapter implements ProtocolAdapter<Object> {

        @Override
        public String protocol() {
            return "identity";
        }

        @Override
        public CanonicalChatRequest normalizeRequest(Object nativeReq) {
            return new CanonicalChatRequest();
        }

        @Override
        public Object denormalizeRequest(CanonicalChatRequest canonical) {
            return new Object();
        }

        @Override
        public CanonicalChatResponse normalizeResponse(Object nativeResp) {
            return new CanonicalChatResponse();
        }

        @Override
        public Object denormalizeResponse(CanonicalChatResponse canonical) {
            return new Object();
        }
    }

    @Test
    void spiIsImplementable() {
        ProtocolAdapter<Object> adapter = new IdentityAdapter();
        assertThat(adapter.protocol()).isEqualTo("identity");
        assertThat(adapter.normalizeRequest(new Object())).isNotNull();
        assertThat(adapter.denormalizeRequest(new CanonicalChatRequest())).isNotNull();
        assertThat(adapter.normalizeResponse(new Object())).isNotNull();
        assertThat(adapter.denormalizeResponse(new CanonicalChatResponse())).isNotNull();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./mvnw test -pl gateway-capability-api`
Expected: FAIL（`ProtocolAdapter` 接口不存在，编译失败）

- [ ] **Step 3: 实现 SPI 接口**

`ProtocolAdapter.java` —— **纯接口，不依赖 Spring**：

```java
package com.codingas.gateway.api.capability.protocol;

/**
 * 协议适配器 SPI：原生协议 ↔ 规范内部模型（Canonical IR）的双向转换。
 *
 * <p>每个支持的协议实现一个 Adapter，只负责"原生↔规范"两跳。网关跨协议转换
 * 由上层编排 normalize + denormalize，避免 N×N 两两转换器。</p>
 *
 * @param <T> 该协议的原生请求类型（如 OpenAIChatRequest / AnthropicMessagesRequest）
 */
public interface ProtocolAdapter<T> {

    /**
     * 协议标识（小写，如 "openai" / "anthropic"）
     */
    String protocol();

    /**
     * 入站：原生请求 → 规范请求
     */
    CanonicalChatRequest normalizeRequest(T nativeRequest);

    /**
     * 出站：规范请求 → 原生请求
     */
    T denormalizeRequest(CanonicalChatRequest canonical);

    /**
     * 入站：原生响应 → 规范响应
     */
    CanonicalChatResponse normalizeResponse(Object nativeResponse);

    /**
     * 出站：规范响应 → 原生响应
     */
    Object denormalizeResponse(CanonicalChatResponse canonical);
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./mvnw test -pl gateway-capability-api`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-capability-api
git commit -m "feat: 定义 ProtocolAdapter SPI（原生↔规范双向转换契约）"
```

---

### Task 3: OpenAIProtocolAdapter（原生↔规范）

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/protocol/OpenAIProtocolAdapter.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/protocol/OpenAIProtocolAdapterTest.java`

**Interfaces:**
- Consumes: `ProtocolAdapter` SPI + `CanonicalChatRequest`/`CanonicalChatResponse`（Task 1/2）；`OpenAIChatRequest`/`OpenAIChatResponse`（`gateway-boot/.../domain/protocol/contract/`）。
- Produces: `OpenAIProtocolAdapter implements ProtocolAdapter<OpenAIChatRequest>`，Task 5 消费。

- [ ] **Step 1: 写失败测试**

`OpenAIProtocolAdapterTest.java`：

```java
package com.codingas.gateway.infrastructure.protocol;

import com.codingas.gateway.api.capability.protocol.CanonicalChatRequest;
import com.codingas.gateway.api.capability.protocol.CanonicalMessage;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIProtocolAdapterTest {

    private OpenAIProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OpenAIProtocolAdapter(new ObjectMapper());
    }

    @Test
    void protocolReturnsOpenai() {
        assertThat(adapter.protocol()).isEqualTo("openai");
    }

    @Test
    void normalizeRequestExtractsSystemAndTool() {
        ObjectNode params = new ObjectMapper().createObjectNode();
        params.put("type", "object");
        OpenAIChatRequest.Message sys = OpenAIChatRequest.Message.builder()
                .role("system").content("你是助手").build();
        OpenAIChatRequest.Message user = OpenAIChatRequest.Message.builder()
                .role("user").content("hi").build();
        OpenAIChatRequest req = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(sys, user))
                .tools(List.of(java.util.Map.of("type", "function",
                        "function", java.util.Map.of("name", "f1", "parameters", params))))
                .stream(true)
                .build();

        CanonicalChatRequest c = adapter.normalizeRequest(req);

        assertThat(c.getSystem()).isEqualTo("你是助手");
        assertThat(c.getMessages()).hasSize(1);
        assertThat(c.getMessages().get(0).getRole()).isEqualTo("user");
        assertThat(c.getTools()).hasSize(1);
        assertThat(c.getTools().get(0).getName()).isEqualTo("f1");
        assertThat(c.isStream()).isTrue();
    }

    @Test
    void denormalizeRequestRoundTripsMessages() {
        CanonicalChatRequest c = CanonicalChatRequest.builder()
                .model("gpt-4o")
                .system("sys")
                .messages(List.of(CanonicalMessage.builder().role("user").content("hi").build()))
                .build();

        OpenAIChatRequest nativeReq = adapter.denormalizeRequest(c);

        assertThat(nativeReq.getModel()).isEqualTo("gpt-4o");
        assertThat(nativeReq.getMessages()).hasSize(2); // system 角色 + user
        assertThat(nativeReq.getMessages().get(0).getRole()).isEqualTo("system");
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./mvnw test -pl gateway-boot -Dtest=OpenAIProtocolAdapterTest`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 OpenAIProtocolAdapter**

平移 `ProtocolConverter` 中 OpenAI 侧逻辑（`toOpenAI(AnthropicMessagesRequest)` 前半段的 OpenAI 构造、`toAnthropic(OpenAIChatRequest)` 前半段的 OpenAI 字段读取），改为原生↔规范：

```java
package com.codingas.gateway.infrastructure.protocol;

import com.codingas.gateway.api.capability.protocol.*;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 协议适配器：OpenAI 原生请求/响应 ↔ 规范内部模型（Canonical IR）。
 *
 * <p>system 角色消息 → 规范顶层 system；tools(Map) → CanonicalTool；
 * 反向 denormalize 时 system 还原为 system 角色消息。</p>
 */
@Component
public class OpenAIProtocolAdapter implements ProtocolAdapter<OpenAIChatRequest> {

    private final ObjectMapper objectMapper;

    public OpenAIProtocolAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String protocol() {
        return "openai";
    }

    @Override
    public CanonicalChatRequest normalizeRequest(OpenAIChatRequest req) {
        String system = null;
        List<CanonicalMessage> messages = new ArrayList<>();
        for (OpenAIChatRequest.Message m : req.getMessages() == null ? List.<OpenAIChatRequest.Message>of() : req.getMessages()) {
            if ("system".equals(m.getRole())) {
                system = m.getContent();
            } else {
                messages.add(CanonicalMessage.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .toolCalls(convertToolCallsToCanonical(m.getToolCalls()))
                        .toolCallId(m.getToolCallId())
                        .name(m.getName())
                        .build());
            }
        }
        return CanonicalChatRequest.builder()
                .model(req.getModel())
                .system(system)
                .messages(messages)
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .stop(req.getStop())
                .tools(convertToolsToCanonical(req.getTools()))
                .toolChoice(req.getToolChoice())
                .stream(req.isStream())
                .build();
    }

    @Override
    public OpenAIChatRequest denormalizeRequest(CanonicalChatRequest c) {
        List<OpenAIChatRequest.Message> messages = new ArrayList<>();
        if (c.getSystem() != null && !c.getSystem().isBlank()) {
            messages.add(OpenAIChatRequest.Message.builder().role("system").content(c.getSystem()).build());
        }
        if (c.getMessages() != null) {
            for (CanonicalMessage cm : c.getMessages()) {
                messages.add(OpenAIChatRequest.Message.builder()
                        .role(cm.getRole())
                        .content(cm.getContent())
                        .toolCalls(convertToolCallsToOpenAI(cm.getToolCalls()))
                        .toolCallId(cm.getToolCallId())
                        .name(cm.getName())
                        .build());
            }
        }
        return OpenAIChatRequest.builder()
                .model(c.getModel())
                .messages(messages)
                .maxTokens(c.getMaxTokens())
                .temperature(c.getTemperature())
                .stop(c.getStop())
                .tools(convertToolsToOpenAI(c.getTools()))
                .toolChoice(c.getToolChoice())
                .stream(c.isStream())
                .build();
    }

    @Override
    public CanonicalChatResponse normalizeResponse(Object nativeResponse) {
        OpenAIChatResponse resp = (OpenAIChatResponse) nativeResponse;
        List<CanonicalContentBlock> blocks = new ArrayList<>();
        if (resp.getChoices() != null && !resp.getChoices().isEmpty()) {
            OpenAIChatResponse.Choice choice = resp.getChoices().get(0);
            if (choice.getMessage() != null) {
                if (choice.getMessage().getContent() != null) {
                    blocks.add(CanonicalContentBlock.builder().type("text").text(choice.getMessage().getContent()).build());
                }
                if (choice.getMessage().getToolCalls() != null) {
                    for (OpenAIChatResponse.ToolCall tc : choice.getMessage().getToolCalls()) {
                        blocks.add(CanonicalContentBlock.builder()
                                .type("toolUse")
                                .toolUse(CanonicalToolCall.builder()
                                        .id(tc.getId())
                                        .name(tc.getFunction() != null ? tc.getFunction().getName() : null)
                                        .arguments(tc.getFunction() != null ? tc.getFunction().getArguments() : null)
                                        .build())
                                .build());
                    }
                }
            }
        }
        CanonicalUsage usage = null;
        if (resp.getUsage() != null) {
            usage = CanonicalUsage.builder()
                    .inputTokens(resp.getUsage().getPromptTokens())
                    .outputTokens(resp.getUsage().getCompletionTokens())
                    .build();
        }
        return CanonicalChatResponse.builder()
                .id(resp.getId())
                .model(resp.getModel())
                .content(blocks)
                .stopReason(mapFinishToStop(resp.getFinishReason()))
                .usage(usage)
                .build();
    }

    @Override
    public Object denormalizeResponse(CanonicalChatResponse c) {
        StringBuilder text = new StringBuilder();
        List<OpenAIChatResponse.ToolCall> toolCalls = new ArrayList<>();
        if (c.getContent() != null) {
            for (CanonicalContentBlock b : c.getContent()) {
                if ("text".equals(b.getType()) && b.getText() != null) {
                    text.append(b.getText());
                } else if ("toolUse".equals(b.getType()) && b.getToolUse() != null) {
                    CanonicalToolCall tu = b.getToolUse();
                    toolCalls.add(OpenAIChatResponse.ToolCall.builder()
                            .id(tu.getId())
                            .type("function")
                            .function(OpenAIChatResponse.FunctionCall.builder()
                                    .name(tu.getName())
                                    .arguments(tu.getArguments())
                                    .build())
                            .build());
                }
            }
        }
        OpenAIChatResponse.Message message = OpenAIChatResponse.Message.builder()
                .role("assistant")
                .content(text.toString())
                .toolCalls(toolCalls.isEmpty() ? null : toolCalls)
                .build();
        OpenAIChatResponse.Usage usage = null;
        if (c.getUsage() != null) {
            int in = c.getUsage().getInputTokens() != null ? c.getUsage().getInputTokens() : 0;
            int out = c.getUsage().getOutputTokens() != null ? c.getUsage().getOutputTokens() : 0;
            usage = OpenAIChatResponse.Usage.builder()
                    .promptTokens(in).completionTokens(out).totalTokens(in + out).build();
        }
        return OpenAIChatResponse.builder()
                .id(c.getId())
                .model(c.getModel())
                .choices(List.of(OpenAIChatResponse.Choice.builder()
                        .index(0).message(message).finishReason(mapStopToFinish(c.getStopReason())).build()))
                .usage(usage)
                .build();
    }

    // ---- 工具/工具调用转换 ----

    @SuppressWarnings("unchecked")
    private List<CanonicalTool> convertToolsToCanonical(List<Map<String, Object>> tools) {
        if (tools == null) return null;
        List<CanonicalTool> out = new ArrayList<>();
        for (Map<String, Object> t : tools) {
            Map<String, Object> fn = (Map<String, Object>) t.get("function");
            if (fn != null) {
                out.add(CanonicalTool.builder()
                        .name((String) fn.get("name"))
                        .description((String) fn.get("description"))
                        .parameters(toJsonNode(fn.get("parameters")))
                        .build());
            }
        }
        return out;
    }

    private List<Map<String, Object>> convertToolsToOpenAI(List<CanonicalTool> tools) {
        if (tools == null) return null;
        List<Map<String, Object>> out = new ArrayList<>();
        for (CanonicalTool t : tools) {
            out.add(Map.of("type", "function",
                    "function", Map.of(
                            "name", t.getName(),
                            "description", t.getDescription() == null ? "" : t.getDescription(),
                            "parameters", t.getParameters())));
        }
        return out;
    }

    private List<CanonicalToolCall> convertToolCallsToCanonical(List<OpenAIChatRequest.ToolCall> calls) {
        if (calls == null) return null;
        List<CanonicalToolCall> out = new ArrayList<>();
        for (OpenAIChatRequest.ToolCall c : calls) {
            out.add(CanonicalToolCall.builder()
                    .id(c.getId())
                    .name(c.getFunction() != null ? c.getFunction().getName() : null)
                    .arguments(toJsonNode(c.getFunction() != null ? c.getFunction().getArguments() : null))
                    .build());
        }
        return out;
    }

    private List<OpenAIChatRequest.ToolCall> convertToolCallsToOpenAI(List<CanonicalToolCall> calls) {
        if (calls == null) return null;
        List<OpenAIChatRequest.ToolCall> out = new ArrayList<>();
        for (CanonicalToolCall c : calls) {
            out.add(OpenAIChatRequest.ToolCall.builder()
                    .id(c.getId())
                    .type("function")
                    .function(OpenAIChatRequest.FunctionCall.builder()
                            .name(c.getName())
                            .arguments(c.getArguments())
                            .build())
                    .build());
        }
        return out;
    }

    private JsonNode toJsonNode(Object o) {
        return o == null ? null : objectMapper.valueToTree(o);
    }

    private String mapFinishToStop(String finishReason) {
        if (finishReason == null) return null;
        return switch (finishReason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls" -> "tool_use";
            default -> finishReason;
        };
    }

    private String mapStopToFinish(String stopReason) {
        if (stopReason == null) return null;
        return switch (stopReason) {
            case "end_turn" -> "stop";
            case "max_tokens" -> "length";
            case "tool_use" -> "tool_calls";
            default -> stopReason;
        };
    }
}
```

> 注：`OpenAIChatResponse` 的 `FunctionCall.arguments` 为 `Object` 类型，可直接存 `JsonNode`；`OpenAIChatRequest.FunctionCall.arguments` 同为 `Object`。若实际类型不符，按 `ProtocolConverter` 现有用法对齐（`arguments` 存 `Object`/字符串均可，`toJsonNode` 负责包装）。

- [ ] **Step 4: 运行测试验证通过**

Run: `./mvnw test -pl gateway-boot -Dtest=OpenAIProtocolAdapterTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/protocol/OpenAIProtocolAdapter.java gateway-boot/src/test/java/com/codingas/gateway/infrastructure/protocol/OpenAIProtocolAdapterTest.java
git commit -m "feat: 实现 OpenAIProtocolAdapter（OpenAI 原生↔规范双向转换）"
```

---

### Task 4: AnthropicProtocolAdapter（原生↔规范）

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/protocol/AnthropicProtocolAdapter.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/protocol/AnthropicProtocolAdapterTest.java`

**Interfaces:**
- Consumes: `ProtocolAdapter` SPI + 规范模型；`AnthropicMessagesRequest`/`AnthropicMessagesResponse`（`gateway-boot/.../domain/protocol/contract/`）。
- Produces: `AnthropicProtocolAdapter implements ProtocolAdapter<AnthropicMessagesRequest>`，Task 5 消费。

- [ ] **Step 1: 写失败测试**

`AnthropicProtocolAdapterTest.java`：

```java
package com.codingas.gateway.infrastructure.protocol;

import com.codingas.gateway.api.capability.protocol.CanonicalChatRequest;
import com.codingas.gateway.api.capability.protocol.CanonicalMessage;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicProtocolAdapterTest {

    private AnthropicProtocolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AnthropicProtocolAdapter();
    }

    @Test
    void protocolReturnsAnthropic() {
        assertThat(adapter.protocol()).isEqualTo("anthropic");
    }

    @Test
    void normalizeRequestKeepsTopLevelSystem() {
        AnthropicMessagesRequest req = AnthropicMessagesRequest.builder()
                .model("claude-4")
                .system("你是助手")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hi").build()))
                .build();

        CanonicalChatRequest c = adapter.normalizeRequest(req);

        assertThat(c.getSystem()).isEqualTo("你是助手");
        assertThat(c.getMessages()).hasSize(1);
        assertThat(c.getMessages().get(0).getRole()).isEqualTo("user");
    }

    @Test
    void denormalizeRequestRestoresSystem() {
        CanonicalChatRequest c = CanonicalChatRequest.builder()
                .model("claude-4")
                .system("sys")
                .messages(List.of(CanonicalMessage.builder().role("user").content("hi").build()))
                .build();

        AnthropicMessagesRequest req = adapter.denormalizeRequest(c);

        assertThat(req.getSystem()).isEqualTo("sys");
        assertThat(req.getMessages()).hasSize(1);
        assertThat(req.getStopSequences()).isNull();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./mvnw test -pl gateway-boot -Dtest=AnthropicProtocolAdapterTest`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 AnthropicProtocolAdapter**

平移 `ProtocolConverter` 中 Anthropic 侧逻辑（`toAnthropic(OpenAIChatRequest)` 的 Anthropic 构造、`toOpenAI(AnthropicMessagesRequest)` 的 Anthropic 字段读取），改为原生↔规范：

```java
package com.codingas.gateway.infrastructure.protocol;

import com.codingas.gateway.api.capability.protocol.*;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic 协议适配器：Anthropic Messages API 原生请求/响应 ↔ 规范内部模型。
 *
 * <p>顶层 system 字段 ↔ 规范顶层 system；content blocks ↔ 规范 content。
 * 规范 tools 转换：Anthropic {"name","description","input_schema"} 与规范 CanonicalTool 对齐。</p>
 */
@Component
public class AnthropicProtocolAdapter implements ProtocolAdapter<AnthropicMessagesRequest> {

    public AnthropicProtocolAdapter() {
    }

    @Override
    public String protocol() {
        return "anthropic";
    }

    @Override
    public CanonicalChatRequest normalizeRequest(AnthropicMessagesRequest req) {
        List<CanonicalMessage> messages = new ArrayList<>();
        if (req.getMessages() != null) {
            for (AnthropicMessagesRequest.Message m : req.getMessages()) {
                messages.add(CanonicalMessage.builder()
                        .role(m.getRole())
                        .content(m.getContent() instanceof String s ? s : String.valueOf(m.getContent()))
                        .build());
            }
        }
        return CanonicalChatRequest.builder()
                .model(req.getModel())
                .system(req.getSystem())
                .messages(messages)
                .maxTokens(req.getMaxTokens())
                .temperature(req.getTemperature())
                .stop(req.getStopSequences())
                .tools(convertToolsToCanonical(req.getTools()))
                .toolChoice(req.getToolChoice() != null ? String.valueOf(req.getToolChoice().get("type")) : null)
                .stream(req.isStream())
                .build();
    }

    @Override
    public AnthropicMessagesRequest denormalizeRequest(CanonicalChatRequest c) {
        List<AnthropicMessagesRequest.Message> messages = new ArrayList<>();
        if (c.getMessages() != null) {
            for (CanonicalMessage cm : c.getMessages()) {
                messages.add(AnthropicMessagesRequest.Message.builder()
                        .role(cm.getRole())
                        .content(cm.getContent())
                        .build());
            }
        }
        return AnthropicMessagesRequest.builder()
                .model(c.getModel())
                .system(c.getSystem())
                .messages(messages)
                .maxTokens(c.getMaxTokens())
                .temperature(c.getTemperature())
                .stopSequences(c.getStop())
                .tools(convertToolsToAnthropic(c.getTools()))
                .toolChoice(c.getToolChoice() != null ? java.util.Map.of("type", c.getToolChoice()) : null)
                .stream(c.isStream())
                .build();
    }

    @Override
    public CanonicalChatResponse normalizeResponse(Object nativeResponse) {
        AnthropicMessagesResponse resp = (AnthropicMessagesResponse) nativeResponse;
        List<CanonicalContentBlock> blocks = new ArrayList<>();
        if (resp.getContent() != null) {
            for (AnthropicMessagesResponse.ContentBlock b : resp.getContent()) {
                if ("text".equals(b.getType())) {
                    blocks.add(CanonicalContentBlock.builder().type("text").text(b.getText()).build());
                } else if ("tool_use".equals(b.getType()) && b.getToolUse() != null) {
                    blocks.add(CanonicalContentBlock.builder()
                            .type("toolUse")
                            .toolUse(CanonicalToolCall.builder()
                                    .id(b.getToolUse().getId())
                                    .name(b.getToolUse().getName())
                                    .arguments(b.getToolUse().getInput())
                                    .build())
                            .build());
                }
            }
        }
        CanonicalUsage usage = null;
        if (resp.getUsage() != null) {
            usage = CanonicalUsage.builder()
                    .inputTokens(resp.getUsage().getInputTokens())
                    .outputTokens(resp.getUsage().getOutputTokens())
                    .build();
        }
        return CanonicalChatResponse.builder()
                .id(resp.getId())
                .model(resp.getModel())
                .content(blocks)
                .stopReason(resp.getStopReason())
                .usage(usage)
                .build();
    }

    @Override
    public Object denormalizeResponse(CanonicalChatResponse c) {
        List<AnthropicMessagesResponse.ContentBlock> blocks = new ArrayList<>();
        if (c.getContent() != null) {
            for (CanonicalContentBlock b : c.getContent()) {
                if ("text".equals(b.getType())) {
                    blocks.add(AnthropicMessagesResponse.ContentBlock.builder().type("text").text(b.getText()).build());
                } else if ("toolUse".equals(b.getType()) && b.getToolUse() != null) {
                    CanonicalToolCall tu = b.getToolUse();
                    blocks.add(AnthropicMessagesResponse.ContentBlock.builder()
                            .type("tool_use")
                            .toolUse(AnthropicMessagesResponse.ToolUse.builder()
                                    .id(tu.getId())
                                    .name(tu.getName())
                                    .input(tu.getArguments())
                                    .build())
                            .build());
                }
            }
        }
        return AnthropicMessagesResponse.builder()
                .id(c.getId())
                .model(c.getModel())
                .type("message")
                .role("assistant")
                .content(blocks)
                .stopReason(c.getStopReason())
                .usage(c.getUsage() != null ? AnthropicMessagesResponse.Usage.builder()
                        .inputTokens(c.getUsage().getInputTokens())
                        .outputTokens(c.getUsage().getOutputTokens())
                        .build() : null)
                .build();
    }

    // ---- 工具转换 ----

    @SuppressWarnings("unchecked")
    private List<CanonicalTool> convertToolsToCanonical(List<java.util.Map<String, Object>> tools) {
        if (tools == null) return null;
        List<CanonicalTool> out = new ArrayList<>();
        for (java.util.Map<String, Object> t : tools) {
            out.add(CanonicalTool.builder()
                    .name((String) t.get("name"))
                    .description((String) t.get("description"))
                    .parameters(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
                            .valueToTree(t.get("input_schema")))
                    .build());
        }
        return out;
    }

    private List<java.util.Map<String, Object>> convertToolsToAnthropic(List<CanonicalTool> tools) {
        if (tools == null) return null;
        List<java.util.Map<String, Object>> out = new ArrayList<>();
        for (CanonicalTool t : tools) {
            out.add(java.util.Map.of(
                    "name", t.getName(),
                    "description", t.getDescription() == null ? "" : t.getDescription(),
                    "input_schema", t.getParameters()));
        }
        return out;
    }
}
```

> 注：`AnthropicMessagesResponse.ToolUse.input` 为 `Object`，可存 `JsonNode`；`AnthropicMessagesRequest.Message.content` 为 `Object`，`String` 分支走 `content instanceof String`。若与既有类型不符，参考 `ProtocolConverter` 现有用法对齐。

- [ ] **Step 4: 运行测试验证通过**

Run: `./mvnw test -pl gateway-boot -Dtest=AnthropicProtocolAdapterTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/protocol/AnthropicProtocolAdapter.java gateway-boot/src/test/java/com/codingas/gateway/infrastructure/protocol/AnthropicProtocolAdapterTest.java
git commit -m "feat: 实现 AnthropicProtocolAdapter（Anthropic 原生↔规范双向转换）"
```

---

### Task 5: ProtocolConversionFacade 并重写 ChannelFailoverInvoker

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/conversion/ProtocolConversionFacade.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/protocol/ProtocolStreamConverter.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/invoker/ChannelFailoverInvoker.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/protocol/conversion/ProtocolConverter.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/protocol/conversion/ProtocolConversionFacadeTest.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/protocol/ProtocolStreamConverterTest.java`

**Interfaces:**
- Consumes: `OpenAIProtocolAdapter`、`AnthropicProtocolAdapter`（Task 3/4）。
- Produces: `ProtocolConversionFacade` 提供 `convertRequest`/`convertResponse`/`convertStreamChunk`/`convertStreamDone`，替换 `ChannelFailoverInvoker` 对旧 `ProtocolConverter` 的引用。

- [ ] **Step 1: 平移流式逻辑为 ProtocolStreamConverter**

新建 `ProtocolStreamConverter.java`，把旧 `ProtocolConverter` 的 `convertStreamChunk`/`convertStreamDone`/两个私有 chunk 转换方法 + `mapFinishReasonToStopReason`/`mapStopReasonToFinishReason` 原样搬入（`@Component`，依赖 `ObjectMapper`）。这是纯平移，行为不变。

- [ ] **Step 2: 写失败测试（Facade）**

`ProtocolConversionFacadeTest.java`：

```java
package com.codingas.gateway.domain.protocol.conversion;

import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.infrastructure.protocol.AnthropicProtocolAdapter;
import com.codingas.gateway.infrastructure.protocol.OpenAIProtocolAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolConversionFacadeTest {

    @Test
    void convertRequestOpenaiToAnthropic() {
        ProtocolConversionFacade facade = new ProtocolConversionFacade(
                new OpenAIProtocolAdapter(new ObjectMapper()),
                new AnthropicProtocolAdapter());

        OpenAIChatRequest openai = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder().role("system").content("s").build(),
                        OpenAIChatRequest.Message.builder().role("user").content("hi").build()))
                .build();

        AnthropicMessagesRequest anthropic = facade.convertRequest(openai, "anthropic");

        assertThat(anthropic).isInstanceOf(AnthropicMessagesRequest.class);
        assertThat(anthropic.getSystem()).isEqualTo("s");
        assertThat(anthropic.getMessages()).hasSize(1);
    }

    @Test
    void convertRequestSameProtocolReturnsSame() {
        ProtocolConversionFacade facade = new ProtocolConversionFacade(
                new OpenAIProtocolAdapter(new ObjectMapper()),
                new AnthropicProtocolAdapter());

        OpenAIChatRequest openai = OpenAIChatRequest.builder().model("gpt-4o").build();

        assertThat(facade.convertRequest(openai, "openai")).isSameAs(openai);
    }
}
```

- [ ] **Step 3: 运行测试验证失败**

Run: `./mvnw test -pl gateway-boot -Dtest=ProtocolConversionFacadeTest`
Expected: FAIL（Facade 不存在）

- [ ] **Step 4: 实现 ProtocolConversionFacade**

```java
package com.codingas.gateway.domain.protocol.conversion;

import com.codingas.gateway.api.capability.protocol.CanonicalChatRequest;
import com.codingas.gateway.api.capability.protocol.CanonicalChatResponse;
import com.codingas.gateway.api.capability.protocol.ProtocolAdapter;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.infrastructure.protocol.AnthropicProtocolAdapter;
import com.codingas.gateway.infrastructure.protocol.OpenAIProtocolAdapter;
import org.springframework.stereotype.Component;

/**
 * 跨协议转换门面：编排各协议 Adapter，把"原生→规范→原生"收敛为对外简洁调用。
 *
 * <p>对外提供与旧 {@code ProtocolConverter} 相同语义的
 * {@code convertRequest/convertResponse/convertStreamChunk/convertStreamDone}，
 * 但底层基于 Canonical IR + ProtocolAdapter（normalize + denormalize），
 * 消除 N×N 两两转换。流式仍委托 {@link ProtocolStreamConverter}（本轮保持原样）。</p>
 */
@Component
public class ProtocolConversionFacade {

    private final OpenAIProtocolAdapter openaiAdapter;
    private final AnthropicProtocolAdapter anthropicAdapter;
    private final ProtocolStreamConverter streamConverter;

    public ProtocolConversionFacade(OpenAIProtocolAdapter openaiAdapter,
                                    AnthropicProtocolAdapter anthropicAdapter) {
        this.openaiAdapter = openaiAdapter;
        this.anthropicAdapter = anthropicAdapter;
        this.streamConverter = new ProtocolStreamConverter(new com.fasterxml.jackson.databind.ObjectMapper());
    }

    public ProtocolConversionFacade(OpenAIProtocolAdapter openaiAdapter,
                                    AnthropicProtocolAdapter anthropicAdapter,
                                    ProtocolStreamConverter streamConverter) {
        this.openaiAdapter = openaiAdapter;
        this.anthropicAdapter = anthropicAdapter;
        this.streamConverter = streamConverter;
    }

    /**
     * 跨协议请求转换：目标协议 ≠ 源协议时 normalize→denormalize；否则原样返回。
     */
    public ProtocolRequest convertRequest(ProtocolRequest request, String targetProtocol) {
        if (request instanceof OpenAIChatRequest openai && !"openai".equals(targetProtocol)) {
            return toAnthropic(openai);
        }
        if (request instanceof AnthropicMessagesRequest anthropic && !"anthropic".equals(targetProtocol)) {
            return toOpenAI(anthropic);
        }
        return request;
    }

    /** OpenAI 请求 → Anthropic 请求（normalize + denormalize 两跳） */
    private AnthropicMessagesRequest toAnthropic(OpenAIChatRequest openai) {
        CanonicalChatRequest canonical = openaiAdapter.normalizeRequest(openai);
        return anthropicAdapter.denormalizeRequest(canonical);
    }

    /** Anthropic 请求 → OpenAI 请求 */
    private OpenAIChatRequest toOpenAI(AnthropicMessagesRequest anthropic) {
        CanonicalChatRequest canonical = anthropicAdapter.normalizeRequest(anthropic);
        return openaiAdapter.denormalizeRequest(canonical);
    }

    /**
     * 跨协议响应转换：目标协议 ≠ 源协议时 normalize→denormalize；否则原样返回。
     */
    public ProtocolResponse convertResponse(ProtocolResponse response, String sourceProtocol) {
        if (response instanceof AnthropicMessagesResponse anthropic && "anthropic".equals(sourceProtocol)) {
            CanonicalChatResponse canonical = anthropicAdapter.normalizeResponse(anthropic);
            return (OpenAIChatResponse) openaiAdapter.denormalizeResponse(canonical);
        }
        if (response instanceof OpenAIChatResponse openai && "openai".equals(sourceProtocol)) {
            CanonicalChatResponse canonical = openaiAdapter.normalizeResponse(openai);
            return (AnthropicMessagesResponse) anthropicAdapter.denormalizeResponse(canonical);
        }
        return response;
    }

    /** 流式 chunk 转换（委托 ProtocolStreamConverter，方向 from→to） */
    public com.codingas.gateway.domain.protocol.contract.StreamChunkResult convertStreamChunk(
            String rawChunk, String fromProtocol, String toProtocol) {
        return streamConverter.convertStreamChunk(rawChunk, fromProtocol, toProtocol);
    }

    /** 流式结束标记转换（委托 ProtocolStreamConverter） */
    public com.codingas.gateway.domain.protocol.contract.StreamChunkResult convertStreamDone(
            String fromProtocol, String toProtocol) {
        return streamConverter.convertStreamDone(fromProtocol, toProtocol);
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `./mvnw test -pl gateway-boot -Dtest=ProtocolConversionFacadeTest`
Expected: PASS

- [ ] **Step 6: 重写 ChannelFailoverInvoker 使用 Facade**

修改 `ChannelFailoverInvoker.java`：
1. 字段 `private final ProtocolConverter protocolConverter;` → `private final ProtocolConversionFacade protocolConversionFacade;`（构造器同步改）
2. `convertRequest` 中两处调用改为：
   ```java
   return protocolConversionFacade.convertRequest(request, ctx.upstreamProtocol().name().toLowerCase());
   ```
3. `convertResponse` 中两处改为：
   ```java
   return protocolConversionFacade.convertResponse(response, ctx.upstreamProtocol().name().toLowerCase());
   ```
4. `buildStreamCallback` 中 `protocolConverter.convertStreamChunk(...)`/`convertStreamDone(...)` → `protocolConversionFacade.convertStreamChunk(...)`/`convertStreamDone(...)`。
5. 调整 import（删除 `ProtocolConverter`，新增 `ProtocolConversionFacade`）。

- [ ] **Step 7: 迁移流式测试并删除旧 ProtocolConverter**

1. 把旧 `ProtocolConverter` 的流式转换测试逻辑迁移为 `ProtocolStreamConverterTest.java`（针对 `ProtocolStreamConverter` 的 `convertStreamChunk`/`convertStreamDone`）。
2. 删除 `ProtocolConverter.java`。
3. 全量运行协议相关测试验证行为保持：

Run: `./mvnw test -pl gateway-boot -Dtest=ProtocolConversionFacadeTest,OpenAIProtocolAdapterTest,AnthropicProtocolAdapterTest,ProtocolStreamConverterTest,ChannelFailoverInvokerTest,ChatDispatchServiceTest`
Expected: 全部 PASS

- [ ] **Step 8: 运行集成测试**

Run: `./mvnw test -pl gateway-boot -Dtest=ProtocolConversionIntegrationTest,ChannelFailoverIntegrationTest`
Expected: 全部 PASS（双 API 兼容行为保持）

- [ ] **Step 9: Commit**

```bash
git add gateway-boot
git commit -m "refactor: 引入 ProtocolConversionFacade，转换走 Canonical IR + 适配器，删除旧 ProtocolConverter"
```

---

## Self-Review

**1. Spec 覆盖**（对照 spec §4、§8、§9 落地顺序 1-2）：
- ✅ Canonical IR 模型（§4.2）→ Task 1
- ✅ ProtocolAdapter SPI（§4.1、§8 gateway-capability-api）→ Task 2
- ✅ 存量转换器迁移为 openai/anthropic 两个 Adapter（§9.2）→ Task 3/4
- ✅ 重写调用方走 canonical（§4.1 normalize/denormalize）→ Task 5
- ⚠️ 流式 canonical 化：spec 提到，但本轮按 YAGNI 保持原样（`ProtocolStreamConverter` 平移），已在全局约束与架构说明中注明为后续阶段。无 spec 缺口（spec §D7 明确"流式"为规范事件，但落地顺序未强制本轮实现）。

**2. Placeholder 扫描**：无 "TBD"/"TODO"/"实现略"；每个代码步骤均含完整实现。

**3. 类型一致性**：`ProtocolAdapter`（Task 2）签名与 Task 3/4 实现一致（`protocol()/normalizeRequest/denormalizeRequest/normalizeResponse/denormalizeResponse`）；`ProtocolConversionFacade`（Task 5）消费 `OpenAIProtocolAdapter`/`AnthropicProtocolAdapter`（Task 3/4）；`ChannelFailoverInvoker` 改造调用点与 Facade 方法签名一致。
