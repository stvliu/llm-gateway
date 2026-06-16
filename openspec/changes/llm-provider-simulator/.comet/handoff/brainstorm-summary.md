# Brainstorm Summary

- Change: llm-provider-simulator
- Date: 2026-06-16

## 确认的技术方案

**方案 A：轻量 Controller 模式**

第一阶段（测试工具包）：
- `ResponseTemplates`：纯静态工具类，提供 OpenAI/Anthropic 的非流式、流式、错误 JSON 模板
- `ProviderSimulator`：封装 MockWebServer，实现 AutoCloseable，提供 enqueue 系列方法 + 客户端工厂方法
- 流式测试使用 CountDownLatch + AtomicBoolean 等待异步回调完成
- `OpenAIUpstreamClientTest` 和 `AnthropicUpstreamClientTest` 各 ~8 个测试场景

第二阶段（独立模拟服务）：
- `gateway-simulator` 独立 Maven 模块，轻量 Controller 模式
- `SimulatorController`：POST `/v1/chat/completions` + POST `/v1/messages`（含 SseEmitter 流式支持）
- `SimulatorAdminController`：POST `/simulator/mode` + GET `/simulator/requests`
- `SimulatorModeService`：持有模式状态（NORMAL/RATE_LIMITED/FAULT）和环形缓冲请求记录
- `SimulatorResponseTemplates`：复制自第一阶段模板逻辑
- 配置：`simulator.port=9090`、`simulator.mode=normal`、`simulator.request-log-capacity=100`

## 关键取舍与风险

- 模板复制导致代码重复 → 模板为纯字符串常量，重复可接受
- 流式异步测试可能不稳定 → CountDownLatch + 超时 + AtomicBoolean 三重保障
- 模拟器无延迟模拟 → 第一阶段 MockWebServer 已覆盖，不在模拟器范围内
- gateway-simulator 需要手动启动 → 文档说明启动命令

## 测试策略

- 第一阶段：MockWebServer 集成级单元测试（~16 场景）
- 第二阶段：SimulatorModeService 单元测试 + Spring Boot 集成测试
- 全量回归测试

## Spec Patch

无
