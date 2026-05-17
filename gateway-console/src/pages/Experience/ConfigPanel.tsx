import { useState, useEffect } from 'react';
import { Form, Select, Input, Button, Space, message } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { providerApi } from '@/services/api/provider';
import type { ProviderType } from '@/types/api';

interface ConfigPanelProps {
  providerType: ProviderType | null;
  apiKey: string;
  baseUrl: string;
  model: string;
  onConfigChange: (config: {
    providerType: ProviderType;
    apiKey: string;
    baseUrl?: string;
    model: string;
  }) => void;
  disabled?: boolean;
}

interface ProviderTypeOption {
  value: string;
  label: string;
}

/**
 * 配置面板组件
 *
 * 选择供应商、API Key、模型等配置。
 */
export function ConfigPanel({
  providerType,
  apiKey,
  baseUrl,
  model,
  onConfigChange,
  disabled,
}: ConfigPanelProps) {
  const { t } = useTranslation('experience');
  const [form] = Form.useForm();
  const [providerTypes, setProviderTypes] = useState<ProviderTypeOption[]>([]);
  const [models, setModels] = useState<string[]>([]);
  const [loadingTypes, setLoadingTypes] = useState(true);
  const [loadingModels, setLoadingModels] = useState(false);
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<'success' | 'error' | null>(null);

  // 获取供应商类型列表
  useEffect(() => {
    const fetchProviderTypes = async () => {
      try {
        const types = await providerApi.getProviderTypes();
        setProviderTypes(types);
      } catch {
        // 使用默认列表
        setProviderTypes([
          { value: 'OPENAI', label: 'OpenAI' },
          { value: 'ANTHROPIC', label: 'Anthropic' },
          { value: 'DEEPSEEK', label: 'DeepSeek' },
          { value: 'MOONSHOT', label: 'Moonshot' },
          { value: 'ZHIPU', label: '智谱 GLM' },
          { value: 'BAICHUAN', label: '百川智能' },
          { value: 'MINIMAX', label: 'MiniMax' },
          { value: 'VOLCENGINE', label: '火山引擎' },
          { value: 'QWEN', label: '通义千问' },
          { value: 'GEMINI', label: 'Google Gemini' },
        ]);
      } finally {
        setLoadingTypes(false);
      }
    };
    fetchProviderTypes();
  }, []);

  // 测试连通性并获取模型列表
  const handleTest = async () => {
    const values = form.getFieldsValue();
    if (!values.providerType || !values.apiKey) {
      message.warning(t('config.testRequired'));
      return;
    }

    setTesting(true);
    setTestResult(null);
    setLoadingModels(true);

    try {
      const result = await providerApi.testConnectivity({
        providerType: values.providerType,
        apiKey: values.apiKey,
        baseUrl: values.baseUrl || undefined,
      });

      setTestResult(result.success ? 'success' : 'error');

      if (result.success) {
        message.success(t('config.testSuccess'));

        // 使用 Level 1 返回的模型列表
        if (result.models && result.models.length > 0) {
          setModels(result.models);
          // 自动选择第一个模型
          if (!model && result.models[0]) {
            form.setFieldValue('model', result.models[0]);
            onConfigChange({
              providerType: values.providerType as ProviderType,
              apiKey: values.apiKey,
              baseUrl: values.baseUrl,
              model: result.models[0],
            });
          }
        }
      } else {
        message.error(result.message || t('config.testFailed'));
      }
    } catch (error) {
      setTestResult('error');
      message.error(t('config.testError'));
    } finally {
      setTesting(false);
      setLoadingModels(false);
    }
  };

  // 表单值变化
  const handleValuesChange = (changed: Record<string, unknown>) => {
    // 切换供应商时重置模型列表和测试结果
    if (changed.providerType) {
      setModels([]);
      setTestResult(null);
      form.setFieldValue('model', undefined);
    }

    // 配置变化时通知父组件
    const values = form.getFieldsValue();
    if (values.providerType && values.apiKey && values.model) {
      onConfigChange({
        providerType: values.providerType as ProviderType,
        apiKey: values.apiKey,
        baseUrl: values.baseUrl,
        model: values.model,
      });
    }
  };

  // 同步外部值到表单
  useEffect(() => {
    form.setFieldsValue({
      providerType,
      apiKey,
      baseUrl,
      model,
    });
  }, [providerType, apiKey, baseUrl, model, form]);

  return (
    <Form
      form={form}
      layout="vertical"
      disabled={disabled}
      onValuesChange={handleValuesChange}
      initialValues={{ providerType, apiKey, baseUrl, model }}
    >
      <Form.Item name="providerType" label={t('config.providerType')} rules={[{ required: true }]}>
        <Select
          loading={loadingTypes}
          placeholder={t('config.selectProvider')}
          options={providerTypes}
        />
      </Form.Item>

      <Form.Item name="apiKey" label={t('config.apiKey')} rules={[{ required: true }]}>
        <Input.Password placeholder={t('config.apiKeyPlaceholder')} />
      </Form.Item>

      <Form.Item name="baseUrl" label={t('config.baseUrl')}>
        <Input placeholder={t('config.baseUrlPlaceholder')} />
      </Form.Item>

      <Form.Item>
        <Space>
          <Button
            type="primary"
            onClick={handleTest}
            loading={testing}
            icon={
              testResult === 'success' ? (
                <CheckCircleOutlined />
              ) : testResult === 'error' ? (
                <CloseCircleOutlined />
              ) : testing ? (
                <LoadingOutlined />
              ) : undefined
            }
            style={
              testResult === 'success'
                ? { backgroundColor: '#52c41a', borderColor: '#52c41a' }
                : testResult === 'error'
                  ? { backgroundColor: '#ff4d4f', borderColor: '#ff4d4f' }
                  : undefined
            }
          >
            {testing ? t('config.testing') : t('config.testConnection')}
          </Button>
        </Space>
      </Form.Item>

      <Form.Item name="model" label={t('config.model')} rules={[{ required: true }]}>
        <Select
          loading={loadingModels}
          placeholder={t('config.selectModel')}
          showSearch
          filterOption={(input, option) =>
            (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
          }
          options={models.map((m) => ({ value: m, label: m }))}
        />
      </Form.Item>
    </Form>
  );
}
