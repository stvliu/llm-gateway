---
title: 能力标准化定义与插件化接入设计
status: draft
date: 2026-08-11
scope: gateway-core / gateway-capability-api / gateway-capability-*
---

# 能力标准化定义与插件化接入设计

> 通过**能力标准化定义**（Canonical IR + Capability 模型 + 能力注册表）与**能力供给/接入的插件化**（Spring Boot Starter 构建期模块化），提升 llm-gateway 能力接入的灵活性、快速性。

## 1. 背景与目标

### 1.1 现状痛点

当前协议与能力接入是**硬编码**的：

- `ProtocolConverter` 是**硬编码的 2 协议双向转换器**，仅支持 OpenAI ↔ Anthropic，且为手写字段映射（system 提取、tools 转换、流式 chunk、stop_reason 映射）。`Protocol` 枚举虽预留 `GEMINI`/`NATIVE` 但未实现。
- 协议组件（`ProtocolTuner`/`ProtocolValidator`）已抽象成接口，但协议集合写死、依赖 Spring 组件发现。
- `Model` 实体的 `capabilities: Map<String,Boolean>` 与 `modalities: List<String>` 字段**仅存储、未参与协议选择/转换/路由逻辑**。
- 新增一个协议/能力类型 = 修改核心转换器，组合爆炸（N×N 两两转换）、难以维护、回归风险高。

### 1.2 目标

1. **灵活性**：新增协议、模型能力项、新能力类型时，核心零改动。
2. **快速性**：接入新能力 = 新增一个能力模块 + 少量配置，无需改核心。
3. **可演进**：协议层围绕**规范内部模型（Canonical IR）**重建，从根上消除 N×N 转换。

## 2. 设计决策（brainstorming 结论）

| # | 决策点 | 结论 |
|---|--------|------|
| D1 | 能力范畴 | **三层全覆盖**：协议接入、模型能力项、新能力类型 |
| D2 | 插件形态 | **Spring Boot Starter 构建期模块化**（放弃外部 JAR 热插拔，换取大幅简化与 Spring 生态惯用） |
| D3 | 插件契约 | `gateway-capability-api` **稳定 SPI（纯接口）**，能力模块只依赖它，与核心实现解耦 |
| D4 | 插件粒度 | 按**协议 / 能力类型**划分，**不按厂商、不按模型** |
| D5 | 健康机制 | 复用 **Spring Boot Actuator**（`HealthIndicator`/`HealthGroup`）；连通性探测复用现有 `ConnectivityTester`/`ChannelKeyProbe`；自建 `@Scheduled` 周期探活 |
| D6 | 数据归属 | **Provider 固有信息**可进插件种子；**Model 规格（含能力）为动态数据，进 DB + 自动发现**；渠道/凭证/配额/启停状态必须留 DB |
| D7 | Canonical IR 范围 | **本轮仅覆盖 chat 类**；embedding/rerank 等非 chat 能力类型后置（YAGNI） |
| D8 | 转换策略 | **每协议一个 Adapter 做"原生↔规范"**，任意两协议互转 = 2 跳到规范，消除 N×N |

## 3. 整体架构

```
gateway/                            # 父 POM
├── gateway-core                    # 核心（现有 gateway-boot 演进）
│   ├── protocol/contract/          # Canonical IR（规范内部模型）
│   ├── capability/                 # Capability 模型 + CapabilityRegistry
│   ├── capability/health/          # 健康聚合（复用 Actuator）
│   ├── protocol/adapter/           # ProtocolAdapter SPI
│   └── routing/                    # 能力感知路由
│
├── gateway-capability-api          # 能力 SPI 契约（纯接口，能力模块依赖它）
│
├── gateway-capability-openai       # 内置能力模块 A（Starter 风格）
├── gateway-capability-anthropic    # 内置能力模块 B
├── gateway-capability-gemini       # 示例/未来能力模块（可选）
└── gateway-boot                    # 组装：依赖核心 + 所需能力模块，只做装配
```

**分层要点**：
- `gateway-capability-api` 是**稳定契约**，能力模块只依赖它，与核心实现解耦。
- 每个能力模块含自己的 `AutoConfiguration`，用 `@ConditionalOnProperty` 条件装配，向 `CapabilityRegistry` 注册其 `ProtocolAdapter`/`HealthIndicator`/能力描述符。
- `gateway-boot` 只负责依赖并启用哪些能力模块（`pom.xml` 即"插件清单"），**不写协议转换逻辑**。

## 4. Canonical IR（规范内部模型）

### 4.1 设计

以**规范内部模型**为中立表示，每协议一个双向 Adapter：

```
         ┌────────────────────────────────────────────┐
         │  Canonical IR（规范内部模型）                  │
         │  CanonicalChatRequest / CanonicalResponse    │
         │  (与厂商无关的中立表示)                        │
         └────────────────────────────────────────────┘
              ▲                        ▲
    normalize │                        │ denormalize
              ▼                        ▼
    ┌────────────────┐        ┌────────────────┐
    │ OpenAIAdapter   │        │ AnthropicAdapter│
    │ 原生↔规范        │        │ 原生↔规范        │
    └────────────────┘        └────────────────┘
```

- **新增协议 = 新增一个 Adapter 模块**，核心零改动。
- 现有 `ProtocolConverter` 的手写映射逻辑平移到各自 Adapter 的 normalize/denormalize，测试可迁移复用。

### 4.2 Canonical IR 字段（中立表示）

| 概念 | 规范字段 |
|------|---------|
| 请求 | `model`, `messages[]`(role/content), `system`, `temperature`, `maxOutputTokens`, `stop`, `tools[]`, `toolChoice`, `stream`, `modalInput[]`(多模态) |
| 响应 | `id`, `model`, `textBlocks[]`, `toolUses[]`, `stopReason`, `usage(input/output)` |
| 流式 | 规范 chunk 事件（`textDelta`/`toolUseDelta`/`messageStart`/`messageStop`） |
| 元数据 | `protocol`, `capabilitiesUsed[]` |

- **能力感知**：规范模型能表达"本次请求用到了哪些能力"（function calling、vision、streaming），供能力感知路由使用。
- **配置外部化**：多模态、tools 等走 `@ConfigurationProperties`，不做魔法数字。

## 5. Capability 模型与能力注册表

### 5.1 Capability 描述符（统一三层）

```
Capability {
  id            // "openai.chat" / "anthropic.chat" / "gemini.chat" / "mcp.tool"
  type          // PROTOCOL_ADAPTER | MODEL_CAPABILITY | CAPABILITY_TYPE
  protocol      // 该能力对应的协议（chat 类）
  name/desc
  features      // 能力项集合: {function_call, streaming, vision, json_mode, multimodal...}
  config        // @ConfigurationProperties 绑定的能力配置
}
```

### 5.2 CapabilityRegistry（核心）

- **注册**：能力模块 `AutoConfiguration` 启动时把 `Capability` 描述符 + `ProtocolAdapter` + `HealthIndicator` 注册进来。
- **查询**：按 `id`/`protocol`/`feature` 查询。
- **路由消费**：路由层读注册表判断"当前请求需要的能力由哪些协议/渠道提供"。

### 5.3 能力感知路由

- 复用并打通现有 `Model.capabilities`（DB 字段，当前未参与决策）。
- 流程：请求解析出 `capabilitiesUsed[]` → 路由层在候选渠道/模型中**过滤出声明了该能力的** → 再走现有 `ModelMatcher`/`PriorityRouter` 等链路。
- 与现有路由链（`RouterChain`/`PriorityRouter`/`LoadBalanceRouter`）**插入式结合**，不重写整条路由。
- 请求指定模型但该模型不满足某能力 → **自动降级到满足能力的同族模型**（或按策略报错）。

## 6. 数据归属（Provider/Model/渠道）

| 数据 | 归属 | 理由 |
|------|------|------|
| Provider 固有信息（code/name/logo/apiDocUrl/默认priority） | **插件种子**（可选） | 相对静态 |
| 能力词表 `Capability` | **插件**（SPI/注册表） | 协议能力静态 |
| **Model 规格**（含 capabilities/modalities） | **DB** | 高频变化，需运行时更新 |
| 渠道/端点/凭证/配额/启停状态 | **DB** | 运行时 + 安全（含密钥） |

**模型能力自动发现**：
- 复用并扩展现有 `ChannelKeyProbe`（其 `KeyTestResult` 已含可用模型列表）。
- 通道连通时（或定时刷新）**拉取厂商模型列表 + 探测各模型能力**，**自动同步进 DB**。
- 新模型出现 → 重连/刷新即自动补进 DB，无需改插件、无需管理员手动录入。
- `Model.capabilities` 数据源 = **自动发现 + 管理员手动录入**（双通道）。

**审计**：种子/自动发现导入记录为**系统操作**（`created_by=system`），符合全实体可审计要求；种子导入不覆盖已有 DB 记录。

## 7. 能力健康机制

### 7.1 复用 Spring Boot Actuator

| 我们要的 | Spring Boot 提供 |
|---------|-----------------|
| 每能力探活 | `HealthIndicator`（每能力模块注册一个 bean） |
| 健康状态模型 | `Health.Status`（UP/DOWN/OUT_OF_SERVICE/UNKNOWN）+ 自定义 |
| 能力级聚合 | `HealthGroup` / `HealthAggregator` |
| 对外暴露/告警 | `/actuator/health`，接 alert 域 |

### 7.2 分工点

1. **能力健康 = 每能力一个 `HealthIndicator` bean**，由能力模块 `AutoConfiguration` 注册，`@ConditionalOnProperty` 控制启停（取代自定义 `HealthProbe`）。
2. **连通性探测逻辑复用现有机制**：协议能力的 `HealthIndicator.health()` 内部委托现有 `ConnectivityTester`/`ChannelKeyProbe`；`ChannelHealthStatus`（HEALTHY/DEGRADED/FAILED/UNKNOWN）映射到 Spring `Health.Status`。
3. **周期调度自建薄层**：Spring Boot 不内置定时探活。补 `@Scheduled` 任务定时调用各 indicator / 探测并**缓存结果**，与 `/actuator/health` 外部按需轮询并存。
4. **隔离查询**：定义独立健康组（如 `/actuator/health/capabilities`），与基础设施健康（DB/Redis/磁盘）分开。

## 8. 能力模块 Starter 结构

```
gateway-capability-openai/
├── pom.xml                        # 依赖 gateway-capability-api
├── src/main/resources/META-INF/spring/
│   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── src/main/java/.../
    ├── OpenAIAdapterAutoConfiguration   # @AutoConfiguration + 条件装配
    ├── OpenAIProtocolAdapter            # 原生↔规范（继承 SPI）
    └── OpenAIHealthIndicator            # 健康（委托 ConnectivityTester）
```

**AutoConfiguration 注册三步**：
1. `@ConditionalOnProperty(gateway.capability.openai.enabled=true)` — 配置开关
2. 注册 `Capability` 描述符 → `CapabilityRegistry`
3. 注册 `ProtocolAdapter` + `HealthIndicator` bean

**新增一个能力 = 加一个 Maven 依赖 + 一段配置**，核心零改动。

## 9. 落地顺序

1. **IR 落地**：在核心引入 Canonical IR + `ProtocolAdapter` SPI（`gateway-capability-api`）。
2. **迁移**：把现有 `ProtocolConverter` 逻辑平移为 openai/anthropic 两个 Adapter，测试迁移，确保存量行为不变。
3. **Capability 体系**：`Capability` 模型 + `CapabilityRegistry` + 能力感知路由 + 健康组。
4. **模块化拆分**：拆出 `gateway-capability-openai/anthropic` 两个 Starter 模块，`gateway-boot` 改为组装。
5. **示例插件**：Gemini 协议适配、embedding 能力类型，验证"加依赖即接入"。

## 10. 测试策略

- **适配器层**：normalize/denormalize 双向单测 + 协议对拍测试（迁移现有转换测试）≥80%。
- **核心层**：CapabilityRegistry、能力感知路由 ≥85%（符合 CLAUDE.md 覆盖率要求）。
- **集成**：`gateway-boot` 装配后跑现有端到端用例，回归保护。

## 11. 边界与 YAGNI

- Canonical IR 本轮仅覆盖 chat 类；embedding/rerank 等非 chat 能力类型后置，届时定义各自的规范模型（如 `CanonicalEmbeddingRequest`）。
- 不做外部 JAR 热插拔、运行期启停管理 API、生命周期状态机（交给 Spring bean 生命周期）。
- 不做插件依赖管理/版本协商（交给 Maven）、不做 JAR 签名校验（构建期由 CI/仓库保证）。
- 模型能力自动发现若工作量过大，可在第 5 步示例插件之后单独评估；本轮先保留"管理员手动录入"兜底通道。
