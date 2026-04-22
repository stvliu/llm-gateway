# Tasks: Provider Adapter Framework

**Input**: Design documents from `/specs/001-provider-adapter/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/adapter-interface.md, research.md

## Phase 1: Setup

**Goal**: Initialize Java 21 + Spring Boot 3.5.x project structure with Maven

- [ ] T001 Create Maven project structure `gateway/pom.xml` with Java 21, Spring Boot 3.5.0 parent
- [ ] T002 Configure dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, lombok, jackson-databind
- [ ] T003 Create package structure `com.codingas.gateway.adapter`, `com.codingas.gateway.domain.channel`, `com.codingas.gateway.service`, `com.codingas.gateway.infrastructure.persistence`, `com.codingas.gateway.common.exception`
- [ ] T004 Configure H2 in-memory database for development in `application-dev.yml`
- [ ] T005 Create `application-local.yml` with external configuration template
- [ ] T006 Add Maven wrapper (`mvnw`) to ensure consistent builds

---

## Phase 2: Foundational

**Goal**: Create domain entities, value objects, and base exceptions that all user stories depend on

- [ ] T007 Create base entity `AuditEntity` with `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted_by`, `deleted_at` fields in `com.codingas.gateway.domain.audit`
- [ ] T008 Create `Provider` entity in `com.codingas.gateway.domain.channel` with fields: id, provider_code, provider_name, provider_type (ENUM), base_url, priority, status
- [ ] T009 Create `Model` entity in `com.codingas.gateway.domain.channel` with fields: id, model_code, provider_id (FK), provider_model_id, display_name, context_window, input_price, output_price, capabilities (JSON), status
- [ ] T010 Create `Channel` entity in `com.codingas.gateway.domain.channel` with fields: id, channel_code, channel_name, provider_id (FK), group_id (FK), base_url, timeout, max_connections, status
- [ ] T011 Create `ChannelKey` entity in `com.codingas.gateway.domain.channel` with fields: id, channel_id (FK), api_key (encrypted), priority, status, last_used_at
- [ ] T012 Create `ChannelGroup` entity in `com.codingas.gateway.domain.channel` with fields: id, group_code, group_name, team_id, description
- [ ] T013 Create `ProviderType` enum: OPENAI, ANTHROPIC, GEMINI, ZHIPU, OTHER (note: may need adapter-specific variants in Phase 3)
- [ ] T014 Create `ChannelStatus` enum: ACTIVE, SUSPENDED, DELETED
- [ ] T015 Create `ChannelKeyStatus` enum: ACTIVE, EXHAUSTED, EXPIRED, DELETED
- [ ] T016 Create `ProviderException` in `com.codingas.gateway.common.exception` with fields: providerCode, modelId, errorType, retryable, traceId
- [ ] T017 Create `ProviderErrorType` enum: AUTHENTICATION_ERROR, RATE_LIMIT_ERROR, QUOTA_EXCEEDED, TIMEOUT_ERROR, INVALID_REQUEST, UPSTREAM_ERROR, NETWORK_ERROR, UNKNOWN_ERROR
- [ ] T018 Create JPA repositories: `ProviderRepository`, `ModelRepository`, `ChannelRepository`, `ChannelKeyRepository`, `ChannelGroupRepository`
- [ ] T019 Create `EncryptionService` interface in `com.codingas.gateway.infrastructure.encryption` (stub implementation for now)

---

## Phase 3: US1 - Adapter Framework Abstraction

**Goal**: Implement LLMProviderAdapter interface with SPI discovery mechanism

**Story**: 网关开发者需要一套标准接口，以便在不影响现有代码的情况下接入新的模型提供商

**Independent Test**: Write unit tests verifying new Provider implementation is discovered and usable without modifying framework code

### Interface & SPI

- [ ] T020 [P] Define `LLMProviderAdapter` interface in `com.codingas.gateway.adapter` with methods: chatCompletion(), messages(), embeddings(), getCapabilities(), isHealthy(), getProviderType()
- [ ] T021 [P] Define request/response records: `ChatCompletionRequest`, `ChatCompletionResult`, `MessagesRequest`, `MessagesResult`, `EmbeddingRequest`, `EmbeddingResult`
- [ ] T022 [P] Define `ProviderCapabilities` record with fields: providerType, supportsChatCompletion, supportsMessages, supportsEmbeddings, supportsStreaming, supportsFunctionCalling, supportedModels
- [ ] T023 [P] Update `ProviderType` enum to include adapter-specific variants (if needed for Provider types beyond T013 values)
- [ ] T024 Create `AdapterLoader` in `com.codingas.gateway.adapter.spi` using `ServiceLoader<LLMProviderAdapter>` for SPI discovery
- [ ] T025 Create `AdapterRegistry` service in `com.codingas.gateway.adapter` to manage registered adapters by ProviderType
- [ ] T026 Create SPI service file `META-INF/services/com.codingas.gateway.adapter.LLMProviderAdapter` with placeholder entries

### OpenAI Adapter (Reference Implementation)

- [ ] T027 [P] Implement `OpenAIAdapter` class implementing `LLMProviderAdapter` for OpenAI format
- [ ] T028 [P] Implement `chatCompletion()` method in OpenAIAdapter with HTTP call to OpenAI API endpoint
- [ ] T029 [P] Implement `messages()` method throwing UnsupportedOperationException (OpenAI doesn't support Anthropic format)
- [ ] T030 [P] Implement `embeddings()` method in OpenAIAdapter
- [ ] T031 [P] Implement `getCapabilities()` returning ProviderCapabilities with supportsChatCompletion=true, supportsMessages=false
- [ ] T032 [P] Implement `isHealthy()` with configurable health check endpoint
- [ ] T033 Update SPI file to include `com.codingas.gateway.adapter.OpenAIAdapter`

### Anthropic Adapter (Reference Implementation)

- [ ] T034 [P] Implement `AnthropicAdapter` class implementing `LLMProviderAdapter`
- [ ] T035 [P] Implement `messages()` method in AnthropicAdapter
- [ ] T036 [P] Implement `chatCompletion()` throwing UnsupportedOperationException (Anthropic doesn't support OpenAI format)
- [ ] T037 [P] Implement `embeddings()` - not supported by Anthropic, return empty result
- [ ] T038 [P] Implement `getCapabilities()` returning ProviderCapabilities with supportsChatCompletion=false, supportsMessages=true
- [ ] T039 [P] Implement `isHealthy()` with health check
- [ ] T040 Update SPI file to include `com.codingas.gateway.adapter.AnthropicAdapter`

### Unit Tests

- [ ] T041 Write `LLMProviderAdapterTest` verifying all required methods exist and have correct signatures
- [ ] T042 Write `AdapterLoaderTest` verifying SPI discovery loads OpenAI and Anthropic adapters
- [ ] T043 Write `AdapterRegistryTest` verifying getAdapter() returns correct adapter by type
- [ ] T044 Write `OpenAIAdapterTest` verifying capability reporting and method support
- [ ] T045 Write `AnthropicAdapterTest` verifying capability reporting and method support

---

## Phase 4: US2 - Provider Management

**Goal**: Implement Provider CRUD operations with hot reload capability

**Story**: 管理员需要通过管理界面配置模型提供商，包括 API 密钥、端点地址、优先级等

**Independent Test**: CRUD tests verifying admin can create/read/update/delete Provider configurations with hot reload

### Service Layer

- [ ] T046 Create `ProviderService` in `com.codingas.gateway.service` with CRUD methods: create(), findById(), findAll(), update(), delete()
- [ ] T047 Implement hot-reload notification mechanism using Spring's `ApplicationEventPublisher`
- [ ] T048 Create `ProviderConfig` @ConfigurationProperties class with refresh scope support

### Management API

- [ ] T049 [P] Create `ProviderController` in `com.codingas.gateway.api` with endpoints: POST /api/v1/providers, GET /api/v1/providers, GET /api/v1/providers/{id}, PUT /api/v1/providers/{id}, DELETE /api/v1/providers/{id}
- [ ] T050 [P] Create request DTOs: `CreateProviderRequest`, `UpdateProviderRequest`
- [ ] T051 [P] Create response DTOs: `ProviderResponse` with unified ApiResponse envelope
- [ ] T052 Implement API versioning with `/api/v1/` prefix
- [ ] T-EXT1 [P] Add Provider capabilities query endpoint GET /api/v1/providers/{id}/capabilities
  - Returns ProviderCapabilities from the Provider's registered adapter
  - Response follows unified ApiResponse format per spec.md §5.4

### Validation & Error Handling

- [ ] T053 Add validation constraints to Provider entity: provider_code unique, provider_name not blank, base_url valid URL format
- [ ] T054 Implement global exception handler for ProviderException with trace_id in response
- [ ] T055 Add @RefreshScope to ProviderConfig for hot reload without restart

### Unit Tests

- [ ] T056 Write `ProviderServiceTest` with mocked repository, verifying CRUD operations
- [ ] T057 Write `ProviderControllerTest` using MockMvc for API endpoint testing
- [ ] T058 Write hot reload test verifying config changes trigger EnvironmentChangeEvent

---

## Phase 5: US3 - Channel & ChannelKey Management

**Goal**: Implement Channel CRUD with multi-Key support and automatic failover

**Story**: 管理员需要为每个 Provider 创建多个渠道，每个渠道可有多个 API Key 实现密钥轮换

**Independent Test**: Multi-Key rotation test verifying automatic failover when active Key fails

### Service Layer

- [ ] T059 Create `ChannelService` in `com.codingas.gateway.service` with CRUD methods
- [ ] T060 Create `ChannelKeyService` in `com.codingas.gateway.service` with multi-Key operations
- [ ] T061 Create `ChannelKeySelector` implementing priority-based + health-aware Key selection
- [ ] T062 Implement `ChannelKeySelector.selectActiveKey(channelId)` returning Key with highest priority that is ACTIVE and healthy
- [ ] T063 Implement `ChannelKeySelector.markUnhealthy(keyId)` and automatic fallback to next available Key
- [ ] T064 Create `ChannelHealthMonitor` scheduled task checking Key health every 30 seconds

### Management API

- [ ] T065 [P] Create `ChannelController` with CRUD endpoints: /api/v1/channels
- [ ] T066 [P] Create `ChannelKeyController` with endpoints: /api/v1/channels/{channelId}/keys
- [ ] T067 [P] Create DTOs: `CreateChannelRequest`, `UpdateChannelRequest`, `CreateChannelKeyRequest`, `ChannelKeyResponse`
- [ ] T068 Implement Key rotation endpoint for manual rotation trigger

### State Management

- [ ] T069 Implement ChannelKey state transitions: ACTIVE → EXHAUSTED (on rate limit), ACTIVE → EXPIRED (on time), recovery logic
- [ ] T070 Create `ChannelKeyEvent` published when Key status changes (for health monitor synchronization)

### Unit Tests

- [ ] T071 Write `ChannelServiceTest` with mocked repository
- [ ] T072 Write `ChannelKeySelectorTest` verifying priority selection and failover
- [ ] T073 Write `ChannelHealthMonitorTest` verifying unhealthy Key detection and recovery

---

## Phase 6: US4 - Model Association

**Goal**: Implement Model entity and Provider-Model relationship management

**Story**: 系统需要维护 Provider 与其提供的模型之间的关联关系，支持模型映射

**Independent Test**: Test verifying Model list query returns correct models for a Provider

### Service Layer

- [ ] T074 Create `ModelService` in `com.codingas.gateway.service` with CRUD methods
- [ ] T075 Implement `findByProviderId(providerId)` for listing models per Provider
- [ ] T076 Implement `findByProviderModelId(providerId, providerModelId)` for lookup

### Management API

- [ ] T077 [P] Create `ModelController` with endpoints: /api/v1/models, /api/v1/providers/{providerId}/models
- [ ] T078 [P] Create DTOs: `CreateModelRequest`, `ModelResponse`
- [ ] T079 Implement model capabilities JSON parsing and validation

### Database Migration

- [ ] T080 Create Flyway migration V1__init_schema.sql creating providers, models, channels, channel_keys, channel_groups tables
- [ ] T081 Create Flyway migration V2__seed_providers.sql with 50+ pre-populated Provider and Model records (OpenAI, Anthropic, Google, etc.)

### Unit Tests

- [ ] T082 Write `ModelServiceTest` with mocked repository
- [ ] T083 Write `ModelControllerTest` using MockMvc

---

## Phase 7: Polish & Cross-Cutting Concerns

**Goal**: Integration verification, documentation, and final quality checks

- [ ] T084 Create AdapterRoutingIntegration stub in `com.codingas.gateway.dispatch` package
  - Defines interface for future routing layer integration (RT-001 to RT-010 per spec.md §4.2.8)
  - Implements only getAdapter() method for now
  - Doc: "Placeholder for RT module integration, full routing in future phase"
- [ ] T085 Add OpenTelemetry tracing to all adapter methods (traceId propagation)
- [ ] T086 Create integration test `AdapterLoaderIntegrationTest` verifying all adapters load correctly
- [ ] T087 Verify SC-001: New adapter integration time ≤2 hours (document process in quickstart.md)
- [ ] T088 Verify SC-003: Hot reload delay ≤100ms (add performance test)
- [ ] T089 Verify SC-004: Failover time ≤500ms (add failover test)
- [ ] T090 Add logging for adapter discovery at startup (INFO level)
- [ ] T091 Ensure API response follows unified ApiResponse format defined in spec.md §5.4
- [ ] T092 Final code review checklist validation
- [ ] T-P1 [P] Add parameter transformation accuracy test verifying ≥99.9% accuracy
  - Create `TransformationAccuracyTest.java` in `gateway/src/test/java/com/codingas/gateway/adapter/`
  - Test OpenAI → internal → OpenAI roundtrip for: messages, temperature, maxTokens, stop, stream
  - Test Anthropic → internal → Anthropic roundtrip for: messages, maxTokens, temperature, systemPrompt
  - Assert accuracy ≥99.9% (no field loss or corruption)

---

## Dependency Graph

```
Phase 1 (Setup)
    │
    ▼
Phase 2 (Foundational) ───────────────────────────────────────┐
    │                                                           │
    ▼                                                           │
Phase 3 (US1 - Adapter Framework)                              │
    │                                                           │
    ▼                                                           │
Phase 4 (US2 - Provider Management) ───────────────────────────┤
    │                                                           │
    ▼                                                           │
Phase 5 (US3 - Channel & ChannelKey) ──────────────────────────┤
    │                                                           │
    ▼                                                           │
Phase 6 (US4 - Model Association) ───────────────────────────────┤
    │                                                           │
    ▼                                                           │
Phase 7 (Polish) ─────────────────────────────────────────────┘
```

**Note**: US2-US4 depend on Phase 2 (Foundational entities). US1 is independent after Phase 2.

---

## Independent Test Criteria

| User Story | Test Criteria |
|------------|--------------|
| US1 (Adapter Framework) | Adapter discovered via SPI, implements interface, capabilities reported correctly |
| US2 (Provider Management) | CRUD operations work, hot reload triggers, validation enforced |
| US3 (Channel & ChannelKey) | Multi-Key selection works, failover triggers automatically within 500ms |
| US4 (Model Association) | Models listed per Provider correctly, capabilities parsed |

---

## Suggested MVP Scope

**Minimum Viable Product**: Phase 1 + Phase 2 + Phase 3 (US1)

This delivers:
- Project structure with entities
- LLMProviderAdapter interface
- OpenAI and Anthropic adapter implementations
- SPI discovery mechanism

Users can:
- Add new Provider adapters without modifying framework code

**Next Increments**:
1. US2 (Provider CRUD) - Admin can configure Providers
2. US3 (Channel & Key) - Multi-Key support with failover
3. US4 (Model Association) - Provider-Model relationship

---

**Total Tasks**: 95
**Parallelizable Tasks**: 26 [P] markers
**Story-based Tasks**: 74 (distributed across US1-US4)
**Test Tasks**: 16