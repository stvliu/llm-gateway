import { useState, useMemo } from 'react';
import { Table, Button, Tag, Space, Input, Select, Popconfirm, Tooltip, Card, App } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SafetyOutlined, SearchOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useApplications, useDeleteApplication, useBindResilienceProfile } from '@/services/query/useApplications';
import { useResilienceProfiles } from '@/services/query/useResilience';
import { modeLabel, modeColor } from '@/pages/resilience/mode';
import ApplicationFormModal from './ApplicationFormModal';
import ChannelManageModal from './ChannelManageModal';
import type { Application } from '@/types/application';
import type { ResilienceProfile } from '@/types/resilience';

/**
 * 应用管理页
 *
 * Application 是权限+行为双聚合根，管理应用 CRUD 与渠道授权。
 * 原 Teams 页的成员管理、模型可见性机制已随 Team 体系废弃移除。
 */
export default function ApplicationsPage() {
  const { t } = useTranslation('applications');
  const { message } = App.useApp();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.APPLICATION_WRITE);
  const navigate = useNavigate();

  const { data: applications, isLoading } = useApplications();
  const deleteMutation = useDeleteApplication();
  // 容灾画像绑定 mutation：成功后 invalidate 应用列表，画像列自动刷新
  const bindProfileMutation = useBindResilienceProfile();
  // 容灾画像列表：用于反查 Application.resilienceProfileId 对应的画像名与档位，并提供下拉选项
  const { data: profiles } = useResilienceProfiles();
  const profileMap = useMemo(() => {
    const m = new Map<number, ResilienceProfile>();
    (profiles ?? []).forEach((p) => m.set(p.id, p));
    return m;
  }, [profiles]);
  // 下拉选项：默认（解绑）+ 全部画像
  const profileOptions = useMemo(
    () => [
      { value: 0, label: t('resilience.default') },
      ...(profiles ?? []).map((p) => ({
        value: p.id,
        label: `${p.name} · ${modeLabel(p.mode)}`,
      })),
    ],
    [profiles, t],
  );

  const [formVisible, setFormVisible] = useState(false);
  const [editingApplication, setEditingApplication] = useState<Application | undefined>();
  const [channelManageApplication, setChannelManageApplication] = useState<Application | null>(null);

  // 搜索筛选状态
  const [searchKeyword, setSearchKeyword] = useState('');
  const [stateFilter, setStateFilter] = useState<string | undefined>(undefined);

  const handleAdd = () => {
    setEditingApplication(undefined);
    setFormVisible(true);
  };

  const handleEdit = (application: Application) => {
    setEditingApplication(application);
    setFormVisible(true);
  };

  // 删除应用：吞掉 mutateAsync 的 rejection 避免未捕获 promise 拒绝，
  // 成功/失败均给出用户反馈（与 Models/UpstreamKeysTable 页风格一致）
  const handleDelete = async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success(t('application.deleteSuccess', { defaultValue: '应用已删除' }));
    } catch {
      message.error(t('application.deleteFailed', { defaultValue: '删除失败' }));
    }
  };

  // 绑定/解绑容灾画像：value=0 表示解绑（传 null）。成功后 invalidate 列表自动刷新画像列。
  const handleBindProfile = async (application: Application, value: number) => {
    const profileId = value === 0 ? null : value;
    try {
      await bindProfileMutation.mutateAsync({ id: application.id, resilienceProfileId: profileId });
      message.success(t('resilience.bindSuccess', { defaultValue: '容灾画像已更新' }));
    } catch {
      message.error(t('resilience.bindFailed', { defaultValue: '容灾画像更新失败' }));
    }
  };

  // 筛选后的应用列表
  const filteredApplications = useMemo(() => {
    if (!applications) return [];
    return applications.filter((application: Application) => {
      // 名称或编码搜索
      if (searchKeyword) {
        const keyword = searchKeyword.toLowerCase();
        const matchName = application.name.toLowerCase().includes(keyword);
        const matchCode = application.code.toLowerCase().includes(keyword);
        if (!matchName && !matchCode) {
          return false;
        }
      }
      // 状态筛选
      if (stateFilter && application.state !== stateFilter) {
        return false;
      }
      return true;
    });
  }, [applications, searchKeyword, stateFilter]);

  const columns = [
    {
      title: t('application.code'),
      dataIndex: 'code',
      key: 'code',
      width: 140,
    },
    {
      title: t('application.name'),
      dataIndex: 'name',
      key: 'name',
      width: 140,
    },
    {
      title: t('application.description'),
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: t('application.state'),
      dataIndex: 'state',
      key: 'state',
      width: 100,
      render: (state: string) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'default'}>
          {state === 'ACTIVE' ? t('application.stateActive') : t('application.stateInactive')}
        </Tag>
      ),
    },
    {
      // 容灾画像列：「选而非填」范式——管理员给应用选容灾画像模板，非逐字段配。
      // canWrite 时为行内 Select（含「默认=解绑」选项），只读时降级为档位 Tag。
      title: t('resilience.column'),
      key: 'resilience',
      width: 200,
      render: (_: unknown, record: Application) => {
        const profile = record.resilienceProfileId
          ? profileMap.get(record.resilienceProfileId)
          : undefined;
        // 只读：档位 Tag
        if (!canWrite) {
          if (!profile) {
            return <Tag>{t('resilience.default')}</Tag>;
          }
          return (
            <Tag color={modeColor(profile.mode)}>
              {profile.name} · {modeLabel(profile.mode)}
            </Tag>
          );
        }
        // 可写：行内 Select 绑定（0=默认/解绑）
        return (
          <Select
            size="small"
            style={{ width: '100%' }}
            value={record.resilienceProfileId ?? 0}
            loading={bindProfileMutation.isPending}
            options={profileOptions}
            onChange={(value: number) => handleBindProfile(record, value)}
          />
        );
      },
    },
    {
      title: t('application.actions'),
      key: 'actions',
      width: 140,
      render: (_: unknown, record: Application) => (
        <Space size="small">
          <Tooltip title={t('channelAuthorization.title')}>
            <Button type="text" size="small" icon={<SafetyOutlined />} onClick={() => setChannelManageApplication(record)} />
          </Tooltip>
          <Tooltip title={t('resilience.configure')}>
            <Button
              type="text"
              size="small"
              icon={<ThunderboltOutlined />}
              onClick={() => navigate('/resilience/profiles')}
            />
          </Tooltip>
          {canWrite && (
            <>
              <Tooltip title={t('application.edit')}>
                <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
              </Tooltip>
              <Popconfirm
                title={t('application.deleteApplication')}
                description={t('application.deleteConfirm', { name: record.name })}
                okType="danger"
                onConfirm={() => handleDelete(record.id)}
              >
                <Tooltip title={t('application.delete')}>
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
              placeholder={t('application.searchPlaceholder')}
              prefix={<SearchOutlined />}
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              allowClear
              style={{ width: 250 }}
            />
            <Select
              placeholder={t('application.state')}
              value={stateFilter}
              onChange={setStateFilter}
              allowClear
              style={{ width: 120 }}
              options={[
                { value: 'ACTIVE', label: t('application.stateActive') },
                { value: 'INACTIVE', label: t('application.stateInactive') },
              ]}
            />
          </Space>
          {canWrite && (
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              {t('application.addApplication')}
            </Button>
          )}
        </div>

        <Table
          rowKey="id"
          columns={columns}
          dataSource={filteredApplications}
          loading={isLoading}
          pagination={false}
          scroll={{ x: 800 }}
        />
      </Card>

      <ApplicationFormModal
        visible={formVisible}
        application={editingApplication}
        onClose={() => setFormVisible(false)}
      />

      {channelManageApplication && (
        <ChannelManageModal
          open={true}
          applicationId={channelManageApplication.id}
          applicationName={channelManageApplication.name}
          onCancel={() => setChannelManageApplication(null)}
        />
      )}
    </div>
  );
}
