import { Modal, Form, Input, Select, DatePicker, Space, Button, Typography, theme } from 'antd';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import type { ApiKey, GatewayApiKeyState } from '@/types/apiKey';

const { Paragraph } = Typography;

interface FormValues {
  name: string;
  userId?: number;
  expiresAt?: string;
  state?: GatewayApiKeyState;
  ipWhitelist?: string[];
}

interface User {
  id: number;
  username: string;
}

interface ApiKeyFormModalProps {
  open: boolean;
  editingApiKey: ApiKey | null;
  newKey: string | null;
  isAdmin: boolean;
  users?: User[];
  onSubmit: (values: FormValues) => Promise<void>;
  onCancel: () => void;
  loading?: boolean;
}

/**
 * API Key 表单弹窗组件
 */
export function ApiKeyFormModal({
  open,
  editingApiKey,
  newKey,
  isAdmin,
  users = [],
  onSubmit,
  onCancel,
  loading,
}: ApiKeyFormModalProps) {
  const { t } = useTranslation('apiKeys');
  const { token } = theme.useToken();
  const [form] = Form.useForm();

  const handleSubmit = async (values: FormValues) => {
    // 转换过期时间为 ISO 字符串
    const submitValues = {
      ...values,
      expiresAt: values.expiresAt ? new Date(values.expiresAt).toISOString() : undefined,
    };
    await onSubmit(submitValues);
  };

  return (
    <Modal
      title={editingApiKey ? t('actions.edit', { ns: 'common' }) : t('add', { defaultValue: '创建 API Key' })}
      open={open}
      onCancel={onCancel}
      footer={null}
      destroyOnClose
      afterOpenChange={(visible) => {
        if (visible && editingApiKey) {
          form.setFieldsValue({
            ...editingApiKey,
            expiresAt: editingApiKey.expiresAt ? new Date(editingApiKey.expiresAt) : undefined,
          });
        }
      }}
    >
      {/* 新创建的 Key 展示 */}
      {newKey && (
        <div
          style={{
            marginBottom: 16,
            padding: 12,
            background: token.colorWarningBg,
            borderRadius: 8,
            border: `1px solid ${token.colorWarningBorder}`,
          }}
        >
          <p style={{ margin: 0, fontWeight: 600, color: token.colorWarningText }}>
            {t('newKeyHint', { defaultValue: 'API Key 已创建（仅显示一次）：' })}
          </p>
          <Paragraph
            copyable={{
              text: newKey,
              tooltips: [t('copy', { defaultValue: '复制' }), t('copied', { defaultValue: '已复制' })],
            }}
            style={{
              margin: '8px 0 0 0',
              fontFamily: 'monospace',
              fontSize: 13,
              wordBreak: 'break-all',
            }}
          >
            {newKey}
          </Paragraph>
        </div>
      )}

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          state: 'ACTIVE',
        }}
      >
        {/* 用户选择器（仅 Admin + 创建模式） */}
        {isAdmin && !editingApiKey && (
          <Form.Item
            name="userId"
            label={t('user', { defaultValue: '所属用户' })}
            rules={[{ required: true, message: t('selectUser', { defaultValue: '请选择用户' }) }]}
          >
            <Select
              showSearch
              optionFilterProp="children"
              placeholder={t('selectUser', { defaultValue: '请选择用户' })}
            >
              {users.map((user) => (
                <Select.Option key={user.id} value={user.id}>
                  {user.username}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        )}

        {/* 名称 */}
        <Form.Item
          name="name"
          label={t('name', { defaultValue: '名称' })}
          rules={[{ required: true, message: t('nameRequired', { defaultValue: '请输入名称' }) }]}
        >
          <Input placeholder={t('namePlaceholder', { defaultValue: '如：生产环境 Key' })} />
        </Form.Item>

        {/* 过期时间 */}
        <Form.Item
          name="expiresAt"
          label={t('expiresAt', { defaultValue: '过期时间' })}
          extra={t('expiresAtHint', { defaultValue: '留空表示永不过期' })}
        >
          <DatePicker
            showTime
            style={{ width: '100%' }}
            placeholder={t('selectExpiresAt', { defaultValue: '选择过期时间' })}
            disabledDate={(current) => current && current.isBefore(dayjs(), 'day')}
          />
        </Form.Item>

        {/* 状态（仅编辑模式） */}
        {editingApiKey && (
          <Form.Item name="state" label={t('state', { defaultValue: '状态' })}>
            <Select>
              <Select.Option value="ACTIVE">{t('state.active', { ns: 'common' })}</Select.Option>
              <Select.Option value="DISABLED">{t('state.disabled', { ns: 'common' })}</Select.Option>
            </Select>
          </Form.Item>
        )}

        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={loading}>
              {editingApiKey ? t('actions.save', { ns: 'common' }) : t('actions.create', { ns: 'common' })}
            </Button>
            <Button onClick={onCancel}>{t('actions.cancel', { ns: 'common' })}</Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
}

export type { ApiKeyFormModalProps, FormValues, User };
