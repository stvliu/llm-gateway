# Application Access Control Delta Spec

## MODIFIED Requirements

### Requirement: ApplicationChannel 渠道可见性

系统 SHALL 通过 `ApplicationChannel` 关联实体决定应用可见的渠道集合，并承载应用级转移顺序（priority）。

**实体字段**: `applicationId`（应用 ID）、`channelId`（渠道 ID）、`priority`（应用级转移顺序，数值越小越先试）；唯一约束 `(application_id, channel_id)`。

**新增字段 `priority`**:
- 应用级转移顺序，L1 候选列表按此升序排序
- 同一渠道对不同应用可有不同 priority（渠道A 对客服应用 priority=1，对内部工具 priority=3）
- 完全取代原全局 `ModelInstance.priority`（ModelInstance.priority 退场）
- 无主备之分，只有先后次序——所有候选资格平等，区别仅在尝试顺序
- 为 null 时回退默认值

**API**:
- `GET /api/v1/applications/{id}/channels` — 查询应用授权的渠道列表（含 priority）
- `PUT /api/v1/applications/{id}/channels` — 更新应用渠道授权（先清空旧关联，再批量保存新关联含 priority；HTTP 204）
  - Request Body: `{ "channels": [{ "channelId": 1, "priority": 1 }, { "channelId": 2, "priority": 2 }] }`

**规则**:
- 模型可见性不独立配置——由「渠道上挂哪些 ModelInstance」隐式决定
- 要某模型就授权挂该模型的渠道；无法「授权渠道但限模型」
- 转移顺序由管理员通过 ApplicationChannel.priority 在前端定义，跨供应商是 priority 排序的自然结果

#### Scenario: 查询应用渠道授权含 priority

- **WHEN** 管理员调用 `GET /api/v1/applications/{id}/channels`
- **THEN** 系统 SHALL 返回该应用授权的渠道列表，每项含 `channelId` 与 `priority`

#### Scenario: 更新应用渠道授权含 priority 全量替换

- **WHEN** 管理员调用 `PUT /api/v1/applications/{id}/channels` 传入含 priority 的 channels 集合
- **THEN** 系统 SHALL 先清空该应用的原有 `ApplicationChannel` 关联
- **THEN** 系统 SHALL 批量保存新的 `ApplicationChannel` 关联（含 priority）
- **THEN** 系统 SHALL 返回 HTTP 204

#### Scenario: 同渠道对不同应用不同 priority

- **WHEN** 渠道A 对客服应用配置 priority=1，对内部工具应用配置 priority=3
- **THEN** L1 候选排序 SHALL 按各自应用的 priority 独立排序
- **THEN** 客服应用候选列表渠道A 排前，内部工具应用候选列表渠道A 排后

#### Scenario: 渠道授权为空时无可用渠道

- **WHEN** 应用的 `ApplicationChannel` 授权集合为空
- **THEN** 该应用的所有 API Key 无可用渠道（自然拒绝）
