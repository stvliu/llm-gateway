import { useEffect, useCallback, useState } from 'react';
import { Modal, Form, Input, InputNumber, Switch, Space, Button, message, Typography, theme } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined, ApiOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useCreateProviderApiKey, useUpdateProviderApiKey } from '@/services/query';
import { providerApi, type ConnectivityTestResult } from '@/services/api/provider';
import type { Provider } from '@/types/provider';
import type { ProviderApiKey } from '@/types/providerApiKey';

const { Text } = Typography;

type TestStatus = 'idle' | 'testing' | 'success' | 'failed';

interface ApiKeyModalProps {
  open: boolean;
  provider: Provider | null;
  editingKey: ProviderApiKey | null;
  onClose: () => void;
  onSuccess: () => void;
}

/**
 * API Key 弹窗
 * 新增或编辑 API Key
 */
export function ApiKeyModal({ open, provider, editingKey, onClose, onSuccess }: ApiKeyModalProps) {
  const { t } = useTranslation('providers');
  const { token } = theme.useToken();
  const [form] = Form.useForm();

  // 测试状态
  const [testStatus, setTestStatus] = useState<TestStatus>('idle');
  const [testResult, setTestResult] = useState<ConnectivityTestResult | null>(null);

  const createMutation = useCreateProviderApiKey();
  const updateMutation = useUpdateProviderApiKey();

  useEffect(() => {
    if (open) {
      // 重置测试状态
      setTestStatus('idle');
      setTestResult(null);

      if (editingKey) {
        form.setFieldsValue({
          keyName: editingKey.keyName,
          apiKey: editingKey.apiKey || '',
          priority: editingKey.priority ?? 100,
          weight: editingKey.weight ?? 100,
          isDefault: editingKey.isDefault ?? false,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ priority: 100, weight: 100, isDefault: false });
      }
    }
  }, [open, editingKey, form]);

  const handleSubmit = useCallback(async (values: {
    keyName: string;
    apiKey: string;
    priority: number;
    weight: number;
    isDefault: boolean;
  }) => {
    if (!provider) return;

    try {
      if (editingKey) {
        await updateMutation.mutateAsync({
          id: editingKey.id,
          data: {
            keyName: values.keyName,
            apiKey: values.apiKey,
            priority: values.priority,
            weight: values.weight,
            isDefault: values.isDefault,
          },
        });
      } else {
        await createMutation.mutateAsync({
          providerId: provider.id,
          keyName: values.keyName,
          apiKey: values.apiKey,
          priority: values.priority,
          weight: values.weight,
          isDefault: values.isDefault,
        });
      }
      onSuccess();
    } catch {
      message.error(t('message.apiKeySaveFailed', { defaultValue: 'API Key 保存失败' }));
    }
  }, [provider, editingKey, createMutation, updateMutation, onSuccess, t]);

  // 测试连通性
  const handleTestConnectivity = useCallback(async () => {
    if (!provider) {
      message.warning(t('validation.providerRequired', { defaultValue: '请先选择供应商' }));
      return;
    }

    // 触发表单验证
    const fieldsToValidate = ['apiKey'];
    if (provider.providerType === 'VOLCENGINE') {
      fieldsToValidate.push('testModel');
    }

    try {
      await form.validateFields(fieldsToValidate);
    } catch {
      // 验证失败，错误信息已由 Form.Item 显示
      return;
    }

    const apiKey = form.getFieldValue('apiKey');
    const testModel = form.getFieldValue('testModel');
    setTestStatus('testing');
    setTestResult(null);

    try {
      const result = await providerApi.testConnectivity({
        providerType: provider.providerType,
        baseUrl: provider.baseUrl,
        apiKey,
        model: testModel || undefined,
      });

      setTestResult(result);
      setTestStatus(result.success ? 'success' : 'failed');

      if (result.success) {
        message.success(t('test.keyValid', { defaultValue: 'API Key 验证成功' }));
      } else {
        message.error(result.message || t('test.keyInvalid', { defaultValue: 'API Key 验证失败' }));
      }
    } catch (error) {
      console.error('Connectivity test failed:', error);
      setTestStatus('failed');
      message.error(t('test.connectionFailed', { defaultValue: '连接失败，请检查网络或 API 地址' }));
    }
  }, [provider, form, t]);

  // 获取测试状态图标
  const getTestIcon = () => {
    switch (testStatus) {
      case 'testing':
        return <LoadingOutlined style={{ color: token.colorPrimary }} />;
      case 'success':
        return <CheckCircleOutlined style={{ color: token.colorSuccess }} />;
      case 'failed':
        return <CloseCircleOutlined style={{ color: token.colorError }} />;
      default:
        return null;
    }
  };

  return (
    <Modal
      title={editingKey
        ? t('provider.editApiKey', { defaultValue: '编辑 API Key' })
        : t('provider.addApiKey', { defaultValue: '添加 API Key' })
      }
      open={open}
      onCancel={onClose}
      footer={null}
      width={480}
    >
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Form.Item
          name="keyName"
          label={t('provider.keyName', { defaultValue: 'Key 名称' })}
          rules={[{ required: true }]}
        >
          <Input placeholder="Production Key" />
        </Form.Item>

        <Form.Item
          name="apiKey"
          label={t('provider.apiKey', { defaultValue: 'API Key' })}
          rules={[
            { required: true, message: t('validation.apiKeyRequired', { defaultValue: '请输入 API Key' }) },
            { min: 10, message: t('validation.apiKeyMinLength', { defaultValue: 'API Key 格式不正确' }) },
            { max: 500, message: t('validation.apiKeyMaxLength', { defaultValue: 'API Key 长度不能超过 500 字符' }) },
          ]}
        >
          <Input.Password placeholder="sk-..." />
        </Form.Item>

        {/* 测试模型/Endpoint ID（火山引擎必填） */}
        {provider?.providerType === 'VOLCENGINE' && (
          <Form.Item
            name="testModel"
            label={t('test.endpointId', { defaultValue: 'Endpoint ID' })}
            extra={t('test.endpointIdHint', { defaultValue: '火山引擎需要推理接入点 ID，请在火山引擎控制台获取' })}
            rules={[{ required: true, message: t('validation.endpointIdRequired', { defaultValue: '火山引擎连通性测试需要 Endpoint ID' }) }]}
          >
            <Input placeholder="ep-202410xxxxxx-xxxxx" />
          </Form.Item>
        )}

        {/* 测试连通性 */}
        {provider && (
          <Form.Item label={t('test.connectivity', { defaultValue: '连通性测试' })}>
            <Space>
              <Button
                icon={testStatus === 'testing' ? <LoadingOutlined /> : <ApiOutlined />}
                onClick={handleTestConnectivity}
                disabled={testStatus === 'testing'}
              >
                {t('test.testConnection', { defaultValue: '测试连通性' })}
              </Button>
              {getTestIcon()}
              {testResult && (
                <Text type={testResult.success ? 'success' : 'danger'}>
                  {testResult.success
                    ? t('test.valid', { defaultValue: '有效' })
                    : t('test.invalid', { defaultValue: '无效' })}
                </Text>
              )}
            </Space>
            {testResult && (
              <div style={{ marginTop: 8, fontSize: 12 }}>
                {testResult.level1 && (
                  <div style={{ color: token.colorTextSecondary }}>
                    {t('test.auth', { defaultValue: '认证' })}: {testResult.level1.success
                      ? t('test.passed', { defaultValue: '通过' })
                      : t('test.failed', { defaultValue: '失败' })}
                    {testResult.level1.latencyMs && ` (${testResult.level1.latencyMs}ms)`}
                    {testResult.level1.models && testResult.level1.models.length > 0 && (
                      <span> - {testResult.level1.models.length} {t('test.models', { defaultValue: '个模型' })}</span>
                    )}
                  </div>
                )}
                {testResult.level2 !== undefined && (
                  <div style={{ color: token.colorTextSecondary }}>
                    {t('test.modelAvailability', { defaultValue: '模型可用性' })}:{' '}
                    {testResult.level2 === null
                      ? t('test.skipped', { defaultValue: '跳过' })
                      : testResult.level2.success
                        ? t('test.passed', { defaultValue: '通过' })
                        : t('test.failed', { defaultValue: '失败' })}
                    {testResult.level2?.latencyMs && ` (${testResult.level2.latencyMs}ms)`}
                  </div>
                )}
              </div>
            )}
          </Form.Item>
        )}

        <Form.Item name="priority" label={t('provider.priority', { defaultValue: '优先级' })}>
          <InputNumber style={{ width: '100%' }} min={1} max={1000} />
        </Form.Item>

        <Form.Item name="weight" label={t('provider.weight', { defaultValue: '权重' })}>
          <InputNumber style={{ width: '100%' }} min={1} max={1000} />
        </Form.Item>

        <Form.Item name="isDefault" label={t('provider.isDefault', { defaultValue: '设为默认' })} valuePropName="checked">
          <Switch />
        </Form.Item>

        <Form.Item style={{ marginBottom: 0 }}>
          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={onClose}>
              {t('actions.cancel', { ns: 'common' })}
            </Button>
            <Button type="primary" htmlType="submit" loading={createMutation.isPending || updateMutation.isPending}>
              {t('actions.save', { ns: 'common' })}
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
}