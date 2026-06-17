# 任务清单：Simulator 验证增强

## Phase 1：Simulator 增强

- [x] 1.1 扩展 SimulatorMode 枚举（新增 AUTH_ERROR / QUOTA_EXCEEDED / INVALID_REQUEST / UPSTREAM_ERROR / SERVICE_DOWN / TIMEOUT / INTERMITTENT）
- [x] 1.2 实现 BehaviorSequence 机制（支持一次性/循环序列，步进消费）
- [x] 1.3 新增错误响应模板（AUTH_ERROR / QUOTA_EXCEEDED / INVALID_REQUEST / SERVICE_DOWN / TIMEOUT）
- [x] 1.4 实现 SimulatorController 对新增模式的响应分发
- [x] 1.5 实现管理 API：POST/GET/DELETE /simulator/behavior
- [x] 1.6 实现延迟配置：POST/DELETE /simulator/delay
- [x] 1.7 实现流控制：POST /simulator/stream（中断/非法数据）
- [x] 1.8 实现按 API Key 区分响应：POST/DELETE /simulator/apikey-override
- [x] 1.9 更新 SimulatorAdminController.parseMode 支持新枚举
- [x] 1.10 编写新增功能的单元测试和 Controller 测试（56 个测试全部通过）

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
