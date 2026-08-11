/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useMemo, useState } from 'react';
import { Tag, Space, Button, Typography, Spin, Table, Input, Row, Col, Card, Tooltip } from 'antd';
import { PlusOutlined, SyncOutlined, CloudDownloadOutlined, ArrowRightOutlined, SearchOutlined, CloseOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { App } from 'antd';
import { ProviderIcon } from '@/components/ui/ProviderIcon';
import {
  useProviderCatalogs,
  usePlanCatalogs,
  useProvisionBatch,
  useSyncCatalog,
} from '@/services/query/useCatalog';
import type { ProviderCatalog, PlanCatalog } from '@/types/catalog';

const { Text } = Typography;

/** 计费模式标签配置 */
const BILLING_MODE_CONFIG: Record<string, { color: string }> = {
  pay_as_you_go: { color: 'green' },
  subscription: { color: 'purple' },
  package: { color: 'orange' },
};

interface PlanCatalogViewProps {
  /** 供应商编码（可选，用于筛选套餐） */
  providerCode?: string;
  /** 选择供应商 */
  onSelectProvider?: (code: string, name: string) => void;
  /** 选择套餐 */
  onSelectPlan?: (planCode: string, planName: string) => void;
  /** 快速创建渠道 */
  onQuickCreate?: (planCode: string, planName: string) => void;
}

/** 套餐目录视图 */
export default function PlanCatalogView({
  providerCode,
  onSelectProvider,
  onSelectPlan,
  onQuickCreate,
}: PlanCatalogViewProps) {
  const { t } = useTranslation('catalog');
  const { message } = App.useApp();

  // 搜索状态
  const [keyword, setKeyword] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');

  // 数据查询
  const { data: providers, isLoading: providersLoading } = useProviderCatalogs(searchKeyword);
  const { data: plans, isLoading: plansLoading } = usePlanCatalogs(providerCode);

  // 开通操作
  const provisionBatchMutation = useProvisionBatch();
  const syncMutation = useSyncCatalog();

  // 是否显示供应商列表（未选择供应商时）
  const showProviders = !providerCode;
  const dataList = showProviders ? (providers ?? []) : (plans ?? []);

  /** 同步目录 */
  const handleSync = async () => {
    try {
      await syncMutation.mutateAsync();
      message.success(t('message.syncSuccess'));
    } catch {
      message.error(t('message.syncFailed'));
    }
  };

  /** 开通供应商 */
  const handleProvisionProvider = async (code: string) => {
    try {
      const result = await provisionBatchMutation.mutateAsync({ providerCode: code });
      const summary = t('provision.resultSummary', {
        success: result.successCount,
        skipped: result.skippedCount,
        failed: result.failedCount,
      });
      message.success(summary);
    } catch {
      message.error(t('message.provisionFailed'));
    }
  };

  const isLoading = showProviders ? providersLoading : plansLoading;
  const hasActiveFilters = !!searchKeyword;

  /** 供应商卡片视图 */
  const renderProviders = () => (
    <Spin spinning={isLoading}>
      {dataList.length === 0 && !isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Text type="secondary">{t('message.noData', { defaultValue: '暂无数据' })}</Text>
        </div>
      ) : (
        <Row gutter={[16, 16]}>
          {(dataList as ProviderCatalog[]).map((provider) => (
            <Col key={provider.code} xs={24} sm={12} md={8} lg={6}>
              <Card
                size="small"
                hoverable
                actions={[
                  <Tooltip key="provision" title={provider.materialized ? t('provider.materialized') : t('provision.provider')}>
                    <Button
                      type="text"
                      icon={<CloudDownloadOutlined />}
                      disabled={provider.materialized}
                      onClick={(e) => {
                        e.stopPropagation();
                        if (provider.materialized) return;
                        handleProvisionProvider(provider.code);
                      }}
                    />
                  </Tooltip>,
                  onSelectProvider && (
                    <Tooltip key="view" title={t('tabs.plans')}>
                      <Button
                        type="text"
                        icon={<ArrowRightOutlined />}
                        onClick={() => onSelectProvider(provider.code, provider.name)}
                      />
                    </Tooltip>
                  ),
                ]}
              >
                <Card.Meta
                  avatar={<ProviderIcon providerId={provider.code} size={40} />}
                  title={
                    <Space>
                      <span>{provider.name}</span>
                      <Tag color={provider.materialized ? 'success' : 'default'} style={{ fontSize: 10 }}>
                        {provider.materialized ? t('provider.materialized') : t('provider.notMaterialized')}
                      </Tag>
                    </Space>
                  }
                  description={<Text type="secondary" style={{ fontSize: 12 }}>{provider.code}</Text>}
                />
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </Spin>
  );

  /** 套餐表格列定义 */
  const planColumns = useMemo(() => [
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
      title: '',
      key: 'actions',
      width: 200,
      render: (_: unknown, record: PlanCatalog) => (
        <Space size="small">
          {onQuickCreate && (
            <Tooltip title={t('quickCreate', { defaultValue: '创建渠道' })}>
              <Button
                type="primary"
                size="small"
                icon={<PlusOutlined />}
                onClick={(e) => {
                  e.stopPropagation();
                  onQuickCreate(record.planCode, record.planName);
                }}
              />
            </Tooltip>
          )}
          {onSelectPlan && (
            <Tooltip title={t('plan.detail')}>
              <Button
                type="text"
                size="small"
                icon={<ArrowRightOutlined />}
                onClick={() => onSelectPlan(record.planCode, record.planName)}
              />
            </Tooltip>
          )}
        </Space>
      ),
    },
  ], [t, onQuickCreate, onSelectPlan]);

  /** 套餐表格视图 */
  const renderPlans = () => (
    <Spin spinning={isLoading}>
      {(dataList as PlanCatalog[]).length === 0 && !isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Text type="secondary">{t('message.noData', { defaultValue: '暂无数据' })}</Text>
        </div>
      ) : (
        <Table
          dataSource={dataList as PlanCatalog[]}
          columns={planColumns}
          rowKey="planCode"
          size="small"
          pagination={false}
        />
      )}
    </Spin>
  );

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
          {hasActiveFilters && (
            <Tooltip title={t('filter.all')}>
              <Button
                icon={<CloseOutlined />}
                onClick={() => {
                  setKeyword('');
                  setSearchKeyword('');
                }}
              />
            </Tooltip>
          )}
        </Space>
        {showProviders && (
          <Tooltip title={t('sync.builtin')}>
            <Button
              icon={<SyncOutlined />}
              onClick={handleSync}
              loading={syncMutation.isPending}
            />
          </Tooltip>
        )}
      </div>

      {/* 供应商列表或套餐列表 */}
      {showProviders ? renderProviders() : renderPlans()}
    </div>
  );
}