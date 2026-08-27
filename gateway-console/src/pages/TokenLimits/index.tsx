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
import { useState, useMemo } from 'react';
import {
  Card,
  Table,
  Button,
  Input,
  Modal,
  Form,
  Select,
  InputNumber,
  Space,
  Tag,
  Popconfirm,
  App,
  Tooltip,
  Alert,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import {
  useTokenLimits,
  useCreateTokenLimit,
  useUpdateTokenLimit,
  useDeleteTokenLimit,
  useResetTokenLimitUsage,
} from '@/services/query/useTokenLimits';
import { useUsers } from '@/services/query/useUsers';
import { useModels } from '@/services/query/useModels';
import type {
  TokenLimit,
  TokenLimitCreateRequest,
  TokenLimitUpdateRequest,
  TokenPeriodType,
  TokenExceededAction,
} from '@/types/tokenLimit';

/**
 * Token 限额管理页
 *
 * <p>管理员为各用户/模型配置 Token 用量限额（额度/周期/超限动作），
 * 支持重置已使用量。仅 ADMIN 可见。</p>
 */
export default function TokenLimits() {
  const { t } = useTranslation('tokenLimits');
  const { message } = App.useApp();
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [search, setSearch] = useState('');

  const params = useMemo(
    () => ({ page, limit, keyword: search || undefined }),
    [page, limit, search],
  );
  const { data, isLoading, isError, refetch } = useTokenLimits(params);

  const createMutation = useCreateTokenLimit();
  const updateMutation = useUpdateTokenLimit();
  const deleteMutation = useDeleteTokenLimit();
  const resetMutation = useResetTokenLimitUsage();

  const { data: usersData } = useUsers({ page: 1, size: 200 });
  const { data: modelsData } = useModels({ limit: 1000 });

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<TokenLimit | null>(null);
  const [form] = Form.useForm();

  const handleAdd = () => {
    setEditing(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: TokenLimit) => {
    setEditing(record);
    form.setFieldsValue({
      userId: record.userId,
      modelId: record.modelId,
      maxTokens: Number(record.maxTokens),
      periodType: record.periodType,
      exceededAction: record.exceededAction,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editing) {
        const data: TokenLimitUpdateRequest = {
          maxTokens: values.maxTokens,
          periodType: values.periodType,
          exceededAction: values.exceededAction,
        };
        await updateMutation.mutateAsync({ id: editing.id, data });
        message.success(t('updated', { defaultValue: '限额已更新' }));
      } else {
        const data: TokenLimitCreateRequest = {
          userId: values.userId,
          modelId: values.modelId,
          maxTokens: values.maxTokens,
          periodType: values.periodType,
          exceededAction: values.exceededAction,
        };
        await createMutation.mutateAsync(data);
        message.success(t('created', { defaultValue: '限额已创建' }));
      }
      setModalOpen(false);
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      message.error(editing
        ? t('updateFailed', { defaultValue: '更新失败' })
        : t('createFailed', { defaultValue: '创建失败' }));
    }
  };

  const handleReset = async (id: number) => {
    try {
      await resetMutation.mutateAsync(id);
      message.success(t('resetSuccess', { defaultValue: '已重置使用量' }));
    } catch {
      message.error(t('resetFailed', { defaultValue: '重置失败' }));
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success(t('deleted', { defaultValue: '限额已删除' }));
    } catch {
      message.error(t('deleteFailed', { defaultValue: '删除失败' }));
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    {
      title: t('user', { defaultValue: '用户' }),
      dataIndex: 'username',
      key: 'user',
      width: 140,
      render: (val: string | undefined, record: TokenLimit) => val || `用户 ${record.userId}`,
    },
    {
      title: t('model', { defaultValue: '模型' }),
      dataIndex: 'modelName',
      key: 'model',
      width: 160,
      render: (val: string | undefined) => val || '-',
    },
    {
      title: t('limitType', { defaultValue: '类型' }),
      dataIndex: 'limitType',
      key: 'limitType',
      width: 100,
      render: (val: string) => (val === 'USER_CUSTOM' ? '自定义' : '系统'),
    },
    {
      title: t('maxTokens', { defaultValue: '额度' }),
      dataIndex: 'maxTokens',
      key: 'maxTokens',
      width: 110,
      render: (val: number) => val?.toLocaleString() ?? '-',
    },
    {
      title: t('usedTokens', { defaultValue: '已用' }),
      dataIndex: 'usedTokens',
      key: 'usedTokens',
      width: 110,
      render: (val: number) => val?.toLocaleString() ?? '-',
    },
    {
      title: t('remainingTokens', { defaultValue: '剩余' }),
      dataIndex: 'remainingTokens',
      key: 'remainingTokens',
      width: 110,
      render: (val: number) => val?.toLocaleString() ?? '-',
    },
    {
      title: t('periodType', { defaultValue: '周期' }),
      dataIndex: 'periodType',
      key: 'periodType',
      width: 90,
      render: (val: TokenPeriodType) => t(`period.${val}`, { defaultValue: val }),
    },
    {
      title: t('exceededAction', { defaultValue: '超限动作' }),
      dataIndex: 'exceededAction',
      key: 'exceededAction',
      width: 90,
      render: (val: TokenExceededAction) => t(`action.${val}`, { defaultValue: val }),
    },
    {
      title: t('enabled', { defaultValue: '启用' }),
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (enabled: boolean) => (enabled ? <Tag color="green">✓</Tag> : <Tag>✗</Tag>),
    },
    {
      title: t('actions', { defaultValue: '操作' }),
      key: 'actions',
      width: 130,
      render: (_: unknown, record: TokenLimit) => (
        <Space size={4}>
          <Tooltip title={t('edit', { defaultValue: '编辑' })}>
            <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          </Tooltip>
          <Tooltip title={t('resetUsage', { defaultValue: '重置用量' })}>
            <Popconfirm
              title={t('confirmReset', { defaultValue: '确定重置此限额的已使用量？' })}
              onConfirm={() => handleReset(record.id)}
            >
              <Button type="text" size="small" icon={<ReloadOutlined />} />
            </Popconfirm>
          </Tooltip>
          <Popconfirm
            title={t('confirmDelete', { defaultValue: '确定删除此限额？' })}
            okType="danger"
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
        title={t('title', { defaultValue: 'Token 限额管理' })}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('addLimit', { defaultValue: '新增限额' })}
          </Button>
        }
      >
        <div style={{ marginBottom: 16, display: 'flex', gap: 12 }}>
          <Input.Search
            placeholder={t('searchPlaceholder', { defaultValue: '搜索用户名...' })}
            style={{ width: 260 }}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={(v) => {
              setSearch(v);
              setPage(1);
            }}
            allowClear
          />
        </div>

        {isError && (
          <Alert
            type="error"
            showIcon
            style={{ marginBottom: 16 }}
            message={t('loadFailed', { defaultValue: '加载失败' })}
            description={t('loadFailedHint', { defaultValue: '请检查网络或稍后重试' })}
            action={
              <Button size="small" onClick={() => refetch()}>
                {t('retry', { defaultValue: '重试' })}
              </Button>
            }
          />
        )}

        <Table
          dataSource={data?.items ?? []}
          columns={columns}
          rowKey="id"
          size="middle"
          loading={isLoading}
          pagination={{
            current: page,
            pageSize: limit,
            total: data?.pagination?.total ?? 0,
            showSizeChanger: true,
            showTotal: (total) => t('total', { defaultValue: '共 {{count}} 条', count: total }),
            onChange: (current, size) => {
              setPage(current);
              setLimit(size);
            },
          }}
          locale={{ emptyText: t('noLimits', { defaultValue: '暂无 Token 限额' }) }}
        />
      </Card>

      <Modal
        title={editing ? t('editTitle', { defaultValue: '编辑限额' }) : t('addTitle', { defaultValue: '新增限额' })}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
        destroyOnClose
        width={520}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="userId"
            label={t('user', { defaultValue: '用户' })}
            rules={[{ required: true, message: t('userRequired', { defaultValue: '请选择用户' }) }]}
          >
            <Select
              showSearch
              disabled={!!editing}
              placeholder={t('userPlaceholder', { defaultValue: '搜索并选择用户' })}
              filterOption={(input, option) =>
                (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={(usersData?.items ?? []).map((u) => ({
                label: `${u.username} (${u.id})`,
                value: u.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="modelId" label={t('model', { defaultValue: '模型' })}>
            <Select
              showSearch
              allowClear
              placeholder={t('modelPlaceholder', { defaultValue: '选择模型（可选）' })}
              filterOption={(input, option) =>
                (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={(modelsData?.items ?? []).map((m) => ({
                label: m.displayName || m.modelName,
                value: m.id,
              }))}
            />
          </Form.Item>
          <Form.Item
            name="maxTokens"
            label={t('maxTokens', { defaultValue: '额度（Token 数）' })}
            rules={[{ required: true, message: t('maxTokensRequired', { defaultValue: '请输入额度' }) }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="periodType" label={t('periodType', { defaultValue: '周期' })} initialValue="MONTHLY">
            <Select
              options={[
                { value: 'DAILY', label: t('period.DAILY', { defaultValue: '每日' }) },
                { value: 'WEEKLY', label: t('period.WEEKLY', { defaultValue: '每周' }) },
                { value: 'MONTHLY', label: t('period.MONTHLY', { defaultValue: '每月' }) },
                { value: 'TOTAL', label: t('period.TOTAL', { defaultValue: '累计' }) },
              ]}
            />
          </Form.Item>
          <Form.Item name="exceededAction" label={t('exceededAction', { defaultValue: '超限动作' })} initialValue="REJECT">
            <Select
              options={[
                { value: 'REJECT', label: t('action.REJECT', { defaultValue: '拒绝' }) },
                { value: 'DOWNGRADE', label: t('action.DOWNGRADE', { defaultValue: '降级' }) },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
