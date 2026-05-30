import { useState } from 'react';
import {
  Table,
  Button,
  Input,
  Select,
  Space,
  Tag,
  Popconfirm,
  App,
  Switch,
} from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { PageHeader } from '@/components/ui/PageHeader';
import { useModels, useDeleteModel, useSetEnabledModel } from '@/services/query/useModels';
import ModelCreateModal from './ModelCreateModal';
import ModelEditDrawer from './ModelEditDrawer';
import type { Model } from '@/types/model';

export default function Models() {
  const { t } = useTranslation('models');
  const { message } = App.useApp();
  const { data: models, isLoading } = useModels();
  const deleteMutation = useDeleteModel();
  const setEnabledMutation = useSetEnabledModel();
  const [search, setSearch] = useState('');
  const [stateFilter, setStateFilter] = useState<string>();
  const [createOpen, setCreateOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<Model | null>(null);
  const [editOpen, setEditOpen] = useState(false);

  const filtered =
    models?.filter((m) => {
      const matchSearch = search
        ? (m.displayName || m.modelName)
            .toLowerCase()
            .includes(search.toLowerCase())
        : true;
      const matchState = stateFilter ? m.state === stateFilter : true;
      return matchSearch && matchState;
    }) ?? [];

  const handleDelete = async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success(t('deleted', { defaultValue: '模型已删除' }));
    } catch {
      message.error(t('deleteFailed', { defaultValue: '删除失败' }));
    }
  };

  const handleToggleState = async (id: number, checked: boolean) => {
    try {
      await setEnabledMutation.mutateAsync({ id, enabled: checked });
      message.success(
        checked
          ? t('enabled', { defaultValue: '模型已启用' })
          : t('disabled', { defaultValue: '模型已禁用' }),
      );
    } catch {
      message.error(t('stateToggleFailed', { defaultValue: '状态切换失败' }));
    }
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
    },
    {
      title: t('modelName', { defaultValue: '模型标识' }),
      dataIndex: 'modelName',
      key: 'modelName',
      width: 160,
    },
    {
      title: t('displayName', { defaultValue: '显示名称' }),
      dataIndex: 'displayName',
      key: 'displayName',
      width: 140,
      render: (val: string) => val || '-',
    },
    {
      title: t('modelFamily', { defaultValue: '模型族' }),
      dataIndex: 'modelFamily',
      key: 'modelFamily',
      width: 120,
      render: (val: string) => val || '-',
    },
    {
      title: t('provider', { defaultValue: '供应商' }),
      dataIndex: 'providerName',
      key: 'providerName',
      width: 120,
      render: (val: string) => val || '-',
    },
    {
      title: t('pricing', { defaultValue: '定价' }),
      key: 'pricing',
      width: 140,
      render: (_: unknown, record: Model) => {
        const p = record.pricing;
        if (!p?.inputPricePerMillion && !p?.outputPricePerMillion) return '-';
        const input = p.inputPricePerMillion != null ? `$${p.inputPricePerMillion}` : '-';
        const output = p.outputPricePerMillion != null ? `$${p.outputPricePerMillion}` : '-';
        return <span style={{ fontSize: 12 }}>{input} / {output}</span>;
      },
    },
    {
      title: t('contextWindow', { defaultValue: '上下文窗口' }),
      dataIndex: 'contextWindow',
      key: 'contextWindow',
      width: 120,
      render: (val: number) => (val ? `${(val / 1000).toFixed(0)}K` : '-'),
    },
    {
      title: t('capabilities', { defaultValue: '能力' }),
      key: 'capabilities',
      width: 200,
      render: (_: unknown, record: Model) => (
        <Space size={4} wrap>
          {record.capabilities
            ? Object.entries(record.capabilities)
                .filter(([, v]) => v)
                .map(([k]) => (
                  <Tag key={k} color="blue" style={{ fontSize: 11 }}>
                    {k}
                  </Tag>
                ))
            : '-'}
        </Space>
      ),
    },
    {
      title: t('state', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      width: 100,
      render: (state: string, record: Model) => (
        <Switch
          size="small"
          checked={state === 'ACTIVE'}
          onChange={(checked) => handleToggleState(record.id, checked)}
          checkedChildren={t('active', { defaultValue: '启用' })}
          unCheckedChildren={t('inactive', { defaultValue: '禁用' })}
        />
      ),
    },
    {
      title: t('createdAt', { defaultValue: '创建时间' }),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (val: string) =>
        val ? new Date(val).toLocaleString('zh-CN') : '-',
    },
    {
      title: t('actions', { defaultValue: '操作' }),
      key: 'actions',
      width: 120,
      render: (_: unknown, record: Model) => (
        <Space>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setEditingModel(record);
              setEditOpen(true);
            }}
          >
            {t('edit', { defaultValue: '编辑' })}
          </Button>
          <Popconfirm
            title={t('confirmDelete', { defaultValue: '确定要删除此模型吗？' })}
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              {t('delete', { defaultValue: '删除' })}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t('title', { defaultValue: '模型目录' })}
        subtitle={t('subtitle', { defaultValue: '浏览和管理全局模型注册表' })}
        actions={[
          {
            key: 'add',
            label: t('addModel', { defaultValue: '新增模型' }),
            type: 'primary',
            icon: <PlusOutlined />,
            onClick: () => setCreateOpen(true),
          },
        ]}
      />
      <div style={{ padding: '0 24px' }}>
        <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
          <Input.Search
            placeholder={t('search', { defaultValue: '搜索模型...' })}
            style={{ width: 280 }}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            allowClear
          />
          <Select
            placeholder={t('filterByState', { defaultValue: '筛选状态' })}
            style={{ width: 140 }}
            allowClear
            value={stateFilter}
            onChange={setStateFilter}
            options={[
              {
                value: 'ACTIVE',
                label: t('active', { defaultValue: '启用' }),
              },
              {
                value: 'INACTIVE',
                label: t('inactive', { defaultValue: '禁用' }),
              },
            ]}
          />
        </div>
        <Table
          dataSource={filtered}
          columns={columns}
          rowKey="id"
          loading={isLoading}
          pagination={{
            pageSize: 20,
            showSizeChanger: true,
            showTotal: (total) =>
              t('total', { defaultValue: `共 ${total} 条`, count: total }),
          }}
          locale={{
            emptyText: t('emptyList', {
              defaultValue: '暂无模型，请先接入供应商',
            }),
          }}
          size="middle"
        />
      </div>
      <ModelCreateModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
      />
      <ModelEditDrawer
        open={editOpen}
        model={editingModel}
        onClose={() => {
          setEditOpen(false);
          setEditingModel(null);
        }}
      />
    </div>
  );
}