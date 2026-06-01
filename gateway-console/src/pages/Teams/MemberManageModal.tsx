import { useState, useEffect, useMemo, useCallback } from 'react';
import { Modal, Tag, Input, List, Avatar, Spin, Empty, Space, Typography, App } from 'antd';
import { UserOutlined, SearchOutlined } from '@ant-design/icons';
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
      .slice(0, 10);
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
        message.warning(t('memberManage.partialSuccess', { defaultValue: `部分操作失败（${failedCount} 个）` }));
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