import { Card, Tag, Typography, Space, Tooltip } from 'antd';
import { GlobalOutlined, LinkOutlined, KeyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { Provider } from '@/types/provider';

const { Text, Paragraph } = Typography;

interface Props {
  provider: Provider;
  onClick: () => void;
}

export default function ProviderCard({ provider, onClick }: Props) {
  const { t } = useTranslation('providers');

  return (
    <Card
      hoverable
      onClick={onClick}
      style={{ height: '100%' }}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <Space>
          <Text strong>{provider.providerName}</Text>
          <Tag color={provider.state === 'ACTIVE' ? 'green' : 'default'}>
            {provider.state}
          </Tag>
        </Space>

        {provider.websiteUrl && (
          <Tooltip title={provider.websiteUrl}>
            <Text type="secondary" ellipsis style={{ maxWidth: 200 }}>
              <GlobalOutlined /> {provider.websiteUrl}
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
