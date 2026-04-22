# AI 大模型网关项目章程

## 元信息

| 属性 | 值 |
|------|------|
| 规范名称 | AI Gateway Constitution |
| 版本 | 2.0.0 |
| 状态 | 草案 |
| 创建日期 | 2026-04-08 |
| 技术栈 | Java 21 + Spring Boot 3.2.x + PostgreSQL 14+ + Redis 6.0+ |

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
每个请求必须分别追踪输入/输出 Token，预算控制必须强制执行。
```

**推论**:
- ✅ 每个请求分别统计输入/输出 Token
- ✅ 团队/项目/用户/模型四级预算控制
- ✅ 超预算后执行预设策略（拒绝/降级/切换）
- ❌ 禁止：遗漏 Token 统计
- ❌ 禁止：超预算后静默放行

**验证规则**:
- Token 计量准确率 ≥99.9%
- 预算超限后必须触发对应策略

---

## 2. 架构约束

### 2.1 分层架构（Layered Architecture）

**定义**:
```
系统必须严格遵循分层架构：Web 展现层 → 应用层 → 调度层 → 服务层 → 基础设施层
上层依赖下层接口，禁止跨层调用或反向依赖。
```

**依赖关系**:
```
┌─────────────────────┐
│   Web 展现层        │  ← React SPA (管理控制台)
├─────────────────────┤
│   应用层            │  ← REST Controller, CLI 命令
├─────────────────────┤
│   调度层            │  ← ModelRouter, TokenTracker, RateLimiter
├─────────────────────┤
│   服务层            │  ← GatewayOrchestrator, TranslationService
├─────────────────────┤
│   基础设施层        │  ← LLM Adapters, FileProcessors
└─────────────────────┘
```

**违规示例**:
- ❌ `GatewayController` 直接调用 `DeepSeekAdapter`
- ✅ `GatewayController` → `GatewayOrchestrator` → `ModelRouter` → `LLMProviderAdapter`

### 2.2 领域模型纯洁性

**定义**:
```
所有 JPA 实体必须保持纯洁，禁止包含业务逻辑。
业务逻辑必须封装于 @Service 类中。
```

**实体允许的内容**:
- ✅ Getter/Setter
- ✅ `@PrePersist`, `@PreUpdate` 生命周期回调
- ✅ `toString()`, `equals()`, `hashCode()`

**实体禁止的内容**:
- ❌ 调用外部 API
- ❌ 复杂业务计算
- ❌ 直接修改其他实体状态

### 2.3 配置外部化

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

### 2.4 物理标识与业务标识分离

**定义**:
```
数据层（持久化层）对象使用物理标识（如自增 BIGINT 主键），核心目标是保证数据存储的唯一性、
查询效率与稳定性。业务层/领域层对象使用业务标识（如 tenant_code、model_code、request_id），
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
| 租户 | `id BIGINT AUTO_INCREMENT` | `team_code VARCHAR(64)` |
| 项目 | `id BIGINT AUTO_INCREMENT` | `project_code VARCHAR(64)` |
| 用户 | `id BIGINT AUTO_INCREMENT` | `user_code VARCHAR(64)` |
| 模型 | `id BIGINT AUTO_INCREMENT` | `model_code VARCHAR(128)` |
| 实例 | `id BIGINT AUTO_INCREMENT` | `instance_code VARCHAR(128)` |
| 请求 | `id BIGINT AUTO_INCREMENT` | `request_id VARCHAR(64)` |
| 预算 | `id BIGINT AUTO_INCREMENT` | `budget_code VARCHAR(64)` |
| 告警 | `id BIGINT AUTO_INCREMENT` | `alert_code VARCHAR(64)` |
| 审计 | `id BIGINT AUTO_INCREMENT` | `audit_code VARCHAR(64)` |
| 提示词 | `id BIGINT AUTO_INCREMENT` | `template_code VARCHAR(128)` |
| 策略 | `id BIGINT AUTO_INCREMENT` | `strategy_code VARCHAR(128)` |

### 2.5 全实体可审计（不可妥协）

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


**并发安全保证**:
- ✅ `TokenQuotaTracker` 使用 `AtomicInteger` 保证线程安全
- ✅ `LLMProviderAdapter` 必须是无状态的，支持多线程并发调用
- ✅ 请求记录按任务 ID 分区写入，避免行锁竞争

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

**版本**: 2.0.0 | **制定日期**: 2026-04-08 | **最后修订**: 2026-04-08
