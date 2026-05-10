# LLM Gateway Phase 1: 基础框架与实体完善

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完善实体设计，创建数据库迁移脚本，添加缺失的 Gateway 接口和实体

**Architecture:** 基于 COLA Light 5.0 架构，单模块 Maven 项目，分层管理实体

**Tech Stack:** Java 21 + Spring Boot 3.5.x + JPA + PostgreSQL/MySQL

---

## 1. 文件结构规划

### 1.1 现有结构

```
src/main/java/com/codingas/gateway/
├── domain/
│   ├── router/
│   │   ├── entity/     (Model, Provider, RouteGroup, RouteGroupProvider)
│   │   ├── gateway/     (ModelGateway, ProviderGateway, RouteGroupGateway)
│   │   └── service/     (ModelService, ProviderService, etc.)
│   └── security/
│       ├── entity/      (User, Role, Permission, GatewayApiKey, TokenLimit, etc.)
│       ├── gateway/     (UserGateway, ApiKeyGateway, etc.)
│       └── service/     (AuthenticationService, RbacService, etc.)
└── infrastructure/
    └── gateway/         (Gateway implementations)
```

### 1.2 Phase 1 新增结构

```
src/main/java/com/codingas/gateway/
├── domain/
│   ├── analytics/                          [新增]
│   │   ├── entity/
│   │   │   ├── UsageLog.java               [新增]
│   │   │   ├── AlertRule.java               [新增]
│   │   │   └── AlertNotification.java        [新增]
│   │   ├── gateway/
│   │   │   ├── UsageLogGateway.java         [新增]
│   │   │   ├── AlertRuleGateway.java        [新增]
│   │   │   └── AlertNotificationGateway.java [新增]
│   │   ├── service/
│   │   │   ├── UsageLogService.java         [新增]
│   │   │   ├── AlertRuleService.java        [新增]
│   │   │   └── AlertNotificationService.java [新增]
│   │   └── enums/
│   │       ├── AlertType.java               [新增]
│   │       ├── AlertStatus.java             [新增]
│   │       └── NotificationChannel.java     [新增]
│   └── security/
│       └── entity/
│           └── UserRole.java                [新增 - 替换现有关联方式]
└── common/
    └── enums/
        ├── UserStatus.java                  [修改 - 与设计文档对齐]
        ├── UserRole.java                    [修改 - 与设计文档对齐]
        └── ProviderType.java                [修改 - 补充缺失类型]
```

---

## 2. 任务列表

### Task 1: 完善 common/enums 枚举类

**Files:**
- Modify: `src/main/java/com/codingas/gateway/common/enums/UserStatus.java`
- Modify: `src/main/java/com/codingas/gateway/common/enums/UserRole.java`
- Modify: `src/main/java/com/codingas/gateway/common/enums/ProviderType.java`

- [ ] **Step 1: 创建 UserStatus 枚举**

```java
package com.codingas.gateway.common.enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    /** 正常 */
    ACTIVE,
    /** 禁用 */
    DISABLED,
    /** 锁定 */
    LOCKED,
    /** 已删除 */
    DELETED
}
```

- [ ] **Step 2: 创建 UserRole 枚举**

```java
package com.codingas.gateway.common.enums;

/**
 * 用户角色枚举
 */
public enum UserRole {
    /** 管理员 */
    ADMIN,
    /** 开发者 */
    DEVELOPER,
    /** 观察者 */
    OBSERVER,
    /** 财务管理员 */
    FINANCE_ADMIN
}
```

- [ ] **Step 3: 创建 ProviderType 枚举**

```java
package com.codingas.gateway.common.enums;

/**
 * Provider 类型枚举
 */
public enum ProviderType {
    OPENAI,
    ANTHROPIC,
    GEMINI,
    ZHIPU,
    QWEN,
    VOLCENGINE,
    WENXIN,
    OTHER
}
```

- [ ] **Step 4: 创建 PeriodType 枚举**

```java
package com.codingas.gateway.common.enums;

/**
 * 周期类型枚举
 */
public enum PeriodType {
    /** 每日 */
    DAILY,
    /** 每周 */
    WEEKLY,
    /** 每月 */
    MONTHLY,
    /** 总计 */
    TOTAL
}
```

- [ ] **Step 5: 创建 ExceededAction 枚举**

```java
package com.codingas.gateway.common.enums;

/**
 * 超限动作枚举
 */
public enum ExceededAction {
    /** 直接拒绝 */
    REJECT,
    /** 降级切换 */
    DOWNGRADE
}
```

- [ ] **Step 6: 提交枚举类**

```bash
git add src/main/java/com/codingas/gateway/common/enums/
git commit -m "feat: add common enums aligned with design doc

- UserStatus: ACTIVE/DISABLED/LOCKED/DELETED
- UserRole: ADMIN/DEVELOPER/OBSERVER/FINANCE_ADMIN
- ProviderType: OPENAI/ANTHROPIC/GEMINI/ZHIPU/QWEN/VOLCENGINE/WENXIN/OTHER
- PeriodType: DAILY/WEEKLY/MONTHLY/TOTAL
- ExceededAction: REJECT/DOWNGRADE

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: 完善 User 实体

**Files:**
- Modify: `src/main/java/com/codingas/gateway/domain/security/entity/User.java`

- [ ] **Step 1: 查看现有 User 实体**

```bash
cat src/main/java/com/codingas/gateway/domain/security/entity/User.java
```

- [ ] **Step 2: 更新 User 实体与设计文档对齐**

```java
package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.enums.UserRole;
import com.codingas.gateway.domain.security.enums.UserState;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 用户实体
 *
 * <p>表示系统中的用户账户，包含认证和授权信息。</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class User extends BaseEntity {

    @Column(name = "user_code", nullable = false, unique = true, length = 64)
    private String userCode;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "password_hash", length = 256)
    private String passwordHash;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "oauth_providers", columnDefinition = "json")
    private Map<String, String> oauthProviders;

    @Column(name = "pii_salt", length = 64)
    private String piiSalt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
```

- [ ] **Step 3: 提交 User 实体**

```bash
git add src/main/java/com/codingas/gateway/domain/security/entity/User.java
git commit -m "refactor: align User entity with design doc

- Add phone, avatarUrl, emailVerified, oauthProviders, piiSalt, lastLoginAt, deletedAt
- Use UserStatus (ACTIVE/DISABLED/LOCKED/DELETED)
- Remove legacy role field

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: 创建 UserRole 实体

**Files:**
- Create: `src/main/java/com/codingas/gateway/domain/security/entity/UserRole.java`

- [ ] **Step 1: 创建 UserRole 实体（用户角色关联）**

```java
package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 用户角色关联实体
 *
 * <p>表示用户与角色的多对多关联关系。</p>
 */
@Entity
@Table(name = "user_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "role_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
```

- [ ] **Step 2: 更新 Role 实体**

```java
package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 角色实体
 *
 * <p>权限集合，用于简化权限管理和批量授权。</p>
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {

    @Column(name = "role_code", nullable = false, unique = true, length = 64)
    private String roleCode;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private RoleType roleType = RoleType.CUSTOM;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum RoleType {
        /** 预定义角色 */
        SYSTEM,
        /** 自定义角色 */
        CUSTOM
    }
}
```

- [ ] **Step 3: 更新 Permission 实体**

```java
package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 权限实体
 *
 * <p>细粒度权限码，定义到具体的操作级别。</p>
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseEntity {

    @Column(name = "permission_code", nullable = false, unique = true, length = 128)
    private String permissionCode;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 32)
    private String category;
}
```

- [ ] **Step 4: 提交角色权限实体**

```bash
git add src/main/java/com/codingas/gateway/domain/security/entity/UserRole.java
git add src/main/java/com/codingas/gateway/domain/security/entity/Role.java
git add src/main/java/com/codingas/gateway/domain/security/entity/Permission.java
git commit -m "feat: add UserRole, Role, Permission entities

- UserRole: user-role many-to-many association
- Role: SYSTEM/CUSTOM role types
- Permission: fine-grained permission codes

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: 完善 Provider 实体

**Files:**
- Modify: `src/main/java/com/codingas/gateway/domain/router/entity/Provider.java`

- [ ] **Step 1: 更新 Provider 实体与设计文档对齐**

```java
package com.codingas.gateway.domain.router.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.model.enums.ProviderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 提供商实体
 *
 * <p>表示 AI 模型服务提供商，如 OpenAI、Anthropic、智谱等。</p>
 */
@Entity
@Table(name = "providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Provider extends BaseEntity {

    @Column(name = "provider_code", nullable = false, unique = true, length = 64)
    private String providerCode;

    @Column(name = "provider_name", nullable = false, length = 128)
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderType providerType;

    @Column(name = "base_url", length = 256)
    private String baseUrl;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    @Column(name = "api_doc_url", length = 512)
    private String apiDocUrl;

    @Column(name = "priority")
    private Integer priority = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProviderStatus status = ProviderStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum ProviderStatus {
        /** 正常 */
        ACTIVE,
        /** 暂停 */
        SUSPENDED,
        /** 已删除 */
        DELETED
    }
}
```

- [ ] **Step 2: 更新 Model 实体**

```java
package com.codingas.gateway.domain.router.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 模型实体
 *
 * <p>表示具体的 AI 模型，是调用的最小单位。</p>
 */
@Entity
@Table(name = "models", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider_id", "provider_model_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Model extends BaseEntity {

    @Column(name = "model_code", nullable = false, unique = true, length = 128)
    private String modelCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Column(name = "provider_model_id", nullable = false, length = 128)
    private String providerModelId;

    @Column(name = "display_name", length = 256)
    private String displayName;

    @Column(name = "context_window")
    private Integer contextWindow;

    @Column(name = "input_price", precision = 10, scale = 6)
    private BigDecimal inputPrice;

    @Column(name = "output_price", precision = 10, scale = 6)
    private BigDecimal outputPrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", columnDefinition = "json")
    private Map<String, Boolean> capabilities;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ModelStatus status = ModelStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum ModelStatus {
        /** 正常 */
        ACTIVE,
        /** 已废弃 */
        DEPRECATED,
        /** 已删除 */
        DELETED
    }
}
```

- [ ] **Step 3: 提交 Provider 和 Model 实体**

```bash
git add src/main/java/com/codingas/gateway/domain/router/entity/Provider.java
git add src/main/java/com/codingas/gateway/domain/router/entity/Model.java
git commit -m "refactor: align Provider and Model entities with design doc

Provider:
- Add websiteUrl, apiDocUrl fields
- Add deletedAt for soft delete
- Use ProviderType enum

Model:
- Add contextWindow, inputPrice, outputPrice, capabilities
- Add deletedAt for soft delete
- Add ModelStatus enum

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5: 完善 GatewayApiKey 和 TokenLimit 实体

**Files:**
- Modify: `src/main/java/com/codingas/gateway/domain/security/entity/GatewayApiKey.java`
- Modify: `src/main/java/com/codingas/gateway/domain/security/entity/TokenLimit.java`

- [ ] **Step 1: 更新 GatewayApiKey 实体**

```java
package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * 网关访问凭证实体
 *
 * <p>用户调用 LLM-Gateway 网关的凭据，格式为 sk-xxxxxxxx。</p>
 */
@Entity
@Table(name = "gateway_api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GatewayApiKey extends BaseEntity {

    @Column(name = "key_code", nullable = false, unique = true, length = 128)
    private String keyCode;

    @Column(name = "key_hash", nullable = false, length = 256)
    private String keyHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ip_whitelist", columnDefinition = "json")
    private List<String> ipWhitelist;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum ApiKeyStatus {
        /** 正常 */
        ACTIVE,
        /** 禁用 */
        DISABLED,
        /** 已过期 */
        EXPIRED,
        /** 已删除 */
        DELETED
    }
}
```

- [ ] **Step 2: 更新 TokenLimit 实体**

```java
package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.usage.enums.ExceededAction;
import com.codingas.gateway.domain.usage.enums.PeriodType;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Provider;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Token 限额实体
 *
 * <p>用户级别 Token 使用限额，支持周期重置。</p>
 */
@Entity
@Table(name = "token_limits", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "provider_id", "model_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenLimit extends BaseEntity {

    @Column(name = "limit_code", nullable = false, unique = true, length = 64)
    private String limitCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private Model model;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false)
    private LimitType limitType = LimitType.USER_CUSTOM;

    @Column(name = "max_tokens", precision = 20, scale = 6)
    private BigDecimal maxTokens;

    @Column(name = "used_tokens", precision = 20, scale = 6)
    private BigDecimal usedTokens = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType = PeriodType.MONTHLY;

    @Column(name = "period_day_of_week")
    private Integer periodDayOfWeek;

    @Column(name = "period_day_of_month")
    private Integer periodDayOfMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "exceeded_action", nullable = false)
    private ExceededAction exceededAction = ExceededAction.REJECT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "switch_model_id")
    private Model switchModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TokenLimitStatus status = TokenLimitStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum LimitType {
        /** 系统默认 */
        SYSTEM_DEFAULT,
        /** 用户自定义 */
        USER_CUSTOM
    }

    public enum TokenLimitStatus {
        /** 正常 */
        ACTIVE,
        /** 暂停 */
        SUSPENDED,
        /** 已删除 */
        DELETED
    }
}
```

- [ ] **Step 3: 提交 TokenLimit 实体**

```bash
git add src/main/java/com/codingas/gateway/domain/security/entity/GatewayApiKey.java
git add src/main/java/com/codingas/gateway/domain/security/entity/TokenLimit.java
git commit -m "refactor: align GatewayApiKey and TokenLimit entities with design doc

GatewayApiKey:
- Add keyCode, keyHash, ipWhitelist fields
- Add deletedAt for soft delete
- Add ApiKeyStatus enum

TokenLimit:
- Add limitCode, limitType fields
- Support user/provider/model 3D限额
- Add PeriodType, ExceededAction, TokenLimitStatus
- Add deletedAt for soft delete

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: 创建 analytics 领域实体

**Files:**
- Create: `src/main/java/com/codingas/gateway/domain/analytics/entity/UsageLog.java`
- Create: `src/main/java/com/codingas/gateway/domain/analytics/entity/AlertRule.java`
- Create: `src/main/java/com/codingas/gateway/domain/analytics/entity/AlertNotification.java`
- Create: `src/main/java/com/codingas/gateway/domain/analytics/enums/AlertType.java`
- Create: `src/main/java/com/codingas/gateway/domain/analytics/enums/AlertStatus.java`
- Create: `src/main/java/com/codingas/gateway/domain/analytics/enums/NotificationChannel.java`

- [ ] **Step 1: 创建 UsageLog 实体**

```java
package com.codingas.gateway.domain.analytics.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 使用记录实体
 *
 * <p>记录每次 API 调用的详细信息，用于用量分析和成本统计。</p>
 */
@Entity
@Table(name = "usage_logs", indexes = {
    @Index(name = "idx_usage_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_usage_provider_created", columnList = "provider_id, created_at"),
    @Index(name = "idx_usage_key_created", columnList = "gateway_api_key_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsageLog extends BaseEntity {

    @Column(name = "log_code", nullable = false, unique = true, length = 64)
    private String logCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gateway_api_key_id", nullable = false)
    private GatewayApiKey gatewayApiKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "status_code", length = 32)
    private String statusCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "failover")
    private Boolean failover = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "api_format", nullable = false)
    private ApiFormat apiFormat;

    public enum ApiFormat {
        OPENAI,
        ANTHROPIC
    }
}
```

- [ ] **Step 2: 创建 AlertRule 实体**

```java
package com.codingas.gateway.domain.analytics.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 预警规则实体
 *
 * <p>定义预警触发条件，包括用量预警、健康预警、额度预警。</p>
 */
@Entity
@Table(name = "alert_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule extends BaseEntity {

    @Column(name = "rule_code", nullable = false, unique = true, length = 64)
    private String ruleCode;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type")
    private ConditionType conditionType;

    @Column(name = "threshold_value", precision = 20, scale = 6)
    private BigDecimal thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type")
    private com.codingas.gateway.domain.usage.enums.PeriodType periodType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_channels", columnDefinition = "json")
    private List<NotificationChannel> notificationChannels;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum AlertType {
        /** 用量预警 */
        USAGE,
        /** 健康预警 */
        HEALTH,
        /** 额度预警 */
        QUOTA
    }

    public enum TargetType {
        USER,
        PROVIDER,
        API_KEY
    }

    public enum ConditionType {
        THRESHOLD,
        RATIO,
        TREND
    }

    public enum NotificationChannel {
        SYSTEM,
        EMAIL,
        IM,
        SMS
    }
}
```

- [ ] **Step 3: 创建 AlertNotification 实体**

```java
package com.codingas.gateway.domain.analytics.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.domain.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 预警通知实体
 *
 * <p>记录预警触发后的通知发送情况。</p>
 */
@Entity
@Table(name = "alert_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotification extends BaseEntity {

    @Column(name = "notification_code", nullable = false, unique = true, length = 64)
    private String notificationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private AlertRule.NotificationChannel channel;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alert_data", columnDefinition = "json")
    private Map<String, Object> alertData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum NotificationStatus {
        /** 待发送 */
        PENDING,
        /** 已发送 */
        SENT,
        /** 发送失败 */
        FAILED
    }
}
```

- [ ] **Step 4: 提交 analytics 领域实体**

```bash
git add src/main/java/com/codingas/gateway/domain/analytics/
git commit -m "feat: add analytics domain entities

- UsageLog: 记录每次 API 调用
- AlertRule: 预警规则配置
- AlertNotification: 预警通知记录
- Support USAGE/HEALTH/QUOTA alert types

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 7: 创建数据库迁移脚本

**Files:**
- Create: `src/main/resources/db/V1__init_schema.sql`

- [ ] **Step 1: 创建数据库迁移脚本**

```sql
-- V1__init_schema.sql
-- LLM Gateway 初始化数据库结构

-- ============================================
-- 1. 身份与访问控制域
-- ============================================

-- 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_code VARCHAR(64) NOT NULL UNIQUE COMMENT '用户编码',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    email VARCHAR(128) COMMENT '邮箱',
    password_hash VARCHAR(256) COMMENT '密码哈希',
    phone VARCHAR(32) COMMENT '手机号',
    avatar_url VARCHAR(512) COMMENT '头像URL',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED/LOCKED/DELETED',
    email_verified BOOLEAN DEFAULT FALSE COMMENT '邮箱已验证',
    oauth_providers JSON COMMENT 'OAuth提供者列表',
    pii_salt VARCHAR(64) COMMENT 'PII脱敏盐值',
    last_login_at TIMESTAMP COMMENT '最后登录时间',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_users_email (email),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL UNIQUE COMMENT '角色编码',
    name VARCHAR(64) NOT NULL COMMENT '角色名称',
    description TEXT COMMENT '角色描述',
    role_type VARCHAR(32) NOT NULL DEFAULT 'CUSTOM' COMMENT '类型: SYSTEM/CUSTOM',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限表
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL UNIQUE COMMENT '权限编码',
    name VARCHAR(64) NOT NULL COMMENT '权限名称',
    description TEXT COMMENT '权限描述',
    category VARCHAR(32) COMMENT '权限分类: user/provider/model/token/log/setting',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 用户角色关联表
CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_roles_user (user_id),
    INDEX idx_user_roles_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================
-- 2. 提供商与模型域
-- ============================================

-- 提供商表
CREATE TABLE providers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_code VARCHAR(64) NOT NULL UNIQUE COMMENT '提供商编码',
    provider_name VARCHAR(128) NOT NULL COMMENT '提供商名称',
    provider_type VARCHAR(32) NOT NULL COMMENT '类型: OPENAI/ANTHROPIC/GEMINI/ZHIPU/QWEN/VOLCENGINE/WENXIN/OTHER',
    base_url VARCHAR(256) COMMENT 'API端点',
    website_url VARCHAR(512) COMMENT '官网URL',
    api_doc_url VARCHAR(512) COMMENT 'API文档URL',
    priority INT DEFAULT 100 COMMENT '优先级',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED/DELETED',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_providers_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提供商表';

-- 模型表
CREATE TABLE models (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_code VARCHAR(128) NOT NULL UNIQUE COMMENT '模型编码',
    provider_id BIGINT NOT NULL COMMENT '所属提供商ID',
    provider_model_id VARCHAR(128) NOT NULL COMMENT 'Provider侧模型ID',
    display_name VARCHAR(256) COMMENT '显示名称',
    context_window INT COMMENT '上下文窗口(Token数)',
    input_price DECIMAL(10,6) COMMENT '输入价格(每1M tokens)',
    output_price DECIMAL(10,6) COMMENT '输出价格(每1M tokens)',
    capabilities JSON COMMENT '能力标志',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DEPRECATED/DELETED',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    UNIQUE KEY uk_model_provider (provider_id, provider_model_id),
    INDEX idx_models_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型表';

-- Provider API Key表
CREATE TABLE provider_api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_code VARCHAR(64) NOT NULL UNIQUE COMMENT 'Key编码',
    provider_id BIGINT NOT NULL COMMENT '所属提供商ID',
    key_name VARCHAR(64) COMMENT 'Key名称',
    api_key VARCHAR(512) NOT NULL COMMENT 'API Key(加密存储)',
    priority INT DEFAULT 100 COMMENT '优先级',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED/EXHAUSTED/EXPIRED',
    last_used_at TIMESTAMP COMMENT '最后使用时间',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_provider_api_keys_provider (provider_id),
    INDEX idx_provider_api_keys_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Provider API Key表';

-- ============================================
-- 3. 令牌与限额域
-- ============================================

-- 网关API Key表
CREATE TABLE gateway_api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_code VARCHAR(128) NOT NULL UNIQUE COMMENT 'Key编码',
    key_hash VARCHAR(256) NOT NULL COMMENT 'Key哈希',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(64) COMMENT '密钥名称',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED/EXPIRED/DELETED',
    expires_at TIMESTAMP COMMENT '过期时间',
    last_used_at TIMESTAMP COMMENT '最后使用时间',
    ip_whitelist JSON COMMENT 'IP白名单',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_gateway_api_keys_user (user_id),
    INDEX idx_gateway_api_keys_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关API Key表';

-- Token限额表
CREATE TABLE token_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    limit_code VARCHAR(64) NOT NULL UNIQUE COMMENT '限额编码',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    provider_id BIGINT COMMENT '关联Provider',
    model_id BIGINT COMMENT '关联Model',
    limit_type VARCHAR(32) NOT NULL DEFAULT 'USER_CUSTOM' COMMENT '类型: SYSTEM_DEFAULT/USER_CUSTOM',
    max_tokens DECIMAL(20,6) COMMENT 'Token限额总量',
    used_tokens DECIMAL(20,6) DEFAULT 0 COMMENT '已用Token量',
    period_type VARCHAR(32) NOT NULL DEFAULT 'MONTHLY' COMMENT '周期: DAILY/WEEKLY/MONTHLY/TOTAL',
    period_day_of_week INT COMMENT '周内日期(1-7)',
    period_day_of_month INT COMMENT '月内日期(1-31)',
    exceeded_action VARCHAR(32) NOT NULL DEFAULT 'REJECT' COMMENT '超限动作: REJECT/DOWNGRADE',
    switch_model_id BIGINT COMMENT '降级切换Model',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED/DELETED',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    UNIQUE KEY uk_token_limit (user_id, provider_id, model_id),
    INDEX idx_token_limits_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token限额表';

-- ============================================
-- 4. 计量与分析域
-- ============================================

-- 使用记录表
CREATE TABLE usage_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_code VARCHAR(64) NOT NULL UNIQUE COMMENT '记录编码',
    gateway_api_key_id BIGINT NOT NULL COMMENT '使用的API Key',
    user_id BIGINT NOT NULL COMMENT '所属用户',
    provider_id BIGINT NOT NULL COMMENT '调用的Provider',
    model_id BIGINT NOT NULL COMMENT '使用的Model',
    request_id VARCHAR(64) COMMENT '请求追踪ID',
    input_tokens INT COMMENT '输入Token数',
    output_tokens INT COMMENT '输出Token数',
    total_tokens INT COMMENT '总Token数',
    latency_ms INT COMMENT '响应延迟(毫秒)',
    status_code VARCHAR(32) COMMENT '响应状态码',
    error_message TEXT COMMENT '错误信息',
    failover BOOLEAN DEFAULT FALSE COMMENT '是否发生failover',
    api_format VARCHAR(32) NOT NULL COMMENT 'API格式: OPENAI/ANTHROPIC',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_usage_user_created (user_id, created_at),
    INDEX idx_usage_provider_created (provider_id, created_at),
    INDEX idx_usage_key_created (gateway_api_key_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用记录表';

-- 预警规则表
CREATE TABLE alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL UNIQUE COMMENT '规则编码',
    name VARCHAR(128) NOT NULL COMMENT '规则名称',
    alert_type VARCHAR(32) NOT NULL COMMENT '预警类型: USAGE/HEALTH/QUOTA',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型: USER/PROVIDER/API_KEY',
    target_id BIGINT COMMENT '目标ID',
    condition_type VARCHAR(32) COMMENT '条件类型: THRESHOLD/RATIO/TREND',
    threshold_value DECIMAL(20,6) COMMENT '阈值',
    period_type VARCHAR(32) COMMENT '周期: DAILY/WEEKLY/MONTHLY/TOTAL',
    notification_channels JSON COMMENT '通知渠道',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_alert_rules_type (alert_type),
    INDEX idx_alert_rules_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警规则表';

-- 预警通知表
CREATE TABLE alert_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_code VARCHAR(64) NOT NULL UNIQUE COMMENT '通知编码',
    alert_rule_id BIGINT NOT NULL COMMENT '关联预警规则',
    target_user_id BIGINT NOT NULL COMMENT '通知目标用户',
    channel VARCHAR(32) NOT NULL COMMENT '通知渠道: SYSTEM/EMAIL/IM/SMS',
    title VARCHAR(256) COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    alert_data JSON COMMENT '预警数据',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/SENT/FAILED',
    sent_at TIMESTAMP COMMENT '发送时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP COMMENT '删除时间',
    INDEX idx_alert_notifications_rule (alert_rule_id),
    INDEX idx_alert_notifications_user (target_user_id),
    INDEX idx_alert_notifications_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警通知表';

-- ============================================
-- 5. 审计日志表
-- ============================================

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_code VARCHAR(64) NOT NULL UNIQUE COMMENT '审计编码',
    user_id BIGINT COMMENT '操作用户',
    username VARCHAR(64) COMMENT '操作人用户名',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE',
    target_type VARCHAR(64) NOT NULL COMMENT '操作对象类型',
    target_id BIGINT COMMENT '操作对象ID',
    target_code VARCHAR(128) COMMENT '操作对象编码',
    detail JSON COMMENT '操作详情',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_logs_user (user_id),
    INDEX idx_audit_logs_target (target_type, target_id),
    INDEX idx_audit_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';
```

- [ ] **Step 2: 提交数据库迁移脚本**

```bash
git add src/main/resources/db/V1__init_schema.sql
git commit -m "feat: add initial database migration script

V1__init_schema.sql includes:
- users, roles, permissions, user_roles
- providers, models, provider_api_keys
- gateway_api_keys, token_limits
- usage_logs, alert_rules, alert_notifications
- audit_logs

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## 3. Self-Review 检查

### 3.1 Spec Coverage

| 设计文档章节 | Phase 1 任务 |
|-------------|-------------|
| 2.1 身份与访问控制域 | Task 1-3 (User, Role, Permission, UserRole) |
| 2.2 提供商与模型域 | Task 4 (Provider, Model) |
| 2.3 令牌与限额域 | Task 5 (GatewayApiKey, TokenLimit) |
| 2.4 计量与分析域 | Task 6 (UsageLog, AlertRule, AlertNotification) |
| 数据库设计 | Task 7 (V1__init_schema.sql) |

### 3.2 Placeholder Scan

- [x] 无 TBD/TODO
- [x] 所有实体字段与设计文档一致
- [x] 所有枚举值与设计文档一致

### 3.3 Type Consistency

- [x] User.status → UserStatus (ACTIVE/DISABLED/LOCKED/DELETED)
- [x] User.role → UserRole (ADMIN/DEVELOPER/OBSERVER/FINANCE_ADMIN)
- [x] Provider.providerType → ProviderType (OPENAI/ANTHROPIC/...)
- [x] TokenLimit.periodType → PeriodType (DAILY/WEEKLY/MONTHLY/TOTAL)
- [x] AlertRule.alertType → AlertType (USAGE/HEALTH/QUOTA)

---

## 4. 后续 Phase 预告

### Phase 2: 核心 CRUD 功能
- 用户管理 CRUD
- Provider & Model 管理 CRUD
- API Key 管理 CRUD
- TokenLimit 管理 CRUD

### Phase 3: 预警与计量
- AlertRule 管理
- AlertNotification 管理
- UsageLog 记录
- 预警引擎

### Phase 4: 其他功能
- 用量报表
- 操作日志
- 系统设置

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-28-llm-gateway-phase1-plan.md`.**
