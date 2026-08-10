/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useState, useCallback, useMemo, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Table, Button, Popconfirm, App, Input, Typography, Modal, Form, Select, Card } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAllUserApiKeys, useDeleteUserApiKey, useCreateUserApiKey } from '@/services/query/useUserApiKeys';
import { useUsers } from '@/services/query/useUsers';
import { useApplications } from '@/services/query/useApplications';
import { MaskedKeyDisplay } from '@/components/MaskedKeyDisplay';
import type { UserApiKey } from '@/types/userApiKey';
import type { User } from '@/types/user';
import type { Application } from '@/types/application';

const { Text } = Typography;

export default function DownstreamKeysTable() {
  const { t } = useTranslation('apiKeys');
  const { message } = App.useApp();
  const [searchParams, setSearchParams] = useSearchParams();

  const { data: keys, isLoading } = useAllUserApiKeys();
  const { data: usersData } = useUsers({ size: 200 });
  const { data: applications } = useApplications();
  const deleteMutation = useDeleteUserApiKey();
  const createMutation = useCreateUserApiKey();
  const [search, setSearch] = useState('');
  // 按应用筛选（从 URL ?applicationId= 初始化）
  const [applicationFilter, setApplicationFilter] = useState<number | undefined>(() => {
    const v = searchParams.get('applicationId');
    return v ? Number(v) : undefined;
  });

  // 创建弹窗
  const [formVisible, setFormVisible] = useState(false);
  const [form] = Form.useForm();
  const [creating, setCreating] = useState(false);
  const [createdKeyPlain, setCreatedKeyPlain] = useState<string | null>(null);

  const userMap = useMemo(() => {
    const map = new Map<number, User>();
    usersData?.items?.forEach((u: User) => map.set(u.id, u));
    return map;
  }, [usersData]);

  const applicationMap = useMemo(() => {
    const map = new Map<number, Application>();
    applications?.forEach((a: Application) => map.set(a.id, a));
    return map;
  }, [applications]);

  // 同步应用筛选到 URL（replace 避免污染浏览历史）
  useEffect(() => {
    if (applicationFilter == null) {
      searchParams.delete('applicationId');
    } else {
      searchParams.set('applicationId', String(applicationFilter));
    }
    setSearchParams(searchParams, { replace: true });
    // 仅在 applicationFilter 变化时同步，避免 URL 回写触发循环
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [applicationFilter]);

  const filtered = useMemo(() => (keys ?? []).filter((k: UserApiKey) => {
    if (applicationFilter != null && k.applicationId !== applicationFilter) return false;
    if (!search) return true;
    const q = search.toLowerCase();
    return k.keyPrefix?.toLowerCase().includes(q) || k.name?.toLowerCase().includes(q);
  }), [keys, applicationFilter, search]);

  const handleRevoke = useCallback(async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success(t('revoked', { defaultValue: 'Key 已吊销' }));
    } catch {
      message.error(t('revokeFailed', { defaultValue: '吊销失败' }));
    }
  }, [deleteMutation, message, t]);

  const handleAdd = () => {
    setCreatedKeyPlain(null);
    form.resetFields();
    // 预设当前筛选的应用，减少重复选择
    if (applicationFilter != null) {
      form.setFieldsValue({ applicationId: applicationFilter });
    }
    setFormVisible(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setCreating(true);

      const result = await createMutation.mutateAsync({
        userId: values.userId,
        applicationId: values.applicationId,
        name: values.name,
      });
      setCreatedKeyPlain(result.keyPlain);
      message.success(t('createSuccess', { defaultValue: 'Key 创建成功' }));
      form.resetFields();
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      message.error(t('createFailed', { defaultValue: '创建失败' }));
    } finally {
      setCreating(false);
    }
  };

  const columns = useMemo(() => [
    {
      title: t('keyPrefix', { defaultValue: 'Key' }),
      dataIndex: 'keyPlain',
      key: 'keyPlain',
      width: 200,
      render: (_: string, record: UserApiKey) => (
        <MaskedKeyDisplay
          keyPlain={record.keyPlain}
          mode="readonly"
          size="small"
        />
      ),
    },
    {
      title: t('keyName', { defaultValue: '名称' }),
      dataIndex: 'name',
      key: 'name',
      width: 100,
    },
    {
      title: t('user', { defaultValue: '所属用户' }),
      dataIndex: 'userId',
      key: 'userId',
      width: 80,
      render: (userId: number) => {
        const user = userMap.get(userId);
        return user ? `${user.username} (${userId})` : `用户 ${userId}`;
      },
    },
    {
      title: t('application', { defaultValue: '所属应用' }),
      dataIndex: 'applicationId',
      key: 'applicationId',
      width: 100,
      render: (applicationId: number | null) => {
        if (applicationId == null) {
          return <Text type="warning">{t('unbound', { defaultValue: '未绑定' })}</Text>;
        }
        const app = applicationMap.get(applicationId);
        return app ? `${app.name} (${applicationId})` : `应用 ${applicationId}`;
      },
    },
    {
      title: t('createdAt', { defaultValue: '创建时间' }),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 80,
      render: (val: string) => (val ? new Date(val).toLocaleString('zh-CN') : <Text type="secondary">-</Text>),
    },
    {
      title: t('actions', { defaultValue: '操作' }),
      key: 'actions',
      width: 40,
      render: (_: unknown, record: UserApiKey) => (
        <Popconfirm
          title={t('confirmRevoke', { defaultValue: '确定吊销此 Key？' })}
          onConfirm={() => handleRevoke(record.id)}
        >
          <Button type="link" size="small" danger icon={<DeleteOutlined />} />
        </Popconfirm>
      ),
    },
  ], [t, handleRevoke, userMap, applicationMap]);

  return (
    <div>
      <Card title={t('title')}>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', gap: 8 }}>
          <div style={{ display: 'flex', gap: 8 }}>
            <Input.Search
              placeholder={t('searchKeys', { defaultValue: '搜索 Key 前缀/名称...' })}
              style={{ width: 320 }}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              allowClear
            />
            <Select
              allowClear
              showSearch
              placeholder={t('filterByApplication', { defaultValue: '按应用筛选' })}
              style={{ width: 220 }}
              value={applicationFilter}
              onChange={(v: number | undefined) => setApplicationFilter(v)}
              filterOption={(input, option) =>
                (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={(applications ?? []).map((a: Application) => ({
                label: `${a.name} (${a.id})`,
                value: a.id,
              }))}
            />
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('createKey', { defaultValue: '创建 API Key' })}
          </Button>
        </div>

        <Table
          dataSource={filtered}
          columns={columns}
          rowKey="id"
          size="middle"
          loading={isLoading}
          pagination={{ pageSize: 15, showSizeChanger: true }}
          locale={{ emptyText: t('noKeys', { defaultValue: '暂无 API Key' }) }}
        />
      </Card>

      <Modal
        title={t('createKey', { defaultValue: '创建 API Key' })}
        open={formVisible}
        onOk={handleSubmit}
        onCancel={() => setFormVisible(false)}
        confirmLoading={creating}
        okText={t('create', { defaultValue: '创建' })}
        width={520}
        destroyOnClose
      >
        {createdKeyPlain && (
          <div style={{ marginBottom: 16, padding: 12, background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6 }}>
            <div style={{ marginBottom: 4, fontWeight: 500 }}>{t('createSuccess', { defaultValue: 'Key 创建成功' })}</div>
            <code style={{ wordBreak: 'break-all', fontSize: 13 }}>{createdKeyPlain}</code>
            <div style={{ marginTop: 4, color: '#999', fontSize: 12 }}>{t('oneTimeHint', { defaultValue: '此密钥仅显示一次，关闭后无法再次查看' })}</div>
          </div>
        )}

        <Form form={form} layout="vertical">
          <Form.Item name="userId" label={t('user', { defaultValue: '所属用户' })} rules={[{ required: true, message: '请选择用户' }]}>
            <Select
              showSearch
              placeholder="搜索并选择用户"
              filterOption={(input, option) =>
                (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={(usersData?.items ?? []).map((u: User) => ({
                label: `${u.username} (${u.id})`,
                value: u.id,
              }))}
            />
          </Form.Item>

          <Form.Item
            name="applicationId"
            label={t('application', { defaultValue: '所属应用' })}
            rules={[{ required: true, message: '请选择应用' }]}
          >
            <Select
              showSearch
              placeholder="搜索并选择应用"
              filterOption={(input, option) =>
                (option?.label as string ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={(applications ?? []).map((a: Application) => ({
                label: `${a.name} (${a.id})`,
                value: a.id,
              }))}
            />
          </Form.Item>

          <Form.Item name="name" label={t('keyName', { defaultValue: '名称' })} rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="例如：生产环境 Key" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
