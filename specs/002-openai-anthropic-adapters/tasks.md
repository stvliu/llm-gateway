# Tasks: OpenAI和Anthropic双适配器

**Input**: Design documents from `/specs/002-openai-anthropic-adapters/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/openai-contract.md, contracts/anthropic-contract.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and dependency configuration

- [x] T001 [P] Add Spring MVC, RestClient, and OkHttp dependencies in gateway-adapter/pom.xml
- [x] T002 [P] Configure application.yml for OpenAI and Anthropic adapter settings in gateway-application/src/main/resources/
- [x] T003 [P] Add OkHttp MockWebServer test dependency in gateway-adapter/pom.xml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core adapter interface and streaming infrastructure that MUST be complete before ANY user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Update LLMProviderAdapter interface with Spring MVC signatures (add StreamCallback) in gateway-adapter/src/main/java/com/codingas/gateway/adapter/LLMProviderAdapter.java
- [x] T005 Create StreamCallback interface for OkHttp streaming in gateway-adapter/src/main/java/com/codingas/gateway/adapter/StreamCallback.java
- [x] T006 [P] Create LLMRequest DTO enhancements (tools, toolChoice fields) in gateway-adapter/src/main/java/com/codingas/gateway/adapter/dto/LLMRequest.java (already complete)
- [x] T007 [P] Create LLMResponse DTO enhancements (toolCalls, extraData fields) in gateway-adapter/src/main/java/com/codingas/gateway/adapter/dto/LLMResponse.java (already complete)
- [x] T008 Create ProviderCapabilities record in gateway-adapter/src/main/java/com/codingas/gateway/adapter/dto/ProviderCapabilities.java (already complete)
- [x] T009 Create ProviderException class in gateway-adapter/src/main/java/com/codingas/gateway/adapter/common/ProviderException.java (already complete)
- [x] T009b [P] Create AES-256 encryption utility in gateway-core/src/main/java/com/codingas/gateway/core/infrastructure/encryption/Aes256EncryptionService.java
- [x] T009c [P] Implement CredentialsLoader to load encrypted API keys from database in gateway-adapter/src/main/java/com/codingas/gateway/adapter/common/CredentialsLoader.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - 企业用户通过统一网关调用多种模型 (Priority: P1) 🎯 MVP

**Goal**: 支持 OpenAI `/v1/chat/completions` 和 Anthropic `/v1/messages` 双端点，实现基本的 chat 和 messages 方法

**Independent Test**: 发送 OpenAI 格式请求到 `/v1/chat/completions` 返回 OpenAI 标准格式响应；发送 Anthropic 格式请求到 `/v1/messages` 返回 Anthropic 标准格式响应

### Implementation for User Story 1

- [x] T010 [P] [US1] Implement OpenAIAdapter.chat() with RestClient in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java
- [x] T011 [P] [US1] Implement OpenAIAdapter.chatStream() with OkHttp in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java
- [x] T012 [P] [US1] Implement AnthropicAdapter.messages() with RestClient in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java
- [x] T013 [P] [US1] Implement AnthropicAdapter.messagesStream() with OkHttp in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java
- [x] T014 [US1] Implement OpenAIAdapter.isAvailable() and isHealthy() in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java
- [x] T015 [US1] Implement AnthropicAdapter.isAvailable() and isHealthy() in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java
- [x] T016 [US1] Implement OpenAIAdapter.getCapabilities() in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java
- [x] T017 [US1] Implement AnthropicAdapter.getCapabilities() in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java

**Checkpoint**: User Story 1 fully functional - OpenAI and Anthropic adapters can handle basic non-streaming and streaming requests

---

## Phase 4: User Story 2 - 模型提供商动态切换 (Priority: P2)

**Goal**: 实现提供商可用性检查和健康检测，支持故障时的自动切换

**Independent Test**: 模拟主提供商超时，验证请求自动切换到备用提供商

### Implementation for User Story 2

- [x] T018 [P] [US2] Add provider health check endpoint support in LLMProviderAdapter interface
- [x] T019 [US2] Enhance OpenAIAdapter with connection check for health status in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java
- [x] T020 [US2] Enhance AnthropicAdapter with connection check for health status in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java
- [x] T021 [US2] Add timeout configuration per request in LLMRequest in gateway-adapter/src/main/java/com/codingas/gateway/adapter/dto/LLMRequest.java

**Checkpoint**: User Story 2 complete - adapters support health checking for failover scenarios

---

## Phase 5: User Story 3 - 流式响应实时返回 (Priority: P3)

**Goal**: 实现 SSE 格式流式响应，支持实时推送和取消

**Independent Test**: 向流式端点发送请求，验证逐步收到 SSE 格式响应片段

### Implementation for User Story 3

- [x] T022 [P] [US3] Implement SSE parsing in OpenAIAdapter.chatStream() for data: lines in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java (via OkHttp EventSourceListener)
- [x] T023 [P] [US3] Implement SSE parsing in AnthropicAdapter.messagesStream() for data: lines in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java (via OkHttp EventSourceListener)
- [x] T024 [US3] Handle stream cancellation (connection close) in OkHttp callback in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java (via onFailure callback)
- [x] T025 [US3] Handle stream cancellation in AnthropicAdapter in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java (via onFailure callback)

**Checkpoint**: User Story 3 complete - streaming works with proper SSE parsing and cancellation

---

## Phase 6: User Story 4 - 请求和响应的 Token 精确计量 (Priority: P2)

**Goal**: 实现准确的 Token 统计，包含在响应中返回

**Independent Test**: 发送已知内容的请求，验证响应的 usage 中包含准确的 prompt_tokens、completion_tokens

### Implementation for User Story 4

- [x] T026 [P] [US4] Parse OpenAI usage field from response in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java (parseUsage method)
- [x] T027 [P] [US4] Parse Anthropic usage field (usage.prompt_tokens, usage.completion_tokens) in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java (parseUsage method)
- [x] T028 [US4] Populate LLMResponse.usage with accurate token counts in gateway-adapter/src/main/java/com/codingas/gateway/adapter/dto/LLMResponse.java (via parseResponse)

**Checkpoint**: User Story 4 complete - token usage accurately tracked and returned

---

## Phase 7: User Story 5 - Function Calling / Tool Use (Priority: P2)

**Goal**: 支持 OpenAI Function Calling 和 Anthropic Tool Use

**Independent Test**: 发送包含 tools 参数的请求，验证适配器正确转发并处理工具调用响应

### Implementation for User Story 5

- [x] T029 [P] [US5] Implement OpenAI function call request building in OpenAIAdapter in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java (buildRequestBody with tools/tool_choice)
- [x] T030 [P] [US5] Implement OpenAI function call response parsing in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java (parseContent with toolCalls)
- [x] T031 [P] [US5] Implement Anthropic tool_use request building in AnthropicAdapter in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java (buildMessagesRequestBody with tools)
- [x] T032 [P] [US5] Implement Anthropic tool_use response parsing in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java (parseContent with tool_use block handling)
- [x] T033 [US5] Add streaming function call/tool_use support in chatStream/messagesStream in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java (SSE streaming via EventSourceListener)
- [x] T034 [US5] Add streaming tool_use support in AnthropicAdapter.messagesStream() in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java (SSE streaming via EventSourceListener)
- [ ] T032 [P] [US5] Implement Anthropic tool_use response parsing in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java
- [ ] T033 [US5] Add streaming function call/tool_use support in chatStream/messagesStream in gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java
- [ ] T034 [US5] Add streaming tool_use support in AnthropicAdapter.messagesStream() in gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java

**Checkpoint**: User Story 5 complete - Function Calling and Tool Use fully supported

---

## Phase 8: User Story 6 - 跨提供商协议转换 (Priority: P3)

**Goal**: 实现 OpenAI ↔ Anthropic 双向协议转换

**Independent Test**: 用 OpenAI 格式请求 Anthropic 模型，验证返回 OpenAI 格式响应

### Implementation for User Story 6

- [x] T035 [P] [US6] Create ProtocolTranslator in gateway-dispatch/src/main/java/com/codingas/gateway/dispatch/ProtocolTranslator.java
- [x] T036 [P] [US6] Implement toAnthropicFormat() for OpenAI → Anthropic conversion in gateway-dispatch/src/main/java/com/codingas/gateway/dispatch/ProtocolTranslator.java
- [x] T037 [P] [US6] Implement toOpenAIFormat() for Anthropic → OpenAI conversion in gateway-dispatch/src/main/java/com/codingas/gateway/dispatch/ProtocolTranslator.java
- [x] T038 [US6] Implement fromAnthropicResponse() response conversion in gateway-dispatch/src/main/java/com/codingas/gateway/dispatch/ProtocolTranslator.java
- [x] T039 [US6] Implement fromOpenAIResponse() response conversion in gateway-dispatch/src/main/java/com/codingas/gateway/dispatch/ProtocolTranslator.java

**Checkpoint**: User Story 6 complete - bidirectional protocol translation works

---

## Phase 9: User Story 7 - 一致的错误格式体验 (Priority: P2)

**Goal**: 根据请求来源适配错误格式

**Independent Test**: 发送会导致错误的请求，验证错误响应格式与请求格式一致

### Implementation for User Story 7

- [x] T040 [P] [US7] Create ErrorResponseAdapter in gateway-dispatch/src/main/java/com/codingas/gateway/dispatch/ErrorResponseAdapter.java
- [x] T041 [P] [US7] Implement toOpenAIError() returning OpenAI error format in gateway-dispatch/src/main/java/com/codingas/gateway/dispatch/ErrorResponseAdapter.java
- [x] T042 [P] [US7] Implement toAnthropicError() returning Anthropic error format in gateway-dispatch/src/main/java/com/codingas/gateway/dispatch/ErrorResponseAdapter.java
- [x] T043 [US7] Integrate ErrorResponseAdapter into dispatch layer error handling in gateway-dispatch/

**Checkpoint**: User Story 7 complete - error responses match request format

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T044 [P] Add OpenTelemetry tracing instrumentation to all adapter methods
- [x] T045 [P] Add structured logging for all provider calls
- [x] T046 Add unit tests for ProtocolTranslator in gateway-dispatch/src/test/java/
- [x] T047 Add unit tests for ErrorResponseAdapter in gateway-dispatch/src/test/java/
- [x] T048 Add integration tests with MockWebServer for OpenAIAdapter in gateway-adapter/src/test/java/
- [x] T049 Add integration tests with MockWebServer for AnthropicAdapter in gateway-adapter/src/test/java/
- [x] T050 Update quickstart.md with final API examples
- [x] T050b [P] Add performance test to verify 10,000 QPS throughput target in gateway-adapter/src/test/java/

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-9)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Phase 10)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational - No dependencies on other stories
- **User Story 3 (P3)**: Can start after Foundational - No dependencies on other stories
- **User Story 4 (P2)**: Can start after Foundational - No dependencies on other stories
- **User Story 5 (P2)**: Can start after Foundational - No dependencies on other stories
- **User Story 6 (P3)**: Can start after Foundational - No dependencies on other stories
- **User Story 7 (P2)**: Can start after Foundational - No dependencies on other stories

### Within Each User Story

- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All implementation tasks for a user story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tasks for User Story 1 together:
Task T010: Implement OpenAIAdapter.chat() with RestClient
Task T011: Implement OpenAIAdapter.chatStream() with OkHttp
Task T012: Implement AnthropicAdapter.messages() with RestClient
Task T013: Implement AnthropicAdapter.messagesStream() with OkHttp
# These 4 tasks can be done in parallel by 4 developers
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Continue with remaining stories...
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 5 (Function Calling)
   - Developer C: User Story 6 (Protocol Translation)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
