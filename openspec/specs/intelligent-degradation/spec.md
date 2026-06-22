# Intelligent Degradation
## Purpose

智能降级能力——主模型不可用时按降级链切换备选模型，受错误分流与画像门禁约束，定位为 L2 应用可选兜底。

## Requirements

### Requirement: 降级链配置

系统 SHALL 支持配置模型降级链，定义主模型不可用时的备选模型列表和回切策略。

```yaml
gateway:
  degradation:
    enabled: true
    chains:
      - primary: "gpt-4o"
        fallbacks: ["claude-sonnet-4", "gpt-4o-mini"]
        recovery:
          check-interval: 60s
          success-threshold: 3
      - primary: "claude-opus-4"
        fallbacks: ["gpt-4o"]
        recovery:
          check-interval: 120s
          success-threshold: 5
    max-chain-depth: 5
```

#### Scenario: 配置合法时加载降级链
- **WHEN** 应用启动时加载降级配置
- **THEN** 所有降级链被解析并注册到 `DegradationService`

#### Scenario: 配置循环引用时拒绝加载
- **WHEN** 降级链配置存在循环引用（A→B→C→A）
- **THEN** 启动时抛出配置异常，提示循环引用路径

### Requirement: 降级触发

`DegradationService` SHALL 在以下条件触发降级：

| 触发条件 | 判定方式 | 触发动作 |
|---------|---------|---------|
| 主模型上游 429/5xx | `ProviderException` 异常类型 | 切换到备选模型 |
| 主模型熔断器 OPEN | `CircuitOpenException` | 切换到备选模型 |
| 主模型 Token 限额超限 | `QuotaExceededException` | 切换到备选模型 |
| 主模型超时 | `TIMEOUT_ERROR` | 切换到备选模型 |

#### Scenario: 主模型不可用时自动降级到备选模型
- **WHEN** 调用主模型抛出 `CircuitOpenException`
- **THEN** `DegradationService.degrade()` 返回降级链中第一个可用备选模型名

#### Scenario: 降级链中所有模型均不可用时抛出异常
- **WHEN** 降级链中所有模型均不可用
- **THEN** 抛出 `ProviderException("ALL_MODELS_DEGRADED")`

#### Scenario: 降级事件记录审计日志
- **WHEN** 降级发生
- **THEN** 审计日志记录 `from_model`、`to_model`、`reason`、`chain_step` 字段

### Requirement: 降级通知

降级发生时 SHALL 通过 `DomainEventPublisher` 发布 `DegradationEvent`：

```java
DegradationEvent {
    String traceId;
    Long userId;
    String originalModel;
    String fallbackModel;
    DegradationTrigger reason;
    int chainStep;       // 降级链第几步
    Instant triggeredAt;
}
```

#### Scenario: 降级时发布 DegradationEvent
- **WHEN** `DegradationService.degrade()` 成功返回备选模型
- **THEN** 发布 `DegradationEvent`，包含降级原因和链步数

### Requirement: 自动回切

`DegradationService` SHALL 定期检查已降级的主模型是否恢复：

1. 每 `check-interval` 执行一次健康检查（调用 `testConnectivity()` 或发送最小请求）
2. 连续成功达到 `success-threshold` 次后，标记模型为已恢复
3. 标记恢复后，下次请求自动切回主模型

#### Scenario: 主模型恢复后自动切回
- **WHEN** 主模型连续 N 次健康检查成功（N = success-threshold）
- **THEN** `DegradationService.canRecover()` 返回 `true`，下次请求切回主模型

#### Scenario: 回切时发布恢复事件
- **WHEN** 模型从降级状态恢复并切回
- **THEN** 发布 `DegradationRecoveredEvent`，包含 `model` 和 `downtime` 字段

### Requirement: Metrics 埋点

系统 SHALL 上报以下降级相关 Micrometer 指标，用于监控降级触发、恢复与当前降级状态：

| 指标名 | 类型 | 标签 | 触发点 |
|--------|------|------|--------|
| `gateway.degradation.triggered` | Counter | `from_model`, `to_model`, `reason` | 降级切换 |
| `gateway.degradation.recovered` | Counter | `model` | 模型恢复回切 |
| `gateway.degradation.active` | Gauge | `model` | 当前处于降级状态的模型数 |

#### Scenario: 降级触发时上报 Metrics
- **WHEN** `DegradationService.degrade()` 被调用并返回备选模型
- **THEN** `gateway.degradation.triggered` Counter 自增 1

#### Scenario: 模型恢复时上报 Metrics
- **WHEN** 模型从降级状态恢复
- **THEN** `gateway.degradation.recovered` Counter 自增 1
