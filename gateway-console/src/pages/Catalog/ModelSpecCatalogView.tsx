import { Tag, Space, Button, Typography, Spin, Table } from 'antd';
import { CloudDownloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModelSpecCatalogs } from '@/services/query/useCatalog';
import type { ModelSpecCatalog, MaterializeType } from '@/types/catalog';

const { Text } = Typography;

interface ModelSpecCatalogViewProps {
  /** 供应商编码（用于筛选） */
  providerCode: string;
  /** 套餐编码（展示用） */
  planCode: string;
  /** 物化操作 */
  onMaterialize: (type: MaterializeType, code: string, name: string) => void;
}

/** 来源标签颜色 */
const sourceColor = (source: string) => {
  switch (source) {
    case 'BUILTIN': return 'blue';
    case 'MODELS_DEV': return 'green';
    case 'PROVIDER_API': return 'orange';
    case 'MANUAL': return 'default';
    case 'OVERRIDE': return 'red';
    default: return 'default';
  }
};

/** 模型规格目录表格视图 */
export default function ModelSpecCatalogView({ providerCode, onMaterialize }: ModelSpecCatalogViewProps) {
  const { t } = useTranslation('catalog');

  // 数据查询：按供应商编码筛选模型规格
  const { data: modelSpecs, isLoading } = useModelSpecCatalogs({
    keyword: providerCode,
  });

  /** 表格列定义 */
  const columns = [
    {
      title: t('modelSpec.modelName'),
      dataIndex: 'modelName',
      key: 'modelName',
      render: (name: string, record: ModelSpecCatalog) => (
        <Space>
          <Text strong>{name}</Text>
          {record.materialized && (
            <Tag color="success" style={{ fontSize: 10 }}>{t('modelSpec.materialized')}</Tag>
          )}
        </Space>
      ),
    },
    {
      title: t('modelSpec.providerModelId'),
      dataIndex: 'providerModelId',
      key: 'providerModelId',
      width: 200,
      render: (id: string) => <Text code style={{ fontSize: 11 }}>{id}</Text>,
    },
    {
      title: t('modelSpec.contextWindow'),
      dataIndex: 'contextWindow',
      key: 'contextWindow',
      width: 120,
      render: (v: number | null) => v ? `${(v / 1024).toFixed(0)}K` : '-',
    },
    {
      title: t('modelSpec.maxOutputTokens'),
      dataIndex: 'maxOutputTokens',
      key: 'maxOutputTokens',
      width: 130,
      render: (v: number | null) => v ? `${(v / 1024).toFixed(0)}K` : '-',
    },
    {
      title: t('modelSpec.capabilities'),
      dataIndex: 'capabilities',
      key: 'capabilities',
      render: (caps: string[] | null) => (
        <Space size={4} wrap>
          {(caps ?? []).map((cap) => (
            <Tag key={cap} style={{ fontSize: 10 }}>{cap}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: t('source.label'),
      dataIndex: 'source',
      key: 'source',
      width: 100,
      render: (source: string) => (
        <Tag color={sourceColor(source)} style={{ fontSize: 10 }}>
          {t(`source.${source}`)}
        </Tag>
      ),
    },
    {
      title: '',
      key: 'actions',
      width: 140,
      render: (_: unknown, record: ModelSpecCatalog) => (
        <Button
          type="link"
          size="small"
          icon={<CloudDownloadOutlined />}
          disabled={record.materialized}
          onClick={(e) => {
            e.stopPropagation();
            if (record.materialized) return;
            onMaterialize('MODEL_SPEC', record.providerModelId, record.modelName);
          }}
        >
          {record.materialized ? t('modelSpec.materialized') : t('materialize.modelSpec')}
        </Button>
      ),
    },
  ];

  const modelSpecList = modelSpecs ?? [];

  return (
    <Spin spinning={isLoading}>
      {modelSpecList.length === 0 && !isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Text type="secondary">{t('message.noData', { defaultValue: '暂无数据' })}</Text>
        </div>
      ) : (
        <Table
          dataSource={modelSpecList}
          columns={columns}
          rowKey="providerModelId"
          size="small"
          pagination={false}
        />
      )}
    </Spin>
  );
}
