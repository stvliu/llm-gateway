import { useState } from 'react';
import { Descriptions, InputNumber, Input, Button, Space, Form, message } from 'antd';
import { useTranslation } from 'react-i18next';
import type { Channel, UpdateChannelRequest } from '@/types/channel';
import { useUpdateChannel } from '@/services/query/useChannels';
import { extractErrorMessage } from '@/utils/errorMessage';

interface QuotaSettingsSectionProps {
  channel: Channel;
}

/**
 * 配额与设置区组件
 * 展示渠道的配额和配置信息，支持编辑
 */
export function QuotaSettingsSection({ channel }: QuotaSettingsSectionProps) {
  const { t } = useTranslation('channels');
  const [editing, setEditing] = useState(false);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const updateChannel = useUpdateChannel();

  /** 格式化显示值 */
  const formatValue = (value: number | null | undefined, unit?: string) => {
    if (value === null || value === undefined) return t('quota.notSet');
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
      message.success(t('quota.updateSuccess'));
      setEditing(false);
    } catch (err) {
      // 校验失败 → AntD 行内已显示，不再弹 toast；其它错误带后端原因输出
      const reason = extractErrorMessage(err);
      if (!reason) {
        return;
      }
      message.error(t('common:message.saveFailed', { reason }));
    } finally {
      setLoading(false);
    }
  };

  if (editing) {
    return (
      <Form form={form} layout="vertical">
        <Form.Item label={t('quota.rpm')} name="rpm">
          <InputNumber min={1} style={{ width: '100%' }} placeholder={t('quota.notSupported')} disabled />
        </Form.Item>
        <Form.Item label={t('quota.tpm')} name="tpm">
          <InputNumber min={1} style={{ width: '100%' }} placeholder={t('quota.notSupported')} disabled />
        </Form.Item>
        <Form.Item label={t('quota.quotaLimit')} name="quotaLimit">
          <InputNumber min={0} style={{ width: '100%' }} placeholder={t('quota.unlimited')} />
        </Form.Item>
        <Form.Item label={t('quota.timeoutMs')} name="timeout">
          <InputNumber min={1000} style={{ width: '100%' }} placeholder={t('quota.useDefault')} />
        </Form.Item>
        <Form.Item label={t('quota.maxRetries')} name="maxRetries">
          <InputNumber min={0} max={5} style={{ width: '100%' }} placeholder={t('quota.useDefault')} />
        </Form.Item>
        <Form.Item label={t('quota.customHeader')}>
          <Input style={{ width: '100%' }} placeholder={t('quota.notSupported')} disabled />
        </Form.Item>
        <Space>
          <Button type="primary" onClick={handleSave} loading={loading}>
            {t('drawer.save')}
          </Button>
          <Button onClick={handleCancel}>{t('drawer.cancel')}</Button>
        </Space>
      </Form>
    );
  }

  return (
    <div>
      <Descriptions column={1} size="small">
        <Descriptions.Item label={t('quota.rpm')}>
          {t('quota.notSupported')}
        </Descriptions.Item>
        <Descriptions.Item label={t('quota.tpm')}>
          {t('quota.notSupported')}
        </Descriptions.Item>
        <Descriptions.Item label={t('quota.quotaLimit')}>
          {formatValue(channel.quotaLimit)}
        </Descriptions.Item>
        <Descriptions.Item label={t('quota.timeout')}>
          {formatValue(channel.timeout, 'ms')}
        </Descriptions.Item>
        <Descriptions.Item label={t('quota.maxRetries')}>
          {formatValue(channel.maxRetries)}
        </Descriptions.Item>
        <Descriptions.Item label={t('quota.customHeader')}>
          {t('quota.notSupported')}
        </Descriptions.Item>
      </Descriptions>
      <Button type="link" style={{ padding: 0, marginTop: 12 }} onClick={handleStartEdit}>
        {t('quota.editSettings')}
      </Button>
    </div>
  );
}
