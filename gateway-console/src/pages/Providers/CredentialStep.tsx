import { useCallback } from 'react';
import { Input, Button, Tag, Space, Typography, App } from 'antd';
import { PlusOutlined, DeleteOutlined, CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Text } = Typography;

export interface CredentialEntry {
  id: string;
  value: string;
  status: 'pending' | 'testing' | 'success' | 'fail';
}

interface Props {
  credentials: CredentialEntry[];
  onChange: (credentials: CredentialEntry[]) => void;
}

/**
 * API Key 配置步骤组件
 * 支持多 Key 管理、批量粘贴和连通性测试（占位）
 */
export default function CredentialStep({ credentials, onChange }: Props) {
  const { t } = useTranslation('providers');
  const { message } = App.useApp();

  const handlePaste = useCallback((e: React.ClipboardEvent) => {
    const text = e.clipboardData.getData('text');
    const keys = text.split(/[\n,]+/).map((s) => s.trim()).filter(Boolean);
    if (keys.length > 1) {
      e.preventDefault();
      const newEntries = keys.map((k) => ({
        id: crypto.randomUUID(),
        value: k,
        status: 'pending' as const,
      }));
      onChange([...credentials, ...newEntries]);
      message.success(t('batchPaste', { defaultValue: `已添加 ${newEntries.length} 个 Key`, count: newEntries.length }));
    }
  }, [credentials, onChange, message, t]);

  const addEntry = useCallback(() => {
    onChange([...credentials, { id: crypto.randomUUID(), value: '', status: 'pending' }]);
  }, [credentials, onChange]);

  const removeEntry = useCallback((id: string) => {
    onChange(credentials.filter((c) => c.id !== id));
  }, [credentials, onChange]);

  const updateValue = useCallback((id: string, value: string) => {
    onChange(credentials.map((c) => (c.id === id ? { ...c, value } : c)));
  }, [credentials, onChange]);

  const testConnectivity = useCallback(async (id: string) => {
    onChange(credentials.map((c) => (c.id === id ? { ...c, status: 'testing' as const } : c)));
    // 模拟连通性测试延迟
    await new Promise((r) => setTimeout(r, 1500));
    const entry = credentials.find((c) => c.id === id);
    if (entry && entry.value.startsWith('sk-')) {
      onChange(credentials.map((c) => (c.id === id ? { ...c, status: 'success' as const } : c)));
      message.success(t('credential.testSuccess', { defaultValue: '连通性测试通过' }));
    } else {
      onChange(credentials.map((c) => (c.id === id ? { ...c, status: 'fail' as const } : c)));
      message.error(t('credential.testFailed', { defaultValue: '连通性测试失败，请检查 Key 是否正确' }));
    }
  }, [credentials, onChange, message, t]);

  return (
    <div>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <div style={{ background: '#f0f5ff', padding: '8px 12px', borderRadius: 6, fontSize: 13, color: '#1d4ed8' }}>
          {t('credential.batchHint', { defaultValue: '支持同时粘贴多个 Key（换行或逗号分隔）' })}
        </div>

        {credentials.map((entry, index) => (
          <div key={entry.id} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <Input.Password
              value={entry.value}
              onChange={(e) => updateValue(entry.id, e.target.value)}
              onPaste={handlePaste}
              placeholder={t('credential.keyPlaceholder', { defaultValue: 'sk-...' })}
              style={{ flex: 1 }}
              addonBefore={<Tag style={{ marginRight: 0 }}>#{index + 1}</Tag>}
              suffix={
                entry.status === 'success' ? <CheckCircleFilled style={{ color: '#22c55e' }} /> :
                entry.status === 'fail' ? <CloseCircleFilled style={{ color: '#ef4444' }} /> :
                entry.status === 'testing' ? <Text type="secondary" style={{ fontSize: 12 }}>测试中...</Text> :
                null
              }
            />
            <Button size="small" onClick={() => testConnectivity(entry.id)} disabled={!entry.value || entry.status === 'testing'}>
              {t('credential.test', { defaultValue: '测试' })}
            </Button>
            <Button type="text" danger icon={<DeleteOutlined />} onClick={() => removeEntry(entry.id)} />
          </div>
        ))}

        <Button type="dashed" icon={<PlusOutlined />} onClick={addEntry} block>
          {t('credential.addKey', { defaultValue: '添加 Key' })}
        </Button>
      </Space>
    </div>
  );
}