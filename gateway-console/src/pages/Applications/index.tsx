import { useState, useMemo } from 'react';
import { Table, Button, Tag, Space, Input, Select, Popconfirm, Tooltip, Card, App } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SafetyOutlined, SearchOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useApplications, useDeleteApplication } from '@/services/query/useApplications';
import ApplicationFormModal from './ApplicationFormModal';
import ChannelManageModal from './ChannelManageModal';
import type { Application } from '@/types/application';

/**
 * 应用管理页
 *
 * Application 是权限+行为双聚合根，管理应用 CRUD 与渠道授权。
 *
 * <p>Task 10：移除容灾画像绑定（ResilienceProfile 退场），改为应用级 timeout 配置展示。
 * timeout=0 表示用渠道默认。</p>
 */
export default function ApplicationsPage() {
  const { t } = useTranslation('applications');
  const { message } = App.useApp();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.APPLICATION_WRITE);

  const { data: applications, isLoading } = useApplications();
  const deleteMutation = useDeleteApplication();

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
      // 应用级超时配置（承接原 ResilienceProfile.timeout，0 表示用渠道默认）
      title: t('application.timeout'),
      dataIndex: 'timeout',
      key: 'timeout',
      width: 120,
      render: (timeout: number) =>
        timeout > 0 ? (
          <span>{timeout}s</span>
        ) : (
          <Tag>{t('application.timeoutChannelDefault')}</Tag>
        ),
    },
    {
      title: t('application.actions'),
      key: 'actions',
      width: 120,
      render: (_: unknown, record: Application) => (
        <Space size="small">
          <Tooltip title={t('channelAuthorization.title')}>
            <Button type="text" size="small" icon={<SafetyOutlined />} onClick={() => setChannelManageApplication(record)} />
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
