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
