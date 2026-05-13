import { useState, useCallback } from 'react';
import { Card, Button, Space, Tag, Input, InputNumber, Switch, Form, Empty, Popconfirm, theme, message, Tooltip, Typography } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, StarFilled, ApiOutlined, CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeStore } from '@/stores/themeStore';
import { providerApi } from '@/services/api/provider';
import type { NestedApiKeyRequest } from '@/types/provider';

const { Text } = Typography;

interface ApiKeySetupStepProps {
  apiKeys: NestedApiKeyRequest[];
  onChange: (keys: NestedApiKeyRequest[]) => void;
  providerType?: string;
  baseUrl?: string;
}

/** API Key 测试状态 */
type TestStatus = 'idle' | 'testing' | 'success' | 'failed';

/**
 * API Key 配置步骤组件
 * 用于创建向导中配置 API Key，支持添加、编辑、删除、连通性测试
 */
export function ApiKeySetupStep({ apiKeys, onChange, providerType, baseUrl }: ApiKeySetupStepProps) {
  const { t } = useTranslation('providers');
  const { token } = theme.useToken();
  const { getEffectiveTheme } = useThemeStore();
  const isDark = getEffectiveTheme() === 'dark';

  // 编辑状态
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [editForm] = Form.useForm();

  // 测试状态
  const [testStatuses, setTestStatuses] = useState<Record<number, TestStatus>>({});
  const [testMessages, setTestMessages] = useState<Record<number, string>>({});

  // 添加 API Key
  const handleAdd = useCallback(() => {
    const newKey: NestedApiKeyRequest = {
      keyName: `Key ${apiKeys.length + 1}`,
      apiKey: '',
      priority: 100,
      weight: 100,
      isDefault: apiKeys.length === 0,
    };
    onChange([...apiKeys, newKey]);
    setEditingIndex(apiKeys.length);
    editForm.setFieldsValue(newKey);
  }, [apiKeys, onChange, editForm]);

  // 编辑 API Key
  const handleEdit = useCallback((index: number) => {
    setEditingIndex(index);
    editForm.setFieldsValue(apiKeys[index]);
  }, [apiKeys, editForm]);

  // 保存编辑
  const handleSaveEdit = useCallback(() => {
    if (editingIndex === null) return;

    editForm.validateFields().then((values) => {
      const newKeys = [...apiKeys];
      newKeys[editingIndex] = values;

      // 如果设置为默认，取消其他 Key 的默认状态
      if (values.isDefault) {
        newKeys.forEach((key, i) => {
          if (i !== editingIndex) {
            key.isDefault = false;
          }
        });
      }

      onChange(newKeys);
      setEditingIndex(null);
      // 重置测试状态
      setTestStatuses((prev) => ({ ...prev, [editingIndex]: 'idle' }));
      setTestMessages((prev) => ({ ...prev, [editingIndex]: '' }));
    });
  }, [editingIndex, apiKeys, editForm, onChange]);

  // 取消编辑
  const handleCancelEdit = useCallback(() => {
    setEditingIndex(null);
  }, []);

  // 删除 API Key
  const handleDelete = useCallback((index: number) => {
    const newKeys = apiKeys.filter((_, i) => i !== index);
    if (apiKeys[index].isDefault && newKeys.length > 0) {
      newKeys[0].isDefault = true;
    }
    onChange(newKeys);
    if (editingIndex === index) {
      setEditingIndex(null);
    }
  }, [apiKeys, onChange, editingIndex]);

  // 测试 API Key 连通性
  const handleTestKey = useCallback(async (index: number) => {
    const key = apiKeys[index];
    if (!key.apiKey) {
      message.warning(t('validation.apiKeyRequired', { defaultValue: '请先输入 API Key' }));
      return;
    }

    if (!providerType) {
      message.warning(t('validation.providerTypeRequired', { defaultValue: '请先选择供应商类型' }));
      return;
    }

    setTestStatuses((prev) => ({ ...prev, [index]: 'testing' }));
    setTestMessages((prev) => ({ ...prev, [index]: '' }));

    try {
      const result = await providerApi.testApiKey({
        providerType,
        baseUrl,
        apiKey: key.apiKey,
      });

      if (result.success) {
        setTestStatuses((prev) => ({ ...prev, [index]: 'success' }));
        setTestMessages((prev) => ({
          ...prev,
          [index]: result.models?.length
            ? t('test.keyValidWithModels', { defaultValue: '验证成功，发现 {{count}} 个模型', count: result.models.length })
            : t('test.keyValid', { defaultValue: 'API Key 验证成功' }),
        }));
        message.success(t('test.keyValid', { defaultValue: 'API Key 验证成功' }));
      } else {
        setTestStatuses((prev) => ({ ...prev, [index]: 'failed' }));
        setTestMessages((prev) => ({
          ...prev,
          [index]: result.message || t('test.keyInvalid', { defaultValue: 'API Key 验证失败' }),
        }));
        message.error(result.message || t('test.keyInvalid', { defaultValue: 'API Key 验证失败' }));
      }
    } catch {
      setTestStatuses((prev) => ({ ...prev, [index]: 'failed' }));
      setTestMessages((prev) => ({
        ...prev,
        [index]: t('test.connectionFailed', { defaultValue: '连接失败，请检查网络或 API 地址' }),
      }));
      message.error(t('test.connectionFailed', { defaultValue: '连接失败，请检查网络或 API 地址' }));
    }
  }, [apiKeys, providerType, baseUrl, t]);

  // 获取测试状态图标
  const getTestIcon = (status: TestStatus) => {
    switch (status) {
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

  // 空状态
  if (apiKeys.length === 0) {
    return (
      <div>
        <Empty
          description={t('provider.noApiKeys', { defaultValue: '暂无 API Key' })}
          style={{ padding: 40 }}
        >
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            {t('actions.add', { ns: 'common' })}
          </Button>
        </Empty>
      </div>
    );
  }

  return (
    <div>
      {/* 头部 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 16,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <ApiOutlined />
          <span style={{ fontWeight: 600, fontSize: 15 }}>
            {t('provider.apiKeys')}
          </span>
          <Tag color="blue">{apiKeys.length}</Tag>
        </div>
        <Button type="primary" ghost icon={<PlusOutlined />} onClick={handleAdd}>
          {t('actions.add', { ns: 'common' })}
        </Button>
      </div>

      {/* API Key 列表 */}
      <Space direction="vertical" style={{ width: '100%' }} size={12}>
        {apiKeys.map((key, index) => (
          <Card
            key={index}
            size="small"
            style={{
              background: isDark ? '#1f1f1f' : '#fafafa',
              borderRadius: 8,
            }}
            styles={{ body: { padding: '12px 16px' } }}
          >
            {editingIndex === index ? (
              /* 编辑模式 */
              <Form form={editForm} layout="vertical" size="small">
                <Form.Item
                  name="keyName"
                  label={t('provider.keyName', { defaultValue: 'Key 名称' })}
                  rules={[
                    { required: true, message: t('validation.keyNameRequired', { defaultValue: '请输入 Key 名称' }) },
                    { min: 2, max: 50, message: t('validation.keyNameLength', { defaultValue: '名称长度 2-50 字符' }) },
                    { pattern: /^[一-龥a-zA-Z0-9_\- ]+$/, message: t('validation.keyNameFormat', { defaultValue: '仅支持中英文、数字、下划线、横线' }) },
                  ]}
                >
                  <Input placeholder={t('template.keyNamePlaceholder', { defaultValue: '例如：生产环境 Key' })} />
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
                <Form.Item
                  name="priority"
                  label={t('provider.priority')}
                  extra={t('template.priorityExtra', { defaultValue: '数值越大优先级越高' })}
                >
                  <InputNumber style={{ width: '100%' }} min={1} max={1000} />
                </Form.Item>
                <Form.Item
                  name="weight"
                  label={t('provider.weight')}
                  extra={t('template.weightExtra', { defaultValue: '负载均衡权重' })}
                >
                  <InputNumber style={{ width: '100%' }} min={1} max={1000} />
                </Form.Item>
                <Form.Item
                  name="isDefault"
                  label={t('provider.isDefault', { defaultValue: '设为默认' })}
                  valuePropName="checked"
                  extra={t('template.isDefaultExtra', { defaultValue: '默认 Key 优先使用' })}
                >
                  <Switch />
                </Form.Item>
                <Form.Item style={{ marginBottom: 0 }}>
                  <Space>
                    <Button type="primary" onClick={handleSaveEdit}>
                      {t('actions.save', { ns: 'common' })}
                    </Button>
                    <Button onClick={handleCancelEdit}>
                      {t('actions.cancel', { ns: 'common' })}
                    </Button>
                  </Space>
                </Form.Item>
              </Form>
            ) : (
              /* 显示模式 */
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                {/* 左侧信息 */}
                <Space>
                  {key.isDefault && (
                    <StarFilled style={{ color: token.colorWarning }} />
                  )}
                  <span style={{ fontWeight: 500 }}>{key.keyName}</span>
                  {key.apiKey && (
                    <Tag>{key.apiKey.slice(0, 8)}...</Tag>
                  )}
                  {/* 测试状态 */}
                  {getTestIcon(testStatuses[index])}
                  {testMessages[index] && (
                    <Tooltip title={testMessages[index]}>
                      <Text
                        type={testStatuses[index] === 'success' ? 'success' : 'danger'}
                        style={{ fontSize: 12, cursor: 'help' }}
                      >
                        {testStatuses[index] === 'success'
                          ? t('test.valid', { defaultValue: '有效' })
                          : t('test.invalid', { defaultValue: '无效' })}
                      </Text>
                    </Tooltip>
                  )}
                </Space>

                {/* 右侧操作 */}
                <Space>
                  <span style={{ fontSize: 12, color: token.colorTextSecondary }}>
                    {t('provider.priority')}: {key.priority || 100}
                  </span>
                  {/* 测试按钮 */}
                  <Tooltip title={t('test.testConnection', { defaultValue: '测试连通性' })}>
                    <Button
                      type="text"
                      size="small"
                      icon={testStatuses[index] === 'testing'
                        ? <LoadingOutlined />
                        : <ApiOutlined />
                      }
                      onClick={() => handleTestKey(index)}
                      disabled={!key.apiKey || !providerType}
                    />
                  </Tooltip>
                  <Button
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => handleEdit(index)}
                  />
                  <Popconfirm
                    title={t('confirm.delete', { ns: 'common' })}
                    onConfirm={() => handleDelete(index)}
                  >
                    <Button
                      type="text"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                    />
                  </Popconfirm>
                </Space>
              </div>
            )}
          </Card>
        ))}
      </Space>

      {/* 提示 */}
      {apiKeys.length > 0 && !apiKeys.some(k => k.apiKey) && (
        <div style={{ marginTop: 16 }}>
          <Tag color="warning">
            {t('template.apiKeyRequired', { defaultValue: '请至少配置一个有效的 API Key' })}
          </Tag>
        </div>
      )}
    </div>
  );
}

export type { ApiKeySetupStepProps };