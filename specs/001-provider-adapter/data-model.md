# Data Model: Provider Adapter Framework

## Entity Overview

| Entity | Table | Description |
|--------|-------|-------------|
| User | `users` | 用户 |
| Provider | `providers` | 模型提供商（OpenAI、Anthropic 等） |
| ProviderApiKey | `provider_api_keys` | Provider 的调用凭证（N:1，支持轮换） |
| Model | `models` | 具体模型（gpt-4o、claude-sonnet-4 等） |
| RouteGroup | `route_groups` | 路由分组（全局，负载均衡/故障转移） |
| RouteGroupProvider | `route_group_providers` | 路由分组与Provider关联 |
| GatewayApiKey | `gateway_api_keys` | 用户调用网关的凭证 |
| TokenLimit | `token_limits` | 用户 Token 限额 |
| ProviderCapabilities | Value Object | Provider 能力描述 |

---

## Two Key Concepts

### Provider API Key（Provider 调用凭证）
- **用途**: 网关调用大模型 Provider 时的凭据
- **管理者**: 管理员配置，属于系统级别
- **所有者**: Provider（一个 Provider 可有多个 Key 做轮换）
- **存储**: 加密存储在 `provider_api_keys` 表
- **轮换**: 按 priority 选择可用 Key，故障时自动切换

### Gateway API Key（网关访问凭证）
- **用途**: 上层应用调用 LLM-Gateway 网关的凭据
- **管理者**: 用户自行管理
- **所有者**: User
- **存储**: 哈希存储在 `gateway_api_keys` 表（用于认证）

---

## Provider（提供商）

**数据库表**: `providers`

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 物理主键 | PK, AUTO_INCREMENT |
| provider_code | VARCHAR(64) | 业务标识 | UNIQUE, NOT NULL |
| provider_name | VARCHAR(128) | 显示名称 | NOT NULL |
| provider_type | ENUM | 类型 | OPENAI / ANTHROPIC / GEMINI / ZHIPU / OTHER |
| base_url | VARCHAR(256) | API 端点 | NOT NULL |
| website_url | VARCHAR(512) | 官网 URL | NULL |
| api_doc_url | VARCHAR(512) | API 文档 URL | NULL |
| priority | INT | 优先级 | DEFAULT 100，数值越大越优先 |
| status | ENUM | 状态 | ACTIVE / SUSPENDED / DELETED |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**业务规则**:
- `provider_type` 决定使用哪个 Adapter 实现类
- `priority` 用于路由时的默认排序
- Provider 是**全局**的，所有用户共享

---

## ProviderApiKey（Provider 调用凭证）

**数据库表**: `provider_api_keys`

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 物理主键 | PK, AUTO_INCREMENT |
| key_code | VARCHAR(64) | 业务标识 | UNIQUE, NOT NULL |
| provider_id | BIGINT | 所属 Provider | FK → providers.id, NOT NULL |
| key_name | VARCHAR(64) | Key 名称（如"主Key"） | NULL |
| api_key | VARCHAR(512) | API Key（加密存储） | NOT NULL |
| priority | INT | 优先级（用于轮换） | DEFAULT 100，数值越大越优先 |
| status | ENUM | 状态 | ACTIVE / DISABLED / EXHAUSTED / EXPIRED / DELETED |
| last_used_at | TIMESTAMP | 最后使用时间 | NULL |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**业务规则**:
- `api_key` 使用 AES-256 加密存储（由 security 模块提供）
- 同一 Provider 下可有多个 Key（主备/轮换）
- `status = EXHAUSTED` 时进入冷却期，不会被选择
- `status = DISABLED` 不会被选择，直到管理员恢复

**索引**:
- `idx_provider_id`: 用于查找某 Provider 下所有 Key
- `idx_provider_status`: 用于查找某 Provider 下可用的 Key

---

## Model（模型）

**数据库表**: `models`

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 物理主键 | PK, AUTO_INCREMENT |
| model_code | VARCHAR(128) | 业务标识 | UNIQUE, NOT NULL |
| provider_id | BIGINT | 所属 Provider | FK → providers.id, NOT NULL |
| provider_model_id | VARCHAR(128) | Provider 侧模型 ID | NOT NULL（如 gpt-4o） |
| display_name | VARCHAR(256) | 显示名称 | NOT NULL |
| context_window | INT | 上下文窗口（token 数） | NULL 表示未知 |
| input_price | DECIMAL(10,6) | 输入价格（每 1M tokens） | NULL 表示不可用 |
| output_price | DECIMAL(10,6) | 输出价格（每 1M tokens） | NULL 表示不可用 |
| capabilities | JSON | 能力标志 | 支持 streaming/function_calling 等 |
| status | ENUM | 状态 | ACTIVE / DEPRECATED / DELETED |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**唯一约束**: `(provider_id, provider_model_id)` 联合唯一

**capabilities JSON 示例**:
```json
{
  "streaming": true,
  "function_calling": true,
  "vision": false,
  "json_output": true
}
```

---

## RouteGroup（路由分组）

**数据库表**: `route_groups`

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 物理主键 | PK, AUTO_INCREMENT |
| group_code | VARCHAR(64) | 业务标识 | UNIQUE, NOT NULL |
| group_name | VARCHAR(128) | 显示名称 | NOT NULL |
| strategy | ENUM | 路由策略 | ROUND_ROBIN / LEAST_LATENCY / PRIORITY |
| failover_enabled | BOOLEAN | 是否启用故障转移 | DEFAULT TRUE |
| max_retry | INT | 最大重试次数 | DEFAULT 2 |
| health_check_interval | INT | 健康检查间隔（秒） | DEFAULT 30 |
| description | TEXT | 描述 | NULL |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**路由策略说明**:

| 策略 | 说明 |
|------|------|
| `ROUND_ROBIN` | 按 weight 加权轮询 |
| `LEAST_LATENCY` | 选择延迟最低的 Provider |
| `PRIORITY` | 优先使用高优先级，失败后 failover |

---

## RouteGroupProvider（路由分组与Provider关联）

**数据库表**: `route_group_providers`

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 物理主键 | PK, AUTO_INCREMENT |
| route_group_id | BIGINT | 路由分组 | FK → route_groups.id, NOT NULL |
| provider_id | BIGINT | Provider | FK → providers.id, NOT NULL |
| weight | INT | 权重（用于负载均衡） | DEFAULT 100 |
| priority | INT | 优先级（用于故障转移） | DEFAULT 100，数值越大越优先 |
| status | ENUM | 状态 | ENABLED / DISABLED / UNHEALTHY |
| health_status | ENUM | 健康状态 | HEALTHY / DEGRADED / UNHEALTHY |
| consecutive_failures | INT | 连续失败次数 | DEFAULT 0 |
| last_health_check_at | TIMESTAMP | 最后健康检查时间 | NULL |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**唯一约束**: `(route_group_id, provider_id)` 联合唯一

**状态说明**:

| status | 说明 |
|--------|------|
| ENABLED | 可用于路由 |
| DISABLED | 管理员禁用 |
| UNHEALTHY | 健康检查失败，自动禁用 |

| health_status | 说明 |
|---------------|------|
| HEALTHY | 正常 |
| DEGRADED | 部分失败，响应慢 |
| UNHEALTHY | 连续失败，不可用 |

---

## GatewayApiKey（网关访问凭证）

**数据库表**: `gateway_api_keys`

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 物理主键 | PK, AUTO_INCREMENT |
| key_code | VARCHAR(128) | 业务标识 | UNIQUE, NOT NULL |
| key_hash | VARCHAR(256) | API Key 哈希（用于验证） | NOT NULL |
| user_id | BIGINT | 所属用户 | FK → users.id, NOT NULL |
| provider_id | BIGINT | 关联的 Provider | FK → providers.id, NULL 表示全部 |
| route_group_id | BIGINT | 路由分组 | FK → route_groups.id, NULL 表示默认 |
| name | VARCHAR(64) | 密钥名称 | NULL |
| status | ENUM | 状态 | ACTIVE / DISABLED / EXPIRED / DELETED |
| expires_at | TIMESTAMP | 过期时间 | NULL 表示永不过期 |
| last_used_at | TIMESTAMP | 最后使用时间 | NULL |
| model_whitelist | JSON | 允许使用的模型列表 | NULL 表示全部允许 |
| ip_whitelist | JSON | IP 白名单（支持 CIDR） | NULL 表示全部允许 |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**业务规则**:
- `key_hash` 用于验证 API 调用时传入的 Key
- `provider_id` 为 NULL 表示可访问所有 Provider
- `route_group_id` 为 NULL 使用系统默认路由策略
- 同一用户可创建多个 Key（主备/轮换）

**索引**:
- `idx_key_hash`: 用于 API 调用时快速查找
- `idx_user_provider`: 用于查询某用户的某 Provider 下所有 Key

---

## TokenLimit（用户 Token 限额）

**数据库表**: `token_limits`

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 物理主键 | PK, AUTO_INCREMENT |
| limit_code | VARCHAR(64) | 业务标识 | UNIQUE, NOT NULL |
| user_id | BIGINT | 所属用户 | FK → users.id, NOT NULL |
| provider_id | BIGINT | 关联的 Provider | FK → providers.id, NULL 表示全部 |
| model_id | BIGINT | 关联的 Model | FK → models.id, NULL 表示全部 |
| token_limit_enabled | BOOLEAN | 是否启用 Token 限额 | DEFAULT TRUE |
| max_tokens | DECIMAL(20,6) | Token 限额总量 | NULL 表示不限 |
| used_tokens | DECIMAL(20,6) | 已用 Token 量 | DEFAULT 0 |
| request_limit_enabled | BOOLEAN | 是否启用请求次数限额 | DEFAULT FALSE |
| max_requests | INT | 请求次数限额 | NULL |
| used_requests | INT | 已用请求次数 | DEFAULT 0 |
| period_type | ENUM | 周期类型 | DAILY / WEEKLY / MONTHLY / TOTAL |
| period_day_of_week | INT | 周内日期 | 1-7, WEEKLY 时有效 |
| period_day_of_month | INT | 月内日期 | 1-31, MONTHLY 时有效 |
| exceeded_action | ENUM | 超限动作 | REJECT / DOWNGRADE |
| switch_model_id | BIGINT | 降级切换模型 | NULL |
| status | ENUM | 状态 | ACTIVE / SUSPENDED / DELETED |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**业务规则**:
- `provider_id` 为 NULL 表示限制适用于所有 Provider
- `model_id` 为 NULL 表示限制适用于所有模型
- `period_type = TOTAL` 时不重置，累加总量
- `exceeded_action = DOWNGRADE` 时会切换到 `switch_model_id`

**周期重置说明**:

| period_type | 重置时机 |
|------------|----------|
| DAILY | 每天 UTC 00:00 重置 |
| WEEKLY | 每周一 UTC 00:00 重置 |
| MONTHLY | 每月 1 日 UTC 00:00 重置 |
| TOTAL | 不重置，累加总量 |

---

## Entity Relationships

```
Provider (1) ──── (N) ProviderApiKey
Provider (1) ──── (N) Model
Provider (1) ──── (N) RouteGroupProvider

RouteGroup (1) ──── (N) RouteGroupProvider

RouteGroupProvider (N) ──── (1) Provider
RouteGroupProvider (N) ──── (1) RouteGroup

User (1) ──── (N) GatewayApiKey
User (1) ──── (N) TokenLimit

GatewayApiKey (N) ──── (1) Provider (可选)
GatewayApiKey (N) ──── (1) RouteGroup (可选)
TokenLimit (N) ──── (1) Provider (可选)
TokenLimit (N) ──── (1) Model (可选)
```

---

## Routing Flow

```
用户请求
    │
    ▼
┌─────────────────────┐
│  GatewayApiKey 认证  │ ← 验证 key_hash
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│   确定 Provider     │ ← 三种方式：
│                     │   1. 请求指定 provider_id
│                     │   2. 请求指定 model → Model.providerId
│                     │   3. 使用 RouteGroup 策略
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│  RouteGroup 策略    │ ← 加权轮询/最小延迟/优先级
│  选择最终 Provider  │
└─────────────────────┘
    │
    ▼
┌─────────────────────┐
│ ProviderApiKey 调用 │ ← 选择优先级最高的可用 Key
└─────────────────────┘
```

---

## Failover Flow

```
请求到达
    │
    ▼
选择 Provider A (优先级最高)
    │
    ▼
调用 Provider A → 失败 (超时/限流/错误)
    │
    ▼
检查 RouteGroup.failoverEnabled = true ?
    │
    ├── 否 → 返回错误
    │
    ▼
查找下一个可用 Provider (status=ENABLED)
    │
    ▼
重新尝试调用 → 成功
    │
    ▼
返回结果（记录 failover 事件）
```

---

## State Transitions

### Provider 状态流转

```
ACTIVE ──→ SUSPENDED（管理员暂停）
ACTIVE ──→ DELETED（软删除）
SUSPENDED ──→ ACTIVE（恢复使用）
```

### ProviderApiKey 状态流转

```
ACTIVE ──→ DISABLED（管理员禁用）
ACTIVE ──→ EXHAUSTED（限流触发）
ACTIVE ──→ EXPIRED（过期）
EXHAUSTED ──→ ACTIVE（冷却后恢复）
DISABLED ──→ ACTIVE（恢复使用）
EXPIRED ──→ ACTIVE（续期后恢复）
```

### RouteGroupProvider 状态流转

```
ENABLED ──→ DISABLED（管理员禁用）
ENABLED ──→ UNHEALTHY（健康检查失败）
UNHEALTHY ──→ ENABLED（健康检查恢复）
DISABLED ──→ ENABLED（恢复使用）
```

### GatewayApiKey 状态流转

```
ACTIVE ──→ DISABLED（管理员禁用）
ACTIVE ──→ EXPIRED（过期）
DISABLED ──→ ACTIVE（恢复使用）
EXPIRED ──→ ACTIVE（续期后恢复）
```
