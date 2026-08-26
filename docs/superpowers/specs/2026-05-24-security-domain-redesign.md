# 安全体系重构设计

> 日期：2026-05-24
> 状态：已确认，待实施

## 1. 背景与问题

当前 `domain/security/` 将身份、凭证、认证、鉴权、限流、IP封禁、脱敏 7 个不同关注点揉在同一个领域包中，存在以下问题：

- **职责混杂**：限流是流量控制，IP封禁是威胁防护，脱敏是数据保护，与认证鉴权本质不同
- **跨域散落**：`UserApiKeyState` 在 `team/enums`，`UserApiKeyDomainService` 在 `team/service`，Gateway 实现在 `infrastructure/alert/`
- **命名模糊**：`UserAuthResult` 程序化命名，`SecurityException` 过于宽泛
- **分层违规**：`TokenLimitGateway` 放在 security 域但操作 quota 域实体；`RateLimitDomainService.getTokenLimits()` 是 quota 用例却挂在限流服务上

## 2. 设计决策

### 2.1 三子域划分

| 子域 | 职责 | 包路径 |
|------|------|--------|
| **iam** | 身份与访问控制：用户、凭证、认证、鉴权 | `domain/iam/` |
| **threat** | 威胁防护：限流、IP封禁 | `domain/threat/` |
| **dataprotection** | 数据保护：脱敏规则 | `domain/dataprotection/` |

**决策依据**：llm-gateway 是 API 网关，不是企业 IAM 平台，不过度拆分。identity 与 accesscontrol 合并为 iam，限流与 IP 封禁合并为 threat，脱敏独立为 dataprotection。

### 2.2 拦截器架构

- 所有拦截器统一走 `GatewayInterceptor` 接口
- 通过 Application Service 调用 Domain，不跳层
- 执行顺序：IP封禁(0) → 限流(1) → Token认证(2) → ApiKey认证(3) → 鉴权(4)

### 2.3 Application 层按用例分包

Application 层用 `auth/` 而非 `iam/`——用例维度命名，不与 Domain 子域同名。

### 2.4 异常按子域内聚

每个子域有独立根异常，子异常内聚在子域内。

## 3. 目标包结构

```
domain/
├── iam/                                    # 身份与访问控制
│   ├── entity/
│   │   ├── User.java
│   │   └── UserApiKey.java
│   ├── gateway/
│   │   ├── UserGateway.java
│   │   └── UserApiKeyGateway.java
│   ├── service/
│   │   ├── AuthenticationDomainService.java
│   │   ├── ApiKeyEncryptionDomainService.java
│   │   ├── UserApiKeyGenerator.java
│   │   ├── DefaultUserApiKeyGenerator.java
│   │   ├── UserApiKeyDomainService.java
│   │   ├── Identity.java                   # 认证后的身份上下文（原 UserAuthResult）
│   │   └── GeneratedApiKey.java
│   ├── exception/
│   │   ├── IamException.java               # 子域根异常
│   │   ├── AuthenticationFailedException.java
│   │   ├── UnauthorizedException.java
│   │   └── ForbiddenException.java
│   └── enums/
│       ├── UserApiKeyState.java             # 从 team/enums 移入
│       ├── UserState.java
│       └── UserRole.java
│
├── threat/                                 # 威胁防护
│   ├── entity/
│   │   └── IpBlocklist.java
│   ├── gateway/
│   │   ├── IpBlockGateway.java
│   │   └── TokenBucketRateLimiter.java
│   ├── service/
│   │   ├── RateLimitDomainService.java
│   │   ├── IpBlocklistDomainService.java
│   │   └── TokenBucketStatus.java
│   └── exception/
│       ├── ThreatException.java             # 子域根异常
│       ├── RateLimitExceededException.java
│       └── IpBlockedException.java
│
└── dataprotection/                         # 数据保护
    ├── entity/
    │   └── SensitiveDataRule.java
    ├── gateway/
    │   └── SensitiveDataRuleGateway.java
    ├── service/
    │   └── SensitiveDataMasker.java
    └── exception/
        └── DataProtectionException.java     # 预留

infrastructure/
├── iam/
│   └── gateway/
│       ├── UserGatewayImpl.java
│       ├── UserApiKeyGatewayImpl.java
│       ├── database/
│       │   ├── UserDo.java
│       │   ├── UserRepository.java
│       │   ├── UserApiKeyDo.java
│       │   └── UserApiKeyRepository.java
│       └── encryption/
│           ├── EncryptionService.java
│           └── Aes256EncryptionService.java
│
├── threat/
│   └── gateway/
│       ├── IpBlockGatewayImpl.java
│       ├── InMemoryTokenBucketRateLimiter.java
│       └── database/
│           ├── IpBlocklistDo.java
│           └── IpBlocklistRepository.java
│
└── dataprotection/
    └── gateway/
        ├── SensitiveDataRuleGatewayImpl.java
        └── database/
            ├── SensitiveDataRuleDo.java
            └── SensitiveDataRuleRepository.java

application/
├── auth/                                   # 认证用例
│   ├── AuthService.java
│   ├── AuthServiceImpl.java
│   └── dto/
│       └── LoginResponse.java
├── userapikey/                             # API Key 管理用例（已有）
│   ├── UserApiKeyService.java
│   ├── UserApiKeyServiceImpl.java
│   └── dto/
│       ...
└── quota/                                  # Token 限额用例（已有，TokenLimitGateway 归此）

adapter/
├── advice/
│   ├── IamExceptionHandler.java            # 原 SecurityExceptionHandler
│   └── ThreatExceptionHandler.java         # 新增
├── interceptor/
│   ├── GatewayInterceptor.java             # 统一接口（不变）
│   ├── AbstractGatewayInterceptor.java     # 不变
│   ├── SecurityInterceptorChain.java       # 不变
│   ├── IPBlockCheckInterceptor.java        # 统一走 GatewayInterceptor
│   ├── RateLimitInterceptor.java           # 新增，从链中提取
│   ├── TokenAuthInterceptor.java           # 统一走 GatewayInterceptor
│   └── ApiKeyAuthInterceptor.java          # 统一走 GatewayInterceptor
```

## 4. 迁移清单

### 4.1 domain/security → domain/iam

| 类 | 当前位置 | 目标位置 |
|---|---------|---------|
| `User` | `domain/security/entity/` | `domain/iam/entity/` |
| `UserApiKey` | `domain/security/entity/` | `domain/iam/entity/` |
| `UserGateway` | `domain/security/gateway/` | `domain/iam/gateway/` |
| `UserApiKeyGateway` | `domain/security/service/` | `domain/iam/gateway/` |
| `AuthenticationDomainService` | `domain/security/service/` | `domain/iam/service/` |
| `ApiKeyEncryptionDomainService` | `domain/security/service/` | `domain/iam/service/` |
| `UserApiKeyGenerator` | `domain/security/service/` | `domain/iam/service/` |
| `DefaultUserApiKeyGenerator` | `domain/security/service/` | `domain/iam/service/` |
| `UserAuthResult` | `domain/security/service/` | `domain/iam/service/` → **重命名为 `Identity`** |
| `GeneratedApiKey` | `domain/security/service/` | `domain/iam/service/` |
| `SecurityException` | `domain/security/exception/` | `domain/iam/exception/` → **重命名为 `IamException`** |
| `AuthenticationFailedException` | `domain/security/exception/` | `domain/iam/exception/` |
| `UnauthorizedException` | `domain/security/exception/` | `domain/iam/exception/` |
| `ForbiddenException` | `domain/security/exception/` | `domain/iam/exception/` |

### 4.2 domain/security → domain/threat

| 类 | 当前位置 | 目标位置 |
|---|---------|---------|
| `IpBlocklist` | `domain/security/entity/` | `domain/threat/entity/` |
| `IpBlockGateway` | `domain/security/gateway/` | `domain/threat/gateway/` |
| `TokenBucketRateLimiter` | `domain/security/gateway/` | `domain/threat/gateway/` |
| `RateLimitDomainService` | `domain/security/service/` | `domain/threat/service/` |
| `IpBlocklistDomainService` | `domain/security/service/` | `domain/threat/service/` |
| `TokenBucketStatus` | `domain/security/service/` | `domain/threat/service/` |
| `RateLimitExceededException` | `domain/security/exception/` | `domain/threat/exception/` |
| `IpBlockedException` | `domain/security/exception/` | `domain/threat/exception/` |

### 4.3 domain/security → domain/dataprotection

| 类 | 当前位置 | 目标位置 |
|---|---------|---------|
| `SensitiveDataRule` | `domain/security/entity/` | `domain/dataprotection/entity/` |
| `SensitiveDataRuleGateway` | `domain/security/gateway/` | `domain/dataprotection/gateway/` |
| `SensitiveDataMasker` | `domain/security/service/` | `domain/dataprotection/service/` |

### 4.4 跨域散落修正

| 类 | 当前位置 | 目标位置 |
|---|---------|---------|
| `UserApiKeyState` | `domain/team/enums/` | `domain/iam/enums/` |
| `UserApiKeyDomainService` | `domain/team/service/` | `domain/iam/service/` |
| `UserApiKeyGatewayImpl` | `infrastructure/team/gateway/` | `infrastructure/iam/gateway/` |
| `UserApiKeyDo` / `UserApiKeyRepository` | `infrastructure/team/gateway/database/` | `infrastructure/iam/gateway/database/` |
| `IpBlockGatewayImpl` | `infrastructure/alert/gateway/` | `infrastructure/threat/gateway/` |
| `IpBlocklistDo` / `IpBlocklistRepository` | `infrastructure/alert/gateway/database/` | `infrastructure/threat/gateway/database/` |
| `SensitiveDataRuleGatewayImpl` | `infrastructure/alert/gateway/` | `infrastructure/dataprotection/gateway/` |
| `SensitiveDataRuleDo` 等 | `infrastructure/alert/gateway/database/` | `infrastructure/dataprotection/gateway/database/` |

### 4.5 infrastructure 层对齐

| 类 | 当前位置 | 目标位置 |
|---|---------|---------|
| `UserGatewayImpl` | `infrastructure/security/` | `infrastructure/iam/gateway/` |
| `UserDo` / `UserRepository` | `infrastructure/security/database/` | `infrastructure/iam/gateway/database/` |
| `InMemoryTokenBucketRateLimiter` | `infrastructure/security/` | `infrastructure/threat/gateway/` |
| `EncryptionService` + `Aes256EncryptionService` | `infrastructure/security/encryption/` | `infrastructure/iam/gateway/encryption/` |
| `SensitiveDataRuleInitializer` | `infrastructure/security/` | `infrastructure/dataprotection/` |

### 4.6 特殊处理

| 项 | 说明 |
|---|------|
| `TokenLimitGateway` | 从 `domain/security/gateway/` 移至 `domain/quota/gateway/`（Token 限额是 quota 职责） |
| `RateLimitDomainService.getTokenLimits()` | 移至 `application/quota/TokenLimitService` 直接调用 Gateway |
| `SecurityConfig` | 保留在 `infrastructure/config/`（全局配置，不属于任何子域） |
| `SecurityInterceptorChain` | 保留在 `adapter/interceptor/`（适配器层概念） |
| `SecurityExceptionHandler` | 重命名为 `IamExceptionHandler`，拆出 `ThreatExceptionHandler` |

## 5. 异常继承关系

```
GatewayException (common/exception/)
├── IamException (domain/iam/exception/)
│   ├── AuthenticationFailedException
│   ├── UnauthorizedException
│   └── ForbiddenException
├── ThreatException (domain/threat/exception/)
│   ├── RateLimitExceededException
│   └── IpBlockedException
├── DataProtectionException (domain/dataprotection/exception/)
└── ... (其他域异常不变)
```

## 6. Identity 不可变对象

替代原 `UserAuthResult`，语义更精准：

```java
// domain/iam/service/Identity.java
public record Identity(
    Long userId,
    String role,
    Long credentialId
) {
    public static Identity of(Long userId, String role, Long credentialId) {
        return new Identity(userId, role, credentialId);
    }
}
```

## 7. AuthService 修正

- 返回 `Identity` 而非 `UserAuthResult`
- 失败由 Domain 层抛 `AuthenticationFailedException`，不再返回 null
- 删除空壳 `checkPermission()`，等需要 RBAC 时再加
