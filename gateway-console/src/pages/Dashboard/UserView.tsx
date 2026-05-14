import { Row, Col, Card, Statistic, Typography, Space, Tag, Spin, theme } from 'antd';
import {
  AppstoreOutlined,
  KeyOutlined,
  DashboardOutlined,
  CodeOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useModels, useApiKeys } from '@/services/query';

const { Paragraph } = Typography;

export default function UserView() {
  const { t } = useTranslation('dashboard');
  const { token } = theme.useToken();

  const { data: modelsData, isLoading: modelsLoading } = useModels({ size: 100 });
  const { data: apiKeysData, isLoading: apiKeysLoading } = useApiKeys({ size: 100 });

  const stats = {
    availableModels: modelsData?.pagination?.total ?? 0,
    myApiKeys: apiKeysData?.pagination?.total ?? 0,
    monthlyUsage: '0',
  };

  const popularModels = (modelsData?.items || [])
    .filter(m => m.state === 'ACTIVE')
    .slice(0, 4)
    .map(m => ({
      key: String(m.id),
      name: m.displayName || m.providerModelId || 'Unknown',
      provider: m.providerName || 'Unknown',
      tag: 'LLM',
    }));

  const statIconStyle = { fontSize: 24, opacity: 0.6 };

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Spin spinning={modelsLoading || apiKeysLoading}>
        <Row gutter={16}>
          <Col span={8}>
            <Card style={{ height: '100%' }}>
              <Statistic title={t('stats.availableModels')} value={stats.availableModels} prefix={<AppstoreOutlined style={statIconStyle} />} />
            </Card>
          </Col>
          <Col span={8}>
            <Card style={{ height: '100%' }}>
              <Statistic title={t('stats.myApiKeys')} value={stats.myApiKeys} prefix={<KeyOutlined style={statIconStyle} />} />
            </Card>
          </Col>
          <Col span={8}>
            <Card style={{ height: '100%' }}>
              <Statistic title={t('stats.monthlyUsage')} value={stats.monthlyUsage} prefix={<DashboardOutlined style={statIconStyle} />} suffix={t('stats.tokens')} />
            </Card>
          </Col>
        </Row>
      </Spin>

      <Card title={<span><ThunderboltOutlined style={{ marginRight: 8 }} />{t('quickStart.title')}</span>}>
        <Typography>
          <Paragraph>{t('quickStart.description')}</Paragraph>
          <pre style={{ background: token.colorFillAlter, padding: 16, borderRadius: 6, overflow: 'auto', fontSize: 13 }}>
{`curl -X POST https://gateway.example.com/v1/chat/completions \\
  -H "Authorization: Bearer YOUR_API_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{
    "model": "gpt-4",
    "messages": [
      {"role": "user", "content": "Hello!"}
    ]
  }'`}
          </pre>
        </Typography>
      </Card>

      <Row gutter={16} style={{ flex: 1 }}>
        <Col span={12}>
          <Card title={t('usageTrend.title')} style={{ height: '100%' }} styles={{ body: { display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1 } }}>
            <div style={{ textAlign: 'center', color: token.colorTextSecondary }}>
              <DashboardOutlined style={{ fontSize: 48, marginBottom: 16 }} />
              <div>{t('usageTrend.placeholder')}</div>
            </div>
          </Card>
        </Col>
        <Col span={12}>
          <Card title={t('popularModels.title')} style={{ height: '100%' }}>
            <Space direction="vertical" style={{ width: '100%' }} size="small">
              {popularModels.length > 0 ? (
                popularModels.map((model) => (
                  <div key={model.key} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 12px', background: token.colorBgLayout, borderRadius: 6 }}>
                    <Space>
                      <CodeOutlined />
                      <span>{model.name}</span>
                      <span style={{ color: token.colorTextSecondary, fontSize: 12 }}>{model.provider}</span>
                    </Space>
                    <Tag color="blue">{model.tag}</Tag>
                  </div>
                ))
              ) : (
                <div style={{ textAlign: 'center', color: token.colorTextSecondary, padding: 20 }}>{t('popularModels.empty')}</div>
              )}
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  );
}