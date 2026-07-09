import { useState, useMemo } from 'react';
import { Table, Button, Space, Tag, Modal, Form, Input, Select, Card, Tooltip } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, KeyOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useConfirm } from '@/hooks/useConfirm';
import { useMessage } from '@/hooks/useMessage';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import { useUsers, useCreateUser, useUpdateUser, useDeleteUser, useResetPassword } from '@/services/query';
import type { User, CreateUserRequest, UserRole, UserState } from '@/types/user';
import type { ColumnsType } from 'antd/es/table';
import UserApiKeyModal from './UserApiKeyModal';

export default function Users() {
  const { t } = useTranslation('users');
  const { confirm } = useConfirm();
  const message = useMessage();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.USER_WRITE);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [apiKeyUserId, setApiKeyUserId] = useState<number | null>(null);
  const [apiKeyUsername, setApiKeyUsername] = useState('');

  const { data, isLoading } = useUsers({ size: 100 });
  const createMutation = useCreateUser();
  const updateMutation = useUpdateUser();
  const deleteMutation = useDeleteUser();
  const resetPasswordMutation = useResetPassword();

  const filtered = useMemo(() => {
    if (!searchKeyword.trim()) return data?.items ?? [];
    const q = searchKeyword.toLowerCase();
    return (data?.items ?? []).filter(
      (u: User) =>
        u.username.toLowerCase().includes(q) ||
        (u.email ?? '').toLowerCase().includes(q)
    );
  }, [data, searchKeyword]);

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
      onConfirm: async () => {
        // resetPassword 现在一次性返回明文密码（不再走邮件），需弹窗展示供管理员转交
        const { newPassword } = await resetPasswordMutation.mutateAsync(id);
        Modal.info({
          title: '重置密码成功',
          content: (
            <div>
              <p>新密码（仅显示一次，请立即保存并转交用户）：</p>
              <Input value={newPassword} readOnly />
            </div>
          ),
          okText: '已保存',
        });
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
      width: 160,
      render: (val: string) => val ? new Date(val).toLocaleString('zh-CN') : '-',
    },
    ...(canWrite ? [{
      title: t('actions.label', { ns: 'common' }),
      key: 'actions',
      width: 60,
      render: (_: unknown, record: User) => (
        <Space>
          <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Tooltip title="管理 API Key">
            <Button
              type="text" size="small"
              icon={<SafetyCertificateOutlined />}
              onClick={() => {
                setApiKeyUserId(record.id);
                setApiKeyUsername(record.username);
              }}
            />
          </Tooltip>
          <Tooltip title="重置密码">
            <Button type="text" size="small" icon={<KeyOutlined />} onClick={() => handleResetPassword(record.id)} />
          </Tooltip>
          <Button type="text" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    }] : []),
  ];

  return (
    <Card title={t('title')}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Input.Search
          placeholder={t('searchPlaceholder')}
          style={{ width: 250 }}
          allowClear
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
        />
        {canWrite && (
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('addUser')}
          </Button>
        )}
      </div>

      <Table
        columns={columns}
        dataSource={filtered}
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

      {/* API Key 管理弹窗 */}
      <UserApiKeyModal
        open={apiKeyUserId !== null}
        userId={apiKeyUserId ?? 0}
        username={apiKeyUsername}
        onClose={() => setApiKeyUserId(null)}
      />
    </Card>
  );
}
