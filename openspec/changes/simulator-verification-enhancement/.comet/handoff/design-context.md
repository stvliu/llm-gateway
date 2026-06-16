# Comet Design Handoff

- Change: simulator-verification-enhancement
- Phase: design
- Mode: compact
- Context hash: 5bac77c0cd28674a8c994d21f87a926074d74d2e628d85c1fd3e804aa38d48ea

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/simulator-verification-enhancement/proposal.md

- Source: openspec/changes/simulator-verification-enhancement/proposal.md
- Lines: 1-54
- SHA256: 5bcfbd3a7a89d3ce03fc6681ee80439254f49e94321cfb7ba4fbbdfc8f9ec4c5

```md
# 模拟器验证增强 (Simulator Verification Enhancement)

## 问题背景

LLM-Gateway 具备完整的韧性链路（安全拦截 → 七阶段调度 → 降级 → Key 故障转移 → 熔断器 → 重试），但当前 gateway-simulator 仅有 `NORMAL` / `RATE_LIMITED` / `FAULT` 三种模拟模式，无法覆盖 Gateway 韧性组件的完整验证场景：

- 缺少 `401` / `400` / `503` 等错误模式，无法验证 `ErrorClassifier` 的完整错误映射
- 缺少行为序列能力，无法验证熔断器 `CLOSED → OPEN → HALF_OPEN → CLOSED` 生命周期
- 缺少延迟配置和流中断模拟，无法验证超时和 SSE 稳定性
- 缺少按 API Key 区分响应能力，无法验证 Key 故障转移

## 目标

对 gateway-simulator 进行增强，使其能够模拟完整的错误场景和韧性行为，并基于增强后的 Simulator 编写 Gateway 全链路集成测试，端到端验证所有韧性组件协同工作。

## 范围

### Phase 1：Simulator 增强（P0-P1）

| 增强项 | 优先级 | 说明 |
|--------|--------|------|
| 新增错误模式 | P0 | `AUTH_ERROR(401)`、`QUOTA_EXCEEDED(429+quota)`、`INVALID_REQUEST(400)`、`UPSTREAM_ERROR(500)`、`SERVICE_DOWN(503)`、`TIMEOUT(延迟超时)` |
| 行为序列（INTERMITTENT） | P0 | `POST /simulator/behavior` 按预定义序列返回响应，支持一次性/循环两种模式 |
| 延迟配置 | P1 | `POST /simulator/delay` 配置响应延迟，支持固定延迟和慢响应 |
| 流中断模拟 | P1 | `POST /simulator/stream` 控制流式响应（中断、非法数据等） |
| 按 API Key 区分响应 | P1 | 根据 Authorization Header 返回不同响应，用于 Key 故障转移验证 |

### Phase 2：Gateway 集成测试（P2）

基于 gateway-simulator 作为 Maven test dependency 引入 gateway-boot，编写全链路集成测试覆盖：

- 正常路径（双协议非流式/流式）
- 异常场景（429 重试、401 不重试、500 重试）
- 熔断器生命周期（CLOSED→OPEN→HALF_OPEN→CLOSED）
- Key 故障转移（多 Key 自动切换、全部 Key 失败）
- 模型降级（主模型失败自动降级到备选模型）
- 跨协议转换（OpenAI→Anthropic / Anthropic→OpenAI）
- 流中断和超时场景

## 非目标

- 不涉及混沌测试套件（Phase 3，本次不做）
- 不修改 Gateway 核心业务代码
- 不修改 gateway-simulator 的构建或部署方式
- 不涉及 UI 管理界面的修改

## 验收场景

1. Simulator 启动后可通过 Admin API 设置任意错误模式，验证返回对应 HTTP 状态码和错误体
2. Simulator 接受行为序列配置，按序列返回响应，循环模式可无限重复
3. Simulator 可配置响应延迟，支持固定延迟场景
4. Simulator 可在流式响应中间中断连接
5. Simulator 可根据不同 API Key 返回不同响应
6. Gateway 集成测试覆盖至少 8 个韧性场景，所有测试通过
```

## openspec/changes/simulator-verification-enhancement/design.md

- Source: openspec/changes/simulator-verification-enhancement/design.md
- Lines: 1-158
- SHA256: 68fb246c34ec942c2866325eb86e8e40efb4af66c3ac85c1de2599bad6a404bc

[TRUNCATED]

```md
# 模拟器验证增强 — 设计文档

## 高层架构

### Phase 1：Simulator 增强（gateway-simulator 模块）

```
┌───────────────────────────────────────────────────────────┐
│                  SimulatorModeService                      │
│                                                           │
│  ┌──────────────────┐   ┌──────────────────────────────┐ │
│  │  SimulatorMode    │   │  BehaviorSequence            │ │
│  │  (增强枚举)       │   │  - List<Step> steps          │ │
│  │                   │   │  - int currentIndex          │ │
│  │  NORMAL           │   │  - boolean loop              │ │
│  │  AUTH_ERROR       │   │                              │ │
│  │  RATE_LIMITED     │   │  getCurrentMode():            │ │
│  │  QUOTA_EXCEEDED   │   │    消费步进，返回当前模式      │ │
│  │  INVALID_REQUEST  │   │    循环/恢复全局模式          │ │
│  │  UPSTREAM_ERROR   │   └──────────────────────────────┘ │
│  │  SERVICE_DOWN     │                                     │
│  │  TIMEOUT          │   ┌──────────────────────────────┐ │
│  │  INTERMITTENT     │   │  DelayConfig                 │ │
│  └──────────────────┘   │  - long fixedDelayMs          │ │
│                         │  - boolean isActive            │ │
│  ┌──────────────────┐   └──────────────────────────────┘ │
│  │  StreamConfig    │                                     │
│  │  - action         │   ┌──────────────────────────────┐ │
│  │  - interruptAfter │   │  ApiKeyOverride              │ │
│  │  - invalidData    │   │  - Map<keyPrefix, mode>      │ │
│  └──────────────────┘   └──────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
```

### SimulatorMode 增强

当前 `SimulatorMode` 从 3 个值扩展到 9 个值：

```java
public enum SimulatorMode {
    NORMAL,             // 200 + 正常 JSON
    AUTH_ERROR,         // 401
    RATE_LIMITED,       // 429 + rate_limit_error
    QUOTA_EXCEEDED,     // 429 + insufficient_quota
    INVALID_REQUEST,    // 400
    UPSTREAM_ERROR,     // 500
    SERVICE_DOWN,       // 503
    TIMEOUT,            // 延迟超时
    INTERMITTENT        // 行为序列
}
```

### 响应模板增强

`SimulatorResponseTemplates` 新增错误模板：

| 模式 | 新增模板方法 | 对应 Error Type |
|------|-------------|-----------------|
| AUTH_ERROR | `openaiAuthError()` / `anthropicAuthError()` | authentication_error |
| QUOTA_EXCEEDED | `openaiQuotaExceeded()` / `anthropicQuotaExceeded()` | insufficient_quota |
| INVALID_REQUEST | `openaiInvalidRequest()` / `anthropicInvalidRequest()` | invalid_request |
| UPSTREAM_ERROR | 复用现有 server_error 模板 | server_error |
| SERVICE_DOWN | `openaiServiceDown()` / `anthropicServiceDown()` | service_unavailable |
| TIMEOUT | (延迟后返回 408) | timeout |

### 管理 API 增强

`SimulatorAdminController` 新增端点：

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/simulator/behavior` | 设置行为序列 `{sequence: [200, 401, 500], loop: false}` |
| GET | `/simulator/behavior` | 获取当前行为序列状态 |
| DELETE | `/simulator/behavior` | 清除行为序列，恢复全局模式 |
| POST | `/simulator/delay` | 设置响应延迟 `{delayMs: 5000}` |
| DELETE | `/simulator/delay` | 清除延迟配置 |
| POST | `/simulator/stream` | 控制流行为 `{action: "interrupt_after", chunks: 2}` |
| POST | `/simulator/apikey-override` | 设置 API Key 覆盖 `{keyPrefix: "sk-test-", mode: "auth_error"}` |
| DELETE | `/simulator/apikey-override/{keyPrefix}` | 清除指定 Key 的覆盖 |

```

Full source: openspec/changes/simulator-verification-enhancement/design.md

## openspec/changes/simulator-verification-enhancement/tasks.md

- Source: openspec/changes/simulator-verification-enhancement/tasks.md
- Lines: 1-33
- SHA256: 4f8fcf7d155e4ff57fa97caa831f56aa7dcb3de153b10f6b41cf147e63cb527d

```md
# 任务清单：Simulator 验证增强

## Phase 1：Simulator 增强

- [ ] 1.1 扩展 SimulatorMode 枚举（新增 AUTH_ERROR / QUOTA_EXCEEDED / INVALID_REQUEST / UPSTREAM_ERROR / SERVICE_DOWN / TIMEOUT / INTERMITTENT）
- [ ] 1.2 实现 BehaviorSequence 机制（支持一次性/循环序列，步进消费）
- [ ] 1.3 新增错误响应模板（AUTH_ERROR / QUOTA_EXCEEDED / INVALID_REQUEST / SERVICE_DOWN / TIMEOUT）
- [ ] 1.4 实现 SimulatorController 对新增模式的响应分发
- [ ] 1.5 实现管理 API：POST/GET/DELETE /simulator/behavior
- [ ] 1.6 实现延迟配置：POST/DELETE /simulator/delay
- [ ] 1.7 实现流控制：POST /simulator/stream（中断/非法数据）
- [ ] 1.8 实现按 API Key 区分响应：POST/DELETE /simulator/apikey-override
- [ ] 1.9 更新 SimulatorAdminController.parseMode 支持新枚举
- [ ] 1.10 编写新增功能的单元测试和 Controller 测试

## Phase 2：Gateway 集成测试

- [ ] 2.1 gateway-boot pom.xml 添加 gateway-simulator test dependency
- [ ] 2.2 创建 SimulatorGatewayIntegrationTest 基类和配置
- [ ] 2.3 实现正常路径测试（非流式/流式）
- [ ] 2.4 实现异常场景测试（429 重试 / 401 不重试 / 500 重试）
- [ ] 2.5 实现熔断器生命周期测试（CLOSED→OPEN→HALF_OPEN→CLOSED）
- [ ] 2.6 实现 Key 故障转移测试
- [ ] 2.7 实现模型降级测试
- [ ] 2.8 实现跨协议转换测试
- [ ] 2.9 实现超时和流中断测试
- [ ] 2.10 验证所有集成测试通过

## 验证与收尾

- [ ] 3.1 运行全部测试，确认无回归
- [ ] 3.2 更新 docs/simulator-gateway-verification.md 标记完成项
- [ ] 3.3 整理提交历史
```

