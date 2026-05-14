import { useState, useEffect, useCallback, forwardRef, useImperativeHandle } from 'react';
import {
  Form,
  Input,
  Select,
} from 'antd';
import { useTranslation } from 'react-i18next';
import {
  useUpdateProvider,
} from '@/services/query';
import type { Provider, UpdateProviderRequest } from '@/types/provider';

/**
 * 检查供应商名称是否重复
 */
function isNameDuplicate(name: string, providers: Provider[], excludeId?: number): boolean {
  return providers.some(p =>
    p.id !== excludeId && p.providerName.toLowerCase() === name.toLowerCase()
  );
}

/**
 * 检查 API URL 是否重复
 */
function isBaseUrlDuplicate(baseUrl: string, providers: Provider[], excludeId?: number): boolean {
  const normalizedUrl = baseUrl.toLowerCase().replace(/\/+$/, '');
  return providers.some(p =>
    p.id !== excludeId && p.baseUrl?.toLowerCase().replace(/\/+$/, '') === normalizedUrl
  );
}

export interface ProviderBasicInfoTabHandle {
  submit: () => Promise<boolean>;
  resetFields: () => void;
}

interface ProviderBasicInfoTabProps {
  provider: Provider | null;
  providers: Provider[];
  editing: boolean;
  onDirtyChange?: (dirty: boolean) => void;
}

/**
 * 供应商基本信息标签页
 * 查看模式显示只读表单，编辑模式显示可编辑表单
 * 操作按钮由父组件在标题栏渲染
 */
export const ProviderBasicInfoTab = forwardRef<ProviderBasicInfoTabHandle, ProviderBasicInfoTabProps>(
  function ProviderBasicInfoTab({
    provider,
    providers,
    editing,
    onDirtyChange,
  }, ref) {
    const { t } = useTranslation('providers');

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
          };

          // 检查名称唯一性
          if (request.providerName && isNameDuplicate(request.providerName, providers, provider.id)) {
            return false;
          }

          // 检查 URL 噯一性
          if (request.baseUrl && isBaseUrlDuplicate(request.baseUrl, providers, provider.id)) {
            return false;
          }

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
    }), [provider, providers, form, updateMutation, dirty, onDirtyChange]);

    // 查看模式：只读表单
    if (!editing) {
      return (
        <Form
          form={form}
          layout="vertical"
          disabled
        >
          <Form.Item name="providerType" label={t('provider.type')}>
            <Select>
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
          <Form.Item name="providerName" label={t('provider.name')}>
            <Input />
          </Form.Item>
          <Form.Item name="baseUrl" label={t('provider.baseUrl')}>
            <Input />
          </Form.Item>
          <Form.Item name="websiteUrl" label={t('provider.websiteUrl', { defaultValue: '官网地址' })}>
            <Input />
          </Form.Item>
          <Form.Item name="apiDocUrl" label={t('provider.apiDocUrl', { defaultValue: 'API 文档' })}>
            <Input />
          </Form.Item>
          <Form.Item name="state" label={t('provider.state')}>
            <Select>
              <Select.Option value="ACTIVE">{t('state.active', { ns: 'common' })}</Select.Option>
              <Select.Option value="DISABLED">{t('state.disabled', { ns: 'common' })}</Select.Option>
            </Select>
          </Form.Item>
          {provider && (
            <>
              <Form.Item label={t('detail.createdAt')}>
                <Input disabled value={new Date(provider.createdAt).toLocaleString()} />
              </Form.Item>
              <Form.Item label={t('detail.updatedAt')}>
                <Input disabled value={new Date(provider.updatedAt).toLocaleString()} />
              </Form.Item>
            </>
          )}
        </Form>
      );
    }

    // 编辑模式：可编辑表单
    return (
      <Form
        form={form}
        layout="vertical"
        onValuesChange={handleValuesChange}
      >
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
        <Form.Item
          name="providerName"
          label={t('provider.name')}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="baseUrl" label={t('provider.baseUrl')}>
          <Input placeholder="https://api.example.com" />
        </Form.Item>
        <Form.Item name="websiteUrl" label={t('provider.websiteUrl', { defaultValue: '官网地址' })}>
          <Input placeholder="https://example.com" />
        </Form.Item>
        <Form.Item name="apiDocUrl" label={t('provider.apiDocUrl', { defaultValue: 'API 文档' })}>
          <Input placeholder="https://docs.example.com" />
        </Form.Item>
        <Form.Item name="state" label={t('provider.state')}>
          <Select>
            <Select.Option value="ACTIVE">{t('state.active', { ns: 'common' })}</Select.Option>
            <Select.Option value="DISABLED">{t('state.disabled', { ns: 'common' })}</Select.Option>
          </Select>
        </Form.Item>
      </Form>
    );
  },
);

export type { ProviderBasicInfoTabProps };