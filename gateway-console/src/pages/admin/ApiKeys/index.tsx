import { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message, Typography, Card, Row, Col, theme, App } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { ColumnsType } from 'antd/es/table';
import { useApiKeys, useCreateApiKey, useUpdateApiKey, useDeleteApiKey } from '@/services/query/useApiKeys';
import type { ApiKey, GatewayApiKeyState } from '@/types/apiKey';

const { Paragraph } = Typography;

export default function AdminApiKeys() {
  const { t } = useTranslation('apiKeys');
  const { t: tc } = useTranslation('common');
  const { token } = theme.useToken();
  const { modal } = App.useApp();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<ApiKey | null>(null);
  const [newKey, setNewKey] = useState<string | null>(null);
  const [searchKeyword, setSearchKeyword] = useState<string>('');
  const [form] = Form.useForm();

  const { data, isLoading } = useApiKeys();
  const createMutation = useCreateApiKey();
  const updateMutation = useUpdateApiKey();
  const deleteMutation = useDeleteApiKey();

  const handleAdd = () => {
    setEditingKey(null);
    setNewKey(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: ApiKey) => {
    setEditingKey(record);
    setNewKey(null);
    form.setFieldsValue({
      name: record.name,
      userId: record.userId,
      state: record.state,
    });
    setModalOpen(true);
  };

  const handleDelete = (id: number) => {
    modal.confirm({
      title: tc('confirm.delete'),
      content: tc('confirm.deleteWarning'),
      okText: tc('actions.delete'),
      cancelText: tc('actions.cancel'),
      okButtonProps: { danger: true, type: "primary" },
      centered: true,
      onOk: async () => {
        await deleteMutation.mutateAsync(id);
        message.success(tc('message.success'));
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
            name: values.name,
            state: values.state,
          },
        });
        message.success(t('message.success', { ns: 'common' }));
      } else {
        const result = await createMutation.mutateAsync({
          name: values.name,
          userId: values.userId,
        });
        setNewKey(result.rawKey);
        message.success(t('createSuccess'));
      }
      setModalOpen(false);
    } catch {
      // 表单验证失败或 API 错误，不做处理
    }
  };

  const stateColorMap: Record<GatewayApiKeyState, string> = {
    ACTIVE: 'green',
    DISABLED: 'red',
    DELETED: 'default',
  };

  const filteredData = data?.items?.filter(
    (item) =>
      !searchKeyword ||
      item.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
      item.username.toLowerCase().includes(searchKeyword.toLowerCase())
  ) || [];

  const columns: ColumnsType<ApiKey> = [
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
      render: (state: GatewayApiKeyState) => (
        <Tag color={stateColorMap[state]}>
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
      render: (date: string | undefined) => date || '-',
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
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Input
            placeholder={t('searchPlaceholder')}
            prefix={<SearchOutlined />}
            allowClear
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
        </Col>
        <Col span={16} style={{ textAlign: 'right' }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('add')}
          </Button>
        </Col>
      </Row>

      <Table
        columns={columns}
        dataSource={filteredData}
        rowKey="id"
        loading={isLoading}
        pagination={{ pageSize: 10, showSizeChanger: true }}
      />

      <Modal
        title={editingKey ? t('actions.label', { ns: 'common' }) : t('add')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        {newKey && (
          <div style={{ marginBottom: 16, padding: 12, background: token.colorWarningBg, borderRadius: 4 }}>
            <p style={{ margin: 0, fontWeight: 600 }}>{t('keyCreated')}</p>
            <Paragraph copyable={{ text: newKey }} style={{ margin: '8px 0', fontFamily: 'monospace' }}>
              {newKey}
            </Paragraph>
          </div>
        )}
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="userId" label={t('user')} rules={[{ required: !editingKey }]}>
            <Select disabled={!!editingKey}>
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
