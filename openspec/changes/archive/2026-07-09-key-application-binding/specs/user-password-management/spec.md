## ADDED Requirements

### Requirement: 管理员重置用户密码

系统 SHALL 提供管理员重置他人密码端点，生成新随机密码并一次性返回明文，修复前端 `POST /api/v1/users/{id}/reset-password` 404 bug。

**API**: `POST /api/v1/users/{id}/reset-password`
- Response: `{ newPassword: String }`（HTTP 200）

**规则**:
- 调用方 MUST 具备管理员权限
- 系统 SHALL 生成随机密码，更新目标用户的密码哈希
- 系统 SHALL 一次性返回新密码明文，不持久化明文
- 禁止重置内建用户（`builtin=true`）密码，与现有内建用户保护策略一致

#### Scenario: 管理员重置密码成功

- **WHEN** 管理员调用 `POST /api/v1/users/{id}/reset-password`
- **THEN** 系统 SHALL 生成新随机密码并更新用户密码哈希
- **THEN** 系统 SHALL 一次性返回新密码明文（HTTP 200）
- **THEN** 用户使用新密码 SHALL 能登录成功

#### Scenario: 重置内建用户密码被拒绝

- **WHEN** 管理员重置内建用户（`builtin=true`）密码
- **THEN** 系统 SHALL 拒绝（4xx），不修改密码

#### Scenario: 重置不存在用户被拒绝

- **WHEN** 管理员重置不存在的用户 ID
- **THEN** 系统 SHALL 返回 404
