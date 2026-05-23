import { useState } from 'react';
import { Card, Button, Space, Modal, Input, Form, Tag, Typography, theme, Spin, App, Table, Breadcrumb } from 'antd';
import { SyncOutlined, CloudDownloadOutlined, SearchOutlined, CloseOutlined, ArrowRightOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviderMetadata, useProductMetadataByProvider, useModelMetadataByProduct, useApplyMetadata, useSyncMetadata } from '@/services/query/useMetadata';
import type { ProviderMetadata, ProductMetadata } from '@/types/metadata';

const { Text } = Typography;

/** 产品类型选项 */
const PRODUCT_TYPE_OPTIONS = [
  { value: 'STANDARD', label: '按量付费', color: 'green' },
  { value: 'BATCH', label: '批量处理', color: 'orange' },
  { value: 'CACHE', label: '缓存折扣', color: 'cyan' },
  { value: 'SUBSCRIPTION', label: '订阅制', color: 'purple' },
  { value: 'PROMOTION', label: '限时优惠', color: 'red' },
  { value: 'FREE_TIER', label: '免费额度', color: 'default' },
];

/** 数据来源选项 */
const SOURCE_OPTIONS = [
  { value: 'BUILTIN', label: '内置' },
  { value: 'MODELS_DEV', label: 'Models.dev 同步' },
  { value: 'MANUAL', label: '手动添加' },
];

/**
 * 元数据管理页面 - 三级联动导航
 * 供应商元数据 → 产品元数据 → 模型元数据
 */
export default function MetadataPage() {
  // 导航状态：三级联动
  const [providerId, setProviderId] = useState<string | undefined>();
  const [productId, setProductId] = useState<number | null>(null);
  const [providerName, setProviderName] = useState<string>('');
  const [productName, setProductName] = useState<string>('');

  return (
    <div style={{ padding: 24 }}>
      <Card>
        {/* 面包屑导航 */}
        <div style={{ marginBottom: 16 }}>
          <Breadcrumb
            items={[
              {
                title: (
                  <Button
                    type={providerId ? 'link' : 'text'}
                    style={{ padding: 0, fontWeight: providerId ? undefined : 600 }}
                    onClick={() => {
                      setProviderId(undefined);
                      setProductId(null);
                    }}
                  >
                    供应商元数据
                  </Button>
                ),
              },
              ...(providerId
                ? [
                    {
                      title: (
                        <Button
                          type={productId ? 'link' : 'text'}
                          style={{ padding: 0, fontWeight: productId ? undefined : 600 }}
                          onClick={() => setProductId(null)}
                        >
                          产品元数据
                          {providerName && <Text type="secondary" style={{ marginLeft: 4, fontSize: 12 }}>({providerName})</Text>}
                        </Button>
                      ),
                    },
                  ]
                : []),
              ...(productId
                ? [
                    {
                      title: (
                        <Text strong>
                          模型元数据
                          {productName && <Text type="secondary" style={{ marginLeft: 4, fontSize: 12 }}>({productName})</Text>}
                        </Text>
                      ),
                    },
                  ]
                : []),
            ]}
          />
        </div>

        {/* 三级视图切换 */}
        {!providerId && (
          <ProviderMetadataView
            onSelectProvider={(id, name) => {
              setProviderId(id);
              setProviderName(name);
              setProductId(null);
            }}
          />
        )}
        {providerId && !productId && (
          <ProductMetadataView
            providerId={providerId}
            onSelectProduct={(id, name) => {
              setProductId(id);
              setProductName(name);
            }}
          />
        )}
        {providerId && productId && (
          <ModelMetadataView productId={productId} />
        )}
      </Card>
    </div>
  );
}

// ============================================================
// 第一级：供应商元数据
// ============================================================

function ProviderMetadataView({
  onSelectProvider,
}: {
  onSelectProvider: (providerId: string, providerName: string) => void;
}) {
  const { t } = useTranslation('metadata');
  const { message } = App.useApp();
  const { token } = theme.useToken();
  const [page] = useState(0);
  const [applyModalOpen, setApplyModalOpen] = useState(false);
  const [selectedMetadata, setSelectedMetadata] = useState<ProviderMetadata | null>(null);
  const [keyword, setKeyword] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');

  const { data, isLoading } = useProviderMetadata({
    page,
    size: 20,
    keyword: searchKeyword || undefined,
  });
  const applyMutation = useApplyMetadata();
  const syncMutation = useSyncMetadata();

  const handleSync = async (type: 'all' | 'builtin' | 'models-dev') => {
    try {
      await syncMutation.mutateAsync(type);
      message.success(t('message.syncSuccess'));
    } catch {
      message.error(t('message.syncFailed'));
    }
  };

  const handleApply = async (values: { apiKey: string; channelName?: string }) => {
    if (!selectedMetadata) return;
    try {
      await applyMutation.mutateAsync({
        id: selectedMetadata.id,
        data: { apiKey: values.apiKey, channelName: values.channelName },
      });
      message.success(t('message.applySuccess'));
      setApplyModalOpen(false);
      setSelectedMetadata(null);
    } catch {
      message.error(t('message.applyFailed', { defaultValue: '应用失败' }));
    }
  };

  const hasActiveFilters = !!searchKeyword;
  const providerList = data?.content ?? [];

  return (
    <div>
      {/* 搜索和同步栏 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <Space wrap>
          <Input.Search
            placeholder={t('searchProviderPlaceholder', { defaultValue: '搜索供应商名称或 ID' })}
            allowClear
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={(value) => setSearchKeyword(value)}
            style={{ width: 240 }}
            prefix={<SearchOutlined />}
          />
          {hasActiveFilters && (
            <Button icon={<CloseOutlined />} onClick={() => { setKeyword(''); setSearchKeyword(''); }}>
              {t('clearFilters', { defaultValue: '清除筛选' })}
            </Button>
          )}
        </Space>
        <Space>
          <Button icon={<SyncOutlined />} onClick={() => handleSync('all')} loading={syncMutation.isPending}>
            {t('syncAll')}
          </Button>
          <Button onClick={() => handleSync('builtin')}>
            {t('syncBuiltin')}
          </Button>
          <Button onClick={() => handleSync('models-dev')}>
            {t('syncModelsDev')}
          </Button>
        </Space>
      </div>

      {/* 供应商卡片网格 */}
      <Spin spinning={isLoading}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 16, minHeight: 200 }}>
          {providerList.map((pm: ProviderMetadata) => (
            <Card
              key={pm.id}
              size="small"
              hoverable
              actions={[
                <Button
                  key="apply"
                  type="link"
                  icon={<CloudDownloadOutlined />}
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelectedMetadata(pm);
                    setApplyModalOpen(true);
                  }}
                >
                  {t('apply')}
                </Button>,
                <Button
                  key="view"
                  type="link"
                  icon={<ArrowRightOutlined />}
                  onClick={() => onSelectProvider(pm.providerId, pm.providerName)}
                >
                  产品
                </Button>,
              ]}
            >
              <Card.Meta
                title={<span>{pm.providerName}</span>}
                description={
                  <div>
                    <Text type="secondary" style={{ fontSize: 12 }}>{pm.providerId}</Text>
                    {pm.description && <div style={{ marginTop: 4 }}>{pm.description}</div>}
                    <div
                      style={{
                        marginTop: 12,
                        padding: '8px 12px',
                        background: token.colorFillAlter,
                        borderRadius: token.borderRadius,
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                      }}
                    >
                      <Space>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {t('modelCount', { defaultValue: '模型数量' })}:
                        </Text>
                        <Tag color={pm.modelCount > 0 ? 'processing' : 'default'}>
                          {pm.modelCount ?? 0}
                        </Tag>
                      </Space>
                    </div>
                  </div>
                }
              />
            </Card>
          ))}
        </div>
      </Spin>

      {providerList.length === 0 && !isLoading && (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Text type="secondary">{t('message.noData', { defaultValue: '暂无数据' })}</Text>
        </div>
      )}

      <ApplyMetadataModal
        open={applyModalOpen}
        metadata={selectedMetadata}
        onClose={() => {
          setApplyModalOpen(false);
          setSelectedMetadata(null);
        }}
        onApply={handleApply}
        loading={applyMutation.isPending}
      />
    </div>
  );
}

// ============================================================
// 第二级：产品元数据（按供应商过滤）
// ============================================================

function ProductMetadataView({
  providerId,
  onSelectProduct,
}: {
  providerId: string;
  onSelectProduct: (productId: number, productName: string) => void;
}) {
  const { t } = useTranslation('metadata');
  const { data, isLoading } = useProductMetadataByProvider(providerId);

  const products = data ?? [];

  return (
    <div>
      <Spin spinning={isLoading}>
        {products.length === 0 && !isLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <Text type="secondary">该供应商暂无产品</Text>
          </div>
        ) : (
          <Table
            dataSource={products}
            rowKey="id"
            size="small"
            pagination={false}
            onRow={(record) => ({
              onClick: () => onSelectProduct(record.id, record.productName),
              style: { cursor: 'pointer' },
            })}
            columns={[
              {
                title: t('product.name', { defaultValue: '产品名称' }),
                dataIndex: 'productName',
                key: 'productName',
                render: (name: string, record: ProductMetadata) => (
                  <Space>
                    <span style={{ fontWeight: 500 }}>{name}</span>
                    {record.isDefault && <Tag color="blue">默认</Tag>}
                  </Space>
                ),
              },
              {
                title: t('product.type', { defaultValue: '类型' }),
                dataIndex: 'productType',
                key: 'productType',
                width: 120,
                render: (type: string) => {
                  const info = PRODUCT_TYPE_OPTIONS.find(o => o.value === type);
                  return <Tag color={info?.color ?? 'default'}>{info?.label ?? type}</Tag>;
                },
              },
              {
                title: t('product.endpoints', { defaultValue: '端点' }),
                dataIndex: 'endpoints',
                key: 'endpoints',
                render: (endpoints: Record<string, string>) => (
                  <Space orientation="vertical" size={2}>
                    {Object.entries(endpoints ?? {}).map(([protocol, url]) => (
                      <Space key={protocol} size={4}>
                        <Tag style={{ fontSize: 10 }}>{protocol}</Tag>
                        <Text code style={{ fontSize: 11 }}>{url}</Text>
                      </Space>
                    ))}
                  </Space>
                ),
              },
              {
                title: t('product.description', { defaultValue: '描述' }),
                dataIndex: 'description',
                key: 'description',
                ellipsis: true,
              },
              {
                title: '',
                key: 'action',
                width: 60,
                render: () => <ArrowRightOutlined style={{ color: 'var(--ant-color-text-secondary)' }} />,
              },
            ]}
          />
        )}
      </Spin>
    </div>
  );
}

// ============================================================
// 第三级：模型元数据（按产品过滤）
// ============================================================

function ModelMetadataView({ productId }: { productId: number }) {
  const { t } = useTranslation('metadata');
  const { token } = theme.useToken();
  const { data, isLoading } = useModelMetadataByProduct(productId);

  const models = data ?? [];

  return (
    <div>
      <Spin spinning={isLoading}>
        {models.length === 0 && !isLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <Text type="secondary">该产品暂无模型</Text>
          </div>
        ) : (
          <Table
            dataSource={models}
            rowKey="id"
            size="small"
            pagination={false}
            columns={[
              {
                title: t('model.displayName', { defaultValue: '显示名称' }),
                dataIndex: 'displayName',
                key: 'displayName',
                render: (name: string) => <Text strong>{name}</Text>,
              },
              {
                title: t('model.providerModelId', { defaultValue: '模型标识' }),
                dataIndex: 'providerModelId',
                key: 'providerModelId',
                render: (id: string) => <Text code style={{ fontSize: 11 }}>{id}</Text>,
              },
              {
                title: t('model.contextWindow', { defaultValue: '上下文窗口' }),
                dataIndex: 'contextWindow',
                key: 'contextWindow',
                width: 120,
                render: (v: number | undefined) => v ? `${(v / 1024).toFixed(0)}K` : '-',
              },
              {
                title: t('model.inputPrice', { defaultValue: '输入价格' }),
                dataIndex: 'inputPrice',
                key: 'inputPrice',
                width: 100,
                render: (v: number | undefined) => v != null ? `$${v}` : '-',
              },
              {
                title: t('model.outputPrice', { defaultValue: '输出价格' }),
                dataIndex: 'outputPrice',
                key: 'outputPrice',
        width: 100,
                render: (v: number | undefined) => v != null ? `$${v}` : '-',
              },
              {
                title: t('model.capabilities', { defaultValue: '能力' }),
                dataIndex: 'capabilities',
                key: 'capabilities',
                render: (caps: Record<string, boolean>) => (
                  <Space size={4} wrap>
                    {Object.entries(caps ?? {})
                      .filter(([, v]) => v)
                      .map(([k]) => <Tag key={k} style={{ fontSize: 10 }}>{k}</Tag>)}
                  </Space>
                ),
              },
              {
                title: t('model.source', { defaultValue: '来源' }),
                dataIndex: 'source',
                key: 'source',
                width: 90,
                render: (source: string) => (
                  <Tag color={source === 'BUILTIN' ? 'blue' : source === 'MODELS_DEV' ? 'green' : 'default'} style={{ fontSize: 10 }}>
                    {SOURCE_OPTIONS.find(s => s.value === source)?.label ?? source}
                  </Tag>
                ),
              },
            ]}
          />
        )}
      </Spin>
    </div>
  );
}

// ============================================================
// 应用元数据弹窗
// ============================================================

function ApplyMetadataModal({
  open,
  metadata,
  onClose,
  onApply,
  loading,
}: {
  open: boolean;
  metadata: ProviderMetadata | null;
  onClose: () => void;
  onApply: (values: { apiKey: string; channelName?: string }) => void;
  loading: boolean;
}) {
  const { t } = useTranslation('metadata');
  const [form] = Form.useForm();

  return (
    <Modal
      title={t('applyTitle')}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={loading}
    >
      {metadata && (
        <div style={{ marginBottom: 16 }}>
          <span>{metadata.providerName}</span>
          {metadata.modelCount !== undefined && (
            <Text type="secondary" style={{ marginLeft: 8 }}>
              ({metadata.modelCount} 个模型)
            </Text>
          )}
        </div>
      )}
      <Form form={form} layout="vertical" onFinish={onApply}>
        <Form.Item
          name="apiKey"
          label={t('apiKey')}
          rules={[{ required: true, message: t('apiKeyRequired') }]}
        >
          <Input.Password placeholder={t('apiKeyPlaceholder')} />
        </Form.Item>
        <Form.Item name="channelName" label={t('channelName')}>
          <Input placeholder={t('channelNamePlaceholder')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}