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
