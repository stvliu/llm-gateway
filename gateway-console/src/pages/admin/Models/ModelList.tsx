import { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModels, useProviders, useCreateModel, useUpdateModel, useDeleteModel } from '@/services/query';
import type { Model, CreateModelRequest, ModelType } from '@/types/model';
import type { ColumnsType } from 'antd/es/table';

interface ModelListProps {
  providerId: number | null;
}

export function ModelList({ providerId }: ModelListProps) {
  const { t } = useTranslation('models');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<Model | null>(null);
  const [form] = Form.useForm();

  const { data, isLoading } = useModels({ providerId: providerId || undefined, size: 100 });
  const { data: providers } = useProviders({ size: 100 });
  const createMutation = useCreateModel();
  const updateMutation = useUpdateModel();
  const deleteMutation = useDeleteModel();

  const handleAdd = () => {
    setEditingModel(null);
    form.resetFields();
    if (providerId) {
      form.setFieldsValue({ providerId });
    }
    setModalOpen(true);
  };

  const handleEdit = (record: Model) => {
    setEditingModel(record);
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

  const handleSubmit = async (values: CreateModelRequest) => {
    if (editingModel) {
      await updateMutation.mutateAsync({ id: editingModel.id, data: values });
    } else {
      await createMutation.mutateAsync(values);
    }
    message.success(t('message.success', { ns: 'common' }));
    setModalOpen(false);
  };

  const columns: ColumnsType<Model> = [
    { title: t('model.name'), dataIndex: 'name', key: 'name' },
    {
      title: t('model.type'),
      dataIndex: 'type',
      key: 'type',
      render: (type: ModelType) => t(`type.${type}`),
    },
    {
      title: t('model.enabled'),
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === 'ENABLED' ? 'green' : 'red'}>
          {t(`status.${status.toLowerCase()}`)}
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
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          {t('addModel')}
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data?.items || []}
        rowKey="id"
        loading={isLoading}
        size="small"
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingModel ? t('actions.edit', { ns: 'common' }) : t('addModel')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="name" label={t('model.name')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="providerModelId" label={t('model.providerModelId')} rules={[{ required: true }]}>
            <Input disabled={!!editingModel} />
          </Form.Item>
          <Form.Item name="providerId" label={t('model.provider')} rules={[{ required: true }]}>
            <Select disabled={!!editingModel}>
              {providers?.items?.map((p) => (
                <Select.Option key={p.id} value={p.id}>
                  {p.providerName}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="type" label={t('model.type')} rules={[{ required: true }]}>
            <Select disabled={!!editingModel}>
              <Select.Option value="CHAT">{t('type.CHAT')}</Select.Option>
              <Select.Option value="COMPLETION">{t('type.COMPLETION')}</Select.Option>
              <Select.Option value="EMBEDDING">{t('type.EMBEDDING')}</Select.Option>
              <Select.Option value="IMAGE">{t('type.IMAGE')}</Select.Option>
              <Select.Option value="AUDIO">{t('type.AUDIO')}</Select.Option>
            </Select>
          </Form.Item>
          {editingModel && (
            <Form.Item name="enabled" label={t('model.enabled')}>
              <Select>
                <Select.Option value="ENABLED">{t('status.enabled')}</Select.Option>
                <Select.Option value="DISABLED">{t('status.disabled')}</Select.Option>
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
