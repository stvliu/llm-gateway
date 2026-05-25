# 协议 DTO 重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将协议 DTO 沉入 domain 层，入站出站共用，消除三层冗余，建立 ProtocolConverter/Validator/Factory 体系。

**Architecture:** 协议 DTO（OpenAIChatRequest 等）从 application 层移到 domain/proxy/protocol/，实现 ProtocolRequest/ProtocolResponse 接口。ProtocolGateway 签名改用 domain 层接口，消除反向依赖。ProtocolGatewayRegistry 替换为 ProtocolGatewayFactory（实例化注入 Provider 配置）。新增 ProtocolConverter（跨协议转换）和 ProtocolValidator（入站校验）。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, Jackson, OkHttp

---

## File Structure

### 新增文件

| 文件 | 职责 |
|------|------|
| `domain/proxy/protocol/ProtocolRequest.java` | 协议请求接口：getModel/setModel/getProtocol/isStream/setStream |
| `domain/proxy/protocol/ProtocolResponse.java` | 协议响应接口：getModel/getFinishReason |
| `domain/proxy/protocol/OpenAIProtocolValidator.java` | OpenAI 入站校验 |
| `domain/proxy/protocol/AnthropicProtocolValidator.java` | Anthropic 入站校验 |
| `domain/proxy/exception/ProtocolValidationException.java` | 协议校验异常 |
| `domain/proxy/protocol/ProtocolConverter.java` | 跨协议转换（请求/响应/流式 chunk） |
| `domain/proxy/gateway/ProtocolGatewayFactory.java` | Gateway 工厂接口 |
| `infrastructure/proxy/gateway/ProtocolGatewayFactoryImpl.java` | Gateway 工厂实现 |
| `domain/proxy/protocol/ProtocolValidator.java` | 校验器接口 |

### 移动文件

| 文件 | 从 → 到 | 变更 |
|------|---------|------|
| `OpenAIChatRequest.java` | `application/proxy/dto/` → `domain/proxy/protocol/` | 实现 ProtocolRequest |
| `OpenAIChatResponse.java` | `application/proxy/dto/` → `domain/proxy/protocol/` | 实现 ProtocolResponse |
| `AnthropicMessagesRequest.java` | `application/proxy/dto/` → `domain/proxy/protocol/` | 实现 ProtocolRequest |
| `AnthropicMessagesResponse.java` | `application/proxy/dto/` → `domain/proxy/protocol/` | 实现 ProtocolResponse |

### 删除文件

| 文件 | 原因 |
|------|------|
| `application/proxy/dto/LLMRequest.java` | 被 ProtocolRequest + 协议 DTO 替代 |
| `application/proxy/dto/LLMResponse.java` | 被 ProtocolResponse + 协议 DTO 替代 |
| `domain/proxy/valueobject/LLMRequestVO.java` | 协议 DTO 在 domain 层，不需要 VO |
| `domain/proxy/valueobject/LLMResponseVO.java` | 同上 |
| `domain/proxy/valueobject/ChatResponseVO.java` | 同上 |
| `application/proxy/dto/chat/ChatRequest.java` | 同上 |
| `application/proxy/dto/chat/ChatResponse.java` | 同上 |
| `application/proxy/dto/LLMDtoConverter.java` | DTO↔VO 转换不再需要 |
| `domain/proxy/gateway/ProtocolGatewayRegistry.java` | 被 ProtocolGatewayFactory 替代 |
| `infrastructure/proxy/gateway/ProtocolGatewayRegistryImpl.java` | 被 ProtocolGatewayFactoryImpl 替代 |

### 修改文件

| 文件 | 变更摘要 |
|------|---------|
| `domain/proxy/gateway/ProtocolGateway.java` | 签名改用 ProtocolRequest/ProtocolResponse，删除 baseUrl/apiKey/timeout 参数 |
| `infrastructure/proxy/gateway/protocol/OpenAIProtocolGateway.java` | 实现新签名；构造函数注入配置；Jackson 替代 Map 手拼 |
| `infrastructure/proxy/gateway/protocol/AnthropicProtocolGateway.java` | 同上 |
| `application/proxy/ProxyServiceImpl.java` | 改用 ProtocolConverter + ProtocolGatewayFactory |
| `application/proxy/ProxyService.java` | 接口签名适配 |
| `adapter/api/OpenAIController.java` | import 路径改为 domain.proxy.protocol；增加入站校验 |
| `adapter/api/AnthropicController.java` | 同上 |
| `adapter/api/ExperienceController.java` | 同上 |
| `application/experience/ModelExperienceService.java` | 删除 LLMRequest 手动构建，改用协议 DTO |
| `adapter/api/ProtocolController.java` | 注入改为 ProtocolGatewayFactory |
| `infrastructure/actuator/ProviderHealthTracker.java` | 适配 ProtocolGatewayFactory |
| `application/proxy/ProductRoutingService.java` | 适配新接口 |

---

### Task 1: ProtocolRequest + ProtocolResponse 接口

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolRequest.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolResponse.java`

- [ ] **Step 1: 创建 ProtocolRequest 接口**

```java
package com.codingas.gateway.domain.proxy.protocol;

/**
 * 协议请求接口，所有协议请求 DTO 实现此接口
 */
public interface ProtocolRequest {

    /**
     * 获取模型名称
     */
    String getModel();

    /**
     * 设置模型名称（路由后覆盖）
     */
    void setModel(String model);

    /**
     * 获取协议标识（"openai" / "anthropic"）
     */
    String getProtocol();

    /**
     * 是否流式请求
     */
    boolean isStream();

    /**
     * 设置流式标记
     */
    void setStream(boolean stream);
}
```

- [ ] **Step 2: 创建 ProtocolResponse 接口**

```java
package com.codingas.gateway.domain.proxy.protocol;

/**
 * 协议响应接口，所有协议响应 DTO 实现此接口
 */
public interface ProtocolResponse {

    /**
     * 获取模型名称
     */
    String getModel();

    /**
     * 获取结束原因
     */
    String getFinishReason();
}
```

- [ ] **Step 3: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolRequest.java gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolResponse.java
git commit -m "feat: 新增 ProtocolRequest/ProtocolResponse 协议接口"
```

---

### Task 2: 协议 DTO 移动到 domain 层 + 实现接口

**Files:**
- Move: `application/proxy/dto/OpenAIChatRequest.java` → `domain/proxy/protocol/OpenAIChatRequest.java`
- Move: `application/proxy/dto/OpenAIChatResponse.java` → `domain/proxy/protocol/OpenAIChatResponse.java`
- Move: `application/proxy/dto/AnthropicMessagesRequest.java` → `domain/proxy/protocol/AnthropicMessagesRequest.java`
- Move: `application/proxy/dto/AnthropicMessagesResponse.java` → `domain/proxy/protocol/AnthropicMessagesResponse.java`

- [ ] **Step 1: 移动 OpenAIChatRequest 并实现 ProtocolRequest**

1. 在 `domain/proxy/protocol/` 下创建 `OpenAIChatRequest.java`，内容从原文件复制
2. 修改 package 为 `com.codingas.gateway.domain.proxy.protocol`
3. 添加 `implements ProtocolRequest`
4. 添加 `getProtocol()` 方法返回 `"openai"`
5. 确认 `getModel()`/`setModel()`/`isStream()`/`setStream()` 已存在（当前类已有这些方法）
6. 删除原文件 `application/proxy/dto/OpenAIChatRequest.java`

- [ ] **Step 2: 移动 OpenAIChatResponse 并实现 ProtocolResponse**

1. 在 `domain/proxy/protocol/` 下创建 `OpenAIChatResponse.java`
2. 修改 package 为 `com.codingas.gateway.domain.proxy.protocol`
3. 添加 `implements ProtocolResponse`
4. 确认 `getModel()` 已存在；添加 `getFinishReason()` 方法（从 `choices[0].finishReason` 提取）
5. 删除原文件

- [ ] **Step 3: 移动 AnthropicMessagesRequest 并实现 ProtocolRequest**

1. 在 `domain/proxy/protocol/` 下创建 `AnthropicMessagesRequest.java`
2. 修改 package 为 `com.codingas.gateway.domain.proxy.protocol`
3. 添加 `implements ProtocolRequest`
4. 添加 `getProtocol()` 方法返回 `"anthropic"`
5. 确保 `getModel()`/`setModel()`/`isStream()`/`setStream()` 存在，如不存在则添加
6. 删除原文件

- [ ] **Step 4: 移动 AnthropicMessagesResponse 并实现 ProtocolResponse**

1. 在 `domain/proxy/protocol/` 下创建 `AnthropicMessagesResponse.java`
2. 修改 package 为 `com.codingas.gateway.domain.proxy.protocol`
3. 添加 `implements ProtocolResponse`
4. 确认 `getModel()` 已存在；添加 `getFinishReason()` 方法（从 `stopReason` 提取）
5. 删除原文件

- [ ] **Step 5: 全局替换 import 路径**

将项目中所有引用这 4 个类的 import 从 `com.codingas.gateway.application.proxy.dto` 替换为 `com.codingas.gateway.domain.proxy.protocol`。涉及文件：

- `OpenAIController.java`
- `AnthropicController.java`
- `ExperienceController.java`
- `ProxyServiceImpl.java`
- `OpenAIProtocolGateway.java`
- `AnthropicProtocolGateway.java`
- `ModelExperienceService.java`
- 以及所有引用这些类的文件

运行 `./mvnw compile -pl gateway-boot` 确认编译通过。

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "refactor: 协议 DTO 从 application 层移到 domain/proxy/protocol/，实现 ProtocolRequest/ProtocolResponse"
```

---

### Task 3: ProtocolValidationException + ProtocolValidator 接口

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/exception/ProtocolValidationException.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolValidator.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/exception/ProtocolValidationExceptionTest.java`

- [ ] **Step 1: 创建 ProtocolValidationException**

```java
package com.codingas.gateway.domain.proxy.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 协议校验异常
 */
public class ProtocolValidationException extends GatewayException {

    private final String protocol;
    private final String field;
    private final String violation;

    public ProtocolValidationException(String protocol, String field, String violation) {
        super(String.format("协议校验失败 [%s]: 字段 '%s' %s", protocol, field, violation));
        this.protocol = protocol;
        this.field = field;
        this.violation = violation;
    }

    public String getProtocol() { return protocol; }
    public String getField() { return field; }
    public String getViolation() { return violation; }
}
```

- [ ] **Step 2: 创建 ProtocolValidator 接口**

```java
package com.codingas.gateway.domain.proxy.protocol;

/**
 * 协议校验器接口
 */
public interface ProtocolValidator<T extends ProtocolRequest> {

    /**
     * 获取支持的协议标识
     */
    String getProtocol();

    /**
     * 入站校验协议请求
     */
    void validate(T request);
}
```

- [ ] **Step 3: 编写 ProtocolValidationException 测试**

```java
package com.codingas.gateway.domain.proxy.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProtocolValidationExceptionTest {

    @Test
    void shouldContainProtocolFieldAndViolation() {
        var ex = new ProtocolValidationException("anthropic", "max_tokens", "必填且大于0");
        assertThat(ex.getProtocol()).isEqualTo("anthropic");
        assertThat(ex.getField()).isEqualTo("max_tokens");
        assertThat(ex.getViolation()).isEqualTo("必填且大于0");
        assertThat(ex.getMessage()).contains("anthropic").contains("max_tokens");
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest=ProtocolValidationExceptionTest -v
```

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/exception/ProtocolValidationException.java gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolValidator.java gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/exception/ProtocolValidationExceptionTest.java
git commit -m "feat: 新增 ProtocolValidationException + ProtocolValidator 接口"
```

---

### Task 4: OpenAIProtocolValidator + AnthropicProtocolValidator

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/OpenAIProtocolValidator.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/AnthropicProtocolValidator.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/protocol/OpenAIProtocolValidatorTest.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/protocol/AnthropicProtocolValidatorTest.java`

- [ ] **Step 1: 编写 OpenAIProtocolValidator 失败测试**

```java
package com.codingas.gateway.domain.proxy.protocol;

import com.codingas.gateway.domain.proxy.exception.ProtocolValidationException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAIProtocolValidatorTest {

    private final OpenAIProtocolValidator validator = new OpenAIProtocolValidator();

    @Test
    void shouldPassValidRequest() {
        var request = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder()
                        .role("user").content("hello").build()))
                .build();
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldRejectNullModel() {
        var request = OpenAIChatRequest.builder()
                .model(null)
                .messages(List.of(OpenAIChatRequest.Message.builder()
                        .role("user").content("hello").build()))
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    var pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).isEqualTo("model");
                });
    }

    @Test
    void shouldRejectEmptyMessages() {
        var request = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(null)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    var pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).isEqualTo("messages");
                });
    }

    @Test
    void shouldRejectInvalidRole() {
        var request = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(OpenAIChatRequest.Message.builder()
                        .role("invalid_role").content("hello").build()))
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> {
                    var pve = (ProtocolValidationException) ex;
                    assertThat(pve.getField()).isEqualTo("messages[0].role");
                });
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest=OpenAIProtocolValidatorTest -v
```

Expected: 编译失败（类不存在）

- [ ] **Step 3: 实现 OpenAIProtocolValidator**

```java
package com.codingas.gateway.domain.proxy.protocol;

import com.codingas.gateway.domain.proxy.exception.ProtocolValidationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * OpenAI Chat Completions 协议校验器
 */
@Component
public class OpenAIProtocolValidator implements ProtocolValidator<OpenAIChatRequest> {

    private static final Set<String> VALID_ROLES = Set.of("system", "user", "assistant", "tool");

    @Override
    public String getProtocol() {
        return "openai";
    }

    @Override
    public void validate(OpenAIChatRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new ProtocolValidationException("openai", "model", "不能为空");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new ProtocolValidationException("openai", "messages", "不能为空");
        }
        List<OpenAIChatRequest.Message> messages = request.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            OpenAIChatRequest.Message msg = messages.get(i);
            if (msg.getRole() == null || !VALID_ROLES.contains(msg.getRole())) {
                throw new ProtocolValidationException("openai",
                        "messages[" + i + "].role", "不合法: " + msg.getRole());
            }
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest=OpenAIProtocolValidatorTest -v
```

- [ ] **Step 5: 编写 AnthropicProtocolValidator 失败测试**

```java
package com.codingas.gateway.domain.proxy.protocol;

import com.codingas.gateway.domain.proxy.exception.ProtocolValidationException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicProtocolValidatorTest {

    private final AnthropicProtocolValidator validator = new AnthropicProtocolValidator();

    @Test
    void shouldPassValidRequest() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hello").build()))
                .maxTokens(1024)
                .build();
        assertThatNoException().isThrownBy(() -> validator.validate(request));
    }

    @Test
    void shouldRejectNullModel() {
        var request = AnthropicMessagesRequest.builder()
                .model(null)
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hello").build()))
                .maxTokens(1024)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> assertThat(((ProtocolValidationException) ex).getField()).isEqualTo("model"));
    }

    @Test
    void shouldRejectNullMaxTokens() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hello").build()))
                .maxTokens(null)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> assertThat(((ProtocolValidationException) ex).getField()).isEqualTo("max_tokens"));
    }

    @Test
    void shouldRejectZeroMaxTokens() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(List.of(AnthropicMessagesRequest.Message.builder()
                        .role("user").content("hello").build()))
                .maxTokens(0)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> assertThat(((ProtocolValidationException) ex).getField()).isEqualTo("max_tokens"));
    }

    @Test
    void shouldRejectEmptyMessages() {
        var request = AnthropicMessagesRequest.builder()
                .model("claude-3-5-sonnet-20241022")
                .messages(null)
                .maxTokens(1024)
                .build();
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProtocolValidationException.class)
                .satisfies(ex -> assertThat(((ProtocolValidationException) ex).getField()).isEqualTo("messages"));
    }
}
```

- [ ] **Step 6: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest=AnthropicProtocolValidatorTest -v
```

- [ ] **Step 7: 实现 AnthropicProtocolValidator**

```java
package com.codingas.gateway.domain.proxy.protocol;

import com.codingas.gateway.domain.proxy.exception.ProtocolValidationException;
import org.springframework.stereotype.Component;

/**
 * Anthropic Messages 协议校验器
 */
@Component
public class AnthropicProtocolValidator implements ProtocolValidator<AnthropicMessagesRequest> {

    @Override
    public String getProtocol() {
        return "anthropic";
    }

    @Override
    public void validate(AnthropicMessagesRequest request) {
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new ProtocolValidationException("anthropic", "model", "不能为空");
        }
        if (request.getMaxTokens() == null) {
            throw new ProtocolValidationException("anthropic", "max_tokens", "必填");
        }
        if (request.getMaxTokens() <= 0) {
            throw new ProtocolValidationException("anthropic", "max_tokens", "必须大于0");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new ProtocolValidationException("anthropic", "messages", "不能为空");
        }
    }
}
```

- [ ] **Step 8: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest=AnthropicProtocolValidatorTest -v
```

- [ ] **Step 9: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/OpenAIProtocolValidator.java gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/AnthropicProtocolValidator.java gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/protocol/OpenAIProtocolValidatorTest.java gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/protocol/AnthropicProtocolValidatorTest.java
git commit -m "feat: 新增 OpenAI/Anthropic 协议校验器（入站校验）"
```

---

### Task 5: ProtocolConverter — 非流式请求转换

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverter.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverterRequestTest.java`

- [ ] **Step 1: 编写 OpenAI→Anthropic 请求转换测试**

```java
package com.codingas.gateway.domain.proxy.protocol;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ProtocolConverterRequestTest {

    private final ProtocolConverter converter = new ProtocolConverter();

    @Test
    void shouldConvertOpenAIToAnthropic_basicRequest() {
        var openai = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(
                        OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                .maxTokens(2048)
                .temperature(0.7)
                .stream(true)
                .build();

        var anthropic = converter.toAnthropic(openai);

        assertThat(anthropic.getModel()).isEqualTo("gpt-4o");
        assertThat(anthropic.getMaxTokens()).isEqualTo(2048);
        assertThat(anthropic.getTemperature()).isEqualTo(0.7);
        assertThat(anthropic.isStream()).isTrue();
        assertThat(anthropic.getMessages()).hasSize(1);
        assertThat(anthropic.getMessages().get(0).getRole()).isEqualTo("user");
    }

    @Test
    void shouldConvertOpenAIToAnthropic_systemMessageExtracted() {
        var openai = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(
                        OpenAIChatRequest.Message.builder().role("system").content("You are helpful").build(),
                        OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                .maxTokens(2048)
                .build();

        var anthropic = converter.toAnthropic(openai);

        // system 消息提取到顶层字段
        assertThat(anthropic.getSystem()).isEqualTo("You are helpful");
        // messages 中不含 system
        assertThat(anthropic.getMessages()).hasSize(1);
        assertThat(anthropic.getMessages().get(0).getRole()).isEqualTo("user");
    }

    @Test
    void shouldConvertOpenAIToAnthropic_maxTokensDefaultWhenNull() {
        var openai = OpenAIChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(
                        OpenAIChatRequest.Message.builder().role("user").content("hello").build()))
                .maxTokens(null)
                .build();

        var anthropic = converter.toAnthropic(openai);

        // max_tokens 缺省补 1024
        assertThat(anthropic.getMaxTokens()).isEqualTo(1024);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest=ProtocolConverterRequestTest -v
```

- [ ] **Step 3: 实现 ProtocolConverter 非流式请求转换部分**

在 `domain/proxy/protocol/ProtocolConverter.java` 中实现 `toAnthropic(OpenAIChatRequest)` 和 `toOpenAI(AnthropicMessagesRequest)` 方法。

关键逻辑：
- `toAnthropic`：遍历 messages，system 角色提取到顶层 system 字段；maxTokens 缺省补 1024
- `toOpenAI`：顶层 system 字段合并为 system 角色消息；content blocks 拼接

注意：此步骤只实现请求转换，响应转换和流式转换的方法暂留空抛 UnsupportedOperationException。

- [ ] **Step 4: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest=ProtocolConverterRequestTest -v
```

- [ ] **Step 5: 编写 Anthropic→OpenAI 请求转换测试并实现**

添加 `shouldConvertAnthropicToOpenAI_basicRequest` 和 `shouldConvertAnthropicToOpenAI_systemMergedToMessages` 测试，实现 `toOpenAI(AnthropicMessagesRequest)` 方法。

- [ ] **Step 6: 运行全部 ProtocolConverter 请求测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest=ProtocolConverterRequestTest -v
```

- [ ] **Step 7: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverter.java gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverterRequestTest.java
git commit -m "feat: ProtocolConverter 非流式请求转换（OpenAI↔Anthropic）"
```

---

### Task 6: ProtocolConverter — 非流式响应转换

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverter.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverterResponseTest.java`

- [ ] **Step 1: 编写响应转换测试**

```java
package com.codingas.gateway.domain.proxy.protocol;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProtocolConverterResponseTest {

    private final ProtocolConverter converter = new ProtocolConverter();

    @Test
    void shouldConvertOpenAIToAnthropic_response() {
        var openai = OpenAIChatResponse.builder()
                .id("chatcmpl-123")
                .model("gpt-4o")
                .choices(List.of(OpenAIChatResponse.Choice.builder()
                        .index(0)
                        .message(OpenAIChatResponse.Message.builder()
                                .role("assistant").content("Hello!").build())
                        .finishReason("stop")
                        .build()))
                .usage(OpenAIChatResponse.Usage.builder()
                        .promptTokens(10).completionTokens(5).totalTokens(15).build())
                .build();

        var anthropic = converter.toAnthropic(openai);

        assertThat(anthropic.getModel()).isEqualTo("gpt-4o");
        assertThat(anthropic.getStopReason()).isEqualTo("end_turn");
    }

    @Test
    void shouldConvertAnthropicToOpenAI_response() {
        var anthropic = AnthropicMessagesResponse.builder()
                .id("msg-123")
                .model("claude-3-5-sonnet-20241022")
                .stopReason("end_turn")
                .usage(AnthropicMessagesResponse.Usage.builder()
                        .inputTokens(10).outputTokens(5).build())
                .build();

        var openai = converter.toOpenAI(anthropic);

        assertThat(openai.getModel()).isEqualTo("claude-3-5-sonnet-20241022");
        assertThat(openai.getChoices().get(0).getFinishReason()).isEqualTo("stop");
        assertThat(openai.getUsage().getTotalTokens()).isEqualTo(15);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest=ProtocolConverterResponseTest -v
```

- [ ] **Step 3: 实现响应转换方法**

关键映射：
- `stop` ↔ `end_turn`
- `length` ↔ `max_tokens`
- `tool_calls` ↔ `tool_use`
- Anthropic→OpenAI 时补 `total_tokens` = inputTokens + outputTokens

- [ ] **Step 4: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest=ProtocolConverterResponseTest -v
```

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverter.java gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverterResponseTest.java
git commit -m "feat: ProtocolConverter 非流式响应转换（OpenAI↔Anthropic）"
```

---

### Task 7: ProtocolConverter — 流式 chunk 转换

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverter.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverterStreamTest.java`

- [ ] **Step 1: 编写流式 chunk 转换测试**

```java
package com.codingas.gateway.domain.proxy.protocol;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProtocolConverterStreamTest {

    private final ProtocolConverter converter = new ProtocolConverter();

    @Test
    void shouldConvertOpenAIChunkToAnthropic_contentDelta() {
        // OpenAI SSE chunk: data: {"choices":[{"delta":{"content":"Hi"}}]}
        String openaiChunk = "{\"id\":\"chatcmpl-1\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Hi\"},\"finish_reason\":null}]}";

        String anthropicChunk = converter.convertStreamChunk(openaiChunk, "openai", "anthropic");

        assertThat(anthropicChunk).isNotNull();
        assertThat(anthropicChunk).contains("content_block_delta");
        assertThat(anthropicChunk).contains("Hi");
    }

    @Test
    void shouldConvertAnthropicChunkToOpenAI_contentDelta() {
        // Anthropic SSE event: event: content_block_delta, data: {"delta":{"text":"Hi"}}
        String anthropicChunk = "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"Hi\"}}";

        String openaiChunk = converter.convertStreamChunk(anthropicChunk, "anthropic", "openai");

        assertThat(openaiChunk).isNotNull();
        assertThat(openaiChunk).contains("choices");
        assertThat(openaiChunk).contains("Hi");
    }

    @Test
    void shouldReturnNullForUnconvertibleChunk() {
        // 空或无效 chunk 返回 null
        assertThat(converter.convertStreamChunk("", "openai", "anthropic")).isNull();
        assertThat(converter.convertStreamChunk(null, "openai", "anthropic")).isNull();
    }

    @Test
    void shouldConvertStreamDone() {
        // OpenAI [DONE] → Anthropic 无标记（返回最后一个 message_delta 事件）
        String result = converter.convertStreamDone("openai", "anthropic");
        assertThat(result).contains("message_delta");

        // Anthropic → OpenAI [DONE]
        String result2 = converter.convertStreamDone("anthropic", "openai");
        assertThat(result2).isEqualTo("[DONE]");
    }

    @Test
    void shouldPassThroughSameProtocolChunk() {
        String chunk = "{\"id\":\"chatcmpl-1\",\"choices\":[{\"delta\":{\"content\":\"Hi\"}}]}";
        // 同协议不转换，直接返回
        String result = converter.convertStreamChunk(chunk, "openai", "openai");
        assertThat(result).isEqualTo(chunk);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw test -pl gateway-boot -Dtest=ProtocolConverterStreamTest -v
```

- [ ] **Step 3: 实现流式 chunk 转换**

关键逻辑：
- `convertStreamChunk(rawChunk, from, to)`：解析 JSON → 按协议类型提取增量内容 → 按目标协议格式构建 JSON
- `convertStreamDone(from, to)`：结束标记适配
- 同协议直接返回原 chunk（零拷贝透传）
- `convertStreamChunk` 返回 null 表示无效/空 chunk，调用方跳过

- [ ] **Step 4: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest=ProtocolConverterStreamTest -v
```

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverter.java gateway-boot/src/test/java/com/codingas/gateway/domain/proxy/protocol/ProtocolConverterStreamTest.java
git commit -m "feat: ProtocolConverter 流式 chunk 转换（OpenAI↔Anthropic SSE）"
```

---

### Task 8: ProtocolGatewayFactory 接口 + 实现

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/gateway/ProtocolGatewayFactory.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/proxy/gateway/ProtocolGatewayFactoryImpl.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/gateway/ProtocolGatewayRegistry.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/proxy/gateway/ProtocolGatewayRegistryImpl.java`

- [ ] **Step 1: 创建 ProtocolGatewayFactory 接口**

```java
package com.codingas.gateway.domain.proxy.gateway;

import com.codingas.gateway.domain.proxy.protocol.ProtocolRequest;
import com.codingas.gateway.domain.proxy.protocol.ProtocolResponse;

import java.util.List;

/**
 * 协议网关工厂，按协议类型创建绑定 Provider 配置的 Gateway 实例
 */
public interface ProtocolGatewayFactory {

    /**
     * 创建绑定特定 Provider 配置的 ProtocolGateway 实例
     *
     * @param protocol       协议标识（"openai" / "anthropic"）
     * @param baseUrl        上游 Base URL
     * @param apiKey         上游 API Key
     * @param timeoutSeconds 超时秒数
     * @return 绑定配置的 ProtocolGateway 实例
     */
    ProtocolGateway create(String protocol, String baseUrl, String apiKey, int timeoutSeconds);

    /**
     * 获取系统支持的所有协议标识
     */
    List<String> getSupportedProtocols();
}
```

- [ ] **Step 2: 修改 ProtocolGateway 接口签名**

将 `ProtocolGateway.java` 的方法签名改为：

```java
package com.codingas.gateway.domain.proxy.gateway;

import com.codingas.gateway.domain.proxy.protocol.ProtocolRequest;
import com.codingas.gateway.domain.proxy.protocol.ProtocolResponse;
import com.codingas.gateway.domain.proxy.valueobject.ConnectivityTestResultVO;

/**
 * 协议网关接口，负责调用上游 LLM API
 */
public interface ProtocolGateway {

    /**
     * 非流式调用
     */
    ProtocolResponse chat(ProtocolRequest request);

    /**
     * 流式调用
     */
    void chatStream(ProtocolRequest request, StreamCallback callback);

    /**
     * 连通性测试（测试已绑定 Provider 的连通性）
     */
    ConnectivityTestResultVO testConnectivity();
}
```

删除所有方法签名中的 baseUrl/apiKey/timeoutSeconds 参数。

- [ ] **Step 3: 创建 ProtocolGatewayFactoryImpl**

基于当前 `ProtocolGatewayRegistryImpl` 的逻辑，改为工厂模式：

```java
package com.codingas.gateway.infrastructure.proxy.gateway;

import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayFactory;
import com.codingas.gateway.infrastructure.proxy.gateway.protocol.AnthropicProtocolGateway;
import com.codingas.gateway.infrastructure.proxy.gateway.protocol.OpenAIProtocolGateway;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 协议网关工厂实现
 */
@Component
public class ProtocolGatewayFactoryImpl implements ProtocolGatewayFactory {

    private final OkHttpClient httpClient;

    public ProtocolGatewayFactoryImpl(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public ProtocolGateway create(String protocol, String baseUrl, String apiKey, int timeoutSeconds) {
        return switch (protocol) {
            case "openai" -> new OpenAIProtocolGateway(httpClient, baseUrl, apiKey, timeoutSeconds);
            case "anthropic" -> new AnthropicProtocolGateway(httpClient, baseUrl, apiKey, timeoutSeconds);
            default -> throw new IllegalArgumentException("不支持的协议: " + protocol);
        };
    }

    @Override
    public List<String> getSupportedProtocols() {
        return List.of("openai", "anthropic");
    }
}
```

- [ ] **Step 4: 删除 ProtocolGatewayRegistry 接口和实现**

删除以下文件：
- `domain/proxy/gateway/ProtocolGatewayRegistry.java`
- `infrastructure/proxy/gateway/ProtocolGatewayRegistryImpl.java`

- [ ] **Step 5: 适配 OpenAIProtocolGateway + AnthropicProtocolGateway**

1. 修改构造函数：接收 `OkHttpClient httpClient, String baseUrl, String apiKey, int timeoutSeconds`
2. 将这些值存为实例字段
3. 修改 `chat()` 签名为 `ProtocolResponse chat(ProtocolRequest request)`，内部强转为 `OpenAIChatRequest`
4. 修改 `chatStream()` 签名为 `void chatStream(ProtocolRequest request, StreamCallback callback)`
5. 修改 `testConnectivity()` 为无参，使用实例字段中的配置
6. `buildRequestBody()` 改用 Jackson ObjectMapper 直接序列化 `OpenAIChatRequest`
7. `parseResponse()` 改用 Jackson 直接反序列化为 `OpenAIChatResponse`
8. 删除所有 Map<String, Object> 手拼/手拆代码

AnthropicProtocolGateway 同理。

- [ ] **Step 6: 编译确认**

```bash
./mvnw compile -pl gateway-boot
```

修复所有编译错误。此时还有引用 Registry 的文件需要改为 Factory，先暂存。

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "refactor: ProtocolGatewayRegistry → ProtocolGatewayFactory + Gateway 签名瘦身"
```

---

### Task 9: 适配所有 Registry 引用方

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ProtocolController.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/actuator/ProviderHealthTracker.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProductRoutingService.java`
- Modify: 以及所有引用 ProtocolGatewayRegistry 的文件

- [ ] **Step 1: 全局搜索 ProtocolGatewayRegistry 引用**

```bash
grep -r "ProtocolGatewayRegistry" gateway-boot/src/ --include="*.java" -l
```

- [ ] **Step 2: 逐文件适配**

对每个引用文件：
- 将 `ProtocolGatewayRegistry` 注入替换为 `ProtocolGatewayFactory`
- 将 `registry.getGateway(protocol).chat(...)` 替换为 `factory.create(protocol, baseUrl, apiKey, timeout).chat(...)`
- `getAllGateways()` 替换为 `factory.getSupportedProtocols()`
- `validateApiKeyFormat()` 等元数据方法：直接在 factory 实现中提供，或移到 Controller 层简单处理

- [ ] **Step 3: 编译确认**

```bash
./mvnw compile -pl gateway-boot
```

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "refactor: 所有 Registry 引用方适配 ProtocolGatewayFactory"
```

---

### Task 10: 重构 ProxyServiceImpl — 核心调用流程

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProxyServiceImpl.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/ProxyService.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/ProxyServiceTest.java`

- [ ] **Step 1: 重写 ProxyService 接口**

将方法签名从使用 `LLMRequest`/`LLMResponse` 改为使用协议 DTO：

```java
public interface ProxyService {
    /**
     * 非流式代理调用
     */
    ProtocolResponse proxy(ProtocolRequest request, AuthResult authResult);

    /**
     * 流式代理调用
     */
    void proxyStream(ProtocolRequest request, AuthResult authResult, StreamCallback callback);
}
```

- [ ] **Step 2: 重写 ProxyServiceImpl**

核心逻辑变更：
1. 注入 `ProtocolGatewayFactory`、`ProtocolConverter`
2. 注入 `OpenAIProtocolValidator`、`AnthropicProtocolValidator`
3. `proxy()` 方法：
   - 入站校验：按 request.getProtocol() 选 Validator 校验
   - 路由决策：获取 RoutingContext（含出站协议、baseUrl、apiKey、timeout）
   - 协议转换：入站≠出站时调用 ProtocolConverter
   - 创建 Gateway：`factory.create(targetProtocol, baseUrl, apiKey, timeout)`
   - 调用：`gateway.chat(convertedRequest)`
   - 响应转换：入站≠出站时调用 ProtocolConverter
   - 计费/审计：从 ProtocolResponse 提取 usage（按协议类型分别提取）
   - 返回入站协议对应的 Response
4. `proxyStream()` 方法同理，但使用 wrapped callback 做流式 chunk 转换

- [ ] **Step 3: 更新 ProxyServiceTest**

将现有测试中的 `LLMRequest`/`LLMResponse` 替换为协议 DTO，Mock 对象同步更新。

- [ ] **Step 4: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -Dtest=ProxyServiceTest -v
```

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "refactor: ProxyServiceImpl 改用 ProtocolConverter + ProtocolGatewayFactory"
```

---

### Task 11: 适配 Controller 层

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/OpenAIController.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/AnthropicController.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ExperienceController.java`

- [ ] **Step 1: 适配 OpenAIController**

1. 将方法签名从接收 `@RequestBody String rawBody` 改为 `@RequestBody OpenAIChatRequest request`
2. 调用 `proxyService.proxy(request, authResult)` 返回 `ProtocolResponse`
3. 将 `ProtocolResponse` 强转为 `OpenAIChatResponse` 返回
4. 增加 try-catch 捕获 `ProtocolValidationException`，转换为 OpenAI 格式错误响应
5. 流式同理

- [ ] **Step 2: 适配 AnthropicController**

同上，但使用 `AnthropicMessagesRequest`/`AnthropicMessagesResponse`。

- [ ] **Step 3: 适配 ExperienceController**

1. `ModelExperienceService` 方法签名改为接收/返回协议 DTO
2. 内部改用 `ProtocolGatewayFactory.create()` + `gateway.chatStream()`
3. 删除所有 `LLMRequest` 手动构建代码

- [ ] **Step 4: 编译 + 运行测试确认**

```bash
./mvnw compile -pl gateway-boot
./mvnw test -pl gateway-boot -v
```

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "refactor: Controller 层适配新协议 DTO 流程"
```

---

### Task 12: 删除冗余类 + 清理

**Files:**
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/dto/LLMRequest.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/dto/LLMResponse.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/valueobject/LLMRequestVO.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/valueobject/LLMResponseVO.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/proxy/valueobject/ChatResponseVO.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/dto/chat/ChatRequest.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/dto/chat/ChatResponse.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/dto/LLMDtoConverter.java`

- [ ] **Step 1: 搜索所有待删除类的引用**

```bash
grep -r "LLMRequest\|LLMResponse\|LLMRequestVO\|LLMResponseVO\|ChatResponseVO\|chat\.ChatRequest\|chat\.ChatResponse\|LLMDtoConverter" gateway-boot/src/ --include="*.java" -l
```

确认没有遗漏的引用。如有，先适配再删除。

- [ ] **Step 2: 删除冗余文件**

删除上述 8 个文件。

- [ ] **Step 3: 清理空目录**

如果 `application/proxy/dto/chat/` 目录为空，删除它。
如果 `domain/proxy/valueobject/` 目录为空，删除它。

- [ ] **Step 4: 全量编译 + 测试**

```bash
./mvnw clean test -pl gateway-boot -v
```

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "refactor: 删除 LLMRequest/LLMResponse/VO/ChatRequest/ChatResponse/LLMDtoConverter 冗余类"
```

---

### Task 13: Infrastructure Gateway 测试适配

**Files:**
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/proxy/gateway/protocol/OpenAIProtocolGatewayTest.java`
- Modify: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/proxy/gateway/protocol/AnthropicProtocolGatewayTest.java`
- Delete: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/proxy/gateway/protocol/ProtocolGatewayRegistryImplTest.java`

- [ ] **Step 1: 适配 OpenAIProtocolGatewayTest**

1. 改为直接 `new OpenAIProtocolGateway(httpClient, baseUrl, apiKey, timeout)` 创建实例
2. 测试 `chat(ProtocolRequest)` 签名
3. 改用 `OpenAIChatRequest` 作为入参

- [ ] **Step 2: 适配 AnthropicProtocolGatewayTest**

同上。

- [ ] **Step 3: 删除 ProtocolGatewayRegistryImplTest**

因为 Registry 已被 Factory 替代。

- [ ] **Step 4: 运行测试确认通过**

```bash
./mvnw test -pl gateway-boot -v
```

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "test: 适配 Gateway 测试到新签名 + 删除 RegistryImplTest"
```

---

### Task 14: 端到端验证

**Files:** 无新增/修改

- [ ] **Step 1: 全量编译**

```bash
./mvnw clean compile -pl gateway-boot
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 全量测试**

```bash
./mvnw clean test -pl gateway-boot
```

Expected: ALL TESTS PASS

- [ ] **Step 3: 检查分层合规**

```bash
# domain 层不应依赖 application 层
grep -r "import com.codingas.gateway.application" gateway-boot/src/main/java/com/codingas/gateway/domain/ --include="*.java"
```

Expected: 无输出（domain 层不引用 application 层任何类）

- [ ] **Step 4: 提交最终状态**

```bash
git add -A
git commit -m "chore: 协议 DTO 重构完成 — 入站出站共用 + domain 层下沉"
```
