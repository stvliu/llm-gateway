# 安全体系重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `domain/security/` 拆分为 `iam`、`threat`、`dataprotection` 三个子域，修正跨域散落，统一拦截器架构，重命名关键类型。

**Architecture:** 三子域划分——iam（身份与访问控制）、threat（威胁防护）、dataprotection（数据保护）。拦截器统一走 `GatewayInterceptor` 接口。Application 层按用例分包。异常按子域内聚。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, Sa-Token

---

## File Structure

### Domain 层新建包

```
domain/iam/entity/User.java                      ← 从 domain/security/entity/ 移入
domain/iam/entity/UserApiKey.java                 ← 从 domain/security/entity/ 移入
domain/iam/gateway/UserGateway.java               ← 从 domain/security/gateway/ 移入
domain/iam/gateway/UserApiKeyGateway.java          ← 从 domain/security/service/ 移入（原接口放错在 service 包）
domain/iam/service/Identity.java                  ← 新建，替代 UserAuthResult
domain/iam/service/GeneratedApiKey.java           ← 从 domain/security/service/ 移入
domain/iam/service/AuthenticationDomainService.java ← 从 domain/security/service/ 移入
domain/iam/service/ApiKeyEncryptionDomainService.java ← 从 domain/security/service/ 移入
domain/iam/service/UserApiKeyGenerator.java        ← 从 domain/security/service/ 移入
domain/iam/service/DefaultUserApiKeyGenerator.java ← 从 domain/security/service/ 移入
domain/iam/service/UserApiKeyDomainService.java     ← 从 domain/team/service/ 移入
domain/iam/exception/IamException.java             ← 新建，替代 SecurityException
domain/iam/exception/AuthenticationFailedException.java ← 从 domain/security/exception/ 移入
domain/iam/exception/UnauthorizedException.java     ← 从 domain/security/exception/ 移入
domain/iam/exception/ForbiddenException.java        ← 从 domain/security/exception/ 移入
domain/iam/enums/UserApiKeyState.java               ← 从 domain/team/enums/ 移入
domain/iam/enums/UserState.java                     ← 从 domain/security/enums/ 移入

domain/threat/entity/IpBlocklist.java               ← 从 domain/security/entity/ 移入
domain/threat/gateway/IpBlockGateway.java            ← 从 domain/security/gateway/ 移入
domain/threat/gateway/TokenBucketRateLimiter.java     ← 从 domain/security/gateway/ 移入
domain/threat/service/RateLimitDomainService.java    ← 从 domain/security/service/ 移入
domain/threat/service/IpBlocklistDomainService.java  ← 从 domain/security/service/ 移入
domain/threat/service/TokenBucketStatus.java         ← 从 domain/security/service/ 移入
domain/threat/exception/ThreatException.java          ← 新建
domain/threat/exception/RateLimitExceededException.java ← 从 domain/security/exception/ 移入
domain/threat/exception/IpBlockedException.java       ← 从 domain/security/exception/ 移入

domain/dataprotection/entity/SensitiveDataRule.java  ← 从 domain/security/entity/ 移入
domain/dataprotection/gateway/SensitiveDataRuleGateway.java ← 从 domain/security/gateway/ 移入
domain/dataprotection/service/SensitiveDataMasker.java ← 从 domain/security/service/ 移入
domain/dataprotection/exception/DataProtectionException.java ← 新建
```

### Infrastructure 层新建包

```
infrastructure/iam/gateway/UserGatewayImpl.java      ← 从 infrastructure/security/ 移入
infrastructure/iam/gateway/UserApiKeyGatewayImpl.java ← 从 infrastructure/team/gateway/ 移入
infrastructure/iam/gateway/database/UserDo.java       ← 从 infrastructure/security/database/ 移入
infrastructure/iam/gateway/database/UserRepository.java ← 从 infrastructure/security/database/ 移入
infrastructure/iam/gateway/database/UserApiKeyDo.java ← 从 infrastructure/team/gateway/database/ 移入
infrastructure/iam/gateway/database/UserApiKeyProductDo.java ← 从 infrastructure/team/gateway/database/ 移入
infrastructure/iam/gateway/database/UserApiKeyRepository.java ← 从 infrastructure/team/gateway/database/ 移入
infrastructure/iam/gateway/database/UserApiKeyProductRepository.java ← 从 infrastructure/team/gateway/database/ 移入
infrastructure/iam/gateway/encryption/EncryptionService.java ← 从 infrastructure/security/encryption/ 移入
infrastructure/iam/gateway/encryption/Aes256EncryptionService.java ← 从 infrastructure/security/encryption/ 移入

infrastructure/threat/gateway/IpBlockGatewayImpl.java ← 从 infrastructure/alert/gateway/ 移入
infrastructure/threat/gateway/IpBlocklistConverter.java ← 从 infrastructure/alert/gateway/ 移入
infrastructure/threat/gateway/InMemoryTokenBucketRateLimiter.java ← 从 infrastructure/security/ 移入
infrastructure/threat/gateway/database/IpBlocklistDo.java ← 从 infrastructure/alert/gateway/database/ 移入
infrastructure/threat/gateway/database/IpBlocklistRepository.java ← 从 infrastructure/alert/gateway/database/ 移入

infrastructure/dataprotection/gateway/SensitiveDataRuleGatewayImpl.java ← 从 infrastructure/alert/gateway/ 移入
infrastructure/dataprotection/gateway/SensitiveDataRuleConverter.java ← 从 infrastructure/alert/gateway/ 移入
infrastructure/dataprotection/SensitiveDataRuleInitializer.java ← 从 infrastructure/security/ 移入
infrastructure/dataprotection/gateway/database/SensitiveDataRuleDo.java ← 从 infrastructure/alert/gateway/database/ 移入
infrastructure/dataprotection/gateway/database/SensitiveDataRuleRepository.java ← 从 infrastructure/alert/gateway/database/ 移入
```

### Adapter 层变更

```
adapter/advice/IamExceptionHandler.java             ← 重命名自 SecurityExceptionHandler
adapter/advice/ThreatExceptionHandler.java           ← 新增
adapter/interceptor/ApiKeyAuthInterceptor.java       ← 改为实现 GatewayInterceptor
adapter/interceptor/RateLimitInterceptor.java        ← 新增，从 SecurityInterceptorChain 逻辑提取
```

### 删除的旧文件

移动完成后删除整个 `domain/security/` 包。
删除 `infrastructure/alert/gateway/` 下已移走的文件（AlertRuleDo、AlertNotificationDo 等属于 alert 域的保留不动）。

---

## Task 1: 创建 domain/iam 子域包与类型

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/iam/exception/IamException.java`
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/iam/service/Identity.java`
- Move: `domain/security/entity/User.java` → `domain/iam/entity/User.java`
- Move: `domain/security/entity/UserApiKey.java` → `domain/iam/entity/UserApiKey.java`
- Move: `domain/security/gateway/UserGateway.java` → `domain/iam/gateway/UserGateway.java`
- Move: `domain/security/service/UserApiKeyGateway.java` → `domain/iam/gateway/UserApiKeyGateway.java`
- Move: `domain/security/service/AuthenticationDomainService.java` → `domain/iam/service/AuthenticationDomainService.java`
- Move: `domain/security/service/ApiKeyEncryptionDomainService.java` → `domain/iam/service/ApiKeyEncryptionDomainService.java`
- Move: `domain/security/service/UserApiKeyGenerator.java` → `domain/iam/service/UserApiKeyGenerator.java`
- Move: `domain/security/service/DefaultUserApiKeyGenerator.java` → `domain/iam/service/DefaultUserApiKeyGenerator.java`
- Move: `domain/security/service/GeneratedApiKey.java` → `domain/iam/service/GeneratedApiKey.java`
- Move: `domain/security/exception/AuthenticationFailedException.java` → `domain/iam/exception/AuthenticationFailedException.java`
- Move: `domain/security/exception/UnauthorizedException.java` → `domain/iam/exception/UnauthorizedException.java`
- Move: `domain/security/exception/ForbiddenException.java` → `domain/iam/exception/ForbiddenException.java`
- Move: `domain/security/enums/UserState.java` → `domain/iam/enums/UserState.java`
- Move: `domain/team/enums/UserApiKeyState.java` → `domain/iam/enums/UserApiKeyState.java`
- Move: `domain/team/service/UserApiKeyDomainService.java` → `domain/iam/service/UserApiKeyDomainService.java`

- [ ] **Step 1: 创建 IamException 根异常**

```java
package com.codingas.gateway.domain.iam.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * IAM 子域根异常
 */
public class IamException extends GatewayException {

    public IamException(String code, String message) {
        super(code, message);
    }

    public IamException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
```

- [ ] **Step 2: 创建 Identity 值对象（替代 UserAuthResult）**

```java
package com.codingas.gateway.domain.iam.service;

/**
 * 认证后的身份上下文
 *
 * @param userId       用户 ID
 * @param role         用户角色
 * @param credentialId 凭证 ID（UserApiKey ID）
 */
public record Identity(
        Long userId,
        String role,
        Long credentialId
) {
    /** 创建身份 */
    public static Identity of(Long userId, String role, Long credentialId) {
        return new Identity(userId, role, credentialId);
    }
}
```

- [ ] **Step 3: 移动 domain/security 下的 IAM 相关类到 domain/iam**

使用 `git mv` 移动文件，然后批量修改 package 声明和 import。

移动命令：
```bash
# entity
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/entity/User.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/entity/User.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/entity/UserApiKey.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/entity/UserApiKey.java

# gateway 接口
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/gateway/UserGateway.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/gateway/UserGateway.java

# UserApiKeyGateway 从 service 移到 gateway
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/UserApiKeyGateway.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/gateway/UserApiKeyGateway.java

# service
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/AuthenticationDomainService.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/service/AuthenticationDomainService.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/ApiKeyEncryptionDomainService.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/service/ApiKeyEncryptionDomainService.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/UserApiKeyGenerator.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/service/UserApiKeyGenerator.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/DefaultUserApiKeyGenerator.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/service/DefaultUserApiKeyGenerator.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/GeneratedApiKey.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/service/GeneratedApiKey.java

# exception（排除 SecurityException 本身，它被 IamException 替代）
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/exception/AuthenticationFailedException.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/exception/AuthenticationFailedException.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/exception/UnauthorizedException.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/exception/UnauthorizedException.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/exception/ForbiddenException.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/exception/ForbiddenException.java

# enums
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/enums/UserState.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/enums/UserState.java

# 跨域散落修正
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/team/enums/UserApiKeyState.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/enums/UserApiKeyState.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/team/service/UserApiKeyDomainService.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/iam/service/UserApiKeyDomainService.java
```

- [ ] **Step 4: 批量修改 package 声明和 import**

对所有移动的文件修改 package 声明。对项目中所有引用旧路径的文件修改 import。

旧路径 → 新路径映射：
- `domain.security.entity.User` → `domain.iam.entity.User`
- `domain.security.entity.UserApiKey` → `domain.iam.entity.UserApiKey`
- `domain.security.gateway.UserGateway` → `domain.iam.gateway.UserGateway`
- `domain.security.service.UserApiKeyGateway` → `domain.iam.gateway.UserApiKeyGateway`
- `domain.security.service.AuthenticationDomainService` → `domain.iam.service.AuthenticationDomainService`
- `domain.security.service.ApiKeyEncryptionDomainService` → `domain.iam.service.ApiKeyEncryptionDomainService`
- `domain.security.service.UserApiKeyGenerator` → `domain.iam.service.UserApiKeyGenerator`
- `domain.security.service.DefaultUserApiKeyGenerator` → `domain.iam.service.DefaultUserApiKeyGenerator`
- `domain.security.service.GeneratedApiKey` → `domain.iam.service.GeneratedApiKey`
- `domain.security.service.UserAuthResult` → `domain.iam.service.Identity`（重命名）
- `domain.security.exception.SecurityException` → `domain.iam.exception.IamException`（重命名）
- `domain.security.exception.AuthenticationFailedException` → `domain.iam.exception.AuthenticationFailedException`
- `domain.security.exception.UnauthorizedException` → `domain.iam.exception.UnauthorizedException`
- `domain.security.exception.ForbiddenException` → `domain.iam.exception.ForbiddenException`
- `domain.security.enums.UserState` → `domain.iam.enums.UserState`
- `domain.team.enums.UserApiKeyState` → `domain.iam.enums.UserApiKeyState`
- `domain.team.service.UserApiKeyDomainService` → `domain.iam.service.UserApiKeyDomainService`

同时需要修改所有异常类的父类：
- `AuthenticationFailedException` 父类从 `SecurityException` 改为 `IamException`
- `UnauthorizedException` 父类从 `SecurityException` 改为 `IamException`
- `ForbiddenException` 父类从 `SecurityException` 改为 `IamException`

需要修改 import 的消费者文件（62 个 main + test 文件），按 sed 或 IDE refactor 批量替换。

- [ ] **Step 5: 修改 AuthenticationDomainService 返回 Identity**

将 `AuthenticationDomainService.authenticateUser()` 的返回类型从 `UserAuthResult` 改为 `Identity`，内部调用从 `UserAuthResult.of()` 改为 `Identity.of()`。

- [ ] **Step 6: 编译验证**

Run: `./mvnw compile -pl gateway-boot -DskipTests 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 7: 运行测试**

Run: `./mvnw test -pl gateway-boot 2>&1 | tail -20`
Expected: 所有测试通过

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "refactor: 创建 domain/iam 子域，移动身份与访问控制相关类

- 新建 IamException 替代 SecurityException 作为 IAM 子域根异常
- 新建 Identity 值对象替代 UserAuthResult
- 将 User/UserApiKey/UserGateway/UserApiKeyGateway/认证/加密/Key生成 移入 domain/iam
- 将 AuthenticationFailedException/UnauthorizedException/ForbiddenException 移入 domain/iam/exception
- 将 UserApiKeyState 从 team/enums 移入 iam/enums
- 将 UserApiKeyDomainService 从 team/service 移入 iam/service
- 批量更新所有 import 引用

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2: 创建 domain/threat 子域包

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/threat/exception/ThreatException.java`
- Move: `domain/security/entity/IpBlocklist.java` → `domain/threat/entity/IpBlocklist.java`
- Move: `domain/security/gateway/IpBlockGateway.java` → `domain/threat/gateway/IpBlockGateway.java`
- Move: `domain/security/gateway/TokenBucketRateLimiter.java` → `domain/threat/gateway/TokenBucketRateLimiter.java`
- Move: `domain/security/service/RateLimitDomainService.java` → `domain/threat/service/RateLimitDomainService.java`
- Move: `domain/security/service/IpBlocklistDomainService.java` → `domain/threat/service/IpBlocklistDomainService.java`
- Move: `domain/security/service/TokenBucketStatus.java` → `domain/threat/service/TokenBucketStatus.java`
- Move: `domain/security/exception/RateLimitExceededException.java` → `domain/threat/exception/RateLimitExceededException.java`
- Move: `domain/security/exception/IpBlockedException.java` → `domain/threat/exception/IpBlockedException.java`

- [ ] **Step 1: 创建 ThreatException 根异常**

```java
package com.codingas.gateway.domain.threat.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * Threat 子域根异常
 */
public class ThreatException extends GatewayException {

    public ThreatException(String code, String message) {
        super(code, message);
    }

    public ThreatException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
```

- [ ] **Step 2: 移动 domain/security 下的威胁防护类到 domain/threat**

```bash
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/entity/IpBlocklist.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/threat/entity/IpBlocklist.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/gateway/IpBlockGateway.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/threat/gateway/IpBlockGateway.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/gateway/TokenBucketRateLimiter.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/threat/gateway/TokenBucketRateLimiter.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/RateLimitDomainService.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/threat/service/RateLimitDomainService.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/IpBlocklistDomainService.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/threat/service/IpBlocklistDomainService.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/TokenBucketStatus.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/threat/service/TokenBucketStatus.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/exception/RateLimitExceededException.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/threat/exception/RateLimitExceededException.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/exception/IpBlockedException.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/threat/exception/IpBlockedException.java
```

- [ ] **Step 3: 修改 package 声明和 import**

旧路径 → 新路径映射：
- `domain.security.entity.IpBlocklist` → `domain.threat.entity.IpBlocklist`
- `domain.security.gateway.IpBlockGateway` → `domain.threat.gateway.IpBlockGateway`
- `domain.security.gateway.TokenBucketRateLimiter` → `domain.threat.gateway.TokenBucketRateLimiter`
- `domain.security.service.RateLimitDomainService` → `domain.threat.service.RateLimitDomainService`
- `domain.security.service.IpBlocklistDomainService` → `domain.threat.service.IpBlocklistDomainService`
- `domain.security.service.TokenBucketStatus` → `domain.threat.service.TokenBucketStatus`
- `domain.security.exception.RateLimitExceededException` → `domain.threat.exception.RateLimitExceededException`
- `domain.security.exception.IpBlockedException` → `domain.threat.exception.IpBlockedException`

同时修改异常类父类：
- `RateLimitExceededException` 父类从 `SecurityException` 改为 `ThreatException`
- `IpBlockedException` 父类从 `SecurityException` 改为 `ThreatException`

- [ ] **Step 4: 从 RateLimitDomainService 移除 getTokenLimits() 方法**

删除 `RateLimitDomainService.getTokenLimits()` 方法——它属于 quota 用例。`application/quota/TokenLimitService` 已直接调用 `TokenLimitGateway`，无需经限流服务中转。

- [ ] **Step 5: 编译验证**

Run: `./mvnw compile -pl gateway-boot -DskipTests 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 6: 运行测试**

Run: `./mvnw test -pl gateway-boot 2>&1 | tail -20`
Expected: 所有测试通过

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "refactor: 创建 domain/threat 子域，移动限流与IP封禁相关类

- 新建 ThreatException 作为 threat 子域根异常
- 将 IpBlocklist/RateLimit/TokenBucket 相关类移入 domain/threat
- 将 RateLimitExceededException/IpBlockedException 移入 domain/threat/exception
- 从 RateLimitDomainService 移除 getTokenLimits()（属于 quota 用例）
- 批量更新所有 import 引用

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3: 创建 domain/dataprotection 子域包

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/dataprotection/exception/DataProtectionException.java`
- Move: `domain/security/entity/SensitiveDataRule.java` → `domain/dataprotection/entity/SensitiveDataRule.java`
- Move: `domain/security/gateway/SensitiveDataRuleGateway.java` → `domain/dataprotection/gateway/SensitiveDataRuleGateway.java`
- Move: `domain/security/service/SensitiveDataMasker.java` → `domain/dataprotection/service/SensitiveDataMasker.java`

- [ ] **Step 1: 创建 DataProtectionException 根异常**

```java
package com.codingas.gateway.domain.dataprotection.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * DataProtection 子域根异常
 */
public class DataProtectionException extends GatewayException {

    public DataProtectionException(String code, String message) {
        super(code, message);
    }

    public DataProtectionException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
```

- [ ] **Step 2: 移动脱敏相关类到 domain/dataprotection**

```bash
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/entity/SensitiveDataRule.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/dataprotection/entity/SensitiveDataRule.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/gateway/SensitiveDataRuleGateway.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/dataprotection/gateway/SensitiveDataRuleGateway.java
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/SensitiveDataMasker.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/dataprotection/service/SensitiveDataMasker.java
```

- [ ] **Step 3: 修改 package 声明和 import**

旧路径 → 新路径映射：
- `domain.security.entity.SensitiveDataRule` → `domain.dataprotection.entity.SensitiveDataRule`
- `domain.security.gateway.SensitiveDataRuleGateway` → `domain.dataprotection.gateway.SensitiveDataRuleGateway`
- `domain.security.service.SensitiveDataMasker` → `domain.dataprotection.service.SensitiveDataMasker`

- [ ] **Step 4: 编译验证**

Run: `./mvnw compile -pl gateway-boot -DskipTests 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 5: 运行测试**

Run: `./mvnw test -pl gateway-boot 2>&1 | tail -20`
Expected: 所有测试通过

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "refactor: 创建 domain/dataprotection 子域，移动脱敏相关类

- 新建 DataProtectionException 作为子域根异常
- 将 SensitiveDataRule/SensitiveDataRuleGateway/SensitiveDataMasker 移入 domain/dataprotection
- 批量更新所有 import 引用

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4: 迁移 infrastructure 层到子域对齐

**Files:**
- Move: `infrastructure/security/UserGatewayImpl.java` → `infrastructure/iam/gateway/UserGatewayImpl.java`
- Move: `infrastructure/security/database/dataobject/UserDo.java` → `infrastructure/iam/gateway/database/dataobject/UserDo.java`
- Move: `infrastructure/security/database/UserRepository.java` → `infrastructure/iam/gateway/database/repository/UserRepository.java`
- Move: `infrastructure/security/encryption/EncryptionService.java` → `infrastructure/iam/gateway/encryption/EncryptionService.java`
- Move: `infrastructure/security/encryption/Aes256EncryptionService.java` → `infrastructure/iam/gateway/encryption/Aes256EncryptionService.java`
- Move: `infrastructure/team/gateway/UserApiKeyGatewayImpl.java` → `infrastructure/iam/gateway/UserApiKeyGatewayImpl.java`
- Move: `infrastructure/team/gateway/database/dataobject/UserApiKeyDo.java` → `infrastructure/iam/gateway/database/dataobject/UserApiKeyDo.java`
- Move: `infrastructure/team/gateway/database/dataobject/UserApiKeyProductDo.java` → `infrastructure/iam/gateway/database/dataobject/UserApiKeyProductDo.java`
- Move: `infrastructure/team/gateway/database/repository/UserApiKeyRepository.java` → `infrastructure/iam/gateway/database/repository/UserApiKeyRepository.java`
- Move: `infrastructure/team/gateway/database/repository/UserApiKeyProductRepository.java` → `infrastructure/iam/gateway/database/repository/UserApiKeyProductRepository.java`
- Move: `infrastructure/security/InMemoryTokenBucketRateLimiter.java` → `infrastructure/threat/gateway/InMemoryTokenBucketRateLimiter.java`
- Move: `infrastructure/alert/gateway/IpBlockGatewayImpl.java` → `infrastructure/threat/gateway/IpBlockGatewayImpl.java`
- Move: `infrastructure/alert/gateway/IpBlocklistConverter.java` → `infrastructure/threat/gateway/IpBlocklistConverter.java`
- Move: `infrastructure/alert/gateway/database/dataobject/IpBlocklistDo.java` → `infrastructure/threat/gateway/database/dataobject/IpBlocklistDo.java`
- Move: `infrastructure/alert/gateway/database/IpBlocklistRepository.java` → `infrastructure/threat/gateway/database/repository/IpBlocklistRepository.java`
- Move: `infrastructure/alert/gateway/SensitiveDataRuleGatewayImpl.java` → `infrastructure/dataprotection/gateway/SensitiveDataRuleGatewayImpl.java`
- Move: `infrastructure/alert/gateway/SensitiveDataRuleConverter.java` → `infrastructure/dataprotection/gateway/SensitiveDataRuleConverter.java`
- Move: `infrastructure/alert/gateway/database/dataobject/SensitiveDataRuleDo.java` → `infrastructure/dataprotection/gateway/database/dataobject/SensitiveDataRuleDo.java`
- Move: `infrastructure/alert/gateway/database/SensitiveDataRuleRepository.java` → `infrastructure/dataprotection/gateway/database/repository/SensitiveDataRuleRepository.java`
- Move: `infrastructure/security/SensitiveDataRuleInitializer.java` → `infrastructure/dataprotection/SensitiveDataRuleInitializer.java`

- [ ] **Step 1: 移动 infrastructure/iam 相关文件**

```bash
# UserGatewayImpl
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/security/UserGatewayImpl.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/UserGatewayImpl.java

# UserDo + UserRepository
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/security/database/dataobject/UserDo.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/database/dataobject/UserDo.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/security/database/UserRepository.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/database/repository/UserRepository.java

# EncryptionService + Aes256EncryptionService
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/security/encryption/EncryptionService.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/encryption/EncryptionService.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/security/encryption/Aes256EncryptionService.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/encryption/Aes256EncryptionService.java

# UserApiKeyGatewayImpl + DO/Repository 从 infrastructure/team 移入 infrastructure/iam
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/UserApiKeyGatewayImpl.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/UserApiKeyGatewayImpl.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/dataobject/UserApiKeyDo.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/database/dataobject/UserApiKeyDo.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/dataobject/UserApiKeyProductDo.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/database/dataobject/UserApiKeyProductDo.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/repository/UserApiKeyRepository.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/database/repository/UserApiKeyRepository.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/team/gateway/database/repository/UserApiKeyProductRepository.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/iam/gateway/database/repository/UserApiKeyProductRepository.java
```

- [ ] **Step 2: 移动 infrastructure/threat 相关文件**

```bash
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/security/InMemoryTokenBucketRateLimiter.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/threat/gateway/InMemoryTokenBucketRateLimiter.java

git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/alert/gateway/IpBlockGatewayImpl.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/threat/gateway/IpBlockGatewayImpl.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/alert/gateway/IpBlocklistConverter.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/threat/gateway/IpBlocklistConverter.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/alert/gateway/database/dataobject/IpBlocklistDo.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/threat/gateway/database/dataobject/IpBlocklistDo.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/alert/gateway/database/IpBlocklistRepository.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/threat/gateway/database/repository/IpBlocklistRepository.java
```

- [ ] **Step 3: 移动 infrastructure/dataprotection 相关文件**

```bash
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/security/SensitiveDataRuleInitializer.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/dataprotection/SensitiveDataRuleInitializer.java

git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/alert/gateway/SensitiveDataRuleGatewayImpl.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/dataprotection/gateway/SensitiveDataRuleGatewayImpl.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/alert/gateway/SensitiveDataRuleConverter.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/dataprotection/gateway/SensitiveDataRuleConverter.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/alert/gateway/database/dataobject/SensitiveDataRuleDo.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/dataprotection/gateway/database/dataobject/SensitiveDataRuleDo.java
git mv gateway-boot/src/main/java/com/codingas/gateway/infrastructure/alert/gateway/database/SensitiveDataRuleRepository.java \
       gateway-boot/src/main/java/com/codingas/gateway/infrastructure/dataprotection/gateway/database/repository/SensitiveDataRuleRepository.java
```

- [ ] **Step 4: 修改所有移动文件的 package 声明**

批量修改每个文件的 `package` 行，以及引用了这些文件的 import 语句。关键映射：

infrastructure 旧路径 → 新路径：
- `infrastructure.security.UserGatewayImpl` → `infrastructure.iam.gateway.UserGatewayImpl`
- `infrastructure.security.database.dataobject.UserDo` → `infrastructure.iam.gateway.database.dataobject.UserDo`
- `infrastructure.security.database.UserRepository` → `infrastructure.iam.gateway.database.repository.UserRepository`
- `infrastructure.security.encryption.EncryptionService` → `infrastructure.iam.gateway.encryption.EncryptionService`
- `infrastructure.security.encryption.Aes256EncryptionService` → `infrastructure.iam.gateway.encryption.Aes256EncryptionService`
- `infrastructure.team.gateway.UserApiKeyGatewayImpl` → `infrastructure.iam.gateway.UserApiKeyGatewayImpl`
- `infrastructure.team.gateway.database.dataobject.UserApiKeyDo` → `infrastructure.iam.gateway.database.dataobject.UserApiKeyDo`
- `infrastructure.team.gateway.database.dataobject.UserApiKeyProductDo` → `infrastructure.iam.gateway.database.dataobject.UserApiKeyProductDo`
- `infrastructure.team.gateway.database.repository.UserApiKeyRepository` → `infrastructure.iam.gateway.database.repository.UserApiKeyRepository`
- `infrastructure.team.gateway.database.repository.UserApiKeyProductRepository` → `infrastructure.iam.gateway.database.repository.UserApiKeyProductRepository`
- `infrastructure.security.InMemoryTokenBucketRateLimiter` → `infrastructure.threat.gateway.InMemoryTokenBucketRateLimiter`
- `infrastructure.alert.gateway.IpBlockGatewayImpl` → `infrastructure.threat.gateway.IpBlockGatewayImpl`
- `infrastructure.alert.gateway.IpBlocklistConverter` → `infrastructure.threat.gateway.IpBlocklistConverter`
- `infrastructure.alert.gateway.database.dataobject.IpBlocklistDo` → `infrastructure.threat.gateway.database.dataobject.IpBlocklistDo`
- `infrastructure.alert.gateway.database.IpBlocklistRepository` → `infrastructure.threat.gateway.database.repository.IpBlocklistRepository`
- `infrastructure.security.SensitiveDataRuleInitializer` → `infrastructure.dataprotection.SensitiveDataRuleInitializer`
- `infrastructure.alert.gateway.SensitiveDataRuleGatewayImpl` → `infrastructure.dataprotection.gateway.SensitiveDataRuleGatewayImpl`
- `infrastructure.alert.gateway.SensitiveDataRuleConverter` → `infrastructure.dataprotection.gateway.SensitiveDataRuleConverter`
- `infrastructure.alert.gateway.database.dataobject.SensitiveDataRuleDo` → `infrastructure.dataprotection.gateway.database.dataobject.SensitiveDataRuleDo`
- `infrastructure.alert.gateway.database.SensitiveDataRuleRepository` → `infrastructure.dataprotection.gateway.database.repository.SensitiveDataRuleRepository`

同时修改 Aes256EncryptionService 中对 `SecurityException` 的引用，改为 `IamException`。

- [ ] **Step 5: 编译验证**

Run: `./mvnw compile -pl gateway-boot -DskipTests 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 6: 运行测试**

Run: `./mvnw test -pl gateway-boot 2>&1 | tail -20`
Expected: 所有测试通过

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "refactor: infrastructure 层对齐三子域（iam/threat/dataprotection）

- 将 UserGatewayImpl/UserDo/EncryptionService 等移入 infrastructure/iam
- 将 UserApiKeyGatewayImpl 及 DO/Repository 从 infrastructure/team 移入 infrastructure/iam
- 将 InMemoryTokenBucketRateLimiter/IpBlockGatewayImpl 等移入 infrastructure/threat
- 将 SensitiveDataRuleGatewayImpl/Initializer 等移入 infrastructure/dataprotection
- Aes256EncryptionService 中 SecurityException 引用改为 IamException

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 5: 迁移 TokenLimitGateway 到 quota 域

**Files:**
- Move: `domain/security/gateway/TokenLimitGateway.java` → `domain/quota/gateway/TokenLimitGateway.java`
- Modify: `infrastructure/usage/gateway/TokenLimitGatewayImpl.java`（更新 import）

- [ ] **Step 1: 移动 TokenLimitGateway**

```bash
git mv gateway-boot/src/main/java/com/codingas/gateway/domain/security/gateway/TokenLimitGateway.java \
       gateway-boot/src/main/java/com/codingas/gateway/domain/quota/gateway/TokenLimitGateway.java
```

- [ ] **Step 2: 修改 package 声明和 import**

旧路径 → 新路径：
- `domain.security.gateway.TokenLimitGateway` → `domain.quota.gateway.TokenLimitGateway`

需要更新 import 的文件：
- `infrastructure/usage/gateway/TokenLimitGatewayImpl.java`
- `application/quota/TokenLimitServiceImpl.java`
- 测试文件 `TokenLimitServiceTest.java`、`TokenLimitServiceImplTest.java`

- [ ] **Step 3: 编译验证**

Run: `./mvnw compile -pl gateway-boot -DskipTests 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 4: 运行测试**

Run: `./mvnw test -pl gateway-boot 2>&1 | tail -20`
Expected: 所有测试通过

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "refactor: TokenLimitGateway 从 security 域移入 quota 域

Token 限额属于用量管控职责，不属于安全域

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 6: 重构拦截器架构

**Files:**
- Modify: `adapter/interceptor/ApiKeyAuthInterceptor.java`（改为实现 GatewayInterceptor）
- Modify: `adapter/interceptor/TokenAuthInterceptor.java`（已实现 GatewayInterceptor，确认通过 Application Service 调用）
- Create: `adapter/interceptor/RateLimitInterceptor.java`
- Modify: `adapter/interceptor/IPBlockCheckInterceptor.java`（已实现 GatewayInterceptor，确认通过 Application Service 调用）
- Modify: `adapter/interceptor/SecurityInterceptorChain.java`（更新注册逻辑）

- [ ] **Step 1: 改造 ApiKeyAuthInterceptor 实现 GatewayInterceptor**

当前 `ApiKeyAuthInterceptor` 实现了 `HandlerInterceptor`（Spring 原生），需要改为实现 `GatewayInterceptor`。

```java
package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.application.auth.AuthService;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * API Key 认证拦截器
 *
 * <p>验证代理路径的 API Key，通过 AuthService 编排领域服务。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor extends AbstractGatewayInterceptor {

    private final AuthService authService;

    @Override
    public String name() {
        return "ApiKeyAuth";
    }

    @Override
    public int order() {
        return 3;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();

        // 非代理路径跳过
        if (!isProxyPath(path)) {
            return true;
        }

        String apiKey = extractApiKey(request);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("请求缺少 API Key: path={}", path);
            try {
                unauthorized(response, "请提供有效的 API Key");
            } catch (Exception e) {
                log.error("Failed to write unauthorized response", e);
            }
            return false;
        }

        try {
            String clientIp = getClientIp(request);
            Identity identity = authService.authenticate(apiKey, clientIp);
            request.setAttribute("identity", identity);
            return true;
        } catch (Exception e) {
            log.warn("认证失败: path={}, reason={}", path, e.getMessage());
            try {
                unauthorized(response, "无效的 API Key");
            } catch (Exception ex) {
                log.error("Failed to write unauthorized response", ex);
            }
            return false;
        }
    }

    /** 从 Authorization header 或 x-api-key 提取 API Key */
    private String extractApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return request.getHeader("x-api-key");
    }

    /** 判断是否为代理路径 */
    private boolean isProxyPath(String path) {
        return path.startsWith("/v1/chat/completions")
                || path.startsWith("/v1/messages")
                || path.startsWith("/v1/models");
    }
}
```

- [ ] **Step 2: 创建 RateLimitInterceptor**

```java
package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.domain.threat.service.RateLimitDomainService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 限流拦截器
 *
 * <p>基于 API Key 级别的令牌桶限流。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor extends AbstractGatewayInterceptor {

    private final RateLimitDomainService rateLimitDomainService;

    @Override
    public String name() {
        return "RateLimit";
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        // 非代理路径跳过
        String path = request.getRequestURI();
        if (!isProxyPath(path)) {
            return true;
        }

        Identity identity = (Identity) request.getAttribute("identity");
        if (identity == null || identity.credentialId() == null) {
            return true; // 尚未认证，放行给后续认证拦截器处理
        }

        if (!rateLimitDomainService.isAllowed(identity.credentialId())) {
            log.warn("Rate limit exceeded: credentialId={}, path={}", identity.credentialId(), path);
            try {
                response.setStatus(429);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"请求过于频繁，请稍后重试\"}}");
            } catch (Exception e) {
                log.error("Failed to write rate limit response", e);
            }
            return false;
        }

        return true;
    }

    private boolean isProxyPath(String path) {
        return path.startsWith("/v1/chat/completions")
                || path.startsWith("/v1/messages")
                || path.startsWith("/v1/models");
    }
}
```

- [ ] **Step 3: 更新 WebConfig 注册逻辑**

修改 `infrastructure/config/WebConfig.java`，将 `ApiKeyAuthInterceptor` 从 `HandlerInterceptor` 注册改为通过 `SecurityInterceptorChain` 统一管理。确保所有 `GatewayInterceptor` 实现类都被 Spring 自动注入到 `SecurityInterceptorChain`。

- [ ] **Step 4: 编译验证**

Run: `./mvnw compile -pl gateway-boot -DskipTests 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 5: 运行测试**

Run: `./mvnw test -pl gateway-boot 2>&1 | tail -20`
Expected: 所有测试通过

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "refactor: 拦截器统一走 GatewayInterceptor 接口

- ApiKeyAuthInterceptor 从 HandlerInterceptor 改为 GatewayInterceptor
- 新增 RateLimitInterceptor（order=1）
- 拦截器执行顺序：IP封禁(0) → 限流(1) → Token认证(2) → ApiKey认证(3)
- 通过 AuthService 编排领域服务，不跳层

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 7: 拆分异常处理器

**Files:**
- Modify: `adapter/advice/SecurityExceptionHandler.java` → 重命名为 `adapter/advice/IamExceptionHandler.java`
- Create: `adapter/advice/ThreatExceptionHandler.java`

- [ ] **Step 1: 重命名 SecurityExceptionHandler 为 IamExceptionHandler**

修改类名和处理的异常类型——只处理 `IamException` 及其子类：

```java
package com.codingas.gateway.adapter.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.domain.iam.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.iam.exception.ForbiddenException;
import com.codingas.gateway.domain.iam.exception.IamException;
import com.codingas.gateway.domain.iam.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * IAM 子域异常处理器
 */
@Slf4j
@RestControllerAdvice
@Order(1)
public class IamExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException e) {
        log.warn("Authentication required: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(e.getCode(), "Authentication required. Please provide a valid API Key."));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException e) {
        log.warn("Permission denied: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getCode(), "Access denied. You do not have permission to perform this action."));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailed(AuthenticationFailedException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(IamException.class)
    public ResponseEntity<ApiResponse<Void>> handleIamException(IamException e) {
        log.warn("IAM error: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
}
```

- [ ] **Step 2: 创建 ThreatExceptionHandler**

```java
package com.codingas.gateway.adapter.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.domain.threat.exception.IpBlockedException;
import com.codingas.gateway.domain.threat.exception.RateLimitExceededException;
import com.codingas.gateway.domain.threat.exception.ThreatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Threat 子域异常处理器
 */
@Slf4j
@RestControllerAdvice
@Order(2)
public class ThreatExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiResponse.error(e.getCode(), "请求过于频繁，请稍后重试"));
    }

    @ExceptionHandler(IpBlockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleIpBlocked(IpBlockedException e) {
        log.warn("IP blocked: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getCode(), "Your IP has been temporarily blocked."));
    }

    @ExceptionHandler(ThreatException.class)
    public ResponseEntity<ApiResponse<Void>> handleThreatException(ThreatException e) {
        log.warn("Threat error: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
}
```

- [ ] **Step 3: 更新测试文件**

修改 `adapter/advice/SecurityExceptionHandlerTest.java`，重命名为 `IamExceptionHandlerTest.java`，并新增 `ThreatExceptionHandlerTest.java`。更新 import 和断言。

- [ ] **Step 4: 编译验证**

Run: `./mvnw compile -pl gateway-boot -DskipTests 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 5: 运行测试**

Run: `./mvnw test -pl gateway-boot 2>&1 | tail -20`
Expected: 所有测试通过

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "refactor: 拆分异常处理器为 IamExceptionHandler + ThreatExceptionHandler

- SecurityExceptionHandler 重命名为 IamExceptionHandler，只处理 IAM 域异常
- 新增 ThreatExceptionHandler，处理限流和 IP 封禁异常
- 更新测试文件

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 8: 修改 AuthService 与 Proxy 相关引用

**Files:**
- Modify: `application/auth/AuthService.java`
- Modify: `application/auth/AuthServiceImpl.java`
- Modify: `application/proxy/ProxyService.java`
- Modify: `application/proxy/ProxyServiceImpl.java`
- Modify: `application/proxy/ChannelRoutingService.java`
- Modify: `adapter/api/OpenAIController.java`
- Modify: `adapter/api/AnthropicController.java`
- Modify: `adapter/api/SseStreamHelper.java`

- [ ] **Step 1: 修改 AuthService 接口**

```java
package com.codingas.gateway.application.auth;

import com.codingas.gateway.domain.iam.valueobject.Identity;

/**
 * 认证应用服务接口
 */
public interface AuthService {

    /** 认证 API Key，返回身份上下文 */
    Identity authenticate(String apiKey, String clientIp);
}
```

- [ ] **Step 2: 修改 AuthServiceImpl**

```java
package com.codingas.gateway.application.auth;

import com.codingas.gateway.domain.iam.service.AuthenticationDomainService;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationDomainService authenticationService;

    @Override
    public Identity authenticate(String apiKey, String clientIp) {
        Identity identity = authenticationService.authenticate(apiKey);
        log.info("API Key authenticated: userId={}, credentialId={}, ip={}",
                identity.userId(), identity.credentialId(), clientIp);
        return identity;
    }
}
```

注意：删除 `checkPermission()` 空壳方法。

- [ ] **Step 3: 修改 ProxyService/ProxyServiceImpl/ChannelRoutingService 引用**

将所有 `UserAuthResult` 引用改为 `Identity`：
- `ProxyService.proxy()` 参数类型 `UserAuthResult authResult` → `Identity identity`
- `ProxyServiceImpl.proxyStream()` 同理
- `ChannelRoutingService.resolve()` 参数类型同理

- [ ] **Step 4: 修改 Controller 和 SseStreamHelper 引用**

将 `OpenAIController`、`AnthropicController`、`SseStreamHelper` 中的 `UserAuthResult` 改为 `Identity`。

- [ ] **Step 5: 编译验证**

Run: `./mvnw compile -pl gateway-boot -DskipTests 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 6: 运行测试**

Run: `./mvnw test -pl gateway-boot 2>&1 | tail -20`
Expected: 所有测试通过

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "refactor: AuthService 返回 Identity，删除空壳 checkPermission

- UserAuthResult 全面替换为 Identity
- ProxyService/ChannelRoutingService/Controller 统一更新
- 删除 AuthService.checkPermission() 空实现

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 9: 迁移测试文件并清理 domain/security 残留

**Files:**
- Move: `test/domain/security/service/*.java` → 按 IAM/threat/dataprotection 拆分
- Move: `test/infrastructure/user/gateway/UserGatewayImplTest.java` → `test/infrastructure/iam/gateway/`
- Move: `test/infrastructure/alert/gateway/*.java` → 按 threat/dataprotection 拆分
- Move: `test/adapter/advice/SecurityExceptionHandlerTest.java` → `test/adapter/advice/IamExceptionHandlerTest.java`
- Delete: `domain/security/` 目录（如果还有残留）
- Delete: `domain/security/exception/SecurityException.java`（已被 IamException 替代）
- Delete: `domain/security/service/UserAuthResult.java`（已被 Identity 替代）

- [ ] **Step 1: 移动 domain 层测试**

```bash
# IAM 测试
git mv gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/AuthenticationDomainServiceTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/iam/service/AuthenticationDomainServiceTest.java
git mv gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/ApiKeyEncryptionDomainServiceTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/iam/service/ApiKeyEncryptionDomainServiceTest.java
git mv gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/ApiKeyGeneratorTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/iam/service/ApiKeyGeneratorTest.java
git mv gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/UserAuthResultTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/iam/service/IdentityTest.java

# Threat 测试
git mv gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/RateLimitDomainServiceTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/threat/service/RateLimitDomainServiceTest.java
git mv gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/IpBlocklistDomainServiceTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/threat/service/IpBlocklistDomainServiceTest.java
git mv gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/TokenBucketStatusTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/threat/service/TokenBucketStatusTest.java

# DataProtection 测试
git mv gateway-boot/src/test/java/com/codingas/gateway/domain/security/service/SensitiveDataMaskerTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/domain/dataprotection/service/SensitiveDataMaskerTest.java
```

- [ ] **Step 2: 移动 infrastructure 层测试**

```bash
git mv gateway-boot/src/test/java/com/codingas/gateway/infrastructure/user/gateway/UserGatewayImplTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/iam/gateway/UserGatewayImplTest.java
git mv gateway-boot/src/test/java/com/codingas/gateway/infrastructure/alert/gateway/IpBlockGatewayImplTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/threat/gateway/IpBlockGatewayImplTest.java
git mv gateway-boot/src/test/java/com/codingas/gateway/infrastructure/alert/gateway/SensitiveDataRuleGatewayImplTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/infrastructure/dataprotection/gateway/SensitiveDataRuleGatewayImplTest.java
```

- [ ] **Step 3: 移动 adapter 层测试**

```bash
git mv gateway-boot/src/test/java/com/codingas/gateway/adapter/advice/SecurityExceptionHandlerTest.java \
       gateway-boot/src/test/java/com/codingas/gateway/adapter/advice/IamExceptionHandlerTest.java
```

- [ ] **Step 4: 更新测试文件的 package、import 和类名**

- `UserAuthResultTest` → `IdentityTest`，内部断言改用 `Identity.of()`
- 所有测试文件的 import 从 `domain.security.*` 改为对应子域
- 所有测试文件的 import 从 `infrastructure.user/alert.*` 改为对应子域

- [ ] **Step 5: 删除 domain/security 残留**

```bash
# 删除已被替代的旧文件（如果还存在）
rm -f gateway-boot/src/main/java/com/codingas/gateway/domain/security/exception/SecurityException.java
rm -f gateway-boot/src/main/java/com/codingas/gateway/domain/security/service/UserAuthResult.java

# 检查 domain/security 是否还有残留文件
find gateway-boot/src/main/java/com/codingas/gateway/domain/security -type f
# 如果有，确认是否需要移走后删除整个目录
```

- [ ] **Step 6: 编译验证**

Run: `./mvnw compile -pl gateway-boot -DskipTests 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 7: 运行全部测试**

Run: `./mvnw test -pl gateway-boot 2>&1 | tail -30`
Expected: 所有测试通过

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "refactor: 迁移测试文件到三子域包，清理 domain/security 残留

- 测试按 iam/threat/dataprotection 拆分
- UserAuthResultTest 重命名为 IdentityTest
- SecurityExceptionHandlerTest 重命名为 IamExceptionHandlerTest
- 删除 SecurityException.java 和 UserAuthResult.java 旧文件

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 10: 更新章程文档，最终验证

**Files:**
- Modify: `docs/constitution.md`
- Modify: `docs/superpowers/specs/2026-05-24-security-domain-redesign.md`（追加实施状态）

- [ ] **Step 1: 更新 constitution.md 中安全领域相关描述**

将章程中 `domain/security/` 的所有引用更新为三子域结构。关键修改点：
- 2.1 节项目结构中 `security` → `iam` / `threat` / `dataprotection`
- 3.2 节异常分层中 `SecurityException` → `IamException` / `ThreatException`
- 确保所有示例路径与实际代码一致

- [ ] **Step 2: 更新设计文档状态**

在设计文档中追加实施完成记录。

- [ ] **Step 3: 全量编译 + 测试**

Run: `./mvnw clean verify -pl gateway-boot 2>&1 | tail -30`
Expected: BUILD SUCCESS，所有测试通过

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "docs: 更新章程和设计文档，反映安全三子域重构

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```