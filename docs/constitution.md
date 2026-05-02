# AI 大模型网关项目章程

## 元信息

| 属性 | 值 |
|------|------|
| 规范名称 | AI Gateway Constitution |
| 版本 | 2.3.0 |
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

### 2.1 COLA Light 5.0 架构

**定义**:
```
系统采用 COLA Light 5.0 架构：单模块架构，用 package 代替模块划分层次。
按业务领域分包，Gateway 接口定义在 domain 层，实现 in infrastructure 层（依赖倒置）。
跨域协作通过应用服务层编排，旁路操作通过领域事件解耦。
```

**项目结构（Maven 单一模块）**:
```
gateway-boot/                          # Maven 单一模块
└── src/main/java/com/codingas/gateway/
    ├── adapter/                       # 适配器层（按用例分包）
    │   ├── auth/controller/ & dto/
    │   ├── chat/controller/ & dto/
    │   ├── model/controller/ & dto/
    │   └── admin/controller/ & dto/
    ├── application/                   # 应用层（按用例分包）
    │   ├── auth/
    │   ├── chat/
    │   └── model/
    ├── domain/                        # 领域层
    │   ├── gateway/                   # 跨领域 Gateway 接口
    │   ├── router/                    # 路由领域（核心域）
    │   │   ├── entity/
    │   │   ├── service/
    │   │   ├── gateway/
    │   │   ├── enums/
    │   │   └── exception/
    │   ├── proxy/                     # 代理领域（核心域）
    │   │   ├── service/
    │   │   └── gateway/
    │   ├── model/                     # 模型广场领域
    │   │   ├── entity/
    │   │   ├── service/
    │   │   ├── gateway/
    │   │   ├── enums/
    │   │   └── exception/
    │   ├── security/                  # 安全控制领域
    │   │   ├── entity/
    │   │   ├── service/
    │   │   ├── gateway/
    │   │   ├── enums/
    │   │   └── exception/
    │   ├── quota/                     # 限额配额领域
    │   │   ├── entity/
    │   │   ├── service/
    │   │   ├── gateway/
    │   │   ├── enums/
    │   │   └── exception/
    │   ├── audit/                     # 审计合规领域
    │   │   ├── entity/
    │   │   ├── service/
    │   │   ├── gateway/
    │   │   ├── enums/
    │   │   └── exception/
    │   └── alert/                     # 告警管理领域
    │       ├── entity/
    │       ├── service/
    │       ├── gateway/
    │       ├── enums/
    │       └── exception/
    ├── infrastructure/                # 基础设施层
    │   ├── config/
    │   ├── gateway/                  # Gateway 实现
    │   │   ├── provider/             # Provider/Model 持久化
    │   │   ├── security/            # User/ApiKey 持久化
    │   │   ├── quota/               # 限流实现
    │   │   ├── audit/               # 审计持久化
    │   │   ├── alert/               # 告警持久化
    │   │   └── llm/                 # OpenAI/Anthropic 代理实现
    │   └── util/
    └── common/                        # 公共组件
        ├── constants/
        ├── exception/
        └── util/
```

**各层职责**:

| 层 | 职责 | 包含内容 |
|---|------|---------|
| **adapter** | 接收请求、返回响应 | Controller、DTO（按用例分包） |
| **application** | 用例编排，跨域协调 | Application Service（按用例分包） |
| **domain** | 业务逻辑、领域模型 | Entity、Domain Service、Gateway 接口、异常、枚举 |
| **infrastructure** | 技术实现 | Gateway 实现、配置、工具 |
| **common** | 跨领域共享 | 基础异常、技术常量、工具类 |

### 2.2 Gateway 模式

**定义**:
```
Gateway 接口定义在 domain/xxx/gateway/ 包中。
Gateway 实现在 infrastructure/xxx/gateway/ 包中。
Domain 只依赖 Gateway 接口，不直接依赖外部资源。
Infrastructure 通过依赖注入实现 Gateway。
```

```
Domain 层                     Infrastructure 层
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
| Gateway 接口 | `domain/xxx/gateway/` |
| Gateway 实现 | `infrastructure/xxx/gateway/` |

### 2.3 服务分类

**Domain Service（领域服务）**：
- 职责：业务逻辑，领域规则
- 放置：`domain/xxx/service/`
- 命名：能力名 + `DomainService` 后缀
- 示例：`AuthenticationDomainService`, `RateLimitDomainService`, `RbacDomainService`

**Application Service（应用服务）**：
- 职责：用例编排，跨领域协调，不含业务逻辑
- 放置：`application/xxx/`（按用例分包）
- 命名：能力名 + `Service` 后缀
- 示例：`AuthenticationService`, `RateLimitService`, `TokenCounterService`

**命名区分原则**：
| 层 | 命名模式 | 语义 |
|---|---------|------|
| Domain | `XxxDomainService` | 领域能力，表示"能做什么" |
| Application | `XxxService` | 应用服务，表示"执行什么操作" |

**示例对照**：
```
domain/security/service/AuthenticationDomainService     # 领域认证能力
domain/security/service/RateLimitDomainService           # 领域限流能力
application/auth/AuthenticationService                    # 应用认证服务
application/auth/TokenLimitService                       # 应用限额服务
```

### 2.4 Exception 分类

| 类型 | 放置位置 | 示例 |
|------|---------|------|
| 基础异常 | `common/exception/` | GatewayException |
| 领域异常 | `domain/xxx/exception/` | AuthenticationException |
| 基础设施异常 | `infrastructure/exception/` | ProviderException |

### 2.5 跨域访问规则

| 方式 | 场景 | 规则 |
|------|------|------|
| Gateway 接口 | Domain 访问外部资源 | ✅ 定义在 `domain/xxx/gateway/` |
| 应用服务编排 | 主流程（认证→路由→调用） | ✅ Application 调用 Domain Service |
| 领域事件 | 旁路（统计、审计） | ✅ 异步解耦 |
| Domain 直接调用其他 Domain | 主流程中 | ❌ 禁止 |
| Domain 直接访问外部资源 | 持久化、外部 API | ❌ 必须通过 Gateway |

**违规示例**:
- ❌ `ChatApplication` 直接调用 `JpaModelRepository` 获取 Entity
- ✅ `ChatApplication` → `ModelRouterService` → `ModelGateway` → `JpaModelGateway`
- ❌ `DomainService` 直接使用 `EntityManager` 持久化
- ✅ `DomainService` → `XxxGateway` → `JpaXxxGateway`
- ❌ `security` Domain 直接调用 `router` Domain 的服务
- ✅ `application/` 编排两者的调用

### 2.3 领域模型纯洁性

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
- ✅ Gateway 接口定义在 `domain/xxx/gateway/`，实现 `infrastructure/xxx/gateway/`
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

### 2.4 配置外部化

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

### 2.5 物理标识与业务标识分离

**定义**:
```
数据层（持久化层）对象使用物理标识（如自增 BIGINT 主键），核心目标是保证数据存储的唯一性、
查询效率与稳定性。业务层/领域层对象使用业务标识（如 model_code、request_id），
核心目标是承载业务规则、提供用户可读性并作为业务交互的入口。
```

**推论**:
- ✅ 所有数据库表使用 `id BIGINT AUTO_INCREMENT` 作为物理主键
- ✅ 每张业务表必须有对应的业务标识字段（`*_code` VARCHAR，UNIQUE 约束）
- ✅ 表间外键关联使用物理 ID（`*_id BIGINT`）
- ✅ 对外 API、日志、审计使用业务标识
- ❌ 禁止：将业务标识用作表间关联（性能差、可变性高）
- ❌ 禁止：将物理主键暴露给外部客户端（缺乏可读性、存在信息泄露风险）

**示例对照**:

| 实体 | 物理标识（数据层） | 业务标识（业务层） |
|------|-------------------|-------------------|
| 用户 | `id BIGINT AUTO_INCREMENT` | `user_code VARCHAR(64)` |
| 模型 | `id BIGINT AUTO_INCREMENT` | `model_code VARCHAR(128)` |
| 实例 | `id BIGINT AUTO_INCREMENT` | `instance_code VARCHAR(128)` |
| 请求 | `id BIGINT AUTO_INCREMENT` | `request_id VARCHAR(64)` |
| Token限额 | `id BIGINT AUTO_INCREMENT` | `quota_code VARCHAR(64)` |
| 告警 | `id BIGINT AUTO_INCREMENT` | `alert_code VARCHAR(64)` |
| 审计 | `id BIGINT AUTO_INCREMENT` | `audit_code VARCHAR(64)` |
| 提示词 | `id BIGINT AUTO_INCREMENT` | `template_code VARCHAR(128)` |
| 策略 | `id BIGINT AUTO_INCREMENT` | `strategy_code VARCHAR(128)` |

### 2.6 全实体可审计（不可妥协）

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

### 3.2 异常处理规范

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
└── SecurityException (安全级异常)
    ├── AuthenticationException
    └── AuthorizationException
```

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
- ✅ `LLMProviderAdapter` 必须是无状态的，支持多线程并发调用
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

**新增 Provider 步骤**:
1. 实现 `LLMProviderAdapter` 接口
2. 添加 `@Component` 注解
3. 通过 Web 界面或 REST API 配置 Provider 信息
4. （可选）调整优先级顺序

**开闭原则**:
- ✅ 对扩展开放：新增 Provider 无需修改现有代码
- ✅ 对修改关闭：现有 Provider 逻辑不受影响

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

**版本**: 2.2.0 | **制定日期**: 2026-04-08 | **最后修订**: 2026-05-02

**变更记录**:
| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v2.0.0 | 2026-04-08 | 初始版本 |
| v2.1.0 | 2026-04-30 | 更新项目结构：替换 analytics 域为 proxy/provider/quota/audit/alert 五域；技术栈版本统一为 Spring Boot 3.5.x |
| v2.2.0 | 2026-05-02 | **域名一致性修正**：统一使用 `provider` 作为模型供给领域名称，与信息架构、应用架构保持一致 |
| v2.3.0 | 2026-05-02 | **领域命名调整**：provider 域更名为 model（模型广场）；与信息架构 v3.5、应用架构 v3.0、数据架构 v1.4 保持一致 |
