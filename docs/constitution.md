# AI 大模型网关项目章程

## 元信息

| 属性 | 值 |
|------|------|
| 规范名称 | AI Gateway Constitution |
| 版本 | 2.8.0 |
| 状态 | 草案 |
| 创建日期 | 2026-04-08 |
| 技术栈 | Java 21 + Spring Boot 3.5.x + PostgreSQL 14+ + Redis 6.0+ |

---

## 概述

系统宪法定义了不可违背的设计铁律与架构约束。所有设计与实现必须严格遵循以下原则，任何偏离均需经过架构评审委员会审批。

---

## 1. 根本原则

### 第一原则：双 API 兼容（不可妥协）

**定义**:
```
网关必须同时向上暴露 OpenAI API v1 和 Anthropic Messages API 两种标准端点。
无论下游模型提供商使用什么格式（Anthropic 兼容端点或原生 API），
上游客户端看到的必须是标准的 OpenAI 或 Anthropic 接口。
```

**推论**:
- ✅ OpenAI 格式：`/v1/chat/completions` 端点 100% 兼容 OpenAI 标准
- ✅ Anthropic 格式：`/v1/messages` 端点 100% 兼容 Anthropic Messages API
- ✅ 使用 `provider/model` 命名约定（如 `openai/gpt-4o`、`zhipu/glm-4.6`）
- ✅ 参数转换准确率 ≥99.9%
- ✅ Tool Use / Function Calling 双向映射完整支持
- ✅ 下游为 Anthropic 兼容端点（智谱/火山/阿里）时使用直通转发（零转换延迟）
- ✅ 下游为原生 API 时自动进行格式转换，对上游透明
- ✅ 新增模型接入时间 ≤2 小时
- ❌ 禁止：引入不兼容的 API 格式
- ❌ 禁止：破坏已有客户端的调用方式
- ❌ 禁止：两种 API 格式的行为不一致
- ❌ 禁止：上游客户端感知下游格式差异

**验证规则**:
- OpenAI 兼容性自动化测试套件必须 100% 通过
- Anthropic 兼容性自动化测试套件必须 100% 通过
- Claude Code E2E 测试必须通过（含 Tool Use 场景）
- 直通转发延迟增加 ≤5ms（P95）
- 每次 API 变更必须进行向后兼容性检查

### 第二原则：安全零信任（不可妥协）

**定义**:
```
所有请求默认不可信，必须经过认证、鉴权、限流、脱敏四层安全检查。
API 密钥等敏感信息必须加密存储，禁止明文或硬编码。
```

**推论**:
- ✅ 传输中数据使用 TLS 1.3+ 加密
- ✅ PII（个人身份信息）自动检测并脱敏
- ✅ API 密钥使用 AES-256 或国密算法加密存储
- ✅ 支持 RBAC 和 ABAC 双重访问控制
- ✅ 每个关键操作记录审计日志
- ❌ 禁止：硬编码 API Key 到代码中
- ❌ 禁止：在日志中明文打印敏感信息
- ❌ 禁止：将 API Key 提交到 Git 仓库

**验证规则**:
- 代码扫描工具不得发现硬编码密钥
- 所有 API 必须有认证拦截
- 审计日志覆盖所有关键操作

### 第三原则：测试驱动开发（不可妥协）

**定义**:
```
所有功能必须遵循 TDD 方法论：编写测试 → 测试失败 → 然后实现。
核心服务层覆盖率 ≥90%，规则引擎 ≥85%，适配器层 ≥80%。
```

**推论**:
- ✅ 单元测试覆盖率：核心服务层 ≥90%
- ✅ 集成测试覆盖所有外部 API 适配器
- ✅ 性能测试验证延迟（P95≤10ms）和吞吐量（≥10,000 QPS）
- ❌ 禁止：无测试代码提交
- ❌ 禁止：跳过集成测试的适配器上线

**验证规则**:
- CI 流水线必须通过所有测试
- 覆盖率低于阈值时构建失败

### 第四原则：可观测性内建

**定义**:
```
所有请求必须支持 OpenTelemetry 标准追踪，全链路可观测。
禁止静默执行或批量完成后才报告结果。
```

**推论**:
- ✅ 所有请求带有 Trace ID，贯穿网关入口→认证→路由→模型调用→响应
- ✅ 结构化 JSON 日志，包含 `[TRACE_ID]`, `[REQUEST_ID]`, `[MODEL]` 等元数据
- ✅ 实时指标采集：延迟（P50/P95/P99）、成功率、QPS、Token 消耗
- ❌ 禁止：仅使用 `System.out.println`
- ❌ 禁止：吞掉异常堆栈

**验证规则**:
- 每个请求必须生成 Trace
- 日志必须包含结构化元数据

### 第五原则：成本 Token 透明化

**定义**:
```
Token 必须是所有成本追踪的核心单位。
每个请求必须分别追踪输入/输出 Token，Token 限额控制必须强制执行。
```

**推论**:
- ✅ 每个请求分别统计输入/输出 Token
- ✅ 用户/模型 Token 限额控制
- ✅ 超限后执行预设策略（拒绝/降级/切换）
- ❌ 禁止：遗漏 Token 统计
- ❌ 禁止：超限后静默放行

**验证规则**:
- Token 计量准确率 ≥99.9%
- 超限后必须触发对应策略

---

## 2. 架构约束

### 2.1 分层依赖规则

**定义**:
```
系统采用域模块化架构：多模块 Maven 三明治结构，按功能域划分模块（模块 = 根包）。
分层规则：HTTP 承载归 gateway-web（api/interceptor/advice），启动装配归 gateway-boot（config/init/event）；
Domain 接口与 Infrastructure 实现下沉至各功能域模块。
业务域以「核心模块（领域逻辑 + Gateway 接口 + 应用服务）+ <域>data 绑定模块（JPA 持久化实现）+ <域>starter（自动装配）」三明治结构组织，
实现依赖倒置；跨域协作通过应用服务层编排，旁路操作通过领域事件解耦。
```

**项目结构（17 模块多模块 Maven，父 POM artifactId = gateway-project）**:
```
gateway-project/                       # 父 POM（打包类型: pom）
├── gateway-common/                    # 横切基础模块（根包 com.codingas.gateway.common，groupId 统一 com.codingas.gateway）
│   └── BaseEntity / 异常 / 工具 / 事件 / 通用枚举，纯横切
├── gateway-protocol/                  # 协议域（抽象层 + 插件化实现）
│   ├── protocol/                      #   协议核心（根包 com.codingas.gateway.protocol，groupId 统一 com.codingas.gateway）
│   │   ├── canonical/                 #     Canonical IR
│   │   ├── contract/                  #     协议数据契约（DTO）
│   │   ├── transport/                 #     上游传输（UpstreamClient 接口 / 错误分类 / SSE 格式化）
│   │   ├── tuning/                    #     出站调谐接口
│   │   └── validation/                #     入站校验接口
│   ├── protocol-openai/               #   OpenAI 协议插件（AutoConfiguration + @ConditionalOnProperty 启用）
│   ├── protocol-anthropic/            #   Anthropic 协议插件
│   └── protocol-gemini/               #   Gemini 协议插件（示例，验证插件化可扩展性）
├── gateway-provider/                  # 供给域（根包 com.codingas.gateway.provider，groupId 统一 com.codingas.gateway）
│   ├── provider/                      #   核心：Provider/Channel/Model/Catalog/Upstream/RoutingContext
│   └── provider-data/                 #   JPA 绑定（providerdata）
├── gateway-iam/                       # 身份与访问域（根包 com.codingas.gateway.iam，groupId 统一 com.codingas.gateway）
│   ├── iam/                           #   核心：User/Application/UserApiKey/Auth/加密
│   └── iam-data/                      #   JPA 绑定（iamdata）
├── gateway-usage/                     # 用量管控域（根包 com.codingas.gateway.usage，groupId 统一 com.codingas.gateway）
│   ├── usage/                         #   核心：TokenLimit/配额/用量事件/限流执行
│   └── usage-data/                    #   JPA 绑定（usagedata）
├── gateway-security/                  # 安全与威胁域（根包 com.codingas.gateway.security，groupId 统一 com.codingas.gateway）
│   ├── security/                      #   核心：IP 威胁检测 + 数据脱敏（SensitiveDataRule/IpBlock）
│   └── security-data/                 #   JPA 绑定（securitydata）
├── gateway-audit/                     # 审计追溯域（根包 com.codingas.gateway.audit，groupId 统一 com.codingas.gateway）
│   ├── audit/                         #   核心：调用日志（CallLogs）/ 审计事件
│   └── audit-data/                    #   JPA 绑定（auditdata）
├── gateway-alert/                     # 告警通知域（根包 com.codingas.gateway.alert，groupId 统一 com.codingas.gateway）
│   ├── alert/                         #   核心：告警通知
│   └── alert-data/                    #   JPA 绑定（alertdata）
├── gateway-resilience/                # 韧性域（根包 com.codingas.gateway.resilience，groupId 统一 com.codingas.gateway）
│   ├── resilience/                    #   核心：failover/retry/circuit-breaker
│   └── resilience-data/               #   JPA 绑定（resiliencedata）
├── gateway-proxy/                     # 模型代理域（根包 com.codingas.gateway.proxy，groupId 统一 com.codingas.gateway）
│   └── proxy/                         #   ChatDispatch 调度 / routing / invoker / 协议转换门面
├── gateway-stats/                     # 聚合统计域（根包 com.codingas.gateway.stats，groupId 统一 com.codingas.gateway）
│   └── stats/                         #   仪表盘聚合统计（读路径）
├── gateway-web/                       # HTTP 承载层（根包 com.codingas.gateway.web，groupId 统一 com.codingas.gateway）
│   └── web/                           #   api 全部 Controller + interceptor + advice（协议校验适配、安全拦截链、全局异常）
├── gateway-boot/                      # 启动装配（根包 com.codingas.gateway.boot，groupId 统一 com.codingas.gateway）
│   └── boot/                          #   config / init / event + GatewayApplication（装配各域模块）
├── gateway-cli/                       # CLI 管理工具（API 消费者）
├── gateway-simulator/                 # LLM 提供商模拟服务（本地开发 / 集成测试）
└── gateway-coverage/                  # 覆盖率聚合（全模块 jacoco 汇总 / 质量门槛校验）
```

> gateway-console 为 Web 管理界面前端（React/Vue，vite），非 Maven 模块，属 API 消费者。

**各层职责**:

| 层 | 职责 | 包含内容 |
|---|------|---------|
| **web** | 接收请求、返回响应 | Controller、拦截器、全局异常（gateway-web，按用例分包） |
| **application** | 用例编排，跨域协调 | Application Service（各功能域核心模块的 service 包，按用例分包） |
| **domain** | 业务逻辑、领域模型 | Entity、Domain Service、Gateway 接口、异常、枚举（位于各功能域核心模块） |
| **infrastructure** | 技术实现 | Gateway 实现、配置、工具（位于 `<域>data` 绑定模块） |
| **common** | 跨领域共享 | 基础异常、技术常量、工具类（gateway-common） |

> 业务域代码（domain/infrastructure）不再集中在 gateway-boot，而是下沉至各功能域模块（domain 接口在核心模块、infrastructure 实现在 `<域>data` 绑定模块）；HTTP 承载归 gateway-web，gateway-boot 只保留启动装配（config/init/event）。

### 2.2 Gateway 模式

**定义**:
```
Gateway 接口定义在功能域核心模块中（根包 = groupId + 子域，如 com.codingas.gateway.iam）。
Gateway 实现在对应的 <域>data 绑定模块中（根包 = groupId + 子域 + data，如 com.codingas.gateway.iamdata）。
Domain 只依赖 Gateway 接口，不直接依赖外部资源。
绑定模块通过依赖注入实现 Gateway，负责 DO ↔ Entity 转换。
```

```
功能域核心模块（领域层）              <域>data 绑定模块（基础设施层）
   │                                │
   │   ┌─────────────────┐          │
   │   │ XxxGateway     │          │
   │   │ (接口定义)      │          │
   │   └────────┬────────┘          │
   │            │                     │
   │            │ 实现                │
   └────────────┼────────────────────┘
                │
         ┌──────┴──────┐
         │ JpaXxx       │
         │ Gateway      │
         └─────────────┘
```

**Gateway 接口与实现放置规则**:

| 组件 | 放置位置 |
|------|---------|
| Gateway 接口 | 功能域核心模块（如 `com.codingas.gateway.audit.AuditGateway`） |
| Gateway 实现 | `<域>data` 绑定模块 gateway 包（如 `com.codingas.gateway.auditdata.gateway.AuditGatewayImpl`） |

### 2.3 服务分类

**Domain Service（领域服务）**：
- 职责：业务逻辑，领域规则
- 放置：功能域核心模块（如 `com.codingas.gateway.iam.auth`、`com.codingas.gateway.iam.service`）
- 命名：能力名 + `DomainService` 后缀
- 示例：`AuthenticationDomainService`, `RateLimitDomainService`, `ApiKeyEncryptionDomainService`

**Application Service（应用服务）**：
- 职责：用例编排，跨领域协调，不含业务逻辑
- 放置：各功能域核心模块的 service 包（如 `com.codingas.gateway.provider.service`、`com.codingas.gateway.iam.service`，按用例分包）
- 命名：能力名 + `Service` 后缀
- 示例：`ChannelService`, `ProviderService`, `ModelService`, `PlanCatalogService`

**命名区分原则**：
| 层 | 命名模式 | 语义 |
|---|---------|------|
| Domain | `XxxDomainService` | 领域能力，表示"能做什么" |
| Application | `XxxService` | 应用服务，表示"执行什么操作" |

**示例对照**：
```
gateway-iam · com.codingas.gateway.iam.auth.AuthenticationDomainService     # 领域认证能力
gateway-iam · com.codingas.gateway.iam.service.ApiKeyEncryptionDomainService # 领域加密能力
gateway-provider · com.codingas.gateway.provider.service.ChannelService     # 应用渠道服务
gateway-provider · com.codingas.gateway.provider.service.ModelService       # 应用模型服务
```

### 2.4 Exception 分类

| 类型 | 放置位置 | 示例 |
|------|---------|------|
| 基础异常 | gateway-common · `com.codingas.gateway.common.exception` | GatewayException |
| 领域异常 | 功能域核心模块 exception 包（如 `com.codingas.gateway.iam.exception`） | IamException |
| 基础设施异常 | 功能域核心模块（如 `com.codingas.gateway.provider.vendor`） | ProviderException |

### 2.5 跨域访问规则

| 方式 | 场景 | 规则 |
|------|------|------|
| Gateway 接口 | Domain 访问外部资源 | ✅ 定义在功能域核心模块，实现在 `<域>data` 绑定模块 |
| 应用服务编排 | 主流程（认证→路由→调用） | ✅ 功能域核心模块 service 包的应用服务调用 Domain Service |
| 领域事件 | 旁路（统计、审计） | ✅ 异步解耦 |
| Domain 直接调用其他 Domain | 主流程中 | ❌ 禁止 |
| Domain 直接访问外部资源 | 持久化、外部 API | ❌ 必须通过 Gateway |

**违规示例**:
- ❌ 应用服务直接调用绑定模块的 `XxxRepository` 获取 Entity
- ✅ 应用服务 → 功能域 Domain Service → `XxxGateway` → `<域>data` 中的 `JpaXxxGateway`
- ❌ `DomainService` 直接使用 `EntityManager` 持久化
- ✅ `DomainService` → `XxxGateway` → `<域>data` 中的 `JpaXxxGateway`
- ❌ iam 域 Domain 直接调用 proxy 域 Domain 的服务
- ✅ 功能域核心模块 service 包的应用服务编排两者的调用

### 2.6 大模型调用链路

**定义**:
```
所有 LLM 模型调用请求必须经过完整的七阶段处理链路。
每个阶段的职责、归属层和依赖方向不可变更。
```

**完整调用链路**:

```
请求入口 (gateway-web · web/api)
  │    OpenAIController / AnthropicController
  ▼
安全拦截链 (gateway-web · web/interceptor)
  │    ApiKeyAuth → RateLimit → IPBlockCheck → TokenAuth ...
  ▼
ChatDispatchService (gateway-proxy · com.codingas.gateway.proxy.chat)  ← 统一编排入口
  │
  │  ┌─ 前置阶段 ────────────────────────────────────────┐
  │  │  1. 校验：ProtocolValidator.validate(request)     │
  │  │     位置：gateway-protocol（校验 SPI）            │
  │  │     校验适配：gateway-protocol 插件（protocol-openai/anthropic） │
  │  │  2. 路由：RoutingResolver.resolve(identity, model)│
  │  │     位置：gateway-proxy · routing                 │
  │  │     ModelMatcher → CredentialResolver             │
  │  │       → EndpointResolver                          │
  │  │  3. 记录审计起点：AuditGateway.logRequest(...)    │
  │  │     位置：gateway-audit（实现：audit-data）        │
  │  └──────────────────────────────────────────────────┘
  │
  │  ┌─ 转换阶段（仅跨协议时执行）───────────────────────┐
  │  │  4. 请求转换：ProtocolConversionFacade.convert()  │
  │  │     位置：gateway-proxy · conversion（编排门面）   │
  │  │     契约与 SPI：gateway-protocol                  │
  │  └──────────────────────────────────────────────────┘
  │
  │  ┌─ 调谐阶段 ────────────────────────────────────────┐
  │  │  5. 调谐：OutboundTuner.tune(request, ctx)        │
  │  │     位置：gateway-proxy · conversion              │
  │  │     调谐 SPI：gateway-protocol                    │
  │  │     职责：模型名替换、默认值填充、字段覆盖、      │
  │  │           敏感字段剥离                              │
  │  │     调谐必须按目标协议要求执行，而非入站协议       │
  │  └──────────────────────────────────────────────────┘
  │
  │  ┌─ 调用阶段 ────────────────────────────────────────┐
  │  │  6. 上游调用：UpstreamClient.chat(request)        │
  │  │     接口：gateway-protocol · protocol.transport    │
  │  │     实现：gateway-protocol 插件（protocol-openai/anthropic） │
  │  │     韧性包装：gateway-resilience                   │
  │  │     纯 HTTP 调用 + SSE 解析，不含业务逻辑         │
  │  └──────────────────────────────────────────────────┘
  │
  │  ┌─ 转换阶段（仅跨协议时执行）───────────────────────┐
  │  │  7. 响应转换：ProtocolConversionFacade.convert()  │
  │  │     位置：gateway-proxy · conversion              │
  │  └──────────────────────────────────────────────────┘
  │
  │  ┌─ 后置阶段 ────────────────────────────────────────┐
  │  │  8. Token 计量：发布 TokenUsedEvent               │
  │  │     位置：gateway-usage · usage.event             │
  │  │  9. 记录审计终点：AuditGateway.logResponse(...)   │
  │  │     包含：duration、success/failure、Token 用量    │
  │  └──────────────────────────────────────────────────┘
  │
  ▼
响应返回
```

**链路铁律**:

| 规则 | 说明 |
|------|------|
| 转换先于调谐 | 跨协议时必须先转换为目标协议格式，再按目标协议要求调谐 |
| 调谐依赖路由 | OutboundTuner 必须在 RoutingResolver 之后执行，因为调谐参数来自路由结果 |
| 调用不含业务 | UpstreamClient 只做 HTTP 调用 + SSE 解析，不含模型名替换、默认值填充等业务逻辑 |
| 韧性包装调用 | 重试策略（RetryStrategy）和 CircuitBreaker 包装在 UpstreamClient 外层，与路由选择联动 |
| 审计前后各记 | 审计日志在调用前记录起点（入站信息），调用后记录终点（响应+耗时） |
| 计量基于响应 | Token 计量从 ProtocolResponse 提取，在响应返回前发布事件 |

**各阶段归属模块**:

| 阶段 | 归属模块 | 关键类 |
|------|--------|--------|
| 校验 | gateway-protocol（SPI）+ gateway-boot（校验适配） | `ProtocolValidator`；`OpenAIProtocolValidator`, `AnthropicProtocolValidator` |
| 路由 | gateway-proxy | `RoutingResolver`, `ModelMatcher`, `CredentialResolver`, `EndpointResolver` |
| 转换 | gateway-proxy（编排）+ gateway-protocol（契约/SPI） | `ProtocolConversionFacade`, `ProtocolAdapter`, `ProtocolRequest/ProtocolResponse` |
| 调谐 | gateway-proxy（编排）+ gateway-protocol（SPI） | `OutboundTuner`, `ProtocolTuner` |
| 调用 | gateway-protocol（transport 接口 / 插件实现） | `UpstreamClient`, `UpstreamClientRegistry`, `OpenAIUpstreamClient`, `AnthropicUpstreamClient` |
| 韧性 | gateway-resilience | `RetryStrategy`/`RetryExecutor`, `CircuitBreaker`, `ChannelEndpointCircuitBreakerManager`, `ResilientUpstreamClient` |
| 计量 | gateway-usage | `ChatDispatchService` → 发布 `TokenUsedEvent` |
| 审计 | gateway-audit + audit-data | `AuditGateway.logRequest()`, `AuditGateway.logResponse()` |

### 2.7 领域模型纯洁性

**定义**:
```
所有 JPA 实体必须保持纯洁，禁止包含业务逻辑。
业务逻辑必须封装于 @Service 类中。
Domain Entity 是业务领域的实体及实体关系，与基础设施的具体实现（DB/JPA、NoSQL、缓存、第三方系统）无关。
Gateway 是领域层与基础设施层之间的防腐接口，隔离技术细节，防止第三方系统变化导致领域层腐化。
```

**Domain Entity 原则**:
- ✅ 使用对象引用表达业务领域关系（如 `User user`, `List<Role> roles`）
- ✅ 反映业务实体及其关联，不暴露任何技术实现细节
- ✅ 纯 POJO，依赖 `BaseEntity`（无 JPA 注解）

**Gateway（防腐层）原则**:
- ✅ Gateway 接口定义在功能域核心模块，实现于 `<域>data` 绑定模块
- ✅ Gateway 实现负责 **DO ↔ Entity 转换**（DO 是 JPA 实体，含 `@Entity`、`@ManyToOne` 等）
- ✅ 领域层只依赖 Gateway 接口，完全不知道 JPA、数据库、ORM 的存在
- ✅ 当第三方系统（DB、缓存、外部API）变化时，只需修改 Gateway 实现，领域层不受影响

**实体允许的内容**:
- ✅ Getter/Setter
- ✅ 对象引用（表达业务关系）
- ✅ `@PrePersist`, `@PreUpdate` 生命周期回调
- ✅ `toString()`, `equals()`, `hashCode()`

**实体禁止的内容**:
- ❌ 调用外部 API
- ❌ 复杂业务计算
- ❌ 直接修改其他实体状态
- ❌ 任何 JPA、数据库、ORM 注解或依赖

**架构示意**:
```
┌─────────────────────────────────────────────────────────────┐
│                      Application Layer                       │
│                  (用例编排，调用 Domain Service)               │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                       Domain Layer                            │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ Domain       │    │ Domain       │    │ Domain       │ │
│  │ Entity       │    │ Service      │    │ Gateway      │ │
│  │ (纯POJO)     │    │ (业务逻辑)   │    │ (接口)       │ │
│  │ User         │    │              │    │ XxxGateway   │ │
│  │ Model ──→    │    │              │    │              │ │
│  │ Provider     │    │              │    │              │ │
│  └──────────────┘    └──────────────┘    └──────┬───────┘ │
└─────────────────────────────────────────────────┼───────────┘
                                                  │ 依赖接口
┌─────────────────────────────────────────────────▼───────────┐
│                   Infrastructure Layer                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              JpaXxxGateway (Gateway 实现)              │   │
│  │         负责 DO ↔ Entity 转换                         │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐   │
│  │    DO        │    │    DO        │    │    DO        │   │
│  │ (JPA实体)    │    │ (JPA实体)    │    │ (JPA实体)    │   │
│  │ @Entity      │    │ @Entity      │    │ @Entity      │   │
│  │ @ManyToOne   │    │ @ManyToOne   │    │ @ManyToOne   │   │
│  └──────────────┘    └──────────────┘    └──────────────┘   │
└───────────────────────────────────────────────────────────────┘
```

> 分层映射到模块：Application Layer → 各功能域核心模块的 service 包；Domain Layer → 各功能域核心模块；Infrastructure Layer → `<域>data` 绑定模块（JPA 实现）。

**Entity 与 DO 关联模式**:

| 层级 | 关联方式 | 原则 |
|------|---------|------|
| **Entity 层** | **统一使用 ID 引用** | Entity 只持有 ID，不持有其他 Entity 引用 |
| **DO 层** | 按场景选择 | 主从关系可用 JPA `@ManyToOne`；中间表/弱引用使用 ID 引用 |

**Entity 层 ID 引用原则**:

- ✅ Entity 是纯数据载体，只持有关联对象的 ID
- ✅ 需要关联数据时，通过 Domain Service 或 Gateway 按需加载
- ✅ 避免隐式 N+1 查询风险
- ✅ 符合聚合根边界原则

```
// 推荐：Entity 使用 ID 引用
@Data
public class Model extends BaseEntity {
    private Long providerId;        // ID 引用
    // ...
}

// 不推荐：Entity 使用对象引用
@Data
public class Model extends BaseEntity {
    private Provider provider;      // ❌ 避免对象引用
    // ...
}
```

**DO 层关联策略**:

| 场景 | 建议方式 | 说明 |
|------|---------|------|
| 主从关系（强依赖） | 可用 JPA `@ManyToOne` | 简化查询，禁用级联删除 |
| 中间表/关联表 | ID 引用 | 避免 JPA 关联带来的复杂性 |
| 弱引用关系 | ID 引用 | 手动 JOIN 查询 |

### 2.8 配置外部化

**定义**:
```
所有可变参数必须通过 @ConfigurationProperties 外部化。
禁止在代码中出现魔法数字或硬编码字符串。
```

**配置来源优先级**:
```
命令行参数 > 环境变量 > application-local.yml > application.yml > 数据库默认值
```

**关键配置必须提供默认值**:
```yaml
gateway:
  llm:
    routing-strategy: COST_OPTIMIZED  # 默认策略
    token-threshold: 0.8              # 80% 触发切换
    max-retries: 3                    # 最大重试次数
    timeout-seconds: 30               # API 超时时间
```
### 2.9 全实体可审计（不可妥协）

**定义**:
```
系统中每一个业务实体表都必须包含完整的审计字段，记录谁在何时创建、修改和删除了数据。
审计字段是系统合规性、追溯性和安全性的基础，不可省略。
```

**推论**:
- ✅ 每张业务表必须包含：`created_by`, `created_at`, `updated_by`, `updated_at`
- ✅ 支持软删除的表还需包含：`deleted_by`, `deleted_at`
- ✅ `created_by`/`updated_by`/`deleted_by` 使用用户物理 ID（BIGINT FK → users.id）
- ✅ 系统自动生成的记录（如请求日志）`created_by` 可为 NULL 或填系统用户 ID（0L）
- ✅ 审计字段必须通过 JPA @EntityListeners 自动填充，禁止业务代码手动修改
- ❌ 禁止：任何业务表缺少审计字段
- ❌ 禁止：审计字段被业务代码手动修改

---

## 3. 技术规范

### 3.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 实体类 | PascalCase + 明确业务含义 | `GatewayConfig`, `ModelProvider` |
| 服务类 | PascalCase + `Service` 后缀 | `RoutingService`, `CostService` |
| 接口 | PascalCase + 能力描述 | `ModelRouter`, `TokenCounter` |
| 方法 | camelCase + 动词开头 | `routeRequest()`, `countTokens()` |
| 变量 | camelCase + 名词 | `tokenThreshold`, `providerId` |
| 常量 | UPPER_SNAKE_CASE | `DEFAULT_TOKEN_THRESHOLD` |
| 数据库表 | snake_case + 复数 | `model_providers`, `routing_strategies` |
| 模块（根包） | 模块 = 根包，去除 `domain/application/infrastructure` DDD 前缀；**groupId 统一 `com.codingas.gateway`**，包名 = groupId + 子域（如 `com.codingas.gateway.provider`） | `com.codingas.gateway.iam` |
| JPA 绑定模块 | `<域>data` 根包 | `iamdata`（`com.codingas.gateway.iamdata`） |
| HTTP 承载包 | gateway-web（根包 `com.codingas.gateway.web`） | `web.api.*`, `web.interceptor.*`, `web.advice.*` |
| 装配包 | gateway-boot（根包 `com.codingas.gateway.boot`） | `boot.config.*`, `boot.init.*`, `boot.event.*` |

> 协议插件模块遵循 `gateway-protocol-<协议>` 命名，根包 `com.codingas.gateway.protocol.<协议>`（如 `com.codingas.gateway.protocol.openai`）。

### 3.2 前端主题色规范

**定义**:
```
前端页面所有组件的颜色必须使用 Ant Design 主题 token（如 colorPrimary、colorBgContainer），
禁止硬编码自定义颜色值。确保全局主题一致性，支持主题切换。
```

**推论**:
- ✅ 使用 Ant Design 5.x 的 `token.useToken()` 获取主题色
- ✅ 背景色、边框色、文字色等均从 token 获取
- ✅ 代码块/代码卡片等特殊组件使用 Ant Design Card 组件默认样式
- ✅ 需要自定义色值时，通过 ConfigProvider theme 配置扩展
- ❌ 禁止：在组件中硬编码 `#1e293b`、`#1890ff` 等色值
- ❌ 禁止：使用内联 style 设置自定义颜色（背景色、边框色等）

**验证规则**:
- 代码扫描不得发现硬编码色值（`#[0-9a-fA-F]{3,8}`、`rgb()`、`hsl()`）
- 所有颜色值必须来自 Ant Design token 或 CSS 变量

### 3.3 异常处理规范

**异常分层**:
```
GatewayException (根异常)
├── GatewayRequestException (请求级异常)
│   ├── InvalidModelException
│   ├── BudgetExceededException
│   └── RateLimitExceededException
├── ProviderException (提供商级异常)
│   ├── ProviderUnavailableException
│   ├── TokenQuotaExceededException
│   └── ProviderResponseException
└── IamException (身份与访问控制异常)
    ├── UnauthorizedException
    ├── ForbiddenException
    └── AuthenticationFailedException
└── ThreatException (威胁防护异常)
    ├── RateLimitExceededException
    └── IpBlockedException
└── DataProtectionException (数据保护异常)
```

> 异常放置：根异常 `GatewayException` 位于 gateway-common；领域异常（`IamException` 位于 gateway-iam、`ThreatException`/`DataProtectionException` 位于 gateway-security 等）位于对应功能域核心模块的 exception 包；提供商异常 `ProviderException` 位于 gateway-provider。

**处理原则**:
- ✅ 所有受检异常必须转换为运行时异常
- ✅ 每个异常必须包含清晰的错误上下文（请求 ID、模型 ID、提供商）
- ✅ 必须在异常日志中包含完整的堆栈轨迹与重试历史

### 3.3 事务边界

**事务配置**:
- 隔离级别：`READ_COMMITTED`
- 传播行为：`REQUIRED`（默认）

### 3.4 并发控制

**并发安全保证**:
- ✅ JDK 21 虚拟线程提供轻量级并发,无需手动管理线程池
- ✅ `TokenQuotaTracker` 使用 `AtomicInteger` 保证线程安全
- ✅ `ProtocolAdapter` 必须是无状态的，支持多线程并发调用
- ✅ 请求记录按任务 ID 分区写入，避免行锁竞争
- ✅ LLM HTTP 客户端使用 OkHttp，虚拟线程中阻塞调用自动挂起

---

## 4. 质量属性

### 4.1 可测试性

**单元测试覆盖率要求**:
- ✅ 核心服务层：≥ 90%
- ✅ 路由引擎：≥ 85%
- ✅ 适配器层：≥ 80%

### 4.2 可观测性

**日志分级要求**:

| 级别 | 使用场景 | 示例 |
|------|----------|------|
| ERROR | 系统错误，需要人工介入 | 所有 Provider 均失败 |
| WARN | 可恢复异常，需关注 | Provider 切换、Token 接近限额 |
| INFO | 关键业务流程 | 请求处理完成、模型切换 |
| DEBUG | 详细调试信息 | 路由决策过程、API 请求/响应 |
| TRACE | 最细粒度追踪 | 每个字段的处理前后对比 |

### 4.3 可扩展性

**新增 Provider/协议步骤**:
1. 在 `gateway-protocol-*` 插件模块实现 `ProtocolAdapter` SPI（协议适配器 + 协议契约 DTO）
2. 提供 `AutoConfiguration`，通过 `@ConditionalOnProperty` 启用插件
3. 通过 Web 界面或 REST API 配置 Provider/Channel 信息
4. （可选）调整优先级顺序

**开闭原则**:
- ✅ 对扩展开放：新增协议无需修改现有代码（插件化 SPI）
- ✅ 对修改关闭：现有协议逻辑不受影响

---

## 5. 安全红线

### 5.1 API Key 保护

**规则**:
- ✅ 必须通过环境变量注入：`export OPENAI_API_KEY=xxx`
- ✅ 配置文件中使用占位符：`api-key: ${OPENAI_API_KEY}`
- ✅ 生产环境使用密钥管理服务（如 AWS Secrets Manager）
- ✅ 数据库中使用 AES-256 加密存储
- ❌ 禁止：将 API Key 硬编码到 Java 代码中
- ❌ 禁止：将 API Key 提交到 Git 仓库
- ❌ 禁止：在日志中明文打印 API Key

### 5.2 数据隐私

**规则**:
- ✅ PII 数据自动检测并脱敏
- ✅ 用户数据删除权（Right to be Forgotten）
- ✅ 跨境数据传输合规
- ❌ 禁止：将用户原始请求明文存储超过保留期限
- ❌ 禁止：在异常消息中泄露完整请求内容

---

## 6. 演进原则

### 6.1 版本兼容性

**版本号语义**:
- 主版本号变更（v1.x → v2.x）：破坏性变更，需迁移脚本
- 次版本号变更（v1.1 → v1.2）：向后兼容的功能增强
- 修订号变更（v1.2.1 → v1.2.2）：Bug 修复，无行为变更

### 6.2 技术债务管理

**Code Smell 零容忍清单**:
- ❌ 重复代码（超过 3 处相同逻辑）
- ❌ 上帝类（超过 500 行代码的类）
- ❌ 长方法（超过 50 行的方法）
- ❌ 过深的嵌套（超过 3 层的 if-else）
- ❌ 注释掉的代码（必须删除并提交 Git 历史）

---

## 治理

本章程优先于所有其他开发实践。任何偏离必须有文档说明并获得团队负责人批准。
所有组件必须在 CI/CD 流水线中验证章程合规性。
复杂度必须有可衡量的性能或业务价值来证明。

**版本**: 2.8.0 | **制定日期**: 2026-04-08 | **最后修订**: 2026-08-23

**变更记录**:
| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v2.8.0 | 2026-08-23 | **P1 模块化重构同步**：§2.1 更新为 17 模块多模块 Maven 结构（模块化命名），Domain/Infrastructure 下沉至各功能域模块；§2.2-2.5 包路径对齐新结构（Gateway 接口在功能域核心模块、实现在 `<域>data` 绑定模块）；§2.6 调用链路归属模块更新（gateway-proxy/gateway-protocol/gateway-provider/gateway-resilience/gateway-usage/gateway-audit）；§3.1 新增包名规范；§4.3 Provider 扩展改为 ProtocolAdapter 插件机制 |
| v2.0.0 | 2026-04-08 | 初始版本 |
| v2.1.0 | 2026-04-30 | 更新项目结构：替换 analytics 域为 proxy/provider/quota/audit/alert 五域；技术栈版本统一为 Spring Boot 3.5.x |
| v2.2.0 | 2026-05-02 | **域名一致性修正**：统一使用 `provider` 作为模型供给领域名称，与信息架构、应用架构保持一致 |
| v2.3.0 | 2026-05-02 | **领域命名调整**：provider 域更名为 model（模型广场）；与信息架构 v3.5、应用架构 v3.0、数据架构 v1.4 保持一致 |
| v2.3.1 | 2026-05-06 | **Entity 与 DO 关联模式**：新增 Entity 层统一使用 ID 引用原则；明确 DO 层关联策略（主从关系可用 JPA 关联，中间表使用 ID 引用） |
| v2.4.0 | 2026-05-24 | **安全子域拆分**：security 域拆分为 iam（身份与访问控制）、threat（威胁防护）、dataprotection（数据保护）三子域；异常分层更新 |
| v2.5.0 | 2026-05-24 | **供给域重构**：将 model、product、proxy、metadata 四个子域合并为统一的 supply（供给域）；实体重命名 Product→Channel、ProductApiKey→ChannelCredential、ProductModel→ChannelModel、Model→ModelSpec；元数据目录迁移为 supply/catalog；协议层迁移为 supply/protocol |
| v2.6.0 | 2026-05-25 | **协议体系重构+调用链路**：新增 protocol 域（contract/conversion/validation）；协议 DTO 从 supply/protocol 迁至 domain/protocol/contract；校验实现迁至 adapter/protocol；ProtocolGateway→UpstreamClient、ProtocolGatewayFactory→UpstreamClientRegistry；新增 OutboundTuner（出站调谐器）；路由拆为 application/routing/（RoutingResolver/ModelMatcher/ChannelSelector）；新增 §2.6 大模型调用链路（七阶段：校验→路由→转换→调谐→调用→转换→后置） |
| v2.7.0 | 2026-05-31 | **前端主题色规范**：新增 §3.2 前端主题色规范，禁止硬编码颜色值，所有组件颜色必须使用 Ant Design 主题 token |
