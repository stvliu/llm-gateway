# 遗留模块迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 gateway-common, gateway-core, gateway-infrastructure, gateway-router, gateway-adapter, gateway-api, gateway-application 中的遗留文件迁移到 gateway-boot 单模块，完成架构统一

**Architecture:** 基于 COLA Light 5.0 单模块架构，按领域内聚迁移：Entity + Domain Service + Gateway 接口 → domain/xxx/，Gateway 实现 → infrastructure/gateway/xxx/，Adapter → infrastructure/adapter/

**Tech Stack:** Java 21, Spring Boot 3.5.x, Maven, JPA

---

## 阶段 1: 核心模型迁移

### 任务 1.1: 检查 gateway-boot 现有实体

**Files:**
- 检查: `gateway-boot/src/main/java/com/codingas/gateway/domain/security/entity/`
- 检查: `gateway-boot/src/main/java/com/codingas/gateway/domain/router/entity/`
- 检查: `gateway-boot/src/main/java/com/codingas/gateway/common/enums/`

- [ ] **Step 1: 列出 gateway-boot 已有实体**

```bash
find /mnt/e/workspace/llm-gateway/gateway-boot -path "*/domain/*/entity/*.java" -type f | sort
find /mnt/e/workspace/llm-gateway/gateway-boot -path "*/common/enums/*.java" -type f | sort
```

**Expected:** 显示已存在的实体和枚举文件

- [ ] **Step 2: 列出 gateway-core 待迁移实体**

```bash
find /mnt/e/workspace/llm-gateway/gateway-core -path "*/domain/*/entity/*.java" -type f | sort
```

**Expected:** 显示需要迁移的实体文件

- [ ] **Step 3: 对比并识别缺失实体**

对比两个列表，识别 gateway-boot 中缺失的实体

- [ ] **Step 4: 提交阶段 1.1 结果**

```bash
git add -A
git commit -m "docs: 阶段1.1 实体检查完成 - 识别待迁移实体"
```

---

### 任务 1.2: 迁移 gateway-core 缺失实体到 gateway-boot

**Files:**
- 迁移: `gateway-core/domain/security/entity/IpBlocklist.java` → `gateway-boot/domain/security/entity/IpBlocklist.java`
- 迁移: `gateway-core/domain/security/entity/RateLimitConfig.java` → `gateway-boot/domain/security/entity/RateLimitConfig.java`
- 迁移: `gateway-core/domain/security/entity/SensitiveDataRule.java` → `gateway-boot/domain/security/entity/SensitiveDataRule.java`
- 迁移: `gateway-core/domain/analytics/entity/TokenLimit.java` → `gateway-boot/domain/security/entity/TokenLimit.java`

- [ ] **Step 1: 读取 gateway-core 待迁移实体文件**

```bash
cat /mnt/e/workspace/llm-gateway/gateway-core/src/main/java/com/codingas/gateway/core/domain/security/entity/IpBlocklist.java
cat /mnt/e/workspace/llm-gateway/gateway-core/src/main/java/com/codingas/gateway/core/domain/security/entity/RateLimitConfig.java
cat /mnt/e/workspace/llm-gateway/gateway-core/src/main/java/com/codingas/gateway/core/domain/security/entity/SensitiveDataRule.java
cat /mnt/e/workspace/llm-gateway/gateway-core/src/main/java/com/codingas/gateway/core/domain/analytics/entity/TokenLimit.java
```

- [ ] **Step 2: 检查 gateway-boot 目标目录**

```bash
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/domain/security/entity/
```

**Expected:** 列出已有实体

- [ ] **Step 3: 迁移 IpBlocklist.java**

如果 gateway-boot 中不存在，则复制文件内容并调整 package 声明

- [ ] **Step 4: 迁移 RateLimitConfig.java**

如果 gateway-boot 中不存在，则复制文件内容并调整 package 声明

- [ ] **Step 5: 迁移 SensitiveDataRule.java**

如果 gateway-boot 中不存在，则复制文件内容并调整 package 声明

- [ ] **Step 6: 迁移 TokenLimit.java**

如果 gateway-boot 中不存在，则复制文件内容并调整 package 声明

- [ ] **Step 7: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/security/entity/
git commit -m "feat(domain): 迁移 IpBlocklist, RateLimitConfig, SensitiveDataRule, TokenLimit 实体"
```

---

### 任务 1.3: 迁移 gateway-core Gateway 接口

**Files:**
- 迁移: `gateway-core/domain/gateway/ProviderApiKeyGateway.java` → `gateway-boot/domain/security/gateway/ProviderApiKeyGateway.java`
- 迁移: `gateway-core/domain/gateway/RouteGroupGateway.java` → `gateway-boot/domain/router/gateway/RouteGroupGateway.java`
- 迁移: `gateway-core/domain/gateway/TokenLimitGateway.java` → `gateway-boot/domain/security/gateway/TokenLimitGateway.java`

- [ ] **Step 1: 列出 gateway-core Gateway 接口**

```bash
find /mnt/e/workspace/llm-gateway/gateway-core -path "*/domain/gateway/*.java" -type f | sort
```

- [ ] **Step 2: 列出 gateway-boot 已有 Gateway 接口**

```bash
find /mnt/e/workspace/llm-gateway/gateway-boot -path "*/domain/*/gateway/*.java" -type f | sort
```

- [ ] **Step 3: 对比并识别缺失接口**

识别需要迁移的 Gateway 接口

- [ ] **Step 4: 迁移 ProviderApiKeyGateway.java**

如果 gateway-boot 中不存在，则复制并调整 package

- [ ] **Step 5: 迁移 RouteGroupGateway.java**

如果 gateway-boot 中不存在，则复制并调整 package

- [ ] **Step 6: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/
git commit -m "feat(domain): 迁移 ProviderApiKeyGateway, RouteGroupGateway, TokenLimitGateway 接口"
```

---

### 任务 1.4: 迁移 gateway-core 枚举和事件

**Files:**
- 迁移: `gateway-core/domain/enums/ProviderErrorType.java` → `gateway-boot/common/enums/ProviderErrorType.java`
- 迁移: `gateway-core/domain/enums/ProviderStatus.java` → `gateway-boot/common/enums/ProviderStatus.java`
- 迁移: `gateway-core/domain/enums/ProviderType.java` → `gateway-boot/common/enums/ProviderType.java`
- 迁移: `gateway-core/domain/event/*.java` → `gateway-boot/common/event/`

- [ ] **Step 1: 列出并检查枚举文件**

```bash
cat /mnt/e/workspace/llm-gateway/gateway-core/src/main/java/com/codingas/gateway/core/domain/enums/ProviderErrorType.java
cat /mnt/e/workspace/llm-gateway/gateway-core/src/main/java/com/codingas/gateway/core/domain/enums/ProviderStatus.java
cat /mnt/e/workspace/llm-gateway/gateway-core/src/main/java/com/codingas/gateway/core/domain/enums/ProviderType.java
```

- [ ] **Step 2: 对比 gateway-boot 枚举**

检查 gateway-boot/common/enums/ 中是否已存在相同文件

- [ ] **Step 3: 迁移缺失枚举**

对于 gateway-boot 中不存在的枚举，复制并调整 package

- [ ] **Step 4: 检查事件文件**

```bash
cat /mnt/e/workspace/llm-gateway/gateway-core/src/main/java/com/codingas/gateway/core/domain/event/AuditEvent.java
cat /mnt/e/workspace/llm-gateway/gateway-core/src/main/java/com/codingas/gateway/core/domain/event/TokenUsedEvent.java
```

- [ ] **Step 5: 迁移缺失事件类**

对比 gateway-boot/common/event/，迁移缺失的事件类

- [ ] **Step 6: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/common/
git commit -m "feat(common): 迁移枚举和事件类"
```

---

## 阶段 2: 基础设施层迁移

### 任务 2.1: 迁移 gateway-infrastructure JpaGateway 实现

**Files:**
- 迁移: `gateway-infrastructure/JpaProviderApiKeyGateway.java` → `gateway-boot/infrastructure/gateway/security/JpaProviderApiKeyGateway.java`
- 迁移: `gateway-infrastructure/JpaRouteGroupGateway.java` → `gateway-boot/infrastructure/gateway/router/JpaRouteGroupGateway.java`
- 迁移: `gateway-infrastructure/PageResult.java` → `gateway-boot/infrastructure/util/PageResult.java`

- [ ] **Step 1: 检查 gateway-infrastructure JpaGateway 实现**

```bash
ls -la /mnt/e/workspace/llm-gateway/gateway-infrastructure/src/main/java/com/codingas/gateway/infrastructure/gateway/
```

- [ ] **Step 2: 检查 gateway-boot 已有实现**

```bash
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/security/
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/infrastructure/gateway/router/
```

- [ ] **Step 3: 迁移 JpaProviderApiKeyGateway.java**

读取源文件并迁移到 gateway-boot 相应位置

- [ ] **Step 4: 迁移 PageResult.java**

如果 gateway-boot 中不存在，则迁移

- [ ] **Step 5: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/
git commit -m "feat(infrastructure): 迁移 JpaProviderApiKeyGateway, PageResult"
```

---

### 任务 2.2: 迁移 gateway-adapter 适配器

**Files:**
- 迁移: `gateway-adapter/openai/OpenAIAdapter.java` → `gateway-boot/infrastructure/adapter/openai/OpenAIAdapter.java`
- 迁移: `gateway-adapter/anthropic/AnthropicAdapter.java` → `gateway-boot/infrastructure/adapter/anthropic/AnthropicAdapter.java`
- 迁移: `gateway-adapter/common/ProviderCapabilities.java` → `gateway-boot/common/ProviderCapabilities.java`
- 迁移: `gateway-adapter/spi/AdapterLoader.java` → `gateway-boot/infrastructure/spi/AdapterLoader.java`
- 迁移: `gateway-adapter/spi/AdapterRegistry.java` → `gateway-boot/infrastructure/spi/AdapterRegistry.java`

- [ ] **Step 1: 检查 gateway-boot 已有适配器**

```bash
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/infrastructure/adapter/
```

- [ ] **Step 2: 读取并分析 OpenAIAdapter.java**

```bash
cat /mnt/e/workspace/llm-gateway/gateway-adapter/src/main/java/com/codingas/gateway/adapter/openai/OpenAIAdapter.java
```

- [ ] **Step 3: 读取并分析 AnthropicAdapter.java**

```bash
cat /mnt/e/workspace/llm-gateway/gateway-adapter/src/main/java/com/codingas/gateway/adapter/anthropic/AnthropicAdapter.java
```

- [ ] **Step 4: 迁移 OpenAIAdapter.java**

如果 gateway-boot 中不存在，则迁移并调整 package

- [ ] **Step 5: 迁移 AnthropicAdapter.java**

如果 gateway-boot 中不存在，则迁移并调整 package

- [ ] **Step 6: 迁移 ProviderCapabilities.java**

如果 gateway-boot 中不存在，则迁移

- [ ] **Step 7: 迁移 AdapterLoader.java 和 AdapterRegistry.java**

如果 gateway-boot 中不存在，则迁移

- [ ] **Step 8: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 9: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/adapter/
git add gateway-boot/src/main/java/com/codingas/gateway/common/
git commit -m "feat(infrastructure): 迁移 OpenAIAdapter, AnthropicAdapter, ProviderCapabilities, AdapterLoader, AdapterRegistry"
```

---

### 任务 2.3: 迁移 gateway-router 辅助类

**Files:**
- 迁移: `gateway-router/ProtocolTranslator.java` → `gateway-boot/infrastructure/util/ProtocolTranslator.java`
- 迁移: `gateway-router/ErrorResponseAdapter.java` → `gateway-boot/infrastructure/adapter/ErrorResponseAdapter.java`
- 迁移: `gateway-router/DefaultModelRouter.java` → `gateway-boot/domain/router/service/DefaultModelRouter.java`

- [ ] **Step 1: 检查 gateway-boot 已有文件**

```bash
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/infrastructure/util/
```

- [ ] **Step 2: 读取并迁移 ProtocolTranslator.java**

如果 gateway-boot 中不存在，则迁移

- [ ] **Step 3: 读取并迁移 ErrorResponseAdapter.java**

如果 gateway-boot 中不存在，则迁移

- [ ] **Step 4: 读取并迁移 DefaultModelRouter.java**

如果 gateway-boot 中不存在，则迁移

- [ ] **Step 5: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/
git add gateway-boot/src/main/java/com/codingas/gateway/domain/
git commit -m "feat(infrastructure): 迁移 ProtocolTranslator, ErrorResponseAdapter, DefaultModelRouter"
```

---

## 阶段 3: 适配层迁移

### 任务 3.1: 迁移 gateway-api Controller

**Files:**
- 迁移: `gateway-api/controller/OpenAIController.java` → `gateway-boot/adapter/chat/controller/OpenAIController.java`
- 迁移: `gateway-api/controller/AnthropicController.java` → `gateway-boot/adapter/chat/controller/AnthropicController.java`
- 迁移: `gateway-api/controller/ModelController.java` → `gateway-boot/adapter/model/controller/ModelController.java`
- 迁移: `gateway-api/controller/ProviderController.java` → `gateway-boot/adapter/model/controller/ProviderController.java`
- 迁移: `gateway-api/controller/HealthController.java` → `gateway-boot/adapter/admin/controller/HealthController.java`

- [ ] **Step 1: 检查 gateway-boot 已有 Controller**

```bash
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/adapter/*/controller/
```

- [ ] **Step 2: 读取并分析 gateway-api Controller**

```bash
cat /mnt/e/workspace/llm-gateway/gateway-api/src/main/java/com/codingas/gateway/web/controller/OpenAIController.java
cat /mnt/e/workspace/llm-gateway/gateway-api/src/main/java/com/codingas/gateway/web/controller/AnthropicController.java
```

- [ ] **Step 3: 迁移 OpenAIController.java**

调整 package 为 `com.codingas.gateway.adapter.chat.controller`

- [ ] **Step 4: 迁移 AnthropicController.java**

调整 package 为 `com.codingas.gateway.adapter.chat.controller`

- [ ] **Step 5: 迁移 ModelController.java**

调整 package 为 `com.codingas.gateway.adapter.model.controller`

- [ ] **Step 6: 迁移 ProviderController.java**

调整 package 为 `com.codingas.gateway.adapter.model.controller`

- [ ] **Step 7: 迁移 HealthController.java**

调整 package 为 `com.codingas.gateway.adapter.admin.controller`

- [ ] **Step 8: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 9: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/
git commit -m "feat(adapter): 迁移 OpenAIController, AnthropicController, ModelController, ProviderController, HealthController"
```

---

### 任务 3.2: 迁移 gateway-api DTO

**Files:**
- 迁移: `gateway-api/dto/OpenAIChatRequest.java` → `gateway-boot/adapter/chat/dto/OpenAIChatRequest.java`
- 迁移: `gateway-api/dto/OpenAIChatResponse.java` → `gateway-boot/adapter/chat/dto/OpenAIChatResponse.java`
- 迁移: `gateway-api/dto/AnthropicMessagesRequest.java` → `gateway-boot/adapter/chat/dto/AnthropicMessagesRequest.java`
- 迁移: `gateway-api/dto/AnthropicMessagesResponse.java` → `gateway-boot/adapter/chat/dto/AnthropicMessagesResponse.java`
- 迁移: `gateway-api/dto/CreateModelRequest.java` → `gateway-boot/adapter/model/dto/CreateModelRequest.java`
- 迁移: `gateway-api/dto/UpdateModelRequest.java` → `gateway-boot/adapter/model/dto/UpdateModelRequest.java`
- 迁移: `gateway-api/dto/ModelResponse.java` → `gateway-boot/adapter/model/dto/ModelResponse.java`
- 迁移: `gateway-api/dto/CreateProviderRequest.java` → `gateway-boot/adapter/model/dto/CreateProviderRequest.java`
- 迁移: `gateway-api/dto/UpdateProviderRequest.java` → `gateway-boot/adapter/model/dto/UpdateProviderRequest.java`
- 迁移: `gateway-api/dto/ProviderResponse.java` → `gateway-boot/adapter/model/dto/ProviderResponse.java`

- [ ] **Step 1: 检查 gateway-boot 已有 DTO**

```bash
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/adapter/*/dto/
```

- [ ] **Step 2: 批量读取 gateway-api DTO**

```bash
find /mnt/e/workspace/llm-gateway/gateway-api -path "*/dto/*.java" -type f | sort
```

- [ ] **Step 3: 对比并迁移缺失 DTO**

对于 gateway-boot 中不存在的 DTO，复制并调整 package

- [ ] **Step 4: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/adapter/
git commit -m "feat(adapter): 迁移 API DTO 类"
```

---

### 任务 3.3: 迁移 gateway-api Service

**Files:**
- 迁移: `gateway-api/service/LLMChatUseCase.java` → `gateway-boot/application/chat/LLMChatUseCase.java`
- 迁移: `gateway-api/service/ModelManageUseCase.java` → `gateway-boot/application/model/ModelManageUseCase.java`
- 迁移: `gateway-api/service/ProviderManageUseCase.java` → `gateway-boot/application/model/ProviderManageUseCase.java`

- [ ] **Step 1: 检查 gateway-boot 已有 Service**

```bash
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/application/*/
```

- [ ] **Step 2: 读取并分析 LLMChatUseCase.java**

```bash
cat /mnt/e/workspace/llm-gateway/gateway-api/src/main/java/com/codingas/gateway/web/service/LLMChatUseCase.java
```

- [ ] **Step 3: 迁移 LLMChatUseCase.java**

调整 package 为 `com.codingas.gateway.application.chat`

- [ ] **Step 4: 迁移 ModelManageUseCase.java**

调整 package 为 `com.codingas.gateway.application.model`

- [ ] **Step 5: 迁移 ProviderManageUseCase.java**

调整 package 为 `com.codingas.gateway.application.model`

- [ ] **Step 6: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/application/
git commit -m "feat(application): 迁移 LLMChatUseCase, ModelManageUseCase, ProviderManageUseCase"
```

---

## 阶段 4: 配置和安全迁移

### 任务 4.1: 迁移 gateway-api Advice 和 Config

**Files:**
- 迁移: `gateway-api/advice/GlobalExceptionHandler.java` → `gateway-boot/infrastructure/advice/GlobalExceptionHandler.java`
- 迁移: `gateway-api/advice/MaskingResponseAdvice.java` → `gateway-boot/infrastructure/advice/MaskingResponseAdvice.java`
- 迁移: `gateway-api/config/CorsConfig.java` → `gateway-boot/infrastructure/config/CorsConfig.java`
- 迁移: `gateway-api/config/GatewayProperties.java` → `gateway-boot/infrastructure/config/GatewayProperties.java`
- 迁移: `gateway-api/config/OpenApiConfig.java` → `gateway-boot/infrastructure/config/OpenApiConfig.java`

- [ ] **Step 1: 检查 gateway-boot 已有配置**

```bash
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/infrastructure/config/
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/infrastructure/advice/
```

- [ ] **Step 2: 迁移 GlobalExceptionHandler.java**

如果 gateway-boot 中不存在，则迁移

- [ ] **Step 3: 迁移 MaskingResponseAdvice.java**

如果 gateway-boot 中不存在，则迁移

- [ ] **Step 4: 迁移 Config 文件**

如果 gateway-boot 中不存在，则迁移

- [ ] **Step 5: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/
git commit -m "feat(infrastructure): 迁移 GlobalExceptionHandler, MaskingResponseAdvice, Config"
```

---

### 任务 4.2: 迁移 gateway-application SaToken 配置

**Files:**
- 迁移: `gateway-application/config/satoken/ApiKeyAuthAdapter.java` → `gateway-boot/infrastructure/security/ApiKeyAuthAdapter.java`
- 迁移: `gateway-application/config/satoken/ApiKeyAuthInterceptor.java` → `gateway-boot/infrastructure/security/ApiKeyAuthInterceptor.java`
- 迁移: `gateway-application/config/satoken/ApiKeyStpInterface.java` → `gateway-boot/infrastructure/security/ApiKeyStpInterface.java`
- 迁移: `gateway-application/config/satoken/IPBlockCheckInterceptor.java` → `gateway-boot/infrastructure/security/IPBlockCheckInterceptor.java`
- 迁移: `gateway-application/config/satoken/SaTokenConfig.java` → `gateway-boot/infrastructure/security/SaTokenConfig.java`

- [ ] **Step 1: 检查 gateway-boot 已有安全配置**

```bash
ls -la /mnt/e/workspace/llm-gateway/gateway-boot/src/main/java/com/codingas/gateway/infrastructure/security/
```

- [ ] **Step 2: 读取并分析 SaToken 配置类**

```bash
cat /mnt/e/workspace/llm-gateway/gateway-application/src/main/java/com/codingas/gateway/config/satoken/SaTokenConfig.java
```

- [ ] **Step 3: 迁移 SaToken 配置类**

如果 gateway-boot 中不存在，则迁移并调整 package

- [ ] **Step 4: 验证编译**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/infrastructure/security/
git commit -m "feat(security): 迁移 SaToken 配置类"
```

---

## 阶段 5: 清理和验证

### 任务 5.1: 检查并清理重复文件

**Files:**
- 检查: 各源模块中的文件是否已全部迁移
- 删除: 确认迁移完成后可删除的源模块

- [ ] **Step 1: 列出 gateway-boot 已迁移文件数量**

```bash
find /mnt/e/workspace/llm-gateway/gateway-boot -name "*.java" -type f | wc -l
```

- [ ] **Step 2: 列出源模块剩余文件**

```bash
for dir in gateway-common gateway-core gateway-infrastructure gateway-router gateway-adapter gateway-analytics gateway-api gateway-application; do
  echo "=== $dir ==="
  find /mnt/e/workspace/llm-gateway/$dir -name "*.java" -type f -not -path "*/target/*" | wc -l
done
```

- [ ] **Step 3: 分析剩余文件**

对于仍有文件的模块，逐一检查是否需要迁移

- [ ] **Step 4: 提交清理状态**

```bash
git add -A
git commit -m "docs: 阶段5.1 清理状态记录"
```

---

### 任务 5.2: 完整构建验证

**Files:**
- 验证: `gateway-boot/pom.xml`
- 验证: 完整编译和测试

- [ ] **Step 1: 清理构建**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw clean -q
```

- [ ] **Step 2: 编译 gateway-boot**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw compile -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 3: 运行测试**

```bash
cd /mnt/e/workspace/llm-gateway && ./mvnw test -pl gateway-boot -q
```

**Expected:** BUILD SUCCESS

- [ ] **Step 4: 提交最终状态**

```bash
git add -A
git commit -m "feat: 完成遗留模块迁移，gateway-boot 完整构建验证通过"
```

---

## 迁移检查清单

### 核心模型
- [ ] IpBlocklist 实体
- [ ] RateLimitConfig 实体
- [ ] SensitiveDataRule 实体
- [ ] TokenLimit 实体
- [ ] ProviderApiKeyGateway 接口
- [ ] RouteGroupGateway 接口
- [ ] TokenLimitGateway 接口
- [ ] 枚举类 (ProviderErrorType, ProviderStatus, ProviderType)
- [ ] 事件类 (AuditEvent, TokenUsedEvent)

### 基础设施层
- [ ] JpaProviderApiKeyGateway 实现
- [ ] PageResult 工具类
- [ ] OpenAIAdapter 适配器
- [ ] AnthropicAdapter 适配器
- [ ] ProviderCapabilities
- [ ] AdapterLoader
- [ ] AdapterRegistry
- [ ] ProtocolTranslator
- [ ] ErrorResponseAdapter
- [ ] DefaultModelRouter

### 适配层
- [ ] OpenAIController
- [ ] AnthropicController
- [ ] ModelController
- [ ] ProviderController
- [ ] HealthController
- [ ] 所有 DTO 类
- [ ] LLMChatUseCase
- [ ] ModelManageUseCase
- [ ] ProviderManageUseCase

### 配置和安全
- [ ] GlobalExceptionHandler
- [ ] MaskingResponseAdvice
- [ ] CorsConfig
- [ ] GatewayProperties
- [ ] OpenApiConfig
- [ ] SaToken 配置类

### 最终验证
- [ ] 编译成功
- [ ] 测试通过
- [ ] 代码已提交
