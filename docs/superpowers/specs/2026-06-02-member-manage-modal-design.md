# 成员管理弹窗交互设计

## 背景

当前成员管理弹窗采用"已选 Tag + 搜索"上下堆叠布局，存在以下问题：
- 信息层级混乱，操作路径不直观
- 已选成员只有 Tag，无法看到邮箱等详细信息
- 搜索结果排除已选成员后，用户无法确认某人是否已选
- 保存时并行调用 API 导致"用户已是成员"异常

## 设计方案：左右分栏布局

### 整体布局

弹窗宽度 700px，左右分栏，右侧已选成员列表，左侧搜索添加区域。

### 左侧：搜索添加

- 标题："添加成员"
- 搜索框（SearchOutlined 前缀，allowClear）
- 搜索结果列表：
  - 每行：Avatar + 用户名 + 邮箱，点击整行添加到右侧
  - 初始状态：显示最近 20 个用户（排除已选成员）
  - 搜索中：实时过滤，最多显示 20 条结果
  - 无结果：Empty 组件
  - 已添加到右侧的用户不显示
- 列表高度固定 400px，超出滚动

### 右侧：已选成员

- 标题："已选成员 (N)"
- 成员列表：
  - 每行：Avatar + 用户名 + 邮箱，右侧红色 DeleteOutlined 图标按钮
  - 空状态："暂无成员"（居中提示文字）
- 列表高度固定 400px，超出滚动

### 底部操作栏

- Modal footer："保存"（primary）+ "取消"按钮
- 保存时 confirmLoading 置为 true

### 交互流程

1. 打开弹窗 → 左侧显示最近用户（排除已选），右侧显示当前团队成员
2. 左侧搜索 → 实时过滤用户列表（按用户名匹配）
3. 点击左侧用户行 → 添加到右侧，该行从左侧消失
4. 点击右侧删除图标 → 从右侧移除，该用户回到左侧搜索结果中
5. 点击保存 → diff 计算原始成员与当前选中，先移除再添加，Promise.allSettled 容忍个别失败
6. 保存成功 → 关闭弹窗；部分失败 → warning 提示后关闭

### 数据流

1. 打开弹窗 → 从 team.members 提取已加入成员 ID 作为 selectedUserIds
2. 并行加载用户列表（useUsers）
3. 左侧列表 = users - selectedUserIds（搜索过滤后取前 20）
4. 右侧列表 = selectedUserIds 对应的用户信息
5. 保存 → diff 计算：
   - toRemove = 原始成员 IDs - selectedUserIds
   - toAdd = selectedUserIds - 原始成员 IDs
   - 先 removeMember（Promise.allSettled），再 addMember（Promise.allSettled）

### 涉及文件

| 文件 | 变更 |
|------|------|
| `gateway-console/src/pages/Teams/MemberManageModal.tsx` | 重写为左右分栏布局 |
| `gateway-console/src/locales/zh-CN/teams.json` | 新增 memberManage.addMemberTitle 文案 |
| `gateway-console/src/locales/en-US/teams.json` | 新增 memberManage.addMemberTitle 文案 |

### 不涉及

- 后端 API 变更
- 团队列表主页面（index.tsx）
- 渠道权限弹窗
