import { useMemo } from 'react';
import { Tag, Space, Button, Typography, Spin, Table } from 'antd';
import { CloudDownloadOutlined, PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { usePlanCatalogs } from '@/services/query/useCatalog';
import type { PlanCatalog, MaterializeType } from '@/types/catalog';

const { Text } = Typography;

interface PlanCatalogViewProps {
  /** 供应商编码 */
  providerCode: string;
  /** 选择套餐，进入模型规格目录 */
  onSelectPlan: (planCode: string, planName: string) => void;
  /** 物化操作 */
  onMaterialize: (type: MaterializeType, code: string, name: string) => void;
  /** 快速创建渠道 */
  onQuickCreate?: (planCode: string, planName: string) => void;
}

/** 计费模式标签配置 */
const BILLING_MODE_CONFIG: Record<string, { color: string }> = {
  pay_as_you_go: { color: 'green' },
  subscription: { color: 'purple' },
  package: { color: 'orange' },
};

/** 套餐目录表格视图 */
export default function PlanCatalogView({ providerCode, onSelectPlan, onMaterialize, onQuickCreate }: PlanCatalogViewProps) {
  const { t } = useTranslation('catalog');

  // 数据查询
  const { data: plans, isLoading } = usePlanCatalogs(providerCode);

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

  /** 表格列定义 */
  const columns = useMemo(() => [
    {
      title: t('plan.planName'),
      dataIndex: 'planName',
      key: 'planName',
      render: (name: string, record: PlanCatalog) => (
        <Space>
          <span style={{ fontWeight: 500 }}>{name}</span>
          {record.materialized && (
            <Tag color="success" style={{ fontSize: 10 }}>{t('plan.materialized')}</Tag>
          )}
        </Space>
      ),
    },
    {
      title: t('plan.planCode'),
      dataIndex: 'planCode',
      key: 'planCode',
      width: 160,
      render: (code: string) => <Text code style={{ fontSize: 11 }}>{code}</Text>,
    },
    {
      title: t('plan.billingMode'),
      dataIndex: 'billingMode',
      key: 'billingMode',
      width: 120,
      render: (mode: string) => {
        const config = BILLING_MODE_CONFIG[mode];
        return <Tag color={config?.color ?? 'default'}>{t(`billingMode.${mode}`, { defaultValue: mode })}</Tag>;
      },
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
      width: 260,
      render: (_: unknown, record: PlanCatalog) => (
        <Space size="small">
          {onQuickCreate && (
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                onQuickCreate(record.planCode, record.planName);
              }}
            >
              {t('quickCreate', { defaultValue: '快速创建' })}
            </Button>
          )}
          <Button
            type="link"
            size="small"
            icon={<CloudDownloadOutlined />}
            disabled={record.materialized}
            onClick={(e) => {
              e.stopPropagation();
              if (record.materialized) return;
              onMaterialize('PLAN', record.planCode, record.planName);
            }}
          >
            {record.materialized ? t('plan.materialized') : t('materialize.plan')}
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => onSelectPlan(record.planCode, record.planName)}
          >
            {t('plan.detail')}
          </Button>
        </Space>
      ),
    },
  ], [t, onSelectPlan, onMaterialize, onQuickCreate]);

  const planList = plans ?? [];

  return (
    <Spin spinning={isLoading}>
      {planList.length === 0 && !isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Text type="secondary">{t('message.noData', { defaultValue: '暂无数据' })}</Text>
        </div>
      ) : (
        <Table
          dataSource={planList}
          columns={columns}
          rowKey="planCode"
          size="small"
          pagination={false}
        />
      )}
    </Spin>
  );
}
