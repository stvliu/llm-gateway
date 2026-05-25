import { useState, useEffect, useCallback } from 'react';
import { Modal, Form, Input, Select, Button, Space, App } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useCreateProduct, useUpdateProduct } from '@/services/query';
import { protocolApi } from '@/services/api/protocolApi';
import type { Product, ProtocolInfo } from '@/types/product';

interface Props {
  open: boolean;
  providerId: number;
  providerName: string;
  editingProduct?: Product | null;
  onClose: () => void;
  onSaved: () => void;
}

export function ProductFormModal({ open, providerId, providerName, editingProduct, onClose, onSaved }: Props) {
  const { t } = useTranslation('providers');
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const createMutation = useCreateProduct();
  const updateMutation = useUpdateProduct();
  const [saving, setSaving] = useState(false);
  const [protocols, setProtocols] = useState<ProtocolInfo[]>([]);
  const [endpoints, setEndpoints] = useState<Array<{ protocol: string; url: string }>>([]);

  // 加载协议列表
  useEffect(() => {
    if (open) {
      protocolApi.list().then(setProtocols).catch(() => {
        // 降级：使用默认协议列表
        setProtocols([
          { name: 'openai', label: 'OpenAI Chat Completions' },
          { name: 'anthropic', label: 'Anthropic Messages' },
        ]);
      });
    }
  }, [open]);

  // 初始化 endpoints
  useEffect(() => {
    if (open && editingProduct?.endpoints) {
      const entries = Object.entries(editingProduct.endpoints).map(([protocol, url]) => ({
        protocol,
        url,
      }));
      setEndpoints(entries.length > 0 ? entries : [{ protocol: '', url: '' }]);
    } else if (open) {
      setEndpoints([{ protocol: '', url: '' }]);
    }
  }, [open, editingProduct]);

  const handleClose = useCallback(() => {
    form.resetFields();
    setEndpoints([{ protocol: '', url: '' }]);
    onClose();
  }, [form, onClose]);

  const addEndpoint = useCallback(() => {
    setEndpoints(prev => [...prev, { protocol: '', url: '' }]);
  }, []);

  const removeEndpoint = useCallback((index: number) => {
    setEndpoints(prev => prev.filter((_, i) => i !== index));
  }, []);

  const updateEndpoint = useCallback((index: number, field: 'protocol' | 'url', value: string) => {
    setEndpoints(prev => prev.map((ep, i) => i === index ? { ...ep, [field]: value } : ep));
  }, []);

  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();

      // 验证至少有一个端点
      const validEndpoints = endpoints.filter(ep => ep.protocol && ep.url);
      if (validEndpoints.length === 0) {
        message.warning(t('validation.endpointRequired', { defaultValue: '请至少配置一个端点' }));
        return;
      }

      // 检查协议重复
      const protocols = validEndpoints.map(ep => ep.protocol);
      if (new Set(protocols).size !== protocols.length) {
        message.warning(t('validation.duplicateProtocol', { defaultValue: '不能配置重复的协议' }));
        return;
      }

      setSaving(true);

      const endpointsMap = Object.fromEntries(
        validEndpoints.map(ep => [ep.protocol, ep.url])
      );

      if (editingProduct) {
        await updateMutation.mutateAsync({
          id: editingProduct.id,
          data: {
            name: values.productName,
            endpoints: endpointsMap,
          },
        });
        message.success(t('message.updateSuccess', { defaultValue: '产品更新成功' }));
      } else {
        await createMutation.mutateAsync({
          providerId,
          name: values.productName,
          endpoints: endpointsMap,
        });
        message.success(t('message.createSuccess', { defaultValue: '产品创建成功' }));
      }

      onSaved();
      handleClose();
    } catch {
      // 表单验证失败
    } finally {
      setSaving(false);
    }
  }, [form, endpoints, editingProduct, providerId, createMutation, updateMutation, message, t, onSaved, handleClose]);

  return (
    <Modal
      title={editingProduct
        ? t('editProduct', { defaultValue: '编辑产品' })
        : t('addProduct', { defaultValue: '新增产品' })}
      open={open}
      onOk={handleSave}
      onCancel={handleClose}
      confirmLoading={saving}
      width={640}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" initialValues={{
        productName: editingProduct?.name,
      }}>
        <Form.Item label={t('form.providerName', { defaultValue: '供应商' })}>
          <Input value={providerName} disabled />
        </Form.Item>
        <Form.Item
          name="productName"
          label={t('form.productName', { defaultValue: '产品名称' })}
          rules={[{ required: true, message: t('validation.productNameRequired', { defaultValue: '请输入产品名称' }) }]}
        >
          <Input />
        </Form.Item>
      </Form>

      {/* 端点配置 */}
      <div style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 8, fontWeight: 500 }}>
          {t('form.endpoints', { defaultValue: '端点配置' })}
        </div>
        {endpoints.map((ep, index) => (
          <Space key={index} style={{ display: 'flex', marginBottom: 8 }} align="start">
            <Select
              value={ep.protocol || undefined}
              onChange={(value) => updateEndpoint(index, 'protocol', value)}
              placeholder={t('form.selectProtocol', { defaultValue: '选择协议' })}
              style={{ width: 180 }}
              options={protocols.map(p => ({ label: p.label, value: p.name }))}
            />
            <Input
              value={ep.url}
              onChange={(e) => updateEndpoint(index, 'url', e.target.value)}
              placeholder="https://api.example.com"
              style={{ width: 320 }}
            />
            {endpoints.length > 1 && (
              <Button
                icon={<DeleteOutlined />}
                danger
                onClick={() => removeEndpoint(index)}
              />
            )}
          </Space>
        ))}
        <Button type="dashed" onClick={addEndpoint} icon={<PlusOutlined />} style={{ width: '100%' }}>
          {t('form.addEndpoint', { defaultValue: '添加端点' })}
        </Button>
      </div>
    </Modal>
  );
}