import { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Card } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviders, useCreateProvider, useUpdateProvider, useDeleteProvider } from '@/services/query';
import type { Provider, CreateProviderRequest } from '@/types/provider';
import type { ProviderType } from '@/types/api';
import type { ColumnsType } from 'antd/es/table';

export default function AdminProviders() {
  const { t } = useTranslation('providers');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);
  const [form] = Form.useForm();

  const { data, isLoading } = useProviders({ size: 100 });
  const createMutation = useCreateProvider();
  const updateMutation = useUpdateProvider();
  const deleteMutation = useDeleteProvider();

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
      title: t('name'),
      dataIndex: 'providerName',
      key: 'providerName',
    },
    {
      title: t('type.label'),
      dataIndex: 'providerType',
      key: 'providerType',
      render: (type: ProviderType) => t(`type.${type}`),
    },
    {
      title: t('baseUrl'),
      dataIndex: 'baseUrl',
      key: 'baseUrl',
      ellipsis: true,
    },
    {
      title: t('state'),
      dataIndex: 'state',
      key: 'state',
      render: (state: string) => (
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
        title={editingProvider ? t('actions.label', { ns: 'common' }) : t('add')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="providerName" label={t('name')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="providerType" label={t('type')} rules={[{ required: true }]}>
            <Select disabled={!!editingProvider}>
              <Select.Option value="OPENAI">{t('type.OPENAI')}</Select.Option>
              <Select.Option value="ANTHROPIC">{t('type.ANTHROPIC')}</Select.Option>
              <Select.Option value="GEMINI">{t('type.GEMINI')}</Select.Option>
              <Select.Option value="DEEPSEEK">{t('type.DEEPSEEK')}</Select.Option>
              <Select.Option value="MOONSHOT">{t('type.MOONSHOT')}</Select.Option>
              <Select.Option value="ZHIPU">{t('type.ZHIPU')}</Select.Option>
              <Select.Option value="YI">{t('type.YI')}</Select.Option>
              <Select.Option value="BAICHUAN">{t('type.BAICHUAN')}</Select.Option>
              <Select.Option value="MINIMAX">{t('type.MINIMAX')}</Select.Option>
              <Select.Option value="SILICONFLOW">{t('type.SILICONFLOW')}</Select.Option>
              <Select.Option value="VOLCENGINE">{t('type.VOLCENGINE')}</Select.Option>
              <Select.Option value="QWEN">{t('type.QWEN')}</Select.Option>
              <Select.Option value="WENXIN">{t('type.WENXIN')}</Select.Option>
              <Select.Option value="OTHER">{t('type.OTHER')}</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="baseUrl" label={t('baseUrl')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          {editingProvider && (
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
