# CLAUDE.md

该文件为 Claude Code (claude.ai/code) 在处理此存储库中的代码时提供指导。

## 项目概述

LLM-Gateway 是新一代企业级 AI 模型 API 聚合分发与智能路由网关（APIPark 竞品），支持 OpenAI 和 Anthropic 双 API 标准。

- **技术栈**: Java 21 + Spring Boot 3.5.x + PostgreSQL + Redis
- **架构**: 分层架构（Web展现层 → 应用层 → 调度层 → 服务层 → 基础设施层）
- **设计规模**: ~100团队 / ~10000用户 / 单实例 10,000 QPS

## 核心原则（不可妥协）

1. **双 API 兼容**: 必须同时支持 OpenAI (`/v1/chat/completions`) 和 Anthropic (`/v1/messages`) 两种标准端点
2. **安全零信任**: 所有请求必须经过认证、鉴权、限流、脱敏四层检查；API Key 必须加密存储
3. **测试驱动开发**: 核心服务层覆盖率 ≥90%，规则引擎 ≥85%，适配器层 ≥80%
4. **可观测性内建**: 所有请求必须支持 OpenTelemetry 标准追踪，带 Trace ID 全链路追踪
5. **Token 成本透明**: 每个请求分别统计输入/输出 Token，四级预算控制（团队/用户/令牌/用户×渠道）

## 架构约束

- **分层依赖**: 上层依赖下层接口，禁止跨层调用或反向依赖
- **职责拆分架构**: 按业务领域内聚 Entity + Service，跨域协作通过应用服务层编排
  - **Entity 归属**: Entity 属于其服务的业务领域（security/router/analytics/adapter）
  - **跨域访问**: 只通过 Service 接口，禁止领域服务直接互相调用
  - **旁路事件**: 统计、审计等旁路操作用领域事件异步处理
- **领域模型纯洁性**: JPA 实体只含 Getter/Setter，禁止含业务逻辑
- **配置外部化**: 所有可变参数通过 `@ConfigurationProperties`，禁止魔法数字
- **物理标识与业务标识分离**: 数据库用自增 BIGINT 主键，业务层用 `*_code` VARCHAR
- **全实体可审计**: 每张业务表必须包含 `created_by/created_at/updated_by/updated_at`

## 模块职责

| 模块 | 职责 |
|------|------|
| **gateway-api** | Controller + 业务 DTO |
| **gateway-app-service** | 用例编排，依赖各领域服务接口 |
| **gateway-security** | Entity + 领域服务（认证、IP封锁） |
| **gateway-router** | Entity + 领域服务（路由选择） |
| **gateway-analytics** | Entity + 领域服务（Token统计、审计） |
| **gateway-adapter** | Entity + 领域服务（LLM 适配器） |
| **gateway-infrastructure** | BaseEntity、通用工具 |
| **gateway-common** | 纯共享类型（无业务语义） |

## 关键文件

- `doc/constitution.md` - 架构章程（设计铁律）
- `doc/spec.md` - 完整需求规格说明书
- `doc/AI-Gateway功能特性.md` - 产品功能文档

## 开发命令

```bash
# 尚未确定具体构建命令，项目仍在规划阶段
```

## 数据库规范

- 表名: snake_case + 复数（如 `model_providers`, `routing_strategies`）
- 主键: `id BIGINT AUTO_INCREMENT`
- 业务标识: `*_code VARCHAR(64/128)` + UNIQUE 约束
- 外键关联: 使用物理 ID（`*_id BIGINT`）
- 审计字段: `created_by/updated_by` 使用 BIGINT FK → users.id

## 异常分层

```
GatewayException (根异常)
├── GatewayRequestException (请求级)
├── ProviderException (提供商级)
└── SecurityException (安全级)
```

## 代码规范

- 实体类: PascalCase + 明确业务含义
- 服务类: PascalCase + `Service` 后缀
- 接口: PascalCase + 能力描述（如 `ModelRouter`, `TokenCounter`）
- 方法: camelCase + 动词开头
- 常量: UPPER_SNAKE_CASE

## Active Technologies
- Java 21 + Spring Boot 3.5.x, Spring MVC (Web), JPA (数据持久化) (001-provider-adapter)
- H2（开发调试）/ PostgreSQL 14+（生产） (001-provider-adapter)
- Java 21 + Spring Boot 3.5.x, WebClient (spring-boot-starter-webflux), Reactor (Project Reactor), Jackson (002-openai-anthropic-adapters)
- PostgreSQL 14+ (provider credentials, encrypted API keys) (002-openai-anthropic-adapters)
- Java 21 + Spring Boot 3.5.x, Spring MVC (spring-boot-starter-web), RestClient, OkHttp 4.12.0, Jackson (002-openai-anthropic-adapters)

## Recent Changes
- 001-provider-adapter: Added Java 21 + Spring Boot 3.5.x, Spring MVC (Web), JPA (数据持久化)
