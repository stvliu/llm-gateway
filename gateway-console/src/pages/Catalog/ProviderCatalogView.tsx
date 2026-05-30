import { useState } from 'react';
import { Card, Tag, Typography, Space, Button, Input, Spin, Select, Row, Col } from 'antd';
import { SearchOutlined, CloseOutlined, SyncOutlined, CloudDownloadOutlined, ArrowRightOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { App } from 'antd';
import { useProviderCatalogs, useSyncCatalog } from '@/services/query/useCatalog';
import type { ProviderType } from '@/types/catalog';

const { Text } = Typography;

interface ProviderCatalogViewProps {
  /** 选择供应商，进入套餐目录 */
  onSelectProvider: (code: string, name: string) => void;
  /** 级联物化供应商（含关联 Plans） */
  onCascadeMaterialize: (code: string, name: string) => void;
}

/** 供应商目录卡片视图 */
export default function ProviderCatalogView({ onSelectProvider, onCascadeMaterialize }: ProviderCatalogViewProps) {
  const { t } = useTranslation('catalog');
  const { message } = App.useApp();

  // 搜索与筛选
  const [keyword, setKeyword] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [providerType, setProviderType] = useState<ProviderType | undefined>();

  // 数据查询
  const { data: providers, isLoading } = useProviderCatalogs({
    keyword: searchKeyword || undefined,
    providerType,
  });
  const syncMutation = useSyncCatalog();

  /** 同步目录 */
  const handleSync = async (type: 'builtin' | 'models-dev') => {
    try {
      await syncMutation.mutateAsync(type);
      message.success(t('message.syncSuccess'));
    } catch {
      message.error(t('message.syncFailed'));
    }
  };

  /** 供应商类型标签颜色 */
  const providerTypeColor = (type: ProviderType) =>
    type === 'INTERNATIONAL' ? 'blue' : 'green';

  /** 供应商类型标签文本 */
  const providerTypeLabel = (type: ProviderType) =>
    t(`providerType.${type}`);

  const hasActiveFilters = !!searchKeyword || !!providerType;
  const providerList = providers ?? [];

  return (
    <div>
      {/* 搜索栏 + 同步按钮 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <Space wrap>
          <Input.Search
            placeholder={t('filter.keyword')}
            allowClear
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={(value) => setSearchKeyword(value)}
            style={{ width: 240 }}
            prefix={<SearchOutlined />}
          />
          <Select
            placeholder={t('filter.providerType')}
            allowClear
            value={providerType}
            onChange={(value) => setProviderType(value)}
            style={{ width: 140 }}
            options={[
              { value: 'INTERNATIONAL', label: t('providerType.INTERNATIONAL') },
              { value: 'DOMESTIC', label: t('providerType.DOMESTIC') },
            ]}
          />
          {hasActiveFilters && (
            <Button
              icon={<CloseOutlined />}
              onClick={() => {
                setKeyword('');
                setSearchKeyword('');
                setProviderType(undefined);
              }}
            >
              {t('filter.all')}
            </Button>
          )}
        </Space>
        <Space>
          <Button
            icon={<SyncOutlined />}
            onClick={() => handleSync('builtin')}
            loading={syncMutation.isPending}
          >
            {t('sync.builtin')}
          </Button>
          <Button
            icon={<SyncOutlined />}
            onClick={() => handleSync('models-dev')}
            loading={syncMutation.isPending}
          >
            {t('sync.modelsDev')}
          </Button>
        </Space>
      </div>

      {/* 供应商卡片网格 */}
      <Spin spinning={isLoading}>
        {providerList.length === 0 && !isLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <Text type="secondary">{t('message.noData', { defaultValue: '暂无数据' })}</Text>
          </div>
        ) : (
          <Row gutter={[16, 16]}>
            {providerList.map((provider) => (
              <Col key={provider.code} xs={24} sm={12} md={8} lg={6}>
                <Card
                  size="small"
                  hoverable
                  actions={[
                    <Button
                      key="materialize"
                      type="link"
                      icon={<CloudDownloadOutlined />}
                      disabled={provider.materialized}
                      onClick={(e) => {
                        e.stopPropagation();
                        if (provider.materialized) return;
                        onCascadeMaterialize(provider.code, provider.name);
                      }}
                    >
                      {provider.materialized ? t('provider.materialized') : t('materialize.cascade')}
                    </Button>,
                    <Button
                      key="view"
                      type="link"
                      icon={<ArrowRightOutlined />}
                      onClick={() => onSelectProvider(provider.code, provider.name)}
                    >
                      {t('tabs.plans')}
                    </Button>,
                  ]}
                >
                  <Card.Meta
                    title={
                      <Space>
                        <span>{provider.name}</span>
                        <Tag color={providerTypeColor(provider.providerType)} style={{ fontSize: 10 }}>
                          {providerTypeLabel(provider.providerType)}
                        </Tag>
                      </Space>
                    }
                    description={
                      <div>
                        <Text type="secondary" style={{ fontSize: 12 }}>{provider.code}</Text>
                        <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Space size={4}>
                            <Tag
                              color={provider.materialized ? 'success' : 'default'}
                              style={{ fontSize: 10 }}
                            >
                              {provider.materialized ? t('provider.materialized') : t('provider.notMaterialized')}
                            </Tag>
                          </Space>
                          <Tag
                            color={provider.source === 'BUILTIN' ? 'blue' : provider.source === 'MODELS_DEV' ? 'green' : 'default'}
                            style={{ fontSize: 10 }}
                          >
                            {t(`source.${provider.source}`)}
                          </Tag>
                        </div>
                      </div>
                    }
                  />
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </Spin>
    </div>
  );
}
