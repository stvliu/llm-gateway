import { useState } from 'react';
import { Table, Button, Tag, Space, App } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, TeamOutlined, KeyOutlined, EyeOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useTeams, useDeleteTeam } from '@/services/query/useTeams';
import TeamFormModal from './TeamFormModal';
import MemberManageModal from './MemberManageModal';
import UserApiKeyManageModal from './UserApiKeyManageModal';
import ModelVisibilityModal from './ModelVisibilityModal';
import type { Team, TeamMember } from '@/types/team';

const ROLE_COLOR: Record<string, string> = {
  owner: 'gold',
  admin: 'blue',
  member: 'default',
};

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
  const [apiKeyTeam, setApiKeyTeam] = useState<Team | undefined>();
  const [visibilityTeam, setVisibilityTeam] = useState<Team | null>(null);

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
      render: (_: unknown, record: Team) => record.members?.length ?? 0,
    },
    {
      title: t('team.state'),
      dataIndex: 'state',
      key: 'state',
      render: (state: string) => (
        <Tag color={state === 'active' ? 'green' : 'default'}>
          {state === 'active' ? t('team.stateActive') : t('team.stateInactive')}
        </Tag>
      ),
    },
    {
      title: t('team.role'),
      key: 'roles',
      render: (_: unknown, record: Team) => {
        const roles = [...new Set(record.members?.map((m: TeamMember) => m.role) || [])];
        return roles.map((role: string) => (
          <Tag key={role} color={ROLE_COLOR[role]}>
            {t(`team.role${role.charAt(0).toUpperCase() + role.slice(1)}`)}
          </Tag>
        ));
      },
    },
    {
      title: t('team.actions', { defaultValue: '操作' }),
      key: 'actions',
      render: (_: unknown, record: Team) => (
        <Space>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => setVisibilityTeam(record)}>
            {t('modelVisibility.title', { defaultValue: '模型可见性' })}
          </Button>
          <Button type="link" size="small" icon={<KeyOutlined />} onClick={() => setApiKeyTeam(record)}>
            {t('apiKey.manageTitle', { defaultValue: '密钥管理' })}
          </Button>
          <Button type="link" size="small" icon={<TeamOutlined />} onClick={() => setMemberTeam(record)}>
            {t('team.manageMembers')}
          </Button>
          {canWrite && (
            <>
              <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
              <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)} />
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <div style={{ marginBottom: 16 }}>
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('team.addTeam')}
          </Button>
        )}
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={teams}
        loading={isLoading}
        pagination={false}
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

      {apiKeyTeam && (
        <UserApiKeyManageModal
          team={apiKeyTeam}
          open={true}
          onClose={() => setApiKeyTeam(undefined)}
        />
      )}

      <ModelVisibilityModal
        open={visibilityTeam !== null}
        team={visibilityTeam}
        onClose={() => setVisibilityTeam(null)}
      />
    </>
  );
}