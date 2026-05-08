# LLM-Gateway 项目章程

<!-- SYNCIMPACT: v1.0.1 | 2026-04-22 | 初始版本，整合 doc/constitution.md、doc/spec.md、doc/实体与实体关系.md -->

## 核心原则

### 一、双 API 兼容（不可妥协）

LLM-Gateway 必须同时暴露 OpenAI API v1 和 Anthropic Messages API 两种标准端点。上游客户端必须看到标准的 OpenAI 或 Anthropic 接口，无论下游提供商使用何种格式。

**规则**：
- OpenAI 格式：`/v1/chat/completions` 端点 100% 兼容 OpenAI 标准
- Anthropic 格式：`/v1/messages` 端点 100% 兼容 Anthropic Messages API
- 使用 `provider/model` 命名约定（如 `openai/gpt-4o`、`zhipu/glm-4.6`）
- 参数转换准确率 ≥99.9%
- 工具调用 / 函数调用 双向映射完整支持
- Anthropic 兼容下游端点直通转发（零转换延迟）
- 新增模型接入时间 ≤2 小时
- 协议转换矩阵：OpenAI ↔ Anthropic ↔ Gemini ↔ 厂商原生格式

### 二、安全零信任（不可妥协）

所有请求默认不可信，必须经过认证、鉴权、限流、脱敏四层安全检查。API 密钥必须加密存储，禁止硬编码。

**规则**：
- 传输中数据使用 TLS 1.3+ 加密
- PII（个人身份信息）自动检测并脱敏
- API 密钥使用 AES-256 或国密算法加密存储
- 支持 RBAC 和 ABAC 双重访问控制
- 每个关键操作记录审计日志
- 禁止硬编码 API 密钥到源代码中
- 禁止在日志中明文打印敏感信息
- 禁止将 API 密钥提交到 Git 仓库

### 三、测试驱动开发（不可妥协）

所有功能必须遵循 TDD 方法论：编写测试 → 测试失败 → 然后实现。覆盖率要求强制执行。

**规则**：
- 核心服务层覆盖率：≥90%
- 路由引擎覆盖率：≥85%
- 适配器层覆盖率：≥80%
- 所有适配器集成需要集成测试
- 性能测试验证 P95 延迟 ≤10ms，吞吐量 ≥10,000 QPS
- 无测试代码 → 禁止提交
- 禁止跳过适配器部署的集成测试

### 四、可观测性内建

每个请求必须支持 OpenTelemetry 标准追踪。禁止静默执行或批量完成后才报告结果。

**规则**：
- 所有请求携带 Trace ID，贯穿网关入口 → 认证 → 路由 → 模型调用 → 响应
- 结构化 JSON 日志，包含 `[TRACE_ID]`、`[REQUEST_ID]`、`[MODEL]` 等元数据
- 实时指标采集：延迟（P50/P95/P99）、成功率、QPS、Token 消耗
- 禁止使用 `System.out.println`
- 禁止吞掉异常堆栈

### 五、Token 成本透明化

Token 必须是所有成本追踪的核心单位。每个请求必须分别追踪输入/输出 Token，预算控制必须强制执行。

**规则**：
- 每个请求分别统计输入/输出 Token
- 四级预算控制：团队 → 用户 → 令牌 → 用户×渠道
- 超预算后执行预设策略：拒绝 / 降级 / 切换
- Token 计量准确率 ≥99.9%
- 预算超限后必须触发对应策略，禁止静默放行

---

## 架构约束

### 2.1 分层架构

系统必须严格遵循分层架构。上层依赖下层接口，禁止跨层调用或反向依赖。

```
┌─────────────────────────┐
│   Web / CLI 层          │  ← React SPA（管理控制台）
├─────────────────────────┤
│   应用层                 │  ← REST Controller, CLI 命令
├─────────────────────────┤
│   调度层                 │  ← ModelRouter, TokenTracker, RateLimiter
├─────────────────────────┤
│   服务层                 │  ← GatewayOrchestrator, TranslationService
├─────────────────────────┤
│   基础设施层             │  ← LLM Adapters, FileProcessors
└─────────────────────────┘
```

### 2.2 领域模型纯洁性

所有 JPA 实体必须保持纯洁，禁止包含业务逻辑。业务逻辑必须封装于 `@Service` 类中。

**实体允许的内容**：
- Getter/Setter
- `@PrePersist`、`@PreUpdate` 生命周期回调
- `toString()`、`equals()`、`hashCode()`

**实体禁止的内容**：
- 调用外部 API
- 复杂业务计算
- 直接修改其他实体状态

### 2.3 配置外部化

所有可变参数必须通过 `@ConfigurationProperties` 外部化。禁止在代码中出现魔法数字或硬编码字符串。

**配置优先级**：`命令行参数 > 环境变量 > application-local.yml > application.yml > 数据库默认值`

### 2.4 全实体可审计（不可妥协）

每个业务实体表必须包含完整的审计字段，记录谁在何时创建、修改和删除了数据。

**规则**：
- 每张业务表必须包含：`created_by`、`created_at`、`updated_by`、`updated_at`
- 支持软删除的表还需包含：`deleted_by`、`deleted_at`
- `created_by`/`updated_by`/`deleted_by` 使用用户物理 ID（`BIGINT FK → users.id`）
- 系统自动生成的记录（如请求日志）`created_by` 可为 NULL 或填系统用户 ID（0L）
- 审计字段必须通过 JPA `@EntityListeners` 自动填充，禁止业务代码手动修改
- 禁止任何业务表缺少审计字段
- 禁止审计字段被业务代码手动修改

---

## 实体领域模型

### 3.1 六大实体域

| 域 | 实体 | 说明 |
|---|------|------|
| **身份与访问控制** | Team, User, Role, Permission, Member | 认证、授权、团队隔离 |
| **渠道与模型** | Provider, Model, Channel, ChannelGroup, ChannelKey, Strategy, StrategyNode | LLM 提供商连接、路由 |
| **令牌与认证** | ApiToken, TokenQuota, TokenAccessLog | API 凭证、额度管理 |
| **Token 额度** | TokenLimit, TokenUsage, TokenLimitAlert | 四级预算控制 |
| **日志与监控** | RequestLog, RequestBodyLog, AuditLog, ChannelHealthLog | 可观测性和审计 |
| **安全与风控** | ApiKeySecret, SensitiveWord, PiiRule, IpBlacklist, IpWhitelist, RateLimitRule | 安全和访问控制 |

### 3.2 核心实体命名规范

| 实体 | 物理主键 | 外键 |
|------|---------|---------|------|
| Team | `id BIGINT` | `admin_id BIGINT → User.id` |
| User | `id BIGINT` | - |
| Role | `id BIGINT` | - |
| Member | `id BIGINT` |`user_id BIGINT`, `team_id BIGINT` |
| Provider | `id BIGINT` | `provider_code VARCHAR(64)` | - |
| Model | `id BIGINT` | `provider_id BIGINT → Provider.id` |
| Channel | `id BIGINT` |`team_id`, `provider_id`, `group_id` |
| ChannelGroup | `id BIGINT` | `team_id BIGINT → Team.id` |
| ChannelKey | `id BIGINT` | - | `channel_id`, `secret_id BIGINT → ApiKeySecret.id` |
| Strategy | `id BIGINT` |`team_id BIGINT → Team.id` |
| TokenLimit | `id BIGINT` | `scope_id`, `user_id`, `channel_id` |
| ApiToken | `id BIGINT` | `user_id`, `team_id BIGINT` |

---

## 技术标准

### 4.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 实体类 | PascalCase + 明确业务含义 | `GatewayConfig`, `ModelProvider` |
| 服务类 | PascalCase + `Service` 后缀 | `RoutingService`, `CostService` |
| 接口 | PascalCase + 能力描述 | `ModelRouter`, `TokenCounter` |
| 方法 | camelCase + 动词开头 | `routeRequest()`, `countTokens()` |
| 变量 | camelCase + 名词 | `tokenThreshold`, `providerId` |
| 常量 | UPPER_SNAKE_CASE | `DEFAULT_TOKEN_THRESHOLD` |
| 数据库表 | snake_case + 复数 | `model_providers`, `routing_strategies` |

**包名规范**：

基础包名：`com.codingas.gateway`

| 包路径 | 说明 |
|--------|------|
| `com.codingas.gateway.api` | API 层（Controller） |
| `com.codingas.gateway.application` | 应用层（DTO、Assembler） |
| `com.codingas.gateway.dispatch` | 调度层（Router、Tracker、Limiter） |
| `com.codingas.gateway.service` | 服务层（业务逻辑） |
| `com.codingas.gateway.infrastructure` | 基础设施层（Adapter、Repository） |
| `com.codingas.gateway.domain` | 领域模型（Entity、ValueObject） |
| `com.codingas.gateway.domain.team` | 团队领域 |
| `com.codingas.gateway.domain.user` | 用户领域 |
| `com.codingas.gateway.domain.channel` | 渠道领域 |
| `com.codingas.gateway.domain.token` | 令牌领域 |
| `com.codingas.gateway.domain.audit` | 审计领域 |
| `com.codingas.gateway.adapter` | LLM 适配器（OpenAI、Anthropic 等） |
| `com.codingas.gateway.common` | 公共工具（异常、日志、工具类） |

### 4.2 异常分层

```
GatewayException（根异常）
├── GatewayRequestException（请求级异常）
│   ├── InvalidModelException
│   ├── BudgetExceededException
│   └── RateLimitExceededException
├── ProviderException（提供商级异常）
│   ├── ProviderUnavailableException
│   ├── TokenQuotaExceededException
│   └── ProviderResponseException
└── SecurityException（安全级异常）
    ├── AuthenticationException
    └── AuthorizationException
```

### 4.3 事务边界

- 隔离级别：`READ_COMMITTED`
- 传播行为：`REQUIRED`（默认）

### 4.4 异常处理

**规则**：
- 所有受检异常必须转换为运行时异常
- 每个异常必须包含清晰的错误上下文（请求 ID、模型 ID、提供商）
- 异常日志必须包含完整的堆栈轨迹与重试历史

### 4.5 可观测性 - 日志分级

| 级别 | 使用场景 | 示例 |
|------|----------|------|
| ERROR | 系统错误，需要人工介入 | 所有 Provider 均失败 |
| WARN | 可恢复异常，需关注 | Provider 切换、Token 接近限额 |
| INFO | 关键业务流程 | 请求处理完成、模型切换 |
| DEBUG | 详细调试信息 | 路由决策过程、API 请求/响应 |
| TRACE | 最细粒度追踪 | 每个字段的处理前后对比 |

### 4.6 可扩展性 - 新增 Provider

**步骤**：
1. 实现 `LLMProviderAdapter` 接口
2. 添加 `@Component` 注解
3. 通过 Web 界面或 REST API 配置 Provider 信息
4. （可选）调整优先级顺序

**开闭原则**：
- ✅ 对扩展开放：新增 Provider 无需修改现有代码
- ✅ 对修改关闭：现有 Provider 逻辑不受影响

### 4.7 并发控制

- JDK 21 虚拟线程提供轻量级并发
- `TokenQuotaTracker` 使用 `AtomicInteger` 保证线程安全
- `LLMProviderAdapter` 必须是无状态的，支持多线程并发调用
- 请求记录按任务 ID 分区写入，避免行锁竞争

### 4.8 Code Smell 零容忍

- ❌ 重复代码（超过 3 处相同逻辑）
- ❌ 上帝类（超过 500 行代码的类）
- ❌ 长方法（超过 50 行的方法）
- ❌ 过深的嵌套（超过 3 层的 if-else）
- ❌ 注释掉的代码（必须删除并提交 Git 历史）

---

## 安全红线

### 5.1 API 密钥保护

**规则**：
- 必须通过环境变量注入：`export OPENAI_API_KEY=xxx`
- 配置文件中使用占位符：`api-key: ${OPENAI_API_KEY}`
- 生产环境使用密钥管理服务（如 AWS Secrets Manager）
- 数据库中使用 AES-256 加密存储
- ❌ 禁止：将 API 密钥硬编码到 Java 代码中
- ❌ 禁止：将 API 密钥提交到 Git 仓库
- ❌ 禁止：在日志中明文打印 API 密钥

### 5.2 数据隐私

**规则**：
- PII 数据自动检测并脱敏
- 用户数据删除权（Right to be Forgotten）支持
- 跨境数据传输合规
- ❌ 禁止：将用户原始请求明文存储超过保留期限
- ❌ 禁止：在异常消息中泄露完整请求内容

---

## 版本演进

### 6.1 语义化版本

| 版本变更 | 含义 | 所需操作 |
|----------|------|----------|
| 主版本（v1.x → v2.x） | 破坏性变更 | 需要迁移脚本 |
| 次版本（v1.1 → v1.2） | 向后兼容的功能增强 | 无需迁移 |
| 修订号（v1.2.1 → v1.2.2） | Bug 修复，无行为变更 | 无需迁移 |

---

## 开发工作流

### 7.1 功能实现

1. **研究**：GitHub 代码搜索优先 → 库文档其次 → Exa 用于更广泛研究
2. **规划**：使用 `/speckit-plan` 创建实现计划
3. **TDD**：先写测试（RED）→ 实现（GREEN）→ 重构（IMPROVE）→ 验证 80%+ 覆盖率
4. **代码审查**：编写代码后立即使用 code-reviewer agent
5. **提交**：遵循约定式提交格式的详细提交消息

### 7.2 修订流程

1. 提议对 `.specify/memory/constitution.md` 的更改
2. 更新同步影响报告中列出的受影响模板
3. 版本升级：MAJOR（破坏性）、MINOR（新内容）、PATCH（澄清）
4. 需要通过 PR 获取维护者批准

### 7.3 模板创建

1. 将新模板放入 `.specify/templates/`
2. 遵循 constitution-template.md 结构
3. 包含 frontmatter：`name`、`description`、`compatibility`、`user-invocable`、`disable-model-invocation`
4. 如有新命令则更新集成清单
5. 在 speckit-constitution skill 中记录

---

## 治理

本章程优先于所有其他开发实践。任何偏离必须有文档说明并获得团队负责人批准。

**合规验证**：
- 所有 PR 必须通过章程检查
- 模板更改需要模板验证
- Hook 更改需要 Hook 模式验证
- CI/CD 流水线验证章程合规性

**复杂度证明**：复杂度必须有可衡量的性能或业务价值来证明。

**版本**：v1.0.1 | **制定日期**：2026-04-22 | **最后修订**：2026-04-22
