# APIPark 实体与实体关系文档

> 本文档基于 APIPark 官方文档（docs.apipark.com）及 GitHub 仓库（github.com/APIParkLab/APIPark）梳理，详细定义 APIPark 平台的所有核心实体及其关系。

---

## 一、实体总览

APIPark 的核心实体可分为以下六个领域：

| 领域 | 核心实体 |
|------|---------|
| **账户与权限** | Account、Role、Team、Member |
| **服务与 API** | Service（AI Service / REST Service）、API |
| **消费者与订阅** | Consumer、Credential、Subscription |
| **AI 模型与渠道** | Provider、Channel、Model、APIKey（资源池） |
| **安全策略** | DataMaskingStrategy |
| **可观测性** | RequestLog |

---

## 二、核心实体定义

### 2.1 账户与权限领域

#### 2.1.1 Account（账户）

**定义**：APIPark 系统中的用户账户。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| username | String | 用户名（用于登录） |
| email | String | 邮箱（用于接收系统通知） |
| department | String | 部门（默认为"未分配"） |
| created_at | Timestamp | 创建时间 |

**说明**：
- 基于 RBAC（基于角色的访问控制）模型
- 初始密码为 `12345678`，用户需及时修改

---

#### 2.1.2 Role（角色）

**定义**：控制用户权限的角色，分系统级和团队级。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| name | String | 角色名称 |
| scope | Enum | SYSTEM（系统级）/ TEAM（团队级） |
| permissions | List | 权限集合 |

**角色类型**：

| 角色类型 | 说明 |
|---------|------|
| **系统级角色** | 全局生效，控制整个 APIPark 系统的用户权限，如管理所有成员、设置 API 网关集群、配置全局日志和数据源 |
| **团队级角色** | 仅在团队内生效，控制团队内的用户权限，如管理团队成员、在团队内创建服务和消费者 |

---

#### 2.1.3 Team（团队）

**定义**：类似租户（Tenant）概念的组织单元，每个团队拥有独立的成员、服务和消费者。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| team_name | String | 团队名称 |
| team_id | String | 唯一标识符（保存后不可更改） |
| description | String | 团队描述 |
| admin_id | UUID | 团队管理员（Account ID） |
| created_at | Timestamp | 创建时间 |

**说明**：
- 用于管理复杂组织结构
- 团队管理员拥有团队内最高权限
- 每个团队独立管理自己的服务和消费者

---

#### 2.1.4 Member（成员）

**定义**：Account 与 Team 之间的多对多关联，记录团队成员及其角色。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| account_id | UUID | 账户 ID |
| team_id | UUID | 团队 ID |
| team_role | String | 团队内角色（如：Application Developer） |
| joined_at | Timestamp | 加入时间 |

**说明**：
- 团队成员默认为 `Application Developer` 角色
- 可在团队内设置不同的访问级别

---

### 2.2 服务与 API 领域

#### 2.2.1 Service（服务）

**定义**：APIPark 中对外暴露的 API 服务单元，是 AI Service 或 REST Service 的统称。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| service_name | String | 服务名称 |
| service_id | String | 唯一标识符（保存后不可更改） |
| service_type | Enum | AI_SERVICE / REST_SERVICE |
| team_id | UUID | 所属团队 ID |
| subscription_review | Enum | NO_REVIEW / MANUAL_REVIEW |
| api_request_prefix | String | 统一 API 请求前缀（AI Service） |
| default_ai_provider_id | UUID | 默认 AI 提供商 ID（AI Service） |
| status | Enum | DRAFT / PUBLISHED / DEPRECATED |
| created_at | Timestamp | 创建时间 |
| updated_at | Timestamp | 更新时间 |

**服务类型**：

| 类型 | 说明 |
|------|------|
| **AI Service** | AI 网关，将不同 AI 模型和 Prompt 转换为统一的 REST API |
| **REST Service** | 传统 API 网关，连接微服务或 HTTP REST API |

**订阅审核方式**：

| 方式 | 说明 |
|------|------|
| **无需审核** | 所有消费者可直接订阅并调用服务 |
| **手动审核** | 消费者订阅后需等待管理员审批才能调用 |

---

#### 2.2.2 API（接口）

**定义**：Service 下的具体 API 端点，是对外暴露的可调用接口。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| service_id | UUID | 所属服务 ID |
| api_name | String | API 名称 |
| api_path | String | API 请求路径 |
| method | String | HTTP 方法（GET/POST/PUT/DELETE） |
| prompt | Text | Prompt 模板（AI Service） |
| variables | JSON | Prompt 变量定义（AI Service） |
| timeout | Integer | 超时时长（毫秒） |
| max_retries | Integer | 最大重试次数 |
| is_unified_api | Boolean | 是否为统一 API（自动创建） |
| model_id | UUID | 绑定模型 ID（AI Service） |
| model_alias_mapping | JSON | 模型别名映射（AI Service） |
| created_at | Timestamp | 创建时间 |

**说明**：
- AI Service 创建时会**自动创建**一个统一 API（Unified API）
- 支持自定义 Prompt + AI 模型封装为新的 API

---

### 2.3 消费者与订阅领域

#### 2.3.1 Consumer（消费者）

**定义**：订阅服务并调用 API 的实体，通过订阅获得调用服务内 API 的权限。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| consumer_name | String | 消费者名称 |
| consumer_id | String | 唯一标识符（保存后不可更改） |
| team_id | UUID | 所属团队 ID |
| description | String | 描述 |
| created_at | Timestamp | 创建时间 |

**说明**：
- 消费者通过订阅服务来获得调用权限
- 确保数据安全性和访问合规性

---

#### 2.3.2 Credential（凭证）

**定义**：消费者的认证凭证，用于 API 调用时的身份验证。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| consumer_id | UUID | 所属消费者 ID |
| credential_name | String | 凭证名称 |
| auth_type | Enum | API_KEY / BASIC / JWT / AK_SK |
| param_position | String | 参数位置（header/query/body） |
| key_value | String | 密钥值 |
| expiration_time | Timestamp | 过期时间（默认永不过期） |
| hide_auth_info | Boolean | 转发时是否隐藏认证信息 |
| created_at | Timestamp | 创建时间 |

**认证类型**：

| 类型 | 说明 |
|------|------|
| **API Key** | API 密钥认证 |
| **Basic Auth** | 基本认证 |
| **JWT** | JSON Web Token 认证 |
| **AK/SK** | Access Key/Secret Key 认证 |

---

#### 2.3.3 Subscription（订阅）

**定义**：Consumer 与 Service 之间的关联，表示消费者对服务的订阅关系。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| consumer_id | UUID | 消费者 ID |
| service_id | UUID | 服务 ID |
| status | Enum | PENDING / APPROVED / REJECTED |
| review_comment | String | 审批备注 |
| subscribed_at | Timestamp | 订阅时间 |
| approved_at | Timestamp | 审批时间 |

**订阅状态流转**：

```
PENDING（待审批）
    ├── APPROVED（通过）→ 可以调用 API
    └── REJECTED（拒绝）→ 不可调用 API
```

---

### 2.4 AI 模型与渠道领域

#### 2.4.1 Provider（AI 提供商）

**定义**：AI 模型服务提供商（如 OpenAI、Anthropic、AWS Bedrock 等）。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| provider_name | String | 提供商名称（如：OpenAI、Anthropic） |
| provider_code | String | 提供商代码（如：openai、anthropic） |
| provider_type | Enum | BUILT_IN（内置） / CUSTOM（自定义） |
| base_url | String | API 端点地址 |
| is_enabled | Boolean | 是否启用 |
| created_at | Timestamp | 创建时间 |

**内置提供商**：

- 火山引擎（字节跳动）
- 阿里云百炼
- Hugging Face
- Ollama
- LM Studio
- Xinference

**自定义提供商**：
- 任何符合 OpenAI 接口标准的第三方 LLM 服务

---

#### 2.4.2 Channel（渠道）

**定义**：V1.6 引入的概念，代表符合 OpenAI 接口标准的第三方 LLM 服务接入通道。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| provider_id | UUID | 所属提供商 ID |
| channel_name | String | 渠道名称 |
| base_url | String | 渠道端点地址 |
| is_enabled | Boolean | 是否启用 |
| priority | Integer | 负载均衡优先级（数字越小优先级越高） |
| status | Enum | NORMAL / ABNORMAL |
| created_at | Timestamp | 创建时间 |

**说明**：
- Channel 是 Provider 下的具体接入通道
- 同一 Provider 可以有多个 Channel（不同 endpoint/API Key 组合）
- 支持自定义渠道接入（V1.6+）

---

#### 2.4.3 Model（AI 模型）

**定义**：AI 提供商下的具体 AI 模型。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| provider_id | UUID | 所属提供商 ID |
| model_name | String | 模型名称（如：gpt-4o、claude-3-5-sonnet） |
| display_name | String | 显示名称 |
| is_default | Boolean | 是否为默认模型 |
| parameters | JSON | 模型参数配置（温度、最大 token 等） |
| is_enabled | Boolean | 是否启用 |
| created_at | Timestamp | 创建时间 |

**说明**：
- 每个 Provider 下有多个 Model
- 创建 AI 服务时可选择默认模型
- 支持模型参数自定义（V1.6+，如最大 token 数、温度值、频率惩罚等）

---

#### 2.4.4 APIKey（API Key 资源池）

**定义**：AI 提供商的 API Key 集中管理和分配机制。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| provider_id | UUID | 所属提供商 ID |
| channel_id | UUID | 所属渠道 ID（可选） |
| key_name | String | Key 名称 |
| api_key | String | 实际 API Key 值 |
| status | Enum | NORMAL / EXCEEDED / EXPIRED / DISABLED |
| priority | Integer | 调用优先级（数字越小优先级越高） |
| expiration_time | Timestamp | 过期时间（默认永不过期） |
| is_default | Boolean | 是否为默认 Key |
| created_at | Timestamp | 创建时间 |

**说明**：
- API Key 资源池集中管理各厂商的 Key
- 支持拖拽调整优先级
- 当 Key 遇到超出额度或过期问题时，系统自动按优先级激活其他 Key

---

### 2.5 安全策略领域

#### 2.5.1 DataMaskingStrategy（数据脱敏策略）

**定义**：过滤 API 调用中敏感数据的安全策略。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| strategy_name | String | 策略名称 |
| scope | Enum | GLOBAL（全局） / SERVICE（服务级） |
| service_id | UUID | 所属服务 ID（服务级） |
| priority | Integer | 优先级（数字越小优先级越高） |
| matching_conditions | JSON | 匹配条件 |
| masking_rules | JSON | 脱敏规则 |
| is_enabled | Boolean | 是否启用 |
| created_at | Timestamp | 创建时间 |

**匹配条件支持**：

| 条件属性 | 可选值 |
|---------|--------|
| API 请求方法 | ALL / GET / POST / PUT / DELETE / PATCH / HEADER / OPTIONS |
| API 路径 | 正则表达式 |
| IP | IP 地址或 CIDR 范围 |
| 消费者 | 从消费者列表中选择 |

**支持的数据格式**：

- 姓名
- 电话号码
- 身份证号
- 银行卡号
- 日期
- 金额

**优先级规则**：
1. 服务级策略优先于全局策略
2. 同一级别内，数字越小优先级越高
3. 同一对象匹配多个策略时，只执行优先级最高的

---

### 2.6 可观测性领域

#### 2.6.1 RequestLog（请求日志）

**定义**：记录 API 和 MCP 的每次调用请求。

| 属性 | 类型 | 说明 |
|------|------|------|
| id | UUID | 唯一标识符 |
| service_id | UUID | 所属服务 ID |
| api_id | UUID | 所属 API ID |
| consumer_id | UUID | 调用消费者 ID |
| request_timestamp | Timestamp | 请求时间 |
| request_method | String | 请求方法 |
| request_path | String | 请求路径 |
| request_headers | JSON | 请求头 |
| request_body | JSON | 请求体 |
| response_status | Integer | 响应状态码 |
| response_body | JSON | 响应体 |
| latency_ms | Integer | 延迟（毫秒） |
| token_usage | JSON | Token 消耗（AI Service） |
| error_message | String | 错误信息（如果有） |
| masking_applied | Boolean | 是否应用了脱敏 |

**说明**：
- V1.8+ 支持流式数据格式化
- 支持查看请求和响应参数
- 支持输出到 Loki（V1.8+ 默认）

---

## 三、实体关系图

### 3.1 账户与权限领域关系

```
Account
  ├── (1,N) Member ──(N,1)── Team
  └── (1,N) RoleBinding
             └── Role (SYSTEM / TEAM scope)
```

**说明**：
- Account 与 Team 通过 Member 实现**多对多**关联
- Account 与 Role 通过 RoleBinding 实现**多对多**关联
- Team 是租户级隔离单元

---

### 3.2 服务与订阅领域关系

```
Team
  └── (1,N) Service
            ├── (1,N) API
            │
            └── (1,N) Subscription
                      ├── (N,1) Consumer
                      │         └── (1,N) Credential
                      │
                      └── (N,1) Service ← (循环引用，订阅关系)

Consumer
  └── (1,N) Subscription
            └── (N,1) Service
```

**说明**：
- Team 与 Service 为**一对多**
- Service 与 API 为**一对多**
- Service 与 Consumer 通过 Subscription 实现**多对多**（带状态）
- Consumer 与 Credential 为**一对多**

---

### 3.3 AI 模型与渠道领域关系

```
Provider
  ├── (1,N) Channel
  │         └── (1,N) APIKey
  │
  └── (1,N) Model
            └── (1,N) APIKey (可选关联)

Service (AI Service)
  └── (1,N) API
            ├── (N,1) Model (绑定)
            └── (N,1) Channel (通过 Model → Provider → Channel 路由)
```

**说明**：
- Provider 与 Channel 为**一对多**
- Provider 与 Model 为**一对多**
- Channel 与 APIKey 为**一对多**
- Model 与 Service 中 API 为**多对多**（一个 Model 可绑定到多个 API，一个 API 可选择不同 Model）
- **关键关系：Model 与 Channel 是 M:N 关系**
  - 一个 Model 可以通过多个 Channel 访问（主备切换）
  - 一个 Channel 可以服务多个 Model

---

### 3.4 完整实体关系总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SYSTEM LEVEL                                      │
│                                                                              │
│  Account ──(Member)──> Team ──(Member)──> Account                           │
│      │                      │                                                 │
│      └──(RoleBinding)──> Role                                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ (Team owns)
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SERVICE LEVEL                                       │
│                                                                              │
│  Team                                                                        │
│    │                                                                        │
│    ├── (1,N) Service                                                        │
│    │         │                                                              │
│    │         ├── (1,N) API                                                 │
│    │         │         │                                                    │
│    │         │         └── (N,1) Model ──(N,N)── Channel                  │
│    │         │                      │                │                      │
│    │         │                      └──(N,N)── Provider                   │
│    │         │                              │                               │
│    │         │                              └──(1,N) APIKey               │
│    │         │                                                              │
│    │         └── (1,N) Subscription ──> Consumer                            │
│    │                                     │                                   │
│    │                                     └── (1,N) Credential              │
│    │                                                                     │
│    └── (1,N) DataMaskingStrategy                                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、关键关系详解

### 4.1 Team（团队/租户）隔离模型

APIPark 采用**共享 Schema + 行级隔离**的多租户模型：

```
Account ──(Member)──> Team ◀──(Member)── Account
                            │
                            ├── (1,N) Service
                            ├── (1,N) Consumer
                            └── (1,N) DataMaskingStrategy
```

**隔离原则**：
- 每个 Team 拥有独立的 Service、Consumer、Subscription
- Team 之间数据完全隔离
- 成员通过 Member 关联加入 Team

---

### 4.2 Consumer 订阅模型

```
Consumer ──(Subscription)──> Service
      │                          ▲
      │                          │
      └── (1,N) Credential ───────┘
```

**订阅流程**：
1. Consumer 创建 Credential（获得调用身份）
2. Consumer 发起订阅请求（Subscription = PENDING）
3. Service 管理员审批（Subscription = APPROVED / REJECTED）
4. APPROVED 后 Consumer 可使用 Credential 调用 Service 内的 API

---

### 4.3 AI 模型路由模型

APIPark 的 AI 模型路由遵循以下层次：

```
API Request (model=provider/model)
         │
         ▼
    ┌─────────┐
    │  API   │
    └────┬────┘
         │ (查找 Model)
         ▼
    ┌─────────┐
    │  Model  │
    └────┬────┘
         │ (查找 Provider)
         ▼
    ┌─────────┐
    │ Provider│
    └────┬────┘
         │ (路由到 Channel)
    ┌────┴────┐
    │ Channel │
    └────┬────┘
         │ (查找可用 APIKey)
         ▼
    ┌─────────┐
    │ APIKey  │ ──── [按优先级选择可用 Key]
    └─────────┘
```

**负载均衡机制**：
- Channel 级别：按 priority 选择可用 Channel
- APIKey 级别：在 APIKey 资源池中按 priority 选择可用 Key
- 支持故障自动切换（主 Key 不可用时切换到备 Key）

---

### 4.4 Model 与 Channel 的 M:N 关系

**核心结论**：Model 与 Channel 是**多对多（M:N）**关系。

**业务场景**：

| 场景 | 说明 |
|------|------|
| **主备切换** | 同一个 Model（如 GPT-4o）可通过 OpenAI Channel 和 Azure OpenAI Channel 访问 |
| **多端点路由** | 同一 Provider 的不同 Region Endpoint（Channel）承载同一 Model |
| **负载均衡** | 同一 Channel 服务多个 Model（如 OpenAI Channel 服务 GPT-4o、GPT-4-turbo） |

**APIPark 证据**：
- V1.5：*"升级为支持模型级的负载均衡...定义 AI 模型间的故障转移策略"*
- V1.6：*"自定义渠道接入...任何符合 OpenAI 接口标准的第三方服务"*
- 灾难恢复文档：*"当主 AI 提供商出现故障时，负载均衡可自动切换到备用 AI 提供商"*

---

## 五、实体属性汇总表

| 实体 | 主要属性 | 所属领域 |
|------|---------|---------|
| Account | id, username, email, department | 账户与权限 |
| Role | id, name, scope, permissions | 账户与权限 |
| Team | id, team_name, team_id, admin_id | 账户与权限 |
| Member | id, account_id, team_id, team_role | 账户与权限 |
| Service | id, service_name, service_id, service_type, team_id | 服务与 API |
| API | id, service_id, api_name, api_path, prompt, model_id | 服务与 API |
| Consumer | id, consumer_name, consumer_id, team_id | 消费者与订阅 |
| Credential | id, consumer_id, auth_type, key_value, expiration_time | 消费者与订阅 |
| Subscription | id, consumer_id, service_id, status | 消费者与订阅 |
| Provider | id, provider_name, provider_code, provider_type | AI 模型与渠道 |
| Channel | id, provider_id, channel_name, priority, status | AI 模型与渠道 |
| Model | id, provider_id, model_name, is_default, parameters | AI 模型与渠道 |
| APIKey | id, provider_id, channel_id, key_name, status, priority | AI 模型与渠道 |
| DataMaskingStrategy | id, strategy_name, scope, service_id, priority | 安全策略 |
| RequestLog | id, service_id, api_id, consumer_id, latency_ms, token_usage | 可观测性 |

---

## 六、关系多重性汇总

| 关系 | 多重性 | 说明 |
|------|--------|------|
| Account ↔ Team | M:N | 通过 Member 关联 |
| Account ↔ Role | M:N | 通过 RoleBinding 关联 |
| Team → Service | 1:N | 团队拥有服务 |
| Team → Consumer | 1:N | 团队拥有消费者 |
| Service → API | 1:N | 服务包含 API |
| Service ↔ Consumer | M:N | 通过 Subscription 关联（带状态） |
| Consumer → Credential | 1:N | 消费者拥有凭证 |
| Provider → Channel | 1:N | 提供商拥有渠道 |
| Provider → Model | 1:N | 提供商拥有模型 |
| Channel → APIKey | 1:N | 渠道拥有 Key |
| Model → API | M:N | 模型可绑定到多个 API |
| **Model ↔ Channel** | **M:N** | **关键结论：多对多关系** |
| Service → DataMaskingStrategy | 1:N | 服务拥有策略 |

---

## 七、参考链接

- **官网**: https://apipark.com
- **文档**: https://docs.apipark.com
- **GitHub**: https://github.com/APIParkLab/APIPark
