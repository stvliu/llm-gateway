import { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, Card } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, KeyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useConfirm } from '@/hooks/useConfirm';
import { useMessage } from '@/hooks/useMessage';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useUsers, useCreateUser, useUpdateUser, useDeleteUser, useResetPassword } from '@/services/query';
import type { User, CreateUserRequest, UserRole, UserState } from '@/types/user';
import type { ColumnsType } from 'antd/es/table';

export default function Users() {
  const { t } = useTranslation('users');
  const { confirm } = useConfirm();
  const message = useMessage();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.USER_WRITE);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();

  const { data, isLoading } = useUsers({ size: 100 });
  const createMutation = useCreateUser();
  const updateMutation = useUpdateUser();
  const deleteMutation = useDeleteUser();
  const resetPasswordMutation = useResetPassword();

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
    confirm({
      type: 'danger',
      onConfirm: () => deleteMutation.mutateAsync(id),
    });
  };

  const handleResetPassword = (id: number) => {
    confirm({
      type: 'warning',
      title: 'confirm.resetPassword',
      content: 'confirm.resetPasswordWarning',
      onConfirm: () => resetPasswordMutation.mutateAsync(id),
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
      title: t('user.state'),
      dataIndex: 'state',
      key: 'state',
      render: (state: UserState) => {
        const colorMap: Record<UserState, string> = {
          ACTIVE: 'green',
          INACTIVE: 'red',
          LOCKED: 'orange',
        };
        return (
          <Tag color={colorMap[state] || 'default'}>
            {t(`state.${state.toLowerCase()}`)}
          </Tag>
        );
      },
    },
    {
      title: t('user.createdAt'),
      dataIndex: 'createdAt',
      key: 'createdAt',
    },
    ...(canWrite ? [{
      title: t('actions.label', { ns: 'common' }),
      key: 'actions',
      width: 150,
      render: (_: unknown, record: User) => (
        <Space>
          <Button type="text" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Button type="text" icon={<KeyOutlined />} onClick={() => handleResetPassword(record.id)} />
          <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    }] : []),
  ];

  return (
    <Card title={t('title')}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Input.Search placeholder={t('searchPlaceholder')} style={{ width: 250 }} allowClear />
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('addUser')}
          </Button>
        )}
      </div>

      <Table
        columns={columns}
        dataSource={data?.items || []}
        rowKey="id"
        loading={isLoading}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingUser ? t('actions.label', { ns: 'common' }) : t('addUser')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="username" label={t('user.username')} rules={[{ required: true }]}>
            <Input disabled={!!editingUser} />
          </Form.Item>
          {!editingUser && (
            <Form.Item name="password" label={t('user.password')} rules={[{ required: true }]}>
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
            <Form.Item name="state" label={t('user.state')}>
              <Select>
                <Select.Option value="ACTIVE">{t('state.active')}</Select.Option>
                <Select.Option value="INACTIVE">{t('state.disabled')}</Select.Option>
                <Select.Option value="LOCKED">{t('state.locked')}</Select.Option>
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
