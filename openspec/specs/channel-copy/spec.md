# channel-copy Specification

## Purpose
TBD - created by archiving change channel-copy. Update Purpose after archive.
## Requirements
### Requirement: 复制渠道配置

系统 SHALL 提供 `ChannelService.copy`，基于源渠道复制生成新渠道：继承源渠道的供应商（providerId）、计费模式、配额、超时、重试配置；复制全部端点与模型实例；新渠道状态重置为 ACTIVE，健康字段清空。凭证（API Key）复制由调用方参数控制，默认不复制。整个复制过程 SHALL 在单一数据库事务内完成。

#### Scenario: 正常复制渠道
- **WHEN** 管理员调用 copy(sourceId, override, copyCredentials=false)
- **THEN** 系统创建新渠道（name=override.name，providerId=源渠道），继承计费/配额/超时/重试，状态 ACTIVE，无健康记录；复制源渠道全部端点与模型实例（模型实例状态 ACTIVE）

#### Scenario: 复制凭证
- **WHEN** 管理员调用 copy(sourceId, override, copyCredentials=true)
- **THEN** 系统额外复制源渠道全部凭证（复用明文 Key，按既有加密策略重新加密存储）

#### Scenario: 源渠道不存在
- **WHEN** copy 传入的 sourceId 不存在
- **THEN** 系统抛出 ResourceNotFoundException（HTTP 404）

#### Scenario: 同供应商渠道重名
- **WHEN** override.name 在源渠道同一供应商下已存在
- **THEN** 系统抛出 DuplicateResourceException（HTTP 409），不创建任何实体

#### Scenario: 复制过程任意步骤失败整体回滚
- **WHEN** 复制端点 / 模型实例 / 凭证任意步骤抛出异常
- **THEN** 数据库事务回滚，不会出现部分创建的渠道或孤儿子实体

### Requirement: 渠道复制 API

系统 SHALL 提供 `POST /api/v1/channels/{id}/copy` 端点，请求体 `ChannelCopyRequest{ name(必填), copyCredentials(默认 false) }`，返回新建渠道的 `ChannelResponse`。

#### Scenario: 复制渠道
- **WHEN** 管理员 POST /api/v1/channels/{id}/copy 且 body 为 {"name":"new-name"}
- **THEN** 系统返回新建渠道（HTTP 200，含 endpoints 列表），复制源渠道端点与模型实例，不复制凭证

#### Scenario: 复制渠道并携带凭证
- **WHEN** 管理员 POST /api/v1/channels/{id}/copy 且 body 为 {"name":"new-name","copyCredentials":true}
- **THEN** 系统复制源渠道凭证到新渠道

#### Scenario: 名称缺失
- **WHEN** POST /api/v1/channels/{id}/copy 且 body 缺少 name
- **THEN** 系统返回 HTTP 400 校验错误

### Requirement: 复制渠道控制台入口

系统 SHALL 在通道管理控制台提供复制入口：通道表格操作列与通道卡片操作区各提供复制按钮；点击打开复制弹窗（预填源渠道名称，name 必填，含"同时复制 API Key"复选框，默认不勾选）。

#### Scenario: 表格行内复制
- **WHEN** 管理员点击通道表格某行的复制按钮
- **THEN** 打开复制弹窗，预填源渠道名称，提交后创建新渠道并刷新列表

#### Scenario: 卡片操作区复制
- **WHEN** 管理员点击通道卡片操作区的复制按钮
- **THEN** 打开复制弹窗，行为与表格行内复制一致

#### Scenario: 复制凭证复选框
- **WHEN** 管理员在复制弹窗勾选"同时复制 API Key"
- **THEN** 提交后新渠道连同源渠道凭证一并创建

