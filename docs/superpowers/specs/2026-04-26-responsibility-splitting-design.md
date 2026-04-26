# LLM-Gateway 架构设计 v2.0

> **版本**: 2.0.0
> **状态**: 草案
> **创建日期**: 2026-04-26
> **基于**: COLA 5.0 (Clean Object-Oriented and Layered Architecture)

---

## 1. 概述

### 1.1 背景

当前 `gateway-core` 是"杂物间"：
- 所有 Entity 集中在 core
- 所有 Service 集中在 core
- 其他模块（security、router 等）只含部分 Service，Entity 仍在 core

这导致：
1. **模块内聚不足** — Entity + Service 分离在不同模块
2. **跨域访问混乱** — 领域服务直接调用其他领域服务
3. **core 难以维护** — 不断膨胀，职责不清

### 1.2 目标

基于 **COLA 5.0** 架构思想，按**业务领域内聚**原则重构：

- Entity + 领域服务 + Gateway 接口 放在同一模块
- **Gateway 接口定义在 domain 层**（依赖倒置）
- **Gateway 实现在 infrastructure 层**
- 跨域协作通过**应用服务层**编排
- 旁路操作通过**领域事件**解耦

### 1.3 COLA 5.0 核心概念

| 概念 | 说明 |
|------|------|
| **Adapter** | 适配器层，用户输入输出接口 |
| **Application** | 应用层，用例编排，无业务逻辑 |
| **Domain** | 领域层，核心业务逻辑，定义 Gateway 接口 |
| **Infrastructure** | 基础设施层，实现 Domain Gateway |

---

## 2. 目标架构

### 2.1 分层结构（基于 Package）

```
gateway/
├── adapter/                 → REST API 端点
│   ├── controller/          → Controller
│   ├── dto/                 → 请求/响应 DTO
│   └── config/              → Web 配置
│
├── application/             → 用例编排（无业务逻辑）
│   └── llm/                 → LLM 调用用例
│
├── domain/                  → 核心业务逻辑
│   ├── gateway/             → 网关接口（依赖倒置核心！）
│   ├── security/            → 安全领域
│   │   ├── entity/          → GatewayApiKey、IpBlocklist
│   │   └── service/         → Domain Service
│   ├── router/              → 路由领域
│   │   ├── entity/          → Model、Provider、RouteGroup
│   │   └── service/         → Domain Service
│   ├── analytics/           → 分析领域
│   │   ├── entity/          → TokenUsage、AuditLog
│   │   └── service/         → Domain Service
│   ├── adapter/             → 适配器领域
│   │   ├── entity/          → ProviderApiKey
│   │   └── service/         → LLM 调用能力
│   └── BizException.java    → 业务异常
│
├── infrastructure/          → 技术实现
│   ├── persistence/        → JPA Repository 实现
│   ├── encryption/         → 加密服务实现
│   └── external/           → 外部 API 调用实现
│
└── common/                  → 共享类型
    ├── dto/                 → 通用 DTO
    └── util/                → 通用工具
```

### 2.2 模块职责

| 模块 | 职责 | 包含内容 |
|------|------|---------|
| **adapter** | HTTP 请求接收，返回响应 | Controller、业务 DTO |
| **application** | 用例编排 | 应用服务，依赖 Domain Gateway 接口 |
| **domain/gateway** | 网关接口定义 | 通往外部世界的门（持久化、外部服务） |
| **domain/xxx** | 领域业务逻辑 | Entity + Service + Gateway 接口 |
| **infrastructure** | 技术实现 | Gateway 实现、加密、持久化 |
| **common** | 纯共享类型 | 通用异常、分页工具（无业务语义） |

### 2.3 依赖方向

```
adapter
    ↓
application
    ↓
domain/gateway ← 定义接口
    ↓
infrastructure ← 实现接口

domain/xxx ← 只依赖 domain/gateway，不依赖 infrastructure
```

---

## 3. Gateway 设计模式（核心）

### 3.1 什么是 Gateway

**Gateway = 通往外部世界的门**

| 外部世界 | Gateway 接口 |
|---------|-------------|
| 数据库 | xxxGateway（持久化） |
| 外部服务 | xxxGateway（远程调用） |
| 文件系统 | xxxGateway（文件操作） |

### 3.2 Gateway vs Service

| 对比项 | Service（之前设计） | Gateway（COLA 模式） |
|--------|---------------------|---------------------|
| **接口定义位置** | 调用方 | domain 层 |
| **实现位置** | 被调用方 | infrastructure 层 |
| **依赖方向** | 应用服务 → 领域服务 | Domain → Gateway 接口 |
| **外部依赖** | 直接调用 | 通过接口间接调用 |

### 3.3 Gateway 接口定义示例

**domain/gateway/ApiKeyGateway.java**:
```java
/**
 * API 密钥网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作。</p>
 */
public interface ApiKeyGateway {

    /**
     * 根据 API Key 查找密钥信息
     *
     * @param apiKey API Key
     * @return 密钥信息，不存在返回 null
     */
    GatewayApiKey findByApiKey(String apiKey);

    /**
     * 保存 API 密钥
     *
     * @param apiKey 密钥实体
     * @return 保存后的实体
     */
    GatewayApiKey save(GatewayApiKey apiKey);

    /**
     * 更新最后使用时间
     *
     * @param apiKey API Key
     * @param lastUsed 最后使用时间
     */
    void updateLastUsed(String apiKey, Instant lastUsed);
}
```

**infrastructure/persistence/JpaApiKeyGateway.java**:
```java
/**
 * API 密钥网关实现
 *
 * <p>实现 ApiKeyGateway 接口，使用 JPA 进行持久化。</p>
 */
@Repository
public class JpaApiKeyGateway implements ApiKeyGateway {

    private final GatewayApiKeyRepository repository;

    @Override
    public GatewayApiKey findByApiKey(String apiKey) {
        return repository.findByApiKey(apiKey).orElse(null);
    }

    @Override
    public GatewayApiKey save(GatewayApiKey apiKey) {
        return repository.save(apiKey);
    }

    @Override
    public void updateLastUsed(String apiKey, Instant lastUsed) {
        repository.findByApiKey(apiKey).ifPresent(key -> {
            key.updateLastUsed(lastUsed);
            repository.save(key);
        });
    }
}
```

### 3.4 Domain Service 使用 Gateway

**domain/security/service/ApiKeyAuthService.java**:
```java
/**
 * API 密钥认证服务
 *
 * <p>领域服务，不直接操作持久化，通过 Gateway 接口访问。</p>
 */
@Service
public class ApiKeyAuthService {

    private final ApiKeyGateway apiKeyGateway;

    public ApiKeyAuthService(ApiKeyGateway apiKeyGateway) {
        this.apiKeyGateway = apiKeyGateway;
    }

    /**
     * 验证 API 密钥
     *
     * @param apiKey API Key
     * @param clientIp 客户端 IP
     * @return 认证结果
     */
    public AuthResult validate(String apiKey, String clientIp) {
        var key = apiKeyGateway.findByApiKey(apiKey);
        if (key == null) {
            return AuthResult.invalid("API Key 不存在");
        }
        if (key.isExpired()) {
            return AuthResult.invalid("API Key 已过期");
        }
        apiKeyGateway.updateLastUsed(apiKey, Instant.now());
        return AuthResult.valid(key.getTeamId());
    }
}
```

### 3.5 依赖注入配置

**infrastructure/config/GatewayConfiguration.java**:
```java
@Configuration
public class GatewayConfiguration {

    @Bean
    public ApiKeyGateway apiKeyGateway(GatewayApiKeyRepository repository) {
        return new JpaApiKeyGateway(repository);
    }

    @Bean
    public ModelGateway modelGateway(ModelRepository repository) {
        return new JpaModelGateway(repository);
    }

    // ... 其他 Gateway 配置
}
```

---

## 4. Domain 结构

### 4.1 Domain 包组织

```
domain/
├── gateway/                 → 网关接口（通往外部）
│   ├── ApiKeyGateway.java
│   ├── ModelGateway.java
│   ├── TokenGateway.java
│   └── AuditGateway.java
│
├── security/               → 安全领域
│   ├── entity/             → 实体
│   │   ├── GatewayApiKey.java
│   │   └── IpBlocklist.java
│   └── service/            → 领域服务
│       ├── ApiKeyAuthService.java
│       └── IpBlockService.java
│
├── router/                 → 路由领域
│   ├── entity/             → 实体
│   │   ├── Model.java
│   │   ├── Provider.java
│   │   └── RouteGroup.java
│   └── service/            → 领域服务
│       └── ModelRouterService.java
│
├── analytics/              → 分析领域
│   ├── entity/            → 实体
│   │   ├── TokenUsage.java
│   │   ├── AuditLog.java
│   │   └── TokenLimit.java
│   └── service/           → 领域服务
│       ├── TokenTrackingService.java
│       └── AuditService.java
│
├── adapter/               → 适配器领域
│   ├── entity/           → 实体
│   │   └── ProviderApiKey.java
│   └── service/          → 领域服务
│       └── LLMProviderService.java
│
├── Entity.java            → 基础实体接口
└── BizException.java      → 业务异常基类
```

### 4.2 Entity 归属

| 领域 | Entity | Gateway 接口 |
|------|--------|-------------|
| **security** | GatewayApiKey | ApiKeyGateway |
| **security** | IpBlocklist | IpBlockGateway |
| **router** | Model | ModelGateway |
| **router** | Provider | ProviderGateway |
| **router** | RouteGroup | RouteGroupGateway |
| **analytics** | TokenUsage | TokenGateway |
| **analytics** | AuditLog | AuditGateway |
| **analytics** | TokenLimit | TokenLimitGateway |
| **adapter** | ProviderApiKey | CredentialGateway |

---

## 5. 请求流程

### 5.1 主流程：应用服务编排

```
HTTP 请求
    ↓
Adapter → Controller
    ↓
Application → LLMChatService
    ↓
    ├── Domain Service（通过 Gateway 接口）
    │   ├── ApiKeyAuthService → ApiKeyGateway
    │   ├── ModelRouterService → ModelGateway
    │   └── LLMProviderService → LLM 调用
    │
    └── ApplicationEventPublisher → 发布事件
        ↓
    ┌─────────┴─────────┐
    ↓                   ↓
TokenUsedEvent        AuditLogEvent
    ↓                   ↓
TokenTrackingService  AuditService（异步）
    ↓                   ↓
    HTTP 响应
```

### 5.2 代码示例

**application/llm/LLMChatService.java**:
```java
@Service
@RequiredArgsConstructor
public class LLMChatService {

    private final ApiKeyAuthService apiKeyAuthService;
    private final ModelRouterService modelRouterService;
    private final LLMProviderService llmProviderService;
    private final ApplicationEventPublisher eventPublisher;

    public LLMResponse chat(ChatRequest request) {
        // 1. 认证（通过 Domain Service）
        var auth = apiKeyAuthService.validate(
            request.getApiKey(),
            request.getClientIp()
        );

        // 2. 路由（通过 Domain Service，使用 Gateway 接口）
        var model = modelRouterService.selectModel(
            request.getModel(),
            auth.getTeamId(),
            RoutingStrategy.COST_OPTIMIZED
        );

        // 3. LLM 调用
        var response = llmProviderService.chat(model, request);

        // 4. 发布事件（异步旁路）
        eventPublisher.publishEvent(new TokenUsedEvent(
            auth.getTeamId(),
            model.getModelCode(),
            response.getInputTokens(),
            response.getOutputTokens()
        ));

        return response;
    }
}
```

---

## 6. 跨域访问规则

| 方式 | 场景 | 规则 |
|------|------|------|
| **Gateway 接口** | Domain 访问外部资源 | ✅ 定义在 domain/gateway/ |
| **应用服务编排** | 主流程（认证→路由→调用） | ✅ Application 调用 Domain Service |
| **领域事件** | 旁路（统计、审计） | ✅ 异步解耦 |
| **Domain 直接调用其他 Domain** | 主流程中 | ❌ 禁止 |

---

## 7. 与之前设计的对比

| 对比项 | 之前设计 | COLA 5.0 设计 |
|--------|---------|---------------|
| **组织方式** | 多模块 Maven | 单模块 Package 分层 |
| **接口定义** | 接口定义在调用方 | 接口定义在 domain/gateway/ |
| **外部访问** | Service 接口 | Gateway 接口 |
| **Domain 组织** | 按技术层拆分 | 按业务能力拆分（domain/xxx/） |
| **Infrastructure** | 通用技术组件 | 实现 domain gateway |

---

## 8. 风险与注意事项

| 风险 | 应对措施 |
|------|---------|
| Gateway 接口膨胀 | 按需定义，避免过度设计 |
| Domain 依赖 infrastructure | 通过 Gateway 依赖倒置，Domain 不直接依赖 |
| 单一模块代码冲突 | 合理分包，按业务能力隔离 |
| 测试难度 | Domain 可 Mock Gateway 独立测试 |

---

## 9. 总结

**核心原则：**
- **Gateway 模式**：接口定义在 domain 层，实现 in infrastructure 层
- **依赖倒置**：Domain 只知道 Gateway 接口，不知道实现
- **业务内聚**：domain/xxx 按业务能力组织，Entity + Service + Gateway 在一起
- **应用服务编排**：跨域协作在 application 层
- **旁路事件驱动**：统计、审计用事件异步处理

**架构优势：**
- 依赖方向清晰：Domain → Gateway → Infrastructure
- 可测试性高：Domain 可 Mock Gateway 独立测试
- 可扩展性强：新的外部依赖只需添加 Gateway 实现
- 业务内聚：相关代码组织在一起
