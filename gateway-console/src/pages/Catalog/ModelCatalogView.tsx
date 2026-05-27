import { Tag, Space, Button, Typography, Spin, Table } from 'antd';
import { CloudDownloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModelCatalogs } from '@/services/query/useCatalog';
import type { ModelCatalog, MaterializeType } from '@/types/catalog';

const { Text } = Typography;

interface ModelCatalogViewProps {
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

/** 模型目录表格视图 */
export default function ModelCatalogView({ providerCode, onMaterialize }: ModelCatalogViewProps) {
  const { t } = useTranslation('catalog');

  // 数据查询：按供应商编码筛选模型
  const { data: models, isLoading } = useModelCatalogs({
    keyword: providerCode,
  });

  /** 表格列定义 */
  const columns = [
    {
      title: t('model.modelName'),
      dataIndex: 'modelName',
      key: 'modelName',
      render: (name: string, record: ModelCatalog) => (
        <Space>
          <Text strong>{name}</Text>
          {record.materialized && (
            <Tag color="success" style={{ fontSize: 10 }}>{t('model.materialized')}</Tag>
          )}
        </Space>
      ),
    },
    {
      title: t('model.contextWindow'),
      dataIndex: 'contextWindow',
      key: 'contextWindow',
      width: 120,
      render: (v: number | null) => v ? `${(v / 1024).toFixed(0)}K` : '-',
    },
    {
      title: t('model.maxOutputTokens'),
      dataIndex: 'maxOutputTokens',
      key: 'maxOutputTokens',
      width: 130,
      render: (v: number | null) => v ? `${(v / 1024).toFixed(0)}K` : '-',
    },
    {
      title: t('model.capabilities'),
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
      render: (_: unknown, record: ModelCatalog) => (
        <Button
          type="link"
          size="small"
          icon={<CloudDownloadOutlined />}
          disabled={record.materialized}
          onClick={(e) => {
            e.stopPropagation();
            if (record.materialized) return;
            onMaterialize('MODEL', record.modelName, record.modelName);
          }}
        >
          {record.materialized ? t('model.materialized') : t('materialize.model')}
        </Button>
      ),
    },
  ];

  const modelList = models ?? [];

  return (
    <Spin spinning={isLoading}>
      {modelList.length === 0 && !isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Text type="secondary">{t('message.noData', { defaultValue: '暂无数据' })}</Text>
        </div>
      ) : (
        <Table
          dataSource={modelList}
          columns={columns}
          rowKey="modelName"
          size="small"
          pagination={false}
        />
      )}
    </Spin>
  );
}