# Feature Specification: 安全零信任

**Feature Branch**: `003-security-zero-trust`  
**Created**: 2026-04-24  
**Status**: Draft  
**Input**: User description: "安全零信任"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 统一身份认证 (Priority: P1)

作为企业用户，我希望在调用网关API时通过API Key进行身份认证，这样我可以安全地访问网关服务。

**Why this priority**: 身份认证是零信任安全的基础，所有请求必须先确认身份才能进行后续处理。

**Independent Test**: 可以通过向网关发送有效API Key的请求，验证请求被正确认证并返回正常响应；发送无效Key则被拒绝。

**Acceptance Scenarios**:

1. **Given** 用户提供了有效的GatewayApiKey（如 `sk-xK9mP2vL8nQ4wF7hJ3dR6tB0yC5sE8gU`），**When** 向网关发送请求，**Then** 请求被认证通过并继续处理
2. **Given** 用户提供了无效或过期的GatewayApiKey，**When** 向网关发送请求，**Then** 请求被拒绝，返回401未授权错误
3. **Given** 用户未提供API Key，**When** 向网关发送请求，**Then** 请求被拒绝，返回401未授权错误

---

### User Story 2 - 角色权限控制 (Priority: P1)

作为管理员，我希望为不同用户分配不同的权限级别，这样我可以控制用户对不同模型提供商的访问。

**Why this priority**: 权限控制确保用户只能访问其被授权的资源，防止越权操作。

**Independent Test**: 可以通过使用不同权限级别的API Key调用网关，验证高权限用户可以访问所有模型，低权限用户只能访问授权的模型。

**Acceptance Scenarios**:

1. **Given** 用户拥有管理员权限，**When** 访问任意模型提供商，**Then** 请求被允许
2. **Given** 用户仅被授权使用OpenAI模型，**When** 尝试访问Anthropic模型，**Then** 请求被拒绝，返回403禁止访问错误
3. **Given** 用户权限被管理员撤销，**When** 使用原API Key发送请求，**Then** 请求被拒绝

---

### User Story 3 - 流量限速保护 (Priority: P1)

作为运维人员，我希望系统对每个API Key进行流量限速，这样我可以防止个别用户过度消耗系统资源。

**Why this priority**: 限流是保护系统稳定性的关键机制，防止DDoS攻击和资源滥用。

**Independent Test**: 可以通过快速连续发送大量请求，验证超出限流阈值的请求被拒绝并返回429错误。

**Acceptance Scenarios**:

1. **Given** 用户在限流阈值内发送请求，**When** 持续发送请求，**Then** 所有请求被正常处理
2. **Given** 用户请求频率超过限流阈值，**When** 发送新的请求，**Then** 请求被拒绝，返回429过多请求错误
3. **Given** 用户的请求被限流，**When** 等待限流窗口重置后重试，**Then** 请求可以正常处理

---

### User Story 4 - 敏感数据脱敏 (Priority: P2)

作为安全管理员，我希望系统自动对敏感数据进行脱敏处理，这样我可以确保敏感信息不会被泄露。

**Why this priority**: 数据脱敏是防止敏感信息泄露的重要手段，满足合规要求。

**Independent Test**: 可以通过发送包含敏感信息（如身份证号、银行卡号、手机号）的请求，验证响应中的敏感信息被正确脱敏。

**Acceptance Scenarios**:

1. **Given** 用户请求中包含手机号，**When** 请求被处理，**Then** 响应中手机号被脱敏为`138****5678`格式
2. **Given** 用户请求中包含身份证号，**When** 请求被处理，**Then** 响应中身份证号被脱敏为`320***********1234`格式
3. **Given** 用户请求中包含银行卡号，**When** 请求被处理，**Then** 响应中银行卡号被脱敏为`**** **** **** 1234`格式

---

### User Story 5 - 审计日志记录 (Priority: P2)

作为审计人员，我希望系统记录所有API调用日志，这样我可以追溯任何安全事件。

**Why this priority**: 完整的审计日志是安全合规的基础，支持事后分析和取证。

**Independent Test**: 可以通过查看系统日志，验证所有API调用都被正确记录，包括调用者、时间、请求内容和响应状态。

**Acceptance Scenarios**:

1. **Given** 用户成功调用API，**When** 请求被处理，**Then** 系统记录包含用户身份、请求时间、请求内容、响应状态的日志
2. **Given** 用户调用被拒绝，**When** 请求被处理，**Then** 系统记录包含拒绝原因的审计日志
3. **Given** 需要查询特定用户的调用历史，**When** 审计人员查询日志，**Then** 可以通过用户标识筛选出该用户的所有调用记录

---

### User Story 6 - 凭证安全存储 (Priority: P2)

作为安全管理员，我希望API Key在数据库中加密存储，这样即使数据库被非法访问，攻击者也无法获取明文凭证。

**Why this priority**: 凭证加密存储是防止凭据泄露的最后一道防线，满足安全最佳实践。

**Independent Test**: 可以通过直接查询数据库，验证存储的API Key是密文形式，无法被直接使用。

**Acceptance Scenarios**:

1. **Given** 管理员在数据库中查看凭据，**When** 查询API Key字段，**Then** 显示的是加密后的密文
2. **Given** 服务启动时，**When** 系统加载API Key，**Then** 系统正确解密并加载到内存供认证使用
3. **Given** 数据库备份文件被窃取，**When** 攻击者尝试读取API Key，**Then** 无法获得可用的明文凭证

---

### User Story 7 - 暴力破解防护 (Priority: P2)

作为安全管理员，我希望系统对连续认证失败进行限制，这样我可以防止攻击者通过暴力破解获取API Key。

**Why this priority**: 暴力破解防护是防止未授权访问的重要措施，保护系统免受凭证填充攻击。

**Independent Test**: 可以通过使用错误API Key连续尝试认证，验证系统自动封禁该来源IP。

**Acceptance Scenarios**:

1. **Given** 用户连续5次使用错误API Key，**When** 第6次尝试认证，**Then** 系统封禁该IP 15分钟
2. **Given** 用户IP被临时封禁，**When** 在封禁期内发送请求，**Then** 请求被拒绝，返回429错误
3. **Given** 封禁期满，**When** 用户使用正确API Key发送请求，**Then** 请求被正常处理

---

### Edge Cases

- 当API Key格式正确但对应的用户已被删除时，系统返回401而非500错误
- 当限流服务不可用时，系统采用fail-open策略：允许请求通过但记录严重警告
- 当检测到疑似暴力破解行为时，系统自动封禁IP 15分钟
- 当API Key即将过期时，系统提前7天通知用户
- 当审计日志存储满时，新日志覆盖旧日志（滚动策略）

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统必须对所有API请求进行身份认证，无有效凭证的请求必须被拒绝
- **FR-002**: 系统必须支持基于角色的访问控制（RBAC），不同角色有不同的模型访问权限
- **FR-003**: 系统必须对每个API Key实施流量限速，支持令牌桶算法
- **FR-004**: 系统必须对请求和响应中的敏感数据进行自动检测和脱敏
- **FR-005**: 系统必须记录所有API调用的完整审计日志
- **FR-006**: API Key在数据库中必须使用AES-256加密存储
- **FR-007**: 服务启动时必须从数据库加载加密的API Key并解密到内存
- **FR-008**: 系统必须支持IP黑名单机制，阻止来自黑名单IP的请求
- **FR-009**: 系统必须对认证失败进行次数限制，5次连续失败后自动封禁IP 15分钟
- **FR-010**: 审计日志必须包含：调用者标识、请求时间、请求内容（脱敏后）、响应状态、响应时间
- **FR-011**: 系统必须支持API Key过期提醒，提前7天通知用户
- **FR-012**: 系统必须支持fail-open和fail-close两种限流容错策略

### Key Entities

- **GatewayApiKey**: 网关访问凭证实体，用户调用网关的凭证，包含密钥标识(UUID)、加密密钥、用户关联、权限级别、限流配置、状态、过期时间
- **ProviderApiKey**: 提供商API密钥实体，存储加密的模型提供商凭证（OpenAI API Key、Anthropic API Key等），包含提供商标识、加密密钥、状态
- **User**: 用户实体，包含用户标识、名称、角色、状态、创建时间
- **AuditLog**: 审计日志实体，包含日志标识、用户标识、操作类型、请求内容、响应状态、时间戳
- **RateLimitConfig**: 限流配置实体，包含限流阈值、窗口大小、当前使用量
- **SensitiveDataRule**: 敏感数据规则实体，包含数据类型、脱敏规则、启用状态
- **IpBlocklist**: IP黑名单实体，包含IP地址、封禁原因、封禁开始时间、封禁结束时间

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100%的API请求必须经过身份认证，无有效凭证的请求必须在100ms内返回401错误
- **SC-002**: 权限检查的准确率达到100%，未授权访问必须在100ms内返回403错误
- **SC-003**: 限流机制必须有效防止资源滥用，超额请求必须在100ms内返回429错误
- **SC-004**: 敏感数据脱敏必须覆盖90%以上的常见敏感数据类型（手机号、身份证、银行卡等）
- **SC-005**: 审计日志必须100%记录所有API调用，支持按用户、时间、操作类型查询
- **SC-006**: API Key在数据库中必须100%加密存储，无法通过数据库直接查询获取明文
- **SC-007**: 暴力破解防护必须在5次连续失败后自动触发临时封禁，封禁时长为15分钟
- **SC-008**: 系统必须支持每秒处理10000次认证请求的能力
- **SC-009**: API Key过期提醒必须在过期前7天发送，不漏发

## Assumptions

- GatewayApiKey 格式：与 OpenAI 一致，`sk-` 前缀 + 32位加密随机字符串（如 `sk-xK9mP2vL8nQ4wF7hJ3dR6tB0yC5sE8gU`），共44字符
- ProviderApiKey 存储加密的提供商凭证（格式与提供商一致，如 OpenAI `sk-xxx`，Anthropic `sk-ant-xxx`）
- 限流阈值基于令牌桶算法，默认每分钟1000次请求
- 敏感数据脱敏使用预设规则，支持正则表达式自定义
- 审计日志存储采用滚动策略，保留最近90天数据
- API Key加密使用AES-256-GCM模式，密钥通过环境变量注入
- 暴力破解检测基于连续失败次数，5次失败后触发15分钟封禁
- 系统默认采用fail-open策略处理限流服务不可用的情况，但会记录严重警告
- fail-close策略在以下情况触发：Redis完全不可用且当前实例已处理>1000 QPS时
