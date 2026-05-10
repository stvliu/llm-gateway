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

const { Paragraph } = Typography;

interface PoolApiKey {
  id: number;
  name: string;
  key: string;
  providerId: number;
  providerName: string;
  state: 'ACTIVE' | 'DISABLED' | 'DELETED';
  priority: number;
  createdAt: string;
}

// TODO: 接入后端 API
// API: GET /api/admin/api-key-pool
// API: POST /api/admin/api-key-pool
// API: PUT /api/admin/api-key-pool/:id
// API: DELETE /api/admin/api-key-pool/:id
// API: PUT /api/admin/api-key-pool/:id/toggle
// API: PUT /api/admin/api-key-pool/reorder (拖拽排序)
const mockData: PoolApiKey[] = [
  { id: 1, name: 'key-1', key: 'sk-xxxx...xxxx', providerId: 1, providerName: 'OpenAI', state: 'ACTIVE', priority: 1, createdAt: '2024-01-15' },
  { id: 2, name: 'key-2', key: 'sk-yyyy...yyyy', providerId: 1, providerName: 'OpenAI', state: 'DISABLED', priority: 2, createdAt: '2024-01-20' },
  { id: 3, name: 'key-3', key: 'sk-zzzz...zzzz', providerId: 2, providerName: 'Anthropic', state: 'ACTIVE', priority: 1, createdAt: '2024-02-01' },
];

export default function AdminApiKeyPool() {
  const { t } = useTranslation('apiKeyPool');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<PoolApiKey | null>(null);
  const [form] = Form.useForm();

  const handleAdd = () => {
    setEditingKey(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: PoolApiKey) => {
    setEditingKey(record);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const handleDelete = (_id: number) => {
    Modal.confirm({
      title: t('confirm.delete', { ns: 'common' }),
      onOk: () => {
        message.success(t('message.success', { ns: 'common' }));
      },
    });
  };

  const handleToggle = (record: PoolApiKey) => {
    const action = record.state === 'DISABLED' ? t('enable') : t('disable');
    Modal.confirm({
      title: t('confirm.toggle', { action }),
      onOk: () => {
        message.success(t('message.success', { ns: 'common' }));
      },
    });
  };

  const handleSubmit = async () => {
    try {
      await form.validateFields();
      // TODO: 调用 API 保存数据
      message.success(t('message.success', { ns: 'common' }));
      setModalOpen(false);
    } catch (error) {
      // 表单验证失败，不做处理
    }
  };

  const stateColorMap: Record<string, string> = {
    ACTIVE: 'green',
    DISABLED: 'default',
    DELETED: 'red',
  };

  const columns: ColumnsType<PoolApiKey> = [
    // TODO: 实现拖拽排序功能，集成 react-beautiful-dnd 或 dnd-kit
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
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('provider'),
      dataIndex: 'providerName',
      key: 'providerName',
    },
    {
      title: t('key'),
      dataIndex: 'key',
      key: 'key',
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
      render: (state) => (
        <Tag color={stateColorMap[state]}>{t(`state.${state.toLowerCase()}`, { ns: 'common' })}</Tag>
      ),
    },
    {
      title: t('actions.edit', { ns: 'common' }),
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
              options={[
                { value: 1, label: 'OpenAI' },
                { value: 2, label: 'Anthropic' },
                { value: 3, label: 'Google' },
              ]}
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
          dataSource={mockData}
          rowKey="id"
          pagination={false}
        />
      </Card>

      <Modal
        title={editingKey ? t('actions.edit', { ns: 'common' }) : t('add')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="providerId" label={t('provider')} rules={[{ required: true }]}>
            <Select>
              <Select.Option value={1}>OpenAI</Select.Option>
              <Select.Option value={2}>Anthropic</Select.Option>
              <Select.Option value={3}>Google</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="name" label={t('name')} rules={[{ required: true }]}>
            <Input placeholder={t('namePlaceholder')} />
          </Form.Item>
          <Form.Item name="key" label={t('key')} rules={[{ required: true }]}>
            <Input.TextArea rows={3} placeholder={t('keyPlaceholder')} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
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
