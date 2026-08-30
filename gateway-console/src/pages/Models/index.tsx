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
  Tooltip,
  Alert,
} from 'antd';
import { PlusOutlined, DeleteOutlined, EditOutlined, CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { Card } from 'antd';
import { useModels, useDeleteModel, useSetEnabledModel } from '@/services/query/useModels';
import ModelCreateModal from './ModelCreateModal';
import ModelEditDrawer from './ModelEditDrawer';
import CopyModelModal from './CopyModelModal';
import type { Model } from '@/types/model';

/** 模型来源标签颜色配置 */
const SOURCE_COLOR: Record<string, string> = {
  MODELS_DEV: 'blue',
  BUILTIN: 'default',
  MANUAL: 'orange',
};

export default function Models() {
  const { t } = useTranslation('models');
  const { message } = App.useApp();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [searchText, setSearchText] = useState('');
  const [search, setSearch] = useState('');
  const [stateFilter, setStateFilter] = useState<string>();
  const deleteMutation = useDeleteModel();
  const setEnabledMutation = useSetEnabledModel();
  const [createOpen, setCreateOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<Model | null>(null);
  const [editOpen, setEditOpen] = useState(false);
  // 复制对话框的源模型（非空时打开）
  const [copySource, setCopySource] = useState<Model | null>(null);

  const params = {
    page,
    limit: pageSize,
    keyword: search || undefined,
    state: stateFilter || undefined,
  };
  const { data, isLoading, isError, refetch } = useModels(params);

  const handleSearch = (value: string) => {
    setSearch(value);
    setPage(1);
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setSearchText(value);
    if (!value) {
      // 清空时立即触发搜索重置
      setSearch('');
      setPage(1);
    }
  };

  const handleStateFilter = (value?: string) => {
    setStateFilter(value);
    setPage(1);
  };

  const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
    if (pagination.current) setPage(pagination.current);
    if (pagination.pageSize) setPageSize(pagination.pageSize);
  };

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
      title: t('modelName', { defaultValue: '模型标识' }),
      dataIndex: 'modelName',
      key: 'modelName',
      width: 190,
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
      title: t('source', { defaultValue: '来源' }),
      dataIndex: 'source',
      key: 'source',
      width: 100,
      render: (val: string) =>
        val ? (
          <Tag color={SOURCE_COLOR[val] ?? 'default'} style={{ fontSize: 11 }}>
            {val}
          </Tag>
        ) : (
          '-'
        ),
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
      width: 170,
      render: (_: unknown, record: Model) => {
        const caps = record.capabilities
          ? Object.entries(record.capabilities)
              .filter(([, v]) => v)
              .map(([k]) => k)
          : [];
        if (caps.length === 0) return '-';
        // 最多展示 2 个能力标签，其余折叠为 +N，保持列宽紧凑
        const shown = caps.slice(0, 2);
        return (
          <Space size={4}>
            {shown.map((k) => (
              <Tag key={k} color="blue" style={{ fontSize: 11 }}>
                {k}
              </Tag>
            ))}
            {caps.length > 2 && (
              <Tooltip title={caps.slice(2).join(', ')}>
                <Tag style={{ fontSize: 11 }}>+{caps.length - 2}</Tag>
              </Tooltip>
            )}
          </Space>
        );
      },
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
      title: t('actions', { defaultValue: '操作' }),
      key: 'actions',
      width: 150,
      render: (_: unknown, record: Model) => (
        <Space>
          <Tooltip title={t('edit', { defaultValue: '编辑' })}>
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => {
                setEditingModel(record);
                setEditOpen(true);
              }}
            />
          </Tooltip>
          {/* 同家族模型复制：以当前行模型为源打开复制对话框 */}
          <Tooltip title={t('copy', { defaultValue: '复制' })}>
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => setCopySource(record)}
            />
          </Tooltip>
          <Popconfirm
            title={t('confirmDelete', { defaultValue: '确定要删除此模型吗？' })}
            onConfirm={() => handleDelete(record.id)}
          >
            <Tooltip title={t('delete', { defaultValue: '删除' })}>
              <Button type="text" size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card
        title={t('title', { defaultValue: '模型目录' })}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
            {t('addModel', { defaultValue: '新增模型' })}
          </Button>
        }
      >
        <div>
        <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
          <Input.Search
            placeholder={t('search', { defaultValue: '搜索模型...' })}
            style={{ width: 280 }}
            value={searchText}
            onSearch={handleSearch}
            onChange={handleSearchChange}
            allowClear
          />
          <Select
            placeholder={t('filterByState', { defaultValue: '筛选状态' })}
            style={{ width: 140 }}
            allowClear
            value={stateFilter}
            onChange={handleStateFilter}
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
        {isError && (
          <Alert
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
            message={t('loadFailed', { ns: 'common' })}
            description={t('loadFailedHint', { ns: 'common' })}
            action={
              <Button size="small" onClick={() => refetch()}>
                {t('retry', { ns: 'common' })}
              </Button>
            }
          />
        )}
        <Table
          dataSource={data?.items ?? []}
          columns={columns}
          rowKey="id"
          loading={isLoading}
          pagination={{
            current: page,
            pageSize,
            total: data?.pagination?.total ?? 0,
            showSizeChanger: true,
            showTotal: (total) =>
              t('total', { defaultValue: `共 ${total} 条`, count: total }),
            onChange: (current, size) => handleTableChange({ current, pageSize: size }),
          }}
          locale={{
            emptyText: t('emptyList', {
              defaultValue: '暂无模型，请先接入供应商',
            }),
          }}
          size="middle"
        />
      </div>
      </Card>

      <ModelCreateModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
      />
      {/* 行内复制对话框：复制成功后刷新模型列表 */}
      <CopyModelModal
        open={!!copySource}
        source={copySource}
        onClose={() => setCopySource(null)}
        onCopied={() => refetch()}
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