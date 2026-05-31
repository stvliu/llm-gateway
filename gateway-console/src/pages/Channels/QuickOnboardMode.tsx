import { useState } from 'react';
import { Steps, Select, Input, Checkbox, Button, Space, Typography, App } from 'antd';
import { useProviders } from '@/services/query/useProviders';
import { useCreateChannel } from '@/services/query/useChannels';

interface QuickOnboardModeProps {
  onComplete: () => void;
  onCancel: () => void;
}

/**
 * 快速接入模式——三步完成渠道创建
 */
export function QuickOnboardMode({ onComplete, onCancel }: QuickOnboardModeProps) {
  const { message } = App.useApp();
  const [step, setStep] = useState(0);
  const [selectedProviderId, setSelectedProviderId] = useState<number | undefined>();
  const [apiKey, setApiKey] = useState('');
  const [selectedModels, setSelectedModels] = useState<string[]>([]);

  const { data: providersData } = useProviders({ size: 100 });
  const createChannel = useCreateChannel();

  const providers = providersData?.items || [];

  const handleFinish = async () => {
    if (!selectedProviderId || !apiKey) return;
    const provider = providers.find(p => p.id === selectedProviderId);
    if (!provider) {
      message.error('未找到选中的供应商');
      return;
    }
    try {
      await createChannel.mutateAsync({
        providerId: selectedProviderId,
        name: `${provider.providerName} 渠道`,
        billingMode: 'PAY_AS_YOU_GO',
        priority: 100,
        weight: 50,
        credentials: [{ apiKey, priority: 1, weight: 50 }],
        models: selectedModels.map(m => ({ modelName: m, upstreamModelName: m })),
      });
      message.success('渠道创建成功');
      onComplete();
    } catch {
      message.error('渠道创建失败');
    }
  };

  return (
    <div>
      <Steps current={step} size="small" style={{ marginBottom: 24 }}
        items={[{ title: '选择供应商' }, { title: '粘贴 Key' }, { title: '确认模型' }]} />

      {step === 0 && (
        <div>
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
            选择供应商模板，系统将自动填充端点和模型
          </Typography.Text>
          <Select
            placeholder="选择供应商"
            style={{ width: '100%' }}
            value={selectedProviderId}
            onChange={setSelectedProviderId}
            options={providers.map(p => ({ label: p.providerName, value: p.id }))}
          />
          <div style={{ marginTop: 24, textAlign: 'right' }}>
            <Button disabled={!selectedProviderId} type="primary" onClick={() => setStep(1)}>下一步</Button>
          </div>
        </div>
      )}

      {step === 1 && (
        <div>
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
            粘贴该供应商的 API Key
          </Typography.Text>
          <Input.TextArea
            placeholder="sk-..."
            value={apiKey}
            onChange={e => setApiKey(e.target.value)}
            rows={3}
          />
          <div style={{ marginTop: 24, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setStep(0)}>上一步</Button>
              <Button disabled={!apiKey} type="primary" onClick={() => setStep(2)}>下一步</Button>
            </Space>
          </div>
        </div>
      )}

      {step === 2 && (
        <div>
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
            勾选要启用的模型
          </Typography.Text>
          <Checkbox.Group
            value={selectedModels}
            onChange={v => setSelectedModels(v as string[])}
            options={[
              { label: 'gpt-4o', value: 'gpt-4o' },
              { label: 'gpt-4-turbo', value: 'gpt-4-turbo' },
              { label: 'gpt-3.5-turbo', value: 'gpt-3.5-turbo' },
            ]}
          />
          <div style={{ marginTop: 24, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setStep(1)}>上一步</Button>
              <Button type="primary" onClick={handleFinish}
                loading={createChannel.isPending}>
                完成
              </Button>
            </Space>
          </div>
        </div>
      )}
    </div>
  );
}
