import { useState, useMemo, useCallback } from 'react';
import { Tag, Space, Button, Tooltip, Segmented, theme } from 'antd';
import {
  PlusOutlined,
  ReloadOutlined,
  DeleteOutlined,
  EyeOutlined,
  LinkOutlined,
  AppstoreOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { PageHeader, EntityTable, ProviderIcon } from '@/components/ui';
import type { ColumnConfig } from '@/components/ui';
import { FilterPanel, FilterTags } from '@/components/common';
import { useConfirm } from '@/hooks/useConfirm';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useProviders, useDeleteProvider } from '@/services/query';
import type { Provider } from '@/types/provider';

interface ProvidersTableViewProps {
  viewMode?: 'card' | 'table';
  onViewModeChange?: (mode: 'card' | 'table') => void;
  onAddProvider?: () => void;
  onProviderSelect?: (provider: Provider) => void;
}

/**
 * 供应商表格视图
 * Header 与卡片视图一致，操作复用 ProviderManagementDrawer
 */
export function ProvidersTableView({ viewMode = 'table', onViewModeChange, onAddProvider, onProviderSelect }: ProvidersTableViewProps = {}) {
  const { t } = useTranslation('providers');
  const { t: tc } = useTranslation('common');
  const { token } = theme.useToken();
  const { confirm } = useConfirm();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);

  // 分页状态
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // 数据查询（动态分页）
  const { data: providersData, isLoading: providersLoading, refetch } = useProviders({ page, limit: pageSize });
  const providers = providersData?.items || [];
  const pagination = providersData?.pagination;

  // Mutations
  const deleteProviderMutation = useDeleteProvider();

  // 过滤器状态
  const [filterValues, setFilterValues] = useState<Record<string, string>>({});

  // 前端过滤
  const filteredProviders = useMemo(() => {
    const hasFilters = Object.values(filterValues).some((v) => v && v !== 'all');
    if (!hasFilters) return providers;

    return providers.filter((provider) => {
      if (filterValues.providerName && filterValues.providerName !== 'all') {
        if (!provider.providerName.toLowerCase().includes(filterValues.providerName.toLowerCase())) {
          return false;
        }
      }
      if (filterValues.state && filterValues.state !== 'all') {
        if (provider.state !== filterValues.state) {
          return false;
        }
      }
      return true;
    });
  }, [providers, filterValues]);

  // 列配置
  const columns: ColumnConfig[] = useMemo(() => [
    {
      key: 'providerName',
      title: t('name', { defaultValue: '供应商名称' }),
      dataIndex: 'providerName',
      sortable: true,
      render: (value: unknown, record: unknown) => {
        const provider = record as Provider;
        return (
          <Space>
            <ProviderIcon providerId={provider.providerId} size={20} />
            <span style={{ fontWeight: 500 }}>{value as string}</span>
          </Space>
        );
      },
    },
    {
      key: 'providerId',
      title: t('provider.providerId', { defaultValue: '品牌标识' }),
      dataIndex: 'providerId',
      width: 100,
      render: (value: unknown) => value ? <Tag>{value as string}</Tag> : '-',
    },
    {
      key: 'websiteUrl',
      title: t('provider.websiteUrl', { defaultValue: '官网地址' }),
      dataIndex: 'websiteUrl',
      render: (value: unknown) => {
        const url = value as string;
        if (!url) return '-';
        return (
          <Tooltip title={url}>
            <a href={url} target="_blank" rel="noopener noreferrer" onClick={(e) => e.stopPropagation()}>
              <Space size={4}>
                <LinkOutlined />
                <span style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'inline-block' }}>
                  {url.replace('https://', '').replace('http://', '')}
                </span>
              </Space>
            </a>
          </Tooltip>
        );
      },
    },
    {
      key: 'keyCount',
      title: t('keyCount', { defaultValue: 'API Keys' }),
      width: 100,
      render: (_: unknown, record: unknown) => {
        const provider = record as Provider;
        const stats = provider.keyStats;
        return (
          <Space size={4}>
            <Tag color={stats?.activeCount ? 'green' : 'default'}>
              {stats?.activeCount ?? 0}
            </Tag>
            <span style={{ color: token.colorTextSecondary, fontSize: 12 }}>/ {stats?.totalCount ?? 0}</span>
          </Space>
        );
      },
    },
    ...(canWrite ? [{
      key: 'actions',
      title: tc('actions.label'),
      width: 100,
      render: (_: unknown, record: unknown) => {
        const provider = record as Provider;
        return (
          <Space className="table-action-cell">
            <Tooltip title={tc('actions.view', { defaultValue: '查看' })}>
              <Button
                type="text"
                size="small"
                icon={<EyeOutlined />}
                onClick={(e) => {
                  e.stopPropagation();
                  onProviderSelect?.(provider);
                }}
              />
            </Tooltip>
            <Tooltip title={tc('actions.delete')}>
              <Button
                type="text"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={(e) => {
                  e.stopPropagation();
                  handleDelete(provider);
                }}
              />
            </Tooltip>
          </Space>
        );
      },
    }] : [{
      key: 'actions',
      title: tc('actions.label'),
      width: 80,
      render: (_: unknown, record: unknown) => {
        const provider = record as Provider;
        return (
          <Space className="table-action-cell">
            <Tooltip title={tc('actions.view', { defaultValue: '查看' })}>
              <Button
                type="text"
                size="small"
                icon={<EyeOutlined />}
                onClick={(e) => {
                  e.stopPropagation();
                  onProviderSelect?.(provider);
                }}
              />
            </Tooltip>
          </Space>
        );
      },
    }]),
  ], [t, tc, onProviderSelect, canWrite]);

  // 行点击处理
  const handleRowClick = useCallback((record: unknown) => {
    const provider = record as Provider;
    onProviderSelect?.(provider);
  }, [onProviderSelect]);

  // 删除处理
  const handleDelete = useCallback((provider: Provider) => {
    confirm({
      type: 'danger',
      entityName: provider.providerName,
      onConfirm: () => deleteProviderMutation.mutateAsync(provider.id),
    });
  }, [confirm, deleteProviderMutation]);

  // 过滤处理
  const handleFilterChange = useCallback((values: Record<string, string>) => {
    setFilterValues(values);
  }, []);

  const handleFilterReset = useCallback(() => {
    setFilterValues({});
  }, []);

  // 过滤字段配置
  const filterFields = useMemo(() => [
    {
      name: 'state',
      label: t('state', { defaultValue: '状态' }),
      options: [
        { value: 'all', label: t('filter.all', { defaultValue: '全部' }) },
        { value: 'ACTIVE', label: tc('state.active') },
        { value: 'DISABLED', label: tc('state.disabled') },
      ],
    },
  ], [t, tc]);

  // 过滤标签
  const filterTags = useMemo(() => {
    const tags: Array<{ key: string; label: string; value: string }> = [];
    if (filterValues.state && filterValues.state !== 'all') {
      const option = filterFields[0].options.find((o) => o.value === filterValues.state);
      tags.push({
        key: 'state',
        label: filterFields[0].label,
        value: option?.label || filterValues.state,
      });
    }
    return tags;
  }, [filterValues, filterFields]);

  // 页面操作按钮（与卡片视图一致）
  const pageActions = useMemo(() => [
    {
      key: 'add',
      label: tc('actions.add'),
      type: 'primary' as const,
      icon: <PlusOutlined />,
      onClick: onAddProvider || (() => {}),
      danger: false,
      disabled: false,
    },
    {
      key: 'refresh',
      label: tc('actions.refresh'),
      icon: <ReloadOutlined />,
      onClick: () => refetch(),
      loading: providersLoading,
      danger: false,
      disabled: false,
    },
  ], [tc, onAddProvider, refetch, providersLoading]);

  // 视图切换组件（与卡片视图一致）
  const viewModeSwitcher = useMemo(() => onViewModeChange ? (
    <Segmented
      value={viewMode}
      onChange={(value) => onViewModeChange(value as 'card' | 'table')}
      options={[
        {
          value: 'card',
          icon: <AppstoreOutlined />,
          label: t('viewMode.card', { defaultValue: '卡片' }),
        },
        {
          value: 'table',
          icon: <UnorderedListOutlined />,
          label: t('viewMode.table', { defaultValue: '表格' }),
        },
      ]}
    />
  ) : undefined, [viewMode, onViewModeChange, t]);

  return (
    <div className="h-full flex flex-col">
      {/* 页面标题（与卡片视图布局一致） */}
      <PageHeader
        title={t('title', { defaultValue: '供应商管理' })}
        actions={
          <Space>
            {viewModeSwitcher}
            {pageActions.map((action) => (
              <Button
                key={action.key}
                type={action.type || 'default'}
                danger={action.danger}
                icon={action.icon}
                onClick={action.onClick}
                loading={action.loading}
                disabled={action.disabled}
              >
                {action.label}
              </Button>
            ))}
          </Space>
        }
        extra={
          <FilterPanel
            fields={filterFields}
            values={filterValues}
            onChange={handleFilterChange}
            onReset={handleFilterReset}
            title={t('filter.providerFilter', { defaultValue: '供应商筛选器' })}
          />
        }
      />

      {/* 过滤标签 */}
      {filterTags.length > 0 && (
        <FilterTags
          filters={filterTags}
          onRemove={(key) => setFilterValues((prev) => ({ ...prev, [key]: 'all' }))}
          onClearAll={handleFilterReset}
        />
      )}

      {/* 表格内容 */}
      <div className="flex-1 overflow-auto p-4">
        <EntityTable
          dataSource={filteredProviders}
          columns={columns}
          rowKey="id"
          loading={providersLoading}
          onRowClick={handleRowClick}
          showColumnConfig
          showRefresh
          onRefresh={() => refetch()}
          pagination={{
            current: pagination?.page ?? page,
            pageSize: pagination?.limit ?? pageSize,
            total: pagination?.total ?? 0,
            onChange: (newPage, newPageSize) => {
              setPage(newPage);
              if (newPageSize !== pageSize) {
                setPageSize(newPageSize);
                setPage(1);
              }
            },
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => tc('table.total', { count: total }),
          }}
        />
      </div>
    </div>
  );
}