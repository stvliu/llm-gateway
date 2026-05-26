import { Table, Button, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModelSpecs } from '@/services/query/useModelSpecs';

interface ProviderModelSpecTabProps {
  providerId: number;
  editing?: boolean;
}

/**
 * 供应商模型规格标签页
 * 展示供应商下的模型规格列表
 */
export default function ProviderModelSpecTab({ providerId, editing }: ProviderModelSpecTabProps) {
  const { t } = useTranslation('providers');
  const { data: modelSpecs, isLoading } = useModelSpecs(providerId);

  return (
    <div>
      {editing && (
        <Button type="primary" icon={<PlusOutlined />} style={{ marginBottom: 16 }}>
          {t('modelSpec.create', { defaultValue: '创建模型规格' })}
        </Button>
      )}
      <Table
        dataSource={modelSpecs}
        loading={isLoading}
        rowKey="id"
        pagination={false}
        columns={[
          { title: t('modelSpec.providerModelId', { defaultValue: '供应商模型 ID' }), dataIndex: 'providerModelId', key: 'providerModelId' },
          { title: t('modelSpec.displayName', { defaultValue: '显示名' }), dataIndex: 'displayName', key: 'displayName' },
          { title: t('modelSpec.modelFamily', { defaultValue: '模型族' }), dataIndex: 'modelFamily', key: 'modelFamily' },
          { title: t('fields.status', { defaultValue: '状态' }), dataIndex: 'state', key: 'state', render: (state: string) => <Tag color={state === 'ACTIVE' ? 'success' : 'warning'}>{state}</Tag> },
        ]}
      />
    </div>
  );
}
