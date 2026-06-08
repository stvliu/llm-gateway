import { useState, useMemo } from 'react';
import { Table, Button, Tag, Space, Input, Select, Popconfirm, Typography, Tooltip, Card } from 'antd';
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
      width: 140,
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
      width: 60,
      render: (_: unknown, record: Team) => (
        <Link onClick={() => setMemberTeam(record)}>
          {record.memberCount ?? record.members?.length ?? 0}
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
      width: 140,
      render: (_: unknown, record: Team) => (
        <Space size="small">
          <Tooltip title={t('team.manageMembers')}>
            <Button type="text" size="small" icon={<TeamOutlined />} onClick={() => setMemberTeam(record)} />
          </Tooltip>
          <Tooltip title={t('channelPermission.title')}>
            <Button type="text" size="small" icon={<SafetyOutlined />} onClick={() => setChannelManageTeam(record)} />
          </Tooltip>
          {canWrite && (
            <>
              <Tooltip title={t('team.edit')}>
                <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
              </Tooltip>
              <Popconfirm
                title={t('team.deleteTeam')}
                description={t('team.deleteConfirm', { name: record.name })}
                okType="danger"
                onConfirm={() => deleteMutation.mutateAsync(record.id)}
              >
                <Tooltip title={t('team.delete')}>
                  <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                </Tooltip>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card title={t('title')}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 8 }}>
        <Space size={12} wrap>
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
        </Space>
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('team.addTeam')}
          </Button>
        )}
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={filteredTeams}
        loading={isLoading}
        pagination={false}
        scroll={{ x: 800 }}
      />

      </Card>

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
    </div>
  );
}
