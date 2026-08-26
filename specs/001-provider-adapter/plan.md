# Implementation Plan: Provider Adapter Framework

**Branch**: `001-provider-adapter` | **Date**: 2026-04-23 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-provider-adapter/spec.md`

## Summary

实现 LLM-Gateway 的 Provider 适配器框架，定义标准接口 `LLMProviderAdapter`，支持 OpenAI/Anthropic 等模型提供商的接入。核心目标是实现**开闭原则**——对扩展开放、对修改关闭，使新增 Provider 无需修改框架代码。

技术方案：Java SPI 机制 + 适配器模式 + Spring Boot 自动配置

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.5.x, Spring MVC (Web), JPA (数据持久化)
**Storage**: H2（开发调试）/ PostgreSQL 14+（生产）
**Testing**: JUnit 5, Mockito (单元测试), Integration Tests
**Target Platform**: Linux Server (K8s 云原生部署)
**Project Type**: Web Service (API Gateway)
**Performance Goals**: 单实例 10,000 QPS，适配器调用延迟 ≤10ms P95
**Constraints**: 虚拟线程支持，OpenTelemetry 追踪，API 密钥 AES-256 加密
**Scale/Scope**: ~10,000 用户 / ~100 Provider / ~1,000 Model

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 分层架构（Web→应用→调度→服务→基础设施） | ✅ | 适配器层位于基础设施层，上层依赖下层 |
| 模型纯洁性（实体仅 Getter/Setter） | ✅ | 实体仅含数据属性，业务逻辑在 Service 层 |
| 配置外部化（@ConfigurationProperties） | ✅ | Provider 配置通过 @ConfigurationProperties 注入 |
| 全实体可审计（created_by/updated_by） | ✅ | 所有实体包含审计字段 |
| 开闭原则（对扩展开放） | ✅ | 通过 SPI + 适配器接口实现 |

## Project Structure

### Documentation (this feature)

```text
specs/001-provider-adapter/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (接口契约)
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
gateway-core/src/main/java/com/codingas/gateway/
├── adapter/                         # 适配器层 (基础设施层)
│   ├── LLMProviderAdapter.java     # 适配器接口
│   ├── OpenAIAdapter.java           # OpenAI 实现
│   ├── AnthropicAdapter.java        # Anthropic 实现
│   └── spi/                         # SPI 加载机制
│       └── AdapterLoader.java
├── domain/                          # 模型
│   ├── entity/
│   │   ├── Provider.java            # 模型提供商
│   │   ├── Model.java               # 具体模型
│   │   ├── ProviderApiKey.java      # Provider 调用凭证
│   │   ├── GatewayApiKey.java        # 网关访问凭证
│   │   ├── RouteGroup.java          # 路由分组
│   │   └── RouteGroupProvider.java   # 路由分组与Provider关联
│   └── enums/                       # 枚举
├── service/                         # 服务层
│   └── ProviderService.java        # Provider 管理服务
├── repository/                      # 数据访问层
│   ├── ProviderRepository.java
│   ├── ModelRepository.java
│   ├── ProviderApiKeyRepository.java
│   ├── GatewayApiKeyRepository.java
│   ├── RouteGroupRepository.java
│   └── RouteGroupProviderRepository.java
├── infrastructure/                  # 基础设施层
│   └── encryption/                  # 加密服务
│       └── EncryptionService.java
└── exception/                      # 异常
    └── ProviderException.java

gateway-core/src/main/resources/
├── META-INF/services/               # SPI 配置
│   └── com.codingas.gateway.adapter.LLMProviderAdapter
└── db/migration/                    # Flyway 迁移脚本

gateway-adapter/src/main/java/com/codingas/gateway/adapter/
└── spi/                            # 适配器实现

gateway-web/src/main/java/com/codingas/gateway/web/
└── controller/                      # REST API 控制器
```

**Structure Decision**: 多模块 Maven 项目（gateway-core/gateway-adapter/gateway-web），符合分层架构。

## Phase 1: Data Model

详细实体设计见 `data-model.md`

**Key Changes** (单租户 + 双 API Key 设计):
- 移除 `Team`、`Channel`、`ChannelKey` 实体
- 新增 `RouteGroup`（路由分组）和 `RouteGroupProvider`（路由关联）
- 分离 `ProviderApiKey`（Provider 调用凭证）和 `GatewayApiKey`（网关访问凭证）
- Provider 为全局共享，GatewayApiKey 属于用户维度

## Phase 1: Interface Contracts

见 `contracts/` 目录

## Phase 1: Quickstart

见 `quickstart.md`

---

*Plan created by `/speckit.plan`. Next: `/speckit.tasks` to generate task list.*