import { useEffect } from 'react';
import { Form, Input, InputNumber, Select, Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateChannel, useUpdateChannel } from '@/services/query/useChannels';
import type { Channel } from '@/types/channel';

interface ChannelFormModalProps {
  open: boolean;
  providerId: number;
  channel?: Channel | null;
  onClose: () => void;
}

/** 渠道创建/编辑弹窗 */
export default function ChannelFormModal({ open, providerId, channel, onClose }: ChannelFormModalProps) {
  const { t } = useTranslation('providers');
  const [form] = Form.useForm();
  const createMutation = useCreateChannel();
  const updateMutation = useUpdateChannel();

  const isEdit = !!channel;

  useEffect(() => {
    if (open) {
      if (channel) {
        form.setFieldsValue({
          name: channel.name,
          billingMode: channel.billingMode,
          priority: channel.priority,
          weight: channel.weight,
          timeout: channel.timeout,
          maxRetries: channel.maxRetries,
          quotaLimit: channel.quotaLimit,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ providerId, priority: 0, weight: 100, billingMode: 'pay_per_call' });
      }
    }
  }, [open, channel, providerId, form]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (isEdit) {
      await updateMutation.mutateAsync({ id: channel!.id, data: values });
    } else {
      await createMutation.mutateAsync({ ...values, providerId });
    }
    onClose();
  };

  return (
    <Modal
      title={isEdit ? t('channel.edit') : t('channel.create')}
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={createMutation.isPending || updateMutation.isPending}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item name="providerId" hidden>
          <Input />
        </Form.Item>
        <Form.Item
          name="name"
          label={t('channel.name')}
          rules={[{ required: true, message: t('channel.nameRequired') as string }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="billingMode" label={t('channel.billingMode')}>
          <Select
            options={[
              { value: 'pay_per_call', label: t('channel.billingPayPerCall') },
              { value: 'subscription', label: t('channel.billingSubscription') },
            ]}
          />
        </Form.Item>
        <Form.Item name="quotaLimit" label={t('channel.quotaLimit')}>
          <InputNumber min={0} style={{ width: '100%' }} placeholder={t('channel.quotaLimitPlaceholder') as string} />
        </Form.Item>
        <Form.Item name="priority" label={t('channel.priority')}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="weight" label={t('channel.weight')}>
          <InputNumber min={1} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="timeout" label={t('channel.timeout')}>
          <InputNumber min={0} style={{ width: '100%' }} addonAfter="ms" />
        </Form.Item>
        <Form.Item name="maxRetries" label={t('channel.maxRetries')}>
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
}