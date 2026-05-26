import { useState } from 'react';
import { Form, Input, Select, Button, Collapse, Tag, Alert, Space, Typography } from 'antd';
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
  const { t } = useTranslation('providers');
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
              {level.success ? t('detail.success', { defaultValue: '成功' }) : t('detail.failed', { defaultValue: '失败' })}
            </Tag>
            {level.latencyMs !== null && <span>{t('detail.latency', { defaultValue: '延迟' })}: {level.latencyMs}ms</span>}
          </div>
          {level.message && <Typography.Text type={level.success ? 'success' : 'danger'}>{level.message}</Typography.Text>}
          {level.models && level.models.length > 0 && (
            <div>
              <Typography.Text strong>可用模型:</Typography.Text>
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
        <Form.Item name="protocol" label={t('channel.protocol', { defaultValue: '协议' })} rules={[{ required: true }]}>
          <Select options={[{ value: 'openai', label: 'OpenAI' }, { value: 'anthropic', label: 'Anthropic' }]} />
        </Form.Item>
        <Form.Item name="baseUrl" label="Base URL">
          <Input placeholder="https://api.openai.com" />
        </Form.Item>
        <Form.Item name="apiKey" label={t('credential.apiKey', { defaultValue: 'API Key' })} rules={[{ required: true }]}>
          <Input.Password placeholder="sk-..." />
        </Form.Item>
        <Form.Item name="model" label="Model">
          <Input placeholder="gpt-4o (optional)" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" onClick={handleTest} loading={testMutation.isPending}>
            {t('detail.runTest', { defaultValue: '运行测试' })}
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
            {renderLevelResult(t('detail.level1Auth', { defaultValue: '认证检测' }), result.level1)}
            {renderLevelResult(t('detail.level2Model', { defaultValue: '模型可用性' }), result.level2)}
          </Collapse>
        </div>
      )}
    </div>
  );
}
