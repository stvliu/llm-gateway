# Resilience Console Delta Spec

## MODIFIED Requirements

### Requirement: 容灾总览页

容灾总览页 SHALL 展示故障域拓扑、转移事件流与耗尽告警，移除降级/会话亲和/PinnedModel 相关展示。

**变更要点**:
- 故障域拓扑：展示 Cluster 分组（语义为跨供应商故障独立性分组），渠道按 clusterId 归域
- 转移事件流：事件字段 `clusterId` 保留，新增「是否共因跳过」标记展示
- 耗尽告警：保留（候选全耗尽事件）
- 移除：降级事件展示（L2 删除）、会话亲和状态、PinnedModel 状态

#### Scenario: 展示故障域拓扑

- **WHEN** 管理员访问容灾总览页
- **THEN** 页面 SHALL 展示 Cluster 分组拓扑，渠道按 clusterId 归域
- **THEN** 页面 SHALL NOT 展示已删除的 Cluster region/priority 字段

#### Scenario: 转移事件流展示 clusterId

- **WHEN** 转移事件流渲染事件
- **THEN** 事件 SHALL 展示 from→to 渠道、clusterId、错误类型、决策、是否共因跳过
- **THEN** 页面 SHALL 高亮共因跳过与耗尽事件

### Requirement: 应用管理页容灾模式选择

应用管理页 SHALL 移除容灾画像绑定，改为应用级 timeout 配置与渠道 priority 排序。

**变更要点**:
- 移除：容灾画像模板选择（ResilienceProfile 退场）、容灾模式档位选择
- 新增：应用 timeout 配置（直接在应用编辑表单）
- 新增：渠道授权页支持 priority 配置（拖拽或数值排序，定义 L1 转移先后次序）

#### Scenario: 应用编辑配置 timeout

- **WHEN** 管理员在应用编辑页配置 timeout
- **THEN** 系统 SHALL 保存到 `Application.timeout`
- **THEN** 页面 SHALL NOT 展示容灾画像绑定入口

#### Scenario: 渠道授权配置 priority

- **WHEN** 管理员在应用渠道授权页配置各渠道 priority
- **THEN** 系统 SHALL 保存到 `ApplicationChannel.priority`
- **THEN** 页面 SHALL 展示渠道按 priority 排序的先后次序
