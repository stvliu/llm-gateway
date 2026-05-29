import { Modal, Form, Input, InputNumber, Select, Tag, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateModel } from '@/services/query/useModels';
import type { CreateModelRequest } from '@/types/model';

interface Props {
  open: boolean;
  onClose: () => void;
}

const capabilityOptions = [
  { value: 'vision', label: '图像识别' },
  { value: 'function_calling', label: '函数调用' },
  { value: 'streaming', label: '流式' },
];

const modalityOptions = [
  { value: 'text', label: '文本' },
  { value: 'image', label: '图像' },
  { value: 'audio', label: '音频' },
];

export default function ModelCreateModal({ open, onClose }: Props) {
  const { t } = useTranslation('models');
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const createMutation = useCreateModel();

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      const payload: CreateModelRequest = {
        ...values,
        capabilities: values.capabilities?.reduce((acc: Record<string, boolean>, k: string) => {
          acc[k] = true;
          return acc;
        }, {}) || {},
      };
      await createMutation.mutateAsync(payload);
      message.success(t('created', { defaultValue: '模型创建成功' }));
      form.resetFields();
      onClose();
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'errorFields' in e) return;
      message.error(t('createFailed', { defaultValue: '创建失败' }));
    }
  };

  return (
    <Modal title={t('createModel', { defaultValue: '新增模型' })} open={open} onOk={handleOk} onCancel={onClose} width={560} confirmLoading={createMutation.isPending}>
      <Form form={form} layout="vertical">
        <Form.Item name="modelName" label={t('modelName', { defaultValue: '模型标识' })} rules={[{ required: true }]}>
          <Input placeholder="gpt-4o" />
        </Form.Item>
        <Form.Item name="displayName" label={t('displayName', { defaultValue: '显示名称' })}>
          <Input placeholder="GPT-4o" />
        </Form.Item>
        <Form.Item name="modelFamily" label={t('modelFamily', { defaultValue: '模型族' })}>
          <Input placeholder="gpt-4" />
        </Form.Item>
        <Form.Item name="contextWindow" label={t('contextWindow', { defaultValue: '上下文窗口' })}>
          <InputNumber style={{ width: '100%' }} placeholder="128000" />
        </Form.Item>
        <Form.Item name="capabilities" label={t('capabilities', { defaultValue: '能力' })}>
          <Select mode="multiple" options={capabilityOptions} tagRender={(props) => <Tag closable={props.closable} onClose={props.onClose}>{props.label}</Tag>} />
        </Form.Item>
        <Form.Item name="modalities" label={t('modalities', { defaultValue: '模态' })}>
          <Select mode="multiple" options={modalityOptions} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
