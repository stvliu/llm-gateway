import { useState } from 'react';
import {
  Modal,
  Table,
  Button,
  Space,
  Tag,
  Popconfirm,
  Form,
  Input,
  InputNumber,
  Select,
  Typography,
  App,
} from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  CopyOutlined,
  KeyOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Team } from '@/types/team';
import type { UserApiKey, CreateUserApiKeyRequest, UpdateUserApiKeyRequest } from '@/types/team';
import { useTeamApiKeys, useCreateUserApiKey, useUpdateUserApiKey, useDeleteUserApiKey } from '@/services/query/useTeams';
import { useProviders } from '@/services/query/useProviders';
import { useProducts } from '@/services/query/useProducts';

const { Text, Paragraph } = Typography;

interface Props {
  team: Team;
  open: boolean;
  onClose: () => void;
}

/** 用户 API Key 管理弹窗 */
export default function UserApiKeyManageModal({ team, open, onClose }: Props) {
  const { t } = useTranslation('teams');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [editForm] = Form.useForm();
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<UserApiKey | null>(null);
  const [createdKeyInfo, setCreatedKeyInfo] = useState<{ keyPrefix: string; apiKeyPlain: string } | null>(null);
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);

  const { data: apiKeys, isLoading } = useTeamApiKeys(team.id);
  const createMutation = useCreateUserApiKey();
  const updateMutation = useUpdateUserApiKey();
  const deleteMutation = useDeleteUserApiKey();

  // 供应商列表（React Query 自动缓存）
  const { data: providersData } = useProviders();
  const providers = providersData?.items ?? [];

  // 产品列表（按供应商级联，enabled 守卫避免无效请求）
  const { data: products } = useProducts(selectedProviderId ?? 0);

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      const req: CreateUserApiKeyRequest = {
        teamId: team.id,
        userId: values.userId,
        productId: values.productId,
        name: values.name,
        models: values.models,
        quotaLimit: values.quotaLimit,
      };
      const result = await createMutation.mutateAsync({ teamId: team.id, data: req });
      setCreatedKeyInfo({ keyPrefix: result.keyPrefix, apiKeyPlain: result.apiKeyPlain });
      setCreateOpen(false);
      form.resetFields();
      setSelectedProviderId(null);
    } catch {
      // 表单验证失败
    }
  };

  const handleEdit = async () => {
    if (!editingKey) return;
    try {
      const values = await editForm.validateFields();
      const req: UpdateUserApiKeyRequest = {
        name: values.name,
        models: values.models,
        quotaLimit: values.quotaLimit,
        state: values.state,
      };
      await updateMutation.mutateAsync({ teamId: team.id, id: editingKey.id, data: req });
      setEditOpen(false);
      setEditingKey(null);
      editForm.resetFields();
    } catch {
      // 表单验证失败
    }
  };

  const handleDelete = async (keyId: number) => {
    await deleteMutation.mutateAsync({ teamId: team.id, id: keyId });
    message.success(t('apiKey.deleteSuccess', { defaultValue: '密钥已删除' }));
  };

  const openEdit = (record: UserApiKey) => {
    setEditingKey(record);
    editForm.setFieldsValue({
      name: record.name,
      models: record.models,
      quotaLimit: record.quotaLimit,
      state: record.state,
    });
    setEditOpen(true);
  };

  const stateColorMap: Record<string, string> = {
    ACTIVE: 'green',
    INACTIVE: 'orange',
    DELETED: 'red',
  };

  const columns = [
    {
      title: t('apiKey.name', { defaultValue: '名称' }),
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('apiKey.prefix', { defaultValue: '前缀' }),
      dataIndex: 'keyPrefix',
      key: 'keyPrefix',
      render: (prefix: string) => <Text code>{prefix}...</Text>,
    },
    {
      title: t('apiKey.userId', { defaultValue: '用户 ID' }),
      dataIndex: 'userId',
      key: 'userId',
    },
    {
      title: t('apiKey.models', { defaultValue: '可用模型' }),
      dataIndex: 'models',
      key: 'models',
      render: (models: string[]) =>
        models?.length > 0
          ? models.map((m) => <Tag key={m}>{m}</Tag>)
          : <Tag>{t('apiKey.allModels', { defaultValue: '全部' })}</Tag>,
    },
    {
      title: t('apiKey.quotaLimit', { defaultValue: '额度限制' }),
      dataIndex: 'quotaLimit',
      key: 'quotaLimit',
      render: (v: number | null) => v ?? t('apiKey.unlimited', { defaultValue: '无限制' }),
    },
    {
      title: t('apiKey.state', { defaultValue: '状态' }),
      dataIndex: 'state',
      key: 'state',
      render: (state: string) => <Tag color={stateColorMap[state]}>{state}</Tag>,
    },
    {
      title: t('apiKey.actions', { defaultValue: '操作' }),
      key: 'actions',
      render: (_: unknown, record: UserApiKey) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openEdit(record)}
          >
            {t('apiKey.edit', { defaultValue: '编辑' })}
          </Button>
          {record.state !== 'DELETED' && (
            <Popconfirm
              title={t('apiKey.deleteConfirm', { defaultValue: '确定删除此密钥？' })}
              onConfirm={() => handleDelete(record.id)}
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                {t('apiKey.delete', { defaultValue: '删除' })}
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <Modal
        title={
          <Space>
            <KeyOutlined />
            {t('apiKey.manageTitle', { defaultValue: '密钥管理' })} - {team.name}
          </Space>
        }
        open={open}
        onCancel={onClose}
        width={800}
        footer={null}
      >
        <div style={{ marginBottom: 16 }}>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setCreateOpen(true)}
          >
            {t('apiKey.create', { defaultValue: '创建密钥' })}
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={apiKeys?.filter((k) => k.state !== 'DELETED') ?? []}
          rowKey="id"
          loading={isLoading}
          size="small"
          pagination={false}
        />
      </Modal>

      {/* 创建密钥 */}
      <Modal
        title={t('apiKey.createTitle', { defaultValue: '创建用户密钥' })}
        open={createOpen}
        onOk={handleCreate}
        onCancel={() => { setCreateOpen(false); form.resetFields(); setSelectedProviderId(null); }}
        confirmLoading={createMutation.isPending}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="providerId"
            label={t('apiKey.selectProvider', { defaultValue: '选择供应商' })}
            rules={[{ required: true, message: t('apiKey.providerRequired', { defaultValue: '请选择供应商' }) }]}
          >
            <Select
              placeholder={t('apiKey.selectProviderPlaceholder', { defaultValue: '请先选择供应商' })}
              options={providers.map((p) => ({ label: p.providerName, value: p.id }))}
              onChange={(value: number) => setSelectedProviderId(value)}
            />
          </Form.Item>
          <Form.Item
            name="productId"
            label={t('apiKey.selectProduct', { defaultValue: '选择产品' })}
            rules={[{ required: true, message: t('apiKey.productRequired', { defaultValue: '请选择产品' }) }]}
          >
            <Select
              placeholder={t('apiKey.selectProductPlaceholder', { defaultValue: '请选择关联的产品' })}
              options={(products ?? []).map((p) => ({ label: p.name, value: p.id }))}
              disabled={!selectedProviderId}
            />
          </Form.Item>
          <Form.Item
            name="name"
            label={t('apiKey.name', { defaultValue: '名称' })}
            rules={[{ required: true, message: t('apiKey.nameRequired', { defaultValue: '请输入密钥名称' }) }]}
          >
            <Input placeholder={t('apiKey.namePlaceholder', { defaultValue: '例如：开发环境密钥' })} />
          </Form.Item>
          <Form.Item
            name="models"
            label={t('apiKey.models', { defaultValue: '可用模型' })}
            extra={t('apiKey.modelsExtra', { defaultValue: '留空表示允许访问所有模型' })}
          >
            <Select
              mode="tags"
              placeholder={t('apiKey.modelsPlaceholder', { defaultValue: '输入模型名称后按回车' })}
            />
          </Form.Item>
          <Form.Item
            name="quotaLimit"
            label={t('apiKey.quotaLimit', { defaultValue: '额度限制' })}
            extra={t('apiKey.quotaExtra', { defaultValue: '留空表示无限制' })}
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑密钥 */}
      <Modal
        title={t('apiKey.editTitle', { defaultValue: '编辑密钥' })}
        open={editOpen}
        onOk={handleEdit}
        onCancel={() => { setEditOpen(false); setEditingKey(null); editForm.resetFields(); }}
        confirmLoading={updateMutation.isPending}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="name"
            label={t('apiKey.name', { defaultValue: '名称' })}
            rules={[{ required: true }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="models"
            label={t('apiKey.models', { defaultValue: '可用模型' })}
          >
            <Select mode="tags" placeholder={t('apiKey.modelsPlaceholder', { defaultValue: '输入模型名称后按回车' })} />
          </Form.Item>
          <Form.Item
            name="quotaLimit"
            label={t('apiKey.quotaLimit', { defaultValue: '额度限制' })}
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="state"
            label={t('apiKey.state', { defaultValue: '状态' })}
          >
            <Select
              options={[
                { label: 'ACTIVE', value: 'ACTIVE' },
                { label: 'INACTIVE', value: 'INACTIVE' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 创建成功 — 显示明文 Key */}
      <Modal
        title={t('apiKey.createdTitle', { defaultValue: '密钥创建成功' })}
        open={!!createdKeyInfo}
        onOk={() => setCreatedKeyInfo(null)}
        onCancel={() => setCreatedKeyInfo(null)}
        okText={t('apiKey.createdOk', { defaultValue: '我已保存' })}
        cancelButtonProps={{ style: { display: 'none' } }}
      >
        <Paragraph type="warning" style={{ marginBottom: 12 }}>
          {t('apiKey.createdHint', { defaultValue: '请立即复制并保存此密钥，关闭后将无法再次查看！' })}
        </Paragraph>
        <Input.TextArea
          value={createdKeyInfo?.apiKeyPlain ?? ''}
          readOnly
    rows={3}
        />
        <Button
          icon={<CopyOutlined />}
          style={{ marginTop: 8 }}
          onClick={() => {
            navigator.clipboard.writeText(createdKeyInfo?.apiKeyPlain ?? '');
            message.success(t('apiKey.copied', { defaultValue: '已复制到剪贴板' }));
          }}
        >
          {t('apiKey.copy', { defaultValue: '复制密钥' })}
        </Button>
      </Modal>
    </>
  );
}