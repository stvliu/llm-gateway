'use client';

import { useEffect, useState } from 'react';
import { Modal, Form, Input, message, Alert } from 'antd';
import type {
  UserApiKey,
  CreateUserApiKeyRequest,
  UpdateUserApiKeyRequest,
} from '@/types/userApiKey';
import { userApiKeyApi } from '@/services/api/userApiKey';
import type { Application } from '@/types/application';

interface UserApiKeyManageModalProps {
  open: boolean;
  application: Application;
  editingKey?: UserApiKey | null;
  onClose: () => void;
  onSuccess?: () => void;
}

/**
 * 应用维度用户 API Key 管理弹窗
 *
 * 在应用上下文创建/编辑用户 API Key。Key 归属用户并通过应用-渠道授权继承渠道权限。
 */
export default function UserApiKeyManageModal({
  open,
  application,
  editingKey,
  onClose,
  onSuccess,
}: UserApiKeyManageModalProps) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [createdKey, setCreatedKey] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      if (editingKey) {
        form.setFieldsValue({
          name: editingKey.name,
        });
        setCreatedKey(null);
      } else {
        form.resetFields();
        setCreatedKey(null);
      }
    }
  }, [open, editingKey, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (editingKey) {
        const request: UpdateUserApiKeyRequest = {
          name: values.name,
        };
        await userApiKeyApi.update(editingKey.id, request);
        message.success('API Key 更新成功');
      } else {
        const request: CreateUserApiKeyRequest = {
          userId: values.userId,
          name: values.name,
        };
        const result = await userApiKeyApi.create(request);
        setCreatedKey(result.keyPlain);
        message.success('API Key 创建成功');
      }

      onSuccess?.();
    } catch (error: unknown) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      message.error(editingKey ? '更新失败' : '创建失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={editingKey ? '编辑 API Key' : '创建 API Key'}
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={loading}
      width={560}
      okText={editingKey ? '保存' : '创建'}
    >
      {createdKey && (
        <div style={{ marginBottom: 16, padding: 12, background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6 }}>
          <div style={{ marginBottom: 4, fontWeight: 500 }}>API Key 创建成功，请妥善保存：</div>
          <code style={{ wordBreak: 'break-all', fontSize: 13 }}>{createdKey}</code>
          <div style={{ marginTop: 4, color: '#999', fontSize: 12 }}>此密钥仅显示一次，关闭后无法再次查看</div>
        </div>
      )}

      {!editingKey && (
        <Alert
          message="权限说明"
          description={`此 API Key 将归属应用「${application.name}」并继承该应用的渠道访问权限。`}
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      <Form form={form} layout="vertical">
        <Form.Item name="name" label="密钥名称" rules={[{ required: true, message: '请输入密钥名称' }]}>
          <Input placeholder="例如：生产环境 Key" />
        </Form.Item>

        {!editingKey && (
          <Form.Item name="userId" label="用户 ID" rules={[{ required: true, message: '请输入用户 ID' }]}>
            <Input placeholder="用户 ID" />
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
}
