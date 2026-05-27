import { useEffect } from 'react';
import { Form, Input, InputNumber, Select, Modal, Space } from 'antd';
import { useTranslation } from 'react-i18next';
import { useCreateModelSpec, useUpdateModelSpec } from '@/services/query/useModelSpecs';
import type { ModelSpec } from '@/types/modelSpec';

interface ModelSpecFormModalProps {
  open: boolean;
  providerId: number;
  modelSpec?: ModelSpec | null;
  onClose: () => void;
}

/** 模型规格能力选项 */
const CAPABILITY_OPTIONS = [
  { value: 'chat', label: 'Chat' },
  { value: 'vision', label: 'Vision' },
  { value: 'streaming', label: 'Streaming' },
  { value: 'function_calling', label: 'Function Calling' },
  { value: 'embedding', label: 'Embedding' },
];

/**
 * 模型规格创建/编辑弹窗
 * 支持创建和编辑两种模式，capabilities 字段使用多选下拉
 */
export default function ModelSpecFormModal({ open, providerId, modelSpec, onClose }: ModelSpecFormModalProps) {
  const { t } = useTranslation('providers');
  const [form] = Form.useForm();
  const createMutation = useCreateModelSpec();
  const updateMutation = useUpdateModelSpec();

  const isEdit = !!modelSpec;

  useEffect(() => {
    if (open) {
      if (modelSpec) {
        // 编辑模式：填充表单，capabilities 从 Map 转为数组
        form.setFieldsValue({
          providerModelId: modelSpec.providerModelId,
          displayName: modelSpec.displayName,
          modelFamily: modelSpec.modelFamily,
          contextWindow: modelSpec.contextWindow,
          maxInputTokens: modelSpec.maxInputTokens,
          maxOutputTokens: modelSpec.maxOutputTokens,
          capabilities: modelSpec.capabilities
            ? Object.entries(modelSpec.capabilities)
                .filter(([, v]) => v)
                .map(([k]) => k)
            : [],
          priority: modelSpec.priority,
          weight: modelSpec.weight,
        });
      } else {
        // 创建模式：重置表单并设置默认值
        form.resetFields();
        form.setFieldsValue({ providerId, priority: 0, weight: 100 });
      }
    }
  }, [open, modelSpec, providerId, form]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    // 将 capabilities 数组转换为 Record<string, boolean>
    const capabilities: Record<string, boolean> = {};
    if (values.capabilities) {
      values.capabilities.forEach((key: string) => {
        capabilities[key] = true;
      });
    }
    const payload = { ...values, capabilities };

    if (isEdit) {
      await updateMutation.mutateAsync({ id: modelSpec!.id, data: payload });
    } else {
      await createMutation.mutateAsync(payload);
    }
    onClose();
  };

  return (
    <Modal
      title={isEdit ? t('modelSpec.edit') : t('modelSpec.create')}
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={createMutation.isPending || updateMutation.isPending}
      width={600}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item name="providerId" hidden>
          <Input />
        </Form.Item>
        <Form.Item
          name="providerModelId"
          label={t('modelSpec.providerModelId')}
          rules={[{ required: true, message: t('modelSpec.providerModelIdRequired', { defaultValue: '请输入供应商模型 ID' }) }]}
        >
          <Input placeholder="gpt-4o" />
        </Form.Item>
        <Form.Item name="displayName" label={t('modelSpec.displayName')}>
          <Input placeholder="GPT-4o" />
        </Form.Item>
        <Form.Item name="modelFamily" label={t('modelSpec.modelFamily')}>
          <Input placeholder="gpt-4" />
        </Form.Item>
        <Space style={{ width: '100%' }} size="large">
          <Form.Item name="contextWindow" label={t('modelSpec.contextWindow')}>
            <InputNumber min={0} style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="maxInputTokens" label={t('modelSpec.maxInputTokens', { defaultValue: '最大输入 Tokens' })}>
            <InputNumber min={0} style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="maxOutputTokens" label={t('modelSpec.maxOutputTokens', { defaultValue: '最大输出 Tokens' })}>
            <InputNumber min={0} style={{ width: 160 }} />
          </Form.Item>
        </Space>
        <Form.Item name="capabilities" label={t('modelSpec.capabilities')}>
          <Select
            mode="multiple"
            options={CAPABILITY_OPTIONS}
            placeholder={t('modelSpec.capabilityPlaceholder', { defaultValue: '选择模型能力' })}
            allowClear
          />
        </Form.Item>
        <Space style={{ width: '100%' }} size="large">
          <Form.Item name="priority" label={t('channel.priority')}>
            <InputNumber min={0} style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="weight" label={t('channel.weight')}>
            <InputNumber min={1} style={{ width: 200 }} />
          </Form.Item>
        </Space>
      </Form>
    </Modal>
  );
}
