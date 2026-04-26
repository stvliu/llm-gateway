# LLM-Gateway 架构设计 v3.0

> **版本**: 3.0.0
> **状态**: 已确认
> **创建日期**: 2026-04-26
> **更新日期**: 2026-04-27
> **基于**: COLA Light 5.0 (单模块架构，用 package 代替模块划分层次)

---

## 1. 概述

### 1.1 背景

当前项目采用多模块 Maven 架构，模块划分与 COLA 架构原则不一致：
- `gateway-core` 过于臃肿，混杂 domain + service + repository
- 模块边界与业务领域边界不一致
- Gateway 接口与实现位置不明确

### 1.2 目标

采用 **COLA Light 5.0** 架构思想：

- **单模块架构**：用 package 代替模块划分层次
- **Gateway 接口定义在 domain 层**（依赖倒置）
- **Gateway 实现在 infrastructure 层**
- 跨域协作通过**应用服务层**编排
- 旁路操作通过**领域事件**解耦

### 1.3 COLA Light 5.0 核心概念

| 概念 | 说明 |
|------|------|
| **Adapter** | 适配器层，用户输入输出接口（按用例分包） |
| **Application** | 应用层，用例编排，无业务逻辑（按用例分包） |
| **Domain** | 领域层，核心业务逻辑，定义 Gateway 接口 |
| **Infrastructure** | 基础设施层，实现 Domain Gateway |
| **Common** | 公共组件，跨领域共享类型 |

---

## 2. 目标架构

### 2.1 分层结构（基于 Package）

```
gateway-boot/                          # Maven 单一模块
├── pom.xml
└── src/main/java/com/codingas/gateway/
    ├── adapter/                       # 适配器层（按用例分包）
    │   ├── auth/                     # 认证用例
    │   │   ├── controller/
    │   │   └── dto/
    │   ├── chat/                     # 聊天用例
    │   │   ├── controller/
    │   │   └── dto/
    │   ├── model/                   # 模型管理用例
    │   │   ├── controller/
    │   │   └── dto/
    │   └── admin/                   # 管理用例
    │       ├── controller/
    │       └── dto/
    │
    ├── application/                   # 应用层（按用例分包）
    │   ├── auth/
    │   ├── chat/
    │   └── model/
    │
    ├── domain/                        # 领域层
    │   ├── gateway/                   # 跨领域 Gateway 接口
    │   ├── security/                  # 安全领域
    │   │   ├── entity/
    │   │   ├── service/
    │   │   ├── gateway/
    │   │   ├── enums/
    │   │   └── exception/
    │   ├── router/                    # 路由领域
    │   │   ├── entity/
    │   │   ├── service/
    │   │   ├── gateway/
    │   │   ├── enums/
    │   │   └── exception/
    │   └── analytics/                # 分析领域
    │       ├── entity/
    │       ├── service/
    │       ├── gateway/
    │       ├── enums/
    │       └── exception/
    │
    ├── infrastructure/                # 基础设施层
    │   ├── config/
    │   ├── gateway/                  # Gateway 实现
    │   │   ├── security/
    │   │   ├── router/
    │   │   └── analytics/
    │   └── util/
    │
    └── common/                        # 公共组件
        ├── constants/
        ├── exception/
        └── util/
```

### 2.2 各层职责

| 层 | 职责 | 包含内容 |
|---|------|---------|
| **adapter** | 接收请求、返回响应 | Controller、DTO（按用例分包） |
| **application** | 用例编排，跨域协调 | Application Service（按用例分包） |
| **domain** | 业务逻辑、领域模型 | Entity、Domain Service、Gateway 接口、异常、枚举 |
| **infrastructure** | 技术实现 | Gateway 实现、配置、工具 |
| **common** | 跨领域共享 | 基础异常、技术常量、工具类 |

### 2.3 依赖方向

```
adapter (Controller)
    ↓
application (用例编排)
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
├── gateway/                   # 跨领域 Gateway 接口
│   └── ...                    # 通用 Gateway
│
├── security/                  # 安全领域
│   ├── entity/               # 实体
│   │   ├── User.java
│   │   ├── GatewayApiKey.java
│   │   └── IpBlocklist.java
│   ├── service/              # 领域服务
│   │   ├── AuthenticationService.java
│   │   ├── RateLimitService.java
│   │   └── RbacService.java
│   ├── gateway/             # 安全领域 Gateway 接口
│   │   ├── ApiKeyGateway.java
│   │   └── AuditGateway.java
│   ├── enums/
│   │   └── ...
│   └── exception/
│       └── ...
│
├── router/                   # 路由领域
│   ├── entity/               # 实体
│   │   ├── Model.java
│   │   ├── Provider.java
│   │   └── RouteGroup.java
│   ├── service/              # 领域服务
│   │   └── ModelRouterService.java
│   ├── gateway/             # 路由领域 Gateway 接口
│   │   ├── ModelGateway.java
│   │   └── ProviderGateway.java
│   ├── enums/
│   └── exception/
│
└── analytics/               # 分析领域
    ├── entity/              # 实体
    │   ├── TokenUsage.java
    │   ├── AuditLog.java
    │   └── TokenLimit.java
    ├── service/             # 领域服务
    │   └── TokenTrackingService.java
    ├── gateway/
    ├── enums/
    └── exception/
```

### 4.2 Entity 与 Gateway 接口归属

| 领域 | Entity | Gateway 接口 | 实现位置 |
|------|--------|-------------|---------|
| **security** | GatewayApiKey | ApiKeyGateway | infrastructure/security/gateway/ |
| **security** | AuditLog | AuditGateway | infrastructure/security/gateway/ |
| **router** | Model | ModelGateway | infrastructure/router/gateway/ |
| **router** | Provider | ProviderGateway | infrastructure/router/gateway/ |
| **analytics** | TokenLimit | TokenLimitGateway | infrastructure/analytics/gateway/ |

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

| 对比项 | 之前设计 | COLA Light 5.0 设计 |
|--------|---------|---------------------|
| **组织方式** | 多模块 Maven（8个模块） | 单模块 Package（gateway-boot） |
| **接口定义** | 接口定义在调用方 | 接口定义在 domain/xxx/gateway/ |
| **外部访问** | Service 接口 | Gateway 接口 |
| **Domain 组织** | 按技术层拆分 | 按业务能力拆分（domain/xxx/） |
| **Infrastructure** | 通用技术组件 | 实现 domain/xxx/gateway/ |
| **Controller/DTO** | 散落各模块 | adapter/xxx/（按用例分包） |
| **Application Service** | 未明确分类 | application/xxx/（按用例分包） |

---

## 8. 风险与注意事项

| 风险 | 应对措施 |
|------|---------|
| Gateway 接口膨胀 | 按需定义，避免过度设计 |
| Domain 依赖 infrastructure | 通过 Gateway 依赖倒置，Domain 不直接依赖 |
| 单一模块代码冲突 | 合理分包，按业务能力隔离 |
| 测试难度 | Domain 可 Mock Gateway 独立测试 |

---

## 9. COLA Light 5.0 关键决策

| 决策项 | 选择 |
|--------|------|
| Maven 模块数 | 单一模块 `gateway-boot` |
| 包名 | `com.codingas.gateway` |
| Gateway 接口放置 | `domain/xxx/gateway/` |
| Gateway 实现放置 | `infrastructure/xxx/gateway/` |
| Repository | 简化方案，Gateway 内部使用 JpaRepository |
| Domain Service | 放 `domain/xxx/service/` |
| Application Service | 放 `application/xxx/`（按用例分包） |
| Controller/DTO | 放 `adapter/xxx/`（按用例分包） |
| Exception | 基础放 common，领域放 domain，技术放 infrastructure |
| Configuration | 放 `infrastructure/config/` |
| 枚举 | 业务枚举放 domain，技术常量放 common |

## 10. 总结

**核心原则：**
- **Gateway 模式**：接口定义在 domain/xxx/gateway/，实现 in infrastructure/xxx/gateway/
- **依赖倒置**：Domain 只知道 Gateway 接口，不知道实现
- **业务内聚**：domain/xxx 按业务能力组织，Entity + Service + Gateway 在一起
- **应用服务编排**：跨域协作在 application 层（按用例分包）
- **旁路事件驱动**：统计、审计用事件异步处理

**架构优势：**
- 依赖方向清晰：Domain → Gateway → Infrastructure
- 可测试性高：Domain 可 Mock Gateway 独立测试
- 可扩展性强：新的外部依赖只需添加 Gateway 实现
- 业务内聚：相关代码组织在一起
- 单一模块：便于维护和团队协作
