import { useState } from 'react';
import { Form, Input, Select, Button, Collapse, Tag, Alert, Space, Typography } from 'antd';
import { App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useTestConnectivity } from '@/services/query/useProviders';
import type { ConnectivityTestResult, ConnectivityTestLevelResult } from '@/types/provider';

interface ConnectivityTestPanelProps {
  providerCode: string;
  defaultBaseUrl?: string;
}

/**
 * 连通性测试面板
 * 支持两级测试：认证检测 + 模型可用性
 */
export default function ConnectivityTestPanel({ providerCode, defaultBaseUrl }: ConnectivityTestPanelProps) {
  const { t } = useTranslation('channels');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const testMutation = useTestConnectivity();
  const [result, setResult] = useState<ConnectivityTestResult | null>(null);

  const handleTest = async () => {
    const values = await form.validateFields();
    testMutation.mutate(
      {
        protocolName: values.protocol,
        baseUrl: values.baseUrl,
        apiKey: values.apiKey,
        model: values.model,
      },
      {
        onSuccess: (data) => setResult(data),
        onError: () => message.error(t('connectivity.testFailed')),
      }
    );
  };

  /** 渲染单级测试结果 */
  const renderLevelResult = (label: string, level: ConnectivityTestLevelResult | null) => {
    if (!level) return null;
    return (
      <Collapse.Panel key={label} header={label}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            <Tag color={level.success ? 'success' : 'error'}>
              {level.success ? t('connectivity.success') : t('connectivity.failed')}
            </Tag>
            {level.latencyMs !== null && <span>{t('connectivity.latency')}: {level.latencyMs}ms</span>}
          </div>
          {level.message && <Typography.Text type={level.success ? 'success' : 'danger'}>{level.message}</Typography.Text>}
          {level.models && level.models.length > 0 && (
            <div>
              <Typography.Text strong>{t('connectivity.availableModels')}:</Typography.Text>
              <div style={{ marginTop: 4 }}>
                {level.models.map(m => <Tag key={m}>{m}</Tag>)}
              </div>
            </div>
          )}
        </Space>
      </Collapse.Panel>
    );
  };

  return (
    <div style={{ marginTop: 16 }}>
      <Form form={form} layout="vertical" initialValues={{ protocol: providerCode === 'anthropic' ? 'anthropic' : 'openai', baseUrl: defaultBaseUrl }}>
        <Form.Item name="protocol" label={t('connectivity.protocol')} rules={[{ required: true }]}>
          <Select options={[{ value: 'openai', label: 'OpenAI' }, { value: 'anthropic', label: 'Anthropic' }]} />
        </Form.Item>
        <Form.Item name="baseUrl" label={t('connectivity.baseUrl')}>
          <Input placeholder={t('connectivity.baseUrlPlaceholder')} />
        </Form.Item>
        <Form.Item name="apiKey" label={t('connectivity.apiKey')} rules={[{ required: true }]}>
          <Input.Password placeholder="sk-..." />
        </Form.Item>
        <Form.Item name="model" label={t('connectivity.model')}>
          <Input placeholder={t('connectivity.modelPlaceholder')} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" onClick={handleTest} loading={testMutation.isPending}>
            {t('connectivity.runTest')}
          </Button>
        </Form.Item>
      </Form>

      {result && (
        <div style={{ marginTop: 16 }}>
          <Alert
            type={result.success ? 'success' : 'error'}
            message={result.message}
            style={{ marginBottom: 12 }}
          />
          <Collapse>
            {renderLevelResult(t('connectivity.level1Auth'), result.level1)}
            {renderLevelResult(t('connectivity.level2Model'), result.level2)}
          </Collapse>
        </div>
      )}
    </div>
  );
}