import { useState, useCallback } from 'react';
import {
  Card,
  Form,
  InputNumber,
  Button,
  App,
  Empty,
  Typography,
  Space,
  Spin,
} from 'antd';
import { SaveOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useChannels, useUpdateChannel } from '@/services/query/useChannels';
import type { Provider } from '@/types/provider';
import type { Channel } from '@/types/channel';

const { Text } = Typography;

interface Props {
  provider: Provider | null;
}

/** 渠道配额表单字段 */
interface ChannelQuotaForm {
  rpmLimit: number | null;
  tpmLimit: number | null;
  quotaLimit: number | null;
  priority: number;
  weight: number;
}

/**
 * 专家模式 - 限流与配额标签页
 * 用于配置每个渠道的 RPM/TPM 限流和 Token 配额
 */
export default function ExpertQuotaTab({ provider }: Props) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();

  // 数据查询与变更
  const { data: channels, isLoading } = useChannels(provider?.id ?? 0);
  const updateChannelMutation = useUpdateChannel();

  // 表单状态：使用 Map 存储每个渠道的表单实例值
  const [formValues, setFormValues] = useState<Map<number, ChannelQuotaForm>>(new Map());
  const [savingChannels, setSavingChannels] = useState<Set<number>>(new Set());

  // 更新表单值
  const handleFormChange = useCallback((channelId: number, field: keyof ChannelQuotaForm, value: number | null) => {
    setFormValues((prev) => {
      const next = new Map(prev);
      const current = next.get(channelId) || {
        rpmLimit: null,
        tpmLimit: null,
        quotaLimit: null,
        priority: 1,
        weight: 100,
      };
      next.set(channelId, { ...current, [field]: value });
      return next;
    });
  }, []);

  // 获取渠道的表单值（优先使用用户输入，否则使用原始数据）
  const getFormValue = useCallback((channel: Channel, field: keyof ChannelQuotaForm): number | null | undefined => {
    const formValue = formValues.get(channel.id);
    if (formValue && formValue[field] !== undefined) {
      return formValue[field];
    }
    // quotaLimit、priority、weight 从 channel 获取，rpmLimit/tpmLimit 暂无后端字段
    if (field === 'quotaLimit') return channel.quotaLimit;
    if (field === 'priority') return channel.priority;
    if (field === 'weight') return channel.weight;
    return null;
  }, [formValues]);

  // 保存渠道配置
  const handleSave = useCallback(async (channel: Channel) => {
    const formValue = formValues.get(channel.id);
    setSavingChannels((prev) => new Set(prev).add(channel.id));

    try {
      await updateChannelMutation.mutateAsync({
        id: channel.id,
        data: {
          quotaLimit: formValue?.quotaLimit ?? channel.quotaLimit,
          priority: formValue?.priority ?? channel.priority,
          weight: formValue?.weight ?? channel.weight,
          // RPM/TPM 暂无后端字段，待后续扩展
        },
      });
      message.success(t('quota.saveSuccess', { defaultValue: '配额配置保存成功' }));
    } catch {
      message.error(t('quota.saveFailed', { defaultValue: '保存失败，请重试' }));
    } finally {
      setSavingChannels((prev) => {
        const next = new Set(prev);
        next.delete(channel.id);
        return next;
      });
    }
  }, [formValues, updateChannelMutation, message, t]);

  // 空状态
  if (!provider) {
    return (
      <Empty
        description={t('noProviderData', { defaultValue: '暂无供应商数据' })}
        style={{ padding: '40px 0' }}
      />
    );
  }

  // 加载状态
  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}>
        <Spin />
      </div>
    );
  }

  // 无渠道状态
  if (!channels || channels.length === 0) {
    return (
      <div>
        <Text strong style={{ fontSize: 16 }}>
          {t('quota.title', { defaultValue: '限流与配额' })}
        </Text>
        <div style={{ marginTop: 16, color: '#64748b', fontSize: 13 }}>
          {t('quota.desc', { defaultValue: '配置 RPM/TPM 限流和 Token 配额。' })}
        </div>
        <Empty
          description={t('quota.noChannels', { defaultValue: '暂无渠道，请先创建渠道' })}
          style={{ marginTop: 24 }}
        />
      </div>
    );
  }

  return (
    <div>
      {/* 标题区域 */}
      <Text strong style={{ fontSize: 16 }}>
        {t('quota.title', { defaultValue: '限流与配额' })}
      </Text>
      <div style={{ marginTop: 8, marginBottom: 24, color: '#64748b', fontSize: 13 }}>
        {t('quota.desc', { defaultValue: '配置 RPM/TPM 限流和 Token 配额。' })}
      </div>

      {/* 渠道卡片列表 */}
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {channels.map((channel) => (
          <Card
            key={channel.id}
            title={
              <Space>
                <Text strong>{channel.name}</Text>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  ({channel.providerName})
                </Text>
              </Space>
            }
            size="small"
            extra={
              <Button
                type="primary"
                size="small"
                icon={<SaveOutlined />}
                loading={savingChannels.has(channel.id)}
                onClick={() => handleSave(channel)}
              >
                {t('actions.save', { defaultValue: '保存' })}
              </Button>
            }
          >
            <Form layout="vertical" style={{ marginBottom: 0 }}>
              {/* 第一行：RPM 和 TPM */}
              <div style={{ display: 'flex', gap: 24 }}>
                <Form.Item
                  label={t('quota.rpmLimit', { defaultValue: 'RPM 限制' })}
                  style={{ flex: 1, marginBottom: 16 }}
                  help={t('quota.rpmLimitHelp', { defaultValue: '每分钟最大请求数 (Requests Per Minute)' })}
                >
                  <InputNumber
                    style={{ width: '100%' }}
                    min={1}
                    placeholder={t('quota.rpmLimitPlaceholder', { defaultValue: '例如: 60' })}
                    value={getFormValue(channel, 'rpmLimit') as number | undefined}
                    onChange={(value) => handleFormChange(channel.id, 'rpmLimit', value)}
                  />
                </Form.Item>

                <Form.Item
                  label={t('quota.tpmLimit', { defaultValue: 'TPM 限制' })}
                  style={{ flex: 1, marginBottom: 16 }}
                  help={t('quota.tpmLimitHelp', { defaultValue: '每分钟最大 Token 数 (Tokens Per Minute)' })}
                >
                  <InputNumber
                    style={{ width: '100%' }}
                    min={1}
                    placeholder={t('quota.tpmLimitPlaceholder', { defaultValue: '例如: 100000' })}
                    value={getFormValue(channel, 'tpmLimit') as number | undefined}
                    onChange={(value) => handleFormChange(channel.id, 'tpmLimit', value)}
                  />
                </Form.Item>
              </div>

              {/* 第二行：Token 配额 */}
              <Form.Item
                label={t('channel.quotaLimit', { defaultValue: 'Token 配额上限' })}
                style={{ marginBottom: 16 }}
                help={t('quota.quotaLimitHelp', { defaultValue: '渠道总 Token 配额，超出后将拒绝请求。留空则不限制' })}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={0}
                  placeholder={t('channel.quotaLimitPlaceholder', { defaultValue: '不填则不限配额' })}
                  value={getFormValue(channel, 'quotaLimit') as number | null | undefined}
                  onChange={(value) => handleFormChange(channel.id, 'quotaLimit', value)}
                />
              </Form.Item>

              {/* 第三行：优先级和权重 */}
              <div style={{ display: 'flex', gap: 24 }}>
                <Form.Item
                  label={t('channel.priority', { defaultValue: '优先级' })}
                  style={{ flex: 1, marginBottom: 0 }}
                  help={t('quota.priorityHelp', { defaultValue: '数值越大优先级越高，范围 1-100' })}
                >
                  <InputNumber
                    style={{ width: '100%' }}
                    min={1}
                    max={100}
                    value={getFormValue(channel, 'priority') as number}
                    onChange={(value) => handleFormChange(channel.id, 'priority', value ?? 1)}
                  />
                </Form.Item>

                <Form.Item
                  label={t('channel.weight', { defaultValue: '权重' })}
                  style={{ flex: 1, marginBottom: 0 }}
                  help={t('quota.weightHelp', { defaultValue: '负载均衡权重，范围 1-100' })}
                >
                  <InputNumber
                    style={{ width: '100%' }}
                    min={1}
                    max={100}
                    value={getFormValue(channel, 'weight') as number}
                    onChange={(value) => handleFormChange(channel.id, 'weight', value ?? 100)}
                  />
                </Form.Item>
              </div>
            </Form>
          </Card>
        ))}
      </Space>
    </div>
  );
}
