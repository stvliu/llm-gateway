import { useState } from 'react';
import { Row, Col, Card } from 'antd';
import { useTranslation } from 'react-i18next';
import { UserList } from './UserList';
import { ApiKeyList } from './ApiKeyList';

export default function AdminUsers() {
  const { t } = useTranslation('users');
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);

  return (
    <Row gutter={16} style={{ height: '100%' }}>
      <Col span={6}>
        <Card title={t('userList')} style={{ height: '100%' }} styles={{ body: { padding: 0 } }}>
          <UserList onSelect={setSelectedUserId} />
        </Card>
      </Col>
      <Col span={18}>
        <Card title={t('apiKeyList')} style={{ height: '100%' }}>
          <ApiKeyList userId={selectedUserId} />
        </Card>
      </Col>
    </Row>
  );
}
