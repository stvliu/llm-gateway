# COLA Light 5.0 重构实施计划 - Phase 3

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

**Goal:** 完成 Router 领域 UseCase 实现，实现 Controller 与 Domain Service 的连接

**Architecture:** COLA Light 5.0 单模块架构，用 package 代替模块划分层次

---

## Phase 3 迁移清单

### Task A: 完成 ModelManageUseCase 实现

**Files:**
- `gateway-boot/src/main/java/com/codingas/gateway/application/model/ModelManageUseCase.java`

**Current State:** Stub 实现，抛出 UnsupportedOperationException

**Target State:** 委托给 domain/router/service/ModelService

- [x] 实现 findAll() - 调用 ModelService.findAll()
- [x] 实现 findById() - 调用 ModelService.findById()
- [x] 实现 create() - 调用 ModelService.create()
- [x] 实现 update() - 调用 ModelService.update()
- [x] 实现 delete() - 调用 ModelService.delete()

---

### Task B: 完成 ProviderManageUseCase 实现

**Files:**
- `gateway-boot/src/main/java/com/codingas/gateway/application/model/ProviderManageUseCase.java`

**Current State:** Stub 实现，抛出 UnsupportedOperationException

**Target State:** 委托给 domain/router/service/ProviderService

- [x] 实现 findAll() - 调用 ProviderService.findAll()
- [x] 实现 findById() - 调用 ProviderService.findById()
- [x] 实现 create() - 调用 ProviderService.create()
- [x] 实现 update() - 调用 ProviderService.update()
- [x] 实现 delete() - 调用 ProviderService.delete()

---

### Task C: 完成 LLMChatUseCase 实现

**Files:**
- `gateway-boot/src/main/java/com/codingas/gateway/application/chat/LLMChatUseCase.java`
- `gateway-boot/src/main/java/com/codingas/gateway/domain/router/service/LLMDispatcher.java`
- `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/adapter/StreamCallbackImpl.java`

**Current State:** Stub 实现，依赖于 LLMDispatcher

**Target State:** 完整实现 send() 和 sendStream() 方法

- [x] 创建 LLMDispatcher 服务
- [x] 创建 StreamCallbackImpl 实现
- [x] 实现 send() - 调用 LLMDispatcher
- [x] 实现 sendStream() - 调用 LLMDispatcher 流式接口
- [x] 确保 TokenUsedEvent 正确发布

---

### Task D: 完成 ChatApplication 实现

**Files:**
- `gateway-boot/src/main/java/com/codingas/gateway/application/chat/ChatApplication.java`

**Current State:** Placeholder 实现

**Target State:** 完整实现聊天请求处理

- [x] 完善 chat() 方法实现
- [x] 实现 chatStream() 方法
- [x] 集成 LLMChatUseCase

---

### Task E: 全量编译和测试

- [x] 全量编译
- [x] 运行测试
- [ ] 提交

---

## Self-Review Checklist

1. **Spec coverage:** 所有 UseCase 已实现 ✅ / ❌
2. **Placeholder scan:** 无 UnsupportedOperationException
3. **Type consistency:** Gateway 接口在 domain/xxx/gateway/
4. **Test coverage:** 全项目编译通过，测试通过
