# Application Failure Strategy Delta Spec

> 新增能力：应用级失败处理策略。Application 通过单枚举字段配置失败处理模式（FAIL_FAST/FAIL_OVER/FAIL_RETRY 三选一），控制 L0/L1 故障转移行为，支持下游场景差异化（BI 快速失败、流程自动化失败转移、研发自动化失败重试）。

## ADDED Requirements

### Requirement: 应用级失败处理策略

系统 SHALL 在 Application 根实体上提供 `failureStrategy` 枚举字段（轻量单字段，不独立实体），承载该应用的失败处理模式。策略 SHALL 为三选一互斥：

- `FAIL_FAST`（快速失败）：第一个 Key 失败立即抛错，L0（同渠道换 Key）与 L1（换渠道）均不跑
- `FAIL_OVER`（失败转移）：L0 跑（同渠道换 Key）+ L1 跑（换渠道），全候选耗尽抛错
- `FAIL_RETRY`（失败重试）：L0 跑（同渠道换 Key），L1 不跑（不换渠道），同渠道 Key 耗尽抛错

`ChannelFailoverInvoker` SHALL 按候选所属应用的 `failureStrategy` 控制 L0/L1 行为：
- `FAIL_FAST`：候选首个 Key 失败立即抛错
- `FAIL_RETRY`：候选内 L0 换 Key 试完失败后抛错，不试下一渠道
- `FAIL_OVER`：候选内 L0 换 Key 试完失败后，按 `ApplicationChannel.priority` 试下一渠道，全耗尽抛错

默认值 SHALL 为 `FAIL_RETRY`。现有应用数据迁移 SHALL 设为 `FAIL_OVER`（保持原行为）。

#### Scenario: 快速失败策略

- **WHEN** 应用 `failureStrategy=FAIL_FAST`，候选首个 Key 调用失败
- **THEN** `ChannelFailoverInvoker` SHALL 立即抛出异常
- **THEN** 系统 SHALL NOT 试同渠道其他 Key，SHALL NOT 试下一渠道

#### Scenario: 失败重试策略（默认）

- **WHEN** 应用 `failureStrategy=FAIL_RETRY`，候选 Key-A 失败
- **THEN** `ChannelFailoverInvoker` SHALL 试同渠道其他 Key（L0）
- **THEN** 同渠道所有 Key 失败时 SHALL 抛出异常
- **THEN** 系统 SHALL NOT 试下一渠道（L1 不跑）

#### Scenario: 失败转移策略

- **WHEN** 应用 `failureStrategy=FAIL_OVER`，候选渠道所有 Key 失败
- **THEN** `ChannelFailoverInvoker` SHALL 按 `ApplicationChannel.priority` 试下一渠道
- **THEN** 所有渠道全耗尽时 SHALL 抛出最后异常

#### Scenario: 现有应用迁移保持行为

- **WHEN** 数据迁移执行
- **THEN** 现有应用 `failureStrategy` SHALL 设为 `FAIL_OVER`
- **THEN** 现有应用容灾行为 SHALL 保持不变（L0+L1 均跑）
