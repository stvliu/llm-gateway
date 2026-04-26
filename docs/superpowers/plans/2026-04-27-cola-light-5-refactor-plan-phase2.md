# COLA Light 5.0 重构实施计划 - Phase 2

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

**Goal:** 完成所有剩余文件的迁移（171 个文件）

**Architecture:** COLA Light 5.0 单模块架构，用 package 代替模块划分层次

---

## Phase 2 迁移清单

### Task A: 迁移 gateway-common DTO

**Files:**
- `gateway-common/src/main/java/com/codingas/gateway/common/dto/LLMRequest.java` → `gateway-boot/common/dto/`
- `gateway-common/src/main/java/com/codingas/gateway/common/dto/LLMResponse.java` → `gateway-boot/common/dto/`

- [ ] 迁移 LLMRequest 和 LLMResponse

---

### Task B: 迁移 gateway-core Service

**Files:**
- `gateway-core/src/main/java/com/codingas/gateway/core/service/ModelService.java` → `domain/router/service/`
- `gateway-core/src/main/java/com/codingas/gateway/core/service/ProviderService.java` → `domain/router/service/`

- [ ] 迁移 ModelService
- [ ] 迁移 ProviderService

---

### Task C: 迁移 gateway-router

**Files:**
- `gateway-router/src/main/java/com/codingas/gateway/router/ModelRouter.java` → `domain/router/gateway/`
- `gateway-router/src/main/java/com/codingas/gateway/router/DefaultModelRouter.java` → `domain/router/service/`
- `gateway-router/src/main/java/com/codingas/gateway/router/LLMDispatcher.java` → `domain/router/service/`
- `gateway-router/src/main/java/com/codingas/gateway/router/ProtocolTranslator.java` → `infrastructure/util/`
- `gateway-router/src/main/java/com/codingas/gateway/router/ErrorResponseAdapter.java` → `infrastructure/adapter/`

- [ ] 迁移 ModelRouter 接口
- [ ] 迁移 DefaultModelRouter 实现
- [ ] 迁移 LLMDispatcher
- [ ] 迁移 ProtocolTranslator
- [ ] 迁移 ErrorResponseAdapter

---

### Task D: 迁移 gateway-security

**Files:**
- `gateway-security/src/main/java/com/codingas/gateway/security/audit/AuditContext.java` → `domain/security/`
- `gateway-security/src/main/java/com/codingas/gateway/security/audit/AuditService.java` → `domain/security/service/`
- `gateway-security/src/main/java/com/codingas/gateway/security/authentication/AuthenticationService.java` → 已迁移
- `gateway-security/src/main/java/com/codingas/gateway/security/authentication/DefaultNotificationService.java` → `domain/security/service/`
- `gateway-security/src/main/java/com/codingas/gateway/security/authentication/GatewayApiKeyExpirationNotifier.java` → `domain/security/service/`
- `gateway-security/src/main/java/com/codingas/gateway/security/authentication/UserAuthResult.java` → 已迁移
- `gateway-security/src/main/java/com/codingas/gateway/security/authorization/Permission.java` → `domain/security/entity/`
- `gateway-security/src/main/java/com/codingas/gateway/security/authorization/RbacService.java` → 已迁移
- `gateway-security/src/main/java/com/codingas/gateway/security/authorization/Role.java` → `domain/security/entity/`
- `gateway-security/src/main/java/com/codingas/gateway/security/bruteforce/BruteForceProtectionService.java` → 已迁移
- `gateway-security/src/main/java/com/codingas/gateway/security/bruteforce/FailedAttemptTracker.java` → `domain/security/service/`
- `gateway-security/src/main/java/com/codingas/gateway/security/bruteforce/InMemoryFailedAttemptTracker.java` → `infrastructure/`
- `gateway-security/src/main/java/com/codingas/gateway/security/encryption/ApiKeyEncryptionService.java` → `domain/security/service/`
- `gateway-security/src/main/java/com/codingas/gateway/security/interceptor/AbstractGatewayInterceptor.java` → `infrastructure/security/`

- [ ] 迁移 AuditContext, AuditService
- [ ] 迁移 Notification 相关
- [ ] 迁移 Authorization 相关
- [ ] 迁移 BruteForce 相关
- [ ] 迁移 Encryption 相关
- [ ] 迁移 Interceptor

---

### Task E: 迁移 gateway-application

**Files:**
- `gateway-application/src/main/java/com/codingas/gateway/application/listener/AuditEventListener.java` → `application/listener/`
- `gateway-application/src/main/java/com/codingas/gateway/application/listener/TokenUsageEventListener.java` → `application/listener/`
- `gateway-application/src/main/java/com/codingas/gateway/config/satoken/ApiKeyAuthAdapter.java` → `infrastructure/config/`
- `gateway-application/src/main/java/com/codingas/gateway/config/satoken/ApiKeyAuthInterceptor.java` → `infrastructure/config/`
- `gateway-application/src/main/java/com/codingas/gateway/config/satoken/ApiKeyStpInterface.java` → `infrastructure/config/`
- `gateway-application/src/main/java/com/codingas/gateway/config/satoken/IPBlockCheckInterceptor.java` → `infrastructure/config/`
- `gateway-application/src/main/java/com/codingas/gateway/config/satoken/SaTokenConfig.java` → `infrastructure/config/`

- [ ] 迁移 Event Listener
- [ ] 迁移 SaToken Config

---

### Task F: 迁移 gateway-adapter

**Files:**
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/LLMProviderAdapter.java` → `infrastructure/adapter/`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/StreamCallback.java` → `infrastructure/adapter/`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java` → `infrastructure/adapter/`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java` → `infrastructure/adapter/`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/common/CredentialsLoader.java` → `infrastructure/util/`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/common/ProviderCapabilities.java` → `common/`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/common/ProviderErrorType.java` → `common/enums/`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/common/ProviderException.java` → `common/exception/`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/common/ProviderType.java` → `common/enums/`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/spi/AdapterLoader.java` → `infrastructure/spi/`
- `gateway-adapter/src/main/java/com/codingas/gateway/adapter/spi/AdapterRegistry.java` → `infrastructure/spi/`

- [ ] 迁移 LLMProviderAdapter 接口
- [ ] 迁移 Anthropic/OpenAI Adapter
- [ ] 迁移 Provider 枚举和异常
- [ ] 迁移 SPI

---

### Task G: 迁移 gateway-api Controller

**Files:**
- `gateway-api/src/main/java/com/codingas/gateway/web/controller/AnthropicController.java` → `adapter/chat/`
- `gateway-api/src/main/java/com/codingas/gateway/web/controller/HealthController.java` → `adapter/admin/`
- `gateway-api/src/main/java/com/codingas/gateway/web/controller/ModelController.java` → `adapter/model/`
- `gateway-api/src/main/java/com/codingas/gateway/web/controller/OpenAIController.java` → `adapter/chat/`
- `gateway-api/src/main/java/com/codingas/gateway/web/controller/ProviderController.java` → `adapter/model/`

- [ ] 迁移 AnthropicController
- [ ] 迁移 HealthController
- [ ] 迁移 ModelController
- [ ] 迁移 OpenAIController
- [ ] 迁移 ProviderController

---

### Task H: 迁移 gateway-api DTO

**Files:**
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/AnthropicMessagesRequest.java` → `adapter/chat/dto/`
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/AnthropicMessagesResponse.java` → `adapter/chat/dto/`
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/ApiResponse.java` → `common/dto/`
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/CreateModelRequest.java` → `adapter/model/dto/`
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/CreateProviderRequest.java` → `adapter/model/dto/`
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/ModelResponse.java` → `adapter/model/dto/`
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/OpenAIChatRequest.java` → `adapter/chat/dto/`
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/OpenAIChatResponse.java` → `adapter/chat/dto/`
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/ProviderResponse.java` → `adapter/model/dto/`
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/UpdateModelRequest.java` → `adapter/model/dto/`
- `gateway-api/src/main/java/com/codingas/gateway/web/dto/UpdateProviderRequest.java` → `adapter/model/dto/`

- [ ] 迁移 chat DTO
- [ ] 迁移 model DTO
- [ ] 迁移 ApiResponse

---

### Task I: 迁移 gateway-api Config

**Files:**
- `gateway-api/src/main/java/com/codingas/gateway/web/config/CorsConfig.java` → `infrastructure/config/`
- `gateway-api/src/main/java/com/codingas/gateway/web/config/GatewayProperties.java` → `infrastructure/config/`
- `gateway-api/src/main/java/com/codingas/gateway/web/config/OpenApiConfig.java` → `infrastructure/config/`
- `gateway-api/src/main/java/com/codingas/gateway/web/advice/GlobalExceptionHandler.java` → `infrastructure/advice/`
- `gateway-api/src/main/java/com/codingas/gateway/web/advice/MaskingResponseAdvice.java` → `infrastructure/advice/`
- `gateway-api/src/main/java/com/codingas/gateway/web/security/SecurityExceptionHandler.java` → `infrastructure/security/`

- [ ] 迁移 Config
- [ ] 迁移 Advice
- [ ] 迁移 Security Handler

---

### Task J: 迁移 gateway-api UseCase

**Files:**
- `gateway-api/src/main/java/com/codingas/gateway/web/service/LLMChatUseCase.java` → `application/chat/`
- `gateway-api/src/main/java/com/codingas/gateway/web/service/ModelManageUseCase.java` → `application/model/`
- `gateway-api/src/main/java/com/codingas/gateway/web/service/ProviderManageUseCase.java` → `application/model/`

- [ ] 迁移 UseCase

---

### Task K: 全量编译和测试

- [ ] 全量编译
- [ ] 运行测试
- [ ] 提交

---

## Self-Review Checklist

1. **Spec coverage:** 所有文件已迁移 ✅ / ❌
2. **Placeholder scan:** 无 TBD/TODO
3. **Type consistency:** Gateway 接口在 domain/xxx/gateway/
4. **Test coverage:** 全项目编译通过，测试通过
