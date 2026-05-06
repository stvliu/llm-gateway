import { Row, Col, Card, Statistic, Table, Tag } from 'antd';
import {
  AppstoreOutlined,
  TeamOutlined,
  ApiOutlined,
  DashboardOutlined,
  RiseOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

export default function AdminDashboard() {
  const { t } = useTranslation('dashboard');

  // 静态数据 - 后续接入真实 API
  const stats = {
    modelCount: 12,
    userCount: 45,
    todayRequests: 1256,
    tokenUsage: '5.6M',
  };

  // 静态数据 - 后续接入真实 API
  // TODO: 接入后端 API，替换 mockData
  const recentActivities = [
    { key: '1', action: t('activity.actions.createUser'), user: 'admin', target: 'user_001', time: t('activity.time.minutesAgo', { count: 2 }), status: 'success' },
    { key: '2', action: t('activity.actions.addModel'), user: 'admin', target: 'gpt-4-turbo', time: t('activity.time.minutesAgo', { count: 15 }), status: 'success' },
    { key: '3', action: t('activity.actions.keyExpired'), user: 'system', target: 'key_xxx', time: t('activity.time.hoursAgo', { count: 1 }), status: 'warning' },
    { key: '4', action: t('activity.actions.userLogin'), user: 'user_001', target: '-', time: t('activity.time.hoursAgo', { count: 2 }), status: 'success' },
    { key: '5', action: t('activity.actions.rateLimit'), user: 'user_002', target: 'api_calls', time: t('activity.time.hoursAgo', { count: 3 }), status: 'error' },
  ];

  const columns = [
    { title: t('activity.action'), dataIndex: 'action', key: 'action', width: 120 },
    { title: t('activity.user'), dataIndex: 'user', key: 'user', width: 100 },
    { title: t('activity.target'), dataIndex: 'target', key: 'target' },
    { title: t('activity.time'), dataIndex: 'time', key: 'time', width: 100 },
    {
      title: t('activity.status'),
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          success: 'green',
          warning: 'orange',
          error: 'red',
        };
        const labelMap: Record<string, string> = {
          success: t('status.success'),
          warning: t('status.warning'),
          error: t('status.error'),
        };
        return <Tag color={colorMap[status]}>{labelMap[status]}</Tag>;
      },
    },
  ];

  const statCardStyle = { height: '100%' };
  const statIconStyle = { fontSize: 24, opacity: 0.6 };

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* 统计卡片 */}
      <Row gutter={16}>
        <Col span={6}>
          <Card style={statCardStyle}>
            <Statistic
              title={t('stats.modelCount')}
              value={stats.modelCount}
              prefix={<AppstoreOutlined style={statIconStyle} />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card style={statCardStyle}>
            <Statistic
              title={t('stats.userCount')}
              value={stats.userCount}
              prefix={<TeamOutlined style={statIconStyle} />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card style={statCardStyle}>
            <Statistic
              title={t('stats.todayRequests')}
              value={stats.todayRequests}
              prefix={<ApiOutlined style={statIconStyle} />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card style={statCardStyle}>
            <Statistic
              title={t('stats.tokenUsage')}
              value={stats.tokenUsage}
              prefix={<DashboardOutlined style={statIconStyle} />}
            />
          </Card>
        </Col>
      </Row>

      {/* 趋势图表占位 */}
      <Row gutter={16} style={{ flex: 1 }}>
        <Col span={16}>
          <Card
            title={t('trend.title')}
            style={{ height: '100%' }}
            styles={{ body: { display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1 } }}
          >
            <div style={{ textAlign: 'center', color: '#999' }}>
              <RiseOutlined style={{ fontSize: 48, marginBottom: 16 }} />
              <div>{t('trend.placeholder')}</div>
            </div>
          </Card>
        </Col>
        <Col span={8}>
          <Card
            title={t('modelUsage.title')}
            style={{ height: '100%' }}
            styles={{ body: { display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1 } }}
          >
            <div style={{ textAlign: 'center', color: '#999' }}>
              <AppstoreOutlined style={{ fontSize: 48, marginBottom: 16 }} />
              <div>{t('modelUsage.placeholder')}</div>
            </div>
          </Card>
        </Col>
      </Row>

      {/* 最近活动 */}
      <Card
        title={
          <span>
            <WarningOutlined style={{ marginRight: 8, color: '#faad14' }} />
            {t('activity.title')}
          </span>
        }
      >
        <Table
          dataSource={recentActivities}
          columns={columns}
          pagination={false}
          size="small"
        />
      </Card>
    </div>
  );
}
