import { Row, Col, Card, Statistic, Typography, Space, Tag } from 'antd';
import {
  AppstoreOutlined,
  KeyOutlined,
  DashboardOutlined,
  CodeOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Paragraph } = Typography;

export default function UserDashboard() {
  const { t } = useTranslation('dashboard');

  // 静态数据 - 后续接入真实 API
  const stats = {
    availableModels: 8,
    myApiKeys: 3,
    monthlyUsage: '120K',
  };

  const popularModels = [
    { key: 'gpt-4', name: 'GPT-4', provider: 'OpenAI', tag: '推荐' },
    { key: 'gpt-3.5-turbo', name: 'GPT-3.5 Turbo', provider: 'OpenAI', tag: '快速' },
    { key: 'claude-3-opus', name: 'Claude 3 Opus', provider: 'Anthropic', tag: '强大' },
    { key: 'claude-3-sonnet', name: 'Claude 3 Sonnet', provider: 'Anthropic', tag: '均衡' },
  ];

  const statCardStyle = { height: '100%' };
  const statIconStyle = { fontSize: 24, opacity: 0.6 };

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* 统计卡片 */}
      <Row gutter={16}>
        <Col span={8}>
          <Card style={statCardStyle}>
            <Statistic
              title={t('stats.availableModels')}
              value={stats.availableModels}
              prefix={<AppstoreOutlined style={statIconStyle} />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card style={statCardStyle}>
            <Statistic
              title={t('stats.myApiKeys')}
              value={stats.myApiKeys}
              prefix={<KeyOutlined style={statIconStyle} />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card style={statCardStyle}>
            <Statistic
              title={t('stats.monthlyUsage')}
              value={stats.monthlyUsage}
              prefix={<DashboardOutlined style={statIconStyle} />}
              suffix={t('stats.tokens')}
            />
          </Card>
        </Col>
      </Row>

      {/* 快速开始 */}
      <Card title={<span><ThunderboltOutlined style={{ marginRight: 8 }} />{t('quickStart.title')}</span>}>
        <Typography>
          <Paragraph>{t('quickStart.description')}</Paragraph>
          <pre style={{
            background: '#f5f5f5',
            padding: 16,
            borderRadius: 6,
            overflow: 'auto',
            fontSize: 13,
          }}>
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

      {/* 用量趋势和热门模型 */}
      <Row gutter={16} style={{ flex: 1 }}>
        <Col span={12}>
          <Card
            title={t('usageTrend.title')}
            style={{ height: '100%' }}
            styles={{ body: { display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1 } }}
          >
            <div style={{ textAlign: 'center', color: '#999' }}>
              <DashboardOutlined style={{ fontSize: 48, marginBottom: 16 }} />
              <div>{t('usageTrend.placeholder')}</div>
            </div>
          </Card>
        </Col>
        <Col span={12}>
          <Card title={t('popularModels.title')} style={{ height: '100%' }}>
            <Space direction="vertical" style={{ width: '100%' }} size="small">
              {popularModels.map((model) => (
                <div
                  key={model.key}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '8px 12px',
                    background: '#fafafa',
                    borderRadius: 6,
                  }}
                >
                  <Space>
                    <CodeOutlined />
                    <span>{model.name}</span>
                    <span style={{ color: '#999', fontSize: 12 }}>{model.provider}</span>
                  </Space>
                  <Tag color="blue">{model.tag}</Tag>
                </div>
              ))}
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
