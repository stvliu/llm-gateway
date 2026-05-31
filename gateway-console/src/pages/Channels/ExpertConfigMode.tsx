import { useState } from 'react';
import { Steps, Select, Input, InputNumber, Button, Space, Typography, App, Form } from 'antd';
import { useProviders } from '@/services/query/useProviders';
import { useCreateChannel } from '@/services/query/useChannels';

interface ExpertConfigModeProps {
  onComplete: () => void;
  onCancel: () => void;
}

const PROTOCOL_OPTIONS = [
  { value: 'openai', label: 'OpenAI' },
  { value: 'anthropic', label: 'Anthropic' },
];

/** 每步需要校验的字段 */
const STEP_FIELDS: Record<number, string[]> = {
  0: ['providerId', 'name'],
  1: [],
  2: [],
  3: [],
  4: [],
};

/**
 * 专家配置模式——五步完整配置
 */
export function ExpertConfigMode({ onComplete, onCancel }: ExpertConfigModeProps) {
  const { message } = App.useApp();
  const [step, setStep] = useState(0);
  const [form] = Form.useForm();
  const createChannel = useCreateChannel();
  const { data: providersData } = useProviders({ size: 100 });

  const providers = providersData?.items || [];

  const handleNext = async () => {
    const fields = STEP_FIELDS[step];
    if (fields.length > 0) {
      try {
        await form.validateFields(fields);
      } catch {
        return;
      }
    }
    setStep(s => s + 1);
  };

  const handleFinish = async () => {
    try {
      const values = form.getFieldsValue();
      await createChannel.mutateAsync({
        providerId: values.providerId,
        name: values.name,
        billingMode: values.billingMode || 'PAY_AS_YOU_GO',
        priority: values.priority || 100,
        weight: values.weight || 50,
        endpoints: values.endpointUrl ? [{ protocol: values.protocol || 'openai', endpointUrl: values.endpointUrl }] : [],
        credentials: values.apiKey ? [{ apiKey: values.apiKey, priority: values.credentialPriority || 1, weight: values.credentialWeight || 50 }] : [],
      });
      message.success('渠道创建成功');
      onComplete();
    } catch {
      message.error('渠道创建失败');
    }
  };

  const steps = [
    { title: '基本信息' },
    { title: '端点' },
    { title: 'Key' },
    { title: '模型' },
    { title: '配额' },
  ];

  return (
    <div>
      <Steps current={step} size="small" style={{ marginBottom: 24 }} items={steps} />
      <Form form={form} layout="vertical">

        {step === 0 && (
          <>
            <Form.Item label="供应商" name="providerId" rules={[{ required: true }]}>
              <Select placeholder="选择供应商" options={providers.map(p => ({ label: p.providerName, value: p.id }))} />
            </Form.Item>
            <Form.Item label="渠道名称" name="name" rules={[{ required: true }]}>
              <Input placeholder="输入渠道名称" />
            </Form.Item>
            <Form.Item label="计费模式" name="billingMode" initialValue="PAY_AS_YOU_GO">
              <Select options={[{ label: '按量付费', value: 'PAY_AS_YOU_GO' }, { label: '订阅制', value: 'SUBSCRIPTION' }]} />
            </Form.Item>
            <Space>
              <Form.Item label="优先级" name="priority" initialValue={100}><InputNumber min={1} max={100} /></Form.Item>
              <Form.Item label="权重" name="weight" initialValue={50}><InputNumber min={1} max={100} /></Form.Item>
            </Space>
          </>
        )}

        {step === 1 && (
          <>
            <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>可跳过，后续在渠道详情中添加</Typography.Text>
            <Form.Item label="协议" name="protocol" initialValue="openai">
              <Select options={PROTOCOL_OPTIONS} />
            </Form.Item>
            <Form.Item label="Endpoint URL" name="endpointUrl">
              <Input placeholder="https://api.openai.com/v1" />
            </Form.Item>
          </>
        )}

        {step === 2 && (
          <>
            <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>可跳过，后续在渠道详情中添加</Typography.Text>
            <Form.Item label="API Key" name="apiKey">
              <Input.Password placeholder="sk-..." />
            </Form.Item>
            <Space>
              <Form.Item label="优先级" name="credentialPriority" initialValue={1}><InputNumber min={1} max={10} /></Form.Item>
              <Form.Item label="权重" name="credentialWeight" initialValue={50}><InputNumber min={1} max={100} /></Form.Item>
            </Space>
          </>
        )}

        {step === 3 && (
          <Typography.Text type="secondary">模型选择将在渠道详情中配置，此处可跳过</Typography.Text>
        )}

        {step === 4 && (
          <>
            <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>可跳过，使用默认值</Typography.Text>
            <Space>
              <Form.Item label="RPM 限制" name="rpmLimit"><InputNumber placeholder="500" /></Form.Item>
              <Form.Item label="TPM 限制" name="tpmLimit"><InputNumber placeholder="100000" /></Form.Item>
            </Space>
            <Space>
              <Form.Item label="超时(秒)" name="timeoutSeconds"><InputNumber placeholder="30" /></Form.Item>
              <Form.Item label="最大重试" name="maxRetries"><InputNumber placeholder="3" /></Form.Item>
            </Space>
          </>
        )}
      </Form>

      <div style={{ marginTop: 24, textAlign: 'right' }}>
        <Space>
          {step > 0 && <Button onClick={() => setStep(s => s - 1)}>上一步</Button>}
          {step < 4 ? (
            <Button type="primary" onClick={handleNext}>
              {step === 0 ? '下一步' : '跳过并下一步'}
            </Button>
          ) : (
            <Button type="primary" onClick={handleFinish} loading={createChannel.isPending}>
              创建渠道
            </Button>
          )}
        </Space>
      </div>
    </div>
  );
}
