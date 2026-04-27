# Phase 3 收尾迁移计划

> **目标**: 完成 gateway-security 外的所有遗留模块迁移到 gateway-boot

## 迁移范围概览

| 模块 | 遗留文件数 | 需迁移 | 重复/可删除 |
|------|-----------|--------|-------------|
| gateway-common | 6 | 0 (仅测试) | 6 |
| gateway-core | 53 | ~20 | ~33 |
| gateway-infrastructure | 9 | 9 | 0 |
| gateway-router | 7 | 1 | 6 |
| gateway-adapter | 16 | ~5 | 11 |
| gateway-api | 31 | ~8 | 23 |
| gateway-application | 11 | ~6 | 5 |
| gateway-analytics | 0 | 0 | 0 |
| **总计** | **133** | **~49** | **~84** |

---

## Phase 3.1: gateway-core Exceptions & Events (~6 文件)

**目标**: 迁移领域异常和事件到 gateway-boot

### 任务 1: 迁移 Exceptions

- [ ] `gateway-core/exception/GatewayRequestException.java` → `gateway-boot/common/exception/`
- [ ] `gateway-core/exception/ProviderException.java` → `gateway-boot/common/exception/`
- [ ] `gateway-core/exception/SecurityException.java` → `gateway-boot/common/exception/`

### 任务 2: 迁移 Events

- [ ] `gateway-core/domain/event/DomainEvent.java` → `gateway-boot/common/event/`
- [ ] `gateway-core/domain/event/AuditEvent.java` → `gateway-boot/common/event/`
- [ ] `gateway-core/domain/event/TokenUsedEvent.java` → `gateway-boot/common/event/`

---

## Phase 3.2: gateway-core Enums (~3 文件)

**目标**: 迁移领域枚举到 gateway-boot

- [ ] `gateway-core/domain/enums/ProviderErrorType.java` → `gateway-boot/domain/router/enums/`
- [ ] `gateway-core/domain/enums/ProviderStatus.java` → `gateway-boot/domain/router/enums/`
- [ ] `gateway-core/domain/enums/ProviderType.java` → `gateway-boot/common/enums/`

> 注意: `ProviderType` 在 gateway-common 中可能已存在，检查后决定

---

## Phase 3.3: gateway-core Infrastructure (~4 文件)

**目标**: 迁移基础设施加密服务

- [ ] `gateway-core/infrastructure/encryption/EncryptionService.java` → `gateway-boot/common/security/`
- [ ] `gateway-core/infrastructure/encryption/StubEncryptionService.java` → `gateway-boot/infrastructure/security/encryption/`
- [ ] `gateway-core/infrastructure/encryption/Aes256EncryptionService.java` → `gateway-boot/infrastructure/security/encryption/`

> `Aes256EncryptionService` 在 gateway-boot 中可能已存在，检查后决定

---

## Phase 3.4: gateway-infrastructure (~9 文件)

**目标**: 迁移 JPA Gateway 实现到 gateway-boot

### 任务 5: 迁移 Jpa*Gateway 实现

- [ ] `JpaApiKeyGateway.java` → `gateway-boot/infrastructure/gateway/security/`
- [ ] `JpaAuditGateway.java` → `gateway-boot/infrastructure/gateway/security/`
- [ ] `JpaIpBlockGateway.java` → `gateway-boot/infrastructure/gateway/security/`
- [ ] `JpaModelGateway.java` → `gateway-boot/infrastructure/gateway/router/`
- [ ] `JpaProviderApiKeyGateway.java` → `gateway-boot/infrastructure/gateway/router/`
- [ ] `JpaProviderGateway.java` → `gateway-boot/infrastructure/gateway/router/`
- [ ] `JpaRouteGroupGateway.java` → `gateway-boot/infrastructure/gateway/router/`
- [ ] `JpaTokenLimitGateway.java` → `gateway-boot/infrastructure/gateway/security/`
- [ ] `PageResult.java` → `gateway-boot/infrastructure/util/`

> 注意: 这些实现依赖 JPA Repository 接口，需要一起迁移或重构

---

## Phase 3.5: gateway-router (~1 文件)

**目标**: 迁移 `DefaultModelRouter`

- [ ] `gateway-router/DefaultModelRouter.java` → `gateway-boot/domain/router/service/DefaultModelRouter.java`

> 其他文件已迁移:
> - `LLMDispatcher.java` → 已迁移
> - `ProtocolTranslator.java` → 已迁移
> - `ErrorResponseAdapter.java` → 已迁移

---

## Phase 3.6: gateway-adapter (~5 文件)

**目标**: 迁移缺失的适配器组件

### 任务 7: 迁移 Anthropic/OpenAI 适配器

- [ ] `gateway-adapter/anthropic/AnthropicAdapter.java` → `gateway-boot/infrastructure/adapter/anthropic/`
- [ ] `gateway-adapter/openai/OpenAIAdapter.java` → `gateway-boot/infrastructure/adapter/openai/`

### 任务 8: 迁移通用组件

- [ ] `gateway-adapter/common/CredentialsLoader.java` → `gateway-boot/infrastructure/adapter/common/`
- [ ] `gateway-adapter/common/ProviderErrorType.java` → `gateway-boot/common/enums/ProviderErrorType.java` (如果未迁移)
- [ ] `gateway-adapter/common/ProviderException.java` → `gateway-boot/common/exception/ProviderException.java` (如果未迁移)

> 已迁移文件:
> - `LLMProviderAdapter.java`, `StreamCallback.java`
> - `ProviderCapabilities.java`, `ProviderType.java`
> - `AdapterLoader.java`, `AdapterRegistry.java`

---

## Phase 3.7: gateway-api (~8 文件)

**目标**: 迁移缺失的 Controller 和组件

### 任务 9: 迁移 Controllers

- [ ] `gateway-api/controller/AnthropicController.java` → `gateway-boot/adapter/chat/controller/`
- [ ] `gateway-api/controller/OpenAIController.java` → `gateway-boot/adapter/chat/controller/`
- [ ] `gateway-api/controller/ModelController.java` → `gateway-boot/adapter/model/controller/`
- [ ] `gateway-api/controller/ProviderController.java` → `gateway-boot/adapter/model/controller/`
- [ ] `gateway-api/controller/HealthController.java` → `gateway-boot/adapter/admin/controller/`

### 任务 10: 迁移缺失组件

- [ ] `gateway-api/advice/MaskingResponseAdvice.java` → `gateway-boot/infrastructure/advice/`
- [ ] `gateway-api/security/SecurityExceptionHandler.java` → `gateway-boot/infrastructure/security/`

> 已迁移:
> - DTOs (10 个)
> - Configs (3 个)
> - GlobalExceptionHandler.java
> - Services (3 个 UseCase)

---

## Phase 3.8: gateway-application (~6 文件)

**目标**: 迁移 SaToken 安全配置组件

### 任务 11: 迁移 SaToken 配置

- [ ] `gateway-application/config/satoken/SaTokenConfig.java` → `gateway-boot/infrastructure/config/`
- [ ] `gateway-application/config/satoken/ApiKeyStpInterface.java` → `gateway-boot/infrastructure/security/`
- [ ] `gateway-application/config/satoken/ApiKeyAuthAdapter.java` → `gateway-boot/infrastructure/security/auth/`
- [ ] `gateway-application/config/satoken/ApiKeyAuthInterceptor.java` → `gateway-boot/infrastructure/security/auth/`
- [ ] `gateway-application/config/satoken/IPBlockCheckInterceptor.java` → `gateway-boot/infrastructure/security/auth/`

> 已迁移:
> - `AuditEventListener.java`, `TokenUsageEventListener.java`

---

## Phase 3.9: 清理和编译测试

**目标**: 删除重复文件，验证编译

### 任务 12: 清理重复文件

```bash
# 删除已迁移的重复文件
rm -rf gateway-security/src
rm -rf gateway-common/src/main  # 保留测试
rm -rf gateway-core/src/main
rm -rf gateway-infrastructure/src
rm -rf gateway-router/src/main
rm -rf gateway-adapter/src/main
rm -rf gateway-api/src/main
rm -rf gateway-application/src/main
```

### 任务 13: 全量编译测试

```bash
mvn compile -pl gateway-boot -am
```

---

## 执行顺序

```
Phase 3.1 (Exceptions & Events)
    ↓
Phase 3.2 (Enums)
    ↓
Phase 3.3 (Infrastructure Encryption)
    ↓
Phase 3.4 (Jpa*Gateway) ← 需要 Phase 3.1-3.3 完成
    ↓
Phase 3.5 (DefaultModelRouter) ← 需要 Phase 3.4 完成
    ↓
Phase 3.6 (Adapters) ← 需要 Phase 3.4 完成
    ↓
Phase 3.7 (API Controllers) ← 需要 Phase 3.6 完成
    ↓
Phase 3.8 (SaToken Config) ← 需要 Phase 3.4 完成
    ↓
Phase 3.9 (Cleanup & Compile)
```

---

## 风险点

1. **Jpa*Gateway 依赖 JPA Repository**: gateway-core 中的 Repository 接口需要一起处理
2. **循环依赖**: 迁移时注意模块间依赖关系
3. **测试文件**: 很多测试文件依赖旧的包路径，需要同步迁移或删除

---

## 预期结果

- 所有业务代码迁移到 `gateway-boot` 模块
- 删除所有重复的旧模块
- 编译通过
- 保留测试文件（在各模块的 test 目录中）
