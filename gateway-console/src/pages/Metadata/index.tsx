import { useState, useEffect, useMemo } from 'react';
import { Tabs, Card, Button, Space, Modal, Input, Form, Tag, Typography, theme, Select, Spin, App } from 'antd';
import { SyncOutlined, CloudDownloadOutlined, RightOutlined, SearchOutlined, FilterOutlined, CloseOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviderMetadata, useModelMetadata, useApplyMetadata, useSyncMetadata } from '@/services/query/useMetadata';
import type { ProviderMetadata, ModelMetadata, MetadataSource } from '@/types/metadata';

const { Text } = Typography;

/** 数据来源选项 */
const SOURCE_OPTIONS = [
  { value: 'BUILTIN', label: '内置' },
  { value: 'MODELS_DEV', label: 'Models.dev 同步' },
  { value: 'MANUAL', label: '手动添加' },
];

/**
 * 元数据管理页面
 * 管理供应商元数据和模型元数据，支持同步和发布
 */
export default function MetadataPage() {
  const { t } = useTranslation('metadata');
  const [activeTab, setActiveTab] = useState('providers');
  const [filterProviderId, setFilterProviderId] = useState<string | undefined>();

  // 切换到模型 Tab 并按供应商筛选
  const handleViewModelsByProvider = (providerId: string) => {
    setFilterProviderId(providerId);
    setActiveTab('models');
  };

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={(key) => {
            setActiveTab(key);
            if (key === 'providers') {
              setFilterProviderId(undefined);
            }
          }}
          items={[
            {
              key: 'providers',
              label: t('tabs.providers'),
              children: (
                <ProviderMetadataTab onViewModelsByProvider={handleViewModelsByProvider} />
              ),
            },
            {
              key: 'models',
              label: (
                <Space>
                  {t('tabs.models')}
                  {filterProviderId && (
                    <Tag
                      closable
                      onClose={() => setFilterProviderId(undefined)}
                      style={{ marginLeft: 4 }}
                    >
                      {filterProviderId}
                    </Tag>
                  )}
                </Space>
              ),
              children: (
                <ModelMetadataTab initialProviderId={filterProviderId} />
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}

/** 供应商元数据 Tab */
function ProviderMetadataTab({
  onViewModelsByProvider,
}: {
  onViewModelsByProvider: (providerId: string) => void;
}) {
  const { t } = useTranslation('metadata');
  const { message } = App.useApp();
  const { token } = theme.useToken();
  const [page] = useState(0);
  const [applyModalOpen, setApplyModalOpen] = useState(false);
  const [selectedMetadata, setSelectedMetadata] = useState<ProviderMetadata | null>(null);

  // 搜索和筛选状态
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

  // 清除所有筛选
  const handleClearFilters = () => {
    setKeyword('');
    setSearchKeyword('');
  };

  // 是否有活跃的筛选条件
  const hasActiveFilters = !!searchKeyword;

  const providerList = data?.content ?? [];

  return (
    <div>
      {/* 搜索和筛选栏 */}
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
            <Button icon={<CloseOutlined />} onClick={handleClearFilters}>
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

      {/* 当前筛选条件显示 */}
      {hasActiveFilters && (
        <div style={{ marginBottom: 12 }}>
          <Space size={4}>
            <FilterOutlined style={{ color: token.colorTextSecondary }} />
            <Text type="secondary">{t('currentFilters', { defaultValue: '当前筛选' })}:</Text>
            {searchKeyword && (
              <Tag closable onClose={() => { setKeyword(''); setSearchKeyword(''); }}>
                {t('keyword', { defaultValue: '关键词' })}: {searchKeyword}
              </Tag>
            )}
          </Space>
        </div>
      )}

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
                  onClick={() => {
                    setSelectedMetadata(pm);
                    setApplyModalOpen(true);
                  }}
                >
                  {t('apply')}
                </Button>,
              ]}
            >
              <Card.Meta
                title={
                  <span>{pm.providerName}</span>
                }
                description={
                  <div>
                    <Text type="secondary" style={{ fontSize: 12 }}>{pm.providerId}</Text>
                    {pm.description && <div style={{ marginTop: 4 }}>{pm.description}</div>}
                    {/* 模型概览 */}
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
                      <Button
                        type="link"
                        size="small"
                        icon={<RightOutlined />}
                        onClick={() => onViewModelsByProvider(pm.providerId)}
                        disabled={!pm.modelCount}
                      >
                        {t('viewAll', { defaultValue: '查看' })}
                      </Button>
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

/** 模型元数据 Tab */
function ModelMetadataTab({ initialProviderId }: { initialProviderId?: string }) {
  const { t } = useTranslation('metadata');
  const { token } = theme.useToken();
  const [page] = useState(0);

  // 搜索和筛选状态
  const [keyword, setKeyword] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [providerIdFilter, setProviderIdFilter] = useState<string | undefined>(initialProviderId);
  const [sourceFilter, setSourceFilter] = useState<MetadataSource | undefined>();

  // 当 initialProviderId 变化时更新筛选
  useEffect(() => {
    if (initialProviderId) {
      setProviderIdFilter(initialProviderId);
    }
  }, [initialProviderId]);

  const { data, isLoading } = useModelMetadata({
    page,
    size: 20,
    keyword: searchKeyword || undefined,
    providerId: providerIdFilter,
    source: sourceFilter,
  });

  // 获取供应商列表用于筛选下拉框
  const { data: providerData } = useProviderMetadata({ page: 0, size: 100 });
  const providerOptions = useMemo(() => {
    const providers = providerData?.content ?? [];
    return providers.map((p: ProviderMetadata) => ({
      value: p.providerId,
      label: `${p.providerName} (${p.providerId})`,
    }));
  }, [providerData]);

  // 清除所有筛选
  const handleClearFilters = () => {
    setKeyword('');
    setSearchKeyword('');
    setProviderIdFilter(undefined);
    setSourceFilter(undefined);
  };

  // 是否有活跃的筛选条件
  const hasActiveFilters = searchKeyword || providerIdFilter || sourceFilter;

  const modelList = data?.content ?? [];

  return (
    <div>
      {/* 搜索和筛选栏 */}
      <div style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search
            placeholder={t('searchModelPlaceholder', { defaultValue: '搜索模型名称' })}
            allowClear
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={(value) => setSearchKeyword(value)}
            style={{ width: 240 }}
            prefix={<SearchOutlined />}
          />
          <Select
            placeholder={t('filterByProvider', { defaultValue: '供应商' })}
            allowClear
            showSearch
            value={providerIdFilter}
            onChange={setProviderIdFilter}
            style={{ width: 200 }}
            options={providerOptions}
            filterOption={(input, option) =>
              (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
            }
          />
          <Select
            placeholder={t('filterBySource', { defaultValue: '来源' })}
            allowClear
            value={sourceFilter}
            onChange={setSourceFilter}
            style={{ width: 160 }}
            options={SOURCE_OPTIONS}
          />
          {hasActiveFilters && (
            <Button icon={<CloseOutlined />} onClick={handleClearFilters}>
              {t('clearFilters', { defaultValue: '清除筛选' })}
            </Button>
          )}
        </Space>
      </div>

      {/* 当前筛选条件显示 */}
      {hasActiveFilters && (
        <div style={{ marginBottom: 12 }}>
          <Space size={4}>
            <FilterOutlined style={{ color: token.colorTextSecondary }} />
            <Text type="secondary">{t('currentFilters', { defaultValue: '当前筛选' })}:</Text>
            {searchKeyword && (
              <Tag closable onClose={() => { setKeyword(''); setSearchKeyword(''); }}>
                {t('keyword', { defaultValue: '关键词' })}: {searchKeyword}
              </Tag>
            )}
            {providerIdFilter && (
              <Tag closable onClose={() => setProviderIdFilter(undefined)}>
                {t('provider', { defaultValue: '供应商' })}: {providerIdFilter}
              </Tag>
            )}
            {sourceFilter && (
              <Tag closable onClose={() => setSourceFilter(undefined)}>
                {t('source', { defaultValue: '来源' })}: {SOURCE_OPTIONS.find(s => s.value === sourceFilter)?.label}
              </Tag>
            )}
          </Space>
        </div>
      )}

      {/* 模型标签列表 */}
      <Spin spinning={isLoading}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, minHeight: 200 }}>
          {modelList.map((mm: ModelMetadata) => (
            <Tag key={mm.id} style={{ padding: '4px 12px', margin: 0 }}>
              <Space size={4}>
                <span>{mm.displayName}</span>
                <span style={{ fontSize: 11, color: token.colorTextSecondary }}>{mm.providerModelId}</span>
                <Tag color={mm.source === 'BUILTIN' ? 'blue' : mm.source === 'MODELS_DEV' ? 'green' : 'default'} style={{ fontSize: 10 }}>
                  {mm.source}
                </Tag>
              </Space>
            </Tag>
          ))}
        </div>
      </Spin>

      {modelList.length === 0 && !isLoading && (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Text type="secondary">{t('message.noData', { defaultValue: '暂无数据' })}</Text>
        </div>
      )}
    </div>
  );
}

/** 应用元数据弹窗 */
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
