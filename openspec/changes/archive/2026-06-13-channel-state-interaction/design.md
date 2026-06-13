## Context

后端 Channel.State 和 ModelInstance.State 已有五状态枚举（PENDING/ACTIVE/SUSPENDED/DEPRECATED/RETIRED），含 isRoutable()、isTerminal()、canTransitionTo() 方法。前端仍在使用旧的二值模型（ACTIVE/INACTIVE）。Provider 和 ChannelCredential 已无状态字段。

## Goals / Non-Goals

**Goals:**
- 前端完整适配五状态模型，状态展示、操作、筛选全部对齐
- 后端状态转换 API 从二值开关改为受约束的 targetState 模式
- PENDING→ACTIVE 带前置校验和级联激活
- Provider 去掉启停开关和状态展示，改为纯组织分组 + 批量操作
- 高风险操作有分层确认保护

**Non-Goals:**
- 不改变路由算法（仅适配状态判断方式）
- 不增加状态变更审计日志（中期规划）
- 不做状态变更时间线 UI（需后端支持）
- 不做 Channel 的 deprecatedAt 字段（状态本身即标记）

## Decisions

### 决策 1：后端状态转换 API 设计

```
PUT /channels/{id}/state
Body: { "targetState": "SUSPENDED", "reason": "供应商维护" }
Response: ChannelResponse

PUT /channels/{channelId}/models/{modelId}/state
Body: { "targetState": "ACTIVE" }
Response: void
```

**理由**：PUT 语义明确（替换状态），targetState 由后端校验 canTransitionTo()，reason 可选用于审计。替代旧的 `PATCH ?enabled=true/false` 二值开关。

### 决策 2：Channel 状态转换前置条件与后置条件

| 转换 | 前置条件（强制） | 前置条件（建议/警告） | 后置条件 |
|------|-----------------|---------------------|---------|
| PENDING → ACTIVE | ≥1 Endpoint, ≥1 Credential, ≥1 ModelInstance | Endpoint 可达, ≥1 Credential 测试通过 | Channel 可路由; PENDING Instance → ACTIVE（级联）; 非 PENDING Instance 不变 |
| ACTIVE → SUSPENDED | 无 | 提示"已有连接不受影响" | Channel 不可路由; 已有连接不切断; Instance 状态不变 |
| ACTIVE → DEPRECATED | 无 | 检查是否为某模型唯一供给 Channel（强警告） | Channel 可路由低优先级; Instance 状态不变 |
| SUSPENDED → ACTIVE | 无 | 重新校验 Endpoint/Credential 完整性（仅警告，不阻塞） | Channel 可路由; Instance 状态不变 |
| SUSPENDED → DEPRECATED | 无 | 提示"将恢复路由（低优先级）" | Channel 可路由低优先级; Instance 状态不变 |
| SUSPENDED → RETIRED | 无 | 确认影响范围 | 终态不可逆; Channel 不可路由; Instance 因 Channel 不可路由而间接不可选 |
| DEPRECATED → RETIRED | 无 | 确认影响范围 | 终态不可逆; Channel 不可路由; Instance 因 Channel 不可路由而间接不可选 |

**PENDING → ACTIVE 级联规则**：激活 Channel 时，自动将同 Channel 下所有 PENDING 的 ModelInstance 设为 ACTIVE。其他状态（ACTIVE/SUSPENDED/DEPRECATED/RETIRED）不变。理由：避免"渠道激活了但没有流量"的断层。

**SUSPENDED → ACTIVE 不级联**：暂停前可能有实例已被单独暂停或标记下线，恢复渠道时不应擅自改变这些运维决策。

**前置条件校验与 API 行为**：
- 强制前置条件不满足 → API 返回 400 + 明确错误信息（如"请先添加端点"）
- 建议前置条件 → 后端不阻塞，前端在 UI 层给出警告提示

**"唯一供给"警告的计算**：前端在标记下线前，查询该 Channel 下每个 ModelInstance 关联的 Model，检查每个 Model 是否还有其他 ACTIVE Channel 供给。无替代的模型在确认弹窗中高亮警告。后端不强制此校验。

**路由层两级过滤**：Channel.isRoutable() AND ModelInstance.isRoutable() 双重条件决定最终可选中。即使 Channel ACTIVE 但没有 ACTIVE 实例，路由层也不会选中，不会出错，只是没有流量。

### 决策 3：Provider 无状态展示

Provider 是纯组织分组实体，不展示任何状态——既无独立 state 字段，也不做派生计算。Channel 的状态已在卡片/表格上一目了然，Provider 层再叠一层状态是冗余信息。

Provider 的交互简化为：
- 展示：名称、图标、Channel 数量、资源统计
- 操作：编辑、连通性测试、导出配置、批量暂停/恢复 Channel
- 无状态 Tag、无启停开关、无状态筛选

### 决策 4：确认策略分级

| 风险 | 操作 | 确认方式 |
|------|------|---------|
| 低 | PENDING→ACTIVE, SUSPENDED→ACTIVE | Popconfirm |
| 中 | ACTIVE→SUSPENDED | Popconfirm |
| 高 | ACTIVE→DEPRECATED, SUSPENDED→DEPRECATED, SUSPENDED→RETIRED, DEPRECATED→RETIRED | Modal + 原因/名称输入 |

### 决策 5：SUSPENDED→RETIRED 允许直达

canTransitionTo() 已支持 SUSPENDED→RETIRED，避免管理员被迫走 SUSPENDED→DEPRECATED→RETIRED 两步。

ACTIVE→RETIRED 不允许，必须经过 DEPRECATED 过渡期。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 旧 API 删除后前端未升级导致调用失败 | 前后端同分支同步修改 |
| 批量暂停 Provider 下所有 Channel 可能影响流量 | 确认弹窗展示影响范围 |
| PENDING→ACTIVE 前置校验过严阻止合理激活 | 只校验结构完整性（有 Endpoint/Key/Model），不校验可达性 |
