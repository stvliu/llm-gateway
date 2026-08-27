# CLAUDE.md

该文件为 Claude Code (claude.ai/code) 在处理此存储库中的代码时提供指导。


## 项目概述

LLM-Gateway 是新一代企业级 AI 模型 API 聚合分发与智能路由网关（APIPark 竞品），支持 OpenAI 和 Anthropic 双 API 标准。

- **技术栈**: Java 21 + Spring Boot 3.5.x + PostgreSQL + Redis
- **架构**: 域模块化（17 模块三明治：HTTP 承载 → 域核心服务 → 持久化绑定 → 自动装配），分层依赖由 ArchUnit 铁律强制执行
- **设计规模**: ~100团队 / ~10000用户 / 单实例 10,000 QPS

## 核心原则（不可妥协）

1. **双 API 兼容**: 必须同时支持 OpenAI (`/v1/chat/completions`) 和 Anthropic (`/v1/messages`) 两种标准端点
2. **安全零信任**: 所有请求必须经过认证、鉴权、限流、脱敏四层检查；API Key 必须加密存储
3. **测试驱动开发**: 核心服务层覆盖率 ≥90%，规则引擎 ≥85%，适配器层 ≥80%
4. **可观测性内建**: 所有请求必须支持 OpenTelemetry 标准追踪，带 Trace ID 全链路追踪
5. **Token 成本透明**: 每个请求分别统计输入/输出 Token，二级预算控制（用户/令牌）

## 架构约束

**分层依赖规则**：上层依赖下层接口、禁止反向依赖——由 **Maven 模块边界 + ArchUnit 铁律**（`LayerDependencyTest`）强制执行，包名不再保留分层约定。详见 `docs/adr/0001-modularization-architecture.md`。

- **分层依赖**: 上层依赖下层接口，禁止跨层调用或反向依赖
- **Gateway 模式**: 接口定义在域核心模块（如 `provider.service.CredentialEncryptor`），实现 in 绑定模块（如 `<域>data.gateway`）或域内实现包
- **依赖倒置**: 业务域只依赖 Gateway 接口，不直接依赖外部资源
- **职责拆分架构**: 按业务域内聚 Entity + Service
- **模型纯洁性**: JPA 实体只含 Getter/Setter，禁止含业务逻辑
- **配置外部化**: 所有可变参数通过 `@ConfigurationProperties`，禁止魔法数字
- **全实体可审计**: 每张业务表必须包含 `created_by/created_at/updated_by/updated_at`

## 项目结构（多模块 Maven 项目，域模块化三明治结构：模块 = 根包）

```
llm-gateway/                          # 项目根目录（父 POM，统一依赖管理）
├── pom.xml                           # 父 POM，打包类型: pom
├── gateway-common/                   # 横切底座（common.data/entity/dto/enums/event/exception/util）
├── gateway-protocol/                 # 协议核心（contract/transport/tuning/validation）
│   ├── protocol-openai/              # OpenAI 协议实现（插件）
│   ├── protocol-anthropic/           # Anthropic 协议实现（插件）
│   └── protocol-gemini/              # Gemini 协议实现（插件）
├── gateway-provider/                 # 供给域（channel/service/catalog/model/vendor/health）
│   ├── provider-data/                # 供给持久化（dataobject/gateway/repository）
│   └── provider-starter/             # 供给自动装配（autoconfigure.provider）
├── gateway-iam/                      # 身份访问域（auth/apikey/encryption/service/dto）
│   ├── iam-data/
│   └── iam-starter/
├── gateway-usage/                    # 用量管控域（tokenlimit 等）
│   ├── usage-data/
│   └── usage-starter/
├── gateway-security/                 # 安全威胁域（threat/dataprotection）
│   ├── security-data/
│   └── security-starter/
├── gateway-audit/                    # 审计追溯域（event 等）
│   ├── audit-data/
│   └── audit-starter/
├── gateway-alert/                    # 告警通知域
│   ├── alert-data/
│   └── alert-starter/
├── gateway-resilience/               # 韧性域（retry/circuitbreaker/failover/upstream）
│   ├── resilience-data/
│   └── resilience-starter/
├── gateway-proxy/                    # 模型代理域（chat/invoker/routing/experience/conversion）
│   └── proxy-starter/
├── gateway-stats/                    # 聚合统计域
│   └── stats-starter/
├── gateway-web/                      # HTTP 承载层（web.api 全部 Controller + web.interceptor + web.advice）
├── gateway-boot/                     # 启动装配（boot.config/init/event + GatewayApplication）
├── gateway-cli/                      # CLI 管理工具
├── gateway-simulator/                # 提供商模拟器
├── gateway-coverage/                 # 覆盖率聚合（jacoco report-aggregate）
└── gateway-console/                  # Web 管理界面（React，Vite）
```

| 模块 | 职责 |
|------|------|
| gateway-boot | 启动装配（config/init/event + GatewayApplication），依赖各域 starter |
| gateway-web | HTTP 承载层（web.api 全部 Controller + interceptor + advice） |
| gateway-common | 横切底座（被所有模块依赖） |
| gateway-<域>（provider/iam/usage/security/audit/alert/resilience/proxy/stats/protocol） | 业务域核心（根包 = 模块名） |
| gateway-<域>-data | 绑定持久化模块（dataobject/gateway/repository） |
| gateway-<域>-starter | 自动装配（autoconfigure.<域>） |
| gateway-console | Web 管理界面（React） | API 消费者（HTTP 调用） |
| gateway-cli | 命令行管理工具 | API 消费者（HTTP 调用） |

## 各层职责（模块化落位）

> 分层依赖规则由模块边界 + ArchUnit 铁律强制执行（见 `docs/adr/0001-modularization-architecture.md`）；包名不再保留 `adapter/application/domain/infrastructure` DDD 层名前缀。

| 层（概念） | 职责 | 模块化落位 |
|---|------|---------|
| **web（原 adapter）** | 接收请求、返回响应 | gateway-web（web.api / web.interceptor / web.advice） |
| **application（用例编排）** | 用例编排，跨域协调 | 域核心模块 `<域>.service` 等 |
| **domain（域核心）** | 业务逻辑、模型 | 域核心模块（provider/iam/proxy/protocol...） |
| **infrastructure（持久化实现）** | Gateway 实现、数据持久化 | 绑定模块 `<域>data`（gateway/repository） |
| **common（横切）** | 跨域共享 | gateway-common（data/entity/dto/enums/event/exception/util） |

## 服务分类

| 类型 | 放置位置 | 示例 |
|------|---------|------|
| 管理服务（Service） | 域核心模块 `<域>.service/`（按用例分包） | ChannelService, TokenLimitService, AuthenticationService |

## Exception 分类

| 类型 | 放置位置 | 示例 |
|------|---------|------|
| 基础异常 | gateway-common `common/exception/` | GatewayException |
| 域异常 | 域模块 `<域>.exception/` | AuthenticationException |
| 基础设施异常 | 域模块 `<域>.exception/`（或绑定模块） | ProviderException |

## 关键文件

- `docs/constitution.md` - 架构章程（设计铁律）
- `docs/spec.md` - 完整需求规格说明书
- `docs/api-spec.md` - API 规格文档
- `docs/adr/0001-modularization-architecture.md` - 模块化后分层与命名规范决策（ADR-0001）

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
- 管理服务类: PascalCase + `Service` 后缀（业务门面）；web 组装层 `XxxFacade`（组装对象 + 跨域访问）
- 接口: PascalCase + 能力描述（如 `ModelRouter`, `TokenCounter`）
- 方法: camelCase + 动词开头
- 常量: UPPER_SNAKE_CASE
- **包名**: 域模块化（模块 = 根包，去 `domain/application/infrastructure` DDD 前缀；绑定模块拼接 `Xdata`/`Xhttp` 根包；starter 用 `autoconfigure.<域>`）。详见 `docs/constitution.md` §3.1 包名规范

## Active Technologies
- Java 21 + Spring Boot 3.5.x, Spring MVC (Web), JPA (数据持久化) 
- H2（开发调试）/ PostgreSQL 14+（生产）
- RestClient, OkHttp 4.12.0, Jackson

## 项目语言规范
请严格遵守以下规则：
1. 所有对话、解释、建议必须使用**简体中文**。
2. 代码注释必须使用中文。
3. 生成的 Commit Message 必须使用中文。
4. 严禁出现大段未翻译的英文技术名词（保留专业术语如 API、SDK 除外）。