import { useState, useCallback } from 'react';
import { Modal, App, Button, Space, Card, Tag, Spin, Typography, Form, Input } from 'antd';
import { RightOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useProviderCatalogs, useMaterializeProvider } from '@/services/query/useCatalog';
import { useCreateProvider } from '@/services/query/useProviders';
import { ProviderIcon } from '@/components/ui';
import type { Provider } from '@/types/provider';
import type { ProviderCatalog } from '@/types/catalog';

const { Text, Paragraph } = Typography;

interface Props {
  open: boolean;
  providers: Provider[];
  onClose: () => void;
  onCreated: () => void;
}

type Step = 'select-catalog' | 'custom-form' | 'materialize';

/**
 * 供应商创建弹窗
 * 支持两种创建方式：
 * 1. 选择目录快速创建（物化供应商目录为运营实体）
 * 2. 自定义创建（手动填写信息）
 */
export function ProviderCreateModal({ open, providers: _providers, onClose, onCreated }: Props) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();
  const materializeMutation = useMaterializeProvider();
  const createMutation = useCreateProvider();
  const { data: catalogList, isLoading: catalogLoading } = useProviderCatalogs();
  const [customForm] = Form.useForm();

  const [currentStep, setCurrentStep] = useState<Step>('select-catalog');
  const [selectedCatalog, setSelectedCatalog] = useState<ProviderCatalog | null>(null);
  const [saving, setSaving] = useState(false);

  const handleClose = useCallback(() => {
    setCurrentStep('select-catalog');
    setSelectedCatalog(null);
    customForm.resetFields();
    onClose();
  }, [onClose, customForm]);

  // 选择目录后进入物化步骤
  const handleSelectCatalog = useCallback((catalog: ProviderCatalog) => {
    setSelectedCatalog(catalog);
    setCurrentStep('materialize');
  }, []);

  // 跳过目录选择，进入自定义创建
  const handleSkipCatalog = useCallback(() => {
    setSelectedCatalog(null);
    setCurrentStep('custom-form');
  }, []);

  // 返回目录选择
  const handleBackToSelect = useCallback(() => {
    setSelectedCatalog(null);
    setCurrentStep('select-catalog');
  }, []);

  // 物化供应商目录
  const handleMaterialize = useCallback(async () => {
    if (!selectedCatalog) return;

    setSaving(true);
    try {
      await materializeMutation.mutateAsync(selectedCatalog.code);
      message.success(t('template.createSuccess', {
        defaultValue: `已创建供应商 "${selectedCatalog.name}"`,
        name: selectedCatalog.name,
      }));
      onCreated();
      handleClose();
    } catch (error) {
      console.error('Materialize failed:', error);
      message.error(t('template.createFailed', { defaultValue: '创建失败' }));
    } finally {
      setSaving(false);
    }
  }, [selectedCatalog, materializeMutation, message, t, onCreated, handleClose]);

  // 渲染目录选择步骤
  const renderCatalogSelection = () => (
    <div>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        {t('template.selectHint', { defaultValue: '选择模板可快速创建供应商和模型' })}
      </Paragraph>

      <Spin spinning={catalogLoading}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 12, maxHeight: 400, overflow: 'auto' }}>
          {catalogList?.map((catalog) => (
            <Card
              key={catalog.code}
              hoverable
              size="small"
              onClick={() => handleSelectCatalog(catalog)}
              style={{ cursor: 'pointer' }}
            >
              <Card.Meta
                avatar={
                  <ProviderIcon providerId={catalog.code} size={24} />
                }
                title={catalog.name}
                description={
                  <Space direction="vertical" size={4}>
                    <Text type="secondary" style={{ fontSize: 12 }}>{catalog.code}</Text>
                    <Tag color={catalog.materialized ? 'green' : 'blue'} style={{ fontSize: 11 }}>
                      {catalog.materialized
                        ? t('template.materialized', { defaultValue: '已物化' })
                        : t('template.notMaterialized', { defaultValue: '未物化' })
                      }
                    </Tag>
                  </Space>
                }
              />
            </Card>
          ))}
        </div>
      </Spin>

      {(!catalogList || catalogList.length === 0) && !catalogLoading && (
        <div style={{ textAlign: 'center', padding: 32 }}>
          <Text type="secondary">{t('template.noTemplate', { defaultValue: '暂无模板' })}</Text>
        </div>
      )}

      <div style={{ marginTop: 16, textAlign: 'center' }}>
        <Button type="link" onClick={handleSkipCatalog}>
          {t('template.customCreate', { defaultValue: '自定义创建' })} <RightOutlined />
        </Button>
      </div>
    </div>
  );

  // 渲染物化步骤
  const renderMaterialize = () => (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 12 }}>
        <ProviderIcon providerId={selectedCatalog?.code} size={32} />
        <div>
          <Text strong>{selectedCatalog?.name}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>{selectedCatalog?.code}</Text>
        </div>
        <Tag color="blue">{selectedCatalog?.providerType}</Tag>
      </div>

      <Paragraph type="secondary" style={{ fontSize: 12 }}>
        {t('template.quickCreateHint', {
          defaultValue: `将物化供应商 "${selectedCatalog?.name}" 目录为运营实体`,
          name: selectedCatalog?.name,
        })}
      </Paragraph>
    </div>
  );

  // 自定义创建提交
  const handleCustomCreate = useCallback(async () => {
    try {
      const values = await customForm.validateFields();
      setSaving(true);
      await createMutation.mutateAsync(values);
      message.success(t('createSuccess', { defaultValue: '供应商创建成功' }));
      onCreated();
      handleClose();
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return; // 表单校验失败
      console.error('Create provider failed:', error);
      message.error(t('createFailed', { defaultValue: '创建失败' }));
    } finally {
      setSaving(false);
    }
  }, [customForm, createMutation, message, t, onCreated, handleClose]);

  // 渲染自定义创建步骤
  const renderCustomForm = () => (
    <Form
      form={customForm}
      layout="vertical"
      autoComplete="off"
    >
      <Form.Item
        name="code"
        label={t('fields.code', { defaultValue: '品牌标识' })}
        rules={[
          { required: true, message: t('fields.codeRequired', { defaultValue: '请输入品牌标识' }) },
          { pattern: /^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$/, message: t('fields.codePattern', { defaultValue: '只能包含小写字母、数字和中划线，长度3-64' }) },
        ]}
        extra={t('fields.codeExtra', { defaultValue: '如 openai、anthropic，全局唯一，创建后不可修改' })}
      >
        <Input placeholder="openai" />
      </Form.Item>
      <Form.Item
        name="providerName"
        label={t('fields.providerName', { defaultValue: '供应商名称' })}
        rules={[{ required: true, message: t('fields.providerNameRequired', { defaultValue: '请输入供应商名称' }) }]}
      >
        <Input placeholder="OpenAI" />
      </Form.Item>
      <Form.Item
        name="websiteUrl"
        label={t('fields.websiteUrl', { defaultValue: '官网地址' })}
      >
        <Input placeholder="https://openai.com" />
      </Form.Item>
      <Form.Item
        name="apiDocUrl"
        label={t('fields.apiDocUrl', { defaultValue: 'API 文档地址' })}
      >
        <Input placeholder="https://platform.openai.com/docs" />
      </Form.Item>
      <Form.Item
        name="description"
        label={t('fields.description', { defaultValue: '描述' })}
      >
        <Input.TextArea rows={3} placeholder={t('fields.descriptionPlaceholder', { defaultValue: '供应商描述信息' })} />
      </Form.Item>
    </Form>
  );

  // 根据当前步骤渲染内容
  const renderContent = () => {
    switch (currentStep) {
      case 'select-catalog':
        return renderCatalogSelection();
      case 'materialize':
        return renderMaterialize();
      case 'custom-form':
        return renderCustomForm();
    }
  };

  // 根据当前步骤确定弹窗标题
  const getTitle = () => {
    switch (currentStep) {
      case 'select-catalog':
        return t('template.quickCreate', { defaultValue: '快速创建' });
      case 'materialize':
        return t('template.createNow', { defaultValue: '一键创建' });
      case 'custom-form':
        return t('addProvider', { defaultValue: '新增供应商' });
    }
  };

  // 根据当前步骤确定底部按钮
  const getFooter = () => {
    if (currentStep === 'select-catalog') {
      return null; // 选择步骤不需要底部按钮
    }

    if (currentStep === 'materialize') {
      return (
        <Space>
          <Button onClick={handleBackToSelect}>
            {t('wizard.previous', { defaultValue: '上一步' })}
          </Button>
          <Button type="primary" onClick={handleMaterialize} loading={saving}>
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
      width={currentStep === 'select-catalog' ? 720 : 560}
      destroyOnHidden
    >
      {renderContent()}
    </Modal>
  );
}