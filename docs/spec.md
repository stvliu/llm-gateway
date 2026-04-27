# LLM-Gateway 项目需求规格说明书

> **文档版本**: v4.7
> **项目版本**: v1.0.0
> **生成日期**: 2026-04-18
> **状态**: 草案

---

## 目录

- [一、项目概述](#一项目概述)
- [二、竞品分析与差异化定位](#二竞品分析与差异化定位)
- [三、系统架构](#三系统架构)
- [四、核心功能需求](#四核心功能需求)
- [五、API 接口需求](#五api-接口需求)
- [六、数据模型需求](#六数据模型需求)
- [七、安全与合规需求](#七安全与合规需求)
- [八、可观测性需求](#八可观测性需求)
- [九、性能需求](#九性能需求)
- [十、部署与运维需求](#十部署与运维需求)
- [十一、非功能性需求](#十一非功能性需求)
  - [11.5 易用性需求](#115-易用性需求)
  - [11.6 性能基准补充需求](#116-性能基准补充需求)
  - [11.7 测试策略](#117-测试策略)
- [十二、版本规划](#十二版本规划)

---

## 一、项目概述

### 1.1 项目定位

**LLM-Gateway** 是新一代企业级 AI 模型 API 聚合分发与智能路由网关，定位为 APIPark 的竞品项目，专注于企业中台、大型团队和政企客户的需求。

### 1.2 核心价值主张

| 利益相关者 | 核心价值 |
|-----------|---------|
| **开发者** | 一次接入，通过标准 OpenAI/Anthropic API 调用 50+ 主流模型 |
| **架构师** | 团队隔离、零信任安全、全链路可观测、云原生部署 |
| **管理者** | Token限额控制、用量透明化、合规审计链、ROI 分析 |
| **运维人员** | K8s 原生、Prometheus/Grafana 集成、零停机升级 |

### 1.3 项目信息

| 项目 | 信息 |
|------|------|
| **项目名称** | LLM-Gateway |
| **当前版本** | v1.0.0 (规划) |
| **开发语言** | Java 21 |
| **核心框架** | Spring Boot 3.5.x + Spring MVC + 虚拟线程 |
| **数据库** | H2（默认），兼容 MySQL 8.0+ / PostgreSQL 14+ |
| **缓存** | 标准版：内存缓存；企业版：Redis 6.0+ |
| **默认端口** | 8080 |
| **开源协议** | 待定 (Apache-2.0 推荐) |

### 1.4 设计规模基准

| 维度 | v1.0 目标 | v2.0 目标 (水平扩展) |
|------|----------|---------------------|
| **用户数** | ~10,000 | ~100,000 |
| **Provider 数** | ~100 | ~500 |
| **模型数** | ~1,000 | ~5,000 |
| **QPS (单实例)** | 10,000 | 10,000 |
| **集群 QPS (10 节点)** | 100,000 | 500,000 |

### 1.5 术语表

| 术语 | 定义 |
|------|------|
| **Provider (提供商)** | 模型服务提供方，如 OpenAI、Anthropic、通义千问等 |
| **ProviderApiKey (Provider 调用凭证)** | 网关调用大模型 Provider 的凭据，管理员配置，支持多 Key 轮换 |
| **Model (模型)** | 具体的 AI 模型，如 gpt-4o、claude-sonnet-4 等 |
| **RouteGroup (路由分组)** | 路由策略配置，支持负载均衡和故障转移 |
| **RouteGroupProvider (路由关联)** | 路由分组与 Provider 的关联，含权重/优先级/健康状态 |
| **GatewayApiKey (网关访问凭证)** | 用户调用 LLM-Gateway 网关的凭据，用户自管理 |
| **TokenLimit (Token限额)** | 用户级别 Token 用量限额，支持周期重置（天/周/月/总量） |
| **Trace ID** | 请求全链路追踪的唯一标识 |
| **Request ID** | 单次 API 请求的唯一标识 |
| **PII (Personally Identifiable Information)** | 个人身份信息，如身份证号、手机号、邮箱等 |
| **SLA (Service Level Agreement)** | 服务级别协议，定义服务可用性指标 |
| **Feature Flag** | 功能开关，用于运行时控制功能开启/关闭 |

### 1.6 架构说明

**单租户设计**: LLM-Gateway v1.0 采用单租户架构，所有用户共享全局的 Provider 和路由配置。

**双 API Key 设计**:
- **ProviderApiKey**: 系统调用 Provider 的凭证，加密存储，支持多 Key 轮换
- **GatewayApiKey**: 用户调用网关的凭证，哈希存储，支持白名单控制 |

## Clarifications

### Session 2026-04-13
- Q: v1.0 设计目标应支撑的团队规模是多少？ → A: 中型规模（~100 团队、1000 渠道、10000 用户）
- Q: 当 API 发生破坏性变更时，迁移策略是什么？ → A: 双版本共存过渡期（新/旧版本并行 6 个月）
- Q: 多管理员并发修改同一配置时，冲突处理策略是什么？ → A: 乐观锁 + 最后写入胜出 + 变更通知

### Session 2026-04-13 (版本规划更新)

- Q: LLM-Gateway 的版本规划是什么？ → A: 单版本策略，聚焦标准版 API 网关核心功能

### Session 2026-14 (AI 高级特性补充)

- Q: 语义缓存的 Embedding 模型来源是什么？ → A: 使用上游 Provider 的 Embedding API（如 OpenAI text-embedding-3-small）
- Q: RAG 支持的向量数据库对接，首个版本应支持哪些向量数据库？ → A: 插件化 SPI，用户/社区自行实现适配器（网关提供 SPI 规范 + 1 个示例实现）

### Session 2026-04-21 (MVP 需求澄清)

- Q: API Key 加密算法选择 → A: AES-256-CBC（仅加密，无完整性验证，需额外 HMAC）
- Q: 密码重置方式 → A: 同时支持邮箱验证码和管理员重置
- Q: 前端技术栈 → A: React 19 + TypeScript + Ant Desing 5.x），参照 Ant Design Pro
- Q: 数据库选择 → A: 默认 H2，兼容 MySQL 8.0+ 和 PostgreSQL 14+（参照 jmix-crm）
- Q: 登录页面及前端页面设计 → A: 参照 jmix-2.8.0 及 jmix-crm 的登录页面和前端 UI 设计
- Q: 用户注册方式 → A: 同时支持邮箱注册和管理员创建用户
- Q: 缓存策略 → A: 标准版使用内存缓存，企业版使用 Redis 6.0+
- Q: 模型列表初始化方式 → A: 预填充（数据库初始化时自动导入 50+ 主流模型）

---

## 二、竞品分析与差异化定位

### 2.1 竞品基准对比

| 维度 | VoAPI | LLM-Gateway |
|------|-------|---------------|
| **目标用户** | 个人开发者/小团队/商业化运营 | 企业中台/大型团队/政企客户 |
| **部署模式** | 单机 Docker | K8s 云原生、集群部署 |
| **API 兼容** | OpenAI 兼容 | **OpenAI + Anthropic 双标准** |
| **多租户** | 简单用户体系 | 团队/成员/角色/权限隔离 |
| **规则引擎** | JS 脚本（用户自行编写） | 可视化策略编排（零代码）+ 脚本扩展 |
| **安全合规** | 基础认证/限流 | 零信任、国密算法、PII 脱敏、完整审计链 |
| **可观测性** | 基础监控看板 | OpenTelemetry 原生、全链路追踪 |
| **成本治理** | 简单计费 | 三级预算控制 + 超预算自动策略 |
| **扩展生态** | 云端规则市场（Pro 版） | 插件化架构 + MCP 协议原生支持 |
| **API 生命周期** | 基础调用 | 设计→发布→调用→下线完整流程 |
| **消费者订阅** | 无 | API Portal + 订阅审批机制 |
| **Prompt 封装** | 无 | 一键将 Prompt 封装为 REST API |
| **密钥池** | 无 | 单渠道多 API Key 自动轮换 |
| **MCP 支持** | 无 | 服务级 + 系统级 MCP |
| **数据脱敏** | PII 脱敏 | 增强：数据掩码策略 |

### 2.2 不做清单（明确排除）

以下功能为 VoAPI 的功能，但本项目**明确排除**或**不优先实现**：

| 功能 | 排除原因 |
|------|---------|
| 签到系统、用户等级体系 | 面向企业客户，非消费者运营场景 |
| 兑换码系统 | 企业客户通过合同/订单管理，非自助充值 |
| 邀请好友裂变营销 | 不适用企业级销售模式 |
| 自定义 SEO/主题色/全局样式 | 管理控制台使用统一 Design System |
| 训练场嵌入/自定义菜单 | 通过插件化扩展实现 |
| 每日签到/连续签到奖励 | 不适用企业场景 |
| 按模型计价/费用计算 | 定价是模型提供商的职责，网关仅负责 Token 用量的计量、跟踪、限额 |

---

## 三、系统架构

### 3.1 整体架构图

```
┌──────────────────────────────────────────────────────────────┐
│                        客户端层                               │
│  Claude Code / ChatGPT Next Web / 网关控制台 / 自研客户端      │
└────────────────────────────┬─────────────────────────────────┘
                             │ HTTP / SSE (OpenAI 或 Anthropic 格式)
┌────────────────────────────▼─────────────────────────────────┐
│                     LLM-Gateway 网关层                      │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │  认证中间件  │  │  策略引擎    │  │  智能路由分发    │    │
│  │  Token 验证  │  │  可视化编排  │  │  负载均衡        │    │
│  │  IP/UA 过滤  │  │  请求转换    │  │  故障转移        │    │
│  └─────────────┘  └──────────────┘  └──────────────────┘    │
│  ┌──────────────────────────────────────────────────────┐    │
│  │              协议转换与适配层                         │    │
│  │  OpenAI ↔ Anthropic ↔ Gemini ↔ 各厂商原生格式        │    │
│  └──────────────────────────────────────────────────────┘    │
│  ┌──────────────────────────────────────────────────────┐    │
│  │              可观测性 & 成本治理                      │    │
│  │  OpenTelemetry 追踪 | 预算控制 | 审计日志 | 指标采集  │    │
│  └──────────────────────────────────────────────────────┘    │
└────────────────────────────┬─────────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────────┐
│                     上游渠道层                               │
│  OpenAI / Anthropic / Gemini / 通义千问 / 文心 / DeepSeek   │
│  Ollama / 智谱 / 讯飞 / 百川 / Moonshot / 硅基流动 ...       │
└──────────────────────────────────────────────────────────────┘

数据持久化层:
  开发/调试用H2数据库，并兼容MySQL/PostgreSQL以支持生产环境部署 (业务数据)
  开发/调试用内存，生产环境用Redis (缓存 + 会话 + 分布式限流)
  
可观测性层:
  OpenTelemetry Collector
  Prometheus (指标采集)
  Grafana / Jaeger (可视化)
```

### 3.2 分层架构

```
(gateway-console)
┌─────────────────────┐
│   Web展现层/命令行   │  ←  React SPA (管理控制台)
┌─────────────────────┐
│   接口层             │  ← REST Controller( API),
├─────────────────────┤
│   应用层            │  ← GatewayOrchestrator, 策略编排服务
├─────────────────────┤
│   调度层            │  ← ModelRouter, TokenTracker, RateLimiter
├─────────────────────┤
│   服务层            │  ← ProviderService, RouteGroupService, TokenLimitService, AuditService
├─────────────────────┤
│   基础设施层        │  ← LLMProviderAdapter, DBRepository, RedisClient, OTelExporter
└─────────────────────┘
```

**约束**: 上层只能依赖下层接口，禁止跨层调用或反向依赖。

### 3.3 前端架构（gateway-console）

```
llm-gateway/
├── gateway-console/          # 前端管理控制台（独立项目）
│   ├── src/
│   │   ├── api/             # API 客户端（调用后端 REST API）
│   │   ├── components/       # 通用组件
│   │   ├── pages/           # 页面组件
│   │   ├── hooks/           # 自定义 Hooks
│   │   ├── store/           # 状态管理（Zustand/Jotai）
│   │   ├── i18n/            # 国际化
│   │   └── utils/           # 工具函数
│   └── package.json
├── gateway/                 # 后端网关（Java/Spring Boot）
└── ...
```

**前后端通信**：
- 后端提供 REST API，前端通过 API 客户端调用
- 认证：前端获取 JWT Token，携带在请求头中
- 实时数据：SSE 或 WebSocket 用于日志实时推送

**前端技术栈**：
| 技术 | 用途 |
|------|------|
| React 19 + TypeScript | UI 框架 |
| Ant Design 5.x | 组件库，参考 Ant Design Pro |
| React Router | 路由管理 |
| Zustand / Jotai | 状态管理 |
| Axios / Fetch | HTTP 客户端 |
| React Query / TanStack Query | 服务端状态 |
| i18next | 国际化 |
| Vite | 构建工具 |

### 3.4 技术栈

| 层级 | 技术选型 | 说明 |
|------|---------|------|
| **后端语言** | Java 21 | 虚拟线程、模式匹配、Record |
| **Web 框架** | Spring Boot 3.5.x + Spring MVC | 企业级标准生态 |
| **并发模型** | JDK 21 虚拟线程 | 轻量级高并发，无需响应式 |
| **ORM** | Spring Data JPA | 类型安全、编译期检查 |
| **缓存** | 标准版：内存缓存；企业版：Redis (Redssion 客户端) | 分布式限流、会话管理 |
| **数据库** | H2（默认），兼容 MySQL 8.0+ / PostgreSQL 14+ | 主从可选 |
| **日志** | Logback + SLF4J | 结构化 JSON 日志 |
| **可观测性** | Micrometer + OpenTelemetry | 指标、追踪、日志导出 |
| **API 文档** | SpringDoc OpenAPI (Swagger) | 自动生成 API 文档 |
| **安全** | Sa-Token | 认证、鉴权、OAuth2 |
| **构建工具** | Maven / Gradle | 多模块构建 |
| **前端** | React + TypeScript + Ant Design 组件 | 管理控制台，参考 Ant Design Pro |
| **容器化** | Docker + Kubernetes | 云原生部署 |

---

## 四、核心功能需求

### 4.1 功能全景矩阵

| 模块 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| **API 网关** | OpenAI 兼容端点 | P0 | `/v1/chat/completions` 等 |
| | Anthropic 兼容端点 | P0 | `/v1/messages` |
| | SSE 流式转发 | P0 | 实时双向流，首 token ≤100ms |
| | 协议转换 | P0 | OpenAI ↔ Anthropic 互转 |
| **API Portal** | API 门户 | P1 | 服务发布、浏览、订阅 | 企业版 |
| | 订阅审批 | P1 | 手动审批 + 自动审批 | 企业版 |
| | Prompt 封装 API | P1 | 一键将 Prompt 封装为 REST API | 企业版 |
| **渠道管理** | 渠道 CRUD | P0 | 创建/查询/编辑/删除 |
| | 渠道分组 | P0 | 按用途/价格分组 |
| | 多 Key 管理 | P0 | 单渠道多 API Key 自动轮换 |
| | 密钥池 | P0 | API Key 资源池，自动调度 |
| | 负载均衡 | P0 | 优先级 + 权重 |
| | 故障转移 | P0 | 自动重试 + 切换 |
| | 熔断超时 | P0 | 防雪崩 |
| | 代理配置 | P1 | HTTP/S、Socket5 |
| **智能路由** | 成本最优路由 | P1 | 按 Token 成本选择渠道 | Phase 2 |
| | 延迟最优路由 | P1 | 按历史延迟选择 | Phase 2 |
| | 场景路由 | P1 | 按场景(background/think/webSearch) |
| | 可视化策略编排 | P1 | 零代码策略配置 | 企业版 |
| | 自定义脚本扩展 | P1 | 高级用户自定义逻辑 | 企业版 |
| **团队管理** | 单租户 | P0 | 所有用户共享全局资源 |
| **密钥管理** | API Key CRUD | P0 | 创建/查询/编辑/删除 |
| | 额度限制 | P0 | API Key 用量上限 |
| | 模型白名单 | P0 | 限制可访问模型 |
| | IP 限制 | P1 | API Key 级 IP 白名单 |
| | 过期时间 | P0 | API Key 有效期 |
| **Token 计量** | Token 计量 | P0 | 输入/输出 Token 分别统计 |
| **Token额度** | Token限额 | P0 | 团队/用户/令牌/用户×渠道Token用量控制 |
| | 请求次数配额 | P1 | 时间周期内请求次数限制 |
| **安全与风控** | 认证中间件 | P0 | Token 验证 |
| | IP 白/黑名单 | P0 | 访问控制 |
| | UA 过滤 | P1 | User-Agent 过滤 |
| | PII 脱敏 | P0 | 自动检测并脱敏 |
| | 数据掩码策略 | P0 | 敏感数据过滤脱敏 |
| | 审计日志 | P0 | 全操作记录 |
| | 密钥加密存储 | P0 | AES-256 加密 |
| **可观测性** | Trace ID | P0 | 全链路追踪 |
| | 结构化日志 | P0 | JSON 格式 |
| | 实时指标 | P0 | 延迟/QPS/Token/费用 |
| | Prometheus 导出 | P0 | 标准指标格式 |
| | Grafana 仪表盘 | P1 | 预配置可视化 |
| | Jaeger 追踪 | P1 | 分布式追踪可视化 |
| **日志管理** | 调用日志 | P0 | API 调用元数据 |
| | 请求体日志 | P1 | 完整请求/响应 |
| | 操作日志 | P0 | 管理操作审计 |
| | 日志归档 | P1 | 定期归档清理 |
| **模型管理** | 模型目录 | P0 | 内置模型列表（50+） |
| | 远程同步 | P1 | 同步官方模型数据 |
| | 供应商管理 | P0 | 供应商 CRUD |
| **MCP 支持** | 系统级 MCP | P0 | 统一入口访问所有已发布服务 |
| | 服务级 MCP | P0 | 单个服务启用 MCP |
| **插件系统** | 插件框架 | P1 | SPI 机制 |
| | MCP 协议支持 | P1 | Model Context Protocol |
| **管理控制台** | 仪表盘 | P0 | 总体统计与趋势 |
| | API Portal 页面 | P1 | 服务发布、订阅管理 | 企业版 |
| | 渠道管理页面 | P0 | CRUD 操作界面 |
| | 策略编排页面 | P1 | 可视化策略配置 | 企业版 |
| | 用户管理页面 | P0 | 用户/角色/权限 |
| | 额度配置页面 | P1 | Token限额管理 | 企业版 |
| | 日志查询页面 | P0 | 多维度日志检索 |
| | 系统设置页面 | P0 | 全局配置 |

### 4.2 模块详细需求

---

#### 4.2.1 API 网关模块

**需求描述**: 提供标准 OpenAI 和 Anthropic API 兼容端点，接收客户端请求并转发到上游渠道。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| GW-001 | OpenAI 端点 | 实现 `/v1/chat/completions`、`/v1/completions`、`/v1/embeddings`、`/v1/images/generations`、`/v1/audio/*`、`/v1/moderations` | 通过 OpenAI 官方 SDK 调用测试 |
| GW-002 | Anthropic 端点 | 实现 `/v1/messages` | 通过 Anthropic 官方 SDK 调用测试 |
| GW-003 | SSE 流式转发 | 支持 `stream: true` 参数，实时转发 | 首 token 延迟 ≤100ms (P95) |
| GW-004 | 非流式响应 | 支持 `stream: false` 参数，等待完整响应后返回 | 响应体完整，无截断 |
| GW-005 | 请求验证 | 验证请求体格式、必填字段、枚举值 | 返回标准错误格式 |
| GW-006 | 错误响应格式 | 所有错误返回 `{"error": {"message": "...", "type": "...", "code": "..."}}` | 符合 OpenAI 错误规范 |
| GW-007 | CORS 支持 | 支持跨域请求配置 | 可配置允许的源、方法、头 |
| GW-008 | 请求超时 | 可配置的请求超时时间 | 默认 30 秒，可配置 |
| GW-009 | 大请求体支持 | 支持最大请求体大小限制 | 默认 10MB，可配置 |
| GW-010 | 请求 ID 生成 | 每个请求生成唯一 Request ID | UUID v4 格式 |

---

#### 4.2.2 协议转换模块

**需求描述**: 在 OpenAI 和 Anthropic 格式之间进行双向转换，确保上游客户端无需感知下游格式差异。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| PT-001 | OpenAI → Anthropic | 将 OpenAI 请求转换为 Anthropic 格式 | 参数映射准确率 ≥99.9% |
| PT-002 | Anthropic → OpenAI | 将 Anthropic 请求转换为 OpenAI 格式 | 参数映射准确率 ≥99.9% |
| PT-003 | Tool Use 双向映射 | Function Calling ↔ tool_use 互转 | 完整支持多轮工具调用 |
| PT-004 | 多模态转换 | 图片/文件内容格式转换 | 支持 base64 和 URL 引用 |
| PT-005 | 流式事件转换 | SSE 事件格式实时转换 | 转换延迟 ≤5ms (P95) |
| PT-006 | Thinking/Reasoning 转换 | 推理过程标记转换 | 兼容不同厂商的 reasoning 标记 |
| PT-007 | 直通转发优化 | 下游为 Anthropic 兼容端点时零转换转发 | 延迟增加 ≤5ms (P95) |
| PT-008 | 响应元数据透传 | 传递 usage、finish_reason 等元数据 | 数据完整性 100% |

**转换矩阵**:

| 源格式 | 目标格式 | 转换内容 |
|--------|---------|---------|
| OpenAI | Anthropic | messages → messages, tools → tools, temperature → temperature, max_tokens → max_tokens |
| Anthropic | OpenAI | messages → messages, tools → functions, system → system message |
| 各厂商 | OpenAI | 统一转换为 OpenAI 标准格式 |
| 各厂商 | Anthropic | 统一转换为 Anthropic 标准格式 |

---

#### 4.2.3 API Portal 模块

**需求描述**: API 门户允许用户将服务发布到门户供其他用户浏览、订阅和调用。支持订阅审批机制确保服务安全可控。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| AP-001 | 服务发布 | 将 AI 服务发布到 API Portal | 自动生成 API 文档 |
| AP-002 | 服务浏览 | 用户可在门户浏览所有已发布服务 | 支持分类、搜索 |
| AP-003 | 服务订阅 | 用户订阅感兴趣的服务 | 支持立即订阅或申请审批 |
| AP-004 | 订阅审批 | 需要审批的服务需管理员审批 | 审批通过后方可调用 |
| AP-005 | 自动审批 | 可配置哪些服务自动审批 | 白名单机制 |
| AP-006 | API 文档 | 自动生成 OpenAPI 格式文档 | 可在线预览和下载 |
| AP-007 | 服务版本 | 服务更新需发布新版本 | 支持版本对比 |
| AP-008 | 服务下架 | 服务可下架但不影响已订阅用户 | 已订阅用户继续可用 |

---

#### 4.2.4 Prompt 封装模块

**需求描述**: 一键将 Prompt 和 AI 模型封装为标准 REST API，无需编写代码即可创建 AI API。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| PP-001 | Prompt 定义 | 定义 Prompt 模板和变量占位符 | 支持 `{{variable}}` 语法 |
| PP-002 | 变量配置 | 配置 Prompt 变量的类型、默认值、验证规则 | 支持必填/可选 |
| PP-003 | 模型选择 | 选择用于处理请求的 AI 模型 | 支持多模型切换 |
| PP-004 | API 生成 | 自动生成 REST API 端点 | 端点格式可自定义 |
| PP-005 | 超时配置 | 设置 API 请求超时时间 | 默认 60 秒 |
| PP-006 | 重试配置 | 配置请求失败时的最大重试次数 | 默认 3 次 |
| PP-007 | 响应格式 | 配置 API 响应格式（全文/结构化JSON） | 支持模板自定义 |
| PP-008 | 批量处理 | 支持批量请求处理 | 批量大小可配置 |

---

#### 4.2.5 密钥池管理模块

**需求描述**: API Key 资源池管理，支持同一渠道下多个 API Key 的自动调度和故障隔离。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| KP-001 | Key 池 | 单渠道支持多个 API Key | 系统自动管理可用 Key |
| KP-002 | Key 调度 | 根据配置策略自动选择可用 Key | 支持轮询、随机、加权 |
| KP-003 | Key 健康检查 | 定期检查 Key 可用性 | 自动禁用失效 Key |
| KP-004 | Key 限流 | 每个 Key 独立 RPM/TPM 限制 | 精确控制单个 Key 用量 |
| KP-005 | Key 切换 | Key 失效时自动切换到备用 Key | 切换延迟 ≤100ms |
| KP-006 | Key 恢复 | 被禁用的 Key 定期恢复检查 | 可配置恢复间隔 |
| KP-007 | Key 统计 | 统计每个 Key 的使用量/错误率 | 可视化展示 |

---

#### 4.2.6 MCP 支持模块

**需求描述**: Model Context Protocol (MCP) 协议支持，提供系统级和服务级 MCP 端点。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| MC-001 | 系统级 MCP | 统一入口访问所有已发布服务 | `/openapi/v1/mcp/global/sse` |
| MC-002 | 服务级 MCP | 单个服务启用 MCP | 每个服务独立 MCP 端点 |
| MC-003 | MCP 消费者隔离 | 消费者级 MCP 只能访问已订阅服务 | 权限隔离 |
| MC-004 | MCP 认证 | 通过 API Key 认证 MCP 访问 | 复用现有认证体系 |
| MC-005 | MCP 工具发现 | 客户端可发现可用工具列表 | 符合 MCP 标准协议 |
| MC-006 | MCP SSE | 支持 Server-Sent Events 传输 | 长连接支持 |
| MC-007 | MCP 配置 | 提供标准 MCP 配置文件 | 支持 Claude/Cursor/Dify 等客户端 |

---

#### 4.2.7 渠道管理模块

**需求描述**: 管理通往各模型提供商的渠道实例，支持多渠道、多 Key、分组、负载均衡。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| CH-001 | 渠道创建 | 创建渠道，配置 Provider、BaseURL、API Key、模型列表 | API Key 加密存储 |
| CH-002 | 渠道编辑 | 修改渠道配置 | 热加载，无需重启 |
| CH-003 | 渠道删除 | 软删除渠道 | 不影响进行中请求 |
| CH-004 | 渠道查询 | 按条件查询渠道列表 | 支持分页、排序 |
| CH-005 | 渠道测试 | 测试渠道连通性 | 返回测试结果 |
| CH-006 | 渠道分组 | 创建/编辑/删除渠道分组 | 支持按组路由 |
| CH-007 | 多 Key 管理 | 单渠道添加/删除/禁用多个 API Key | Key 级故障隔离 |
| CH-008 | 优先级设置 | 设置渠道优先级 | 高优先级优先使用 |
| CH-009 | 权重设置 | 同优先级按权重分配流量 | 流量分配符合权重比例 |
| CH-010 | 渠道限流 (RPM) | 设置渠道每分钟最大请求数 | 超限时返回 429 |
| CH-011 | 密钥级限流 (RPM) | 单 Key 每分钟最大请求数 | Key 级精确限流 |
| CH-012 | 密钥错误禁用 | API Key 无效时自动禁用 | 可禁用单 Key 或模型 |
| CH-013 | 自动恢复 | 密钥被禁用后定时恢复 | 可配置恢复间隔 |
| CH-014 | 请求熔断 | 连续失败时熔断渠道 | 可配置熔断阈值 |
| CH-015 | 超时保护 | 请求超时自动终止 | 默认 30 秒 |
| CH-016 | 自动重试 | 请求失败自动重试其他渠道 | 可配置重试次数 |
| CH-017 | 代理配置 | 配置全局或渠道级代理 | 支持 HTTP/S、Socket5 |
| CH-018 | IP/UA 规则 | 配置渠道级 IP/UA 访问规则 | 支持白/黑名单 |
| CH-019 | 余额同步 | 从上游渠道同步余额 | 支持手动/定时同步 |
| CH-020 | 渠道健康检查 | 定期检查渠道可用性 | 可配置检查间隔 |

---

#### 4.2.8 智能路由模块

**需求描述**: 根据策略配置，智能选择最优渠道和模型。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| RT-001 | 成本最优路由 | 选择当前成本最低的可用渠道（Phase 2） | Token 成本计算准确率 ≥99.9% |
| RT-002 | 延迟最优路由 | 选择历史延迟最低的渠道 | P95 延迟优化 ≥20% |
| RT-003 | 负载均衡 | 按权重分配流量到组内渠道 | 流量分配符合权重 |
| RT-004 | 故障转移 | 主渠道失败自动切换备用 | 切换延迟 ≤100ms |
| RT-005 | 场景路由 | 按场景(background/think/webSearch)选择模型 | 场景识别准确率 ≥95% |
| RT-006 | 可视化策略编排 | 通过 UI 拖拽配置路由策略 | 零代码，所见即所得 |
| RT-007 | 策略热加载 | 策略变更即时生效 | 0 秒延迟，不影响进行中请求 |
| RT-008 | 策略版本管理 | 策略变更历史记录与回滚 | 支持版本对比 |
| RT-009 | 自定义脚本扩展 | 高级用户通过脚本自定义路由逻辑 | 沙箱隔离，超时保护 |
| RT-010 | 模型映射 | 将请求模型名映射到实际可用模型 | 支持正则/前缀匹配 |

**策略编排器**:

提供可视化策略编排界面，支持以下节点类型：
- **条件节点**: if/else 条件分支（按模型、用户、Token 量、时间段等）
- **路由节点**: 选择渠道分组或具体渠道
- **转换节点**: 修改请求参数（temperature、max_tokens 等）
- **限流节点**: 设置 RPM/TPM 阈值
- **限额节点**: 检查Token余额
- **降级节点**: 额度不足或超限时切换到廉价模型

---

#### 4.2.9 用户与角色管理模块

**需求描述**: 单租户架构，用户共享全局 Provider/RouteGroup 资源，支持基于角色的访问控制。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| UM-001 | 用户管理 | 用户注册/编辑/禁用/删除 | 支持批量操作 |
| UM-002 | 角色管理 | 创建/编辑/删除角色（系统级） | 预设角色模板 |
| UM-003 | 权限管理 | 为角色分配权限 | 基于权限码的细粒度控制 |
| UM-004 | 角色绑定 | 将角色绑定到用户 | 支持多角色 |
| UM-005 | 用户登录 | 用户名密码、邮箱验证码 | 密码 BCrypt 加密 |
| UM-006 | OAuth 登录 | GitHub / Gitee / 企业 SSO | OAuth2 标准协议 |

**预设角色**:

| 角色 | 权限范围 |
|------|---------|
| **管理员** | 系统全部权限 |
| **开发者** | 创建 API Key、查看日志、调用 API |
| **观察者** | 仅查看用量和日志 |
| **财务管理员** | 额度配置、用量查看 |

---

#### 4.2.10 API 密钥管理模块

**需求描述**: 管理 API 调用密钥（API Key），支持额度限制、模型白名单、IP 限制。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| AK-001 | API Key 创建 | 生成 API Key，关联用户/团队 | Key 格式: `sk-xxxxxxxxxxxxxxxx` |
| AK-002 | API Key 编辑 | 修改额度、模型白名单等 | 热加载 |
| AK-003 | API Key 删除 | 软删除 API Key | 立即失效 |
| AK-004 | API Key 查询 | 按条件查询 API Key 列表 | 支持分页 |
| AK-005 | 额度限制 | 设置 API Key 最大使用额度 | 超限拒绝 |
| AK-006 | 模型白名单 | 限制 API Key 可访问的模型 | 未授权模型返回 403 |
| AK-007 | IP 白名单 | 限制 API Key 可用 IP 范围 | 支持 CIDR 格式 |
| AK-008 | 过期时间 | 设置 API Key 有效期 | 过期自动失效 |
| AK-009 | 用量统计 | 实时查看 API Key 已用/剩余额度 | 按天/周/月聚合 |
| AK-010 | 批量创建 | 一次性创建多个 API Key | 支持导出 |
| AK-011 | API Key 刷新 | 重新生成 Key（旧 Key 保留宽限期） | 宽限期可配置 |

---

#### 4.2.11 Token额度模块

**需求描述**: Token 用量限额控制，四级 Token 限额（团队/用户/API Key/用户×渠道）+ 请求次数配额。网关仅跟踪 Token 使用量，不计算实际费用。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 | 版本 |
|----|------|---------|---------|------|
| BL-001 | Token 计量 | 分别统计输入/输出 Token | 准确率 ≥99.9% | 标准版 |
| BL-002 | 团队级限额 | 设置团队级Token限额 | 超限触发策略 | 标准版 |
| BL-003 | 用户级限额 | 设置用户级Token限额 | 总和不超过团队限额 |
| BL-004 | API Key 级限额 | 设置 API Key 级 Token 限额 | 超限拒绝 |
| BL-005 | 用户×渠道限额 | 设置用户×渠道Token限额 | 精细化配额控制 |
| BL-006 | 预扣额度 | 请求前预扣预估Token量 | 防止超额使用 |
| BL-007 | 差额调整 | 响应后按实际 Token 量调整使用量 | 多退少补 |
| BL-008 | 超限策略 | 拒绝/降级模型 | 策略可配置 |
| BL-009 | 额度报表 | 按团队/用户/API Key 生成用量报表 | 支持导出 CSV |
| BL-010 | 限额告警 | Token使用达到阈值时告警 | 如 80%、100% |
| BL-011 | 请求次数配额 | 按时间周期限制请求次数 | 支持 RPM/TPM 外的补充限制 |

**Token限额层级关系**:

```
用户Token限额
├── 用户级别: User (max_tokens)
├── Provider级别: User × Provider (可选)
└── Model级别: User × Model (可选)

周期类型:
├── DAILY:   每天重置 (如: 每天最多 100,000 tokens)
├── WEEKLY:  每周重置 (如: 每周一重置，每周 500,000 tokens)
├── MONTHLY: 每月重置 (如: 每月1日重置，每月 2,000,000 tokens)
└── TOTAL:   不重置 (累计总量限制)
```

---

#### 4.2.12 安全与风控模块

**需求描述**: 零信任安全架构，四层安全检查。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| SC-001 | Token 认证 | API 请求必须携带有效 Token | Bearer Token 标准 |
| SC-002 | IP 白/黑名单 | 系统级 IP 访问控制 | 支持 CIDR |
| SC-003 | UA 过滤 | 按 User-Agent 允许/拒绝 | 支持正则匹配 |
| SC-004 | PII 脱敏 | 自动检测并脱敏个人身份信息 | 检测准确率 ≥95% |
| SC-005 | 敏感词过滤 | 内置敏感词库，可自定义 | 拦截命中率 ≥99% |
| SC-006 | 密钥加密存储 | API Key AES-256 加密存储 | 密钥不可逆 |
| SC-007 | 密钥掩码展示 | API Key 显示为 `sk-****abcd` | 掩码规则统一 |
| SC-008 | 审计日志 | 记录所有管理操作 | 不可篡改 |
| SC-009 | 请求体日志脱敏 | 日志中不记录 API Key 等敏感信息 | 自动脱敏 |
| SC-010 | 密码策略 | 密码强度要求、过期时间 | 符合 OWASP 标准 |
| SC-011 | 登录失败锁定 | 连续失败 N 次后锁定账户 | 可配置阈值 |
| SC-012 | 国密算法支持 | SM2/SM3/SM4 算法支持 | 等保合规 |

---

#### 4.2.13 可观测性模块

**需求描述**: OpenTelemetry 原生，全链路可观测。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| OB-001 | Trace ID 生成 | 每个请求生成唯一 Trace ID | 贯穿全链路 |
| OB-002 | 分布式追踪 | 追踪网关→认证→路由→渠道调用→响应 | 支持 Jaeger/Zipkin |
| OB-003 | 结构化日志 | JSON 格式日志，包含元数据 | 必须字段: trace_id, request_id, user_id, model |
| OB-004 | 实时指标采集 | 延迟(P50/P95/P99)、QPS、成功率、Token 消耗 | 采集间隔 ≤1s |
| OB-005 | Prometheus 导出 | 标准 Prometheus 指标格式 | `/metrics` 端点 |
| OB-006 | 渠道延迟监控 | 每个渠道的实时延迟 | 历史趋势图 |
| OB-007 | 错误率监控 | 渠道/模型级错误率 | 阈值告警 |
| OB-008 | 预算使用率 | 实时显示各层级预算使用情况 | 可视化仪表盘 |
| OB-009 | 仪表盘 | 内置 Grafana 仪表盘模板 | 开箱即用 |
| OB-010 | 日志查询 | 多维度日志检索 | 按时间/用户/模型/渠道过滤 |

**核心指标清单**:

| 指标名 | 类型 | 标签 | 说明 |
|--------|------|------|------|
| `http_request_duration_seconds` | Histogram | method, path, status | 请求延迟 |
| `http_requests_total` | Counter | method, path, status | 请求总数 |
| `llm_token_total` | Counter | model, type(input/output), tenant | Token 消耗 |
| `llm_channel_latency_seconds` | Histogram | channel, model | 渠道延迟 |
| `llm_channel_errors_total` | Counter | channel, error_type | 渠道错误 |
| `llm_budget_usage_ratio` | Gauge | tenant, project, user | 预算使用率 |
| `llm_active_users` | Gauge | tenant | 活跃用户数 |

---

#### 4.2.14 日志管理模块

**需求描述**: 结构化日志，支持归档和检索。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| LG-001 | 调用日志 | 记录每次 API 调用元数据 | 用户、模型、Token 量、费用、状态码 |
| LG-002 | 请求体日志 | 可选记录完整请求/响应体 | 默认关闭，可配置开启 |
| LG-003 | 操作日志 | 记录管理后台操作 | 操作人、时间、动作、结果 |
| LG-004 | 日志归档 | 按天/周/月归档历史数据到冷存储 | 可配置策略 |
| LG-005 | 日志清理 | 定期清理过期日志 | 保留期限可配置 |

---

#### 4.2.15 模型管理模块

**需求描述**: 管理系统支持的模型目录和供应商信息。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| MD-001 | 模型目录 | 内置支持的模型列表 | 包含 50+ 模型 |
| MD-002 | 模型详情 | 模型能力描述（上下文长度、多模态等） | 对外 API 可查询 |
| MD-003 | 供应商管理 | 供应商 CRUD | 名称、官网、API 文档链接 |
| MD-004 | 远程同步 | 同步官方模型/供应商数据 | 一键同步 |
| MD-005 | 自定义模型 | 用户自定义模型接入 | 通过渠道配置 |

**内置模型目录**（首批）:

| 提供商 | 模型 | 上下文长度 | 多模态 | Tool Use |
|--------|------|-----------|--------|----------|
| OpenAI | gpt-4o | 128K | ✅ | ✅ |
| OpenAI | gpt-4-turbo | 128K | ✅ | ✅ |
| OpenAI | gpt-3.5-turbo | 16K | ❌ | ✅ |
| Anthropic | claude-sonnet-4-20250514 | 200K | ✅ | ✅ |
| Anthropic | claude-opus-4-20250514 | 200K | ✅ | ✅ |
| Google | gemini-2.5-pro | 1M | ✅ | ✅ |
| 通义千问 | qwen-max | 32K | ✅ | ✅ |
| 智谱 AI | glm-4 | 128K | ✅ | ✅ |
| DeepSeek | deepseek-v3 | 128K | ❌ | ✅ |

---

#### 4.2.16 插件系统模块

**需求描述**: 插件化架构，支持 MCP 协议与 Agent 工具市场。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| PL-001 | 插件框架 | SPI 机制加载插件 | 无需修改核心代码 |
| PL-002 | 插件生命周期 | 安装/启用/禁用/卸载 | 热加载 |
| PL-003 | MCP 协议支持 | Model Context Protocol 服务分发 | 兼容 MCP 标准 |
| PL-004 | 插件市场 | 内置/社区插件浏览安装 | P2 版本 |

---

#### 4.2.17 Agent 工具市场模块

**需求描述**: 为企业提供可复用的 AI Agent 工具库，支持 Tool Use / Function Calling 场景下的工具注册、编排与分发。

**核心概念**:

```
Agent 工具市场 = 工具注册中心 + 工具编排引擎 + 工具分发网关

┌─────────────────────────────────────────────────┐
│              Agent 工具市场                      │
│  ┌─────────────┐  ┌──────────────┐             │
│  │  工具注册    │  │  工具编排    │             │
│  │  描述/Schema │  │  组合/链式   │             │
│  └─────────────┘  └──────────────┘             │
│  ┌─────────────┐  ┌──────────────┐             │
│  │  工具分发    │  │  用量统计    │             │
│  │  按需注入    │  │  调用/费用   │             │
│  └─────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────┘
         │                          │
         ▼                          ▼
   请求携带工具列表          Claude Code 调用工具
```

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| AG-001 | 工具注册 | 注册 Agent 工具（名称、描述、参数 Schema、端点 URL） | 兼容 OpenAI Function Calling 格式 |
| AG-002 | 工具分类 | 按用途分类管理（搜索/计算/数据库/API 调用等） | 支持标签体系 |
| AG-003 | 工具编排 | 将多个工具组合为工作流（如：搜索→总结→写邮件） | 支持顺序/并行/条件分支 |
| AG-004 | 工具发现 | 客户端可查询可用工具列表及 Schema | REST API 可查询 |
| AG-005 | 工具注入 | 根据请求上下文自动注入相关工具到模型调用 | 支持手动绑定/自动匹配 |
| AG-006 | 工具调用代理 | 代理转发模型的工具调用请求到实际工具端点 | 支持 HTTP/Webhook |
| AG-007 | 工具响应回传 | 将工具执行结果回传给模型 | 格式兼容 Function Calling 规范 |
| AG-008 | 工具用量统计 | 统计每个工具的调用次数、延迟、费用 | 按组织/项目/用户聚合 |
| AG-009 | 工具权限控制 | 控制哪些用户/项目可使用哪些工具 | 基于角色的工具授权 |
| AG-010 | 内置工具库 | 预置常用工具（网页搜索、代码执行、计算器、数据库查询等） | 开箱即用 ≥10 个内置工具 |
| AG-011 | 自定义工具 | 用户通过 HTTP 端点注册自定义工具 | 支持任何 HTTP 可访问服务 |
| AG-012 | 工具版本管理 | 工具支持多版本共存与灰度切换 | 版本化 Schema |
| AG-013 | 工具沙箱执行 | 内置工具（如代码执行）在沙箱中运行 | 隔离文件系统/网络访问 |
| AG-014 | 工具错误处理 | 工具执行失败时的降级与重试策略 | 可配置重试/跳过/终止 |
| AG-015 | 工具市场 UI | 可视化工具浏览、搜索、安装界面 | 管理控制台集成 |

**内置工具清单**（v1.0 首批）:

| 工具名称 | 类型 | 说明 |
|---------|------|------|
| Web Search | 搜索 | 调用搜索引擎获取实时信息 |
| Code Executor | 计算 | 执行 Python/JavaScript 代码 |
| Calculator | 计算 | 数学表达式计算 |
| SQL Runner | 数据库 | 执行 SQL 查询（只读模式） |
| HTTP Request | API 调用 | 发起 HTTP 请求到外部服务 |
| File Reader | 文件 | 读取指定文件内容 |
| Time/Date | 系统 | 获取当前时间/时区信息 |
| JSON Parser | 工具 | 解析/格式化 JSON 数据 |
| Text Summarizer | AI | 调用指定模型进行文本摘要 |
| Diff Checker | 工具 | 比较两段文本差异 |

**典型使用场景**:

```
场景 1: Claude Code 调用自定义工具
  Claude Code → POST /v1/messages (携带 tool_use)
      → LLM-Gateway 路由到对应渠道
      → 模型返回 tool_use 请求
      → LLM-Gateway 代理到注册的自定义工具端点
      → 工具执行结果回传给模型
      → 模型生成最终响应

场景 2: 工具编排工作流
  用户请求: "帮我分析这个 URL 的内容并写一份报告"
      → 工具编排引擎解析为:
          1. HTTP Request 获取 URL 内容
          2. Text Summarizer 生成摘要
          3. File Writer 保存为报告文件
      → 按顺序执行，最终结果返回给用户
```

---

#### 4.2.18 语义缓存模块（AI 高级特性）

**需求描述**: 对语义相似的 prompt 识别并返回缓存答案，降低延迟和上游调用成本。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| CM-001 | 向量化缓存 | 使用 Embedding 模型将 prompt 转为向量，存储到向量缓存中 | 相似度阈值可配置（默认 ≥0.95） |
| CM-002 | 语义匹配 | 新请求到来时计算向量相似度，匹配历史缓存 | 匹配延迟 ≤10ms (P95) |
| CM-003 | 缓存 TTL | 缓存可设置过期时间，避免返回过时答案 | 默认 TTL 1 小时，可配置 |
| CM-004 | 缓存命中统计 | 统计缓存命中率、节省的 Token 数量和费用 | 实时仪表盘可展示 |
| CM-005 | 缓存作用域 | 缓存可按租户/项目隔离 | 跨租户缓存不共享 |
| CM-006 | 缓存失效 | 支持手动清除缓存条目 | API 接口和 UI 可操作 |
| CM-007 | 流式缓存绕过 | 流式请求可配置是否跳过缓存 | 默认流式不缓存（避免缓存不完整内容） |

**工作流程**:
```
请求进入
    │
    ▼
计算 Prompt Embedding
    │
    ▼
向量相似度查询缓存
    │
    ├── 匹配 (相似度 ≥ 阈值) ──▶ 返回缓存响应
    │                                  │
    │                                  ▼
    │                           记录缓存命中指标
    │
    └── 不匹配 ──────────────▶ 正常路由到上游
                                       │
                                       ▼
                                响应存储到缓存（非流式）
```

---

#### 4.2.19 Prompt 工程模块（AI 高级特性）

**需求描述**: 在网关层面提供提示模板、装饰器和系统指令约束能力，确保 AI 输出一致性和合规性。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| PE-001 | Prompt 模板 | 在网关定义标准提示模板（Markdown + 变量占位符） | 支持 `{{variable}}` 变量替换 |
| PE-002 | Prompt 装饰器 | 在用户提示前后自动附加内容（企业声明、格式要求等） | 可配置前置装饰器和后置装饰器 |
| PE-003 | 系统指令约束 | 统一附加系统指令（system message），约束模型行为 | 所有请求自动注入，不可被用户覆盖 |
| PE-004 | 模板变量注入 | 根据用户属性/租户配置动态填充模板变量 | 支持 `{{user_name}}`, `{{tenant_name}}`, `{{date}}` 等 |
| PE-005 | 请求改写 | 检测到不当请求时自动改写（如去除敏感内容） | 改写规则可配置 |
| PE-006 | 请求拒绝 | 检测到违规请求时拒绝并返回标准错误 | 拒绝规则与内容审核联动 |
| PE-007 | 模板版本管理 | Prompt 模板支持版本化管理，可回滚 | 版本切换不影响进行中请求 |
| PE-008 | 模板共享 | 跨项目/组织共享 Prompt 模板 | 需显式授权 |

**典型应用场景**:
- 企业统一品牌：在所有 AI 响应末尾附加企业品牌声明
- 格式约束：强制模型输出 JSON 格式
- 敏感内容防护：自动过滤用户输入中的 API Key、身份证号等
- 多语言适配：根据用户语言偏好附加对应的系统指令

---

#### 4.2.20 双向内容审核模块（安全增强）

**需求描述**: 在请求进入模型和响应返回客户端两个方向进行内容安全检查，防止不当输入和输出造成合规风险。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| CS-001 | 请求侧内容审核 | 检测用户输入中的敏感内容（暴力、仇恨、色情等） | 支持多级审核策略 |
| CS-002 | 响应侧内容审核 | 检测模型返回内容中的不当输出 | 支持流式响应实时检测 |
| CS-003 | 提示词检查（Prompt Guard） | 检测 Prompt 注入攻击、越权指令、模型越狱 | 内置 OWASP Top 10 for LLM 规则集 |
| CS-004 | 大模型攻击防护 | 防御 Prompt 泄露、系统指令覆盖、角色扮演攻击 | 检测准确率 ≥95% |
| CS-005 | 自定义审核规则 | 用户可添加自定义审核规则（关键词、正则、AI 模型判断） | 规则热加载 |
| CS-006 | 审核动作 | 检测到违规内容时可执行：拦截/改写/标记/记录 | 动作可配置 |
| CS-007 | 审核日志 | 记录所有审核事件（通过/拦截/改写），含匹配规则 | 独立审计日志表 |
| CS-008 | 流式响应逐块检测 | 流式输出逐块进行内容检测，不等待完整响应 | 检测延迟 ≤5ms/块 |

**审核管道**:
```
请求侧:  用户输入 ──▶ PII 脱敏 ──▶ 内容审核 ──▶ Prompt Guard ──▶ 路由到上游
                                                          │
响应侧:  上游响应 ◀── 结果返回 ◀── 内容审核 ◀── 流式检测 ◀──┘
```

---

#### 4.2.21 RAG 支持模块（P2）

**需求描述**: 网关级 RAG（检索增强生成）管道，在请求流经网关时自动检索知识库并扩充到 prompt 上下文。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| RG-001 | 知识库集成 | 定义向量数据库 SPI 接口，用户/社区自行实现适配器 | 提供 SPI 规范文档 + 1 个示例实现 |
| RG-002 | 自动检索 | 根据用户问题自动检索相关知识片段 | 检索延迟 ≤50ms (P95) |
| RG-003 | 上下文注入 | 将检索结果注入 prompt 上下文（系统消息或用户消息） | 上下文长度不超过模型上下文限制 |
| RG-004 | RAG 策略配置 | 可配置 RAG 触发条件（哪些模型/哪些请求启用 RAG） | 按模型/用户/项目配置 |
| RG-005 | 结果排序 | 控制注入的知识片段数量和相关性阈值 | 可配置 top-k 和最小相似度 |
| RG-006 | 缓存集成 | RAG 检索结果可缓存，相同问题不重复检索 | 缓存 TTL 可配置 |

**典型使用场景**:
- 企业内部知识问答：员工提问时自动检索企业内部文档
- 客服场景：根据客户问题检索产品文档和 FAQ
- 代码助手：根据代码问题检索内部代码库和最佳实践

---

#### 4.2.22 会话上下文缓存模块（P2）

**需求描述**: 提供会话级别的上下文缓存，支持对话式 AI 应用维护对话历史和聊天记忆。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| SX-001 | 会话 ID 管理 | 每个对话会话关联唯一会话 ID | 通过 `session_id` 请求参数或 Header 传递 |
| SX-002 | 对话历史缓存 | 缓存最近 N 轮对话历史（消息角色+内容） | 默认缓存最近 10 轮，可配置 |
| SX-003 | 自动上下文注入 | 新请求到来时自动注入历史对话上下文 | 注入后总 Token 数不超过模型上下文限制 |
| SX-004 | 会话 TTL | 会话超时自动清理 | 默认 30 分钟无活动，可配置 |
| SX-005 | 会话存储 | 会话数据存储在 Redis（热）或数据库（冷） | Redis 优先，过期后降级到 DB |
| SX-006 | 跨渠道会话 | 同一会话可跨模型/渠道继续 | 用户切换模型时保持对话连续 |

---

#### 4.2.23 多语言 SDK 模块（P2）

**需求描述**: 封装对统一网关 API 的调用，为开发者提供便捷的多语言客户端库。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| SDK-001 | Python SDK | `pip install ai-gateway-sdk`，兼容 OpenAI Python SDK 接口 | 支持同步和异步调用 |
| SDK-002 | TypeScript SDK | `npm install @ai-gateway/sdk`，兼容 OpenAI Node SDK 接口 | 支持 Promise 和 Stream |
| SDK-003 | Go SDK | `go get github.com/ai-gateway/sdk-go` | 支持 context 和流式 |
| SDK-004 | 自动认证 | SDK 自动处理 API Key 注入和 Token 刷新 | 开发者只需设置 `api_key` |
| SDK-005 | 错误处理 | SDK 统一错误类型和重试逻辑 | 自动重试（网络错误），不重试（业务错误） |
| SDK-006 | 文档 | 各语言 SDK 附带 API 文档和示例代码 | README + 示例项目 |

---

#### 4.2.24 国际化模块

**需求描述**: 系统支持中文和英文双语，包括管理控制台界面、API 文档、错误消息、邮件通知等。

**后端实现（Spring Boot）**：
```
gateway/src/main/resources/
├── messages/
│   ├── messages.properties        # 默认（中文）
│   ├── messages_en.properties   # 英文
│   └── messages_zh.properties   # 中文（显式）
└── i18n/
    └── I18nConfig.java         # 国际化配置
```

**后端实现细节**：
| 组件 | 技术方案 |
|------|----------|
| 消息解析 | Spring MessageSource + JDK MessageFormat |
| 语言检测 | `Accept-Language` Header → 用户偏好 → 默认 |
| 错误消息 | `@ExceptionHandler` 统一返回国际化消息 |
| 邮件模板 | Thymeleaf 模板 + Locale 动态切换 |

**后端翻译范围**：
- API 错误响应消息
- 验证异常消息
- 邮件通知内容
- 回调消息（如 Webhook 通知）

**gateway-console 前端实现**：
```
gateway-console/src/i18n/
├── locales/
│   ├── en.json    # 英文翻译
│   └── zh.json    # 中文翻译
├── index.ts       # i18next 配置
└── keys.ts       # 翻译 key 常量（类型安全）
```

**前端翻译范围**：
- 导航菜单、按钮、提示语
- 表单 label、placeholder、验证消息
- 页面标题、面包屑、描述
- 错误消息、成功提示
- 数据表格表头、分页

**语言切换机制**：
- 用户个人设置优先
- 无设置时继承组织默认语言
- 未登录用户使用浏览器语言偏好
- 支持 URL 参数强制指定（如 `?lang=en`）

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| I18N-001 | API 错误消息国际化 | 错误消息支持中文和英文 | 通过 `Accept-Language` Header 选择语言 |
| I18N-002 | 管理控制台国际化 | React SPA 管理界面支持中/英切换 | 界面元素 100% 翻译 |
| I18N-003 | 邮件通知国际化 | 预算告警、License 到期等邮件支持双语 | 邮件模板按用户语言偏好发送 |
| I18N-004 | 用户语言偏好 | 用户可设置个人语言偏好 | 存储在用户 Profile 中 |
| I18N-005 | 组织默认语言 | 组织可设置默认语言 | 新用户继承组织默认语言 |
| I18N-006 | API 文档国际化 | SpringDoc OpenAPI 生成的文档支持双语 | 中文/英文两套文档 |
| I18N-007 | 日期/数字/货币格式 | 根据语言环境自动格式化 | 中文: ¥1,000.00；英文: $1,000.00 |
| I18N-008 | 翻译 key 类型安全 | 翻译 key 使用常量定义，编译期检查 | 避免拼写错误 |
| I18N-009 | 懒加载翻译 | 翻译文件按需加载，减少首屏体积 | 仅加载当前语言包 |

---

#### 4.2.25 管理控制台模块

**需求描述**: Web 管理界面，所有功能的可视化操作入口。

**功能需求**:

| ID | 功能 | 详细描述 | 验收标准 |
|----|------|---------|---------|
| UI-001 | 仪表盘 | 总体统计与趋势图 | 用户数、渠道数、调用次数、费用 |
| UI-002 | 渠道管理 | 渠道 CRUD 操作界面 | 批量操作 |
| UI-003 | 策略编排 | 可视化策略配置界面 | 拖拽式节点编排 |
| UI-004 | 用户管理 | 用户/角色/权限管理 | 批量操作 |
| UI-005 | API Key 管理 | API Key CRUD 操作界面 | 批量创建/导出 |
| UI-006 | 额度配置 | Token限额管理界面 | 额度使用率可视化 |
| UI-007 | 日志查询 | 多维度日志检索界面 | 实时搜索结果 |
| UI-008 | 模型目录 | 模型浏览与搜索 | 按提供商/能力过滤 |
| UI-009 | 系统设置 | 全局配置界面 | 端口、超时、日志策略等 |
| UI-010 | 响应式设计 | 适配桌面/平板 | 移动端仅查看 |

---

## 五、API 接口需求

### 5.1 外部 API（网关端点）

#### 5.1.1 OpenAI 兼容端点

| 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/v1/chat/completions` | POST | 文本对话 | Bearer Token |
| `/v1/completions` | POST | 文本补全 | Bearer Token |
| `/v1/embeddings` | POST | 向量化 | Bearer Token |
| `/v1/images/generations` | POST | 图像生成 | Bearer Token |
| `/v1/audio/transcriptions` | POST | 语音识别 | Bearer Token |
| `/v1/audio/translations` | POST | 语音翻译 | Bearer Token |
| `/v1/audio/speech` | POST | 语音合成 | Bearer Token |
| `/v1/moderations` | POST | 内容审核 | Bearer Token |

#### 5.1.2 Anthropic 兼容端点

| 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/v1/messages` | POST | 消息对话 | x-api-key Header |

#### 5.1.3 管理 API

| 端点 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/api/v1/teams` | CRUD | 团队管理 | Bearer Token (Admin) |
| `/api/v1/members` | CRUD | 成员管理 | Bearer Token (Admin) |
| `/api/v1/users` | CRUD | 用户管理 | Bearer Token (Admin) |
| `/api/v1/channels` | CRUD | 渠道管理 | Bearer Token (Admin) |
| `/api/v1/channel-groups` | CRUD | 渠道分组 | Bearer Token (Admin) |
| `/api/v1/models` | GET | 模型目录 | Bearer Token |
| `/api/v1/providers` | CRUD | 供应商管理 | Bearer Token (Admin) |
| `/api/v1/api-keys` | CRUD | API Key 管理 | Bearer Token |
| `/api/v1/strategies` | CRUD | 策略管理 | Bearer Token (Admin) |
| `/api/v1/token-limits` | CRUD | Token限额管理 | Bearer Token (Admin) |
| `/api/v1/logs` | GET | 日志查询 | Bearer Token |
| `/api/v1/metrics` | GET | 指标查询 | Bearer Token |
| `/api/v1/health` | GET | 健康检查 | 无 |
| `/api/v1/plugins` | CRUD | 插件管理 | Bearer Token (Admin) |
| `/api/v1/tools` | CRUD | Agent 工具管理 | Bearer Token (Admin) |
| `/api/v1/prompts` | CRUD | Prompt 模板管理 | Bearer Token |
| `/api/v1/knowledge` | CRUD | 知识库管理 | Bearer Token (Admin) |
| `/api/v1/sessions` | CRUD | 会话管理 | Bearer Token |
| `/api/v1/cache` | CRUD | 语义缓存管理 | Bearer Token (Admin) |
| `/api/v1/audit-policies` | CRUD | 内容审核规则 | Bearer Token (Admin) |

**说明**: 管理 API 采用用户级别访问控制，所有资源（Provider、RouteGroup 等）全局共享。API 请求通过 GatewayApiKey 认证并关联到用户。

**P2 模块 API 说明**: Agent 工具、语义缓存、会话上下文、RAG 知识库、内容审核等功能为企业版或 P2 版本特性，通过独立端点管理。

### 5.2 API 认证方式

| 认证方式 | 使用场景 | Header 格式 |
|---------|---------|------------|
| **Bearer Token** | 外部 API 调用、管理 API | `Authorization: Bearer sk-xxx` |
| **x-api-key** | Anthropic 兼容端点 | `x-api-key: sk-xxx` |
| **Session Cookie** | 管理控制台 Web 界面 | `Cookie: session=xxx` |

### 5.3 标准错误响应格式

**OpenAI 格式**（所有 OpenAI 兼容端点和限流/预算等通用错误）:
```json
{
  "error": {
    "message": "Invalid API key provided.",
    "type": "invalid_request_error",
    "param": "api_key",
    "code": "invalid_api_key"
  },
  "request_id": "req_abc123def456",
  "trace_id": "trace_789xyz"
}
```

**Anthropic 格式**（仅 `/v1/messages` 端点的 Anthropic 原生错误）:
```json
{
  "type": "error",
  "error": {
    "type": "authentication_error",
    "message": "Invalid API key"
  },
  "request_id": "req_abc123def456",
  "trace_id": "trace_789xyz"
}
```

**统一规则**: 两种格式的错误响应均包含 `request_id` 和 `trace_id` 字段（Anthropic 官方格式中追加）。

### 5.4 Management API 统一响应格式

**适用范围**: `/api/v1/*` 管理 API（不包括 Proxy API `/v1/*`）

**响应信封结构**:
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "trace_id": "trace_789xyz",
  "timestamp": "2026-04-23T10:30:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | boolean | 操作是否成功 |
| `data` | object / null | 成功时返回数据，失败时为 null |
| `error` | object / null | 失败时包含错误信息，成功时为 null |
| `trace_id` | string | OpenTelemetry 追踪 ID |
| `timestamp` | string | ISO 8601 时间戳 |

**成功响应示例**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "team_code": "team_abc123",
    "team_name": "研发团队",
    "status": "ACTIVE"
  },
  "error": null,
  "trace_id": "trace_789xyz",
  "timestamp": "2026-04-23T10:30:00Z"
}
```

**错误响应示例**:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_PARAMETER",
    "message": "团队名称不能为空",
    "details": { "field": "team_name" }
  },
  "trace_id": "trace_789xyz",
  "timestamp": "2026-04-23T10:30:00Z"
}
```

**分页响应格式**:
```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 100,
      "total_pages": 5
    }
  },
  "error": null,
  "trace_id": "trace_789xyz",
  "timestamp": "2026-04-23T10:30:00Z"
}
```

**与 Proxy API 的区分**:

| API 类型 | 路径 | 响应格式 |
|----------|------|---------|
| Proxy API (OpenAI) | `/v1/chat/completions` | OpenAI 原生格式 |
| Proxy API (Anthropic) | `/v1/messages` | Anthropic 原生格式 |
| Management API | `/api/v1/*` | 统一 ApiResponse 信封 |

**HTTP 状态码映射**（统一适用于两种格式）:

| HTTP 状态码 | 错误类型 | 触发场景 |
|------------|---------|---------|
| 400 | `invalid_request_error` | 请求体格式错误、缺少必填字段、枚举值无效 |
| 401 | `authentication_error` / `invalid_request_error` | Token 无效、过期、格式错误 |
| 402 | `payment_required` / `insufficient_quota` | 额度超限、预算超限（REJECT 策略） |
| 403 | `permission_error` / `forbidden` | 模型不在白名单、IP 不在白名单 |
| 404 | `not_found_error` | 请求的模型不存在或无可用渠道 |
| 408 | `timeout_error` | 上游渠道请求超时 |
| 429 | `rate_limit_error` | RPM/TPM 超限 |
| 500 | `api_error` | 所有渠道均失败 |
| 502 | `upstream_error` | 上游渠道返回非预期格式 |
| 503 | `service_unavailable` | 无可用渠道、预算超限（REJECT 策略） |
| 504 | `upstream_timeout` | 上游渠道网关超时 |

### 5.4 限流响应头

所有返回 429 的响应必须包含以下标准头：

| 头名 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `Retry-After` | 秒数 | 建议客户端重试等待时间 | `30` |
| `X-RateLimit-Limit` | 整数 | 当前限流阈值 | `1000` |
| `X-RateLimit-Remaining` | 整数 | 剩余可用请求数（窗口内） | `0` |
| `X-RateLimit-Reset` | Unix 时间戳 | 限流窗口重置时间 | `1712937660` |

### 5.5 API 版本策略

**网关 API 版本控制**: 使用 URL 路径版本（`/v1/`, `/v2/`），不使用 Header 版本。

| 阶段 | 时长 | 行为 |
|------|------|------|
| **并行期** | 6 个月 | 新旧版本同时可用，客户端可自由选择 |
| **废弃期** | 3 个月 | 旧版本标记 deprecated，响应头返回 `Deprecation: true` 和 `Sunset: <date>` |
| **下线期** | — | 旧版本返回 410 Gone，携带 `Link` 头指向迁移指南 |

**弃用响应头**:
```
HTTP/1.1 200 OK
Deprecation: true
Sunset: Sat, 01 Jan 2028 00:00:00 GMT
Link: <https://docs.example.com/migration/v2>; rel="successor-version"
```

**向后兼容性保证**:
- **非破坏性变更**（无需版本升级）: 响应体新增字段、新增可选请求参数、新增枚举值
- **破坏性变更**（需版本升级）: 移除字段、修改字段类型、修改端点路径、修改必需请求参数

**Gateway API 与 Management API 版本独立演进**: 两套 API 使用独立版本号，可各自升级。

---

## 六、数据模型需求

### 6.1 实体域划分

根据业务领域，将实体划分为以下四大域：

| 域 | 实体数 | 实体列表 |
|---|--------|----------|
| ① 身份与访问控制 | 1 | User |
| ② 提供商与模型 | 4 | Provider, ProviderApiKey, Model, RouteGroup, RouteGroupProvider |
| ③ 令牌与认证 | 2 | GatewayApiKey, TokenLimit |
| ④ 日志与监控 | 3 | RequestLog, RequestBodyLog, AuditLog |
| **合计** | **10** | |

### 6.2 核心实体清单

| 实体 | 说明 | 物理标识 | 业务标识 |
|------|------|---------|---------|
| **① 身份与访问控制域** | | | |
| User | 用户 | `id BIGINT` | `user_code VARCHAR(64)` |
| **② 提供商与模型域** | | | |
| Provider | 模型提供商 | `id BIGINT` | `provider_code VARCHAR(64)` |
| ProviderApiKey | Provider 调用凭证 | `id BIGINT` | `key_code VARCHAR(64)` |
| Model | 具体模型 | `id BIGINT` | `model_code VARCHAR(128)` |
| RouteGroup | 路由分组 | `id BIGINT` | `group_code VARCHAR(64)` |
| RouteGroupProvider | 路由关联 | `id BIGINT` | - |
| **③ 令牌与认证域** | | | |
| GatewayApiKey | 网关访问凭证 | `id BIGINT` | `key_code VARCHAR(128)` |
| TokenLimit | Token 限额 | `id BIGINT` | `limit_code VARCHAR(64)` |
| **④ 日志与监控域** | | | |
| RequestLog | 调用日志 | `id BIGINT` | `request_id VARCHAR(64)` |
| RequestBodyLog | 请求体日志 | `id BIGINT` | - |
| AuditLog | 审计日志 | `id BIGINT` | `audit_code VARCHAR(64)` |

### 6.3 实体关系图

#### 6.3.1 身份与访问控制域

```
User (1) ──── (N) GatewayApiKey
User (1) ──── (N) TokenLimit
```

#### 6.3.2 提供商与模型域

```
Provider (1) ──── (N) ProviderApiKey
Provider (1) ──── (N) Model
Provider (1) ──── (N) RouteGroupProvider
RouteGroup (1) ──── (N) RouteGroupProvider
RouteGroupProvider (N) ──── (1) Provider
```

#### 6.3.3 令牌与限额域

```
TokenLimit (用户限额)
├── 层级: USER (用户级别)
├── 周期: DAILY / WEEKLY / MONTHLY / TOTAL
├── Token限额: max_tokens
├── 已用Token: used_tokens
├── 请求次数限额: max_requests (可选)
└── 周期类型: period_type

GatewayApiKey (网关访问凭证)
├── key_hash: 用于认证
├── provider_id: 可访问的 Provider (NULL 表示全部)
├── route_group_id: 路由分组 (NULL 表示默认)
├── model_whitelist: 模型白名单 (可选)
└── ip_whitelist: IP 白名单 (可选)
```

#### 6.3.4 日志与监控域

```
RequestLog ──── 1:1 ──── RequestBodyLog (可选)

AuditLog (链式哈希: hash_chain = SHA256(前一条 + 当前内容))
```

#### 6.3.5 P2 模块实体简图

以下为 P2/企业版高级功能的实体关系简化描述，实体定义在对应模块详细需求中。

```
Agent 工具市场:
Tool (1) ──── (N) ToolVersion
  │
  └── (N) ToolCategory (多对多 via ToolCategoryMapping)
  │
  └── (1:N) ToolInvocationLog (工具调用记录)

Prompt 工程 (企业版):
PromptTemplate (1) ──── (N) PromptVersion
  │
  └── (1:N) PromptDecorator (装饰器链)

会话上下文缓存 (企业版):
Session (1) ──── (N) SessionMessage
  │
  └── (N) User

RAG 知识库 (企业版):
KnowledgeBase (1) ──── (N) KnowledgeChunk
  │
  └── (1) VectorStore (向量数据库 SPI 实现)

内容审核 (企业版):
AuditPolicy (1) ──── (N) AuditRule
  │
  └── (1:N) AuditLog (审核记录)

语义缓存 (企业版):
CacheEntry (1) ──── (N) CacheHitLog
```

### 6.4 单租户架构

**LLM-Gateway 采用单租户架构**：所有用户共享全局 Provider、RouteGroup 等资源，用户级别隔离仅通过 GatewayApiKey 和 TokenLimit 实现。

| 维度 | 说明 |
|---------|------|
| **Provider** | 全局共享，所有用户可见 |
| **RouteGroup** | 全局共享，所有用户可用 |
| **Model** | 全局共享，通过 Provider 关联 |
| **GatewayApiKey** | 用户级别，每个用户可创建多个 |
| **TokenLimit** | 用户级别，支持 Provider/Model 维度限制 |

**资源隔离**:
- ✅ GatewayApiKey 通过 user_id 关联，确保用户只能访问自己的 Key
- ✅ TokenLimit 通过 user_id 关联，确保用户只能使用自己的额度
- ✅ ProviderApiKey 由管理员配置，与用户无关

### 6.5 并发冲突处理

**乐观锁 + 最后写入胜出 (LWW) + 变更通知**:

| 机制 | 实现方式 |
|------|---------|
| **版本号字段** | 所有业务实体表包含 `version INT` 字段 |
| **更新校验** | `UPDATE table SET ..., version = version + 1 WHERE id = ? AND version = ?` |
| **冲突检测** | 更新影响行数为 0 表示版本冲突 |
| **冲突处理** | 返回 409 Conflict，携带最新数据版本号 |
| **变更通知** | 通过 WebSocket/SSE 推送给其他在线管理员 |
| **审计记录** | 每次成功写入记录审计日志，包含变更前后的版本号 |

### 6.6 审计字段要求

**所有业务实体表必须包含**:
```sql
created_by    BIGINT       -- 创建人 ID (FK → users.id, 系统生成填 0L)
created_at    TIMESTAMP    -- 创建时间
updated_by    BIGINT       -- 最后更新人 ID
updated_at    TIMESTAMP    -- 最后更新时间
deleted_by    BIGINT NULL  -- 删除人 ID (软删除)
deleted_at    TIMESTAMP NULL -- 删除时间 (软删除)
```

### 6.7 核心实体详细规格

#### 6.7.1 User（用户）

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| user_code | VARCHAR(64) | 用户编码 | UNIQUE, NOT NULL |
| username | VARCHAR(64) | 用户名 | NOT NULL |
| email | VARCHAR(128) | 邮箱 | UNIQUE, NOT NULL |
| password_hash | VARCHAR(256) | 密码哈希 | BCrypt 加密，cost≥12 |
| phone | VARCHAR(32) | 手机号 | NULL |
| status | ENUM | 状态 | ACTIVE / DISABLED / LOCKED / DELETED |
| email_verified | BOOLEAN | 邮箱已验证 | DEFAULT false |
| oauth_providers | JSON | OAuth提供者列表 | GitHub / Gitee / LinuxDO |
| pii_salt | VARCHAR(64) | PII脱敏盐值 | GDPR 删除权用 |
| last_login_at | TIMESTAMP | 最后登录时间 | NULL |

**业务说明**: 单租户架构，所有用户共享全局 Provider/RouteGroup 资源。

#### 6.7.2 Role（角色）

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| role_code | VARCHAR(64) | 角色编码 | UNIQUE, NOT NULL |
| name | VARCHAR(64) | 角色名称 | NOT NULL |
| description | TEXT | 角色描述 | NULL |
| role_type | ENUM | 角色类型 | SYSTEM / CUSTOM |
| is_active | BOOLEAN | 是否启用 | DEFAULT true |

**预设角色**（SYSTEM 类型）:

| 角色 | 编码 | 权限范围 |
|------|------|----------|
| 管理员 | ADMIN | 系统全部权限 |
| 开发者 | DEVELOPER | 创建 API Key、查看日志、调用 API |
| 观察者 | OBSERVER | 仅查看用量和日志 |
| 财务管理员 | FINANCE_ADMIN | 额度配置、用量查看 |

#### 6.7.3 UserRole（用户角色关联）

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| user_id | BIGINT | 用户ID | FK → User.id, NOT NULL |
| role_id | BIGINT | 角色ID | FK → Role.id, NOT NULL |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |

**唯一约束**: `(user_id, role_id)` 联合唯一。

#### 6.7.4 Permission（权限）

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| permission_code | VARCHAR(128) | 权限编码 | UNIQUE, NOT NULL |
| name | VARCHAR(64) | 权限名称 | NOT NULL |
| description | TEXT | 权限描述 | NULL |
| category | VARCHAR(32) | 权限分类 | user / provider / model / token / log / setting |

**权限编码规范**: `resource:action` 格式（如 `provider:create`, `token:read`）。

#### 6.7.5 Provider（提供商）

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
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

**业务说明**: Provider 是全局共享的，所有用户可见。`provider_type` 决定使用哪个 Adapter 实现类。

#### 6.7.6 ProviderApiKey（Provider 调用凭证）

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| key_code | VARCHAR(64) | 业务标识 | UNIQUE, NOT NULL |
| provider_id | BIGINT | 所属 Provider | FK → Provider.id, NOT NULL |
| key_name | VARCHAR(64) | Key 名称 | NULL |
| api_key | VARCHAR(512) | API Key（加密存储） | NOT NULL |
| priority | INT | 优先级（用于轮换） | DEFAULT 100 |
| status | ENUM | 状态 | ACTIVE / DISABLED / EXHAUSTED / EXPIRED / DELETED |
| last_used_at | TIMESTAMP | 最后使用时间 | NULL |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

**业务规则**:
- `api_key` 使用 AES-256 加密存储
- 同一 Provider 下可有多个 Key（主备/轮换）
- `status = EXHAUSTED` 时进入冷却期，不会被选择

#### 6.7.7 GatewayApiKey（网关访问凭证）

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| key_code | VARCHAR(128) | 业务标识 | UNIQUE, NOT NULL |
| key_hash | VARCHAR(256) | API Key 哈希 | NOT NULL |
| user_id | BIGINT | 所属用户 | FK → User.id, NOT NULL |
| provider_id | BIGINT | 关联的 Provider | FK → Provider.id, NULL 表示全部 |
| route_group_id | BIGINT | 路由分组 | FK → RouteGroup.id, NULL 表示默认 |
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
- 同一用户可创建多个 Key（主备/轮换）

#### 6.7.8 RouteGroup（路由分组）

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| group_code | VARCHAR(64) | 分组编码 | UNIQUE, NOT NULL |
| group_name | VARCHAR(128) | 分组名称 | NOT NULL |
| strategy | ENUM | 路由策略 | ROUND_ROBIN / LEAST_LATENCY / PRIORITY |
| failover_enabled | BOOLEAN | 是否启用故障转移 | DEFAULT true |
| max_retry | INT | 最大重试次数 | DEFAULT 2 |
| health_check_interval | INT | 健康检查间隔（秒） | DEFAULT 30 |
| description | TEXT | 描述 | NULL |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

#### 6.7.9 RouteGroupProvider（路由关联）

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| route_group_id | BIGINT | 路由分组 | FK → RouteGroup.id, NOT NULL |
| provider_id | BIGINT | Provider | FK → Provider.id, NOT NULL |
| weight | INT | 权重（用于负载均衡） | DEFAULT 100 |
| priority | INT | 优先级（用于故障转移） | DEFAULT 100 |
| status | ENUM | 状态 | ENABLED / DISABLED / UNHEALTHY |
| health_status | ENUM | 健康状态 | HEALTHY / DEGRADED / UNHEALTHY |
| consecutive_failures | INT | 连续失败次数 | DEFAULT 0 |
| last_health_check_at | TIMESTAMP | 最后健康检查时间 | NULL |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

#### 6.7.10 TokenLimit（Token限额）

| 属性 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | PK, AUTO_INCREMENT |
| limit_code | VARCHAR(64) | 限额编码 | UNIQUE, NOT NULL |
| user_id | BIGINT | 所属用户 | FK → User.id, NOT NULL |
| provider_id | BIGINT | 关联的 Provider | FK → Provider.id, NULL 表示全部 |
| model_id | BIGINT | 关联的 Model | FK → Model.id, NULL 表示全部 |
| token_limit_enabled | BOOLEAN | 是否启用Token限额 | DEFAULT true |
| max_tokens | DECIMAL(20,6) | Token限额总量 | NULL 表示不限 |
| used_tokens | DECIMAL(20,6) | 已用Token量 | DEFAULT 0 |
| request_limit_enabled | BOOLEAN | 是否启用请求次数限额 | DEFAULT false |
| max_requests | INT | 请求次数限额 | NULL 表示不限 |
| used_requests | INT | 已用请求次数 | DEFAULT 0 |
| period_type | ENUM | 周期类型 | DAILY / WEEKLY / MONTHLY / TOTAL |
| period_day_of_week | INT | 周内日期 | 1-7，WEEKLY 时有效 |
| period_day_of_month | INT | 月内日期 | 1-31，MONTHLY 时有效 |
| exceeded_action | ENUM | 超限动作 | REJECT / DOWNGRADE |
| switch_model_id | BIGINT | 降级切换模型 | exceeded_action 为 DOWNGRADE 时必填 |
| status | ENUM | 状态 | ACTIVE / SUSPENDED / DELETED |
| created_by | BIGINT | 创建人 | FK → users.id |
| created_at | TIMESTAMP | 创建时间 | NOT NULL |
| updated_by | BIGINT | 更新人 | FK → users.id |
| updated_at | TIMESTAMP | 更新时间 | NOT NULL |

---

## 七、安全与合规需求

### 7.0 安全合规差异化定位

**LLM-Gateway 的安全合规能力是其对标 VoAPI 的核心差异化竞争力，面向政企客户的采购决策流程提供以下关键卖点：**

| 差异化卖点 | 客户感知价值 | 对标合规场景 |
|-----------|-------------|-------------|
| **等保三级就绪** | 内置国密算法 (SM2/SM3/SM4)，满足等保 2.0 三级技术要求 | 政务、央企、金融机构采购门槛 |
| **全链路 PII 保护** | 自动检测、脱敏、加密个人身份信息，覆盖传输/存储/日志全链路 | GDPR / 个保法 (PIPL) 合规 |
| **不可篡改审计链** | 所有管理操作与 API 调用记录采用 WORM（一次写入多次读取）存储 | 金融行业监管 (SOX/ Basel III) |
| **零信任架构** | 默认不信任任何请求，10 层安全检查逐层验证 | 企业安全红线、零信任采购标准 |
| **密钥生命周期管理** | 支持对接 Vault/KMS，密钥自动轮换、细粒度授权、到期提醒 | 企业安全运营中心 (SOC) 要求 |
| **数据主权控制** | 数据本地化配置、跨境传输审计与阻断 | 数据出境合规 (CAC 评估) |
| **行列级数据权限** | RBAC + ABAC 双重控制，实现字段级、行级数据可见性隔离 | 多部门协作、外包场景权限管控 |
| **安全评分与基线** | 内置安全基线扫描与实时安全评分，持续监控合规状态 | 安全团队日常运维抓手 |

### 7.1 零信任安全架构

```
请求进入
    │
    ▼
[1] TLS 验证 ──────── 失败 → 拒绝
    │ 通过
    ▼
[2] Token 认证 ────── 失败 → 401
    │ 通过
    ▼
[3] IP/UA 检查 ───── 失败 → 403
    │ 通过
    ▼
[4] API Key 状态检查 ──── 失效 → 403
    │ 有效
    ▼
[5] 模型白名单 ───── 未授权 → 403
    │ 通过
    ▼
[6] IP 白名单 ────── 不在 → 403
    │ 通过
    ▼
[7] 额度检查 ──────── 超限 → 402
    │ 通过
    ▼
[8] 预算检查 ──────── 超限 → 执行策略
    │ 通过
    ▼
[9] 限流检查 ──────── 超限 → 429
    │ 通过
    ▼
[10] 敏感词过滤 ───── 命中 → 400
    │ 通过
    ▼
转发到上游渠道
```

### 7.2 数据加密要求

| 数据类型 | 加密方式 | 存储要求 | 合规对标 |
|---------|---------|---------|---------|
| 渠道 API Key | AES-256-GCM / SM4 | 加密存储，密钥与数据分离 | 等保三级 |
| 用户密码 | BCrypt (cost≥12) | 哈希存储，不可逆 | OWASP ASVS L2 |
| PII 数据 | SM3 脱敏 / SM4 加密 | 日志中脱敏，数据库中加密 | GDPR / PIPL |
| 传输数据 | TLS 1.3+ / 国密 TLS | 全链路加密 | 等保三级 |
| 审计日志 | SHA-256 链式哈希 | WORM 存储，不可篡改 | SOX / 金融监管 |

### 7.3 合规要求

| 合规项 | 要求 | 验收标准 | 适用行业 |
|--------|------|---------|---------|
| **等保 2.0 三级** | 支持国密算法 (SM2/SM3/SM4)、身份鉴别、访问控制、安全审计 | 通过等保三级测评 | 政务、央企、金融 |
| **GDPR** | 用户数据删除权、数据可携带权、PII 加密与脱敏 | 支持用户数据导出/删除 API | 欧洲业务 |
| **个人信息保护法 (PIPL)** | 个人信息处理告知同意、敏感信息单独授权、跨境传输评估 | PII 检测准确率 ≥95%，跨境传输审计日志 | 国内企业 |
| **审计合规 (SOX)** | 所有关键操作不可篡改的审计日志，保留期限 ≥7 年 | 审计日志 WORM 存储，链式哈希校验 | 金融、上市公司 |
| **跨境数据合规** | 支持数据本地化存储配置、跨境传输审计与阻断 | 可配置数据驻留策略 | 出海企业 |
| **OWASP ASVS L2** | 密码策略、登录失败锁定、会话管理、CSRF/XSS 防护 | 通过 OWASP ZAP 安全扫描 | 全行业 |

### 7.4 密钥管理

| 功能 | 详细描述 | 验收标准 |
|------|---------|---------|
| **加密存储** | API Key 使用 AES-256-GCM 或 SM4 加密存储 | 密钥不可逆，密文无法还原 |
| **密钥分离** | 加密密钥与加密数据分开存储 | 独立 KMS 管理 |
| **密钥轮换** | 支持定期自动轮换加密密钥 | 轮换周期可配置（默认 90 天） |
| **Vault 集成** | 支持对接 HashiCorp Vault / 云厂商 KMS | 支持标准 KMS API |
| **密钥授权** | 支持临时密钥、时间限制、使用次数限制 | 细粒度授权策略 |
| **密钥审计** | 记录密钥使用、轮换、吊销全生命周期 | 审计日志可查询 |

### 7.5 PII 全链路保护

**保护范围**:

| 环节 | 保护措施 | 覆盖的 PII 类型 |
|------|---------|---------------|
| **传输中** | TLS 1.3+ / 国密 TLS 加密 | 姓名、身份证号、手机号、邮箱 |
| **存储中** | SM4 加密 / AES-256-GCM | 用户密码、API Key、身份证号 |
| **日志中** | 自动脱敏（掩码/哈希） | 手机号 `138****1234`、邮箱 `a***@example.com` |
| **使用中** | 内存安全擦除、调试模式关闭 | 临时变量中的敏感信息 |
| **删除时** | 安全擦除、不可恢复 | 用户数据删除权 |

**PII 自动检测规则**:

| PII 类型 | 检测规则 | 脱敏方式 |
|---------|---------|---------|
| 身份证号 | 18 位数字 + 校验位 | `110***********1234` |
| 手机号 | 11 位数字，1 开头 | `138****1234` |
| 邮箱 | 标准邮箱格式 | `a***@example.com` |
| 银行卡号 | 16-19 位数字 | `**** **** **** 1234` |
| 姓名 | 中文 2-4 字 | `张*` |
| 地址 | 完整地址模式 | `北京市****` |

### 7.6 审计链

**审计日志要求**:

| 属性 | 要求 | 说明 |
|------|------|------|
| **完整性** | 记录所有管理操作与关键 API 调用 | 操作人、时间、动作、结果、IP |
| **不可篡改性** | 采用链式哈希（Hash Chain）确保日志不可篡改 | 每条日志包含前一条的哈希值 |
| **WORM 存储** | 一次写入多次读取，不支持修改/删除 | 支持合规归档 |
| **保留期限** | 默认 7 年，可配置 | 满足 SOX/金融监管要求 |
| **可追溯性** | 支持按用户/时间/操作类型检索 | 全文索引 |
| **独立存储** | 审计日志与业务日志分离 | 独立数据库/存储桶 |

**审计事件清单**:

| 事件类别 | 事件示例 |
|---------|---------|
| **用户管理** | 用户创建/删除/禁用、角色变更、密码重置 |
| **渠道管理** | 渠道创建/编辑/删除、API Key 轮换 |
| **策略变更** | 路由策略创建/修改/删除、预算调整 |
| **安全事件** | 登录失败、IP 黑名单命中、Token 过期 |
| **数据访问** | 批量导出、敏感数据查询、日志下载 |
| **系统变更** | 配置变更、版本升级、数据库迁移 |

### 7.7 安全评分与基线

**安全评分系统**:

| 评分项 | 权重 | 检查内容 |
|--------|------|---------|
| 认证配置 | 20% | 是否启用 Token 认证、OAuth 配置 |
| 加密配置 | 20% | API Key 加密、TLS 启用、国密算法 |
| 访问控制 | 15% | IP 白名单、UA 过滤、模型白名单 |
| 审计配置 | 15% | 审计日志启用、WORM 存储、保留期限 |
| PII 保护 | 15% | 脱敏规则启用、检测覆盖率 |
| 密码策略 | 10% | 密码强度、过期时间、失败锁定 |
| 网络安全 | 5% | CORS 配置、请求体大小限制、超时设置 |

**安全评分等级**:

| 分数 | 等级 | 建议 |
|------|------|------|
| 90-100 | 🟢 优秀 | 满足企业安全要求 |
| 75-89 | 🟡 良好 | 建议修复剩余风险 |
| 60-74 | 🟠 及格 | 存在中等风险，需尽快修复 |
| <60 | 🔴 危险 | 不满足安全基线，禁止生产部署 |

**安全评分计算**: 各评分项得分 = 权重 × (实际配置达标数 / 该类别总检查项数) × 100
总分 = Σ(各类别得分)。评分频率：每次启动时计算 + 管理员手动触发重新评分。
<60 分强制执行：CI/CD 流水线中集成安全评分检查，<60 分自动阻止构建部署（技术卡点，非流程策略）。

### 7.8 零信任 10 层安全检查逐层标准

| 层级 | 检查项 | 通过标准 | 失败响应 | 延迟预算 | 依赖不可用时行为 |
|------|--------|---------|---------|---------|---------------|
| [1] TLS 验证 | 请求使用 HTTPS、TLS ≥1.2 | 连接建立成功 | 连接拒绝 | 0ms（TCP 层） | 连接不可用 → 拒绝 |
| [2] Token 认证 | `Authorization: Bearer sk-xxx` 有效 | Token 存在于数据库且未过期 | 401 `invalid_api_key` | ≤2ms（Redis 缓存） | Redis 不可用 → DB 查询；DB 不可用 → 503 `service_unavailable`（fail-closed） |
| [3] IP/UA 检查 | 源 IP 不在黑名单、UA 合法 | IP 通过规则匹配 | 403 `access_denied` | ≤1ms | 规则引擎不可用 → 跳过（fail-open，但记录审计） |
| [4] API Key 状态检查 | Key status = ACTIVE，未过期，额度未超限 | 状态校验通过 | 403 `key_inactive` / 402 `quota_exceeded` | ≤1ms | DB 不可用 → 503 |
| [5] 模型白名单 | 请求模型在 Token 的 model_whitelist 中 | 模型匹配（空名单 = 全部允许） | 403 `model_not_allowed` | ≤1ms | — |
| [6] IP 白名单 | 源 IP 在 Token 的 ip_whitelist 中 | CIDR 匹配（空名单 = 全部允许） | 403 `ip_not_allowed` | ≤1ms | — |
| [7] 额度检查 | used_quota + 预估费用 ≤ quota | 预扣额度检查 | 402 `quota_exceeded` | ≤2ms | Redis 不可用 → DB 查询；DB 不可用 → 503（fail-closed） |
| [8] 预算检查 | 各层级预算未超限 | 预算使用率 < 100% | 执行 exceeded_action（REJECT → 503 / DOWNGRADE / SWITCH） | ≤3ms | DB 不可用 → fail-closed（拒绝，防止超额消费） |
| [9] 限流检查 | RPM/TPM 未超阈值 | Redis 原子计数 | 429 `rate_limit_exceeded` + `Retry-After` 头 | ≤2ms | Redis 不可用 → fail-open（跳过限流，记录审计告警） |
| [10] 敏感词过滤 | 请求内容不包含敏感词 | 规则匹配 | 400 `content_violation` | ≤1ms | 规则引擎不可用 → 跳过（fail-open，记录审计） |

**10 层总延迟预算**: ≤15ms (P95)，其中 Token 认证和预算检查为关键路径。

### 7.9 ABAC 属性定义

ABAC（Attribute-Based Access Control）基于以下属性组合进行访问决策：

| 属性类别 | 属性名 | 类型 | 示例值 | 说明 |
|---------|--------|------|--------|------|
| **用户属性** | `user.role` | Set | `["developer"]` | 用户角色集合 |
| | `user.department` | String | `"engineering"` | 用户部门 |
| | `user.ip` | String | `"192.168.1.100"` | 用户 IP |
| **环境属性** | `env.time` | Time | `"14:30:00"` | 当前时间（用于工作时间限制） |
| | `env.day_of_week` | String | `"Monday"` | 当前星期 |
| | `env.is_holiday` | Boolean | `false` | 是否节假日 |
| **资源属性** | `resource.type` | String | `"provider"` | 资源类型 |
| | `resource.owner_id` | Long | `5` | 资源创建者 ID |
| **操作属性** | `action` | String | `"provider:delete"` | 操作标识 |
| | `action.risk_level` | String | `"high"` | 操作风险等级 |

**ABAC 规则示例**:
```
ALLOW IF:
  user.role CONTAINS 'admin'
  OR (user.role CONTAINS 'developer' AND action IN ['provider:read', 'token:create'])
  OR (user.role CONTAINS 'finance_admin' AND action STARTS_WITH 'token:')
  AND env.time BETWEEN '09:00' AND '18:00'  -- 仅工作时间允许高危操作
```

### 7.10 PII 检测范围

PII 自动检测应用于以下数据范围：

| 数据范围 | 是否检测 | 说明 |
|---------|---------|------|
| **请求体 (Request Body)** | ✅ 检测 | messages.content、system prompt 等文本字段 |
| **响应体 (Response Body)** | ✅ 检测 | 模型返回的 content 字段 |
| **URL 参数** | ❌ 不检测 | 网关不解析 URL 参数内容 |
| **HTTP 头** | ❌ 不检测 | 头字段通常不含 PII |
| **日志中的请求/响应体** | ✅ 脱敏 | 日志写入前自动脱敏 |
| **数据库中的 API Key** | ✅ 加密 | AES-256-GCM 加密存储，非脱敏 |

### 7.11 合规冲突解决：GDPR 删除权 vs WORM 审计

**冲突**: GDPR 允许用户要求删除个人数据，但审计日志要求 WORM 7 年不可篡改。

**解决方案**: **PII 可擦除 + 审计记录保留**

| 操作 | 审计日志中的 PII 字段 | 审计日志中的非 PII 字段 |
|------|---------------------|----------------------|
| GDPR 删除请求 | 使用确定性哈希替换（如 `SHA256(email + salt)` → `a1b2c3...`） | 保留不变 |
| 审计链完整性 | 记录哈希替换操作本身作为新审计条目 | — |

- 审计记录本身不删除（保留 WORM 链完整性）
- PII 内容被不可逆哈希替换（满足 GDPR "不可识别" 要求）
- 替换操作本身记录为新审计条目（可追溯删除行为）
- 仅处理直接 PII（姓名、邮箱、手机号、身份证号），不处理间接 PII

**适用场景**: 仅适用于审计日志中的 PII 字段。业务数据（用户表、渠道表等）中的 PII 可完全删除。

### 7.12 威胁模型 (STRIDE)

| 威胁类别 | 识别威胁 | 缓解措施 | 对应需求 |
|---------|---------|---------|---------|
| **Spoofing** | 伪造 Token 访问 | JWT/Bcrypt Token 验证、IP 白名单 | §7.1 [2], [6] |
| | 伪造上游 Provider | BaseURL 白名单、TLS 证书校验 | §7.1 [1] |
| **Tampering** | 篡改渠道配置 | 乐观锁 + 审计链 + RBAC | data-model.md §FK Cascade |
| | 篡改审计日志 | WORM 存储 + 链式哈希 | §7.6 |
| **Repudiation** | 否认操作行为 | 完整审计链（操作人/IP/时间/详情） | §7.6 |
| **Information Disclosure** | PII 泄露 | PII 全链路脱敏 + 加密 | §7.5, §7.2 |
| | 密钥泄露 | AES-256 加密 + Vault 集成 | §7.4 |
| **Denial of Service** | 超频请求耗尽资源 | RPM/TPM 限流 + 熔断 | §7.1 [9], §4.2.3 CH-014 |
| | 恶意大请求体 | 请求体大小限制（10MB） | §4.2.1 GW-009 |
| **Elevation of Privilege** | 越权访问其他用户数据 | RBAC + GatewayApiKey 关联 | §6.3, §7.9 |
| | 越权访问管理功能 | RBAC + ABAC | §7.9 |

---

## 八、可观测性需求

### 8.1 全链路追踪

```
客户端请求
    │
    ▼ [Trace ID 生成]
    │ Trace-ID: trace_abc123
    ▼
[认证中间件] ──── Span 1: auth.duration
    │
    ▼
[策略引擎] ────── Span 2: strategy.evaluation
    │
    ▼
[路由决策] ────── Span 3: routing.decision
    │
    ▼
[协议转换] ────── Span 4: protocol.transform
    │
    ▼
[渠道调用] ────── Span 5: channel.call.{channel_code}
    │
    ▼
[响应处理] ────── Span 6: response.process
    │
    ▼
[日志记录] ────── Span 7: audit.log
    │
    ▼
返回客户端
```

### 8.2 日志规范

**结构化日志格式**:
```json
{
  "timestamp": "2026-04-13T10:30:00.000Z",
  "level": "INFO",
  "trace_id": "trace_abc123",
  "request_id": "req_def456",
  "user_id": "user_789",
  "team_code": "acme-corp",
  "model": "openai/gpt-4o",
  "channel": "ch_001",
  "event": "request.completed",
  "duration_ms": 245,
  "input_tokens": 1500,
  "output_tokens": 800,
  "total_tokens": 2300
}
```

### 8.3 告警规则

| 告警项 | 条件 | 级别 | 通知方式 |
|--------|------|------|---------|
| 渠道失败率 | >5% (5 分钟内) | WARN | 邮件/Webhook |
| 渠道失败率 | >20% (5 分钟内) | ERROR | 邮件/Webhook/短信 |
| 预算使用率 | >80% | WARN | 邮件 |
| 预算使用率 | >100% | ERROR | 邮件/Webhook |
| 平均延迟 | P95 > 500ms | WARN | 邮件 |
| Token 计量异常 | 偏差 >1% | ERROR | 邮件/Webhook |

---

## 九、性能需求

### 9.1 核心性能指标

**基准请求特征**（所有性能指标的测试基准）:
- 平均输入 Token: 1,000 tokens
- 平均输出 Token: 500 tokens
- 非流式:流式 = 60%:40%
- Payload 大小: 平均 5KB 请求 / 平均 3KB 响应
- 模型分布: GPT-4o 40%, Claude Sonnet 30%, 其他 30%

| 指标 | 要求 | 测量方法 |
|------|------|---------|
| **首 token 延迟** | ≤100ms (P95) | 从网关接收客户端请求完毕到首字符 SSE 事件发出 |
| **非流式响应延迟** | ≤5,000ms (P95) | 从网关接收请求到完整响应返回 |
| **直通转发延迟增加** | ≤5ms (P95) | 下游为 Anthropic 兼容端点时 |
| **协议转换延迟** | ≤5ms (P95) | 格式转换耗时 |
| **策略评估延迟** | ≤1ms (P95，≤3 节点策略) | 路由决策耗时 |
| **Token 计量延迟** | ≤1ms (P95) | 费用计算耗时 |
| **吞吐量** | ≥10,000 QPS (单实例) | 基准请求特征下的非流式+流式混合 QPS |
| **并发流式连接** | ≥1,000 | 同时活跃的 SSE 连接，每连接 10 tokens/s |
| **可用性** | ≥99.9% (月度 SLA) | 外部探针每 30 秒健康检查 |
| **策略热加载** | 0 秒 | 不影响进行中请求 |

### 9.2 容量规划

| 资源 | 单实例 | 集群 (3 节点) | 集群 (10 节点) |
|------|--------|--------------|---------------|
| QPS | 10,000 | ~28,000 | ~85,000 |
| 并发连接 | 1,000 | ~2,800 | ~8,500 |
| 日志存储/天 | 10GB | 30GB | 100GB |
| 数据库连接 | 100 | 300 | 1,000 |

**扩容假设**:
- 网关实例无状态，水平扩展效率接近线性 (90%+ 效率)
- Redis 为单实例瓶颈点（限流原子操作依赖 Redis），集群 QPS 受 Redis 吞吐量限制
- 数据库连接池为共享资源，10 节点集群建议使用数据库连接代理（如 ProxySQL）
- 容量数字为理论上限，实际需在目标硬件上通过基准测试验证（T276）

---

## 十、部署与运维需求

### 10.1 部署模式

| 模式 | 说明 | 适用场景 |
|------|------|---------|
| **Docker Compose** | 单机部署，一键启动 | 开发/测试/小规模 |
| **Kubernetes Helm** | K8s 集群部署 | 生产环境 |
| **独立 Jar 包** | 直接运行 | 传统服务器部署 |

### 10.2 配置文件

```yaml
# application.yml 核心配置
server:
  port: 8080

gateway:
  llm:
    routing-strategy: COST_OPTIMIZED
    max-retries: 3
    timeout-seconds: 30

datasource:
  url: jdbc:mysql://localhost:3306/ai_api_router
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD}

redis:
  url: redis://localhost:6379/0

logging:
  level: INFO
  format: json
  log-retention:
    enabled: true
    mode: day  # day/week/month/year

opentelemetry:
  enabled: true
  exporter:
    endpoint: http://otel-collector:4317
```

### 10.3 Docker Compose 部署

```yaml
services:
  redis:
    image: redis:7-alpine

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: ai_api_router

  LLM-Gateway:
    image: LLM-Gateway:latest
    ports:
      - "8080:8080"
    environment:
      DB_USERNAME: root
      DB_PASSWORD: ${DB_PASSWORD}
    depends_on:
      - redis
      - mysql
```

### 10.4 Kubernetes 部署

- 提供 Helm Chart
- 支持 HPA 自动扩缩容
- 支持零停机滚动升级
- 支持多可用区部署

### 10.5 CI/CD 流水线

**CI/CD 平台**: GitHub Actions

| 阶段 | 工作流 | 触发条件 | 步骤 |
|------|--------|---------|------|
| **Build** | `build.yml` | PR 创建、push 到 main | checkout → JDK 21 setup → Maven compile |
| **Test** | `test.yml` | PR 创建、push 到 main | unit tests → integration tests (Testcontainers) → Jacoco coverage report (gate: core≥90%, adapter≥80%) |
| **Security** | `security.yml` | PR merge、weekly schedule | OWASP Dependency-Check → SAST scan → container image scan (Trivy) |
| **Release** | `release.yml` | Git tag 推送 | Docker Buildx 多架构镜像 (amd64/arm64) → push 到 GHCR → Helm chart publish |

**分支策略**: Trunk-based development。Feature branch → PR → review (≥1 reviewer) → merge to main → auto-deploy to staging → manual promote to production.

**质量门禁**:
- 单元测试覆盖率: core ≥90%, adapter ≥80%
- 无 Critical/High 安全漏洞 (OWASP Dependency-Check)
- 无 Code Smell (SonarQube Quality Gate)
- 容器镜像无 Critical CVE (Trivy)

### 10.6 备份与恢复

| 数据类型 | 备份策略 | 备份类型 | 保留期限 | RPO |
|---------|---------|---------|---------|-----|
| MySQL 主库 | 每日 02:00 | 全量 + binlog 增量 | 30 天全量 + 90 天 binlog | ≤1 小时 |
| Redis | 每日 03:00 | RDB 快照 | 7 天 | ≤24 小时 (缓存可重建) |
| S3 审计日志归档 | 不适用 (本身就是 WORM 归档) | — | 7 年 | — |
| 配置文件 | Git 版本控制 | — | 永久 | — |

**恢复时间目标 (RTO)**:
- MySQL 全量恢复: ≤2 小时 (100GB 数据集)
- 服务重启恢复: ≤15 分钟 (Docker 重启)
- 集群故障恢复: ≤1 小时 (切换到备用集群)

**灾备策略**: 生产环境跨可用区部署。单可用区故障自动切换。区域级故障需手动切换到备份区域。

### 10.7 环境管理

| 环境 | 用途 | 数据量 | 渠道配置 | Feature Flags |
|------|------|--------|---------|--------------|
| **dev** | 开发调试 | 种子数据 | Mock Provider | 全功能开启 |
| **staging** | 集成测试、预发布 | 生产数据脱敏副本 | 测试 Provider (低配额) | 与生产对齐 |
| **prod** | 生产环境 | 真实数据 | 真实 Provider | 按 License 级别 |

**Secret 管理策略**:

| 环境 | Secret 存储 | 轮换方式 |
|------|-----------|---------|
| dev | `.env` 文件 (不提交 Git) | 手动 |
| staging | GitHub Secrets / 环境变量 | 每月 |
| prod | HashiCorp Vault / 云 KMS | 自动轮换 (90 天) |

**数据库迁移**: 通过 Flyway 在应用启动时自动应用。dev/staging 自动执行，production 需手动审批 (`flyway migrate` via CI/CD)。

### 10.8 SLA 测量方法

**可用性 ≥99.9% 定义**:
- 测量周期: 自然月
- 不可用定义: 网关对所有渠道返回 5xx 错误持续时间 ≥1 分钟
- 排除情况: 单个渠道故障 (不影响 SLA，仅影响该渠道可用性)
- 允许停机预算: ≤43.2 分钟/月 (99.9%)
- 测量方式: 外部探针每 30 秒发送健康检查请求到 `/api/v1/health`，记录成功/失败率

**性能 SLA 测量**:
- 首 token 延迟: 生产环境每 5 分钟采样 1 次请求，计算 P95
- 吞吐量: Prometheus `http_requests_total` 计数器，5 分钟窗口计算 QPS
- 并发连接: Prometheus `gateway_sse_connections_active` Gauge 峰值

---

## 十一、非功能性需求

### 11.1 可测试性

| 要求 | 标准 |
|------|------|
| 核心服务层单元测试覆盖率 | ≥ 90% |
| 路由引擎单元测试覆盖率 | ≥ 85% |
| 适配器层单元测试覆盖率 | ≥ 80% |
| 集成测试 | 所有外部 API 适配器 |
| 性能测试 | 延迟/吞吐量基准测试 |

### 11.2 可维护性

| 要求 | 标准 |
|------|------|
| Code Smell 零容忍 | 重复代码、上帝类、长方法 |
| 方法长度限制 | ≤ 50 行 |
| 嵌套深度限制 | ≤ 3 层 |
| 类长度限制 | ≤ 500 行 |

### 11.3 可扩展性

| 扩展点 | 方式 |
|--------|------|
| 新增 Provider | 实现 `LLMProviderAdapter` 接口 |
| 新增路由策略 | 实现 `RouteStrategy` 接口 |
| 新增协议 | 实现 `ProtocolAdapter` 接口 |
| 插件 | SPI 机制加载 |

### 11.4 国际化

| 语言 | 支持阶段 |
|------|---------|
| 简体中文 | v1.0 |
| English | v1.0 |

### 11.5 易用性需求

**设计原则**: LLM-Gateway 作为企业级 APIPark 竞品，功能复杂度高（30+ 实体、25+ 模块），必须通过易用性设计降低用户上手成本，提升使用效率。

#### 11.5.1 快速配置体系

| 功能 | 详细描述 | 验收标准 | 版本 |
|------|---------|---------|------|
| **预置 Provider 模板** | 内置 50+ 主流模型预配置（BaseURL、认证方式、模型列表），用户只需填 API Key | API Key 填写后自动识别 Provider | v1.0 |
| **配置向导 (Wizard)** | 首次配置引导用户完成：选择场景 → 填 API Key → 选模型 → 配置限额 → 生成 Token → 测试连通性 | 新用户 5 分钟内完成首个渠道配置 | v1.0 |
| **一键导入配置** | 支持从环境变量/配置文件批量导入渠道配置 | 批量导入成功率 ≥99% | v1.0 |
| **智能推荐** | 根据用户输入的 API Key 自动识别 Provider，推荐合适的渠道配置和路由策略 | Provider 识别准确率 ≥95% | v1.0 |

#### 11.5.2 可视化策略编排

| 功能 | 详细描述 | 验收标准 | 版本 |
|------|---------|---------|------|
| **拖拽式策略编辑器** | 通过 UI 拖拽配置路由策略，所见即所得 | 零代码，所见即所得 | v1.0 |
| **策略节点类型** | 支持条件节点、路由节点、转换节点、限流节点、限额节点、降级节点 | 覆盖所有策略编排场景 | v1.0 |
| **策略模拟测试** | 输入样本请求，预览路由结果和策略生效范围 | 模拟测试延迟 ≤500ms | v1.0 |
| **策略热加载** | 策略变更即时生效，不影响进行中请求 | 0 秒延迟 | v1.0 |
| **策略版本管理** | 策略变更历史记录与回滚，支持版本对比 | 支持版本对比和一键回滚 | v1.0 |

#### 11.5.3 开发者体验优化

| 功能 | 详细描述 | 验收标准 | 版本 |
|------|---------|---------|------|
| **API Playground** | 内置类似 OpenAI Playground 的调试工具，支持发送测试请求、查看响应、生成调用代码 | 支持 cURL/Python/JS/Go 示例生成 | v1.0 |
| **多语言 SDK** | 提供 Python/TypeScript/Go SDK，兼容 OpenAI SDK 接口，一行代码接入 | SDK 自动处理认证、重试、错误处理 | v2.0 |
| **快速开始文档** | 5 分钟快速入门、场景化教程（"如何配置一个客服机器人"）、常见错误排查指南 | 文档覆盖 ≥10 个常见场景 | v1.0 |
| **SDK 代码补全** | IDE 插件支持 SDK 代码补全和类型提示 | 支持 VS Code / IntelliJ IDEA | v2.0 |

#### 11.5.4 上手指引与帮助

| 功能 | 详细描述 | 验收标准 | 版本 |
|------|---------|---------|------|
| **交互式引导** | 首次登录引导创建第一个渠道；新增渠道时智能检测并提示配置缺失 | 首次配置完成率 ≥90% | v1.0 |
| **状态可视化** | 实时显示渠道状态、Token 使用率、请求统计；问题快速定位（哪个渠道挂了、谁的额度超了） | 状态刷新延迟 ≤3s | v1.0 |
| **智能诊断** | 自动检测配置问题（API Key 无效、模型不支持等），提供修复建议和一键修复按钮 | 诊断准确率 ≥85% | v1.0 |
| **上下文帮助** | 表单字段、配置项显示上下文帮助，悬停查看详细说明 | 所有配置项 100% 覆盖帮助文档 | v1.0 |
| **视频教程** | 关键功能提供操作视频（渠道配置、策略编排、额度设置等） | 视频总数 ≥20 个 | v2.0 |

#### 11.5.5 监控与反馈闭环

| 功能 | 详细描述 | 验收标准 | 版本 |
|------|---------|---------|------|
| **实时仪表盘** | Token 使用趋势图、各模型调用量占比、渠道延迟热力图 | 数据延迟 ≤1s | v1.0 |
| **告警通知** | 额度快用完时邮件/钉钉通知；渠道故障即时告警；异常请求模式预警 | 告警触达率 ≥99% | v1.0 |
| **使用分析** | 团队/个人用量排行榜、成本归因、ROI 报表 | 支持 CSV/Excel 导出 | v1.0 |
| **调用链追踪** | 请求全链路追踪，支持从日志快速定位问题 | Trace ID 可点击跳转 | v1.0 |
| **健康报告** | 每周/每月自动生成系统健康报告，推送给管理员 | 报告生成成功率 ≥99% | v2.0 |

#### 11.5.6 易用性优先级矩阵

| 优先级 | 特性 | 用户价值 | 复杂度 |
|-------|------|---------|--------|
| **P0** | 预置 Provider 模板 + 一键导入 | 5 分钟内完成渠道配置 | 低 |
| **P0** | 配置向导（首次设置） | 零基础用户快速上手 | 中 |
| **P0** | 智能诊断 + 修复建议 | 减少 50% 配置问题 | 中 |
| **P1** | 可视化策略编排器 | 零代码配置复杂路由 | 高 |
| **P1** | API Playground | 开发者快速调试 | 中 |
| **P1** | 实时仪表盘 + 告警 | 主动发现和解决问题 | 低 |
| **P1** | 交互式引导 | 减少首次配置流失率 | 中 |
| **P2** | 多语言 SDK | 一行代码接入 | 高 |
| **P2** | 使用分析 + 成本报表 | 辅助决策 | 低 |
| **P2** | 视频教程 | 降低学习门槛 | 中 |

#### 11.6 性能基准补充需求

**补充说明**: 以下指标在 9.1 核心性能指标基础上补充，适用于企业版或特定场景。

| 指标类别 | 指标项 | 目标值 | 验收标准 |
|---------|-------|--------|---------|
| **延迟** | 语义缓存匹配延迟 | ≤ 10ms (P95) | 向量相似度计算 |
| | 渠道切换延迟（故障转移） | ≤ 100ms | 切换完成时间 |
| | 策略评估延迟（>3 节点） | ≤ 3ms (P95) | 复杂策略 |
| **可用性** | 渠道健康检查间隔 | ≤ 60s | 可配置 |
| **资源** | 内存占用（空载） | ≤ 512MB | JVM 默认堆 |
| | 内存占用（满载） | ≤ 2GB | 10K QPS 持续压力 |
| | CPU 利用率（满载） | ≤ 80% | 10K QPS 持续压力 |
| **扩展性** | 集群 QPS（10 节点） | ≥ 100,000 | 线性扩展验证 |

#### 11.7 测试策略

**设计原则**: 测试驱动开发，核心模块覆盖率必须达标。

| 测试类型 | 覆盖率要求 | 测试策略 |
|---------|-----------|---------|
| **单元测试** | 核心服务层 ≥ 90% | 使用 JUnit 5 + Mockito |
| | 路由引擎 ≥ 85% | 边界条件、异常场景 |
| | 适配器层 ≥ 80% | 模拟 Provider 响应 |
| **集成测试** | 所有外部 API 适配器 | 使用 Testcontainers |
| | 数据库 CRUD 操作 | H2 内存数据库 |
| | Redis 连接（企业版） | embedded-redis |
| **E2E 测试** | 关键用户流程 ≥ 80% | Playwright |
| | OpenAI 兼容性 | 官方 SDK 集成测试 |
| | Anthropic 兼容性 | 官方 SDK 集成测试 |
| **性能测试** | 延迟基准 | JMeter / k6 |
| | 吞吐量基准 | 持续 30 分钟压测 |
| | 并发连接数 | 10K concurrent connections |
| **安全测试** | 认证流程 | 边界条件覆盖 |
| | 权限隔离 | 跨团队数据隔离验证 |
| | 敏感数据脱敏 | 日志审查验证 |

---

## 十二、版本规划

### 12.1 产品版本定义

LLM-Gateway 是开源的 AI 模型 API 聚合分发网关，提供两个版本：

| 版本 | 目标客户 | 部署模式 | 核心价值 | 商业模式 |
|------|---------|---------|---------|---------|
| **标准版 (Standard)** | 个人开发者/小团队 | 自托管 Docker | 基础 API 网关，快速上手 | 开源免费 |
| **企业版 (Enterprise)** | 中大型企业/政企客户 | K8s 集群高可用 | 团队隔离、安全合规、全链路可观测 | 商业授权 |

### 12.2 版本功能矩阵

#### 12.2.1 功能对比矩阵

| 模块 | 功能 | 标准版 | 企业版 |
|------|------|--------|--------|
| **API 网关** | OpenAI 端点 | ✅ | ✅ |
| | Anthropic 端点 | ✅ | ✅ |
| | SSE 流式转发 | ✅ | ✅ |
| | 协议转换 | ✅ | ✅ |
| **API Portal** | 服务发布/订阅 | - | ✅ |
| | 订阅审批 | - | ✅ |
| | Prompt 封装 API | - | ✅ |
| **渠道管理** | 渠道 CRUD | ✅ | ✅ |
| | 多 Key 管理 | ✅ | ✅ |
| | 负载均衡 | ✅ | ✅ |
| | 故障转移 | ✅ | ✅ |
| | 渠道分组 | ✅ | ✅ |
| | 代理配置 | - | ✅ |
| | 余额同步 | - | ✅ |
| **认证** | Token 认证 | ✅ | ✅ |
| | OAuth 登录 | GitHub、QQ、企业微信 | ✅ |
| | 企业 OAuth 登录 | 飞书、钉钉、GitHub Enterprise | - | ✅ |
| | SSO (LDAP/SAML) | - | ✅ |
| **路由** | 基础路由 | ✅ | ✅ |
| | 可视化策略编排 | - | ✅ |
| | 成本最优路由 | - | ✅ |
| | 延迟最优路由 | - | ✅ |
| | 场景路由 | - | ✅ |
| | 自定义脚本扩展 | - | ✅ |
| **团队管理** | 团队隔离 | - | ✅ |
| | 成员与角色管理 | - | ✅ |
| | 跨团队共享 | - | ✅ |
| **API Key 管理** | API Key CRUD | ✅ | ✅ |
| | 模型白名单 | - | ✅ |
| | IP 白名单 | - | ✅ |
| | 过期时间 | ✅ | ✅ |
| **Token 计量** | Token 计量 | ✅ | ✅ |
| **Token 限额** | 四级限额控制 | - | ✅ |
| | 独立开关 (Token/次数) | - | ✅ |
| | 预扣额度 | - | ✅ |
| | 差额调整 | - | ✅ |
| | 超限降级策略 | - | ✅ |
| | 限额告警 | - | ✅ |
| **安全** | IP 白/黑名单 | - | ✅ |
| | 密钥加密存储 | - | ✅ |
| | PII 脱敏 | - | ✅ |
| | 审计日志 (WORM) | - | ✅ |
| | 国密算法 (SM2/SM3/SM4) | - | ✅ |
| **可观测性** | 结构化日志 | - | ✅ |
| | Prometheus 指标 | - | ✅ |
| | OpenTelemetry 追踪 | - | ✅ |
| | Grafana 仪表盘 | - | ✅ |
| **日志** | 调用日志 | ✅ | ✅ |
| | 操作日志 | - | ✅ |
| | 请求体日志 | - | ✅ |
| | 日志归档 | - | ✅ |
| **管理控制台** | 仪表盘 | ✅ | ✅ |
| | 渠道管理页面 | ✅ | ✅ |
| | API Key 管理页面 | ✅ | ✅ |
| | 额度配置页面 | - | ✅ |
| | 策略编排页面 | - | ✅ |
| | 日志查询页面 | - | ✅ |
| | 用户/角色管理 | - | ✅ |
| **模型管理** | 模型目录 | ✅ | ✅ |
| | 供应商管理 | ✅ | ✅ |
| | 远程同步 | - | ✅ |
| **插件系统** | SPI 插件框架 | - | ✅ |
| | MCP 协议支持 | - | ✅ |
| **部署** | Docker Compose | ✅ | ✅ |
| | Kubernetes Helm | - | ✅ |
| | 高可用集群 | - | ✅ |

#### 12.2.2 标准版 (Standard)

| 模块 | 功能 | 说明 |
|------|------|------|
| **API 网关** | OpenAI/Anthropic 双端点 | `/v1/chat/completions`、`/v1/messages` |
| | SSE 流式转发 | 首 token ≤100ms |
| | 协议转换 | OpenAI ↔ Anthropic 双向转换 |
| **渠道管理** | 渠道 CRUD | 创建/查询/编辑/删除 |
| | 多 Key 管理 | 单渠道多 API Key |
| | 负载均衡 | 优先级 + 权重 |
| | 故障转移 | 自动重试 + 切换 |
| **认证** | Token 认证 | Bearer Token 验证 |
| **路由** | 基础路由 | 优先级 + 权重路由 |
| **Token 计量** | Token 计量 | 输入/输出 Token 分别统计 |
| **日志** | 调用日志 | 基础日志查询 |
| **管理控制台** | 仪表盘 | 总体统计 |
| | 渠道管理页面 | CRUD 操作界面 |
| | API Key 管理页面 | API Key CRUD 操作 |
| | 日志查询页面 | 多维度日志检索 |
| **部署** | Docker Compose | 一键部署 |

#### 12.2.3 企业版 (Enterprise)

| 模块 | 功能 | 说明 |
|------|------|------|
| **API 网关** | 标准版全部功能 | - |
| **渠道管理** | 代理配置 | HTTP/S、SOCKS5 |
| | 余额同步 | 手动/定时同步上游余额 |
| **认证** | OAuth 登录 | GitHub、QQ、企业微信 |
| | 企业 OAuth 登录 | 飞书、钉钉、GitHub Enterprise |
| **路由** | 可视化策略编排 | 拖拽式节点配置 |
| | 成本/延迟最优路由 | Phase 2 |
| | 场景路由 | background/think/webSearch |
| | 自定义脚本扩展 | 沙箱隔离 |
| **用户管理** | 单租户 | 所有用户共享全局资源 |
| | 角色与权限 | 系统级角色 + 细粒度权限 |
| **Token 限额** | 用户级别限额 | User / User×Provider / User×Model |
| | 独立开关 | Token 限额/请求次数限额 独立控制 |
| | 预扣额度 | 请求前预扣，防止超额 |
| | 差额调整 | 多退少补 |
| | 超限降级 | REJECT / DOWNGRADE |
| | 限额告警 | 80%/100% 阈值 |
| **安全** | PII 全链路脱敏 | 检测 + 脱敏 + 加密 |
| | 审计日志 | WORM 存储、链式哈希 |
| | 国密算法 | SM2/SM3/SM4 |
| **可观测性** | OpenTelemetry | 全链路追踪 |
| | Grafana 仪表盘 | 预配置模板 |
| **日志** | 操作日志 | 管理操作审计 |
| | 请求体日志 | 可选开启 |
| | 日志归档 | S3/OSS 冷存储 |
| **管理控制台** | 额度配置页面 | 可视化配置 |
| | 策略编排页面 | 所见即所得 |
| | 日志查询页面 | 多维度检索 |
| | 用户/角色管理 | 完整 RBAC |
| **模型管理** | 远程同步 | 一键同步官方数据 |
| **插件系统** | SPI 插件框架 | 无需修改核心代码 |
| | MCP 协议支持 | Model Context Protocol |
| **部署** | Kubernetes Helm | HPA 自动扩缩容 |
| | 高可用集群 | 多可用区部署 |

**明确排除**：
- **定价/计费**：按模型计价、费用计算、成本优化（由模型提供方管理）
- **训练场嵌入**：通过插件化扩展实现

### 12.3 不做清单

以下功能为 VoAPI 的功能，但本项目**明确排除**或**不优先实现**：

| 功能 | 排除原因 |
|------|---------|
| 签到系统、用户等级体系 | 面向企业客户，非消费者运营场景 |
| 兑换码系统 | 企业客户通过合同/订单管理，非自助充值 |
| 邀请好友裂变营销 | 不适用企业级销售模式 |
| 自定义 SEO/主题色/全局样式 | 管理控制台使用统一 Design System |
| 训练场嵌入/自定义菜单 | 通过插件化扩展实现 |
| 每日签到/连续签到奖励 | 不适用企业场景 |
| 按模型计价/费用计算 | 定价是模型提供商的职责，网关仅负责 Token 用量的计量、跟踪、限额 |
| 按模型计价/费用计算 | 定价是模型提供商的职责，网关仅负责 Token 用量的计量、跟踪、限额 |


### 12.4 技术架构策略

#### 12.4.1 系统架构

LLM-Gateway 采用分层架构，确保代码清晰、职责分明。

```
┌─────────────────────────────────────────────────────┐
│                    API Gateway                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │ OpenAI      │  │ Anthropic  │  │ Console UI    │ │
│  │ Endpoint    │  │ Endpoint   │  │ (React)     │ │
│  └─────────────┘  └─────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────┘
                         │
┌─────────────────────────────────────────────────────┐
│                   Service Layer                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │ Router      │  │ Provider    │  │ Token       │ │
│  │ Service     │  │ Service     │  │ Metering    │ │
│  └─────────────┘  └─────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────┘
                         │
┌─────────────────────────────────────────────────────┐
│                  Adapter Layer (SPI)                 │
│  ┌─────────────┐  ┌─────────────┐                  │
│  │ OpenAI      │  │ Anthropic   │  ...             │
│  │ Adapter     │  │ Adapter     │                  │
│  └─────────────┘  └─────────────┘                  │
└─────────────────────────────────────────────────────┘
```

#### 12.4.2 Feature Flag 清单

| Flag 名称 | 默认值 | 控制功能 | 运行时可变 |
|-----------|--------|---------|-----------|
| `features.multi_tenant.enabled` | false | 单租户模式（固定） | ❌ 启动时 |
| `features.rbac.enabled` | false | RBAC 权限控制 | ❌ 启动时 |
| `features.budget.enabled` | false | Token 限额控制 | ❌ 启动时 |
| `features.strategy_orchestration.enabled` | false | 可视化策略编排 | ❌ 启动时 |
| `features.otel.enabled` | false | OpenTelemetry 全链路 | ❌ 启动时 |
| `features.prometheus.enabled` | false | Prometheus 指标导出 | ❌ 启动时 |
| `features.log_retention.enabled` | false | 日志归档到 S3/OSS | ✅ 可热更新 |
| `features.log_body.enabled` | false | 请求体日志记录 | ✅ 可热更新 |

#### 12.4.3 数据库策略

单租户架构，Provider/RouteGroup 全局共享，GatewayApiKey/TokenLimit 按用户隔离。

#### 12.4.4 API 版本策略

```
v1.0 ──(升级)──▶ v1.1 ──(升级)──▶ v2.0
  │                │                │
  └─ Flyway迁移     └─ Flyway迁移     └─ 破坏性变更
```

### 12.5 开发路线图

| 阶段 | 发布时间 (目标) | 交付内容 |
|------|----------------|---------|
| **Phase 1** | TBD | **标准版 v1.0**：API 网关、渠道管理、Token 认证、Token 计量、Docker 部署 |

### 12.6 MVP (Phase 1) 范围

MVP 必须包含以下最小可用功能：

| 模块 | MVP 功能 |
|------|---------|
| API 网关 | `/v1/chat/completions`、`/v1/messages` |
| 协议转换 | OpenAI ↔ Anthropic 双向转换 |
| SSE 流式 | 首 token ≤100ms |
| 渠道管理 | 渠道 CRUD、多 Key、负载均衡 |
| Token 认证 | Bearer Token 验证 |
| 基础路由 | 优先级 + 权重路由 |
| Token 计量 | 输入/输出 Token 分别统计 |
| 管理控制台 | 基础仪表盘、渠道管理页面、API_Key管理 |
| 部署 | Docker Compose 一键部署 |

### 12.7 版本号语义

遵循 Semantic Versioning:

- **主版本号** (v1.x → v2.x): 破坏性变更，需迁移脚本
- **次版本号** (v1.1 → v1.2): 向后兼容的功能增强
- **修订号** (v1.2.1 → v1.2.2): Bug 修复

**版本号格式**:

| 版本类型 | 版本格式 | 示例 |
|---------|---------|------|
| 标准版 | `v{major}.{minor}.{patch}` | `v1.0.0` |

### 12.8 API 破坏性变更迁移策略

当 API 发生破坏性变更时（如端点路径变更、请求/响应格式变更），采用**双版本共存过渡期**策略：

| 阶段 | 时长 | 行为 |
|------|------|------|
| **并行期** | 6 个月 | 新旧版本同时可用，客户端可自由选择 |
| **废弃期** | 3 个月 | 旧版本标记 deprecated，返回 Warning Header |
| **下线期** | - | 旧版本返回 410 Gone，携带迁移指引 |

**迁移保障**:
- ✅ 新版本发布时自动提供向后兼容层
- ✅ 旧版本请求自动添加 `Deprecation: true` 响应头
- ✅ 管理控制台显示 API 版本健康度仪表盘
- ✅ 提供自动化迁移工具（如 API 签名变更检测、客户端代码生成）

---

## 附录

### A. 需求优先级定义

| 优先级 | 含义 | 发布时间 |
|--------|------|---------|
| **P0** | 必须有，无此功能产品不可用 | v1.0 |
| **P1** | 应该有，严重影响体验 | v1.0-v1.5 |
| **P2** | 最好有，增强竞争力 | v1.5+ |

### B. 参考文档

- [项目宪章](../.specify/memory/constitution.md)
- [VoAPI 功能特性文档](../VoAPI功能特性文档.md)
- [可行性分析](../FEASIBILITY_ANALYSIS.md)
- [Spring MVC vs WebFlux 评估](../FEASIBILITY_SPRING_MVC_VT.md)
- [LangChain4j/Spring AI 评估](../FEASIBILITY_LANGCHAIN4J_SPRINGAI.md)

### C. 变更历史

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|---------|--------|
| v1.0 | 2026-04-13 | 初始版本 | - |
