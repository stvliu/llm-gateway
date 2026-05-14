import { useState, useCallback, useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Row, Col, Checkbox, Space, Button, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { ModelTemplateSelector, type ModelTemplate } from './ModelTemplateSelector';
import { useCreateModel, useUpdateModel } from '@/services/query';
import type { Provider } from '@/types/provider';
import type { Model } from '@/types/model';

interface ModelAddModalProps {
  open: boolean;
  provider: Provider | null;
  editingModel?: Model | null;
  onClose: () => void;
  onSuccess: () => void;
}

/**
 * 模型快速添加/编辑弹窗
 * 用于从卡片快速添加模型或编辑现有模型，支持模板选择和自定义表单
 */
export function ModelAddModal({ open, provider, editingModel, onClose, onSuccess }: ModelAddModalProps) {
  const { t } = useTranslation('models');
  const createModelMutation = useCreateModel();
  const updateModelMutation = useUpdateModel();
  const [form] = Form.useForm();
  const [showCustomForm, setShowCustomForm] = useState(false);

  const isEditMode = !!editingModel?.id;

  // 重置状态
  useEffect(() => {
    if (open) {
      if (isEditMode && editingModel) {
        // 编辑模式：填充现有数据
        setShowCustomForm(true);
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
        // 新增模式
        setShowCustomForm(false);
        form.resetFields();
      }
    }
  }, [open, isEditMode, editingModel, form]);

  // 从模板快速添加
  const handleTemplateSelect = useCallback(async (template: ModelTemplate) => {
    if (!provider) return;

    try {
      await createModelMutation.mutateAsync({
        providerId: provider.id,
        providerModelId: template.id,
        displayName: template.displayName,
        contextWindow: template.contextWindow,
        inputPrice: template.inputPrice,
        outputPrice: template.outputPrice,
        capabilities: template.capabilities,
      });
      message.success(t('message.modelAdded', { defaultValue: '模型添加成功' }));
      onSuccess();
      onClose();
    } catch (error) {
      // 快速添加失败，回退到表单模式并预填充数据
      console.error('Failed to add model from template:', error);
      setShowCustomForm(true);
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
    }
  }, [provider, createModelMutation, form, t, onSuccess, onClose]);

  // 切换到自定义表单模式
  const handleCustomAdd = useCallback(() => {
    setShowCustomForm(true);
  }, []);

  // 提交自定义表单（新增或编辑）
  const handleSubmit = useCallback(async (values: {
    providerModelId: string;
    displayName?: string;
    contextWindow?: number;
    inputPrice?: number;
    outputPrice?: number;
    capabilities?: string[];
  }) => {
    if (!provider && !isEditMode) return;

    const capabilities: Record<string, boolean> = {};
    if (values.capabilities) {
      values.capabilities.forEach(cap => {
        capabilities[cap] = true;
      });
    }

    try {
      if (isEditMode && editingModel) {
        // 编辑模式
        await updateModelMutation.mutateAsync({
          id: editingModel.id,
          data: {
            displayName: values.displayName,
            contextWindow: values.contextWindow,
            inputPrice: values.inputPrice,
            outputPrice: values.outputPrice,
            capabilities,
          },
        });
        message.success(t('message.modelUpdated', { defaultValue: '模型更新成功' }));
      } else if (provider) {
        // 新增模式
        await createModelMutation.mutateAsync({
          providerId: provider.id,
          providerModelId: values.providerModelId,
          displayName: values.displayName,
          contextWindow: values.contextWindow,
          inputPrice: values.inputPrice,
          outputPrice: values.outputPrice,
          capabilities,
        });
        message.success(t('message.modelAdded', { defaultValue: '模型添加成功' }));
      }
      onSuccess();
      onClose();
    } catch (error) {
      console.error('Failed to save model:', error);
    }
  }, [provider, isEditMode, editingModel, createModelMutation, updateModelMutation, t, onSuccess, onClose]);

  const handleClose = useCallback(() => {
    setShowCustomForm(false);
    form.resetFields();
    onClose();
  }, [form, onClose]);

  if (!provider) return null;

  return (
    <Modal
      title={
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingRight: 24 }}>
          <span>{isEditMode ? t('editModel', { defaultValue: '编辑模型' }) : t('addModel')}</span>
          {!showCustomForm && !isEditMode && (
            <Button type="link" size="small" onClick={handleCustomAdd} style={{ fontSize: 13 }}>
              {t('template.customAdd', { defaultValue: '自定义模型' })}
            </Button>
          )}
        </div>
      }
      open={open}
      onCancel={handleClose}
      footer={null}
      width={560}
    >
      {/* 模板选择模式（仅新增） */}
      {!showCustomForm && !isEditMode && (
        <ModelTemplateSelector
          providerType={provider.providerType}
          onSelect={handleTemplateSelect}
        />
      )}

      {/* 自定义表单模式（新增或编辑） */}
      {(showCustomForm || isEditMode) && (
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            name="providerModelId"
            label={t('model.providerModelId')}
            rules={[{ required: true }]}
          >
            <Input placeholder="gpt-4o" disabled={isEditMode} />
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
                    <Checkbox value="embedding">Embedding</Checkbox>
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
              <Button onClick={handleClose}>
                {t('actions.cancel', { ns: 'common' })}
              </Button>
              <Button type="primary" htmlType="submit" loading={createModelMutation.isPending || updateModelMutation.isPending}>
                {t('actions.save', { ns: 'common' })}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
}
