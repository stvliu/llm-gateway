# Brainstorm Summary

- Change: simulator-verification-completion
- Date: 2026-06-17

## 确认的技术方案

### 集成测试架构
- 使用 `@SpringBootTest` 启动完整 Gateway Spring Context
- `@MockBean AuthenticationDomainService` → 固定 Identity (1L, "user", 100L)
- `@MockBean CredentialResolver` → 返回可控 ChannelCredential 列表
- `@MockBean RoutingResolver` → 返回固定 RoutingContext 指向 MockWebServer
- `@MockBean DegradationService` → 返回降级模型或 null
- **真实**: UpstreamClientRegistry, ResilientClientFactory, ChannelEndpointCircuitBreakerManager
- **上游**: ProviderSimulator (MockWebServer)

### Key 故障转移
- `ChannelCredential` 通过 setter 构造（@Data Lombok）
- Key1 401 → KeyFailoverInvoker 遍历 → Key2 200 → 成功
- 全部失败 → ProviderException("所有 Key 均失败")
- 熔断跳过 → circuitBreakerManager.isAvailable() = false 时跳过

### 模型降级
- Mock DegradationService.degrade() 返回 "gpt-3.5-turbo"
- DegradationInvoker 捕获 ProviderException → 降级 → 递归调用
- 降级链耗尽 → degrade() 返回 null → 原异常抛出

### 跨协议转换
- RoutingContext.upstreamProtocol 设为 ANTHROPIC/OPENAI
- ChatDispatchService.dispatch() 走 needsProtocolAdaptation=true 路径
- 验证请求转换 + 响应转换

### Simulator E2E 测试
- 在 SimulatorEndToEndTest 中新增 6 个测试
- 通过 HTTP 调用管理 API + LLM API 验证

## 关键取舍与风险

| 取舍 | 决策 | 原因 |
|------|------|------|
| DegradationService Mock vs 真实 | Mock | 避免配置和健康状态依赖 |
| RoutingResolver Mock vs 真实 | Mock | 避免数据库查询 |
| CredentialResolver Mock vs 真实 | Mock | 避免数据库查询 |
| UpstreamClientRegistry Mock vs 真实 | 真实 | 需要验证真实的 UpstreamClient 调用 |

**风险**: DegradationInvoker 降级后调用 routingResolver.resolve() 需要 Mock 也处理新模型名 → Mock 始终返回同一个 RoutingContext。

## 测试策略

- Simulator E2E: 6 个测试（behavior/delay/stream/apikey-override）
- Gateway 全链路: ~10 个测试（Key 故障转移 3 + 降级 2 + 跨协议 2 + 熔断器 1 + 间歇故障 1 + 超时 1）

## Spec Patch

无。
