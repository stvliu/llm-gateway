import { useState } from 'react';
import { Card, Table, Button, Space, Tag, Modal, Form, Input, Select, message, Typography, Tooltip } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  StopOutlined,
  PlayCircleOutlined,
  HolderOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import {
  useProviderApiKeys,
  useCreateProviderApiKey,
  useUpdateProviderApiKey,
  useDeleteProviderApiKey,
  useSetEnabledProviderApiKey,
} from '@/services/query/useProviderApiKeys';
import { useProviders } from '@/services/query/useProviders';
import type { ProviderApiKey, ProviderApiKeyState } from '@/types/providerApiKey';

const { Paragraph } = Typography;

export default function AdminApiKeyPool() {
  const { t } = useTranslation('apiKeyPool');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<ProviderApiKey | null>(null);
  const [selectedProviderId, setSelectedProviderId] = useState<number | undefined>();
  const [form] = Form.useForm();

  const { data, isLoading } = useProviderApiKeys({ providerId: selectedProviderId });
  const { data: providers } = useProviders();
  const createMutation = useCreateProviderApiKey();
  const updateMutation = useUpdateProviderApiKey();
  const deleteMutation = useDeleteProviderApiKey();
  const toggleMutation = useSetEnabledProviderApiKey();

  const handleAdd = () => {
    setEditingKey(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: ProviderApiKey) => {
    setEditingKey(record);
    form.setFieldsValue({
      providerId: record.providerId,
      keyName: record.keyName,
      apiKey: '',
      priority: record.priority,
      weight: record.weight,
      isDefault: record.isDefault,
    });
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

  const handleToggle = (record: ProviderApiKey) => {
    const action = record.state === 'DISABLED' ? t('enable') : t('disable');
    Modal.confirm({
      title: t('confirm.toggle', { action }),
      onOk: async () => {
        await toggleMutation.mutateAsync({
          id: record.id,
          enabled: record.state === 'DISABLED',
        });
        message.success(t('message.success', { ns: 'common' }));
      },
    });
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingKey) {
        await updateMutation.mutateAsync({
          id: editingKey.id,
          data: {
            keyName: values.keyName,
            apiKey: values.apiKey || undefined,
            priority: values.priority,
            weight: values.weight,
            isDefault: values.isDefault,
          },
        });
      } else {
        await createMutation.mutateAsync({
          providerId: values.providerId,
          keyName: values.keyName,
          apiKey: values.apiKey,
          priority: values.priority,
          weight: values.weight,
          isDefault: values.isDefault,
        });
      }
      message.success(t('message.success', { ns: 'common' }));
      setModalOpen(false);
    } catch {
      // 表单验证失败或 API 错误，不做处理
    }
  };

  const stateColorMap: Record<ProviderApiKeyState, string> = {
    ACTIVE: 'green',
    DISABLED: 'default',
    DELETED: 'red',
  };

  const columns: ColumnsType<ProviderApiKey> = [
    {
      title: '',
      key: 'drag',
      width: 40,
      render: () => <HolderOutlined style={{ cursor: 'grab', color: '#999' }} />,
    },
    {
      title: t('priority'),
      dataIndex: 'priority',
      key: 'priority',
      width: 80,
    },
    {
      title: t('name'),
      dataIndex: 'keyName',
      key: 'keyName',
    },
    {
      title: t('provider'),
      dataIndex: 'providerId',
      key: 'providerId',
      render: (providerId: number) => {
        const provider = providers?.items?.find((p) => p.id === providerId);
        return provider?.providerName || '-';
      },
    },
    {
      title: t('key'),
      dataIndex: 'keyHint',
      key: 'keyHint',
      render: (key: string) => (
        <Paragraph copyable={{ text: key }} style={{ margin: 0, fontFamily: 'monospace' }}>
          {key}
        </Paragraph>
      ),
    },
    {
      title: t('state'),
      dataIndex: 'state',
      key: 'state',
      render: (state: ProviderApiKeyState) => (
        <Tag color={stateColorMap[state]}>{t(`state.${state.toLowerCase()}`, { ns: 'common' })}</Tag>
      ),
    },
    {
      title: t('actions.label', { ns: 'common' }),
      key: 'actions',
      width: 140,
      render: (_, record) => (
        <Space>
          <Tooltip title={record.state === 'DISABLED' ? t('enable') : t('disable')}>
            <Button
              type="text"
              icon={record.state === 'DISABLED' ? <PlayCircleOutlined /> : <StopOutlined />}
              onClick={() => handleToggle(record)}
              disabled={record.state === 'DELETED'}
            />
          </Tooltip>
          <Button type="text" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    },
  ];

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Card title={t('title')}>
        <div style={{ marginBottom: 16 }}>
          <Space>
            <Select
              placeholder={t('selectProvider')}
              style={{ width: 200 }}
              allowClear
              value={selectedProviderId}
              onChange={setSelectedProviderId}
              options={providers?.items?.map((p) => ({ value: p.id, label: p.providerName }))}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              {t('add')}
            </Button>
          </Space>
        </div>

        <div style={{ marginBottom: 16, padding: 12, background: '#fafafa', borderRadius: 6 }}>
          <p style={{ margin: 0, fontSize: 13, color: '#666' }}>
            {t('dragHint')}
          </p>
        </div>

        <Table
          columns={columns}
          dataSource={data?.items || []}
          rowKey="id"
          loading={isLoading}
          pagination={false}
        />
      </Card>

      <Modal
        title={editingKey ? t('actions.label', { ns: 'common' }) : t('add')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="providerId" label={t('provider')} rules={[{ required: !editingKey }]}>
            <Select disabled={!!editingKey} options={providers?.items?.map((p) => ({ value: p.id, label: p.providerName }))} />
          </Form.Item>
          <Form.Item name="keyName" label={t('name')} rules={[{ required: true }]}>
            <Input placeholder={t('namePlaceholder')} />
          </Form.Item>
          <Form.Item name="apiKey" label={t('key')} rules={[{ required: !editingKey }]}>
            <Input.TextArea rows={3} placeholder={t('keyPlaceholder')} />
          </Form.Item>
          <Form.Item name="priority" label={t('priority')}>
            <Input type="number" min={1} />
          </Form.Item>
          <Form.Item name="weight" label={t('weight')}>
            <Input type="number" min={1} max={100} />
          </Form.Item>
          <Form.Item name="isDefault" label={t('isDefault')} valuePropName="checked">
            <Select>
              <Select.Option value={true}>{t('yes')}</Select.Option>
              <Select.Option value={false}>{t('no')}</Select.Option>
            </Select>
          </Form.Item>
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
