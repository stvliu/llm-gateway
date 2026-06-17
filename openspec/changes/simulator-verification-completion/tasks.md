# 任务清单：Simulator 验证补全

## Phase 1：Simulator 管理 API E2E 测试

- [x] 1.1 新增行为序列 E2E 测试（POST/GET/DELETE /simulator/behavior + 请求消费验证）
- [x] 1.2 新增延迟配置 E2E 测试（POST/DELETE/GET /simulator/delay）
- [x] 1.3 新增流控制 E2E 测试（POST /simulator/stream + 流式请求验证）
- [x] 1.4 新增 API Key 覆盖 E2E 测试（POST/DELETE/GET /simulator/apikey-override + 请求验证）
- [x] 1.5 运行 Simulator 全部测试确认通过

## Phase 2：Gateway 全链路集成测试

- [x] 2.1 创建 FullContextIntegrationTestBase（Mock 认证+路由）
- [x] 2.2 创建集成测试配置（application-integration-test.yml）
- [x] 2.3 实现 Key 故障转移测试（2 个 Key、全部失败、熔断跳过）
- [x] 2.4 实现模型降级测试（主模型失败、降级链耗尽）
- [x] 2.5 实现跨协议转换测试（OpenAI→Anthropic / Anthropic→OpenAI）
- [x] 2.6 实现熔断器+行为序列测试（CLOSED→OPEN→HALF_OPEN→CLOSED）
- [x] 2.7 实现间歇故障恢复测试（交替 200/500）
- [x] 2.8 实现超时和流中断测试
- [x] 2.9 运行全部测试确认无回归

## 验证与收尾

- [x] 3.1 全量构建通过
- [x] 3.2 更新 docs/simulator-gateway-verification.md 标记完成项
- [x] 3.3 整理提交历史
