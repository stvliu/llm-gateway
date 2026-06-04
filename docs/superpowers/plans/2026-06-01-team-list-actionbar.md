# 团队页面列表操作栏改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 简化团队页面操作栏，统一成员管理交互，添加搜索筛选能力

**Architecture:** 前端改造，复用现有 API，通过 diff 计算增量操作成员

**Tech Stack:** React 18, Ant Design 5, TypeScript, TanStack Query

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `gateway-console/src/pages/Teams/index.tsx` | 修改 | 主页面：操作栏精简、搜索筛选、成员数可点击 |
| `gateway-console/src/pages/Teams/MemberManageModal.tsx` | 重写 | 成员管理弹窗：已选成员 + 搜索添加模式 |
| `gateway-console/src/pages/Teams/ChannelManageModal.tsx` | 修改 | 标题改名"渠道权限" |
| `gateway-console/src/locales/zh-CN/teams.json` | 修改 | 中文文案 |
| `gateway-console/src/locales/en-US/teams.json` | 修改 | 英文文案 |

---

### Task 1: 更新国际化文案

**Files:**
- Modify: `gateway-console/src/locales/zh-CN/teams.json`
- Modify: `gateway-console/src/locales/en-US/teams.json`

- [ ] **Step 1: 更新中文文案**

```json
{
  "team": {
    "name": "团队名称",
    "description": "描述",
    "memberCount": "成员数",
    "state": "状态",
    "stateActive": "活跃",
    "stateInactive": "停用",
    "actions": "操作",
    "addTeam": "新建团队",
    "editTeam": "编辑团队",
    "deleteTeam": "删除团队",
    "deleteConfirm": "确定删除团队「{{name}}」？此操作不可撤销。",
    "manageMembers": "成员",
    "searchPlaceholder": "搜索团队名称",
    "allState": "全部状态"
  },
  "memberManage": {
    "title": "成员管理",
    "selectedMembers": "已选成员",
    "searchUser": "搜索用户",
    "searchPlaceholder": "输入用户名搜索",
    "noResults": "未找到匹配用户",
    "addHint": "点击搜索结果添加成员",
    "removeHint": "点击 × 移除成员",
    "save": "保存",
    "cancel": "取消",
    "saveSuccess": "成员已更新",
    "saveError": "保存失败"
  },
  "channelPermission": {
    "title": "渠道权限",
    "permissionHint": "配置该团队可访问的渠道，团队成员的 API Key 将继承这些渠道权限",
    "selectedCount": "已选择 {{selectedCount}}/{{totalCount}} 个渠道",
    "channelName": "渠道名称",
    "billingMode": "计费模式",
    "payAsYouGo": "按量付费",
    "subscription": "订阅",
    "package": "套餐",
    "state": "状态",
    "active": "启用",
    "inactive": "禁用",
    "accessible": "可访问",
    "save": "保存",
    "cancel": "取消",
    "saveSuccess": "渠道配置已保存",
    "saveError": "保存失败",
    "loadError": "加载渠道数据失败"
  },
  "apiKey": {
    "manageTitle": "密钥管理",
    "create": "创建密钥",
    "createTitle": "创建用户密钥",
    "edit": "编辑",
    "editTitle": "编辑密钥",
    "delete": "删除",
    "deleteConfirm": "确定删除此密钥？",
    "deleteSuccess": "密钥已删除",
    "name": "名称",
    "nameRequired": "请输入密钥名称",
    "namePlaceholder": "例如：开发环境密钥",
    "prefix": "前缀",
    "models": "可用模型",
    "modelsPlaceholder": "输入模型名称后按回车",
    "modelsExtra": "留空表示允许访问所有模型",
    "allModels": "全部",
    "quotaLimit": "额度限制",
    "quotaExtra": "留空表示无限制",
    "unlimited": "无限制",
    "state": "状态",
    "actions": "操作",
    "selectProduct": "选择产品",
    "selectProductPlaceholder": "请选择关联的产品",
    "productRequired": "请选择产品",
    "selectProvider": "选择供应商",
    "selectProviderPlaceholder": "请先选择供应商",
    "providerRequired": "请选择供应商",
    "copy": "复制密钥",
    "createdTitle": "密钥创建成功",
    "createdHint": "请立即复制并保存此密钥，关闭后将无法再次查看！",
    "createdOk": "我已保存",
    "copied": "已复制到剪贴板"
  }
}
```

- [ ] **Step 2: 更新英文文案**

```json
{
  "team": {
    "name": "Team Name",
    "description": "Description",
    "memberCount": "Members",
    "state": "State",
    "stateActive": "Active",
    "stateInactive": "Inactive",
    "actions": "Actions",
    "addTeam": "New Team",
    "editTeam": "Edit Team",
    "deleteTeam": "Delete Team",
    "deleteConfirm": "Are you sure you want to delete team \"{{name}}\"? This cannot be undone.",
    "manageMembers": "Members",
    "searchPlaceholder": "Search team name",
    "allState": "All States"
  },
  "memberManage": {
    "title": "Member Management",
    "selectedMembers": "Selected Members",
    "searchUser": "Search User",
    "searchPlaceholder": "Enter username to search",
    "noResults": "No matching users found",
    "addHint": "Click search result to add member",
    "removeHint": "Click × to remove member",
    "save": "Save",
    "cancel": "Cancel",
    "saveSuccess": "Members updated",
    "saveError": "Failed to save"
  },
  "channelPermission": {
    "title": "Channel Permissions",
    "permissionHint": "Configure channels accessible to this team. Team members' API Keys will inherit these channel permissions.",
    "selectedCount": "Selected {{selectedCount}}/{{totalCount}} channels",
    "channelName": "Channel Name",
    "billingMode": "Billing Mode",
    "payAsYouGo": "Pay As You Go",
    "subscription": "Subscription",
    "package": "Package",
    "state": "State",
    "active": "Active",
    "inactive": "Inactive",
    "accessible": "Accessible",
    "save": "Save",
    "cancel": "Cancel",
    "saveSuccess": "Channel configuration saved",
    "saveError": "Failed to save",
    "loadError": "Failed to load channel data"
  },
  "apiKey": {
    "manageTitle": "API Key Management",
    "create": "Create Key",
    "createTitle": "Create User API Key",
    "edit": "Edit",
    "editTitle": "Edit API Key",
    "delete": "Delete",
    "deleteConfirm": "Are you sure you want to delete this key?",
    "deleteSuccess": "Key deleted",
    "name": "Name",
    "nameRequired": "Please enter a key name",
    "namePlaceholder": "e.g., Development Key",
    "prefix": "Prefix",
    "models": "Available Models",
    "modelsPlaceholder": "Enter model name and press Enter",
    "modelsExtra": "Leave empty to allow access to all models",
    "allModels": "All",
    "quotaLimit": "Quota Limit",
    "quotaExtra": "Leave empty for unlimited",
    "unlimited": "Unlimited",
    "state": "State",
    "actions": "Actions",
    "selectProduct": "Select Product",
    "selectProductPlaceholder": "Select a product to associate",
    "productRequired": "Please select a product",
    "selectProvider": "Select Provider",
    "selectProviderPlaceholder": "Select a provider first",
    "providerRequired": "Please select a provider",
    "copy": "Copy Key",
    "createdTitle": "API Key Created Successfully",
    "createdHint": "Copy and save this key now. It will not be shown again!",
    "createdOk": "I've saved it",
    "copied": "Copied to clipboard"
  }
}
```

- [ ] **Step 3: 提交文案更新**

```bash
git add gateway-console/src/locales/zh-CN/teams.json gateway-console/src/locales/en-US/teams.json
git commit -m "feat(i18n): 更新团队页面国际化文案"
```

---

### Task 2: 重写成员管理弹窗

**Files:**
- Rewrite: `gateway-console/src/pages/Teams/MemberManageModal.tsx`

- [ ] **Step 1: 重写成员管理弹窗组件**

```tsx
import { useState, useEffect, useMemo, useCallback } from 'react';
import { Modal, Tag, Input, List, Avatar, Spin, Empty, Space, Typography, App } from 'antd';
import { UserOutlined, SearchOutlined, CloseOutlined } from '@ant-design/icons';
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

/**
 * 团队成员管理弹窗
 * 采用"已选成员 + 搜索添加"模式
 */
export default function MemberManageModal({ visible, team, onClose }: MemberManageModalProps) {
  const { t } = useTranslation('teams');
  const { message } = App.useApp();

  // 已选成员 ID 集合
  const [selectedUserIds, setSelectedUserIds] = useState<Set<number>>(new Set());
  // 搜索关键词
  const [searchKeyword, setSearchKeyword] = useState('');
  // 保存中状态
  const [saving, setSaving] = useState(false);

  // 获取用户列表（用于搜索）
  const { data: usersData, isLoading: usersLoading } = useUsers({ size: 100 });
  const addMemberMutation = useAddTeamMember();
  const removeMemberMutation = useRemoveTeamMember();

  // 初始化已选成员（从 team.members 提取）
  useEffect(() => {
    if (visible && team?.members) {
      setSelectedUserIds(new Set(team.members.map((m: TeamMember) => m.userId)));
      setSearchKeyword('');
    }
  }, [visible, team]);

  // 用户 ID -> 用户名 映射
  const userMap = useMemo(() => {
    const map = new Map<number, User>();
    usersData?.items?.forEach((u: User) => map.set(u.id, u));
    return map;
  }, [usersData]);

  // 已选成员列表（带用户名）
  const selectedMembers = useMemo(() => {
    return Array.from(selectedUserIds).map((id) => ({
      id,
      name: userMap.get(id)?.username ?? `用户 ${id}`,
    }));
  }, [selectedUserIds, userMap]);

  // 搜索结果（排除已选成员）
  const searchResults = useMemo(() => {
    if (!searchKeyword.trim() || !usersData?.items) return [];
    const keyword = searchKeyword.toLowerCase();
    return usersData.items
      .filter((u: User) => !selectedUserIds.has(u.id))
      .filter((u: User) => u.username.toLowerCase().includes(keyword))
      .slice(0, 10); // 限制结果数量
  }, [searchKeyword, usersData, selectedUserIds]);

  // 添加成员
  const handleAdd = useCallback((userId: number) => {
    setSelectedUserIds((prev) => new Set(prev).add(userId));
  }, []);

  // 移除成员
  const handleRemove = useCallback((userId: number) => {
    setSelectedUserIds((prev) => {
      const next = new Set(prev);
      next.delete(userId);
      return next;
    });
  }, []);

  // 保存
  const handleSave = async () => {
    if (!team) return;

    setSaving(true);
    try {
      const originalIds = new Set(team.members?.map((m: TeamMember) => m.userId) ?? []);
      const toAdd = Array.from(selectedUserIds).filter((id) => !originalIds.has(id));
      const toRemove = Array.from(originalIds).filter((id) => !selectedUserIds.has(id));

      // 并行执行添加和移除
      await Promise.all([
        ...toAdd.map((userId) =>
          addMemberMutation.mutateAsync({ teamId: team.id, data: { userId, role: 'member' } })
        ),
        ...toRemove.map((userId) =>
          removeMemberMutation.mutateAsync({ teamId: team.id, userId })
        ),
      ]);

      message.success(t('memberManage.saveSuccess'));
      onClose();
    } catch {
      message.error(t('memberManage.saveError'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title={`${team?.name ?? ''} - ${t('memberManage.title')}`}
      open={visible}
      onCancel={onClose}
      onOk={handleSave}
      okText={t('memberManage.save')}
      cancelText={t('memberManage.cancel')}
      confirmLoading={saving}
      width={500}
      destroyOnHidden
    >
      {/* 已选成员 */}
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary">{t('memberManage.selectedMembers')} ({selectedMembers.length})</Text>
        <div style={{ marginTop: 8, minHeight: 32 }}>
          {selectedMembers.length === 0 ? (
            <Text type="secondary">暂无成员</Text>
          ) : (
            <Space wrap size={[4, 4]}>
              {selectedMembers.map((m) => (
                <Tag
                  key={m.id}
                  closable
                  onClose={() => handleRemove(m.id)}
                  style={{ padding: '4px 8px' }}
                >
                  <UserOutlined style={{ marginRight: 4 }} />
                  {m.name}
                </Tag>
              ))}
            </Space>
          )}
        </div>
      </div>

      {/* 搜索添加 */}
      <div>
        <Text type="secondary">{t('memberManage.searchUser')}</Text>
        <Input
          placeholder={t('memberManage.searchPlaceholder')}
          prefix={<SearchOutlined />}
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          allowClear
          style={{ marginTop: 8, marginBottom: 8 }}
        />

        {usersLoading ? (
          <div style={{ textAlign: 'center', padding: 20 }}>
            <Spin />
          </div>
        ) : searchKeyword.trim() ? (
          searchResults.length === 0 ? (
            <Empty description={t('memberManage.noResults')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
          ) : (
            <List
              dataSource={searchResults}
              renderItem={(user: User) => (
                <List.Item
                  style={{ padding: '8px 0', cursor: 'pointer' }}
                  onClick={() => handleAdd(user.id)}
                >
                  <List.Item.Meta
                    avatar={<Avatar icon={<UserOutlined />} />}
                    title={user.username}
                    description={user.email}
                  />
                </List.Item>
              )}
            />
          )
        ) : (
          <Text type="secondary" style={{ fontSize: 12 }}>
            {t('memberManage.addHint')}
          </Text>
        )}
      </div>
    </Modal>
  );
}
```

- [ ] **Step 2: 提交成员管理弹窗重写**

```bash
git add gateway-console/src/pages/Teams/MemberManageModal.tsx
git commit -m "feat(teams): 重写成员管理弹窗为已选成员+搜索添加模式"
```

---

### Task 3: 修改渠道权限弹窗标题

**Files:**
- Modify: `gateway-console/src/pages/Teams/ChannelManageModal.tsx`

- [ ] **Step 1: 更新标题和文案**

修改 `ChannelManageModal.tsx` 中的标题和文案，将"渠道管理"改为"渠道权限"：

```tsx
// 第 175 行，修改 Modal title
title={`${teamName} - ${t('channelPermission.title', { defaultValue: '渠道权限' })}`}

// 第 188-189 行，修改 Alert message
message={t('channelPermission.permissionHint', { defaultValue: '配置该团队可访问的渠道，团队成员的 API Key 将继承这些渠道权限' })}

// 第 193-198 行，修改已选计数文案
{t('channelPermission.selectedCount', {
  defaultValue: `已选择 ${selectedCount}/${totalCount} 个渠道`,
  selectedCount,
  totalCount,
})}

// 第 182-183 行，修改按钮文案
okText={t('channelPermission.save', { defaultValue: '保存' })}
cancelText={t('channelPermission.cancel', { defaultValue: '取消' })}

// 第 55 行，修改错误提示
message.error(t('channelPermission.loadError', { defaultValue: '加载渠道数据失败' }));

// 第 97 行，修改成功提示
message.success(t('channelPermission.saveSuccess', { defaultValue: '渠道配置已保存' }));

// 第 100 行，修改失败提示
message.error(t('channelPermission.saveError', { defaultValue: '保存失败' }));

// 第 108-117 行，修改列标题
title: t('channelPermission.channelName', { defaultValue: '渠道名称' }),

title: t('channelPermission.billingMode', { defaultValue: '计费模式' }),
// modeMap 中的 label
pay_as_you_go: { label: t('channelPermission.payAsYouGo', { defaultValue: '按量付费' }), color: 'green' },
subscription: { label: t('channelPermission.subscription', { defaultValue: '订阅' }), color: 'blue' },
package: { label: t('channelPermission.package', { defaultValue: '套餐' }), color: 'orange' },

title: t('channelPermission.state', { defaultValue: '状态' }),
// state 渲染
{state === 'ACTIVE' ? t('channelPermission.active', { defaultValue: '启用' }) : t('channelPermission.inactive', { defaultValue: '禁用' })}

// 第 156 行，修改 Checkbox 列标题
{t('channelPermission.accessible', { defaultValue: '可访问' })}
```

- [ ] **Step 2: 提交渠道权限弹窗修改**

```bash
git add gateway-console/src/pages/Teams/ChannelManageModal.tsx
git commit -m "feat(teams): 渠道管理弹窗改名为渠道权限"
```

---

### Task 4: 改造团队列表主页面

**Files:**
- Modify: `gateway-console/src/pages/Teams/index.tsx`

- [ ] **Step 1: 重写团队列表主页面**

```tsx
import { useState, useMemo } from 'react';
import { Table, Button, Tag, Space, App, Input, Select, Popconfirm, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, TeamOutlined, SafetyOutlined, SearchOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useTeams, useDeleteTeam } from '@/services/query/useTeams';
import TeamFormModal from './TeamFormModal';
import MemberManageModal from './MemberManageModal';
import ChannelManageModal from './ChannelManageModal';
import type { Team } from '@/types/team';

const { Link } = Typography;

export default function TeamsPage() {
  const { t } = useTranslation('teams');
  const { modal } = App.useApp();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.USER_WRITE);

  const { data: teams, isLoading } = useTeams();
  const deleteMutation = useDeleteTeam();

  const [formVisible, setFormVisible] = useState(false);
  const [editingTeam, setEditingTeam] = useState<Team | undefined>();
  const [memberTeam, setMemberTeam] = useState<Team | undefined>();
  const [channelManageTeam, setChannelManageTeam] = useState<Team | null>(null);

  // 搜索筛选状态
  const [searchKeyword, setSearchKeyword] = useState('');
  const [stateFilter, setStateFilter] = useState<string | undefined>(undefined);

  const handleAdd = () => {
    setEditingTeam(undefined);
    setFormVisible(true);
  };

  const handleEdit = (team: Team) => {
    setEditingTeam(team);
    setFormVisible(true);
  };

  const handleDelete = (team: Team) => {
    modal.confirm({
      title: t('team.deleteTeam'),
      content: t('team.deleteConfirm', { name: team.name }),
      okType: 'danger',
      onOk: () => deleteMutation.mutateAsync(team.id),
    });
  };

  // 筛选后的团队列表
  const filteredTeams = useMemo(() => {
    if (!teams) return [];
    return teams.filter((team: Team) => {
      // 名称搜索
      if (searchKeyword && !team.name.toLowerCase().includes(searchKeyword.toLowerCase())) {
        return false;
      }
      // 状态筛选
      if (stateFilter && team.state !== stateFilter) {
        return false;
      }
      return true;
    });
  }, [teams, searchKeyword, stateFilter]);

  const columns = [
    {
      title: t('team.name'),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('team.description'),
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: t('team.memberCount'),
      key: 'memberCount',
      width: 100,
      render: (_: unknown, record: Team) => (
        <Link onClick={() => setMemberTeam(record)}>
          {record.members?.length ?? 0}
        </Link>
      ),
    },
    {
      title: t('team.state'),
      dataIndex: 'state',
      key: 'state',
      width: 100,
      render: (state: string) => (
        <Tag color={state === 'active' ? 'green' : 'default'}>
          {state === 'active' ? t('team.stateActive') : t('team.stateInactive')}
        </Tag>
      ),
    },
    {
      title: t('team.actions'),
      key: 'actions',
      width: 200,
      fixed: 'right' as const,
      render: (_: unknown, record: Team) => (
        <Space size="small">
          <Button type="link" size="small" icon={<TeamOutlined />} onClick={() => setMemberTeam(record)}>
            {t('team.manageMembers')}
          </Button>
          <Button type="link" size="small" icon={<SafetyOutlined />} onClick={() => setChannelManageTeam(record)}>
            {t('channelPermission.title', { defaultValue: '渠道权限' })}
          </Button>
          {canWrite && (
            <>
              <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
              <Popconfirm
                title={t('team.deleteTeam')}
                description={t('team.deleteConfirm', { name: record.name })}
                okType="danger"
                onConfirm={() => deleteMutation.mutateAsync(record.id)}
              >
                <Button type="text" size="small" danger icon={<DeleteOutlined />} />
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <div style={{ marginBottom: 16, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('team.addTeam')}
          </Button>
        )}
        <Input
          placeholder={t('team.searchPlaceholder', { defaultValue: '搜索团队名称' })}
          prefix={<SearchOutlined />}
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          allowClear
          style={{ width: 250 }}
        />
        <Select
          placeholder={t('team.state', { defaultValue: '状态' })}
          value={stateFilter}
          onChange={setStateFilter}
          allowClear
          style={{ width: 120 }}
          options={[
            { value: 'active', label: t('team.stateActive') },
            { value: 'inactive', label: t('team.stateInactive') },
          ]}
        />
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={filteredTeams}
        loading={isLoading}
        pagination={false}
        scroll={{ x: 800 }}
      />

      <TeamFormModal
        visible={formVisible}
        team={editingTeam}
        onClose={() => setFormVisible(false)}
      />

      <MemberManageModal
        visible={!!memberTeam}
        team={memberTeam}
        onClose={() => setMemberTeam(undefined)}
      />

      {channelManageTeam && (
        <ChannelManageModal
          open={true}
          teamId={channelManageTeam.id}
          teamName={channelManageTeam.name}
          onCancel={() => setChannelManageTeam(null)}
        />
      )}
    </>
  );
}
```

- [ ] **Step 2: 提交主页面改造**

```bash
git add gateway-console/src/pages/Teams/index.tsx
git commit -m "feat(teams): 简化操作栏、添加搜索筛选、成员数可点击"
```

---

### Task 5: 验证功能

- [ ] **Step 1: 启动前端开发服务器**

```bash
cd gateway-console && npm run dev
```

- [ ] **Step 2: 手动验证功能**

验证清单：
1. 团队列表页面加载正常
2. 搜索框按名称筛选正常
3. 状态下拉筛选正常
4. 成员数点击打开成员管理弹窗
5. 成员管理弹窗：已选成员 Tag 显示正常
6. 成员管理弹窗：搜索用户并添加正常
7. 成员管理弹窗：移除成员正常
8. 成员管理弹窗：保存后数据更新正常
9. 渠道权限弹窗标题显示"渠道权限"
10. 操作栏：成员、渠道权限按钮带文字
11. 操作栏：编辑、删除按钮纯图标
12. 删除按钮 Popconfirm 确认正常

---

### Task 6: 最终提交

- [ ] **Step 1: 确认所有更改已提交**

```bash
git status
git log --oneline -5
```

- [ ] **Step 2: 推送到远程分支**

```bash
git push origin feat/provider-frontend-ux-redesign
```
