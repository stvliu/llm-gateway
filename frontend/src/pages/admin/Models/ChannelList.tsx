import { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviders, useCreateProvider, useUpdateProvider, useDeleteProvider } from '@/services/query';
import type { Provider, CreateProviderRequest, ProviderType } from '@/types/provider';
import type { ColumnsType } from 'antd/es/table';

interface ChannelListProps {
  onSelect: (providerId: number | null) => void;
}

export function ChannelList({ onSelect }: ChannelListProps) {
  const { t } = useTranslation('models');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);
  const [form] = Form.useForm();

  const { data, isLoading } = useProviders({ size: 100 });
  const createMutation = useCreateProvider();
  const updateMutation = useUpdateProvider();
  const deleteMutation = useDeleteProvider();

  const handleSelect = (id: number) => {
    setSelectedId(id);
    onSelect(id);
  };

  const handleAdd = () => {
    setEditingProvider(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: Provider) => {
    setEditingProvider(record);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: t('confirm.delete', { ns: 'common' }),
      onOk: async () => {
        await deleteMutation.mutateAsync(id);
        message.success(t('message.success', { ns: 'common' }));
        if (selectedId === id) {
          setSelectedId(null);
          onSelect(null);
        }
      },
    });
  };

  const handleSubmit = async (values: CreateProviderRequest) => {
    if (editingProvider) {
      await updateMutation.mutateAsync({ id: editingProvider.id, data: values });
    } else {
      await createMutation.mutateAsync(values);
    }
    message.success(t('message.success', { ns: 'common' }));
    setModalOpen(false);
  };

  const columns: ColumnsType<Provider> = [
    {
      title: t('channel.name'),
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <a onClick={() => handleSelect(record.id)} style={{ fontWeight: selectedId === record.id ? 600 : 400 }}>
          {text}
        </a>
      ),
    },
    {
      title: t('channel.type'),
      dataIndex: 'type',
      key: 'type',
      render: (type: ProviderType) => t(`type.${type}`),
    },
    {
      title: t('channel.status'),
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
      <div style={{ padding: 8, borderBottom: '1px solid #f0f0f0' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          {t('addChannel')}
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={data?.content || []}
        rowKey="id"
        loading={isLoading}
        size="small"
        pagination={false}
        onRow={(record) => ({
          onClick: () => handleSelect(record.id),
          style: { cursor: 'pointer', background: selectedId === record.id ? '#e6f7ff' : undefined },
        })}
      />

      <Modal
        title={editingProvider ? t('actions.edit', { ns: 'common' }) : t('addChannel')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="name" label={t('channel.name')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label={t('channel.code')} rules={[{ required: true }]}>
            <Input disabled={!!editingProvider} />
          </Form.Item>
          <Form.Item name="type" label={t('channel.type')} rules={[{ required: true }]}>
            <Select disabled={!!editingProvider}>
              <Select.Option value="OPENAI">OpenAI</Select.Option>
              <Select.Option value="ANTHROPIC">Anthropic</Select.Option>
              <Select.Option value="GOOGLE">Google</Select.Option>
              <Select.Option value="AZURE">Azure</Select.Option>
              <Select.Option value="CUSTOM">{t('type.CUSTOM')}</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="baseUrl" label={t('channel.baseUrl')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          {editingProvider && (
            <Form.Item name="status" label={t('channel.status')}>
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
