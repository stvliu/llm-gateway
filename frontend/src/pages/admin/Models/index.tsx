import { useState } from 'react';
import { Row, Col, Card } from 'antd';
import { useTranslation } from 'react-i18next';
import { ChannelList } from './ChannelList';
import { ModelList } from './ModelList';

export default function AdminModels() {
  const { t } = useTranslation('models');
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null);

  return (
    <Row gutter={16} style={{ height: '100%' }}>
      <Col span={6}>
        <Card title={t('channelList')} style={{ height: '100%' }} styles={{ body: { padding: 0 } }}>
          <ChannelList onSelect={setSelectedProviderId} />
        </Card>
      </Col>
      <Col span={18}>
        <Card title={t('modelList')} style={{ height: '100%' }}>
          <ModelList providerId={selectedProviderId} />
        </Card>
      </Col>
    </Row>
  );
}
