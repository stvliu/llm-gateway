import { Card, Tag, Typography, theme } from 'antd';
import { CheckCircleFilled } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Model } from '@/types/model';

const { Text } = Typography;

interface Props {
  model: Model;
  onSelect?: (model: Model) => void;
}

export default function ModelCard({ model, onSelect }: Props) {
  const { t } = useTranslation('developer');
  const { token } = theme.useToken();

  const capabilityKeys: Record<string, string> = {
    vision: 'capability.vision',
    function_calling: 'capability.functionCalling',
    streaming: 'capability.streaming',
  };

  const caps = model.capabilities
    ? Object.entries(model.capabilities)
        .filter(([, v]) => v)
        .map(([k]) => t(capabilityKeys[k] || k))
    : [];

  return (
    <Card
      hoverable
      size="small"
      onClick={() => onSelect?.(model)}
      styles={{ body: { padding: 16 } }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Text strong style={{ fontSize: 15 }}>{model.displayName || model.modelName}</Text>
          <div>
            <Text type="secondary" style={{ fontSize: 12 }}>{model.modelName}</Text>
          </div>
        </div>
        <CheckCircleFilled style={{ color: token.colorSuccess, fontSize: 18 }} />
      </div>
      {caps.length > 0 && (
        <div style={{ marginTop: 8, display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {caps.map((c) => (
            <Tag key={c} color="blue" style={{ fontSize: 11 }}>{c}</Tag>
          ))}
        </div>
      )}
      <div style={{ marginTop: 8, fontSize: 12, color: token.colorTextSecondary }}>
        {t('model.contextWindow')} {model.contextWindow ? `${(model.contextWindow / 1000).toFixed(0)}K` : '-'}
      </div>
    </Card>
  );
}