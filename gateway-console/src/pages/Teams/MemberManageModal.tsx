import { useState, useEffect, useMemo, useCallback } from 'react';
import { Modal, Input, List, Avatar, Spin, Empty, Typography, App, Button } from 'antd';
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