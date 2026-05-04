import { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, KeyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useUsers, useCreateUser, useUpdateUser, useDeleteUser, useResetPassword } from '@/services/query';
import type { User, CreateUserRequest, UserRole } from '@/types/user';
import type { ColumnsType } from 'antd/es/table';

interface UserListProps {
  onSelect: (userId: number | null) => void;
}

export function UserList({ onSelect }: UserListProps) {
  const { t } = useTranslation('users');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();

  const { data, isLoading } = useUsers({ size: 100 });
  const createMutation = useCreateUser();
  const updateMutation = useUpdateUser();
  const deleteMutation = useDeleteUser();
  const resetPasswordMutation = useResetPassword();

  const handleSelect = (id: number) => {
    setSelectedId(id);
    onSelect(id);
  };

  const handleAdd = () => {
    setEditingUser(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: User) => {
    setEditingUser(record);
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

  const handleResetPassword = (id: number) => {
    Modal.confirm({
      title: t('actions.resetPassword', { ns: 'common' }),
      onOk: async () => {
        await resetPasswordMutation.mutateAsync(id);
        message.success(t('message.success', { ns: 'common' }));
      },
    });
  };

  const handleSubmit = async (values: CreateUserRequest) => {
    if (editingUser) {
      await updateMutation.mutateAsync({ id: editingUser.id, data: values });
    } else {
      await createMutation.mutateAsync(values);
    }
    message.success(t('message.success', { ns: 'common' }));
    setModalOpen(false);
  };

  const columns: ColumnsType<User> = [
    {
      title: t('user.username'),
      dataIndex: 'username',
      key: 'username',
      render: (text, record) => (
        <a onClick={() => handleSelect(record.id)} style={{ fontWeight: selectedId === record.id ? 600 : 400 }}>
          {text}
        </a>
      ),
    },
    {
      title: t('user.email'),
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: t('user.role'),
      dataIndex: 'role',
      key: 'role',
      render: (role: UserRole) => t(`role.${role}`),
    },
    {
      title: t('user.status'),
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
      width: 150,
      render: (_, record) => (
        <Space>
          <Button type="text" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Button type="text" icon={<KeyOutlined />} onClick={() => handleResetPassword(record.id)} />
          <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ padding: 8, borderBottom: '1px solid #f0f0f0' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          {t('addUser')}
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
        title={editingUser ? t('actions.edit', { ns: 'common' }) : t('addUser')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="username" label={t('user.username')} rules={[{ required: true }]}>
            <Input disabled={!!editingUser} />
          </Form.Item>
          {!editingUser && (
            <Form.Item name="password" label="密码" rules={[{ required: true }]}>
              <Input.Password />
            </Form.Item>
          )}
          <Form.Item name="email" label={t('user.email')} rules={[{ required: true, type: 'email' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="role" label={t('user.role')} rules={[{ required: true }]}>
            <Select>
              <Select.Option value="ADMIN">{t('role.ADMIN')}</Select.Option>
              <Select.Option value="USER">{t('role.USER')}</Select.Option>
            </Select>
          </Form.Item>
          {editingUser && (
            <Form.Item name="status" label={t('user.status')}>
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
