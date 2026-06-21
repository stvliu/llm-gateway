import { useState } from 'react';
import { Table, Button, Tag, Card, Tooltip, Empty } from 'antd';
import { PlusOutlined, EditOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useResilienceProfiles } from '@/services/query/useResilience';
import { modeLabel, modeColor } from '../mode';
import ProfileFormModal from './ProfileFormModal';
import type { ResilienceProfile, ResilienceMode } from '@/types/resilience';

/**
 * 容灾画像模板页
 *
 * <p>「选而非填」范式屏2：管理预设画像模板，专家字段折叠在表单「高级配置」里。
 * 不提供 delete（default 为系统兜底，后端 4.11a 未交付 delete 端点）。</p>
 */
export default function ProfilesPage() {
  const { t } = useTranslation('resilience');
  const { data: profiles, isLoading } = useResilienceProfiles();
  const [formVisible, setFormVisible] = useState(false);
  const [editingProfile, setEditingProfile] = useState<ResilienceProfile | undefined>();

  const handleAdd = () => {
    setEditingProfile(undefined);
    setFormVisible(true);
  };

  const handleEdit = (profile: ResilienceProfile) => {
    setEditingProfile(profile);
    setFormVisible(true);
  };

  const columns = [
    {
      title: t('profiles.code'),
      dataIndex: 'code',
      key: 'code',
      width: 160,
      render: (code: string) => <span style={{ fontFamily: 'monospace' }}>{code}</span>,
    },
    {
      title: t('profiles.name'),
      dataIndex: 'name',
      key: 'name',
      width: 160,
    },
    {
      title: t('profiles.mode'),
      dataIndex: 'mode',
      key: 'mode',
      width: 100,
      render: (mode: ResilienceMode) => (
        <Tag color={modeColor(mode)}>{modeLabel(mode)}</Tag>
      ),
    },
    {
      title: t('fallback.label'),
      key: 'fallback',
      width: 160,
      render: (_: unknown, record: ResilienceProfile) =>
        record.enableL2ModelDegradation ? (
          <Tag color="green">
            {t('fallback.enabled')} · {t('fallback.maxDepth')}={record.degradationMaxDepth}
          </Tag>
        ) : (
          <Tag color="default">{t('mode.STRICT')}</Tag>
        ),
    },
    {
      title: t('profiles.timeout'),
      dataIndex: 'timeout',
      key: 'timeout',
      width: 120,
      render: (timeout: number) =>
        timeout > 0 ? <span>{timeout}s</span> : <Tag>渠道默认</Tag>,
    },
    {
      title: t('profiles.edit'),
      key: 'actions',
      width: 80,
      render: (_: unknown, record: ResilienceProfile) => (
        <Tooltip title={t('profiles.edit')}>
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          />
        </Tooltip>
      ),
    },
  ];

  return (
    <Card title={t('profiles.title')}>
      <div style={{ marginBottom: 16, color: 'rgba(0,0,0,0.45)', fontSize: 13 }}>
        {t('profiles.subtitle')}
      </div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          {t('profiles.add')}
        </Button>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={profiles ?? []}
        loading={isLoading}
        pagination={false}
        locale={{
          emptyText: <Empty description={t('profiles.deleteForbidden')} />,
        }}
      />

      <ProfileFormModal
        visible={formVisible}
        profile={editingProfile}
        onClose={() => setFormVisible(false)}
      />
    </Card>
  );
}
