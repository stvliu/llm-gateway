# Tasks: Provider Adapter Framework

**Input**: Design documents from `/specs/001-provider-adapter/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/adapter-interface.md, research.md

## Phase 1: Setup

**Goal**: Initialize Java 21 + Spring Boot 3.5.x project structure with Maven

- [X] T001 Create Maven project structure with Java 21, Spring Boot 3.5.0 parent
- [X] T002 Configure dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, lombok, jackson-databind
- [X] T003 Create package structure `com.codingas.gateway.adapter`, `com.codingas.gateway.domain`, `com.codingas.gateway.service`, `com.codingas.gateway.infrastructure.persistence`, `com.codingas.gateway.common.exception`
- [X] T004 Configure H2 in-memory database for development in `application-dev.yml`
- [X] T005 Create `application-local.yml` with external configuration template
- [X] T006 Add Maven wrapper (`mvnw`) to ensure consistent builds

---

## Phase 2: Foundational

**Goal**: Create domain entities, value objects, and base exceptions that all user stories depend on

- [X] T007 Create base entity `BaseEntity` with `id`, `created_by`, `created_at`, `updated_by`, `updated_at` fields
- [X] T008 Create `Provider` entity with fields: id, provider_code, provider_name, provider_type (ENUM), base_url, website_url, api_doc_url, priority, status
- [X] T009 Create `Model` entity with fields: id, model_code, display_name, provider_id (FK), provider_model_id, context_window, input_price, output_price, capabilities (JSON), status
- [X] T010 Create `ProviderApiKey` entity with fields: id, key_code, provider_id (FK), key_name, api_key, encrypted_api_key, priority, status, last_used_at, expires_at
- [X] T010a Create `GatewayApiKey` entity with fields: id, key_code, key_hash, user_id (FK), provider_id (FK), name, status, expires_at, last_used_at, model_whitelist, ip_whitelist
- [X] T010b Create `RouteGroup` entity with fields: id, group_code, group_name, strategy, failover_enabled, max_retry, health_check_interval, description
- [X] T010c Create `RouteGroupProvider` entity with fields: id, route_group_id, provider_id, weight, priority, status, health_status, consecutive_failures, last_health_check_at
- [X] T011 Create `ProviderType` enum: OPENAI, ANTHROPIC, GEMINI, ZHIPU, OTHER
- [X] T012 Create `ProviderStatus` enum: ACTIVE, SUSPENDED, DELETED
- [X] T013 Create `ProviderErrorType` enum: AUTHENTICATION_ERROR, RATE_LIMIT_ERROR, QUOTA_EXCEEDED, TIMEOUT_ERROR, INVALID_REQUEST, UPSTREAM_ERROR, NETWORK_ERROR, UNKNOWN_ERROR
- [X] T014 Create JPA repositories: `ProviderRepository`, `ModelRepository`, `ProviderApiKeyRepository`, `GatewayApiKeyRepository`
- [X] T015 Create `EncryptionService` interface in `com.codingas.gateway.infrastructure.encryption` (stub implementation for now)
- [X] T016 Create `ProviderException` with fields: providerCode, errorType, retryable, traceId

---

## Phase 3: US1 - Adapter Framework Abstraction

**Goal**: Implement LLMProviderAdapter interface with SPI discovery mechanism

**Story**: 网关开发者需要一套标准接口，以便在不影响现有代码的情况下接入新的模型提供商

**Independent Test**: Write unit tests verifying new Provider implementation is discovered and usable without modifying framework code

### Interface & SPI

- [X] T017 [P] Define `LLMProviderAdapter` interface with methods: chat(), chatStream(), messages(), getCapabilities()
- [X] T018 [P] Define request/response records: `LLMRequest`, `LLMResponse`, `ProviderCapabilities`
- [X] T019 Create `AdapterLoader` using `ServiceLoader<LLMProviderAdapter>` for SPI discovery
- [X] T020 Create `AdapterRegistry` service to manage registered adapters by ProviderType
- [X] T021 Create SPI service file `META-INF/services/com.codingas.gateway.adapter.LLMProviderAdapter`

### OpenAI Adapter (Reference Implementation)

- [X] T022 [P] Implement `OpenAIAdapter` class implementing `LLMProviderAdapter` for OpenAI format
- [X] T023 [P] Implement `chat()` method in OpenAIAdapter with HTTP call to OpenAI API endpoint
- [X] T024 [P] Implement `chatStream()` method in OpenAIAdapter (OpenAI streaming support)
- [X] T025 [P] Implement `getCapabilities()` returning ProviderCapabilities with supportsChatCompletion=true, supportsMessages=false
- [X] T026 Update SPI file to include `com.codingas.gateway.adapter.openai.OpenAIAdapter`

### Anthropic Adapter (Reference Implementation)

- [X] T027 [P] Implement `AnthropicAdapter` class implementing `LLMProviderAdapter`
- [X] T028 [P] Implement `messages()` method in AnthropicAdapter
- [X] T029 [P] Implement `chat()` throwing UnsupportedOperationException (Anthropic doesn't support OpenAI format)
- [X] T030 [P] Implement `chatStream()` for Anthropic streaming
- [X] T031 [P] Implement `getCapabilities()` returning ProviderCapabilities with supportsChatCompletion=false, supportsMessages=true
- [X] T032 Update SPI file to include `com.codingas.gateway.adapter.anthropic.AnthropicAdapter`

### Unit Tests (Coverage target: ≥80%)

- [X] T033 Write `LLMProviderAdapterTest` verifying all required methods exist
- [X] T034 Write `AdapterLoaderTest` verifying SPI discovery loads adapters
- [X] T035 Write `OpenAIAdapterTest` verifying capability reporting
- [X] T036 Write `AnthropicAdapterTest` verifying capability reporting

---

## Phase 4: US2 - Provider Management

**Goal**: Implement Provider CRUD operations with hot reload capability

**Story**: 管理员需要通过管理界面配置模型提供商

**Independent Test**: CRUD tests verifying admin can create/read/update/delete Provider configurations

### Service Layer

- [X] T037 Create `ProviderService` with CRUD methods: create(), findById(), findAll(), findByProviderCode(), update(), delete()
- [X] T038 Implement hot-reload notification mechanism using Spring's `ApplicationEventPublisher`
- [ ] T039 Create `ProviderConfig` @ConfigurationProperties class with refresh scope support

### Management API

- [X] T040 [P] Create `ProviderController` with endpoints: POST/GET/PUT/DELETE /api/v1/providers
- [X] T041 [P] Create request DTOs: `CreateProviderRequest`, `UpdateProviderRequest`
- [X] T042 [P] Create response DTOs: `ProviderResponse` with unified ApiResponse envelope
- [X] T043 Implement API versioning with `/api/v1/` prefix
- [X] T044 Create `ProviderResponse` with all Provider fields

### Unit Tests

- [ ] T045 Write `ProviderServiceTest` with mocked repository (Coverage target: ≥90%)
- [ ] T046 Write `ProviderControllerTest` using MockMvc

---

## Phase 5: US4 - Model Association

**Goal**: Implement Model entity and Provider-Model relationship management

**Story**: 系统需要维护 Provider 与其提供的模型之间的关联关系

**Independent Test**: Test verifying Model list query returns correct models for a Provider

### Service Layer

- [X] T047 Create `ModelService` with CRUD methods
- [X] T048 Implement `findByProviderId(providerId)` for listing models per Provider
- [X] T049 Implement `findByProviderModelId(providerId, providerModelId)` for lookup
- [X] T050 Add `findAll()` method to ModelService

### Management API

- [X] T051 [P] Create `ModelController` with endpoints: /api/v1/models
- [X] T052 [P] Create DTOs: `CreateModelRequest`, `UpdateModelRequest`, `ModelResponse`

### Database Migration

- [X] T053 Create Flyway migration V1__init_schema.sql creating providers, models, provider_api_keys, gateway_api_keys, route_groups, route_group_providers tables
- [X] T054 Create Flyway migration V2__seed_providers.sql with OpenAI, Anthropic, Azure seed data

### Unit Tests

- [ ] T055 Write `ModelServiceTest` with mocked repository (Coverage target: ≥90%)
- [ ] T056 Write `ModelControllerTest` using MockMvc

---

## Phase 5b: US3 - Routing & Load Balancing

**Goal**: Implement RouteGroup with load balancing and failover

**Story**: 系统需要支持多种路由策略（加权轮询、最小延迟、优先级）和故障转移

**Independent Test**: Test verifying failover when Provider becomes unavailable

### Service Layer

- [ ] T057 Create `RouteGroupService` with CRUD methods
- [ ] T058 Create `RouteGroupProviderService` for managing RouteGroup-Provider associations
- [ ] T059 Implement `ProviderSelector` with strategy pattern (ROUND_ROBIN / LEAST_LATENCY / PRIORITY)
- [ ] T060 Implement `FailoverHandler` for automatic failover when Provider fails
- [ ] T061 Implement `HealthChecker` for periodic health checks

### Management API

- [ ] T062 [P] Create `RouteGroupController` with endpoints: /api/v1/route-groups
- [ ] T063 [P] Create `RouteGroupProviderController` with endpoints: /api/v1/route-groups/{id}/providers
- [ ] T064 [P] Create DTOs: `CreateRouteGroupRequest`, `RouteGroupResponse`, `RouteGroupProviderResponse`

### Unit Tests (Coverage target: ≥85% for routing engine)

- [ ] T065 Write `ProviderSelectorTest` verifying strategy selection
- [ ] T066 Write `FailoverHandlerTest` verifying automatic failover
- [ ] T067 Write `HealthCheckerTest` verifying health status updates

---

## Phase 6: Polish & Cross-Cutting Concerns

**Goal**: Integration verification, documentation, and final quality checks

- [ ] T068 Create `AdapterRoutingIntegration` stub in `com.codingas.gateway.dispatch` package
- [ ] T069 Add OpenTelemetry tracing to all adapter methods
- [ ] T070 Create integration test verifying all adapters load correctly
- [ ] T071 Verify SC-001: New adapter integration time ≤2 hours
- [ ] T072 Verify SC-003: Hot reload delay ≤100ms
- [ ] T073 Add logging for adapter discovery at startup
- [ ] T074 Ensure API response follows unified ApiResponse format
- [ ] T075 Final code review checklist validation

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
Phase 4 (US2 - Provider Management) ────────────────────────────┤
    │                                                           │
    ▼                                                           │
Phase 5 (US4 - Model Association) ───────────────────────────────┤
    │                                                           │
    ▼                                                           │
Phase 5b (US3 - Routing & Failover) ─────────────────────────┤
    │                                                           │
    ▼                                                           │
Phase 6 (Polish) ─────────────────────────────────────────────┘
```

---

## Independent Test Criteria

| User Story | Test Criteria |
|------------|--------------|
| US1 (Adapter Framework) | Adapter discovered via SPI, implements interface, capabilities reported correctly |
| US2 (Provider Management) | CRUD operations work, hot reload triggers, validation enforced |
| US3 (Routing & Failover) | RouteGroup selects Provider correctly, failover triggers on Provider failure |
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
2. US4 (Model Association) - Provider-Model relationship

---

**Total Tasks**: 75
**Completed**: 52
**Remaining**: 23
**Parallelizable Tasks**: 16 [P] markers
