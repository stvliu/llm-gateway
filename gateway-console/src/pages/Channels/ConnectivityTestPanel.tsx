/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
import { useState } from 'react';
import { Card, Form, Input, Button, Tag, Typography, Space, Divider, Spin } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useTestConnectivity } from '@/services/query/useProviders';
import type { ConnectivityTestResult, ConnectivityTestLevelResult } from '@/types/provider';

const { Text, Title } = Typography;

interface ConnectivityTestPanelProps {
  providerCode: string;
  defaultBaseUrl?: string;
}

/**
 * 预检工具（任务 9.7：原 ConnectivityTestPanel 改名）。
 *
 * <p>用于在创建渠道前独立测试 baseUrl + Key 的可用性（与已有渠道解耦）。
 * 调用 /providers/test-connectivity 端点（非 /channels/{id}/health-check），
 * 后端按 PRECHECK 分支隐式跳过任何持久化。</p>
 */
export function ConnectivityTestPanel({ providerCode, defaultBaseUrl }: ConnectivityTestPanelProps) {
  const { t } = useTranslation('channels');
  const [form] = Form.useForm();
  const testMutation = useTestConnectivity();
  const [result, setResult] = useState<ConnectivityTestResult | null>(null);

  const handleTest = async () => {
    const values = await form.validateFields();
    // PRECHECK 路径不持久化健康字段（后端 /providers/test-connectivity 端点不写 channels.last_*）
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
      },
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
      {/* 任务 9.7：标题 + 副标题，明确"创建渠道前"定位 */}
      <div style={{ marginBottom: 16 }}>
        <Title level={5} style={{ margin: 0 }}>
          {t('precheck.title')}
        </Title>
        <Text type="secondary" style={{ fontSize: 12 }}>
          {t('precheck.subtitle')}
        </Text>
      </div>
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