# LLM-Gateway 职责拆分架构设计

> **版本**: 1.0.0
> **状态**: 草案
> **创建日期**: 2026-04-26

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

按**业务领域内聚**原则重构：
- Entity + 领域服务 放在同一模块
- 跨域协作通过**应用服务层**编排
- 旁路操作通过**领域事件**解耦

---

## 2. 目标架构

### 2.1 分层结构

```
gateway-api                  → Controller + 业务 DTO
           ↓
gateway-app-service          → 应用服务（用例编排）
           ↓
    ┌───────┼───────┬──────────┐
    ↓       ↓       ↓          ↓
security  router  analytics  adapter
    ↓       ↓       ↓          ↓
infrastructure           common
```

### 2.2 模块职责

| 模块 | 职责 | 包含内容 |
|------|------|---------|
| **gateway-api** | HTTP 请求接收，返回响应 | Controller、业务 DTO |
| **gateway-app-service** | 用例编排 | 应用服务，依赖各领域服务接口 |
| **gateway-security** | Entity + 领域服务 | GatewayApiKey、IpBlocklist + Service |
| **gateway-router** | Entity + 领域服务 | Model、Provider、RouteGroup + Service |
| **gateway-analytics** | Entity + 领域服务 | TokenUsage、AuditLog + Service |
| **gateway-adapter** | Entity + 领域服务 | ProviderApiKey、Credentials + Service |
| **gateway-infrastructure** | 基础设施 | BaseEntity、通用工具 |
| **gateway-common** | 纯共享类型 | 通用异常、分页工具（无业务语义） |

### 2.3 依赖方向

```
gateway-api
      ↓
gateway-app-service
      ↓
   ┌──┼──┬──┐
   ↓  ↓  ↓  ↓
security router analytics adapter
   ↓  ↓  ↓  ↓
   infrastructure + common
```

---

## 3. Entity 归属

| 领域 | Entity | 说明 |
|------|--------|------|
| **security** | GatewayApiKey | API 密钥 |
| **security** | IpBlocklist | IP 黑名单 |
| **router** | Model | 模型 |
| **router** | Provider | 提供商 |
| **router** | RouteGroup | 路由组 |
| **router** | RouteGroupProvider | 路由组-提供商关联 |
| **analytics** | TokenUsage | Token 使用记录 |
| **analytics** | AuditLog | 审计日志 |
| **analytics** | TokenLimit | Token 限额 |
| **adapter** | ProviderApiKey | 提供商 API 密钥 |
| **adapter** | Credentials | 凭证（加密存储） |

---

## 4. 服务接口定义

### 4.1 接口定义原则

**接口定义在调用方**，由被调用方实现。

| 调用方向 | 接口定义位置 | 实现位置 |
|---------|-------------|---------|
| API → 应用服务 | gateway-api | gateway-app-service |
| 应用服务 → 安全 | gateway-app-service | gateway-security |
| 应用服务 → 路由 | gateway-app-service | gateway-router |
| 应用服务 → 统计 | gateway-app-service | gateway-analytics |
| 应用服务 → 适配器 | gateway-app-service | gateway-adapter |

### 4.2 安全服务接口

```java
// gateway-app-service 定义
public interface ApiKeyAuthService {
    AuthResult validate(String apiKey, String clientIp);
    ApiKeyInfo getApiKeyInfo(String apiKey);
}

// gateway-app-service 定义
public interface IpBlockService {
    boolean isBlocked(String clientIp);
    void blockIp(String clientIp, String reason, Long operatorId);
}
```

### 4.3 路由服务接口

```java
// gateway-app-service 定义
public interface ModelRouterService {
    ModelInfo selectModel(String modelCode, Long teamId, RoutingStrategy strategy);
    List<ModelInfo> listAvailableModels(Long teamId);
}

// gateway-app-service 定义
public interface RouteGroupService {
    RouteGroupInfo getRouteGroup(String groupCode);
    List<RouteGroupInfo> listRouteGroups();
}
```

### 4.4 统计服务接口

```java
// gateway-app-service 定义
public interface TokenTrackingService {
    void recordUsage(Long teamId, String modelCode, TokenUsage usage);
    UsageSummary getUsageSummary(Long teamId, String modelCode);
}

// gateway-app-service 定义
public interface AuditService {
    void log(String action, Long operatorId, Object details);
}
```

### 4.5 适配器服务接口

```java
// gateway-app-service 定义
public interface CredentialService {
    ProviderCredential getCredential(String providerCode);
}
```

---

## 5. 跨域访问规则

| 方式 | 场景 | 是否允许 |
|------|------|---------|
| **应用服务编排** | 主流程（认证→路由→调用） | ✅ 正确做法 |
| **领域事件** | 旁路（统计、审计） | ✅ 正确做法 |
| **领域服务直接调用** | 主流程中 | ❌ 禁止 |

### 5.1 主流程：应用服务编排

```java
@Service
@RequiredArgsConstructor
public class LLMChatService {
    
    private final ApiKeyAuthService apiKeyAuthService;
    private final ModelRouterService modelRouterService;
    private final CredentialService credentialService;
    private final LLMProviderAdapter adapter;
    private final ApplicationEventPublisher eventPublisher;
    
    public LLMResponse chat(ChatRequest request) {
        // 1. 认证
        var auth = apiKeyAuthService.validate(request.getApiKey(), request.getClientIp());
        
        // 2. 路由
        var model = modelRouterService.selectModel(
            request.getModel(), 
            auth.getTeamId(), 
            RoutingStrategy.COST_OPTIMIZED
        );
        
        // 3. 获取凭证
        var credential = credentialService.getCredential(model.getProviderCode());
        
        // 4. LLM 调用
        var response = adapter.chat(model, credential, request);
        
        // 5. 发布事件
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

### 5.2 旁路操作：领域事件

```java
// 事件定义
public record TokenUsedEvent(
    Long teamId,
    String modelCode,
    int inputTokens,
    int outputTokens,
    Instant timestamp
) {}

// TokenTrackingService 订阅
@Service
public class TokenTrackingServiceImpl implements TokenTrackingService {
    
    @EventListener
    public void onTokenUsed(TokenUsedEvent event) {
        tokenUsageRepository.save(TokenUsage.builder()
            .teamId(event.teamId())
            .modelCode(event.modelCode())
            .inputTokens(event.inputTokens())
            .outputTokens(event.outputTokens())
            .timestamp(event.timestamp())
            .build());
    }
}

// AuditService 订阅
@Service
public class AuditServiceImpl implements AuditService {
    
    @EventListener
    public void onTokenUsed(TokenUsedEvent event) {
        auditLogRepository.save(AuditLog.builder()
            .action("TOKEN_USED")
            .teamId(event.teamId())
            .details(event)
            .build());
    }
}
```

---

## 6. 业务事件体系

### 6.1 核心事件

| 事件 | 发布时机 | 订阅者 |
|------|---------|--------|
| ApiKeyValidatedEvent | 认证成功时 | AuditService |
| ModelSelectedEvent | 路由完成时 | AuditService |
| TokenUsedEvent | LLM 调用完成时 | TokenTrackingService, AuditService |
| BudgetExceededEvent | 超出预算时 | NotificationService |
| RateLimitExceededEvent | 触发限流时 | AuditService |

### 6.2 事件处理方式

| 场景 | 处理方式 | 示例 |
|------|---------|------|
| **同步处理** | @TransactionalEventListener | 预算校验（失败要回滚） |
| **异步处理** | @Async + @EventListener | 审计日志、统计 |
| **重试处理** | 死信队列 | 外部系统调用 |

---

## 7. 请求流程

```
HTTP 请求
    ↓
Controller → 应用服务（gateway-app-service）
    ↓ 同步编排
安全认证 → 模型路由 → 凭证获取 → LLM调用
    ↓
发布事件（异步）
    ↓
┌─────────┴─────────┐
↓                   ↓
TokenUsedEvent     AuditLogEvent
↓                   ↓
统计服务（异步）    审计服务（异步）
↓                   ↓
    HTTP 响应
```

---

## 8. 模块创建计划

### 阶段 1：基础设施层

| 任务 | 说明 |
|------|------|
| 创建 gateway-infrastructure | BaseEntity、通用工具 |
| 创建 gateway-common（清理） | 移除业务语义，只保留纯通用组件 |

### 阶段 2：业务领域模块

| 任务 | 说明 |
|------|------|
| 创建 gateway-analytics | TokenUsage、AuditLog、TokenLimit Entity |
| 创建 gateway-app-service | 应用服务层 |

### 阶段 3：Entity 迁移

| 任务 | 说明 |
|------|------|
| 迁移 security Entity | GatewayApiKey、IpBlocklist |
| 迁移 router Entity | Model、Provider、RouteGroup |
| 迁移 analytics Entity | TokenUsage、AuditLog、TokenLimit |
| 迁移 adapter Entity | ProviderApiKey、Credentials |

### 阶段 4：服务接口与实现

| 任务 | 说明 |
|------|------|
| 定义服务接口 | 按调用方向定义接口 |
| 实现领域服务 | 在各业务模块实现 |
| 实现应用服务 | 编排用例 |

### 阶段 5：领域事件

| 任务 | 说明 |
|------|------|
| 定义事件体系 | 核心业务事件 |
| 实现事件订阅 | 各服务的事件处理 |

---

## 9. 风险与注意事项

| 风险 | 应对措施 |
|------|---------|
| Entity 迁移影响大 | 同步更新所有引用，先小范围试点 |
| 接口定义要稳 | 接口一旦发布不轻易改动，通过新接口扩展 |
| 事件机制先设计 | 避免后期改来改去 |
| 测试先行 | 每迁移一个 Entity，保证相关测试通过 |
| 编译验证 | 每个阶段编译通过后再进入下一阶段 |

---

## 10. 总结

**核心原则：**
- 领域服务内聚：Entity + Service 在同一模块
- Entity 不泄漏：只通过接口操作
- 应用服务编排：跨域协作在应用服务层
- 旁路事件驱动：统计、审计用事件

**架构优势：**
- 分层清晰，符合 DDD 思想
- 模块独立，可单独测试和修改
- 易于演进，新功能只需添加新模块
