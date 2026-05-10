import { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Typography, Card, Row, Col } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';

const { Paragraph } = Typography;

interface UserApiKey {
  id: number;
  name: string;
  key: string;
  userId: number;
  username: string;
  state: 'ACTIVE' | 'DISABLED' | 'DELETED';
  createdAt: string;
  lastUsedAt: string | null;
}

// TODO: 接入后端 API
// API: GET /api/admin/api-keys
// API: POST /api/admin/api-keys
// API: PUT /api/admin/api-keys/:id
// API: DELETE /api/admin/api-keys/:id
const mockData: UserApiKey[] = [
  { id: 1, name: '开发环境', key: 'gw-xxxx...xxxx', userId: 1, username: 'user_001', state: 'ACTIVE', createdAt: '2024-01-15', lastUsedAt: '2024-03-20' },
  { id: 2, name: '测试环境', key: 'gw-yyyy...yyyy', userId: 1, username: 'user_001', state: 'ACTIVE', createdAt: '2024-01-20', lastUsedAt: '2024-03-19' },
  { id: 3, name: '生产环境', key: 'gw-zzzz...zzzz', userId: 2, username: 'user_002', state: 'DISABLED', createdAt: '2024-02-01', lastUsedAt: null },
];

export default function AdminApiKeys() {
  const { t } = useTranslation('apiKeys');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<UserApiKey | null>(null);
  const [newKey, setNewKey] = useState<string | null>(null);
  const [form] = Form.useForm();

  const handleAdd = () => {
    setEditingKey(null);
    setNewKey(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: UserApiKey) => {
    setEditingKey(record);
    setNewKey(null);
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

  const handleSubmit = async () => {
    try {
      await form.validateFields();
      // TODO: 调用 API 保存数据
      if (editingKey) {
        message.success(t('message.success', { ns: 'common' }));
      } else {
        // 模拟生成新 Key
        setNewKey('gw-new-generated-key-xxxx');
        message.success(t('createSuccess'));
      }
      setModalOpen(false);
    } catch (error) {
      // 表单验证失败，不做处理
    }
  };

  const columns: ColumnsType<UserApiKey> = [
    {
      title: t('name'),
      dataIndex: 'name',
      key: 'name',
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
      title: t('username'),
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: t('state'),
      dataIndex: 'state',
      key: 'state',
      render: (state) => (
        <Tag color={state === 'ACTIVE' ? 'green' : 'red'}>
          {t(`state.${state.toLowerCase()}`, { ns: 'common' })}
        </Tag>
      ),
    },
    {
      title: t('createdAt'),
      dataIndex: 'createdAt',
      key: 'createdAt',
    },
    {
      title: t('lastUsedAt'),
      dataIndex: 'lastUsedAt',
      key: 'lastUsedAt',
      render: (date: string | null) => date || '-',
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
    <Card title={t('title')}>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Input placeholder={t('searchPlaceholder')} prefix={<SearchOutlined />} allowClear />
        </Col>
        <Col span={16} style={{ textAlign: 'right' }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('add')}
          </Button>
        </Col>
      </Row>

      <Table
        columns={columns}
        dataSource={mockData}
        rowKey="id"
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingKey ? t('actions.edit', { ns: 'common' }) : t('add')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        {newKey && (
          <div style={{ marginBottom: 16, padding: 12, background: '#fff7e6', borderRadius: 4 }}>
            <p style={{ margin: 0, fontWeight: 600 }}>{t('keyCreated')}</p>
            <Paragraph copyable={{ text: newKey }} style={{ margin: '8px 0', fontFamily: 'monospace' }}>
              {newKey}
            </Paragraph>
          </div>
        )}
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="userId" label={t('user')} rules={[{ required: true }]}>
            <Select>
              <Select.Option value={1}>user_001</Select.Option>
              <Select.Option value={2}>user_002</Select.Option>
              <Select.Option value={3}>user_003</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="name" label={t('name')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          {editingKey && (
            <Form.Item name="state" label={t('state')}>
              <Select>
                <Select.Option value="ACTIVE">{t('state.active', { ns: 'common' })}</Select.Option>
                <Select.Option value="DISABLED">{t('state.disabled', { ns: 'common' })}</Select.Option>
              </Select>
            </Form.Item>
          )}
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
    </Card>
  );
}
