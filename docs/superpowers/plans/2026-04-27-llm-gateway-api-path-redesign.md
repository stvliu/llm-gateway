# LLM Gateway API 双兼容路径实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修改 AnthropicController 的 @RequestMapping 从 `/v1` 改为 `/anthropic/v1`，实现 OpenAI SDK 和 Anthropic SDK 的双兼容路径。

**Architecture:** 通过路径前缀分离两种 API 格式兼容。OpenAIController 保持 `/v1` 前缀不变，AnthropicController 改为 `/anthropic/v1` 前缀。两个 Controller 内部共用同一个 LLMChatUseCase，只是请求/响应格式转换不同。

**Tech Stack:** Java 21, Spring Boot 3.5.x

---

## 变更范围

| 文件 | 改动 |
|------|------|
| `src/main/java/com/codingas/gateway/adapter/chat/controller/AnthropicController.java` | `@RequestMapping` 从 `/v1` 改为 `/anthropic/v1` |

---

### Task 1: 修改 AnthropicController 路径映射

**Files:**
- Modify: `src/main/java/com/codingas/gateway/adapter/chat/controller/AnthropicController.java:30`

- [ ] **Step 1: 修改 @RequestMapping 注解**

将第 30 行的 `@RequestMapping("/v1")` 改为 `@RequestMapping("/anthropic/v1")`

```java
// 修改前
@RequestMapping("/v1")
public class AnthropicController {

// 修改后
@RequestMapping("/anthropic/v1")
public class AnthropicController {
```

同时更新类注释（第 22-26 行）以反映新的路径：

```java
/**
 * Anthropic 兼容 API 控制器
 *
 * <p>暴露 /anthropic/v1/messages 端点，兼容 Anthropic API 格式。</p>
 *
 * @see <a href="https://docs.anthropic.com/en/api/reference/messages">Anthropic Messages API</a>
 */
```

- [ ] **Step 2: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -q
```

预期：编译成功，无错误

- [ ] **Step 3: 运行相关测试**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw test -Dtest=*Anthropic* -q
```

预期：所有 Anthropic 相关测试通过

- [ ] **Step 4: 提交更改**

```bash
git add src/main/java/com/codingas/gateway/adapter/chat/controller/AnthropicController.java
git commit -m "$(cat <<'EOF'
refactor: change Anthropic API path from /v1 to /anthropic/v1

实现双 API 兼容路径设计：
- OpenAI: /v1/chat/completions
- Anthropic: /anthropic/v1/messages

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
EOF
)"
```

---

## 验证清单

- [ ] `POST /anthropic/v1/messages` 端点可访问
- [ ] OpenAI SDK 调用 `POST /v1/chat/completions` 正常
- [ ] Anthropic SDK 调用 `POST /anthropic/v1/messages` 正常
- [ ] 流式响应在两个端点均正常
