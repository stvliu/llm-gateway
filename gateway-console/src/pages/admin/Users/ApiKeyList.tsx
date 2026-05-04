import { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CopyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useApiKeys, useCreateApiKey, useUpdateApiKey, useDeleteApiKey } from '@/services/query';
import type { ApiKey, CreateApiKeyRequest } from '@/types/apiKey';
import type { ColumnsType } from 'antd/es/table';

const { Paragraph } = Typography;

interface ApiKeyListProps {
  userId: number | null;
}

export function ApiKeyList({ userId }: ApiKeyListProps) {
  const { t } = useTranslation('users');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingApiKey, setEditingApiKey] = useState<ApiKey | null>(null);
  const [newKey, setNewKey] = useState<string | null>(null);
  const [form] = Form.useForm();

  const { data, isLoading } = useApiKeys({ userId: userId || undefined, size: 100 });
  const createMutation = useCreateApiKey();
  const updateMutation = useUpdateApiKey();
  const deleteMutation = useDeleteApiKey();

  const handleAdd = () => {
    if (!userId) {
      message.warning('请先选择用户');
      return;
    }
    setEditingApiKey(null);
    setNewKey(null);
    form.resetFields();
    form.setFieldsValue({ userId });
    setModalOpen(true);
  };

  const handleEdit = (record: ApiKey) => {
    setEditingApiKey(record);
    setNewKey(null);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: t('confirm.delete', { ns: 'common' }),
      onOk: async () => {
        await deleteMutation.mutateAsync(id);
        message.success(t('message.success', { ns: 'common' }));
      },
    });
  };

  const handleCopy = (key: string) => {
    navigator.clipboard.writeText(key);
    message.success(t('apiKey.copySuccess'));
  };

  const handleSubmit = async (values: CreateApiKeyRequest) => {
    if (editingApiKey) {
      await updateMutation.mutateAsync({ id: editingApiKey.id, data: values });
      message.success(t('message.success', { ns: 'common' }));
    } else {
      const result = await createMutation.mutateAsync(values);
      setNewKey(result.key);
      message.success(t('apiKey.createSuccess'));
    }
    setModalOpen(false);
  };

  const columns: ColumnsType<ApiKey> = [
    {
      title: t('apiKey.name'),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('apiKey.key'),
      dataIndex: 'key',
      key: 'key',
      render: (key: string) => (
        <Paragraph copyable={{ text: key, tooltips: ['复制', '已复制'] }} style={{ margin: 0 }}>
          {key}
        </Paragraph>
      ),
    },
    {
      title: t('apiKey.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === 'ENABLED' ? 'green' : 'red'}>
          {t(`status.${status.toLowerCase()}`, { ns: 'common' })}
        </Tag>
      ),
    },
    {
      title: t('actions.edit', { ns: 'common' }),
      key: 'actions',
      width: 100,
      render: (_, record) => (
        <Space>
          <Button type="text" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} disabled={!userId}>
          {t('addApiKey')}
        </Button>
        {!userId && <span style={{ marginLeft: 8, color: '#999' }}>请先选择用户</span>}
      </div>

      <Table
        columns={columns}
        dataSource={data?.content || []}
        rowKey="id"
        loading={isLoading}
        size="small"
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingApiKey ? t('actions.edit', { ns: 'common' }) : t('addApiKey')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        {newKey && (
          <div style={{ marginBottom: 16, padding: 12, background: '#fff7e6', borderRadius: 4 }}>
            <p style={{ margin: 0, fontWeight: 600 }}>API Key 已创建（仅显示一次）：</p>
            <Paragraph copyable={{ text: newKey }} style={{ margin: '8px 0', fontFamily: 'monospace' }}>
              {newKey}
            </Paragraph>
            <Button icon={<CopyOutlined />} onClick={() => handleCopy(newKey)}>
              复制 Key
            </Button>
          </div>
        )}
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="name" label={t('apiKey.name')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="userId" hidden>
            <Input />
          </Form.Item>
          {editingApiKey && (
            <Form.Item name="status" label={t('apiKey.status')}>
              <Select>
                <Select.Option value="ENABLED">{t('status.enabled', { ns: 'common' })}</Select.Option>
                <Select.Option value="DISABLED">{t('status.disabled', { ns: 'common' })}</Select.Option>
              </Select>
            </Form.Item>
          )}
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={createMutation.isPending || updateMutation.isPending}>
                {t('actions.save', { ns: 'common' })}
              </Button>
              <Button onClick={() => setModalOpen(false)}>{t('actions.cancel', { ns: 'common' })}</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
