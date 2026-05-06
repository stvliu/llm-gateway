# LLM Gateway 完整设计规格书

> **文档版本**: v1.0
> **创建日期**: 2026-04-28
> **状态**: 已完成设计
> **架构师**: Liu Ye

---

## 一、项目概述

### 1.1 项目定位

LLM Gateway 是企业级 AI 模型 API 聚合分发与智能路由网关（APIPark 竞品）。

### 1.2 核心目标

| 目标 | 说明 |
|------|------|
| **统一管理** | 统一管理 100 家主流大模型 |
| **提高透明度** | 完整使用分析（多维度）+ 趋势图表 + 预警 |
| **提高切换效率** | 支持 Provider 快速切换（暂不实现路由） |
| **成本优化** | Token 限额控制，成本透明化 |

### 1.3 技术栈

| 组件 | 技术选型 |
|------|---------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.5.x + Spring WebFlux |
| 数据库 | PostgreSQL 14+ / MySQL 8.0+ (双支持) |
| 缓存 | Redis 6.0+ |
| 插件机制 | JDK SPI + ProviderRegistry |
| API 标准 | OpenAI + Anthropic 双标准 |
| 安全 | RBAC + AES-256 加密 |
| 可观测性 | OpenTelemetry + 结构化日志 |

### 1.4 部署架构

单体多实例部署，通过负载均衡实现高可用。

---

## 二、核心实体设计

### 2.1 实体域划分

```
┌─────────────────────────────────────────────────────────────┐
│                      实体域划分                              │
├─────────────────────────────────────────────────────────────┤
│  ① 身份与访问控制域  │  User、Role、Permission、UserRole  │
│  ② 提供商与模型域    │  Provider、Model、ProviderApiKey   │
│  ③ 令牌与限额域      │  GatewayApiKey、TokenLimit          │
│  ④ 计量与分析域      │  UsageLog、AlertRule、AlertNotif    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 身份与访问控制域

#### User（用户）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(64) | 用户名 |
| email | VARCHAR(128) | 邮箱 (UNIQUE) |
| password_hash | VARCHAR(256) | 密码哈希 (BCrypt) |
| phone | VARCHAR(32) | 手机号 |
| avatar_url | VARCHAR(512) | 头像 URL |
| status | ENUM | ACTIVE/DISABLED/LOCKED/DELETED |
| email_verified | BOOLEAN | 邮箱已验证 |
| last_login_at | TIMESTAMP | 最后登录时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |
| deleted_at | TIMESTAMP | 软删除时间 |

#### Role（角色）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(64) | 角色名称 |
| description | TEXT | 描述 |
| role_type | ENUM | SYSTEM/CUSTOM |
| is_active | BOOLEAN | 是否启用 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

**预设角色：**
- ADMIN (管理员) - 系统全部权限
- DEVELOPER (开发者) - 创建令牌、查看日志、调用API
- OBSERVER (观察者) - 仅查看用量和日志
- FINANCE_ADMIN (财务管理员) - Token 限额配置、用量查看

#### Permission（权限）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| permission_code | VARCHAR(128) | 权限编码 (UNIQUE) |
| name | VARCHAR(64) | 权限名称 |
| description | TEXT | 描述 |
| category | VARCHAR(32) | 分类 (user/provider/model/token/log/setting) |
| created_at | TIMESTAMP | 创建时间 |

**权限编码规范**: `resource:action` 格式 (如 `user:create`, `model:read`)

#### UserRole（用户角色关联）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT FK | 用户 ID |
| role_id | BIGINT FK | 角色 ID |
| created_at | TIMESTAMP | 创建时间 |

**唯一约束**: `(user_id, role_id)`

---

### 2.3 提供商与模型域

#### Provider（提供商）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| provider_name | VARCHAR(128) | 显示名称 |
| provider_type | ENUM | OPENAI/ANTHROPIC/GEMINI/ZHIPU/QWEN/VOLCENGINE/WENXIN/OTHER |
| base_url | VARCHAR(256) | API 端点 |
| website_url | VARCHAR(512) | 官网 URL |
| api_doc_url | VARCHAR(512) | API 文档 URL |
| priority | INT | 优先级 (默认 100) |
| status | ENUM | ACTIVE/SUSPENDED/DELETED |
| created_by | BIGINT FK | 创建人 |
| created_at | TIMESTAMP | 创建时间 |
| updated_by | BIGINT FK | 更新人 |
| updated_at | TIMESTAMP | 更新时间 |

**Provider 模板（内置）：**

| 模板 | Provider Code | API 端点 |
|------|--------------|----------|
| OpenAI | `openai` | `https://api.openai.com` |
| Anthropic | `anthropic` | `https://api.anthropic.com` |
| 智谱 AI | `zhipu` | `https://open.bigmodel.cn/api/paas` |
| 通义千问 | `qwen` | `https://dashscope.aliyuncs.com/api/v1` |
| 火山引擎 | `volcengine` | `https://ark.cn-beijing.volces.com/api/v3` |
| 文心一言 | `wenxin` | `https://qianfan.baidubce.com/v2` |

#### Model（模型）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| provider_id | BIGINT FK | 所属 Provider |
| provider_model_id | VARCHAR(128) | Provider 侧模型 ID |
| display_name | VARCHAR(256) | 显示名称 |
| context_window | INT | 上下文窗口 (Token 数) |
| input_price | DECIMAL(10,6) | 输入价格 (每 1M tokens) |
| output_price | DECIMAL(10,6) | 输出价格 (每 1M tokens) |
| capabilities | JSON | 能力标志 (streaming/function_calling/vision/json_output) |
| status | ENUM | ACTIVE/DEPRECATED/DELETED |
| created_by | BIGINT FK | 创建人 |
| created_at | TIMESTAMP | 创建时间 |
| updated_by | BIGINT FK | 更新人 |
| updated_at | TIMESTAMP | 更新时间 |

**关联方式**：一对多（一个 Provider → 多个 Model）

#### ProviderApiKey（Provider 调用凭证）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| provider_id | BIGINT FK | 所属 Provider |
| key_name | VARCHAR(64) | Key 名称 (如"主Key") |
| api_key | VARCHAR(512) | API Key (AES-256 加密) |
| priority | INT | 优先级 (默认 100) |
| status | ENUM | ACTIVE/DISABLED/EXHAUSTED/EXPIRED |
| last_used_at | TIMESTAMP | 最后使用时间 |
| created_by | BIGINT FK | 创建人 |
| created_at | TIMESTAMP | 创建时间 |
| updated_by | BIGINT FK | 更新人 |
| updated_at | TIMESTAMP | 更新时间 |

---

### 2.4 令牌与限额域

#### GatewayApiKey（用户 API Key）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| key_hash | VARCHAR(256) | API Key 哈希 (用于验证) |
| user_id | BIGINT FK | 所属用户 |
| name | VARCHAR(64) | 密钥名称 |
| status | ENUM | ACTIVE/DISABLED/EXPIRED/DELETED |
| expires_at | TIMESTAMP | 过期时间 (NULL 表示永不过期) |
| last_used_at | TIMESTAMP | 最后使用时间 |
| ip_whitelist | JSON | IP 白名单 (支持 CIDR) |
| created_by | BIGINT FK | 创建人 |
| created_at | TIMESTAMP | 创建时间 |
| updated_by | BIGINT FK | 更新人 |
| updated_at | TIMESTAMP | 更新时间 |

**特性**：
- 绑定到用户，同一用户的所有 Key 共享 TokenLimit
- API Key 脱敏显示，支持复制功能
- 支持 IP 白名单

**索引**: `idx_key_hash`, `idx_user_provider`

#### TokenLimit（Token 限额）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT FK | 所属用户 |
| provider_id | BIGINT FK | 关联 Provider (NULL 表示全部) |
| model_id | BIGINT FK | 关联 Model (NULL 表示全部) |
| max_tokens | DECIMAL(20,6) | Token 限额总量 |
| used_tokens | DECIMAL(20,6) | 已用 Token 量 (DEFAULT 0) |
| period_type | ENUM | DAILY/WEEKLY/MONTHLY/TOTAL |
| period_day_of_week | INT | 周内日期 (1-7, WEEKLY 时有效) |
| period_day_of_month | INT | 月内日期 (1-31, MONTHLY 时有效) |
| exceeded_action | ENUM | REJECT (暂不支持降级) |
| status | ENUM | ACTIVE/SUSPENDED/DELETED |
| created_by | BIGINT FK | 创建人 |
| created_at | TIMESTAMP | 创建时间 |
| updated_by | BIGINT FK | 更新人 |
| updated_at | TIMESTAMP | 更新时间 |

**唯一约束**: `(user_id, provider_id, model_id)`

**分层限额逻辑**：
1. 用户有自定义 → 使用用户自定义
2. 用户无自定义 → 使用系统默认 TokenLimit

**匹配逻辑**（精确匹配）：
- 查找 `WHERE user_id = ? AND provider_id = ? AND model_id = ?`
- 精确匹配优先于 NULL 匹配

---

### 2.5 计量与分析域

#### UsageLog（使用记录）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| gateway_api_key_id | BIGINT FK | 使用的 Gateway API Key |
| user_id | BIGINT FK | 所属用户 |
| provider_id | BIGINT FK | 调用的 Provider |
| model_id | BIGINT FK | 使用的 Model |
| request_id | VARCHAR(64) | 请求追踪 ID (trace_id) |
| input_tokens | INT | 输入 Token 数 |
| output_tokens | INT | 输出 Token 数 |
| total_tokens | INT | 总 Token 数 |
| latency_ms | INT | 响应延迟 (毫秒) |
| status_code | VARCHAR(32) | 响应状态码 |
| error_message | TEXT | 错误信息 (如有) |
| api_format | ENUM | OPENAI / ANTHROPIC |
| created_at | TIMESTAMP | 创建时间 |

**分区策略**: 按月分区 (PARTITION BY RANGE on created_at)
**保留周期**: 90 天（暂不实现）

#### AlertRule（预警规则）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(128) | 规则名称 |
| alert_type | ENUM | USAGE / HEALTH / QUOTA |
| target_type | ENUM | USER / PROVIDER / API_KEY |
| target_id | BIGINT | 目标 ID |
| condition_type | ENUM | THRESHOLD / RATIO / TREND |
| threshold_value | DECIMAL(20,6) | 阈值 |
| period_type | ENUM | DAILY / WEEKLY / MONTHLY / TOTAL |
| notification_channels | JSON | 通知渠道 [SYSTEM, EMAIL, IM, SMS] |
| is_active | BOOLEAN | 是否启用 |
| created_by | BIGINT FK | 创建人 |
| created_at | TIMESTAMP | 创建时间 |
| updated_by | BIGINT FK | 更新人 |
| updated_at | TIMESTAMP | 更新时间 |

**预警触发条件**：

| 类型 | 触发条件 |
|------|---------|
| 用量预警 | 80% / 90% / 100% |
| 健康预警 | 连续失败次数 + 成功率 + 延迟 组合 |
| 额度预警 | 剩余额度 20% / 10% / 5% |

#### AlertNotification（预警通知）

| 属性 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| alert_rule_id | BIGINT FK | 关联预警规则 |
| target_user_id | BIGINT FK | 通知目标用户 |
| channel | ENUM | SYSTEM / EMAIL / IM / SMS |
| title | VARCHAR(256) | 通知标题 |
| content | TEXT | 通知内容 |
| alert_data | JSON | 预警数据 (当前值/阈值等) |
| status | ENUM | PENDING / SENT / FAILED |
| sent_at | TIMESTAMP | 发送时间 |
| created_at | TIMESTAMP | 创建时间 |

---

## 三、API 设计

### 3.1 端点定义

#### OpenAI 标准端点

```
POST /v1/chat/completions        聊天补全 (OpenAI 兼容)
GET  /v1/models                  获取可用模型列表
```

#### Anthropic 标准端点

```
POST /v1/messages               消息创建 (Anthropic 兼容)
GET  /v1/models                  获取可用模型列表
```

#### 管理 API 端点

```
用户管理:
  POST   /admin/users                     创建用户
  GET    /admin/users                     获取用户列表
  GET    /admin/users/{id}                获取用户详情
  PUT    /admin/users/{id}                更新用户
  DELETE /admin/users/{id}                删除用户

Provider 管理:
  POST   /admin/providers                  创建 Provider
  GET    /admin/providers                  获取 Provider 列表
  GET    /admin/providers/{id}            获取 Provider 详情
  PUT    /admin/providers/{id}            更新 Provider
  DELETE /admin/providers/{id}            删除 Provider

Model 管理:
  POST   /admin/models                     创建 Model
  GET    /admin/models                     获取 Model 列表
  GET    /admin/models/{id}                获取 Model 详情
  PUT    /admin/models/{id}                更新 Model
  DELETE /admin/models/{id}                删除 Model

API Key 管理:
  POST   /admin/api-keys                   创建 API Key
  GET    /admin/api-keys                   获取 API Key 列表
  GET    /admin/api-keys/{id}              获取 API Key 详情
  PUT    /admin/api-keys/{id}              更新 API Key
  DELETE /admin/api-keys/{id}              删除 API Key

TokenLimit 管理:
  POST   /admin/token-limits               创建 TokenLimit
  GET    /admin/token-limits               获取 TokenLimit 列表
  GET    /admin/token-limits/{id}          获取 TokenLimit 详情
  PUT    /admin/token-limits/{id}          更新 TokenLimit
  DELETE /admin/token-limits/{id}          删除 TokenLimit

预警管理:
  POST   /admin/alert-rules                创建预警规则
  GET    /admin/alert-rules                获取预警规则列表
  GET    /admin/alert-rules/{id}           获取预警规则详情
  PUT    /admin/alert-rules/{id}           更新预警规则
  DELETE /admin/alert-rules/{id}           删除预警规则
  GET    /admin/alerts                     获取预警通知列表
  PUT    /admin/alerts/{id}/read          标记已读

统计分析:
  GET    /admin/analytics/usage            用量统计
  GET    /admin/analytics/cost             成本统计

操作日志:
  GET    /admin/audit-logs                 操作日志列表

系统设置:
  GET    /admin/settings                   获取系统设置
  PUT    /admin/settings                   更新系统设置
```

---

## 四、SPI 插件机制

### 4.1 Provider 插件接口

```java
public interface LLMProviderAdapter {
    /**
     * 聊天补全 (OpenAI 格式)
     */
    ChatResponse chat(ChatRequest request);
    
    /**
     * 消息创建 (Anthropic 格式)
     */
    MessagesResponse messages(MessagesRequest request);
    
    /**
     * 获取 Provider 能力描述
     */
    ProviderCapabilities getCapabilities();
}
```

### 4.2 Provider 注册机制

```java
@Component
public class ProviderRegistry {
    private final Map<String, LLMProviderAdapter> adapters = new ConcurrentHashMap<>();
    
    public void register(String providerCode, LLMProviderAdapter adapter) {
        adapters.put(providerCode, adapter);
    }
    
    public LLMProviderAdapter get(String providerCode) {
        return adapters.get(providerCode);
    }
    
    public List<String> getAllProviderCodes() {
        return new ArrayList<>(adapters.keySet());
    }
}
```

### 4.3 SPI 注册

```
META-INF/services/
    └── com.codingas.gateway.domain.router.gateway.LLMProviderAdapter
    
文件内容:
    com.codingas.gateway.infrastructure.gateway.openai.OpenAIAdapter
    com.codingas.gateway.infrastructure.gateway.anthropic.AnthropicAdapter
    com.codingas.gateway.infrastructure.gateway.zhipu.ZhipuAdapter
    ...
```

---

## 五、管理后台设计

### 5.1 功能模块

```
┌─────────────────────────────────────────────────────────────────┐
│                     LLM Gateway 管理后台                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │  用户管理   │  │  角色权限   │  │  Provider   │            │
│  │             │  │             │  │   管理     │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │  Model 管理 │  │ API Key    │  │ TokenLimit  │            │
│  │             │  │   管理     │  │   管理     │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │  预警管理   │  │  用量报表   │  │  系统设置   │            │
│  │             │  │             │  │             │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
│  ┌─────────────┐                                               │
│  │  操作日志   │                                               │
│  │             │                                               │
│  └─────────────┘                                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 Provider & Model 合并页面

- Provider 和 Model 在同一页面管理
- Provider 模板化（内置常用 Provider）
- ProviderApiKey 直接在 Provider 页面填写

### 5.3 API Key 管理

- 企业内部使用，每个应用/开发者独立 Key
- API Key 脱敏显示：默认显示 `sk-live-****...****xxxx`
- 支持「显示」和「复制」功能

### 5.4 TokenLimit 管理

- 用户级别 TokenLimit
- 支持按 Provider/Model 细分
- 分层配置：系统默认 + 用户自定义

### 5.5 预警管理

- 预警规则配置
- 多渠道通知：系统内 + 邮件 + IM + 短信

### 5.6 用量报表

- 用量概览：总调用量、总 Token、总成本
- 用量明细：按用户/Provider/Model 多维度

---

## 六、暂不实现功能

以下功能暂不实现，后续迭代：

| 功能 | 说明 |
|------|------|
| 路由管理 | RouteGroup、智能路由、故障转移 |
| 数据存储设计 | UsageLog 分区、聚合表、归档策略 |
| 降级切换 | TokenLimit 超限时切换到备用模型 |

---

## 七、版本信息

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2026-04-28 | 初始版本，完成核心设计 |
