import { useState } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  message,
  Card,
  Tabs,
  Descriptions,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UploadOutlined,
  RocketOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  useTemplates,
  useCreateTemplate,
  useUpdateTemplate,
  useDeleteTemplate,
  usePublishTemplate,
  useApplyTemplate,
} from '@/services/query';
import type {
  ProviderTemplate,
  CreateTemplateRequest,
  TemplateListParams,
  TemplateType,
  MarketStatus,
} from '@/types/template';
import type { ColumnsType } from 'antd/es/table';

export default function AdminTemplates() {
  const { t } = useTranslation('templates');
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<TemplateType>('USER');
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [applyModalOpen, setApplyModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<ProviderTemplate | null>(null);
  const [selectedTemplate, setSelectedTemplate] = useState<ProviderTemplate | null>(null);
  const [createForm] = Form.useForm();
  const [applyForm] = Form.useForm();

  const [listParams, setListParams] = useState<TemplateListParams>({
    type: 'USER',
  });

  const { data, isLoading } = useTemplates(listParams);
  const createMutation = useCreateTemplate();
  const updateMutation = useUpdateTemplate();
  const deleteMutation = useDeleteTemplate();
  const publishMutation = usePublishTemplate();
  const applyMutation = useApplyTemplate();

  const handleTabChange = (key: string) => {
    const type = key as TemplateType;
    setActiveTab(type);
    setListParams({ ...listParams, type });
  };

  const handleCreate = () => {
    setEditingTemplate(null);
    createForm.resetFields();
    setCreateModalOpen(true);
  };

  const handleEdit = (record: ProviderTemplate) => {
    setEditingTemplate(record);
    createForm.setFieldsValue(record);
    setCreateModalOpen(true);
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

  const handlePublish = (id: number) => {
    Modal.confirm({
      title: t('confirm.publish'),
      onOk: async () => {
        await publishMutation.mutateAsync(id);
        message.success(t('message.publishSuccess'));
      },
    });
  };

  const handleApply = (record: ProviderTemplate) => {
    setSelectedTemplate(record);
    applyForm.resetFields();
    setApplyModalOpen(true);
  };

  const handleCreateSubmit = async (values: CreateTemplateRequest) => {
    if (editingTemplate) {
      await updateMutation.mutateAsync({ id: editingTemplate.id, data: values });
    } else {
      await createMutation.mutateAsync(values);
    }
    message.success(t('message.success', { ns: 'common' }));
    setCreateModalOpen(false);
  };

  const handleApplySubmit = async (values: { apiKey: string; channelName?: string }) => {
    if (!selectedTemplate) return;
    await applyMutation.mutateAsync({ id: selectedTemplate.id, data: values });
    message.success(t('message.applySuccess'));
    setApplyModalOpen(false);
    navigate('/admin/providers');
  };

  const columns: ColumnsType<ProviderTemplate> = [
    {
      title: t('name'),
      dataIndex: 'templateName',
      key: 'templateName',
    },
    {
      title: t('code'),
      dataIndex: 'templateCode',
      key: 'templateCode',
    },
    {
      title: t('providerType'),
      dataIndex: 'providerType',
      key: 'providerType',
    },
    {
      title: t('marketStatus'),
      dataIndex: 'marketStatus',
      key: 'marketStatus',
      render: (status: MarketStatus) => {
        const colorMap: Record<MarketStatus, string> = {
          PRIVATE: 'default',
          PENDING: 'orange',
          PUBLISHED: 'green',
          REJECTED: 'red',
        };
        return <Tag color={colorMap[status]}>{t(`marketStatus.${status}`)}</Tag>;
      },
    },
    {
      title: t('downloadCount'),
      dataIndex: 'downloadCount',
      key: 'downloadCount',
    },
    {
      title: t('actions.edit', { ns: 'common' }),
      key: 'actions',
      width: 200,
      render: (_, record) => (
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<RocketOutlined />}
            onClick={() => handleApply(record)}
          >
            {t('apply')}
          </Button>
          {record.templateType === 'USER' && record.marketStatus === 'PRIVATE' && (
            <Button
              type="link"
              size="small"
              onClick={() => handlePublish(record.id)}
              loading={publishMutation.isPending}
            >
              {t('publish')}
            </Button>
          )}
          <Button type="text" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Button
            type="text"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          />
        </Space>
      ),
    },
  ];

  const officialColumns: ColumnsType<ProviderTemplate> = [
    {
      title: t('name'),
      dataIndex: 'templateName',
      key: 'templateName',
    },
    {
      title: t('providerType'),
      dataIndex: 'providerType',
      key: 'providerType',
    },
    {
      title: t('modelCount'),
      dataIndex: 'modelCount',
      key: 'modelCount',
    },
    {
      title: t('downloadCount'),
      dataIndex: 'downloadCount',
      key: 'downloadCount',
    },
    {
      title: t('actions.edit', { ns: 'common' }),
      key: 'actions',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<RocketOutlined />}
            onClick={() => handleApply(record)}
          >
            {t('apply')}
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Tabs
        activeKey={activeTab}
        onChange={handleTabChange}
        items={[
          {
            key: 'USER',
            label: t('tabs.user'),
          },
          {
            key: 'OFFICIAL',
            label: t('tabs.official'),
          },
        ]}
      />

      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            {t('add')}
          </Button>
          <Button icon={<UploadOutlined />}>{t('import')}</Button>
        </Space>
      </div>

      <Table
        columns={activeTab === 'USER' ? columns : officialColumns}
        dataSource={data?.content || []}
        rowKey="id"
        loading={isLoading}
        pagination={{ pageSize: 10 }}
      />

      {/* Create/Edit Modal */}
      <Modal
        title={editingTemplate ? t('actions.edit', { ns: 'common' }) : t('add')}
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        footer={null}
        width={600}
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreateSubmit}>
          <Form.Item name="templateName" label={t('name')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="templateCode" label={t('code')} rules={[{ required: true }]}>
            <Input disabled={!!editingTemplate} />
          </Form.Item>
          <Form.Item name="providerType" label={t('providerType')} rules={[{ required: true }]}>
            <Select disabled={!!editingTemplate}>
              <Select.Option value="OPENAI">OpenAI</Select.Option>
              <Select.Option value="ANTHROPIC">Anthropic</Select.Option>
              <Select.Option value="DEEPSEEK">DeepSeek</Select.Option>
              <Select.Option value="QWEN">Qwen</Select.Option>
              <Select.Option value="OTHER">Other</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="description" label={t('description')}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={createMutation.isPending}>
                {t('actions.save', { ns: 'common' })}
              </Button>
              <Button onClick={() => setCreateModalOpen(false)}>{t('actions.cancel', { ns: 'common' })}</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Apply Template Modal */}
      <Modal
        title={t('applyTitle')}
        open={applyModalOpen}
        onCancel={() => setApplyModalOpen(false)}
        footer={null}
      >
        {selectedTemplate && (
          <>
            <Descriptions column={1} style={{ marginBottom: 16 }}>
              <Descriptions.Item label={t('name')}>{selectedTemplate.templateName}</Descriptions.Item>
              <Descriptions.Item label={t('providerType')}>{selectedTemplate.providerType}</Descriptions.Item>
              <Descriptions.Item label={t('modelCount')}>{selectedTemplate.modelCount}</Descriptions.Item>
            </Descriptions>
            <Form form={applyForm} layout="vertical" onFinish={handleApplySubmit}>
              <Form.Item
                name="apiKey"
                label={t('apiKey')}
                rules={[{ required: true, message: t('apiKeyRequired') }]}
              >
                <Input.Password placeholder={t('apiKeyPlaceholder')} />
              </Form.Item>
              <Form.Item name="channelName" label={t('channelName')}>
                <Input placeholder={t('channelNamePlaceholder')} />
              </Form.Item>
              <Form.Item>
                <Space>
                  <Button type="primary" htmlType="submit" loading={applyMutation.isPending}>
                    {t('apply')}
                  </Button>
                  <Button onClick={() => setApplyModalOpen(false)}>
                    {t('actions.cancel', { ns: 'common' })}
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          </>
        )}
      </Modal>
    </Card>
  );
}
