import { useMemo } from 'react';
import { Button, Typography } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Text } = Typography;

interface Props {
  provider: Record<string, unknown>;
  onEnterYamlMode: () => void;
}

function toYaml(obj: Record<string, unknown>, indent = 0): string {
  const pad = '  '.repeat(indent);
  return Object.entries(obj)
    .map(([key, value]) => {
      if (value === null || value === undefined) return '';
      if (Array.isArray(value)) {
        if (value.length === 0) return `${pad}${key}: []`;
        return `${pad}${key}:\n${value.map((item) => {
          if (typeof item === 'object') return `${pad}  - ${toYaml(item as Record<string, unknown>, indent + 2).trimStart()}`;
          return `${pad}  - ${item}`;
        }).join('\n')}`;
      }
      if (typeof value === 'object') {
        return `${pad}${key}:\n${toYaml(value as Record<string, unknown>, indent + 1)}`;
      }
      return `${pad}${key}: ${value}`;
    })
    .filter(Boolean)
    .join('\n');
}

export default function YamlPreview({ provider, onEnterYamlMode }: Props) {
  const { t } = useTranslation('providers');
  const yaml = useMemo(() => toYaml(provider), [provider]);

  return (
    <div style={{ border: '1px solid #e2e8f0', borderRadius: 8, overflow: 'hidden', marginTop: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px', background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
        <Text strong style={{ fontSize: 13 }}>{t('yamlPreview', { defaultValue: '配置预览（YAML）' })}</Text>
        <Button size="small" icon={<EditOutlined />} onClick={onEnterYamlMode}>
          {t('editYaml', { defaultValue: '编辑 YAML' })}
        </Button>
      </div>
      <pre style={{ margin: 0, padding: 16, background: '#1e293b', color: '#e2e8f0', fontSize: 12, lineHeight: 1.6, maxHeight: 300, overflow: 'auto', borderRadius: '0 0 8px 8px' }}>
        <code>{yaml}</code>
      </pre>
    </div>
  );
}