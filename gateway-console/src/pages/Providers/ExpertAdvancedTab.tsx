import { useState, useCallback } from 'react';
import {
  Card,
  Form,
  InputNumber,
  Input,
  Button,
  App,
  Empty,
  Typography,
  Space,
  Alert,
} from 'antd';
import { PlusOutlined, DeleteOutlined, SaveOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useChannels, useUpdateChannel } from '@/services/query/useChannels';
import { useAuthStore } from '@/stores/authStore';
import { P } from '@/constants/permissions';
import type { Provider } from '@/types/provider';
import type { Channel } from '@/types/channel';

const { Text } = Typography;

/** 自定义 Header 行 */
interface HeaderRow {
  key: string;
  name: string;
  value: string;
}

interface Props {
  provider: Provider | null;
}

/**
 * 高级设置标签页
 * 配置超时、重试策略和自定义 Header
 */
export default function ExpertAdvancedTab({ provider }: Props) {
  const { t } = useTranslation('providers');

  if (!provider) {
    return <Empty description={t('noProviderData', { defaultValue: '暂无供应商数据' })} />;
  }

  return (
    <div>
      <Text strong style={{ fontSize: 16 }}>
        {t('advanced.title', { defaultValue: '高级设置' })}
      </Text>
      <div style={{ marginTop: 8, marginBottom: 16, color: '#64748b', fontSize: 13 }}>
        {t('advanced.desc', { defaultValue: '配置超时、重试策略、自定义 Header 等高级选项。' })}
      </div>
      <ChannelAdvancedList providerId={provider.id} />
    </div>
  );
}

/** 渠道高级设置列表 */
function ChannelAdvancedList({ providerId }: { providerId: number }) {
  const { t } = useTranslation('providers');
  const { data: channels, isLoading } = useChannels(providerId);

  if (isLoading) {
    return <Card loading style={{ marginBottom: 16 }} />;
  }

  if (!channels || channels.length === 0) {
    return (
      <Empty
        description={t('advanced.noChannels', { defaultValue: '暂无渠道，请先创建渠道' })}
        style={{ margin: '40px 0' }}
      />
    );
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      {channels.map((channel) => (
        <ChannelAdvancedCard key={channel.id} channel={channel} />
      ))}
    </Space>
  );
}

/** 单个渠道的高级设置卡片 */
function ChannelAdvancedCard({ channel }: { channel: Channel }) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();
  const { hasPermission } = useAuthStore();
  const canWrite = hasPermission(P.PROVIDER_WRITE);
  const updateChannelMutation = useUpdateChannel();

  // 表单状态
  const [timeout, setTimeout] = useState<number | null>(channel.timeout ?? null);
  const [maxRetries, setMaxRetries] = useState<number | null>(channel.maxRetries ?? null);

  // 自定义 Header 本地状态
  const [headers, setHeaders] = useState<HeaderRow[]>([]);

  // 添加 Header 行
  const handleAddHeader = useCallback(() => {
    setHeaders((prev) => [
      ...prev,
      { key: `header-${Date.now()}`, name: '', value: '' },
    ]);
  }, []);

  // 删除 Header 行
  const handleRemoveHeader = useCallback((rowKey: string) => {
    setHeaders((prev) => prev.filter((h) => h.key !== rowKey));
  }, []);

  // 更新 Header 名称
  const handleHeaderNameChange = useCallback((rowKey: string, name: string) => {
    setHeaders((prev) =>
      prev.map((h) => (h.key === rowKey ? { ...h, name } : h))
    );
  }, []);

  // 更新 Header 值
  const handleHeaderValueChange = useCallback((rowKey: string, value: string) => {
    setHeaders((prev) =>
      prev.map((h) => (h.key === rowKey ? { ...h, value } : h))
    );
  }, []);

  // 保存
  const handleSave = useCallback(async () => {
    // 自定义 Header 演示提示
    const hasCustomHeaders = headers.some((h) => h.name.trim() !== '');
    if (hasCustomHeaders) {
      message.info(
        t('advanced.headerDemoMode', {
          defaultValue: '自定义 Header 功能即将上线，当前仅为演示模式，数据不会持久化',
        })
      );
    }

    try {
      await updateChannelMutation.mutateAsync({
        id: channel.id,
        data: {
          timeout,
          maxRetries,
        },
      });
      message.success(
        t('advanced.saveSuccess', { defaultValue: '高级设置保存成功' })
      );
    } catch {
      message.error(
        t('advanced.saveFailed', { defaultValue: '高级设置保存失败' })
      );
    }
  }, [timeout, maxRetries, headers, channel.id, updateChannelMutation, message, t]);

  return (
    <Card
      title={channel.name}
      size="small"
      style={{ marginBottom: 0 }}
      extra={
        canWrite ? (
          <Button
            type="primary"
            size="small"
            icon={<SaveOutlined />}
            loading={updateChannelMutation.isPending}
            onClick={handleSave}
          >
            {t('actions.save', { defaultValue: '保存' })}
          </Button>
        ) : null
      }
    >
      <Form layout="vertical" disabled={!canWrite}>
        {/* 超时时间 */}
        <Form.Item
          label={t('advanced.timeout', { defaultValue: '超时时间 (ms)' })}
          help={t('advanced.timeoutHelp', {
            defaultValue: '请求上游服务的最大等待时间，超时后自动断开连接',
          })}
        >
          <InputNumber
            value={timeout}
            onChange={(val) => setTimeout(val)}
            style={{ width: '100%' }}
            step={1000}
            min={1000}
            placeholder="30000"
            addonAfter="ms"
          />
        </Form.Item>

        {/* 最大重试次数 */}
        <Form.Item
          label={t('advanced.maxRetries', { defaultValue: '最大重试次数' })}
          help={t('advanced.maxRetriesHelp', {
            defaultValue: '请求失败后自动重试的最大次数，0 表示不重试',
          })}
        >
          <InputNumber
            value={maxRetries}
            onChange={(val) => setMaxRetries(val)}
            style={{ width: '100%' }}
            min={0}
            max={10}
            placeholder="3"
          />
        </Form.Item>

        {/* 自定义 Header */}
        <Form.Item
          label={t('advanced.customHeaders', { defaultValue: '自定义 Header' })}
          help={t('advanced.customHeadersHelp', {
            defaultValue: '每个请求发往上游时额外携带的 HTTP Header，可用于鉴权或透传标识',
          })}
        >
          <div style={{ marginBottom: 8 }}>
            <Alert
              type="info"
              showIcon
              message={t('advanced.headerDemoNotice', {
                defaultValue: '自定义 Header 存储于渠道元数据中，该功能即将上线',
              })}
              style={{ marginBottom: 8 }}
            />
          </div>

          {headers.map((row) => (
            <div
              key={row.key}
              style={{
                display: 'flex',
                gap: 8,
                marginBottom: 8,
                alignItems: 'center',
              }}
            >
              <Input
                value={row.name}
                onChange={(e) => handleHeaderNameChange(row.key, e.target.value)}
                placeholder={t('advanced.headerName', {
                  defaultValue: 'Header 名称',
                })}
                style={{ flex: 1 }}
              />
              <Input
                value={row.value}
                onChange={(e) => handleHeaderValueChange(row.key, e.target.value)}
                placeholder={t('advanced.headerValue', {
                  defaultValue: 'Header 值',
                })}
                style={{ flex: 1 }}
              />
              <Button
                type="text"
                danger
                icon={<DeleteOutlined />}
                onClick={() => handleRemoveHeader(row.key)}
                disabled={!canWrite}
              />
            </div>
          ))}

          {canWrite && (
            <Button
              type="dashed"
              icon={<PlusOutlined />}
              onClick={handleAddHeader}
              style={{ width: '100%' }}
            >
              {t('advanced.addHeader', { defaultValue: '添加 Header' })}
            </Button>
          )}
        </Form.Item>
      </Form>
    </Card>
  );
}
