import { useState } from 'react';
import { Row, Col, Input, Button, Typography, Empty, Skeleton } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModels } from '@/services/query/useModels';
import ModelCard from './ModelCard';
import KeyGenerateModal from './KeyGenerateModal';
import CodeSnippet from './CodeSnippet';
import DeveloperKeyList from './DeveloperKeyList';

const { Title, Paragraph } = Typography;

export default function Developer() {
  const { t } = useTranslation('developer');
  const { data: models, isLoading } = useModels();
  const [search, setSearch] = useState('');
  const [keyModalOpen, setKeyModalOpen] = useState(false);
  const [currentKey, setCurrentKey] = useState<string>();

  const filtered = models?.filter((m) =>
    m.state === 'ACTIVE' &&
    (m.displayName || m.modelName).toLowerCase().includes(search.toLowerCase())
  ) ?? [];

  return (
    <div style={{ maxWidth: 960, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>{t('title', { defaultValue: '开发者门户' })}</Title>
          <Paragraph type="secondary" style={{ margin: '4px 0 0' }}>
            {t('subtitle', { defaultValue: '浏览可用模型，创建 API Key 快速开始' })}
          </Paragraph>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setKeyModalOpen(true)}>
          {t('createKey', { defaultValue: '创建 API Key' })}
        </Button>
      </div>

      <Input.Search
        placeholder={t('search', { defaultValue: '搜索模型...' })}
        style={{ width: 320, marginBottom: 16 }}
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        allowClear
      />

      {isLoading ? (
        <Row gutter={[12, 12]}>
          {Array.from({ length: 6 }).map((_, i) => (
            <Col key={i} xs={24} sm={12} md={8}>
              <Skeleton active paragraph={{ rows: 2 }} />
            </Col>
          ))}
        </Row>
      ) : filtered.length === 0 ? (
        <Empty description={t('noModels', { defaultValue: '暂无可用模型，请联系团队管理员开通' })} />
      ) : (
        <Row gutter={[12, 12]}>
          {filtered.map((m) => (
            <Col key={m.id} xs={24} sm={12} md={8}>
              <ModelCard model={m} />
            </Col>
          ))}
        </Row>
      )}

      <div style={{ marginTop: 32 }}>
        <Title level={5}>{t('myKeys', { defaultValue: '我的 API Key' })}</Title>
        <DeveloperKeyList />
      </div>

      <div style={{ marginTop: 32 }}>
        <Title level={5}>{t('quickStart', { defaultValue: '快速开始' })}</Title>
        <CodeSnippet apiKey={currentKey} />
      </div>

      <KeyGenerateModal
        open={keyModalOpen}
        onClose={() => setKeyModalOpen(false)}
        onKeyCreated={(key) => setCurrentKey(key)}
      />
    </div>
  );
}