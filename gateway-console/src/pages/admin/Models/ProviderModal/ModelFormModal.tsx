import { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Row, Col, Checkbox, Space, Button } from 'antd';
import { useTranslation } from 'react-i18next';
import { ModelTemplateSelector } from '../ModelTemplateSelector';

export interface ModelFormItem {
  id?: number;
  providerModelId: string;
  displayName?: string;
  contextWindow?: number;
  inputPrice?: number;
  outputPrice?: number;
  capabilities?: Record<string, boolean>;
  state?: string;
}

interface ModelFormModalProps {
  open: boolean;
  editingModel: ModelFormItem | null;
  providerType?: string;
  onClose: () => void;
  onSubmit: (values: ModelFormItem) => void;
  onShowTemplateSelector: () => void;
}

/**
 * 模型表单弹窗
 */
export function ModelFormModal({
  open,
  editingModel,
  providerType,
  onClose,
  onSubmit,
  onShowTemplateSelector,
}: ModelFormModalProps) {
  const { t } = useTranslation('models');
  const [form] = Form.useForm();

  useEffect(() => {
    if (open) {
      if (editingModel) {
        form.setFieldsValue({
          providerModelId: editingModel.providerModelId,
          displayName: editingModel.displayName,
          contextWindow: editingModel.contextWindow,
          inputPrice: editingModel.inputPrice,
          outputPrice: editingModel.outputPrice,
          capabilities: editingModel.capabilities
            ? Object.entries(editingModel.capabilities)
                .filter(([, v]) => v)
                .map(([k]) => k)
            : [],
        });
      } else {
        form.resetFields();
      }
    }
  }, [open, editingModel, form]);

  const handleSubmit = async (values: ModelFormItem & { capabilities?: string[] }) => {
    const capabilities: Record<string, boolean> = {};
    if (values.capabilities) {
      values.capabilities.forEach(cap => {
        capabilities[cap] = true;
      });
    }

    onSubmit({
      ...editingModel,
      providerModelId: values.providerModelId,
      displayName: values.displayName,
      contextWindow: values.contextWindow,
      inputPrice: values.inputPrice,
      outputPrice: values.outputPrice,
      capabilities,
    });
  };

  return (
    <Modal
      title={editingModel ? t('actions.edit', { ns: 'common' }) : t('addModel')}
      open={open}
      onCancel={onClose}
      footer={null}
      width={560}
    >
      {!editingModel && providerType && (
        <ModelTemplateSelector
          providerType={providerType}
          onSelect={(template) => {
            form.setFieldsValue({
              providerModelId: template.id,
              displayName: template.displayName,
              contextWindow: template.contextWindow,
              inputPrice: template.inputPrice,
              outputPrice: template.outputPrice,
              capabilities: template.capabilities
                ? Object.entries(template.capabilities)
                    .filter(([, v]) => v)
                    .map(([k]) => k)
                : [],
            });
          }}
          onCustomAdd={onShowTemplateSelector}
        />
      )}

      {!editingModel && !providerType && (
        <Button type="link" onClick={onShowTemplateSelector} style={{ padding: 0, marginBottom: 16 }}>
          {t('template.selectFromList', { defaultValue: '从模板选择' })}
        </Button>
      )}

      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item
          name="providerModelId"
          label={t('model.providerModelId')}
          rules={[{ required: true }]}
        >
          <Input disabled={!!editingModel?.id} placeholder="gpt-4o" />
        </Form.Item>

        <Form.Item name="displayName" label={t('model.name')}>
          <Input placeholder="GPT-4o" />
        </Form.Item>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="contextWindow" label={t('detail.contextWindow')}>
              <InputNumber
                style={{ width: '100%' }}
                formatter={(v) => v ? `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',') : ''}
                parser={(v) => v!.replace(/,/g, '') as any}
                addonAfter="tokens"
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="capabilities" label={t('model.capabilities', { defaultValue: '能力' })}>
              <Checkbox.Group>
                <Space direction="vertical">
                  <Checkbox value="chat">Chat</Checkbox>
                  <Checkbox value="vision">Vision</Checkbox>
                  <Checkbox value="function_calling">Function Calling</Checkbox>
                </Space>
              </Checkbox.Group>
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="inputPrice" label={t('detail.inputPrice')}>
              <InputNumber
                style={{ width: '100%' }}
                step="0.0001"
                precision={4}
                min={0}
                addonBefore="$"
                addonAfter="/M"
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="outputPrice" label={t('detail.outputPrice')}>
              <InputNumber
                style={{ width: '100%' }}
                step="0.0001"
                precision={4}
                min={0}
                addonBefore="$"
                addonAfter="/M"
              />
            </Form.Item>
          </Col>
        </Row>

        <Form.Item style={{ marginBottom: 0 }}>
          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={onClose}>
              {t('actions.cancel', { ns: 'common' })}
            </Button>
            <Button type="primary" htmlType="submit">
              {t('actions.save', { ns: 'common' })}
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
}
