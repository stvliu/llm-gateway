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
5. **Token 成本透明**: 每个请求分别统计输入/输出 Token，二级预算控制（用户/令牌）

## 架构约束

**COLA Light 5.0 架构**：单模块架构，用 package 代替模块划分层次。

- **分层依赖**: 上层依赖下层接口，禁止跨层调用或反向依赖
- **Gateway 模式**: 接口定义在 domain/xxx/gateway/，实现 in infrastructure/xxx/gateway/
- **依赖倒置**: Domain 只依赖 Gateway 接口，不直接依赖外部资源
- **职责拆分架构**: 按业务领域内聚 Entity + Domain Service + Gateway
- **领域模型纯洁性**: JPA 实体只含 Getter/Setter，禁止含业务逻辑
- **配置外部化**: 所有可变参数通过 `@ConfigurationProperties`，禁止魔法数字
- **全实体可审计**: 每张业务表必须包含 `created_by/created_at/updated_by/updated_at`

## 项目结构（多模块 Maven 项目）

```
gateway/                              # 项目根目录（父 POM）
├── pom.xml                           # 父 POM，打包类型: pom
├── gateway-boot/                     # 后端模块（所有层）
│   ├── pom.xml
│   └── src/main/java/com/codingas/gateway/
│       ├── adapter/                  # 适配器层（按用例分包）
│       │   ├── api/                  # 所有 API Controller
│       │   ├── interceptor/          # 拦截器
│       │   └── protocol/            # 协议适配层
│       │       ├── openai/          # OpenAI 协议校验器/调谐器
│       │       └── anthropic/       # Anthropic 协议校验器/调谐器
│       ├── application/              # 应用层（按用例分包）
│       │   └── proxy/               # 代理调度
│       │       ├── routing/          # 路由解析（RoutingResolver/ModelMatcher/ChannelSelector/CredentialResolver/EndpointResolver）
│       │       └── ChatDispatchService  # 七阶段调度
│       ├── domain/                   # 领域层
│       │   ├── gateway/              # 跨领域 Gateway 接口
│       │   ├── protocol/             # 协议领域
│       │   │   ├── contract/         # 协议数据契约（DTO）
│       │   │   ├── conversion/       # 跨协议转换规则
│       │   │   ├── tuning/           # 出站调谐接口
│       │   │   └── validation/       # 入站校验接口
│       │   ├── supply/               # 供给域
│       │   ├── proxy/                # 模型代理领域
│       │   ├── model/                # 模型广场领域
│       │   ├── security/             # 访问控制领域
│       │   ├── quota/                # 用量管控领域
│       │   ├── audit/                # 审计追溯领域
│       │   └── alert/                # 告警通知领域
│       ├── infrastructure/           # 基础设施层
│       │   ├── config/
│       │   ├── gateway/              # Gateway 实现
│       │   ├── resilience/           # 韧性组件（Retry/CircuitBreaker/ResilientUpstreamClient）
│       │   ├── security/
│       │   └── util/
│       └── common/                   # 公共组件
├── gateway-console                   # React/Vue 前端代码
└── gateway-cli/                      # CLI 模块（骨架）
    ├── pom.xml
    └── src/main/java/com/codingas/gateway/cli/
```

| 模块 | 职责 | 与 gateway-boot 关系 |
|------|------|---------------------|
| gateway-boot | 后端模块，包含所有层 | - |
| gateway-console | Web 管理界面 | API 消费者（HTTP 调用） |
| gateway-cli | 命令行管理工具 | API 消费者（HTTP 调用） |

## 各层职责

| 层 | 职责 | 包含内容 |
|---|------|---------|
| **adapter** | 接收请求、返回响应 | Controller、DTO（按用例分包） |
| **application** | 用例编排，跨域协调 | Application Service（按用例分包） |
| **domain** | 业务逻辑、领域模型 | Entity、Domain Service、Gateway 接口、异常、枚举 |
| **infrastructure** | 技术实现 | Gateway 实现、配置、工具 |
| **common** | 跨领域共享 | 基础异常、技术常量、工具类 |

## 服务分类

| 类型 | 放置位置 | 示例 |
|------|---------|------|
| Domain Service | domain/xxx/service/ | AuthenticationService, RateLimitService |
| Application Service | application/xxx/ | AuthApplication, ChatApplication |

## Exception 分类

| 类型 | 放置位置 | 示例 |
|------|---------|------|
| 基础异常 | common/exception/ | GatewayException |
| 领域异常 | domain/xxx/exception/ | AuthenticationException |
| 基础设施异常 | infrastructure/exception/ | ProviderException |

## 关键文件

- `doc/constitution.md` - 架构章程（设计铁律）
- `doc/spec.md` - 完整需求规格说明书
- `doc/AI-Gateway功能特性.md` - 产品功能文档

## 开发命令

```bash
# 构建所有模块
./mvnw clean install

# 构建并跳过测试
./mvnw clean install -DskipTests

# 只构建 gateway-boot 模块
./mvnw clean install -pl gateway-boot

# 运行 gateway-boot
./mvnw spring-boot:run -pl gateway-boot

# 运行 gateway-cli
./mvnw spring-boot:run -pl gateway-cli
```

## 数据库规范

- 表名: snake_case + 复数（如 `model_providers`, `routing_strategies`）
- 主键: `id BIGINT AUTO_INCREMENT`
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

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
On every task, check for relevant Superpowers skills and use them.