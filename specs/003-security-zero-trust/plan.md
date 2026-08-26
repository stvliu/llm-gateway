# Implementation Plan: 安全零信任

**Branch**: `003-security-zero-trust` | **Date**: 2026-04-24 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-security-zero-trust/spec.md`

## Summary

实现企业级 AI 网关的安全零信任体系，包括：统一身份认证（API Key）、基于角色的访问控制（RBAC）、流量限速保护、敏感数据脱敏、审计日志记录、凭证安全存储（AES-256加密）、IP 黑名单、暴力破解防护和 API Key 过期提醒九大核心安全能力。

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.5.x, Sa-Token (认证与授权框架), Spring Data JPA, Redis
**Storage**: PostgreSQL 14+ (主数据), Redis (限流计数、缓存)
**Testing**: JUnit 5, Mockito, Testcontainers
**Target Platform**: Linux Server (Docker)
**Project Type**: Gateway API Service (Java Backend)
**Performance Goals**: 10,000 QPS 认证请求处理能力
**Constraints**: 认证延迟 <100ms，限流延迟 <100ms
**Scale/Scope**: ~10000用户 / 单实例 10,000 QPS

## Constitution Check

| 原则 | 状态 | 说明 |
|------|------|------|
| 双 API 兼容 | ✅ 不适用 | 本需求不涉及协议转换 |
| 安全零信任 | ✅ 必须满足 | 所有请求必须经过认证、鉴权、限流、脱敏四层检查 |
| 测试驱动开发 | ✅ 必须满足 | 核心服务覆盖率 ≥90%，安全模块 ≥85% |
| 可观测性内建 | ✅ 必须满足 | 所有安全事件必须带 Trace ID 全链路追踪 |
| Token 成本透明 | ⚠️ 部分相关 | 审计日志需记录 Token 使用量 |
| 分层架构 | ✅ 必须满足 | 上层依赖下层接口，禁止跨层调用 |
| 模型纯洁性 | ✅ 必须满足 | JPA 实体只含 Getter/Setter |
| 配置外部化 | ✅ 必须满足 | 所有安全参数通过 @ConfigurationProperties |
| 全实体可审计 | ✅ 必须满足 | 所有安全相关表含审计字段 |

## Project Structure

### Documentation (this feature)

```text
specs/003-security-zero-trust/
├── plan.md              # This file
├── research.md          # Phase 0: 技术调研 (可选)
├── data-model.md        # Phase 1: 数据模型设计
├── quickstart.md        # Phase 1: 快速开始指南
├── contracts/           # Phase 1: 接口契约
│   ├── AuthService.java
│   ├── RbacService.java
│   └── AuditService.java
└── tasks.md             # Phase 2: 任务分解 (由 /speckit.tasks 生成)
```

### Source Code (repository root)

```text
gateway-core/
├── src/main/java/com/codingas/gateway/core/
│   ├── security/                    # 安全模块
│   │   ├── authentication/           # 认证
│   │   │   ├── ApiKeyAuthenticationFilter.java
│   │   │   ├── ApiKeyAuthenticationToken.java
│   │   │   └── AuthenticationService.java
│   │   ├── authorization/           # 授权
│   │   │   ├── RbacService.java
│   │   │   ├── Permission.java
│   │   │   └── Role.java
│   │   ├── ratelimit/              # 限流
│   │   │   ├── TokenBucketRateLimiter.java
│   │   │   └── RateLimitService.java
│   │   ├── encryption/              # 加密
│   │   │   ├── Aes256EncryptionService.java  (已有)
│   │   │   └── ApiKeyEncryptionService.java
│   │   ├── masking/                # 脱敏
│   │   │   ├── SensitiveDataMasker.java
│   │   │   └── SensitiveDataRule.java
│   │   ├── audit/                  # 审计
│   │   │   ├── AuditService.java
│   │   │   └── AuditLog.java
│   │   ├── ipblock/                # IP 黑名单
│   │   │   ├── IpBlocklistService.java
│   │   │   └── IpBlocklist.java
│   │   └── brute-force/            # 暴力破解防护
│   │       ├── BruteForceProtectionService.java
│   │       └── FailedAttemptTracker.java
│   └── domain/entity/              # 实体 (Phase 1)
│       ├── GatewayApiKey.java
│       ├── ProviderApiKey.java
│       ├── User.java
│       ├── AuditLog.java
│       ├── RateLimitConfig.java
│       ├── SensitiveDataRule.java
│       └── IpBlocklist.java

gateway-web/
├── src/main/java/com/codingas/gateway/web/
│   ├── security/                   # Web 层安全配置
│   │   ├── SecurityConfig.java
│   │   └── SecurityExceptionHandler.java
│   └── controller/                 # 控制器 (如有需要)
│       └── AdminSecurityController.java

gateway-application/
├── src/main/resources/
│   ├── db/migration/               # 数据库迁移
│   │   └── V4__add_security_tables.sql
│   └── config/                     # 配置
│       └── security.yml
```

**Structure Decision**: 基于现有分层架构，安全模块位于 `gateway-core` 的 `security/` 包下，Web 层安全配置位于 `gateway-web`。遵循 Constitution 规定的分层依赖原则。

## Implementation Phases

### Phase 1: 基础设施 (Foundation)

| Step | Task | Description | Dependencies |
|------|------|-------------|---------------|
| 1.1 | 数据库迁移 | 创建 GatewayApiKey、ProviderApiKey、User、AuditLog、RateLimitConfig、SensitiveDataRule、IpBlocklist 表 | None |
| 1.2 | JPA 实体 | 实现上述 7 个实体类，含 Getter/Setter | 1.1 |
| 1.3 | API Key 加密 | 实现 ApiKeyEncryptionService，加密存储和运行时解密 | 1.2 |
| 1.4 | 认证过滤器 | 实现 ApiKeyAuthenticationFilter，拦截请求进行认证 | 1.3 |

### Phase 2: 核心安全 (Core Security)

| Step | Task | Description | Dependencies |
|------|------|-------------|---------------|
| 2.1 | RBAC 权限控制 | 实现 RbacService，支持角色和权限管理 | Phase 1 |
| 2.2 | 流量限流 | 实现 TokenBucketRateLimiter，支持限流配置 | Phase 1 |
| 2.3 | 敏感数据脱敏 | 实现 SensitiveDataMasker，支持手机号、身份证、银行卡脱敏 | Phase 1 |
| 2.4 | 审计日志 | 实现 AuditService，记录所有 API 调用 | Phase 1 |

### Phase 3: 高级安全 (Advanced Security)

| Step | Task | Description | Dependencies |
|------|------|-------------|---------------|
| 3.1 | IP 黑名单 | 实现 IpBlocklistService，支持动态封禁 | Phase 2 |
| 3.2 | 暴力破解防护 | 实现 BruteForceProtectionService，5次失败封禁15分钟 | Phase 2 |
| 3.3 | API Key 过期提醒 | 实现过期提醒机制，提前7天通知 | Phase 1 |

### Phase 4: 可观测性 (Observability)

| Step | Task | Description | Dependencies |
|------|------|-------------|---------------|
| 4.1 | 安全事件追踪 | 所有安全事件带 Trace ID | All Phase |
| 4.2 | 安全指标 | 认证成功率、限流触发次数等指标 | All Phase |

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| 认证方式 | API Key (UUID v4) | 符合 Assumptions，无状态便于水平扩展 |
| 限流算法 | 令牌桶 | 支持突发流量，符合 Assumptions |
| 加密算法 | AES-256-GCM | 符合 Assumptions 和 FR-006 |
| 密钥管理 | 环境变量注入 | 符合 Constitution 安全要求 |
| 失败追踪 | Redis 存储 | 支持分布式环境下多实例共享 |
| 黑名单存储 | 数据库 | 持久化，支持动态管理 |

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| API Key 加密密钥泄露 | Critical | 密钥通过环境变量注入，不在代码中存储 |
| 限流性能瓶颈 | Medium | 使用 Redis 分布式计数，本地缓存优化 |
| 暴力破解检测延迟 | Medium | 使用 Redis 原子操作，保证计数准确性 |
| 敏感数据漏脱敏 | High | 多层验证 + 规则可配置 |
