import { Table, Button, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useChannels } from '@/services/query/useChannels';

interface ProviderChannelTabProps {
  providerId: number;
  editing?: boolean;
}

/**
 * 供应商渠道标签页
 * 展示供应商下的渠道列表，支持展开查看端点和凭证（后续实现）
 */
export default function ProviderChannelTab({ providerId, editing }: ProviderChannelTabProps) {
  const { t } = useTranslation('providers');
  const { data: channels, isLoading } = useChannels(providerId);

  return (
    <div>
      {editing && (
        <Button type="primary" icon={<PlusOutlined />} style={{ marginBottom: 16 }}>
          {t('channel.create', { defaultValue: '创建渠道' })}
        </Button>
      )}
      <Table
        dataSource={channels}
        loading={isLoading}
        rowKey="id"
        pagination={false}
        columns={[
          { title: t('channel.name', { defaultValue: '渠道名称' }), dataIndex: 'name', key: 'name' },
          { title: t('fields.status', { defaultValue: '状态' }), dataIndex: 'state', key: 'state', render: (state: string) => <Tag color={state === 'ACTIVE' ? 'success' : 'warning'}>{state}</Tag> },
          { title: t('channel.priority', { defaultValue: '优先级' }), dataIndex: 'priority', key: 'priority' },
        ]}
        expandable={{
          expandedRowRender: () => <div>端点和凭证（后续 Task 实现）</div>,
        }}
      />
    </div>
  );
}
