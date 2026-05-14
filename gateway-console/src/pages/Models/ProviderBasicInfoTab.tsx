import { useState, useEffect, useCallback, forwardRef, useImperativeHandle } from 'react';
import {
  Descriptions,
  Form,
  Input,
  Select,
  Tag,
  Space,
  Collapse,
} from 'antd';
import {
  SettingOutlined,
  GlobalOutlined,
  LinkOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { StatusIndicator } from '@/components/common';
import {
  useUpdateProvider,
} from '@/services/query';
import type { Provider, UpdateProviderRequest } from '@/types/provider';

export interface ProviderBasicInfoTabHandle {
  submit: () => Promise<boolean>;
  resetFields: () => void;
}

interface ProviderBasicInfoTabProps {
  provider: Provider | null;
  editing: boolean;
  onDirtyChange?: (dirty: boolean) => void;
}

/**
 * 供应商基本信息标签页（模型页面内嵌）
 * 查看模式显示 Descriptions，编辑模式显示 Form
 * 操作按钮由父组件在标题栏渲染
 */
export const ProviderBasicInfoTab = forwardRef<ProviderBasicInfoTabHandle, ProviderBasicInfoTabProps>(
  function ProviderBasicInfoTab({
    provider,
    editing,
    onDirtyChange,
  }, ref) {
    const { t } = useTranslation('models');

    const [form] = Form.useForm();
    const [dirty, setDirty] = useState(false);

    const updateMutation = useUpdateProvider();

    // 初始化表单值
    useEffect(() => {
      if (provider) {
        form.setFieldsValue(provider);
      }
    }, [provider, form]);

    // 编辑模式切换时重置 dirty
    useEffect(() => {
      if (editing) {
        setDirty(false);
        onDirtyChange?.(false);
      }
    }, [editing, onDirtyChange]);

    // 表单值变化时追踪 dirty 状态
    const handleValuesChange = useCallback(() => {
      if (!dirty) {
        setDirty(true);
        onDirtyChange?.(true);
      }
    }, [dirty, onDirtyChange]);

    // 暴露提交和重置方法给父组件
    useImperativeHandle(ref, () => ({
      submit: async () => {
        if (!provider) return false;

        try {
          const values = await form.validateFields();
          const request: UpdateProviderRequest = {
            providerName: values.providerName,
            baseUrl: values.baseUrl,
            websiteUrl: values.websiteUrl,
            apiDocUrl: values.apiDocUrl,
            state: values.state,
          };

          await updateMutation.mutateAsync({ id: provider.id, data: request });
          setDirty(false);
          onDirtyChange?.(false);
          return true;
        } catch {
          return false;
        }
      },
      resetFields: () => {
        if (provider) {
          form.setFieldsValue(provider);
        }
        setDirty(false);
        onDirtyChange?.(false);
      },
    }), [provider, form, updateMutation, dirty, onDirtyChange]);

    // 查看模式：Descriptions
    if (!editing && provider) {
      return (
        <>
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
        </>
      );
    }

    // 编辑模式：Form
    return (
      <Form
        form={form}
        layout="vertical"
        onValuesChange={handleValuesChange}
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
                    <Select disabled>
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
                </>
              ),
            },
          ]}
        />
      </Form>
    );
  },
);

export type { ProviderBasicInfoTabProps };