import { Card, Tag, Typography, Space, Tooltip } from 'antd';
import { GlobalOutlined, LinkOutlined, KeyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Provider, ProviderType } from '@/types/provider';

const { Text, Paragraph } = Typography;

const TYPE_LABEL: Record<ProviderType, { label: string; color: string }> = {
  OPENAI: { label: 'OpenAI', color: 'green' },
  ANTHROPIC: { label: 'Anthropic', color: 'purple' },
};

interface Props {
  provider: Provider;
  onClick: () => void;
}

export default function ProviderCard({ provider, onClick }: Props) {
  const { t } = useTranslation('providers');

  const typeInfo = TYPE_LABEL[provider.providerType] ?? { label: provider.providerType, color: 'default' };

  return (
    <Card
      hoverable
      onClick={onClick}
      style={{ height: '100%' }}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <Space>
          <Text strong>{provider.providerName}</Text>
          <Tag color={typeInfo.color}>{typeInfo.label}</Tag>
          <Tag color={provider.state === 'ACTIVE' ? 'green' : 'default'}>
            {provider.state}
          </Tag>
        </Space>

        {provider.baseUrl && (
          <Tooltip title={provider.baseUrl}>
            <Text type="secondary" ellipsis style={{ maxWidth: 200 }}>
              <GlobalOutlined /> {provider.baseUrl}
            </Text>
          </Tooltip>
        )}

        {provider.apiDocUrl && (
          <Paragraph type="secondary" ellipsis style={{ maxWidth: 200, marginBottom: 0 }}>
            <LinkOutlined /> {provider.apiDocUrl}
          </Paragraph>
        )}

        {provider.keyStats && (
          <Text type="secondary">
            <KeyOutlined /> {provider.keyStats.activeCount}/{provider.keyStats.totalCount} {t('keys', { defaultValue: 'Keys' })}
          </Text>
        )}
      </Space>
    </Card>
  );
}
