# 成员管理弹窗左右分栏改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将成员管理弹窗从上下堆叠布局改为左右分栏布局（左侧搜索添加，右侧已选成员）

**Architecture:** 左侧搜索面板实时过滤用户列表，点击行添加到右侧；右侧已选成员列表带删除图标；保存时 diff 计算增量操作

**Tech Stack:** React 18, Ant Design 5, TypeScript, TanStack Query

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `gateway-console/src/pages/Teams/MemberManageModal.tsx` | 重写 | 左右分栏布局 |
| `gateway-console/src/locales/zh-CN/teams.json` | 修改 | 新增文案 |
| `gateway-console/src/locales/en-US/teams.json` | 修改 | 新增文案 |

---

### Task 1: 更新国际化文案

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/teams.json`
- Modify: `gateway-console/src/locales/en-US/teams.json`

- [ ] **Step 1: 更新中文文案**

在 `memberManage` 对象中，新增 `addMemberTitle` 和 `emptyMembers` 键，更新 `noResults` 文案：

```json
"memberManage": {
    "title": "成员管理",
    "addMemberTitle": "添加成员",
    "selectedMembers": "已选成员",
    "searchUser": "搜索用户",
    "searchPlaceholder": "输入用户名搜索",
    "noResults": "未找到匹配用户",
    "addHint": "点击搜索结果添加成员",
    "emptyMembers": "暂无成员",
    "removeHint": "点击 × 移除成员",
    "save": "保存",
    "cancel": "取消",
    "saveSuccess": "成员已更新",
    "partialSuccess": "部分操作失败（{{count}} 个）",
    "saveError": "保存失败"
  }
```

- [ ] **Step 2: 更新英文文案**

```json
"memberManage": {
    "title": "Member Management",
    "addMemberTitle": "Add Members",
    "selectedMembers": "Selected Members",
    "searchUser": "Search User",
    "searchPlaceholder": "Enter username to search",
    "noResults": "No matching users found",
    "addHint": "Click search result to add member",
    "emptyMembers": "No members yet",
    "removeHint": "Click × to remove member",
    "save": "Save",
    "cancel": "Cancel",
    "saveSuccess": "Members updated",
    "partialSuccess": "Some operations failed ({{count}})",
    "saveError": "Failed to save"
  }
```

- [ ] **Step 3: 提交文案更新**

```bash
git add gateway-console/src/locales/zh-CN/teams.json gateway-console/src/locales/en-US/teams.json
git commit -m "feat(i18n): 新增成员管理弹窗分栏布局文案"
```

---

### Task 2: 重写成员管理弹窗为左右分栏布局

**Files:**
- Rewrite: `gateway-console/src/pages/Teams/MemberManageModal.tsx`

- [ ] **Step 1: 重写组件**

```tsx
import { useState, useEffect, useMemo, useCallback } from 'react';
import { Modal, Tag, Input, List, Avatar, Spin, Empty, Space, Typography, App, Button } from 'antd';
import { UserOutlined, SearchOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useUsers } from '@/services/query/useUsers';
import { useAddTeamMember, useRemoveTeamMember } from '@/services/query/useTeams';
import type { Team, TeamMember } from '@/types/team';
import type { User } from '@/types/user';

const { Text } = Typography;

interface MemberManageModalProps {
  visible: boolean;
  team?: Team;
  onClose: () => void;
}

const LIST_HEIGHT = 400;

/**
 * 团队成员管理弹窗
 * 左侧搜索添加，右侧已选成员列表
 */
export default function MemberManageModal({ visible, team, onClose }: MemberManageModalProps) {
  const { t } = useTranslation('teams');
  const { message } = App.useApp();

  const [selectedUserIds, setSelectedUserIds] = useState<Set<number>>(new Set());
  const [searchKeyword, setSearchKeyword] = useState('');
  const [saving, setSaving] = useState(false);

  const { data: usersData, isLoading: usersLoading } = useUsers({ size: 100 });
  const addMemberMutation = useAddTeamMember();
  const removeMemberMutation = useRemoveTeamMember();

  // 初始化已选成员
  useEffect(() => {
    if (visible && team?.members) {
      setSelectedUserIds(new Set(team.members.map((m: TeamMember) => m.userId)));
      setSearchKeyword('');
    }
  }, [visible, team]);

  // 用户 ID -> User 映射
  const userMap = useMemo(() => {
    const map = new Map<number, User>();
    usersData?.items?.forEach((u: User) => map.set(u.id, u));
    return map;
  }, [usersData]);

  // 已选成员列表（带完整用户信息）
  const selectedMembers = useMemo(() => {
    return Array.from(selectedUserIds)
      .map((id) => {
        const user = userMap.get(id);
        return user ? { id: user.id, username: user.username, email: user.email ?? '' } : null;
      })
      .filter(Boolean) as { id: number; username: string; email: string }[];
  }, [selectedUserIds, userMap]);

  // 左侧可用用户列表（排除已选，支持搜索，限制 20 条）
  const availableUsers = useMemo(() => {
    if (!usersData?.items) return [];
    const list = usersData.items.filter((u: User) => !selectedUserIds.has(u.id));
    if (searchKeyword.trim()) {
      const keyword = searchKeyword.toLowerCase();
      return list
        .filter((u: User) => u.username.toLowerCase().includes(keyword))
        .slice(0, 20);
    }
    return list.slice(0, 20);
  }, [usersData, selectedUserIds, searchKeyword]);

  const handleAdd = useCallback((userId: number) => {
    setSelectedUserIds((prev) => new Set(prev).add(userId));
  }, []);

  const handleRemove = useCallback((userId: number) => {
    setSelectedUserIds((prev) => {
      const next = new Set(prev);
      next.delete(userId);
      return next;
    });
  }, []);

  const handleSave = async () => {
    if (!team) return;

    setSaving(true);
    try {
      const originalIds = new Set(team.members?.map((m: TeamMember) => m.userId) ?? []);
      const toAdd = Array.from(selectedUserIds).filter((id) => !originalIds.has(id));
      const toRemove = Array.from(originalIds).filter((id) => !selectedUserIds.has(id));

      // 先移除再添加（顺序执行，避免并发冲突）
      const removeResults = await Promise.allSettled(
        toRemove.map((userId) =>
          removeMemberMutation.mutateAsync({ teamId: team.id, userId })
        ),
      );
      const addResults = await Promise.allSettled(
        toAdd.map((userId) =>
          addMemberMutation.mutateAsync({ teamId: team.id, data: { userId, role: 'member' } })
        ),
      );

      const failedCount =
        removeResults.filter((r) => r.status === 'rejected').length +
        addResults.filter((r) => r.status === 'rejected').length;

      if (failedCount > 0) {
        message.warning(t('memberManage.partialSuccess', { count: failedCount }));
      } else {
        message.success(t('memberManage.saveSuccess'));
      }
      onClose();
    } catch {
      message.error(t('memberManage.saveError'));
    } finally {
      setSaving(false);
    }
  };

  // 渲染用户行（左右两栏共用）
  const renderUserItem = (user: { id: number; username: string; email: string }, action: React.ReactNode) => (
    <List.Item style={{ padding: '8px 12px' }} extra={action}>
      <List.Item.Meta
        avatar={<Avatar size="small" icon={<UserOutlined />} />}
        title={user.username}
        description={<Text type="secondary" style={{ fontSize: 12 }}>{user.email}</Text>}
      />
    </List.Item>
  );

  return (
    <Modal
      title={`${team?.name ?? ''} - ${t('memberManage.title')}`}
      open={visible}
      onCancel={onClose}
      onOk={handleSave}
      okText={t('memberManage.save')}
      cancelText={t('memberManage.cancel')}
      confirmLoading={saving}
      width={700}
      destroyOnHidden
    >
      <div style={{ display: 'flex', gap: 16 }}>
        {/* 左侧：搜索添加 */}
        <div style={{ flex: 1, borderRight: '1px solid #f0f0f0', paddingRight: 16 }}>
          <Text strong style={{ marginBottom: 8, display: 'block' }}>
            {t('memberManage.addMemberTitle')}
          </Text>
          <Input
            placeholder={t('memberManage.searchPlaceholder')}
            prefix={<SearchOutlined />}
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            allowClear
            size="small"
            style={{ marginBottom: 8 }}
          />
          <div style={{ height: LIST_HEIGHT, overflowY: 'auto' }}>
            {usersLoading ? (
              <div style={{ textAlign: 'center', padding: 40 }}>
                <Spin />
              </div>
            ) : availableUsers.length === 0 ? (
              <Empty
                description={t('memberManage.noResults')}
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                style={{ marginTop: 60 }}
              />
            ) : (
              <List
                dataSource={availableUsers}
                renderItem={(user: User) => renderUserItem(
                  { id: user.id, username: user.username, email: user.email ?? '' },
                  <Button
                    type="text"
                    size="small"
                    icon={<UserOutlined />}
                    onClick={() => handleAdd(user.id)}
                    title={t('memberManage.addHint')}
                  />
                )}
              />
            )}
          </div>
        </div>

        {/* 右侧：已选成员 */}
        <div style={{ flex: 1, paddingLeft: 16 }}>
          <Text strong style={{ marginBottom: 8, display: 'block' }}>
            {t('memberManage.selectedMembers')} ({selectedMembers.length})
          </Text>
          <div style={{ height: LIST_HEIGHT, overflowY: 'auto' }}>
            {selectedMembers.length === 0 ? (
              <div style={{ textAlign: 'center', marginTop: 80 }}>
                <UserOutlined style={{ fontSize: 32, color: '#d9d9d9', marginBottom: 8 }} />
                <br />
                <Text type="secondary">{t('memberManage.emptyMembers')}</Text>
              </div>
            ) : (
              <List
                dataSource={selectedMembers}
                renderItem={(member) => renderUserItem(
                  member,
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => handleRemove(member.id)}
                    title={t('memberManage.removeHint')}
                  />
                )}
              />
            )}
          </div>
        </div>
      </div>
    </Modal>
  );
}
```

- [ ] **Step 2: 验证编译**

```bash
cd gateway-console && npm run build 2>&1 | grep -E "MemberManage|error TS" | head -10
```

Expected: 无 MemberManage 相关错误

- [ ] **Step 3: 提交**

```bash
git add gateway-console/src/pages/Teams/MemberManageModal.tsx
git commit -m "feat(teams): 成员管理弹窗改为左右分栏布局"
```

---

### Task 3: 验证功能

- [ ] **Step 1: 启动前端开发服务器**

```bash
cd gateway-console && npm run dev
```

- [ ] **Step 2: 手动验证**

验证清单：
1. 弹窗打开后左右分栏显示正常
2. 左侧显示最近用户列表（排除已选成员）
3. 左侧搜索框实时过滤正常
4. 点击左侧用户行添加到右侧
5. 点击右侧删除图标移除成员，该用户回到左侧
6. 右侧空状态显示"暂无成员"
7. 保存正常（无变更时不发请求）
8. 保存添加/移除正常
9. 部分失败时 warning 提示

- [ ] **Step 3: 提交验证通过**

```bash
git log --oneline -3
```
