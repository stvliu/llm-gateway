# Simulator 验证补全 (Simulator Verification Completion)

## 问题背景

上一 change (`simulator-verification-enhancement`) 完成了 Simulator 自身增强（9 种模式、BehaviorSequence 等），但全链路 Gateway 集成验证仅完成了约 40%。

具体缺失场景：
- **Key 故障转移**: 多 Key 自动切换、全部 Key 失败、熔断跳过 Key 未在集成测试中验证
- **模型降级**: 主模型失败自动降级、降级链耗尽未验证
- **跨协议转换**: OpenAI→Anthropic / Anthropic→OpenAI 未验证
- **SSE 稳定性**: 流中断、非法数据、不完整流未通过 Gateway 验证
- **间歇故障**: BehaviorSequence 的能力未在 Gateway 集成测试中使用
- **鲁棒性边界**: 非法请求体、Content-Type 错误等未验证
- **Simulator 管理 API**: 行为序列/延迟/流控制/API Key 覆盖的管理 API 未通过 HTTP E2E 验证

## 目标

将 Gateway 全链路集成验证的覆盖率从约 40% 提升到约 100%，使所有韧性组件协同工作的场景都通过集成测试验证。

## 范围

### Phase 1：Simulator 集成测试（补齐剩余验证）

在 gateway-simulator 模块中新增 E2E 测试，验证增强后的管理 API 通过 HTTP 调用是否正常工作：

- 行为序列管理 API（POST/GET/DELETE /simulator/behavior）
- 延迟配置（POST/DELETE/GET /simulator/delay）
- 流控制（POST /simulator/stream）
- API Key 覆盖（POST/DELETE/GET /simulator/apikey-override）

### Phase 2：Gateway 全链路集成测试（核心补齐）

在 gateway-boot 中创建新的全链路集成测试，使用 Mock 认证+路由策略，让请求流经完整的七阶段调度链：

| 场景 | Mock 策略 | 验证点 |
|------|----------|--------|
| Key 故障转移 | Mock CredentialResolver 返回 2 个 Credential | Key1 失败→自动切换到 Key2 |
| 全部 Key 失败 | Mock CredentialResolver 返回 2 个失败 Key | 抛出"所有 Key 均失败" |
| 模型降级 | Mock DegradationService 返回降级模型 | gpt-4 失败→降级到 gpt-3.5 |
| 降级链耗尽 | Mock DegradationService 返回 null | 抛出 ProviderException |
| 跨协议 OpenAI→Anthropic | 配置跨协议 RoutingContext | 请求转换+响应转换 |
| 跨协议 Anthropic→OpenAI | 配置跨协议 RoutingContext | 请求转换+响应转换 |
| 流中断 | ProviderSimulator 流中断 | onError 被触发 |
| 熔断器+行为序列 | ProviderSimulator 连续 10 次 500 | 第 11 次 CircuitOpenException |
| 间歇故障恢复 | 交替 200/500 序列 | 重试后恢复 |
| 慢响应 | ProviderSimulator bodyDelay | 超时→TIMEOUT_ERROR |
| 非法请求体 | 发送损坏 JSON | Gateway 返回 400 |

## 非目标

- 不修改 Simulator 或 Gateway 核心代码
- 不涉及混沌测试套件
- 不涉及 UI 测试
- 不涉及性能测试

## 验收场景

1. Simulator 管理 API 的行为序列/延迟/流控制/API Key 覆盖全部通过 HTTP E2E 验证
2. Key 故障转移：多 Key 场景下失败 Key 自动切换到成功 Key
3. 模型降级：主模型失败后自动切换到备选模型
4. 跨协议转换：两种方向的协议转换均正确
5. 熔断器：通过行为序列验证 CLOSED→OPEN→HALF_OPEN 生命周期
6. 流中断：中断后 onError 被正确触发
7. 所有新增测试通过，不影响现有 66 个测试
