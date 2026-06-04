# 团队页面列表操作栏改造设计

## 背景

当前团队页面操作栏存在以下问题：
- 操作按钮 6 个（渠道管理、模型可见性、密钥管理、成员管理、编辑、删除），过多且列宽过大
- 成员管理区分 owner/admin/member 角色，复杂度与当前业务需求不匹配
- "渠道管理"命名不够直观
- 团队列表缺少搜索筛选能力
- 成员数仅展示数字，缺少交互

## 改造内容

### 1. 简化角色模型

- 成员只保留一种角色，去掉 owner/admin/member 区分
- 团队列表表格移除"角色"列
- 成员管理弹窗移除角色相关 UI（角色 Select、角色 Tag）

### 2. 操作按钮精简为 4 个（全部平铺，视觉分层）

| 按钮 | 图标 | 样式 | 行为 | 权限控制 |
|------|------|------|------|---------|
| 成员 | TeamOutlined | link + 文字 | 打开成员管理弹窗 | 无 |
| 渠道权限 | SafetyOutlined | link + 文字 | 打开渠道权限弹窗（原渠道管理） | 无 |
| 编辑 | EditOutlined | text 纯图标 | 打开编辑弹窗 | canWrite |
| 删除 | DeleteOutlined | text 纯图标 + danger | Popconfirm 确认后删除 | canWrite |

移除的操作：模型可见性、密钥管理

**视觉层级**：高频操作（成员、渠道权限）带文字标签，低频操作（编辑、删除）纯图标，缩短操作列宽度。

### 3. 成员管理弹窗改造

**交互模式**：已选成员 + 搜索添加，与权限弹窗风格统一但适配用户量大场景

**改造前**：
- 顶部"添加成员"行（用户 Select + 角色 Select + 确定按钮）
- 每行有角色 Select 和"移除"按钮
- footer 为 null

**改造后**：
- 上半区：已选成员列表（Tag 展示，每个 Tag 带关闭按钮可移除）
- 下半区：搜索框 + 搜索结果列表（点击添加到已选）
- Modal footer 保留"保存"和"取消"按钮
- 打开时从 team.members 提取已加入成员作为初始已选
- 保存时 diff 计算增量操作

**数据流**：
1. 打开弹窗 → 从 team.members 提取已加入成员 ID 和名称
2. 渲染已选成员 Tag 列表 + 搜索框
3. 用户输入搜索关键词 → 调用 `users.list({ keyword })` 搜索
4. 搜索结果中点击用户 → 添加到已选列表
5. 已选 Tag 点击关闭 → 从已选列表移除
6. 点击保存 → 对比原始成员列表与当前选中，计算新增和移除：
   - 新增：selectedUserIds - 原始成员 IDs → 逐个调用 `teamApi.addMember(teamId, { userId, role: 'member' })`
   - 移除：原始成员 IDs - selectedUserIds → 逐个调用 `teamApi.removeMember(teamId, userId)`
   - 并行执行所有 addMember 和 removeMember
7. 保存成功 → 关闭弹窗

> 注：当前 API 只有逐个 addMember/removeMember，没有批量 updateMembers。
> 保存时通过 diff 计算增量操作，避免全量替换。

### 4. 渠道权限弹窗改名

- 标题文案："渠道管理" → "渠道权限"
- 操作按钮文案："渠道管理" → "渠道权限"
- 图标：PartitionOutlined → SafetyOutlined
- 功能逻辑不变（Checkbox 勾选渠道 + 保存）

### 5. 成员数可点击

- "成员数"列改为可点击链接样式
- 点击后打开成员管理弹窗
- 提供更直觉的入口

### 6. 团队列表搜索筛选

- 列表顶部添加搜索筛选栏（复用 SearchFilterBar 组件）
- 支持按团队名称搜索
- 支持按状态筛选（活跃/停用）

## 涉及文件

| 文件 | 变更 |
|------|------|
| `gateway-console/src/pages/Teams/index.tsx` | 操作按钮精简、图标替换、视觉分层、移除角色列、成员数可点击、添加搜索筛选、移除模型可见性/密钥管理相关 state 和 Modal |
| `gateway-console/src/pages/Teams/MemberManageModal.tsx` | 重写为已选成员 + 搜索添加模式 |
| `gateway-console/src/pages/Teams/ChannelManageModal.tsx` | 标题改名"渠道权限" |
| `gateway-console/src/locales/zh-CN/teams.json` | 更新/新增文案 |
| `gateway-console/src/locales/en-US/teams.json` | 更新/新增文案 |
| `gateway-console/src/types/team.ts` | 简化 TeamMember 类型（role 字段保留但前端不再使用区分逻辑） |
| `gateway-console/src/services/api/team.ts` | 无需新增 API，复用现有 addMember/removeMember |

## 不涉及

- 后端 API 变更（复用现有 addMember/removeMember，通过前端 diff 计算增量操作）
- 模型可见性、密钥管理功能删除（仅从团队列表入口移除，功能本身保留在其他入口）
- TeamMember 类型的 role 字段不删除（后端仍使用），前端只是不再展示和操作角色
