# Data Model: Provider Adapter Framework

## Entity Overview

| Entity | Table | Description |
|--------|-------|-------------|
| Provider | `providers` | 模型提供商（OpenAI、Anthropic 等） |
| Model | `models` | 具体模型（gpt-4o、claude-sonnet-4 等） |
| Channel | `channels` | Provider 下的连接实例 |
| ChannelKey | `channel_keys` | Channel 下的 API Key |
| ChannelGroup | `channel_groups` | 渠道分组（用于路由策略） |
| ProviderCapabilities | Value Object | Provider 能力描述 |

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
| priority | INT | 优先级 | DEFAULT 100，数值越大越优先 |
| status | ENUM | 状态 | ACTIVE / SUSPENDED / DELETED |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**业务规则**:
- `provider_type` 决定使用哪个 Adapter 实现类
- `priority` 用于路由时的默认排序

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

## Channel（渠道）

**数据库表**: `channels`

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 物理主键 | PK, AUTO_INCREMENT |
| channel_code | VARCHAR(64) | 业务标识 | UNIQUE, NOT NULL |
| channel_name | VARCHAR(128) | 显示名称 | NOT NULL |
| provider_id | BIGINT | 所属 Provider | FK → providers.id, NOT NULL |
| group_id | BIGINT | 所属分组 | FK → channel_groups.id, NULL |
| base_url | VARCHAR(256) | 自定义端点（可覆盖 Provider） | NULL |
| timeout | INT | 超时时间（毫秒） | DEFAULT 30000 |
| max_connections | INT | 最大并发连接数 | DEFAULT 100 |
| status | ENUM | 状态 | ACTIVE / SUSPENDED / DELETED |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**关系**:
- 一个 Provider 可以有多个 Channel
- 一个 Channel 可以属于一个 ChannelGroup

---

## ChannelKey（渠道密钥）

**数据库表**: `channel_keys`

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 物理主键 | PK, AUTO_INCREMENT |
| channel_id | BIGINT | 所属 Channel | FK → channels.id, NOT NULL |
| api_key | VARCHAR(256) | API Key（加密存储） | NOT NULL |
| priority | INT | 优先级（用于轮换） | DEFAULT 100 |
| status | ENUM | 状态 | ACTIVE / EXHAUSTED / EXPIRED / DELETED |
| last_used_at | TIMESTAMP | 最后使用时间 | NULL |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**业务规则**:
- `api_key` 使用 AES-256 加密存储（由 security 模块提供）
- `priority` 数值越大越优先被使用
- `status = EXHAUSTED` 时不会被选择，直到恢复

---

## ChannelGroup（渠道分组）

**数据库表**: `channel_groups`

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 物理主键 | PK, AUTO_INCREMENT |
| group_code | VARCHAR(64) | 业务标识 | UNIQUE, NOT NULL |
| group_name | VARCHAR(128) | 显示名称 | NOT NULL |
| team_id | BIGINT | 所属团队 | FK → teams.id, NOT NULL |
| description | TEXT | 描述 | NULL |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

---

## Value Objects

### ProviderCapabilities

```java
public record ProviderCapabilities(
    boolean supportsChatCompletion,  // OpenAI 格式
    boolean supportsMessages,        // Anthropic 格式
    boolean supportsEmbeddings,     // 向量嵌入
    boolean supportsStreaming,      // 流式响应
    boolean supportsFunctionCalling,// 函数调用
    Set<String> supportedModels      // 支持的模型 ID 列表
) {}
```

### ChatCompletionRequest / Result

```java
public record ChatCompletionRequest(
    String model,
    List<Message> messages,
    Double temperature,
    Integer maxTokens,
    Map<String, Object> extraParams
) {}

public record ChatCompletionResult(
    String id,
    String model,
    List<Choice> choices,
    Usage usage,
    String finishReason
) {}
```

---

## Entity Relationships

```
Provider (1) ──── (N) Model
Provider (1) ──── (N) Channel
Channel (1) ──── (N) ChannelKey
ChannelGroup (1) ──── (N) Channel
ChannelGroup (1) ──── (N) Strategy (via ChannelGroupStrategy)
Model (N) ──── (N) Channel (via ModelChannelBinding)
```

---

## State Transitions

### Provider 状态流转

```
ACTIVE ──→ SUSPENDED（管理员暂停）
ACTIVE ──→ DELETED（软删除）
SUSPENDED ──→ ACTIVE（恢复使用）
```

### ChannelKey 状态流转

```
ACTIVE ──→ EXHAUSTED（配额用尽/限流触发）
ACTIVE ──→ EXPIRED（过期）
EXHAUSTED ──→ ACTIVE（健康检查恢复）
EXPIRED ──→ ACTIVE（续期后恢复）
```