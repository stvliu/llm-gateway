# 身份认证与授权设计方案

## 1. 概述

**目标**：为企业内部 LLM 网关设计身份认证与授权体系

**场景**：
- 企业内部使用，对接现有 SSO/IdP（OAuth2.0/LDAP/AD）
- API Key 由用户在 gateway-console 控制台自行创建管理
- 普通用户可查看自己的用量
- 用量按用户维度统计

---

## 2. 认证流程

### 2.1 双层认证架构

```
┌─────────────────────────────────────────────────────────────┐
│                        认证流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  SSO 身份认证（控制台登录）                                   │
│  ────────────────────────                                   │
│  管理员/用户 ──► SSO Provider ──► 验证成功 ──► 发放 JWT      │
│                                              │              │
│                                              ▼              │
│                                    gateway-console 控制台    │
│                                                             │
│  API Key 授权（API 调用）                                    │
│  ────────────────────────                                   │
│  客户端 ──► 网关 ──► 验证 API Key ──► 查询用户限额 ──► 放行  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 SSO 认证流程（控制台）

1. 用户访问 gateway-console
2. 重定向到 SSO Provider 进行身份验证
3. 验证成功，SSO Provider 返回 ID Token / Authorization Code
4. 网关验证 Token，创建 Session 或返回访问令牌
5. 用户登录控制台，可进行 API Key 管理、用量查看等操作

### 2.3 API Key 认证流程（API 调用）

1. 客户端携带 `X-API-Key` Header 请求网关
2. 网关拦截器验证 API Key：
   - 检查 Key 是否存在
   - 检查 Key 是否启用
   - 检查 Key 是否过期
   - 检查关联用户是否被禁用
3. 验证通过后，从 Key 解码用户信息
4. 进行用量检查（该用户所有 Key 的用量合并计算）
5. 请求放行

---

## 3. 角色模型

### 3.1 角色定义

| 角色 | 代码 | 权限描述 |
|------|------|---------|
| 管理员 | `ADMIN` | 配置模型 Provider、管理模型列表、查看全局用量、查看所有用户信息 |
| 普通用户 | `USER` | 创建/管理自己的 API Key、查看自己的用量 |

### 3.2 权限矩阵

| 功能 | ADMIN | USER |
|------|-------|------|
| 管理模型 Provider | ✅ | ❌ |
| 管理模型列表 | ✅ | ❌ |
| 创建 API Key | ✅ | ✅ |
| 查看自己 API Key | ✅（所有） | ✅（仅自己） |
| 禁用/删除 API Key | ✅（所有） | ✅（仅自己） |
| 查看全局用量统计 | ✅ | ❌ |
| 查看自己用量统计 | ✅ | ✅ |
| 管理所有用户 | ✅ | ❌ |

---

## 4. API Key 设计

### 4.1 API Key 结构

```
格式: gw_live_{随机字符串}
示例: gw_live_a1b2c3d4e5f6g7h8i9j0

存储:
- key_hash: SHA-256 哈希值（用于验证）
- user_id: 关联用户 ID
- key_name: 用户自定义名称（如"测试Key"）
- created_at: 创建时间
- expires_at: 过期时间（可选，NULL 表示永不过期）
- enabled: 是否启用
- last_used_at: 最后使用时间
```

### 4.2 API Key 生成规则

```java
// 生成策略
String prefix = "gw_live_";
String randomPart = generateSecureRandom(24); // 24字符随机字符串
String fullKey = prefix + randomPart;

// 存储哈希值，不存储明文
String keyHash = sha256(fullKey);
```

### 4.3 API Key 生命周期

```
创建 ──► 显示明文一次 ──► 用户复制保存 ──► 后续仅存储哈希值
  │
  ├──► 使用中 ──► 禁用 ──► 删除
  │
  └──► 过期 ──► 自动禁用（定时任务）
```

---

## 5. 用量统计设计

### 5.1 统计维度

**按用户统计**（非按 Key）：
- 同一用户的多个 API Key 用量合并计算
- 限流也在用户维度（所有 Key 共享限额）

### 5.2 用量记录表（usage_records）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| request_id | VARCHAR(64) | 请求唯一标识|
| model_code | VARCHAR(128) | 调用的模型 |
| provider_code | VARCHAR(64) | Provider 标识 |
| input_tokens | BIGINT | 输入 Token 数 |
| output_tokens | BIGINT | 输出 Token 数 |
| latency_ms | INT | 请求延迟（毫秒） |
| status | VARCHAR(32) | 成功/失败/限流 |
| created_at | TIMESTAMP | 创建时间 |

**索引**：
- `idx_user_id_created_at(user_id, created_at)` - 按用户查询用量
- `idx_created_at(created_at)` - 按时间查询全量

### 5.3 用量聚合表（usage_daily）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| stat_date | DATE | 统计日期 |
| total_requests | BIGINT | 总调用次数 |
| total_input_tokens | BIGINT | 总输入 Token |
| total_output_tokens | BIGINT | 总输出 Token |
| avg_latency_ms | INT | 平均延迟 |

**索引**：
- `UNIQUE idx_user_date(user_id, stat_date)` - 联合唯一

### 5.4 用户用量视图（控制台展示）

```
用户: 张三 (user_code: zhangsan)
总调用次数: 12,345
总输入 Token: 1,234,567
总输出 Token: 3,456,789
本月限额: 10,000,000 Token
本月已用: 4,691,356 (46.9%)
剩余额度: 5,308,644

最近7天趋势:
日期       | 调用次数 | 输入Token | 输出Token
----------|---------|----------|----------
2026-04-29 | 1,234   | 123,456  | 345,678
2026-04-28 | 1,456   | 145,678  | 401,234
...
```

---

## 6. 限流设计

### 6.1 限流维度

**按用户维度限流**（非按 Key）：
- 用户维度令牌桶，所有 Key 共享
- 防止用户通过多个 Key 突破限流

### 6.2 限流策略

```java
// 用户限流配置
public class UserRateLimitConfig {
    Long userId;           // 用户 ID
    int requestsPerMinute; // 每分钟请求数
    long tokenLimit;       // Token 限额（可选）
}

// 限流检查流程
public boolean checkRateLimit(String apiKey) {
    UserAuthResult user = authService.authenticate(apiKey);
    UserRateLimitConfig config = rateLimitService.getConfig(user.userId());

    // 检查请求频率
    if (!rateLimiter.tryAcquire(user.userId(), config.requestsPerMinute())) {
        return false; // 限流
    }

    // 检查 Token 限额（本月已用 + 本次预估）
    long usedThisMonth = usageService.getMonthlyTokenUsage(user.userId());
    if (usedThisMonth >= config.tokenLimit()) {
        return false; // 超限额
    }

    return true;
}
```

### 6.3 限流响应

当触发限流时，返回 HTTP 429：
```json
{
    "error": {
        "code": "RATE_LIMIT_EXCEEDED",
        "message": "请求频率超出限制，请稍后重试",
        "retry_after": 60
    }
}
```

---

## 7. 数据库表设计

### 7.1 核心表

```sql
-- 用户表（从 SSO 同步）
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(128) NOT NULL COMMENT '用户名（来自SSO）',
    email VARCHAR(256) COMMENT '邮箱',
    role VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN/USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    UNIQUE INDEX idx_username(username)
) COMMENT '用户表';

-- API Key 表
CREATE TABLE api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_hash VARCHAR(128) NOT NULL UNIQUE COMMENT 'Key哈希（用于验证）',
    user_id BIGINT NOT NULL COMMENT '关联用户',
    key_name VARCHAR(128) COMMENT 'Key名称（用户自定义）',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP COMMENT '过期时间，NULL永不过期',
    last_used_at TIMESTAMP COMMENT '最后使用时间',
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP,
    INDEX idx_user_id(user_id),
    INDEX idx_key_hash(key_hash),
    FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT 'API Key表';

-- 用量记录表
CREATE TABLE usage_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL UNIQUE COMMENT '请求唯一标识',
    user_id BIGINT NOT NULL,
    api_key_id BIGINT COMMENT '使用的API Key',
    model_code VARCHAR(128) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    latency_ms INT,
    status VARCHAR(32) NOT NULL COMMENT 'SUCCESS/FAILED/RATE_LIMITED',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id_created_at(user_id, created_at),
    INDEX idx_created_at(created_at),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (api_key_id) REFERENCES api_keys(id)
) COMMENT '用量记录表';

-- 用量聚合表（按日聚合）
CREATE TABLE usage_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    total_requests BIGINT NOT NULL DEFAULT 0,
    total_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_output_tokens BIGINT NOT NULL DEFAULT 0,
    avg_latency_ms INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_user_date(user_id, stat_date),
    FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT '用量聚合表（按日）';

-- 用户限流配置表
CREATE TABLE user_rate_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    requests_per_minute INT NOT NULL DEFAULT 60,
    monthly_token_limit BIGINT COMMENT '月度Token限额，NULL无限制',
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT '用户限流配置表';
```

---

## 8. 安全设计

### 8.1 API Key 安全

| 措施 | 说明 |
|------|------|
| 哈希存储 | 明文 Key 只在创建时返回一次，之后只存储 SHA-256 哈希 |
| 不可逆验证 | 验证时比对哈希值，无法从哈希反推明文 |
| 一次显示 | 创建后立即显示明文，刷新页面后不再显示 |
| 独立启停 | 每个 Key 可独立启用/禁用，不影响其他 Key |

### 8.2 传输安全

- 所有 API 调用强制 HTTPS（TLS 1.3）
- API Key 通过 Header 传递，不放在 URL 中（避免日志泄露）

### 8.3 审计日志

| 事件 | 记录内容 |
|------|---------|
| API Key 创建 | user_id, key_name, created_at, operator |
| API Key 禁用 | user_id, key_id, operator, reason |
| API Key 删除 | user_id, key_id, operator |
| 限流触发 | user_id, api_key_id, request_time |
| 认证失败 | key_hash（前8位）, request_ip, request_time |

---

## 9. 控制台功能

### 9.1 管理员功能

- **模型管理**：添加/编辑/删除模型 Provider 和模型
- **用户管理**：查看/禁用/启用用户
- **用量统计**：查看所有用户用量、全局统计
- **限流配置**：为用户设置限流参数

### 9.2 普通用户功能

- **API Key 管理**：
  - 创建新 Key（输入名称，可选设置过期时间）
  - 查看 Key 列表（显示名称、创建时间、最后使用、状态）
  - 禁用/启用 Key
  - 删除 Key（需二次确认）
- **用量查看**：
  - 本月用量统计（调用次数、Token 消耗、剩余限额）
  - 最近7天趋势图
  - 按模型分布（饼图）

---

## 10. 技术实现要点

### 10.1 SSO 集成

```java
// SSO 配置
@Configuration
public class SsoConfig {
    @Value("${sso.provider:oauth2}")
    private String provider; // oauth2, ldap, ad

    @Value("${sso.issuer-uri}")
    private String issuerUri;

    @Value("${sso.client-id}")
    private String clientId;

    @Value("${sso.client-secret}")
    private String clientSecret;
}
```

### 10.2 API Key 验证

```java
// 验证流程
public UserAuthResult authenticate(String apiKey) {
    String keyHash = sha256(apiKey);

    return apiKeyGateway.findByHash(keyHash)
        .filter(ApiKey::isEnabled)
        .filter(key -> key.getExpiresAt() == null || key.getExpiresAt().isAfter(now()))
        .flatMap(key -> userGateway.findById(key.getUserId()))
        .filter(User::isEnabled)
        .map(user -> new UserAuthResult(user.getId(), user.getUserCode(), user.getRole()))
        .orElse(null);
}
```

### 10.3 用量记录切面

```java
// LLM 调用后自动记录用量
@Aspect
@Component
public class UsageRecordingAspect {

    @AfterReturning(pointcut = "execution(* LLMProviderAdapter.chat(..))", returning = "result")
    public void recordUsage(ChatRequest request, ChatResponse result) {
        UsageRecord record = UsageRecord.builder()
            .requestId(request.getRequestId())
            .userId(getCurrentUserId())
            .apiKeyId(getCurrentApiKeyId())
            .modelCode(request.getModel())
            .providerCode(request.getProvider())
            .inputTokens(result.getInputTokens())
            .outputTokens(result.getOutputTokens())
            .latencyMs(result.getLatencyMs())
            .status("SUCCESS")
            .build();

        usageGateway.save(record);
    }
}
```