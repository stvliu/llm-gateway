import { Table, Tag, Card } from 'antd';
import { useTranslation } from 'react-i18next';
import { useModels } from '@/services/query';
import type { ModelType } from '@/types/model';
import type { ColumnsType } from 'antd/es/table';
import type { Model } from '@/types/model';

export default function UserModels() {
  const { t } = useTranslation('models');
  const { data, isLoading } = useModels({ size: 100 });

  const columns: ColumnsType<Model> = [
    { title: t('model.name'), dataIndex: 'name', key: 'name' },
    { title: t('model.provider'), dataIndex: 'providerName', key: 'providerName' },
    {
      title: t('model.type'),
      dataIndex: 'type',
      key: 'type',
      render: (type: ModelType) => t(`type.${type}`),
    },
    {
      title: t('model.state'),
      dataIndex: 'state',
      key: 'state',
      render: (state) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'red'}>
          {t(`state.${state.toLowerCase()}`, { ns: 'common' })}
        </Tag>
      ),
    },
  ];

  return (
    <Card title={t('title')}>
      <Table
        columns={columns}
        dataSource={data?.items || []}
        rowKey="id"
        loading={isLoading}
        pagination={{ pageSize: 10 }}
      />
    </Card>
  );
}
