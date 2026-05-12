import { useState, useEffect, useCallback } from 'react';
import {
  Descriptions,
  Form,
  Input,
  Select,
  Tag,
  Space,
  Button,
  Collapse,
} from 'antd';
import {
  SettingOutlined,
  GlobalOutlined,
  LinkOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { ProviderTemplateSelector } from './ProviderTemplateSelector';
import { StatusIndicator } from '@/components/common';
import type { Provider, CreateProviderRequest } from '@/types/provider';
import type { ProviderTemplate } from '@/types/template';

interface ProviderBasicInfoTabProps {
  provider: Provider | null;
  mode: 'view' | 'edit' | 'create';
  onValuesChange?: (changed: boolean) => void;
  onSubmit?: (values: CreateProviderRequest) => Promise<void>;
}

type FormStep = 'select-template' | 'fill-form';

/**
 * 供应商基本信息标签页
 * - 查看模式：Descriptions 展示
 * - 编辑模式：Form 表单
 * - 新增模式：模板选择 + Form 表单
 */
export function ProviderBasicInfoTab({
  provider,
  mode,
  onValuesChange,
  onSubmit,
}: ProviderBasicInfoTabProps) {
  const { t } = useTranslation('providers');

  const [form] = Form.useForm();
  const [step, setStep] = useState<FormStep>(mode === 'create' ? 'select-template' : 'fill-form');

  // 初始化表单
  useEffect(() => {
    if (mode === 'edit' && provider) {
      form.setFieldsValue(provider);
      setStep('fill-form');
    } else if (mode === 'create') {
      form.resetFields();
      setStep('select-template');
    } else if (mode === 'view' && provider) {
      form.setFieldsValue(provider);
    }
  }, [mode, provider, form]);

  // 监听表单变化
  useEffect(() => {
    if (mode !== 'view' && onValuesChange) {
      onValuesChange(false);
    }
  }, [mode, onValuesChange]);

  // 选择模板
  const handleTemplateSelect = useCallback((template: ProviderTemplate) => {
    const config = template.providerConfig as Record<string, unknown>;
    form.setFieldsValue({
      providerName: config.provider_name || template.templateName,
      providerType: template.providerType,
      baseUrl: config.base_url || '',
      websiteUrl: config.website_url || '',
      apiDocUrl: config.api_doc_url || '',
    });
    setStep('fill-form');
    onValuesChange?.(true);
  }, [form, onValuesChange]);

  // 自定义添加
  const handleCustomAdd = useCallback(() => {
    form.resetFields();
    setStep('fill-form');
    onValuesChange?.(true);
  }, [form, onValuesChange]);

  // 提交表单
  const handleSubmit = useCallback(async () => {
    try {
      const values = await form.validateFields();
      await onSubmit?.(values);
    } catch {
      // 验证失败
    }
  }, [form, onSubmit]);

  // 查看模式
  if (mode === 'view' && provider) {
    return (
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label={t('provider.name')}>
          <Space>
            <StatusIndicator
              status={provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'}
              showLabel={false}
            />
            <span style={{ fontWeight: 500 }}>{provider.providerName}</span>
          </Space>
        </Descriptions.Item>
        <Descriptions.Item label={t('provider.type')}>
          <Tag color="blue">
            {t(`type.${provider.providerType}`, { ns: 'providers', defaultValue: provider.providerType })}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label={t('provider.state')}>
          <StatusIndicator
            status={provider.state === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'}
          />
        </Descriptions.Item>
        <Descriptions.Item label={t('provider.baseUrl')}>
          <code style={{ fontSize: 12 }}>{provider.baseUrl || '-'}</code>
        </Descriptions.Item>
        {provider.websiteUrl && (
          <Descriptions.Item label={t('provider.websiteUrl', { defaultValue: '官网' })}>
            <a href={provider.websiteUrl} target="_blank" rel="noopener noreferrer">
              <Space>
                <GlobalOutlined />
                {provider.websiteUrl}
              </Space>
            </a>
          </Descriptions.Item>
        )}
        {provider.apiDocUrl && (
          <Descriptions.Item label={t('provider.apiDocUrl', { defaultValue: 'API 文档' })}>
            <a href={provider.apiDocUrl} target="_blank" rel="noopener noreferrer">
              <Space>
                <LinkOutlined />
                {provider.apiDocUrl}
              </Space>
            </a>
          </Descriptions.Item>
        )}
        <Descriptions.Item label={t('detail.createdAt')}>
          {new Date(provider.createdAt).toLocaleString()}
        </Descriptions.Item>
        <Descriptions.Item label={t('detail.updatedAt')}>
          {new Date(provider.updatedAt).toLocaleString()}
        </Descriptions.Item>
      </Descriptions>
    );
  }

  // 新增模式：模板选择
  if (mode === 'create' && step === 'select-template') {
    return (
      <div>
        <div style={{ marginBottom: 16, textAlign: 'right' }}>
          <a onClick={handleCustomAdd} style={{ fontSize: 13 }}>
            {t('provider.customAdd', { defaultValue: '自定义供应商' })}
          </a>
        </div>
        <ProviderTemplateSelector onSelect={handleTemplateSelect} />
      </div>
    );
  }

  // 编辑/新增模式：表单
  return (
    <Form
      form={form}
      layout="vertical"
      onValuesChange={() => onValuesChange?.(true)}
      onFinish={handleSubmit}
    >
      <Collapse
        defaultActiveKey={['basic']}
        items={[
          {
            key: 'basic',
            label: (
              <Space>
                <SettingOutlined />
                {t('detail.basicInfo', { defaultValue: '基本信息' })}
              </Space>
            ),
            children: (
              <>
                <Form.Item
                  name="providerName"
                  label={t('provider.name')}
                  rules={[{ required: true }]}
                >
                  <Input />
                </Form.Item>
                <Form.Item
                  name="providerType"
                  label={t('provider.type')}
                  rules={[{ required: true }]}
                >
                  <Select disabled={mode === 'edit'}>
                    <Select.Option value="OPENAI">OpenAI</Select.Option>
                    <Select.Option value="ANTHROPIC">Anthropic</Select.Option>
                    <Select.Option value="GOOGLE">Google</Select.Option>
                    <Select.Option value="AZURE">Azure</Select.Option>
                    <Select.Option value="DEEPSEEK">DeepSeek</Select.Option>
                    <Select.Option value="QWEN">Qwen</Select.Option>
                    <Select.Option value="ZHIPU">Zhipu</Select.Option>
                    <Select.Option value="MOONSHOT">Moonshot</Select.Option>
                    <Select.Option value="BAICHUAN">Baichuan</Select.Option>
                    <Select.Option value="MINIMAX">MiniMax</Select.Option>
                    <Select.Option value="WENXIN">Wenxin</Select.Option>
                    <Select.Option value="VOLCENGINE">Volcengine</Select.Option>
                    <Select.Option value="TENCENT">Tencent</Select.Option>
                    <Select.Option value="XUNFEI">Xunfei</Select.Option>
                    <Select.Option value="CUSTOM">
                      {t('type.OTHER', { ns: 'providers' })}
                    </Select.Option>
                  </Select>
                </Form.Item>
                <Form.Item name="baseUrl" label={t('provider.baseUrl')}>
                  <Input placeholder="https://api.example.com" />
                </Form.Item>
                <Form.Item
                  name="websiteUrl"
                  label={t('provider.websiteUrl', { defaultValue: '官网地址' })}
                >
                  <Input placeholder="https://example.com" />
                </Form.Item>
                <Form.Item
                  name="apiDocUrl"
                  label={t('provider.apiDocUrl', { defaultValue: 'API 文档' })}
                >
                  <Input placeholder="https://docs.example.com" />
                </Form.Item>
                {mode === 'edit' && (
                  <Form.Item name="state" label={t('provider.state')}>
                    <Select>
                      <Select.Option value="ACTIVE">
                        {t('state.active', { ns: 'common' })}
                      </Select.Option>
                      <Select.Option value="DISABLED">
                        {t('state.disabled', { ns: 'common' })}
                      </Select.Option>
                    </Select>
                  </Form.Item>
                )}
              </>
            ),
          },
        ]}
      />

      {mode === 'create' && step === 'fill-form' && (
        <Button onClick={() => setStep('select-template')} style={{ marginTop: 16 }}>
          {t('actions.back', { ns: 'common', defaultValue: '返回' })}
        </Button>
      )}
    </Form>
  );
}

export type { ProviderBasicInfoTabProps };
