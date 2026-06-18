# Comet Design Handoff

- Change: simulator-verification-completion
- Phase: design
- Mode: compact
- Context hash: b72709f3f22d4c4bf40da9550bd87215dde238d4d93b74fb885b21bf761fa2a7

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/simulator-verification-completion/proposal.md

- Source: openspec/changes/simulator-verification-completion/proposal.md
- Lines: 1-64
- SHA256: 19e735933f52b8a976ea25d0f8ac11d71316090c5d561ae354628d8481b79930

```md
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
```

## openspec/changes/simulator-verification-completion/design.md

- Source: openspec/changes/simulator-verification-completion/design.md
- Lines: 1-178
- SHA256: 535ce7168f743acf2e41f58588f953d2648b5723611fbb35c3314a0602dc2943

[TRUNCATED]

```md
# Simulator 验证补全 — 设计文档

## 高层架构

### Phase 1：Simulator 集成测试补齐

在 `gateway-simulator` 的 `SimulatorEndToEndTest` 中新增 E2E 测试，通过 HTTP 调用验证增强后的管理 API。

```
SimulatorEndToEndTest (新增)
├── testBehaviorSequence_consumesStepsViaHttp()
│   POST /simulator/behavior {"sequence":[500,401,200],"loop":false}
│   → GET /v1/chat/completions 3 次 → 500/401/200
│   → 第 4 次恢复全局模式 NORMAL
│
├── testBehaviorSequence_loop_resets()
│   POST /simulator/behavior {"sequence":[200,500],"loop":true}
│   → GET 6 次 → 200/500/200/500/200/500
│
├── testDelayConfig_appliesDelay()
│   POST /simulator/delay {"delayMs":100}
│   → 测量响应时间 ≥ 100ms
│
├── testStreamConfig_interruptAfter()
│   POST /simulator/stream {"action":"interrupt_after","chunks":2}
│   → GET stream=true → 收到 2 chunk 后中断
│
├── testApiKeyOverride_matchesByPrefix()
│   POST /simulator/apikey-override {"keyPrefix":"sk-key1","mode":"auth_error"}
│   → 带 Bearer sk-key1-xxx 请求 → 401
│   → 带 Bearer sk-other-xxx 请求 → 200 (全局 NORMAL)
```

### Phase 2：Gateway 全链路集成测试

```
FullContextIntegrationTestBase
├── @MockBean AuthenticationDomainService (返回固定 Identity)
├── @MockBean CredentialResolver (返回可控的 Credential 列表)
├── @MockBean RoutingResolver (返回固定 RoutingContext)
├── @MockBean DegradationService (可选: 返回降级模型/抛出异常)
│
├── Upstream: ProviderSimulator (MockWebServer, 随机端口)
│   Gateway 配置 endpoint 指向 MockWebServer
│
└── 通过 ChatDispatchService.dispatch() 直接调用
    (绕过 Controller 层和认证拦截器)
```

### Mock 策略详情

| Mock Bean | 方法 | 固定返回值 |
|-----------|------|-----------|
| `AuthenticationDomainService` | `authenticateUser()` | `Identity.of(1L, "user", 100L)` |
| `CredentialResolver` | `resolveAll(channelId)` | 根据测试返回 1 或 2 个 `ChannelCredential` |
| `RoutingResolver` | `resolve()` | `RoutingContext` 指向 MockWebServer URL |

### 测试场景架构

```
测试类: FullContextIntegrationTest
├── Nested: KeyFailoverTests
│   ├── testKeyFailover_key1Fails_key2Succeeds()
│   │   Mock: CredentialResolver → [key1(401), key2(200)]
│   │   → KeyFailoverInvoker 遍历 2 个 Key
│   │   → key1 401 → key2 成功
│   │   → 返回 ProtocolResponse
│   │
│   ├── testKeyFailover_allKeysFail()
│   │   Mock: CredentialResolver → [key1(401), key2(401)]
│   │   → 两个 Key 都失败
│   │   → 抛出 ProviderException("所有 Key 均失败")
│   │
│   └── testKeyFailover_circuitBreakerSkips()
│       Mock: CredentialResolver → [key1(熔断), key2(200)]
│       → KeyFailoverInvoker 跳过熔断 Key
│       → key2 成功
│
├── Nested: DegradationTests
│   ├── testDegradation_primaryFails_fallbackSucceeds()
```

Full source: openspec/changes/simulator-verification-completion/design.md

## openspec/changes/simulator-verification-completion/tasks.md

- Source: openspec/changes/simulator-verification-completion/tasks.md
- Lines: 1-27
- SHA256: 43e7f78c14c62ff461498d889307587be68d9ba1a6f10cf79bd307ef8649518e

```md
# 任务清单：Simulator 验证补全

## Phase 1：Simulator 管理 API E2E 测试

- [ ] 1.1 新增行为序列 E2E 测试（POST/GET/DELETE /simulator/behavior + 请求消费验证）
- [ ] 1.2 新增延迟配置 E2E 测试（POST/DELETE/GET /simulator/delay）
- [ ] 1.3 新增流控制 E2E 测试（POST /simulator/stream + 流式请求验证）
- [ ] 1.4 新增 API Key 覆盖 E2E 测试（POST/DELETE/GET /simulator/apikey-override + 请求验证）
- [ ] 1.5 运行 Simulator 全部测试确认通过

## Phase 2：Gateway 全链路集成测试

- [ ] 2.1 创建 FullContextIntegrationTestBase（Mock 认证+路由）
- [ ] 2.2 创建集成测试配置（application-integration-test.yml）
- [ ] 2.3 实现 Key 故障转移测试（2 个 Key、全部失败、熔断跳过）
- [ ] 2.4 实现模型降级测试（主模型失败、降级链耗尽）
- [ ] 2.5 实现跨协议转换测试（OpenAI→Anthropic / Anthropic→OpenAI）
- [ ] 2.6 实现熔断器+行为序列测试（CLOSED→OPEN→HALF_OPEN→CLOSED）
- [ ] 2.7 实现间歇故障恢复测试（交替 200/500）
- [ ] 2.8 实现超时和流中断测试
- [ ] 2.9 运行全部测试确认无回归

## 验证与收尾

- [ ] 3.1 全量构建通过
- [ ] 3.2 更新 docs/simulator-gateway-verification.md 标记完成项
- [ ] 3.3 整理提交历史
```

