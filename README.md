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

- **开发者**：一次接入，通过标准 OpenAI/Anthropic API 调用 50+ 主流模型，开箱即用
- **架构师**：零信任安全、智能降级、全链路可观测、云原生部署
- **管理者**：Token 限额控制、用量透明化、合规审计链
- **运维人员**：K8s 原生、Prometheus/Grafana 集成、零停机升级

---

## ✨ 功能特性

| 功能域 | 已实现 | 规划中 |
|--------|--------|--------|
| **API 网关** | OpenAI 兼容端点（`/v1/chat/completions`、`/v1/completions`）、Anthropic 兼容端点（`/v1/messages`）、SSE 流式转发（首 token ≤100ms）、协议互转 | 图像 / 语音 / 内容审核端点 |
| **Provider 管理** | Provider CRUD、多 Key 自动轮换与故障切换、默认 Key、负载均衡（优先级 + 权重）、渠道分组、熔断超时、HTTP/Socks5 代理 | — |
| **路由** | 模型级智能降级（额度不足 / 模型不可用自动切换）、场景路由、模型别名映射、可视化策略编排、自定义脚本扩展 | — |
| **用户与认证** | 用户 CRUD、用户名密码登录/登出 | OAuth（GitHub / Gitee / 企业微信 / 飞书 / 钉钉） |
| **密钥管理** | API Key CRUD、额度限制、模型白名单、IP 限制、过期时间 | — |
| **计量配额** | Token 输入/输出分别统计、用户级 / API Key 级 / 用户×渠道限额、请求次数配额 | — |
| **安全风控** | IP 黑白名单、UA 过滤、PII 脱敏、数据掩码、审计日志、密钥加密存储（AES-256-GCM） | 国密 SM2/SM3/SM4、WORM 审计链 |
| **可观测性** | Trace ID 全链路追踪、实时指标（延迟/QPS/Token/费用）、Prometheus / Grafana / Jaeger | — |
| **系统管理** | 应用渠道配置本地缓存（Caffeine + 事务后双 evict）、缓存手工失效端点（按应用 evict / 全清，仅 ADMIN） | — |
| **语义缓存** | — | 相似请求缓存（降本 30%+）、TTL 配置、命中率统计（pgvector） |
| **MCP 协议** | — | Resources / Prompts / Tools |

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
| **数据库迁移** | Flyway | 11.0.0 | 版本化 schema 迁移（V1~V70） |
| **缓存** | Redis + Caffeine | 7.x | 分布式缓存/会话 + 本地缓存 |
| **安全** | Sa-Token | 1.45.0 | 轻量级权限框架 |
| **HTTP 客户端** | OkHttp | 4.12.0 | 同步/异步调用 + SSE |
| **可观测性** | OpenTelemetry + Micrometer | 1.47.0 | 链路追踪 + Prometheus 指标 |
| **测试** | Testcontainers | 1.20.4 | 容器化集成测试 |

### 模块化架构

采用 **38 模块多模块 Maven 结构（17 个顶层 `gateway-*` 分组的三明治结构）**，业务按功能域拆分为核心模块 + JPA 绑定模块，协议层插件化：

| 分组 | 模块 | 职责 |
|------|------|------|
| 横切基础 | `gateway-common` | BaseDo/BaseEntity、异常、DTO、事件、工具、通用枚举 |
| 协议域 | `gateway-protocol/protocol`（+`protocol-openai`/`protocol-anthropic`/`protocol-gemini`） | Canonical IR + ProtocolAdapter SPI + 三协议插件 |
| 供给域 | `gateway-provider/provider`（+`provider-data`/`provider-starter`） | Provider / Channel / Model / Catalog / Upstream |
| 身份与访问 | `gateway-iam/iam`（+`iam-data`/`iam-starter`） | User / Application / UserApiKey / Auth / 加密 |
| 用量管控 | `gateway-usage/usage`（+`usage-data`/`usage-starter`） | Token 计量 / 配额 / 限流 |
| 安全与威胁 | `gateway-security/security`（+`security-data`/`security-starter`） | IP 威胁检测 + 数据脱敏 |
| 审计追溯 | `gateway-audit/audit`（+`audit-data`/`audit-starter`） | 调用日志 / 审计事件 |
| 告警通知 | `gateway-alert/alert`（+`alert-data`/`alert-starter`） | 告警通知 |
| 韧性 | `gateway-resilience/resilience`（+`resilience-data`/`resilience-starter`） | failover / retry / 熔断 |
| 模型代理 | `gateway-proxy/proxy`（+`proxy-starter`） | ChatDispatch 调度 / routing / 协议转换 |
| 聚合统计 | `gateway-stats/stats`（+`stats-starter`） | 仪表盘统计（读路径） |
| HTTP 承载 | `gateway-web` | Controller / Interceptor / Advice（web.api） |
| 启动装配 | `gateway-boot` | 应用配置 / 初始化种子 / 事件发布（boot.config/init/event） |
| 工具 | `gateway-cli` / `gateway-simulator` / `gateway-coverage` | CLI 管理工具 / LLM 模拟服务 / 覆盖率聚合 |
| 前端 | `gateway-console` | Web 管理界面 |

- **协议插件化**：OpenAI / Anthropic / Gemini 以插件形式通过 AutoConfiguration + `@ConditionalOnProperty` 启用，可扩展新协议
- **分层架构**：HTTP 承载归 gateway-web，启动装配归 gateway-boot；业务逻辑下沉各域核心模块，持久化实现位于 `-data` 绑定模块；分层依赖规则由 **Maven 模块边界 + ArchUnit 铁律**（`LayerDependencyTest`）强制执行

```
┌─────────────────────────────────────────────────────┐
│              HTTP 承载层 (gateway-web)                │
│         (Controller / Interceptor / Advice)         │
└──────────────────┬──────────────────────────────────┘
                   │ 调用域服务接口（依赖功能域核心模块）
┌──────────────────▼──────────────────────────────────┐
│               功能域核心模块                         │
│     (Service + Gateway 接口定义 + 业务逻辑)         │
└──────────────────┬──────────────────────────────────┘
                   │ 依赖倒置：核心仅定义接口，不依赖实现
┌──────────────────▼──────────────────────────────────┐
│            JPA 绑定模块 (gateway-*-data)             │
│       Gateway 实现 / Repository / dataobject        │
└─────────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 前置要求

- JDK 21+
- Maven 3.9+（仓库自带 `mvnw`）
- PostgreSQL 14+（可选，默认 H2）
- Redis 7.x+（可选，分布式部署用）

### 源码构建运行

```bash
# 编译打包（多模块）
./mvnw clean install -DskipTests

# 运行（默认 local profile：H2 文件持久化 + Caffeine，零外部依赖）
./mvnw spring-boot:run -pl gateway-boot

# 或直接运行 fat jar
java -jar gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar

# 连接 PostgreSQL 运行（需先创建数据库）
java -jar gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar --spring.profiles.active=postgresql
```

访问地址：API 服务 http://localhost:8080 · 健康检查 `/actuator/health` · Prometheus 指标 `/actuator/prometheus` · H2 控制台（local 模式）`/h2-console`

### Docker Compose (推荐)

```bash
cd deployments/docker
docker-compose up -d
```

包含 LLM-Gateway、PostgreSQL、Redis、Prometheus、Grafana、Jaeger + OpenTelemetry Collector。

单容器运行：

```bash
docker run -d --name llm-gateway -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local llm-gateway:latest
```

---

## 📡 API 文档

### OpenAI 兼容接口

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-your-api-key" \
  -d '{"model": "gpt-4o-mini", "messages": [{"role": "user", "content": "Hello!"}], "stream": false}'
```

> 流式请求将 `"stream"` 设为 `true`（SSE 返回）。

### Anthropic 兼容接口

```bash
curl -X POST http://localhost:8080/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: sk-your-api-key" \
  -d '{"model": "claude-3-haiku-20240307", "messages": [{"role": "user", "content": "Hello!"}], "max_tokens": 1000}'
```

### 管理 API

详见 [API 规格文档](docs/api-spec.md)。

---

## ⚙️ 配置说明

```
gateway-boot/src/main/resources/
├── application.yml              # 主配置（默认 local profile）
├── application-local.yml        # 本地调试（H2 文件库 + Caffeine，零外部依赖）
├── application-dev.yml          # 开发环境
├── application-prod.yml         # 生产环境
├── application-postgresql.yml   # PostgreSQL 独立配置
└── application-standalone.yml   # 单机部署配置
```

```yaml
# H2（默认 local profile，文件持久化，MODE=PostgreSQL 兼容）
spring.datasource.url: jdbc:h2:file:./data/gateway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE

# PostgreSQL（生产 / postgresql profile，账号密码经 DB_USERNAME/DB_PASSWORD 注入）
spring.datasource.url: jdbc:postgresql://localhost:5432/llm_gateway

# 缓存：spring.cache.type = caffeine（单机）| redis（分布式）
```

> schema 由 **Flyway** 版本化迁移管理（`gateway-boot/src/main/resources/db/migration/`，V1~V70），无需手动建表。更多配置详见 [技术架构文档](docs/技术架构.md)。

---

## 📊 监控与可观测性

- **Prometheus 指标**：`/actuator/prometheus`（Micrometer 标准 + 自定义指标）。已实现：`gateway.failover.triggered` / `gateway.failover.exhausted`；请求总量、延迟分布、Token 计量、缓存命中、限流拒绝等指标规划中
- **Grafana 仪表板**：Docker Compose 启动后访问 http://localhost:3000，预置请求概览、Token 统计、渠道健康、缓存效率、限流效果
- **日志**：`tail -f logs/llm-gateway.log`（错误日志 `llm-gateway-error.log`）；Docker 环境用 `docker logs -f llm-gateway`

---

## 🧪 测试

```bash
./mvnw test    # 单元测试（Surefire：*Test / *Tests）
./mvnw verify  # 集成测试（Failsafe：*IntegrationTest / *E2ETest，基于 Testcontainers）
```

性能目标 *(Roadmap)*：QPS 10,000/实例 · P95 < 500ms · P99 < 2000ms · SSE 并发 1,000

---

## 📦 部署指南

### 1. 系统安装包（推荐：非 Docker 一键部署）

默认 `local` profile（H2 文件持久化 + Caffeine，零外部依赖），装完即用：

- **Linux**：`apt install ./llm-gateway_*.deb` 或 `dnf install ./llm-gateway-*.rpm`
- **Windows**：双击 `llm-gateway-setup.exe`

安装时交互设置端口（默认 8080），加密密钥自动生成。详见 [deployments/package/README.md](deployments/package/README.md)。

### 2. Docker / Docker Compose

见[快速开始](#docker-compose-推荐)；配置文件详见 [deployments/docker/](deployments/docker/)。

### 3. Kubernetes（分布式高可用）

```bash
helm install llm-gateway deployments/helm/llm-gateway \
  --set replicaCount=3 \
  --set resources.requests.memory=512Mi \
  --set resources.limits.memory=2Gi
```

> 单机部署资源参考：最低 2 核 1 GB / 推荐 4 核 2 GB。详见 [技术架构 - 单机部署](docs/技术架构.md#144-标准版单机部署)。

### ⚠️ 重要提示

- **默认凭据**：`local` profile 自动创建 `admin/admin`，首次登录后请**立即修改密码**
- **H2 Console 风险**：`local` profile 开启 H2 Console（`/h2-console`）且允许远程访问，生产环境请关闭或限制
- **加密密钥备份**：系统安装包部署时 `GATEWAY_ENCRYPTION_KEY` 自动生成，**务必备份**，丢失则历史加密数据无法解密

---

## 📚 文档

- [需求规格说明书](docs/spec.md) - 完整的功能需求和非功能性需求
- [API 规格文档](docs/api-spec.md) - 详细的 API 接口定义
- [信息架构文档](docs/信息架构.md) - 模型和业务概念
- [应用架构文档](docs/应用架构.md) - 分层架构和模块设计
- [数据架构文档](docs/数据架构.md) - 数据库设计和 ER 图
- [技术架构文档](docs/技术架构.md) - 技术选型和实现细节
- [Constitution](docs/constitution.md) - 项目宪法和设计原则

---

## 🤝 贡献指南

1. Fork 本仓库并创建特性分支（`git checkout -b feature/amazing-feature`）
2. 提交更改（遵循 Conventional Commits：`feat` / `fix` / `docs` / `refactor` / `test` / `chore`）
3. 提交 Pull Request

代码要求：编写单元测试覆盖核心逻辑、保持中文注释清晰、更新相关文档。

---

## 📄 开源协议

本项目采用 [Apache License 2.0](LICENSE) 开源协议，版权所有（Copyright）归 [codingas.com](https://codingas.com) 所有。

---

**作者**: Liu Ye · **组织**: CodingAS
**Issues**: [GitHub Issues](https://github.com/codingas/llm-gateway/issues) · **Discussions**: [GitHub Discussions](https://github.com/codingas/llm-gateway/discussions)

<div align="center">

**⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！**

Made with ❤️ by CodingAS Team

</div>
