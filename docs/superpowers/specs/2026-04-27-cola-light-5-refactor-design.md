# COLA Light 5.0 重构设计

## 概述

本设计文档定义 LLM-Gateway 项目从当前多层模块架构向 COLA Light 5.0 轻量级分层架构的重构方案。

COLA Light 5.0 核心思想：**单模块架构，用 package 代替模块划分层次**。

---

## 目标架构

### 项目结构

```
gateway-boot/                          # Maven 单一模块
├── pom.xml
└── src/main/java/com/codingas/gateway/
    ├── adapter/                       # 适配器层
    │   ├── auth/                     # 认证用例
    │   │   ├── controller/
    │   │   └── dto/
    │   ├── chat/                     # 聊天用例
    │   │   ├── controller/
    │   │   └── dto/
    │   ├── model/                    # 模型管理用例
    │   │   ├── controller/
    │   │   └── dto/
    │   └── admin/                    # 管理用例
    │       ├── controller/
    │       └── dto/
    │
    ├── application/                   # 应用层
    │   ├── auth/
    │   ├── chat/
    │   └── model/
    │
    ├── domain/                        # 领域层
    │   ├── gateway/                   # 跨领域 Gateway 接口
    │   ├── security/
    │   │   ├── entity/
    │   │   ├── service/
    │   │   ├── gateway/
    │   │   ├── enums/
    │   │   └── exception/
    │   ├── router/
    │   │   ├── entity/
    │   │   ├── service/
    │   │   ├── gateway/
    │   │   ├── enums/
    │   │   └── exception/
    │   └── analytics/
    │       ├── entity/
    │       ├── service/
    │       ├── gateway/
    │       ├── enums/
    │       └── exception/
    │
    ├── infrastructure/                # 基础设施层
    │   ├── config/
    │   ├── gateway/
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

---

## 各层职责

### 1. Adapter 层（适配器层）

**职责：** 外部请求入口，接口适配与输入输出转换

**包含组件：**
- Controller：接收 HTTP 请求
- DTO：Request/Response 数据传输对象

**分包规则：** 按业务用例分包，DTO 跟随 Controller

```
adapter/
├── auth/controller/AuthController.java
├── auth/dto/request/
├── auth/dto/response/
├── chat/controller/ChatController.java
├── chat/dto/
└── ...
```

### 2. Application 层（应用层）

**职责：** 用例编排，跨领域协调，不含业务逻辑

**包含组件：**
- Application Service：用例编排服务

**分包规则：** 按业务用例分包

```
application/
├── auth/AuthApplication.java
├── chat/ChatApplication.java
└── model/ModelApplication.java
```

### 3. Domain 层（领域层）

**职责：** 业务逻辑核心，包含模型、业务规则、Gateway 接口定义

**包含组件：**
- Entity：实体
- Domain Service：管理服务（业务逻辑）
- Gateway 接口：通往外部世界的门（接口定义）
- 领域异常：业务规则违反
- 枚举：业务概念表达

**分包规则：** 按业务领域分包

```
domain/
├── security/
│   ├── entity/User.java, GatewayApiKey.java
│   ├── service/AuthenticationService.java, RateLimitService.java
│   ├── gateway/ApiKeyGateway.java, AuditGateway.java
│   ├── enums/
│   └── exception/AuthenticationException.java
├── router/
│   ├── entity/Model.java, Provider.java
│   ├── service/
│   ├── gateway/ModelGateway.java, ProviderGateway.java
│   ├── enums/
│   └── exception/
└── analytics/
    ├── entity/TokenLimit.java
    ├── service/
    ├── gateway/
    ├── enums/
    └── exception/
```

### 4. Infrastructure 层（基础设施层）

**职责：** 技术实现细节，Gateway 接口实现、配置、工具类

**包含组件：**
- Gateway 实现：JpaXxxGateway
- Configuration：技术配置
- Util：工具类

**分包规则：** 按领域分包

```
infrastructure/
├── config/SaTokenConfig.java, RedisConfig.java
├── gateway/
│   ├── security/JpaApiKeyGateway.java, JpaAuditGateway.java
│   ├── router/JpaModelGateway.java, JpaProviderGateway.java
│   └── analytics/
└── util/
```

### 5. Common 层（公共组件）

**职责：** 跨领域共享的类型和工具

**包含组件：**
- Exception：基础异常（GatewayException 根异常）
- Constants：技术常量
- Util：通用工具类

---

## 关键设计决策

### 1. Maven 模块结构

**决策：** 单一 Maven 模块 `gateway-boot`

**原因：** COLA Light 5.0 核心思想是用 package 代替模块划分

### 2. Gateway 模式

**接口定义位置：** `domain/xxx/gateway/`
**实现位置：** `infrastructure/xxx/gateway/`

**依赖关系：**
```
Domain 层定义 Gateway 接口（抽象）
         ↓
Infrastructure 层实现 Gateway 接口（具体）
         ↓
通过依赖注入解耦
```

### 3. Repository 处理

**决策：** 简化方案，不单独定义 Repository 接口

**实现：** JpaXxxGateway 内部直接使用 Spring Data JpaRepository

```
infrastructure/gateway/
└── JpaModelGateway.java
    @Repository
    public interface JpaModelRepository extends JpaRepository<Model, Long> {}
```

### 4. 服务分类

**Domain Service（放 domain/xxx/service/）：**
- AuthenticationService：认证领域逻辑
- RateLimitService：限流领域逻辑
- RbacService：权限领域逻辑
- ModelRouterService：路由领域逻辑

**Application Service（放 application/xxx/）：**
- AuthApplication：认证用例编排
- ChatApplication：聊天用例编排
- ModelApplication：模型管理用例编排

### 5. Exception 分类

| 类型 | 放置位置 | 示例 |
|------|---------|------|
| 基础异常 | common/exception/ | GatewayException |
| 领域异常 | domain/xxx/exception/ | AuthenticationException |
| 基础设施异常 | infrastructure/ | ProviderException |

### 6. Configuration 分类

**决策：** 统一放 `infrastructure/config/`

包括：SaToken 配置、Redis 配置、OpenTelemetry 配置等。

### 7. 枚举与常量

| 类型 | 放置位置 | 示例 |
|------|---------|------|
| 业务枚举 | domain/xxx/enums/ | ProviderStatus, KeyStatus |
| 技术常量 | common/constants/ | HTTP 状态码 |
| 业务常量 | 直接写在领域类中 | - |

---

## 重构映射表

### 当前模块 → 新包结构

| 当前模块 | 迁移目标 |
|---------|---------|
| gateway-common | common/ |
| gateway-adapter | adapter/ |
| gateway-application | application/ |
| gateway-core/domain/* | domain/xxx/ |
| gateway-core/service/* | 分析后分类 |
| gateway-security | domain/security/ + infrastructure/security/gateway/ |
| gateway-router | domain/router/ + infrastructure/router/gateway/ |
| gateway-infrastructure | infrastructure/ |

### Entity 迁移

| 当前 Entity | 新位置 |
|------------|--------|
| User | domain/security/entity/ |
| GatewayApiKey | domain/security/entity/ |
| Model | domain/router/entity/ |
| Provider | domain/router/entity/ |
| TokenLimit | domain/analytics/entity/ |

---

## 重构步骤（概要）

1. 创建单一 Maven 模块 `gateway-boot`
2. 建立 COLA 分层包结构
3. 迁移 Entity 到 domain/xxx/entity/
4. 迁移 Domain Service 到 domain/xxx/service/
5. 迁移 Gateway 接口到 domain/xxx/gateway/
6. 迁移 Gateway 实现到 infrastructure/xxx/gateway/
7. 创建 Application Service 到 application/xxx/
8. 迁移 Controller 和 DTO 到 adapter/xxx/
9. 整理 Exception 到各层
10. 清理无用代码和重复类

---

## 验证标准

- 所有代码按 COLA 分层原则放置
- 领域层无外部依赖（仅依赖 Gateway 接口）
- 基础设施层实现 Gateway 接口
- 应用层编排管理服务
- 无循环依赖

---

**版本：** 1.0.0
**创建日期：** 2026-04-27
**状态：** 待用户评审
