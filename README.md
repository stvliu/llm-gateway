# LLM-Gateway

<div align="center">

**企业级 AI 网关 - 更合规、更安全、更智能、更易用**

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

[快速开始](#快速开始) · [功能特性](#功能特性) · [架构设计](#架构设计) · [API 文档](#api-文档) · [部署指南](#部署指南)

</div>

---

## 📖 项目简介

**LLM-Gateway** 是专注大模型的企业级大模型网关。通过统一的标准 API 接口（OpenAI/Anthropic 兼容），实现对 50+ 主流大模型的接入和管理。

### 核心价值主张

| 利益相关者 | 核心价值 |
|-----------|---------|
| **开发者** | 一次接入，通过标准 OpenAI/Anthropic API 调用 50+ 主流模型，开箱即用 |
| **架构师** | 零信任安全、国密合规、智能降级、全链路可观测、云原生部署 |
| **管理者** | Token 限额控制、用量透明化、合规审计链、ROI 分析 |
| **运维人员** | K8s 原生、智能诊断、Prometheus/Grafana 集成、零停机升级 |

### 差异化竞争力

- ✅ **更易用** - 开箱即用 (预置模板) + 零学习成本 (OpenAI 兼容) + 运维友好 (智能诊断)
- 🚧 **更合规** - 等保 2.0 三级合规；国密算法 (SM2/SM3/SM4) + 完整审计链 (WORM) *(规划中)*
- ✅ **更安全** - Prompt 注入防护 + PII 智能脱敏 + 内容安全审核 + 零信任架构
- ✅ **更智能** - 智能降级 (业务连续性) + 语义缓存 (降本 30%+) + 场景路由

---

## ✨ 功能特性

### 核心功能矩阵

#### API 网关
- ✅ OpenAI 兼容端点 (`/v1/chat/completions`, `/v1/completions`)
- ✅ Anthropic 兼容端点 (`/v1/messages`)
- ✅ SSE 流式转发 (首 token ≤100ms)
- ✅ 协议转换 (OpenAI ↔ Anthropic 互转)
- 🚧 图像生成端点 (`/v1/images/generations`) *(规划中)*
- 🚧 语音合成端点 (`/v1/audio/speech`) *(规划中)*
- 🚧 语音识别端点 (`/v1/audio/transcriptions`) *(规划中)*
- 🚧 内容审核端点 (`/v1/moderations`) *(规划中)*

#### Provider 管理
- ✅ Provider CRUD (创建/查询/编辑/删除)
- ✅ 多 Key 管理 (自动轮换、调度、故障切换)
- ✅ 默认 Key 设置 (唯一默认 Key，自动清除其他标记)
- ✅ Key 统计展示 (列表页显示活跃数/总数)
- ✅ 负载均衡 (优先级 + 权重)
- ✅ 渠道分组 (按用途/价格分组)
- ✅ 渠道级故障转移
- ✅ 熔断超时 (防雪崩)
- ✅ 代理配置 (HTTP/S、Socket5)

#### 路由
- ✅ 模型级智能降级 (额度不足/模型不可用时自动切换)
- ✅ 场景路由 (CODE/CREATIVE/SUMMARY 等)
- ✅ 模型别名映射
- ✅ 可视化策略编排
- ✅ 自定义脚本扩展

#### 用户与认证
- ✅ 用户 CRUD
- ✅ 用户名密码登录/登出
- 🚧 OAuth 登录 (GitHub, Gitee, QQ, 企业微信) *(规划中)*
- 🚧 企业 OAuth (飞书、钉钉、GitHub Enterprise) *(规划中)*

#### 密钥管理
- ✅ API Key CRUD
- ✅ 额度限制
- ✅ 模型白名单
- ✅ IP 限制
- ✅ 过期时间

#### Token 计量与配额
- ✅ Token 计量 (输入/输出分别统计)
- ✅ Token 限额 (用户级/API Key 级)
- ✅ 用户×渠道限额
- ✅ 请求次数配额

#### 安全与风控
- ✅ 认证中间件 (Token 验证)
- ✅ IP 白/黑名单
- ✅ UA 过滤
- ✅ PII 脱敏 (手机号、身份证、邮箱、银行卡等)
- ✅ 数据掩码策略
- ✅ 审计日志
- ✅ 密钥加密存储 (AES-256-GCM)
- 🚧 国密算法 (SM2/SM3/SM4) *(规划中)*
- 🚧 完整审计链 (WORM) *(规划中)*

#### 可观测性
- ✅ Trace ID (全链路追踪)
- ✅ 结构化日志 (JSON 格式)
- ✅ 实时指标 (延迟/QPS/Token/费用)
- ✅ Prometheus 导出
- ✅ Grafana 仪表盘
- ✅ Jaeger 追踪

#### 语义缓存 *(规划中)*
- 相似请求返回缓存，降低成本 30%+
- 缓存 TTL 配置
- 缓存命中率统计
- 基于 pgvector 的向量相似度搜索

#### MCP 协议 *(规划中)*
- Resources: 提供上下文数据
- Prompts: 提供预定义的提示模板
- Tools: 提供可调用的工具函数

---

## 🏗️ 架构设计

### 技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **语言** | Java | 21 LTS | 支持虚拟线程 |
| **框架** | Spring Boot | 3.5.13 | Web MVC（同步） |
| **ORM** | Spring Data JPA | 3.5.x | Hibernate 6.x |
| **主数据库** | PostgreSQL | 14+ | 生产环境 |
| **开发数据库** | H2 | 2.3.232 | 本地开发调试（MODE=PostgreSQL） |
| **数据库迁移** | Flyway | 11.0.0 | 版本化 schema 迁移（V1~V68） |
| **缓存** | Redis + Caffeine | 7.x | 分布式缓存/会话 + 本地缓存 |
| **安全** | Sa-Token | 1.45.0 | 轻量级权限框架 |
| **HTTP 客户端** | OkHttp | 4.12.0 | 同步/异步调用 + SSE |
| **可观测性** | OpenTelemetry + Micrometer | 1.47.0 | 链路追踪 + Prometheus 指标 |
| **日志** | Logstash Logback Encoder | 7.4 | JSON 结构化日志 |
| **测试** | Testcontainers | 1.20.4 | 容器化集成测试 |

### 模块化架构

采用 **17 模块多模块 Maven 结构**（命名对齐 Jmix），业务按功能域拆分为核心模块 + JPA 绑定模块，协议层插件化：

| 分组 | 模块 | 职责 |
|------|------|------|
| 横切基础 | `gateway-common` | BaseEntity、异常、工具、通用枚举 |
| 协议域 | `gateway-protocol/protocol` | Canonical IR + ProtocolAdapter SPI + 协议契约 |
| | `gateway-protocol/protocol-openai` | OpenAI 协议插件 |
| | `gateway-protocol/protocol-anthropic` | Anthropic 协议插件 |
| | `gateway-protocol/protocol-gemini` | Gemini 协议插件（可扩展性示例） |
| 供给域 | `gateway-provider/provider`（+`provider-data`） | Provider / Channel / Model / Catalog / Upstream |
| 身份与访问 | `gateway-iam/iam`（+`iam-data`） | User / Application / UserApiKey / Auth / 加密 |
| 用量管控 | `gateway-usage/usage`（+`usage-data`） | Token 计量 / 配额 / 限流 |
| 安全与威胁 | `gateway-security/security`（+`security-data`） | IP 威胁检测 + 数据脱敏 |
| 审计追溯 | `gateway-audit/audit`（+`audit-data`） | 调用日志 / 审计事件 |
| 告警通知 | `gateway-alert/alert`（+`alert-data`） | 告警通知 |
| 韧性 | `gateway-resilience/resilience`（+`resilience-data`） | failover / retry / 熔断 |
| 模型代理 | `gateway-proxy/proxy` | ChatDispatch 调度 / routing / 协议转换 |
| 聚合统计 | `gateway-stats/stats` | 仪表盘统计（读路径） |
| 应用 / 入口 | `gateway-boot` | 后端主模块（Adapter / Application + 模块装配） |
| | `gateway-cli` | CLI 管理工具 |
| | `gateway-simulator` | LLM 提供商模拟服务 |
| 前端 | `gateway-console` | Web 管理界面 |

- **命名规范（Jmix 式）**：模块 = 根包，去除 `domain/application/infrastructure` DDD 前缀；groupId 按功能域划分（`com.codingas.gateway.<域>`）；JPA 绑定模块根包为 `<域>data`（如 `usagedata`、`securitydata`）
- **协议插件化**：OpenAI / Anthropic / Gemini 以插件形式通过 AutoConfiguration + `@ConditionalOnProperty` 启用，可扩展新协议
- **分层架构**：gateway-boot 内仍保留 Adapter → Application 分层，Domain 与 Infrastructure 下沉至各功能域模块

```
┌─────────────────────────────────────────────────────┐
│                    Adapter 层                        │
│        (Controller / REST API / Interceptor)        │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│                  Application 层                      │
│         (Service / Use Case Orchestration)          │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│                   功能域模块                         │
│   (Domain + Gateway Interface + Infrastructure)    │
│   gateway-provider / gateway-iam / gateway-usage    │
│   gateway-proxy / gateway-protocol / ...            │
└─────────────────────────────────────────────────────┘
```

### 部署模式

#### 单机部署
- 单 JVM 实例运行
- H2 / PostgreSQL 数据库
- 本地限流 (JVM 内存令牌桶)
- 本地缓存 (Caffeine)
- 无语义缓存依赖

**适用场景**: 中小企业、开发测试、POC 验证、资源受限环境

#### 分布式部署（K8s 高可用）
- K8s 多副本高可用
- PostgreSQL（+ pgvector 语义缓存，规划中）
- Redis 集群 (分布式限流/缓存)
- Nacos 配置中心
- Prometheus + Grafana 监控

**适用场景**: 政企客户、金融机构、中大型企业生产环境

---

## 🚀 快速开始

### 前置要求

- JDK 21+
- Maven 3.9+
- PostgreSQL 14+ (可选，默认使用 H2)
- Redis 7.x+ (可选，用于分布式部署)

### 方式一：源码构建运行

```bash
# 克隆仓库
git clone https://github.com/codingas/llm-gateway.git
cd llm-gateway

# 编译打包（多模块）
./mvnw clean install -DskipTests

# 运行应用（默认 local profile：H2 文件持久化 + Caffeine，零外部依赖，适合开发调试）
./mvnw spring-boot:run -pl gateway-boot

# 或直接运行 fat jar
java -jar gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar

# 连接 PostgreSQL 运行（需先创建数据库）
java -jar gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar --spring.profiles.active=postgresql
```

访问地址:
- API 服务: http://localhost:8080
- Actuator 健康检查: http://localhost:8080/actuator/health
- Prometheus 指标: http://localhost:8080/actuator/prometheus
- H2 控制台 (local 模式): http://localhost:8080/h2-console

### 方式二：Docker 运行

```bash
# 构建镜像
docker build -t llm-gateway:latest .

# 运行容器 (默认 local profile，H2 文件持久化)
docker run -d \
  --name llm-gateway \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  llm-gateway:latest

# 运行容器 (连接外部 PostgreSQL)
docker run -d \
  --name llm-gateway \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=postgresql \
  -e DB_HOST=host.docker.internal \
  -e DB_USERNAME=llm_gateway \
  -e DB_PASSWORD=yourpassword \
  llm-gateway:latest
```

### 方式三：Docker Compose (推荐)

```bash
cd deployments/docker
docker-compose up -d
```

包含组件:
- LLM-Gateway 应用
- PostgreSQL 数据库
- Redis 缓存
- Prometheus 监控
- Grafana 可视化
- Jaeger 链路追踪 + OpenTelemetry Collector

---

## 📡 API 文档

### OpenAI 兼容接口

#### Chat Completions

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-your-api-key" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [
      {"role": "user", "content": "Hello!"}
    ],
    "stream": false
  }'
```

#### Streaming Chat

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-your-api-key" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [
      {"role": "user", "content": "Write a poem"}
    ],
    "stream": true
  }'
```

### Anthropic 兼容接口

#### Messages

```bash
curl -X POST http://localhost:8080/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: sk-your-api-key" \
  -d '{
    "model": "claude-3-haiku-20240307",
    "messages": [
      {"role": "user", "content": "Hello!"}
    ],
    "max_tokens": 1000
  }'
```

### 管理 API

详见 [API 规格文档](docs/api-spec.md)

---

## ⚙️ 配置说明

### 配置文件结构

```
gateway-boot/src/main/resources/
├── application.yml              # 主配置（默认 local profile）
├── application-local.yml        # 本地调试（H2 文件库 + Caffeine，零外部依赖）
├── application-dev.yml          # 开发环境
├── application-prod.yml         # 生产环境
├── application-postgresql.yml   # PostgreSQL 独立配置
└── application-standalone.yml   # 单机部署配置
```

### 关键配置项

#### 数据库配置

```yaml
# H2 配置（默认 local profile，文件持久化，MODE=PostgreSQL 兼容）
spring:
  datasource:
    url: jdbc:h2:file:./data/gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver

# PostgreSQL 配置（生产 / postgresql profile）
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/llm_gateway
    username: ${DB_USERNAME:llm_gateway}
    password: ${DB_PASSWORD:}
    driver-class-name: org.postgresql.Driver
```

> schema 由 **Flyway** 版本化迁移管理（`gateway-boot/src/main/resources/db/migration/`，V1~V68），无需手动建表。

#### 缓存配置

```yaml
# 本地缓存 (单机)
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=5m

# Redis 缓存 (分布式)
spring:
  cache:
    type: redis
    redis:
      time-to-live: 5m
      cache-null-values: false
```

#### 网关配置

```yaml
gateway:
  init:
    demo-data-enabled: false       # 初始化演示数据
  actuator:
    health:
      public-access: true          # 健康检查公网可访问
  health:
    provider:
      stale-threshold: 300s        # Provider 健康状态过期阈值
      failure-threshold: 3         # 连续失败熔断阈值
      success-threshold: 2         # 连续成功恢复阈值
      probe-timeout: 10s           # 健康探测超时
```

#### 管理端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,traces   # 暴露端点
      base-path: /actuator
  endpoint:
    health:
      group:
        liveness:                  # 存活探针
          include: ping
        readiness:                 # 就绪探针
          include: db, providerRegistry
  tracing:
    sampling:
      probability: 1.0             # 全量链路采样
```

更多配置详见 [技术架构文档](docs/技术架构.md)

---

## 📊 监控与可观测性

### Prometheus 指标

访问 `http://localhost:8080/actuator/prometheus` 获取指标（Micrometer 标准指标 + 自定义指标）。

已实现的自定义指标:
- `gateway.failover.triggered` - 渠道故障切换触发次数
- `gateway.failover.exhausted` - 故障切换全部耗尽次数

规划中指标 *(Roadmap)*:
- `llm_gateway_requests_total` - 请求总数
- `llm_gateway_request_duration_seconds` - 请求延迟分布
- `llm_gateway_tokens_input_total` / `llm_gateway_tokens_output_total` - Token 计量
- `llm_gateway_channel_requests_total` - 按 Channel 统计请求数
- `llm_gateway_cache_hits_total` - 缓存命中次数
- `llm_gateway_rate_limit_rejected_total` - 限流拒绝次数

### Grafana 仪表板

启动 Docker Compose 后访问: http://localhost:3000

预置仪表板:
- 请求概览 (QPS、延迟、错误率)
- Token 统计 (输入/输出趋势、成本估算)
- Channel 健康 (各渠道请求量、错误率、延迟)
- 缓存效率 (命中率、缓存大小)
- 限流效果 (拒绝率、限流 Key 分布)

### 日志查看

```bash
# 查看应用日志
tail -f logs/llm-gateway.log

# 查看错误日志
tail -f logs/llm-gateway-error.log

# Docker 环境
docker logs -f llm-gateway
```

---

## 🧪 测试

### 单元测试

```bash
./mvnw test
```

### 集成测试

```bash
./mvnw verify
```

> 单元测试（Surefire：`*Test`/`*Tests`）与集成测试（Failsafe：`*IntegrationTest`/`*E2ETest`，基于 Testcontainers）分离。

### 性能目标 *(Roadmap)*

- QPS: 10,000/实例
- P95 延迟: < 500ms
- P99 延迟: < 2000ms
- SSE 并发: 1,000

---

## 📦 部署指南

LLM-Gateway 支持三种部署形态：

### 1. 系统安装包（推荐：非 Docker 一键部署）

默认 `local` profile（H2 文件持久化 + Caffeine 缓存，零外部依赖，无 Redis），装完即用。

- **Linux deb/rpm**：`apt install ./llm-gateway_*.deb` 或 `dnf install ./llm-gateway-*.rpm`
- **Windows exe**：双击 `llm-gateway-setup.exe`

安装时交互设置端口（默认 8080），加密密钥自动生成。详见 [deployments/package/README.md](deployments/package/README.md)。

构建产物：`gateway-boot-1.0.0-SNAPSHOT.jar`（fat jar，Main-Class=`org.springframework.boot.loader.launch.JarLauncher`）。

### 2. Docker

```bash
cd deployments/docker
docker-compose up -d
```

详见 [deployments/docker/](deployments/docker/)。

### 3. 源码运行

```bash
./mvnw spring-boot:run -pl gateway-boot
```

> **重要提示：**
> - **默认凭据**：`local` profile 自动创建 `admin/admin`，首次登录后请**立即修改密码**。
> - **H2 Console 风险**：`local` profile 开启 H2 Console（`/h2-console`）且 `web-allow-others=true`，允许远程访问，生产环境请关闭或限制。
> - **加密密钥备份**：系统安装包部署时 `GATEWAY_ENCRYPTION_KEY` 自动生成，**务必备份**，丢失则历史加密数据无法解密。

---

### 单机部署

#### 最低配置
- CPU: 2 核
- 内存: 1 GB
- 磁盘: 10 GB

#### 推荐配置
- CPU: 4 核
- 内存: 2 GB
- 磁盘: 20 GB

#### 启动命令

```bash
# JAR 直接运行
nohup java -Xms512m -Xmx1g \
  -XX:+UseZGC \
  -jar gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=standalone > gateway.log 2>&1 &
```

详见 [技术架构 - 单机部署](docs/技术架构.md#144-标准版单机部署)

### Kubernetes 部署 (分布式高可用)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: llm-gateway
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: gateway
          image: llm-gateway:latest
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "2000m"
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
```

使用 Helm 部署:

```bash
helm install llm-gateway deployments/helm/llm-gateway \
  --set replicaCount=3 \
  --set resources.requests.memory=512Mi \
  --set resources.limits.memory=2Gi
```

### 高可用架构

```
                    ┌─────────────┐
                    │   负载均衡   │
                    │  (Nginx/LB) │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
         ┌─────────┐  ┌─────────┐  ┌─────────┐
         │ Gateway │  │ Gateway │  │ Gateway │
         │  Pod 1  │  │  Pod 2  │  │  Pod 3  │
         └────┬────┘  └────┬────┘  └────┬────┘
              │            │            │
              └────────────┼────────────┘
                           │
                           ▼
                  ┌────────────────┐
                  │   Redis 集群    │
                  │ (限流/缓存/锁)  │
                  └────────┬───────┘
                           │
                           ▼
                  ┌────────────────┐
                  │ PostgreSQL 集群 │
                  │  (+ pgvector)  │
                  └────────────────┘
```

---

## 📚 文档

### 核心文档

- [需求规格说明书](docs/spec.md) - 完整的功能需求和非功能性需求
- [API 规格文档](docs/api-spec.md) - 详细的 API 接口定义
- [信息架构文档](docs/信息架构.md) - 领域模型和业务概念
- [应用架构文档](docs/应用架构.md) - 分层架构和模块设计
- [数据架构文档](docs/数据架构.md) - 数据库设计和 ER 图
- [技术架构文档](docs/技术架构.md) - 技术选型和实现细节

### 其他资源

- [Constitution](docs/constitution.md) - 项目宪法和设计原则
- [Banner](docs/banner.txt) - 应用启动横幅

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

### 开发流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

### 代码规范

- 遵循 Google Java Style Guide
- 编写单元测试覆盖核心逻辑
- 保持代码注释清晰
- 更新相关文档

### 提交规范

使用 Conventional Commits 规范:

```
feat: 添加新功能
fix: 修复 bug
docs: 更新文档
style: 代码格式调整
refactor: 代码重构
test: 添加测试
chore: 构建过程或辅助工具的变动
```

---

## 📄 开源协议

本项目采用 [Apache License 2.0](LICENSE) 开源协议，版权所有（Copyright）归 [codingas.com](https://codingas.com) 所有。

你可以自由使用、修改和分发本项目，但需保留原始版权声明，并在分发时附带一份本协议副本。详见 [LICENSE](LICENSE) 文件。

---

## 👥 团队

**作者**: Liu Ye  
**组织**: CodingAS  

---

## 🙏 致谢

感谢以下开源项目的支持:

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Sa-Token](https://sa-token.cc/)
- [OkHttp](https://square.github.io/okhttp/)
- [Flyway](https://flywaydb.org/)
- [Redisson](https://redisson.org/)

---

## 📞 联系方式

- **Issues**: [GitHub Issues](https://github.com/codingas/llm-gateway/issues)
- **Discussions**: [GitHub Discussions](https://github.com/codingas/llm-gateway/discussions)

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！**

Made with ❤️ by CodingAS Team

</div>
