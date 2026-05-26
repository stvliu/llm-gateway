import { useEffect } from 'react';
import { Form, Input, Select, Modal } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAddChannelEndpoint } from '@/services/query/useChannels';

interface ChannelEndpointFormModalProps {
  open: boolean;
  channelId: number;
  onClose: () => void;
}

/** 渠道端点创建弹窗 */
export default function ChannelEndpointFormModal({ open, channelId, onClose }: ChannelEndpointFormModalProps) {
  const { t } = useTranslation('providers');
  const [form] = Form.useForm();
  const addMutation = useAddChannelEndpoint();

  useEffect(() => {
    if (open) {
      form.resetFields();
    }
  }, [open, form]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    await addMutation.mutateAsync({ channelId, data: values });
    onClose();
  };

  return (
    <Modal
      title={t('channel.addEndpoint')}
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={addMutation.isPending}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="protocol"
          label={t('channel.protocol')}
          rules={[{ required: true, message: t('channel.protocolRequired') as string }]}
        >
          <Select
            options={[
              { value: 'openai', label: 'OpenAI' },
              { value: 'anthropic', label: 'Anthropic' },
            ]}
          />
        </Form.Item>
        <Form.Item
          name="endpointUrl"
          label={t('channel.endpointUrl')}
          rules={[{ required: true, message: t('channel.endpointUrlRequired') as string }]}
        >
          <Input placeholder="https://api.openai.com/v1" />
        </Form.Item>
      </Form>
    </Modal>
  );
}