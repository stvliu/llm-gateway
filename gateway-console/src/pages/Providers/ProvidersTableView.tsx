import { useCallback } from 'react';
import { Table, Tag, Space, Button, Tooltip, Empty } from 'antd';
import { EyeOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { ProviderIcon } from '@/components/ui';
import type { Provider } from '@/types/provider';

interface Props {
  providers: Provider[];
  onSelect: (provider: Provider) => void;
  onEdit: (provider: Provider) => void;
  onDelete: (provider: Provider) => void;
  onViewChannels?: (provider: Provider) => void;
}

/**
 * 供应商表格视图
 * 使用 Ant Design Table 渲染供应商列表，支持行点击选择和操作按钮
 */
export default function ProvidersTableView({ providers, onSelect, onEdit, onDelete }: Props) {
  const { t } = useTranslation('providers');
  const { t: tc } = useTranslation('common');
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);

  /** 状态配置：颜色和文本映射 */
  const stateConfig: Record<string, { color: string; label: string }> = {
    ACTIVE: { color: 'success', label: t('state.active', { defaultValue: '启用' }) },
    INACTIVE: { color: 'warning', label: t('state.inactive', { defaultValue: '停用' }) },
  };

  /** 格式化日期显示 */
  const formatDate = useCallback((dateStr: string) => {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }, []);

  /** 列定义 */
  const columns: ColumnsType<Provider> = [
    {
      key: 'brand',
      title: t('provider.providerId', { defaultValue: '品牌标识' }),
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <ProviderIcon providerId={record.providerId} size={20} />
          <span>{record.providerId || '-'}</span>
        </Space>
      ),
    },
    {
      key: 'providerName',
      title: t('name', { defaultValue: '供应商名称' }),
      dataIndex: 'providerName',
      render: (value: string) => <span style={{ fontWeight: 500 }}>{value}</span>,
    },
    {
      key: 'state',
      title: t('state', { defaultValue: '状态' }),
      dataIndex: 'state',
      width: 100,
      render: (value: string) => {
        const config = stateConfig[value?.toUpperCase()] || { color: 'default', label: value || 'Unknown' };
        return <Tag color={config.color}>{config.label}</Tag>;
      },
    },
    {
      key: 'priority',
      title: t('priority', { defaultValue: '优先级' }),
      dataIndex: 'priority',
      width: 80,
      align: 'center',
      render: (value: number) => value ?? '-',
    },
    {
      key: 'createdAt',
      title: t('detail.createdAt', { defaultValue: '创建时间' }),
      dataIndex: 'createdAt',
      width: 180,
      render: (value: string) => formatDate(value),
    },
    {
      key: 'actions',
      title: tc('actions.label'),
      width: canWrite ? 120 : 60,
      render: (_, record) => (
        <Space className="table-action-cell" size="small">
          <Tooltip title={tc('actions.view', { defaultValue: '查看' })}>
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                onSelect(record);
              }}
            />
          </Tooltip>
          {canWrite && (
            <>
              <Tooltip title={tc('actions.edit', { defaultValue: '编辑' })}>
                <Button
                  type="text"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={(e) => {
                    e.stopPropagation();
                    onEdit(record);
                  }}
                />
              </Tooltip>
              <Tooltip title={tc('actions.delete', { defaultValue: '删除' })}>
                <Button
                  type="text"
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                  onClick={(e) => {
                    e.stopPropagation();
                    onDelete(record);
                  }}
                />
              </Tooltip>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table<Provider>
      dataSource={providers}
      columns={columns}
      rowKey="id"
      size="middle"
      pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total) => tc('table.total', { count: total }) }}
      onRow={(record) => ({
        onClick: () => onSelect(record),
        style: { cursor: 'pointer' },
      })}
      locale={{
        emptyText: (
          <Empty
            description={t('empty.noProvider', { defaultValue: '暂无供应商' })}
          />
        ),
      }}
    />
  );
}
