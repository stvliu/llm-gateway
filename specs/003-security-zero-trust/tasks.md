# Implementation Tasks: 安全零信任

**Branch**: `003-security-zero-trust` | **Date**: 2026-04-24 | **Plan**: [plan.md](./plan.md)

## Overview

本任务清单基于 plan.md 的实现阶段分解，共 19 个任务，覆盖 Phase 1-4。
**认证框架**: Sa-Token (非 Spring Security)

## Phase 1: 基础设施 (Foundation)

| ID | Task | Description | Dependencies | Files | Priority | Parallel |
|----|------|-------------|--------------|-------|----------|----------|
| T001 | 数据库迁移脚本 | 创建 GatewayApiKey、ProviderApiKey、User、AuditLog、RateLimitConfig、SensitiveDataRule、IpBlocklist 表，含审计字段和索引 | - | `gateway-application/src/main/resources/db/migration/V4__add_security_tables.sql` | P1 | [P] |
| T002 | JPA 实体类 - GatewayApiKey | 实现 GatewayApiKey 实体：用户调用网关的凭证，采用与 OpenAI 一致的格式 `sk-` 前缀 + 32位加密随机字符串（如 `sk-xK9mP2vL8nQ4wF7hJ3dR6tB0yC5sE8gU`），包含encrypted_key、user_id、status、rate_limit_config_id、expires_at，含 Getter/Setter | T001 | `gateway-core/src/main/java/com/codingas/gateway/core/domain/entity/GatewayApiKey.java` | P1 | [P] |
| T002b | JPA 实体类 - ProviderApiKey | 实现 ProviderApiKey 实体：存储加密的模型提供商凭证（OpenAI API Key、Anthropic API Key等），包含provider_id、encrypted_key、status，含 Getter/Setter | T001 | `gateway-core/src/main/java/com/codingas/gateway/core/domain/entity/ProviderApiKey.java` | P1 | [P] |
| T003 | JPA 实体类 - User | 实现 User 实体：user_code、name、role、status | T001 | `gateway-core/src/main/java/com/codingas/gateway/core/domain/entity/User.java` | P1 | [P] |
| T004 | JPA 实体类 - AuditLog/RateLimitConfig/SensitiveDataRule/IpBlocklist | 实现 AuditLog、RateLimitConfig、SensitiveDataRule、IpBlocklist 实体 | T001 | `gateway-core/src/main/java/com/codingas/gateway/core/domain/entity/AuditLog.java`, `gateway-core/src/main/java/com/codingas/gateway/core/domain/entity/RateLimitConfig.java`, `gateway-core/src/main/java/com/codingas/gateway/core/domain/entity/SensitiveDataRule.java`, `gateway-core/src/main/java/com/codingas/gateway/core/domain/entity/IpBlocklist.java` | P1 | [P] |
| T005 | API Key 加密服务 | 实现 ApiKeyEncryptionService：AES-256-GCM 加密/解密，密钥从环境变量注入，支持 GatewayApiKey 和 ProviderApiKey 两类凭证的加密存储和运行时解密 | T002, T002b | `gateway-core/src/main/java/com/codingas/gateway/core/security/encryption/ApiKeyEncryptionService.java` | P1 | |
| T006 | Sa-Token 认证集成 | 配置 Sa-Token：自定义 API Key 认证方式、StpLogic 实现、认证异常处理 | T005 | `gateway-application/src/main/java/com/codingas/gateway/config/satoken/SaTokenConfig.java`, `gateway-application/src/main/java/com/codingas/gateway/config/satoken/ApiKeyAuthAdapter.java` | P1 | |
| T007 | 认证服务 | 实现 AuthenticationService：API Key 验证、解密、用户信息加载，支持 Redis 缓存 | T005 | `gateway-core/src/main/java/com/codingas/gateway/core/security/authentication/AuthenticationService.java` | P1 | |
| T008 | 安全配置 | 实现 Web 层安全配置：Sa-Token 全局异常处理器 SecurityExceptionHandler | T006 | `gateway-web/src/main/java/com/codingas/gateway/web/security/SecurityExceptionHandler.java` | P1 | |

## Phase 2: 核心安全 (Core Security)

| ID | Task | Description | Dependencies | Files | Priority | Parallel |
|----|------|-------------|--------------|-------|----------|----------|
| T009 | RBAC 权限服务 | 实现 RbacService：角色管理、权限检查、模型访问控制，支持管理员/普通用户/只读用户角色，基于 Sa-Token 权限码 | T007, T008 | `gateway-core/src/main/java/com/codingas/gateway/core/security/authorization/RbacService.java`, `gateway-core/src/main/java/com/codingas/gateway/core/security/authorization/Permission.java`, `gateway-core/src/main/java/com/codingas/gateway/core/security/authorization/Role.java` | P1 | [P] |
| T010 | 流量限流服务 | 实现 TokenBucketRateLimiter 和 RateLimitService：令牌桶算法、限流配置加载、Redis 分布式计数、支持 fail-open/fail-close 策略 | T007, T008 | `gateway-core/src/main/java/com/codingas/gateway/core/security/ratelimit/TokenBucketRateLimiter.java`, `gateway-core/src/main/java/com/codingas/gateway/core/security/ratelimit/RateLimitService.java` | P1 | [P] |
| T011 | 敏感数据脱敏服务 | 实现 SensitiveDataMasker：手机号、身份证、银行卡脱敏规则，支持正则表达式自定义规则 | T007, T008 | `gateway-core/src/main/java/com/codingas/gateway/core/security/masking/SensitiveDataMasker.java`, `gateway-core/src/main/java/com/codingas/gateway/core/security/masking/SensitiveDataRule.java` | P2 | [P] |
| T012 | 审计日志服务 | 实现 AuditService：记录所有 API 调用日志（调用者、时间、请求内容、响应状态、响应时间），支持按用户/时间/操作类型查询，滚动策略保留 90 天 | T007, T008 | `gateway-core/src/main/java/com/codingas/gateway/core/security/audit/AuditService.java`, `gateway-core/src/main/java/com/codingas/gateway/core/security/audit/AuditLogEntity.java` | P2 | [P] |

## Phase 3: 高级安全 (Advanced Security)

| ID | Task | Description | Dependencies | Files | Priority | Parallel |
|----|------|-------------|--------------|-------|----------|----------|
| T013 | IP 黑名单服务 | 实现 IpBlocklistService：动态 IP 封禁/解封、黑名单查询、封禁原因记录，支持数据库持久化 | T009, T010 | `gateway-core/src/main/java/com/codingas/gateway/core/security/ipblock/IpBlocklistService.java`, `gateway-core/src/main/java/com/codingas/gateway/core/security/ipblock/IpBlocklistEntity.java` | P2 | [P] |
| T014 | 暴力破解防护服务 | 实现 BruteForceProtectionService：连续 5 次失败后封禁 IP 15 分钟，Redis 原子计数，支持分布式环境 | T009, T010 | `gateway-core/src/main/java/com/codingas/gateway/core/security/bruteforce/BruteForceProtectionService.java`, `gateway-core/src/main/java/com/codingas/gateway/core/security/bruteforce/FailedAttemptTracker.java` | P2 | [P] |
| T015 | API Key 过期提醒 | 实现过期提醒机制：提前 7 天通知，支持定时任务扫描即将过期的 Key，发送通知（可扩展为邮件/短信/Webhook）。**必须包含**：1) 扫描任务执行日志记录；2) 漏发告警机制；3) 手动触发接口用于补发 | T005 | `gateway-core/src/main/java/com/codingas/gateway/core/security/authentication/GatewayApiKeyExpirationNotifier.java` | P2 | [P] |

## Phase 4: 可观测性 (Observability)

| ID | Task | Description | Dependencies | Files | Priority | Parallel |
|----|------|-------------|--------------|-------|----------|----------|
| T016 | 安全事件追踪 | 所有安全事件带 Trace ID 全链路追踪，集成 OpenTelemetry，认证/授权/限流/脱敏事件均记录 trace_id | T009, T010, T011, T012 | `gateway-core/src/main/java/com/codingas/gateway/core/security/tracing/SecurityTracingInterceptor.java` | P1 | [P] |
| T017 | 安全指标 | 实现安全指标收集：认证成功率、限流触发次数、暴力破解封禁次数、脱敏处理量，支持 Prometheus 格式导出 | T016 | `gateway-core/src/main/java/com/codingas/gateway/core/security/metrics/SecurityMetricsService.java`, `gateway-core/src/main/java/com/codingas/gateway/core/security/metrics/SecurityMetrics.java` | P2 | [P] |

## Task Dependencies Graph

```
Phase 1 (基础设施)
├── T001 (数据库迁移) ─────────────────────────────────────┐
├── T002 (GatewayApiKey 实体) ──┐                          │
├── T002b (ProviderApiKey 实体) ─┤                         │
├── T003 (User 实体) ──────────┤                          │
├── T004 (其他实体) ─────────────┤                          │
├── T005 (加密服务) ←────────────┴── T002, T002b           │
├── T006 (Sa-Token集成) ←────────────── T005                │
├── T007 (认证服务) ←────────────────────── T005            │
└── T008 (安全配置) ←────────────────────────── T006, T007

Phase 2 (核心安全) ←── All Phase 1 tasks
├── T009 (RBAC) ←───────────────────────────────────────── T007, T008
├── T010 (限流) ←───────────────────────────────────────── T007, T008
├── T011 (脱敏) ←──────────────────────────────────────── T007, T008
└── T012 (审计) ←──────────────────────────────────────── T007, T008

Phase 3 (高级安全) ←── All Phase 2 tasks
├── T013 (IP黑名单) ←───────────────────────────────────── T009, T010
├── T014 (暴力破解防护) ←──────────────────────────────── T009, T010
└── T015 (过期提醒) ←───────────────────────────────────── T005

Phase 4 (可观测性) ←── All Phase 2/3 tasks
├── T016 (安全追踪) ←───────────────────────────────────── T009, T010, T011, T012
└── T017 (安全指标) ←──────────────────────────────────── T016
```

## Implementation Order

1. **第一批次** (可并行): T001, T002, T002b, T003, T004
2. **第二批次** (可并行): T005, T006, T007, T008 (依赖 T001-T004)
3. **第三批次** (可并行): T009, T010, T011, T012 (依赖 T007, T008)
4. **第四批次** (可并行): T013, T014, T015 (依赖 Phase 2)
5. **第五批次** (可并行): T016, T017 (依赖 Phase 2/3 完成)

## Priority Summary

| Priority | Tasks |
|----------|-------|
| P1 | T001-T010, T016 |
| P2 | T002b, T011-T015, T017 |

## Files Summary

| Module | Files |
|--------|-------|
| gateway-application | `V4__add_security_tables.sql`, `SaTokenConfig.java`, `ApiKeyAuthAdapter.java` |
| gateway-core/domain/entity | `GatewayApiKey.java`, `ProviderApiKey.java`, `User.java`, `AuditLogEntity.java`, `RateLimitConfig.java`, `SensitiveDataRule.java`, `IpBlocklistEntity.java` |
| gateway-core/security/authentication | `AuthenticationService.java`, `GatewayApiKeyExpirationNotifier.java` |
| gateway-core/security/authorization | `RbacService.java`, `Permission.java`, `Role.java` |
| gateway-core/security/ratelimit | `TokenBucketRateLimiter.java`, `RateLimitService.java` |
| gateway-core/security/masking | `SensitiveDataMasker.java`, `SensitiveDataRule.java` |
| gateway-core/security/audit | `AuditService.java`, `AuditLogEntity.java` |
| gateway-core/security/ipblock | `IpBlocklistService.java`, `IpBlocklistEntity.java` |
| gateway-core/security/bruteforce | `BruteForceProtectionService.java`, `FailedAttemptTracker.java` |
| gateway-core/security/tracing | `SecurityTracingInterceptor.java` |
| gateway-core/security/metrics | `SecurityMetricsService.java`, `SecurityMetrics.java` |
| gateway-web/security | `SecurityExceptionHandler.java` |

## Success Criteria

- [x] 所有 19 个任务完成
- [x] Phase 1 任务通过单元测试
- [x] Phase 2 核心安全功能集成测试通过
- [x] Phase 3 高级安全功能通过压力测试
- [x] Phase 4 可观测性指标可正常采集
- [x] 核心服务覆盖率 >= 90%，安全模块覆盖率 >= 85%
