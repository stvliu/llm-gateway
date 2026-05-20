import { useState } from 'react';
import { Modal, Table, Button, Select, Space, Tag, App } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAddTeamMember, useRemoveTeamMember, useUpdateMemberRole } from '@/services/query/useTeams';
import { useUsers } from '@/services/query/useUsers';
import type { Team, TeamRole } from '@/types/team';

const ROLE_COLOR: Record<string, string> = {
  owner: 'gold',
  admin: 'blue',
  member: 'default',
};

interface MemberManageModalProps {
  visible: boolean;
  team?: Team;
  onClose: () => void;
}

export default function MemberManageModal({ visible, team, onClose }: MemberManageModalProps) {
  const { t } = useTranslation('teams');
  const { message, modal } = App.useApp();
  const addMemberMutation = useAddTeamMember();
  const removeMemberMutation = useRemoveTeamMember();
  const updateRoleMutation = useUpdateMemberRole();
  const { data: usersData } = useUsers({ size: 100 });

  const [adding, setAdding] = useState(false);
  const [selectedUser, setSelectedUser] = useState<number | undefined>();
  const [selectedRole, setSelectedRole] = useState<TeamRole>('member');

  const handleAddMember = async () => {
    if (!team || !selectedUser) return;
    try {
      await addMemberMutation.mutateAsync({
        teamId: team.id,
        data: { userId: selectedUser, role: selectedRole },
      });
      message.success(t('team.addMember'));
      setAdding(false);
      setSelectedUser(undefined);
      setSelectedRole('member');
    } catch {
      message.error(t('team.addMember'));
    }
  };

  const handleRemove = (userId: number) => {
    if (!team) return;
    modal.confirm({
      title: t('team.removeMember'),
      content: t('team.removeMemberConfirm'),
      okType: 'danger',
      onOk: () => removeMemberMutation.mutateAsync({ teamId: team.id, userId }),
    });
  };

  const handleRoleChange = async (userId: number, role: TeamRole) => {
    if (!team) return;
    try {
      await updateRoleMutation.mutateAsync({ teamId: team.id, userId, data: { role } });
      message.success(t('team.editTeam'));
    } catch {
      message.error(t('team.editTeam'));
    }
  };

  const memberIds = new Set(team?.members?.map((m) => m.userId) ?? []);
  const availableUsers = usersData?.items?.filter((u) => !memberIds.has(u.id)) ?? [];

  const columns = [
    {
      title: 'ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 80,
    },
    {
      title: t('team.role'),
      dataIndex: 'role',
      key: 'role',
      width: 140,
      render: (role: TeamRole, record: { userId: number }) =>
        role === 'owner' ? (
          <Tag color={ROLE_COLOR[role]}>{t('team.roleOwner')}</Tag>
        ) : (
          <Select
            value={role}
            size="small"
            style={{ width: 100 }}
            onChange={(val: TeamRole) => handleRoleChange(record.userId, val)}
            options={[
              { value: 'admin', label: t('team.roleAdmin') },
              { value: 'member', label: t('team.roleMember') },
            ]}
          />
        ),
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      render: (_: unknown, record: { userId: number; role: string }) =>
        record.role !== 'owner' ? (
          <Button type="link" size="small" danger onClick={() => handleRemove(record.userId)}>
            {t('team.removeMember')}
          </Button>
        ) : null,
    },
  ];

  return (
    <Modal
      title={`${team?.name ?? ''} - ${t('team.manageMembers')}`}
      open={visible}
      onCancel={onClose}
      footer={null}
      width={600}
      destroyOnHidden
    >
      <div style={{ marginBottom: 16 }}>
        {adding ? (
          <Space>
            <Select
              style={{ width: 200 }}
              placeholder={t('team.selectUser')}
              value={selectedUser}
              onChange={setSelectedUser}
              options={availableUsers.map((u) => ({ value: u.id, label: u.username }))}
              showSearch
              optionFilterProp="label"
            />
            <Select
              style={{ width: 120 }}
              value={selectedRole}
              onChange={setSelectedRole}
              options={[
                { value: 'admin', label: t('team.roleAdmin') },
                { value: 'member', label: t('team.roleMember') },
              ]}
            />
            <Button type="primary" onClick={handleAddMember} disabled={!selectedUser}>
              确定
            </Button>
            <Button onClick={() => setAdding(false)}>取消</Button>
          </Space>
        ) : (
          <Button type="dashed" icon={<PlusOutlined />} onClick={() => setAdding(true)}>
            {t('team.addMember')}
          </Button>
        )}
      </div>

      <Table
        rowKey="userId"
        columns={columns}
        dataSource={team?.members ?? []}
        pagination={false}
        size="small"
      />
    </Modal>
  );
}