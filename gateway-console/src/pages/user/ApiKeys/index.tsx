import { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Typography, Card } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useApiKeys, useCreateApiKey, useUpdateApiKey, useDeleteApiKey } from '@/services/query';
import type { ApiKey, GatewayApiKeyState } from '@/types/apiKey';
import type { ColumnsType } from 'antd/es/table';

const { Paragraph } = Typography;

export default function UserApiKeys() {
  const { t } = useTranslation('apiKeys');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingApiKey, setEditingApiKey] = useState<ApiKey | null>(null);
  const [newKey, setNewKey] = useState<string | null>(null);
  const [form] = Form.useForm();

  const { data, isLoading } = useApiKeys({ size: 100 });
  const createMutation = useCreateApiKey();
  const updateMutation = useUpdateApiKey();
  const deleteMutation = useDeleteApiKey();

  const handleAdd = () => {
    setEditingApiKey(null);
    setNewKey(null);
    form.resetFields();
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
      title: t('confirmDelete'),
      onOk: async () => {
        await deleteMutation.mutateAsync(id);
        message.success(t('message.success', { ns: 'common' }));
      },
    });
  };

  const handleSubmit = async (values: { name: string; state?: GatewayApiKeyState }) => {
    if (editingApiKey) {
      await updateMutation.mutateAsync({ id: editingApiKey.id, data: values });
      message.success(t('message.success', { ns: 'common' }));
    } else {
      const result = await createMutation.mutateAsync({ name: values.name, userId: 0 }); // userId will be set by backend
      setNewKey(result.rawKey);
      message.success(t('createSuccess'));
    }
    setModalOpen(false);
  };

  const columns: ColumnsType<ApiKey> = [
    { title: t('name'), dataIndex: 'name', key: 'name' },
    {
      title: t('key'),
      dataIndex: 'key',
      key: 'key',
      render: (key: string) => (
        <Paragraph copyable={{ text: key, tooltips: ['复制', '已复制'] }} style={{ margin: 0 }}>
          {key}
        </Paragraph>
      ),
    },
    {
      title: t('state'),
      dataIndex: 'state',
      key: 'state',
      render: (state: GatewayApiKeyState) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'red'}>
          {t(`state.${state.toLowerCase()}`, { ns: 'common' })}
        </Tag>
      ),
    },
    {
      title: t('actions.label', { ns: 'common' }),
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
    <Card title={t('title')}>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          {t('add')}
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data?.items || []}
        rowKey="id"
        loading={isLoading}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingApiKey ? t('actions.label', { ns: 'common' }) : t('add')}
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
          </div>
        )}
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="name" label={t('name')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          {editingApiKey && (
            <Form.Item name="state" label={t('state')}>
              <Select>
                <Select.Option value="ACTIVE">{t('state.active', { ns: 'common' })}</Select.Option>
                <Select.Option value="DISABLED">{t('state.disabled', { ns: 'common' })}</Select.Option>
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
    </Card>
  );
}
