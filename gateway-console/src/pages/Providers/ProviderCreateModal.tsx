import { useState, useCallback } from 'react';
import { Modal, Form, Input, App, Button, Space, Card, Tag, Spin, Typography } from 'antd';
import { RightOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviderMetadataList, useApplyMetadata } from '@/services/query/useMetadata';
import { useCreateProvider } from '@/services/query';
import { ProviderIcon } from '@/components/ui';
import type { Provider, CreateProviderRequest } from '@/types/provider';
import type { ProviderMetadata } from '@/types/metadata';

const { Text, Paragraph } = Typography;

interface Props {
  open: boolean;
  providers: Provider[];
  onClose: () => void;
  onCreated: () => void;
}

type Step = 'select-metadata' | 'custom-form' | 'apply-metadata';

/**
 * 供应商创建弹窗
 * 支持两种创建方式：
 * 1. 选择元数据快速创建（自动创建供应商和模型）
 * 2. 自定义创建（手动填写信息）
 */
export function ProviderCreateModal({ open, providers, onClose, onCreated }: Props) {
  const { t } = useTranslation('providers');
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const createMutation = useCreateProvider();
  const applyMutation = useApplyMetadata();
  const { data: metadataList, isLoading: metadataLoading } = useProviderMetadataList();

  const [currentStep, setCurrentStep] = useState<Step>('select-metadata');
  const [selectedMetadata, setSelectedMetadata] = useState<ProviderMetadata | null>(null);
  const [saving, setSaving] = useState(false);

  const handleClose = useCallback(() => {
    form.resetFields();
    setCurrentStep('select-metadata');
    setSelectedMetadata(null);
    onClose();
  }, [form, onClose]);

  // 选择元数据后进入应用步骤
  const handleSelectMetadata = useCallback((metadata: ProviderMetadata) => {
    setSelectedMetadata(metadata);
    setCurrentStep('apply-metadata');
  }, []);

  // 跳过元数据，进入自定义表单
  const handleSkipMetadata = useCallback(() => {
    setSelectedMetadata(null);
    setCurrentStep('custom-form');
  }, []);

  // 返回元数据选择
  const handleBackToSelect = useCallback(() => {
    setSelectedMetadata(null);
    setCurrentStep('select-metadata');
  }, []);

  // 应用元数据创建供应商
  const handleApplyMetadata = useCallback(async (values: { apiKey: string; channelName?: string }) => {
    if (!selectedMetadata) return;

    setSaving(true);
    try {
      const result = await applyMutation.mutateAsync({
        id: selectedMetadata.id,
        data: { apiKey: values.apiKey, channelName: values.channelName },
      });
      console.log('Apply metadata result:', result);
      message.success(t('template.createSuccess', {
        defaultValue: `已创建供应商 "${selectedMetadata.providerName}"`,
        name: selectedMetadata.providerName,
      }));
      onCreated();
      handleClose();
    } catch (error) {
      console.error('Apply metadata failed:', error);
      message.error(t('template.createFailed', { defaultValue: '创建失败，请检查 API Key 是否正确' }));
    } finally {
      setSaving(false);
    }
  }, [selectedMetadata, applyMutation, message, t, onCreated, handleClose]);

  // 自定义创建供应商
  const handleCustomCreate = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const name = values.providerName as string;

      if (providers.some(p => p.providerName.toLowerCase() === name.toLowerCase())) {
        message.warning(t('validation.nameDuplicate', { defaultValue: '供应商名称已存在' }));
        return;
      }

      setSaving(true);
      const request: CreateProviderRequest = {
        providerName: name,
        websiteUrl: values.websiteUrl,
        apiDocUrl: values.apiDocUrl,
        priority: values.priority,
      };

      await createMutation.mutateAsync(request);
      message.success(t('message.createSuccess', { defaultValue: '供应商创建成功' }));
      onCreated();
      handleClose();
    } catch {
      // 表单验证失败
    } finally {
      setSaving(false);
    }
  }, [form, providers, createMutation, message, t, onCreated, handleClose]);

  // 渲染元数据选择步骤
  const renderMetadataSelection = () => (
    <div>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        {t('template.selectHint', { defaultValue: '选择模板可快速创建供应商和模型' })}
      </Paragraph>

      <Spin spinning={metadataLoading}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 12, maxHeight: 400, overflow: 'auto' }}>
          {metadataList?.map((metadata) => (
            <Card
              key={metadata.id}
              hoverable
              size="small"
              onClick={() => handleSelectMetadata(metadata)}
              style={{ cursor: 'pointer' }}
            >
              <Card.Meta
                avatar={
                  <ProviderIcon providerId={metadata.providerId} size="small" iconSize={24} />
                }
                title={metadata.providerName}
                description={
                  <Space direction="vertical" size={4}>
                    <Text type="secondary" style={{ fontSize: 12 }}>{metadata.providerId}</Text>
                    {metadata.modelCount !== undefined && (
                      <Tag color="blue" style={{ fontSize: 11 }}>
                        {metadata.modelCount} {t('template.models', { defaultValue: '个' })} {t('template.modelCount', { defaultValue: '个模型' })}
                      </Tag>
                    )}
                  </Space>
                }
              />
            </Card>
          ))}
        </div>
      </Spin>

      {(!metadataList || metadataList.length === 0) && !metadataLoading && (
        <div style={{ textAlign: 'center', padding: 32 }}>
          <Text type="secondary">{t('template.noTemplate', { defaultValue: '暂无模板' })}</Text>
        </div>
      )}

      <div style={{ marginTop: 16, textAlign: 'center' }}>
        <Button type="link" onClick={handleSkipMetadata}>
          {t('template.customCreate', { defaultValue: '自定义创建' })} <RightOutlined />
        </Button>
      </div>
    </div>
  );

  // 渲染应用元数据步骤
  const renderApplyMetadata = () => (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 12 }}>
        {selectedMetadata?.iconUrl ? (
          <Avatar src={selectedMetadata.iconUrl} />
        ) : (
          <Avatar style={{ backgroundColor: '#87e8de' }}>
            {selectedMetadata?.providerName.charAt(0)}
          </Avatar>
        )}
        <div>
          <Text strong>{selectedMetadata?.providerName}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>{selectedMetadata?.providerId}</Text>
        </div>
        {selectedMetadata?.modelCount !== undefined && (
          <Tag color="blue">{selectedMetadata.modelCount} {t('template.modelCount')}</Tag>
        )}
      </div>

      <Form form={form} layout="vertical" onFinish={handleApplyMetadata}>
        <Form.Item
          name="apiKey"
          label={t('template.apiKeyPlaceholder', { defaultValue: 'API Key' })}
          rules={[{ required: true, message: t('validation.apiKeyRequired', { defaultValue: '请输入 API Key' }) }]}
        >
          <Input.Password placeholder={t('template.apiKeyPlaceholder', { defaultValue: '请输入 API Key' })} />
        </Form.Item>
        <Form.Item
          name="channelName"
          label={t('template.channelNameExtra', { defaultValue: '渠道名称（可选）' })}
        >
          <Input placeholder={t('template.channelNameExtra', { defaultValue: '留空则使用模板名称' })} />
        </Form.Item>
      </Form>

      <Paragraph type="secondary" style={{ fontSize: 12 }}>
        {t('template.quickCreateHint', {
          defaultValue: `将创建供应商 "${selectedMetadata?.providerName}" 并自动配置模型`,
          name: selectedMetadata?.providerName,
          count: selectedMetadata?.modelCount || 0,
        })}
      </Paragraph>
    </div>
  );

  // 渲染自定义表单步骤
  const renderCustomForm = () => (
    <Form form={form} layout="vertical">
      <Form.Item
        name="providerName"
        label={t('form.providerName', { defaultValue: '供应商名称' })}
        rules={[{ required: true, message: t('validation.nameRequired', { defaultValue: '请输入供应商名称' }) }]}
      >
        <Input />
      </Form.Item>
      <Form.Item name="websiteUrl" label={t('form.websiteUrl', { defaultValue: '官网地址' })}>
        <Input />
      </Form.Item>
      <Form.Item name="apiDocUrl" label={t('form.apiDocUrl', { defaultValue: 'API 文档地址' })}>
        <Input />
      </Form.Item>
      <Form.Item name="priority" label={t('form.priority', { defaultValue: '优先级' })}>
        <Input type="number" />
      </Form.Item>
    </Form>
  );

  // 根据当前步骤渲染内容
  const renderContent = () => {
    switch (currentStep) {
      case 'select-metadata':
        return renderMetadataSelection();
      case 'apply-metadata':
        return renderApplyMetadata();
      case 'custom-form':
        return renderCustomForm();
    }
  };

  // 根据当前步骤确定弹窗标题
  const getTitle = () => {
    switch (currentStep) {
      case 'select-metadata':
        return t('template.quickCreate', { defaultValue: '快速创建' });
      case 'apply-metadata':
        return t('template.createNow', { defaultValue: '一键创建' });
      case 'custom-form':
        return t('addProvider', { defaultValue: '新增供应商' });
    }
  };

  // 根据当前步骤确定底部按钮
  const getFooter = () => {
    if (currentStep === 'select-metadata') {
      return null; // 选择步骤不需要底部按钮
    }

    if (currentStep === 'apply-metadata') {
      return (
        <Space>
          <Button onClick={handleBackToSelect}>
            {t('wizard.previous', { defaultValue: '上一步' })}
          </Button>
          <Button type="primary" onClick={() => form.submit()} loading={saving}>
            {t('template.createNow', { defaultValue: '一键创建' })}
          </Button>
        </Space>
      );
    }

    if (currentStep === 'custom-form') {
      return (
        <Space>
          <Button onClick={handleBackToSelect}>
            {t('wizard.previous', { defaultValue: '上一步' })}
          </Button>
          <Button type="primary" onClick={handleCustomCreate} loading={saving}>
            {t('wizard.create', { defaultValue: '完成创建' })}
          </Button>
        </Space>
      );
    }
  };

  return (
    <Modal
      title={getTitle()}
      open={open}
      onCancel={handleClose}
      footer={getFooter()}
      width={currentStep === 'select-metadata' ? 720 : 560}
      destroyOnClose
    >
      {renderContent()}
    </Modal>
  );
}
