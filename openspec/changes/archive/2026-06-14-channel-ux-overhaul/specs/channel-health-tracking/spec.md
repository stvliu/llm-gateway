## ADDED Requirements

### Requirement: Channel 实体扩展健康状态字段

Channel 实体 SHALL 持久化最近一次连通性测试的结果摘要，以便列表与卡片视图能够直接呈现渠道健康状态而无需每次重新测试。

#### Scenario: 实体新增三个健康字段
- **WHEN** 系统启动并完成数据库迁移
- **THEN** channels 表存在 last_health_check_at（TIMESTAMP NULL）、last_health_status（VARCHAR NULL）、last_health_source（VARCHAR NULL）三列

#### Scenario: 健康状态枚举有限值
- **WHEN** last_health_status 字段被写入
- **THEN** 其值必须属于以下枚举：HEALTHY、DEGRADED、FAILED、UNKNOWN

#### Scenario: 健康来源枚举有限值
- **WHEN** last_health_source 字段被写入
- **THEN** 其值必须属于以下枚举：CARD、DRAWER、PRECHECK

#### Scenario: 未测试的渠道字段为 null
- **WHEN** 渠道从未执行过连通性测试
- **THEN** 三个健康字段保持 null，前端将渠道健康状态视为 UNKNOWN

### Requirement: 连通性测试结果聚合与持久化

系统 SHALL 提供专用的连通性测试 API，并按既定聚合规则将多 Key 测试结果聚合为单一健康状态写入 Channel 实体。

#### Scenario: 提供专用健康检查端点
- **WHEN** 客户端发起 POST /api/channels/{id}/health-check 请求，请求体包含 source 字段（CARD/DRAWER/PRECHECK）
- **THEN** 系统对该渠道下所有 Key 执行连通性测试，返回矩阵详情（每个 Key 的认证状态、可用模型数、延迟）+ 聚合状态

#### Scenario: 全部 Key 通过聚合为 HEALTHY
- **WHEN** 测试矩阵中所有 Key 的认证均成功且返回了至少一个可用模型
- **THEN** 聚合状态为 HEALTHY，写入 last_health_status

#### Scenario: 部分 Key 失败聚合为 DEGRADED
- **WHEN** 测试矩阵中至少一个 Key 通过且至少一个 Key 失败
- **THEN** 聚合状态为 DEGRADED，写入 last_health_status

#### Scenario: 全部 Key 失败聚合为 FAILED
- **WHEN** 测试矩阵中所有 Key 均认证失败或无任何可用模型
- **THEN** 聚合状态为 FAILED，写入 last_health_status

#### Scenario: 无 Key 时聚合为 UNKNOWN
- **WHEN** 渠道没有任何 Key
- **THEN** 聚合状态为 UNKNOWN，写入 last_health_status；不报错

#### Scenario: 持久化失败不阻断主流程
- **WHEN** 健康字段写入数据库失败
- **THEN** API 仍然返回测试矩阵详情，仅记录错误日志，不影响响应状态码

#### Scenario: 并发测试采用 last-write-wins
- **WHEN** 同一渠道在短时间内被多次触发测试
- **THEN** 最终持久化的 last_health_check_at 为最晚完成的那次，其他字段同步覆盖

### Requirement: Channel 列表响应包含健康状态

GET /api/channels 响应 DTO SHALL 在不破坏现有契约的前提下，附加返回三个健康字段，以便前端列表卡片直接渲染健康指示点。

#### Scenario: 列表 DTO 向后兼容地新增字段
- **WHEN** 客户端请求 GET /api/channels
- **THEN** 响应中每个渠道对象包含 lastHealthCheckAt、lastHealthStatus、lastHealthSource 三个字段（可为 null），原有字段保持不变

#### Scenario: 详情 DTO 同样包含字段
- **WHEN** 客户端请求 GET /api/channels/{id}
- **THEN** 响应包含三个健康字段
