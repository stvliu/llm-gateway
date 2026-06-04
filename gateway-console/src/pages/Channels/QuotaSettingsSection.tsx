import { useState } from 'react';
import { Descriptions, InputNumber, Input, Button, Space, Form, message } from 'antd';
import type { Channel, UpdateChannelRequest } from '@/types/channel';
import { useUpdateChannel } from '@/services/query/useChannels';

interface QuotaSettingsSectionProps {
  channel: Channel;
}

/**
 * 配额与设置区组件
 * 展示渠道的配额和配置信息，支持编辑
 */
export function QuotaSettingsSection({ channel }: QuotaSettingsSectionProps) {
  const [editing, setEditing] = useState(false);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const updateChannel = useUpdateChannel();

  /** 格式化显示值 */
  const formatValue = (value: number | null | undefined, unit?: string) => {
    if (value === null || value === undefined) return '未设置';
    return unit ? `${value}${unit}` : String(value);
  };

  /** 开始编辑 */
  const handleStartEdit = () => {
    form.setFieldsValue({
      quotaLimit: channel.quotaLimit,
      timeout: channel.timeout,
      maxRetries: channel.maxRetries,
    });
    setEditing(true);
  };

  /** 取消编辑 */
  const handleCancel = () => {
    setEditing(false);
    form.resetFields();
  };

  /** 保存编辑 */
  const handleSave = async () => {
    try {
      setLoading(true);
      const values = await form.validateFields();
      const data: UpdateChannelRequest = {
        quotaLimit: values.quotaLimit,
        timeout: values.timeout,
        maxRetries: values.maxRetries,
      };
      await updateChannel.mutateAsync({ id: channel.id, data });
      message.success('设置更新成功');
      setEditing(false);
    } catch (error) {
      message.error('设置更新失败');
    } finally {
      setLoading(false);
    }
  };

  if (editing) {
    return (
      <Form form={form} layout="vertical">
        <Form.Item label="RPM（每分钟请求数）" name="rpm">
          <InputNumber min={1} style={{ width: '100%' }} placeholder="暂不支持" disabled />
        </Form.Item>
        <Form.Item label="TPM（每分钟 Token 数）" name="tpm">
          <InputNumber min={1} style={{ width: '100%' }} placeholder="暂不支持" disabled />
        </Form.Item>
        <Form.Item label="配额上限" name="quotaLimit">
          <InputNumber min={0} style={{ width: '100%' }} placeholder="不限制" />
        </Form.Item>
        <Form.Item label="超时（毫秒）" name="timeout">
          <InputNumber min={1000} style={{ width: '100%' }} placeholder="使用默认值" />
        </Form.Item>
        <Form.Item label="重试次数" name="maxRetries">
          <InputNumber min={0} max={5} style={{ width: '100%' }} placeholder="使用默认值" />
        </Form.Item>
        <Form.Item label="自定义 Header">
          <Input style={{ width: '100%' }} placeholder="暂不支持" disabled />
        </Form.Item>
        <Space>
          <Button type="primary" onClick={handleSave} loading={loading}>
            保存
          </Button>
          <Button onClick={handleCancel}>取消</Button>
        </Space>
      </Form>
    );
  }

  return (
    <div>
      <Descriptions column={1} size="small">
        <Descriptions.Item label="RPM（每分钟请求数）">
          暂不支持
        </Descriptions.Item>
        <Descriptions.Item label="TPM（每分钟 Token 数）">
          暂不支持
        </Descriptions.Item>
        <Descriptions.Item label="配额上限">
          {formatValue(channel.quotaLimit)}
        </Descriptions.Item>
        <Descriptions.Item label="超时">
          {formatValue(channel.timeout, 'ms')}
        </Descriptions.Item>
        <Descriptions.Item label="重试次数">
          {formatValue(channel.maxRetries)}
        </Descriptions.Item>
        <Descriptions.Item label="自定义 Header">
          暂不支持
        </Descriptions.Item>
      </Descriptions>
      <Button type="link" style={{ padding: 0, marginTop: 12 }} onClick={handleStartEdit}>
        编辑设置
      </Button>
    </div>
  );
}