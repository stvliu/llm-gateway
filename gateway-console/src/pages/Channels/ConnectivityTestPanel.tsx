import { useState } from 'react';
import { Card, Form, Input, Button, Tag, Typography, Space, Divider, Spin } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTestConnectivity } from '@/services/query/useProviders';
import type { ConnectivityTestResult, ConnectivityTestLevelResult } from '@/types/provider';

const { Text } = Typography;

interface ConnectivityTestPanelProps {
  providerCode: string;
  defaultBaseUrl?: string;
}

/**
 * 供应商连通性测试面板
 * 支持两级测试：认证检测 + 模型可用性
 */
export function ConnectivityTestPanel({ providerCode, defaultBaseUrl }: ConnectivityTestPanelProps) {
  const { t } = useTranslation('channels');
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
        onError: () => setResult(null),
      }
    );
  };

  /** 渲染单级测试结果 */
  const renderLevelResult = (_label: string, level: ConnectivityTestLevelResult | null) => {
    if (!level) return null;
    return (
      <Card.Grid style={{ width: '100%', padding: '12px 16px' }}>
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
      </Card.Grid>
    );
  };

  return (
    <div>
      <Form form={form} layout="vertical" initialValues={{ protocol: providerCode === 'anthropic' ? 'anthropic' : 'openai', baseUrl: defaultBaseUrl }}>
        <Form.Item name="protocol" label={t('connectivity.protocol')} rules={[{ required: true }]}>
          <Input disabled placeholder={providerCode === 'anthropic' ? 'Anthropic' : 'OpenAI'} />
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
          <Tag color={result.success ? 'success' : 'error'} style={{ fontSize: 13, padding: '2px 8px' }}>
            {result.success ? t('connectivity.success') : t('connectivity.failed')}
          </Tag>
          {result.message && <Text style={{ marginLeft: 8 }}>{result.message}</Text>}
          <Divider style={{ margin: '12px 0' }} />
          {renderLevelResult(t('connectivity.level1Auth'), result.level1)}
          {renderLevelResult(t('connectivity.level2Model'), result.level2)}
        </div>
      )}

      {testMutation.isPending && !result && (
        <div style={{ textAlign: 'center', padding: 24 }}>
          <Spin indicator={<LoadingOutlined style={{ fontSize: 24 }} />} />
        </div>
      )}
    </div>
  );
}